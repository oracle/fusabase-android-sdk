// Copyright (c) 2015, 2025, Oracle and/or its affiliates.

//-----------------------------------------------------------------------------
//
// This software is dual-licensed to you under the Universal Permissive License
// (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl and Apache License
// 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
// either license.
//
// If you elect to accept the software under the Apache License, Version 2.0,
// the following applies:
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//-----------------------------------------------------------------------------

package com.oracle.mobile.fusabase.oracledb;


import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;


import com.oracle.mobile.fusabase.http.WebSocketClient;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.task.OnCompleteListener;
import com.oracle.mobile.fusabase.task.Task;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * Manages event listeners for database references, providing registration, removal, and execution
 * of listeners. Supports both WebSocket-based real-time updates and long polling fallback.
 * Handles lifecycle management, query deduplication, and communication with the database backend.
 *
 * @param <K> The type of Reference (e.g., DocumentReference, CollectionReference)
 * @param <T> The type of Snapshot (e.g., DocumentSnapshot, QuerySnapshot)
 */
class ListenerManager<K extends Reference, T extends Snapshot> {

    /**
     * Enumeration of communication methods supported for listener management.
     */
    enum Method {
        LONG_POLLING,
        WEBSOCKET
    }

    private static final String TAG = "FusabaseOradb";
    /** List of all registered listeners managed by this instance */
    private final List<ListenerData<K, T>> listeners;
    /** Executor for handling listener operations */
    private final Executor executor;
    /** WebSocket controller for real-time communication */
    private final WebSocketController webSocketController;
    /** Main database instance */
    private final FusabaseOracledb oracledb;
    /** The database reference this manager is associated with */
    private final K ref;
    /** The communication method to use (WebSocket or long polling) */
    private final Method method;
    /** Controller for handling long polling operations */
    private final PollingController<K, T> pollingController;

    /**
     * Constructs a new ListenerManager for the given database instance and reference.
     * Automatically determines whether to use WebSocket or long polling based on configuration.
     *
     * @param oracledb The main database instance
     * @param ref The database reference to manage listeners for
     */
    ListenerManager(@NonNull FusabaseOracledb oracledb,
                    @NonNull K ref) {
        this.listeners = new CopyOnWriteArrayList<>();
        this.executor = Executors.newSingleThreadExecutor();
        this.oracledb = oracledb;
        this.ref = ref;
        this.method = oracledb.getOracledbSettings().getOptions().isUseSocket() ? Method.WEBSOCKET : Method.LONG_POLLING;
        this.pollingController = new PollingController(this);
        this.webSocketController = WebSocketController.getInstance(oracledb);
    }

    /**
     * Creates a standardized JSON payload for WebSocket communication containing
     * the reference path and query parameters.
     *
     * @return A JSON object representing the WebSocket payload
     */
    protected JsonObject createWebSocketPayload() {
        JsonArrayBuilder conditions = Json.createArrayBuilder();
        if (ref instanceof Query) {
            Query query = (Query) ref;
            for (Filter condition : query.conditions) {
                conditions.add(condition.getJsonObject());
            }
        }

        JsonObjectBuilder payload = Json.createObjectBuilder()
                .add("path", Json.createArrayBuilder(ref.getPathSegments()).build())
                .add("conditions", conditions.build());
        return payload.build();
    }

    /**
     * Registers a new event listener for the database reference. Handles both WebSocket
     * and long polling communication methods. Sets up lifecycle management if an Activity
     * is provided and establishes the appropriate communication channel.
     *
     * @param listener The event listener to register
     * @param options Configuration options for the listener including executor and lifecycle management
     * @return A ListenerRegistration object that can be used to remove the listener
     */
    protected ListenerRegistration registerListener(@NonNull EventListener<T> listener,
                                                    @NonNull SnapshotListenOptions options) {

        FusabaseLogger.d(TAG, "Registering listener for reference: " + ref.getPath() + ", method: " + method);

        // First we need to canonicalize the query payload
        JsonObject queryPayload = createWebSocketPayload();
        JsonObject canonicalQueryPayload = sortJsonObject(queryPayload);
        int queryId = canonicalQueryPayload.hashCode();

        ListenerData<K, T> listenerData = new ListenerData<>(listener,
                this.ref,
                queryId,
                this,
                options.activity,
                options.executor,
                options.metadataChanges);

        listeners.add(listenerData);
        OfflineEventBus.register(listenerData);
        listenerData.attachSession();
        if (!listenerData.isActive()) return listenerData.getListenerRegistration();

        if (options.getActivity() != null) {
            LifecycleOwner lifecycleOwner = ((LifecycleOwner) options.getActivity());
            lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    removeListener(listenerData);
                }
            });
        }

        boolean cacheOnly = options.getSource() == ListenSource.CACHE;
        emitCacheSnapshot(listenerData, cacheOnly);
        if (cacheOnly) {
            return listenerData.getListenerRegistration();
        }

        switch (this.method) {
            case WEBSOCKET:
                // Create the registration payload
                JsonObject registrationPayload = Json.createObjectBuilder()
                        .add("payload", canonicalQueryPayload)
                        .add("queryId", queryId)
                        .add("status", 1)
                        .build();

                CompletableFuture.runAsync(() ->
                        webSocketController.subscribeQuery(queryId, listenerData, registrationPayload));
                break;
            case LONG_POLLING:
                // Fetch the current snapshot of data
                listenerData.getReference().get(Source.SERVER).addOnCompleteListener(new OnCompleteListener<Snapshot>() {
                    @Override
                    public void onComplete(Task<Snapshot> task) {
                            if (!listeners.contains(listenerData)) {
                                return;
                            }

                            if (task.isSuccessful()) {
                                // Emitting event directly
                                listenerData.executeListener(task.getResult(),
                                        null);
                                listenerData.markInitialRemoteSnapshotLoaded();
                                webSocketController.processQueue(queryId);
                            } else {
                                FusabaseOracledbException error = new FusabaseOracledbException(
                                        "Cannot fetch the current snapshot for the data at " +
                                                listenerData.getReference().getPath() + ". " +
                                                Objects.requireNonNull(task.getException().getMessage()),
                                        FusabaseOracledbException.Code.INTERNAL
                                );
                                listenerData.executeListener(null, error);
                            }
                    }
                });
                pollingController.listenersUpdated();
                break;
        }

        return listenerData.getListenerRegistration();
    }

    private void emitCacheSnapshot(@NonNull ListenerData<K, T> listenerData, boolean reportCacheErrors) {
        listenerData.getReference().get(Source.CACHE).addOnCompleteListener(new OnCompleteListener<Snapshot>() {
            @Override
            public void onComplete(Task<Snapshot> task) {
                if (!listeners.contains(listenerData)) {
                    return;
                }

                if (task.isSuccessful() && (reportCacheErrors || shouldRaiseInitialCacheEvent(task.getResult()))) {
                    listenerData.executeListener(task.getResult(), null);
                } else if (reportCacheErrors) {
                    Exception taskException = task.getException();
                    FusabaseOracledbException error = taskException instanceof FusabaseOracledbException ?
                            (FusabaseOracledbException) taskException :
                            new FusabaseOracledbException(taskException == null ||
                                    taskException.getMessage() == null ?
                                    "Cannot fetch the current snapshot from the local cache." :
                                    taskException.getMessage(),
                                    FusabaseOracledbException.Code.INTERNAL);
                    listenerData.executeListener(null, error);
                }
            }
        });
    }

    private boolean shouldRaiseInitialCacheEvent(@NonNull Snapshot snapshot) {
        if (!oracledb.isNetworkEnabled()) {
            return true;
        }
        if (snapshot instanceof DocumentSnapshot) {
            DocumentSnapshot documentSnapshot = (DocumentSnapshot) snapshot;
            return documentSnapshot.exists() || documentSnapshot.getMetadata().hasPendingWrites();
        }
        if (snapshot instanceof QuerySnapshot) {
            QuerySnapshot querySnapshot = (QuerySnapshot) snapshot;
            return !querySnapshot.isEmpty() || querySnapshot.getMetadata().hasPendingWrites();
        }
        return true;
    }

    /**
     * Removes a registered listener from this manager. Handles deregistration
     * with the appropriate communication method (WebSocket or long polling)
     * and updates the listener count.
     *
     * @param listenerData The listener data object to remove
     */
    protected void removeListener(ListenerData<K, T> listenerData) {
        listenerData.deactivate();
        FusabaseLogger.d(TAG, "Removing listener for reference: " + ref.getPath() + ", listenerId: " + listenerData.getListenerId());
        boolean removed = this.listeners.remove(listenerData);
        OfflineEventBus.unregister(listenerData);
        if (!removed) {
            FusabaseLogger.d(TAG, "Listener already removed for reference: " + ref.getPath() + ", listenerId: " + listenerData.getListenerId());
            return;
        }

        // Unsubscribe
        JsonObject queryPayload = createWebSocketPayload();
        JsonObject canonicalQueryPayload = sortJsonObject(queryPayload);
        int queryId = canonicalQueryPayload.hashCode();

        JsonObject deRegistrationPayload = Json.createObjectBuilder()
                .add("payload", canonicalQueryPayload)
                .add("queryId", queryId)
                .add("status", 0)
                .build();
        switch (this.method)
        {
            case WEBSOCKET:
                this.webSocketController.deregisterQuery(queryId, listenerData);
                if(this.webSocketController.shouldDeregisterQuery(queryId)) {
                    if (webSocketController.getWebSocketTransport().getState() != WebSocketClient.State.OPEN) {
                        FusabaseLogger.d(TAG, "Websocket connection already destablished");
                    } else {
                        // Send request for subscription
                        webSocketController.getWebSocketTransport().sendMessage(deRegistrationPayload.toString());
                        FusabaseLogger.d(TAG, "Listener deregistered");
                    }
                }
                if(this.webSocketController.shouldCloseConnection()){
                    this.webSocketController.closeWebSocketConnection();
                }
                break;
            case LONG_POLLING:
                // Listener has already been removed, so polling observes the updated count.
                pollingController.listenersUpdated();
                break;
        }
    }

    protected List<ListenerData<K, T>> getListeners() {
        return this.listeners;
    }

    protected int getListenerCount() {
        return this.listeners.size();
    }

    // listenerId is queryId
    protected ListenerData<K,T>  getListener (int listenerId) {
       for(ListenerData<K,T> listenerData : this.listeners)
           if(listenerData.getListenerId() == listenerId)
               return listenerData;

       return null;
    }

    protected void triggerListeners(T value, FusabaseOracledbException error) {
        FusabaseLogger.d(TAG, "Triggering " + listeners.size() + " listeners for reference: " + ref.getPath() +
                (error != null ? ", error: " + error.getMessage() : ", success"));
        this.listeners.forEach(listenerData -> listenerData.executeListener(value, error));
    }

    private static JsonObject sortJsonObject(JsonObject obj) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        obj.keySet().stream()
                .sorted()
                .forEach(key -> {
                    JsonValue value = obj.get(key);
                    switch (value.getValueType()) {
                        case OBJECT:
                            builder.add(key, sortJsonObject(obj.getJsonObject(key)));
                            break;
                        case ARRAY:
                            builder.add(key, sortJsonArray(obj.getJsonArray(key)));
                            break;
                        default: // STRING, NUMBER, TRUE, FALSE, NULL
                            builder.add(key, value);
                    }
                });
        return builder.build();
    }

    private static JsonArray sortJsonArray(JsonArray array) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (JsonValue value : array) {
            switch (value.getValueType()) {
                case OBJECT:
                    builder.add(sortJsonObject(value.asJsonObject()));
                    break;
                case ARRAY:
                    builder.add(sortJsonArray(value.asJsonArray()));
                    break;
                default:
                    builder.add(value);
            }
        }
        return builder.build();
    }
}
