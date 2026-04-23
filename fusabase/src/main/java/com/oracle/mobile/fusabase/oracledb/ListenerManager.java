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
import com.oracle.mobile.fusabase.utils.Utils;

import java.security.GeneralSecurityException;
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
    /** Current count of active listeners */
    private int listenerCount;
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
        this.listenerCount = 0;
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

        return Json.createObjectBuilder()
                .add("path", Json.createArrayBuilder(ref.getPathSegments()).build())
                .add("conditions", Json.createArrayBuilder().build())
                .add("explicitOrder", Json.createArrayBuilder().build())
                .build();
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

        listenerCount++;

        listeners.add(listenerData);

        if (options.getActivity() != null) {
            LifecycleOwner lifecycleOwner = ((LifecycleOwner) options.getActivity());
            lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    removeListener(listenerData);
                }
            });
        }

        switch (this.method) {
            case WEBSOCKET:
                // Create the registration payload
                JsonObject registrationPayload = Json.createObjectBuilder()
                        .add("payload", canonicalQueryPayload)
                        .add("queryId", queryId)
                        .add("status", 1)
                        .build();

                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        String key = Utils.getPreferenceData(oracledb.getApp().getApplicationContext(), "LOGGED_IN_USER", "FusabasePreferences");
                        if (key == null || key.isEmpty()) {
                            FusabaseLogger.e(TAG, "User not authenticated - no logged in user key found.");
                            return "";
                        }

                        JsonObject userDetails = Utils.loadJsonObjectFromPreferences(oracledb.getApp().getApplicationContext(), key, "FusabasePreferences");
                        if (!userDetails.containsKey("accessToken")) {
                            FusabaseLogger.e(TAG, "User not authenticated - no access token found.");
                            return "";
                        }
                    } catch (GeneralSecurityException e) {
                        FusabaseLogger.e(TAG, "Security error during authentication check: " + e.getMessage());
                        return "";
                    } catch (Exception e) {
                        FusabaseLogger.e(TAG, "Unexpected error during authentication check: " + e.getMessage());
                        return "";
                    }

                    // Establish a connection with ORDS
                    if (!webSocketController.getWebSocketClient().getState().equals(WebSocketClient.State.OPEN) ||
                        !webSocketController.getWebSocketClient().getState().equals(WebSocketClient.State.CONNECTING) ) {
                        webSocketController.establishWebSocketConnection();
                    } else {
                        FusabaseLogger.d(TAG, "Websocket connection already established or connecting");
                    }

                    // Send request for subscription
                    // Fetch the current state once the connection with the ORDS is successful
                    if(webSocketController.shouldRegisterQuery(queryId)) {
                        // Registration with Controller must happen first before sending the request
                        webSocketController.registerQueryWithController(queryId, listenerData);
                        webSocketController.getWebSocketClient().sendMessage(registrationPayload.toString());
                    } else {
                        webSocketController.registerQueryWithController(queryId, listenerData);
                        webSocketController.putRegistrationSuccessMessage(queryId);
                    }
                    return "";
                });
                break;
            case LONG_POLLING:
                // Fetch the current snapshot of data
                listenerData.getReference().get(Source.SERVER).addOnCompleteListener(new OnCompleteListener<Snapshot>() {
                    @Override
                    public void onComplete(Task<Snapshot> task) {

                            if (task.isSuccessful()) {
                                // Emitting event directly
                                listenerData.executeListener(task.getResult(),
                                        null);
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

    /**
     * Removes a registered listener from this manager. Handles deregistration
     * with the appropriate communication method (WebSocket or long polling)
     * and updates the listener count.
     *
     * @param listenerData The listener data object to remove
     */
    protected void removeListener(ListenerData<K, T> listenerData) {
        FusabaseLogger.d(TAG, "Removing listener for reference: " + ref.getPath() + ", listenerId: " + listenerData.getListenerId());
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
                    if (webSocketController.getWebSocketClient().getState() != WebSocketClient.State.OPEN) {
                        FusabaseLogger.d(TAG, "Websocket connection already destablished");
                    } else {
                        // Send request for subscription
                        webSocketController.getWebSocketClient().sendMessage(deRegistrationPayload.toString());
                        FusabaseLogger.d(TAG, "Listener deregistered");
                    }
                }
                if(this.webSocketController.shouldCloseConnection()){
                    this.webSocketController.closeWebSocketConnection();
                }
                break;
            case LONG_POLLING:
                // We need to just remove listener
                pollingController.listenersUpdated();
                break;
        }


        listenerCount--;
        this.listeners.removeIf(listener -> listenerData.getListenerId() == listener.getListenerId());
    }

    protected List<ListenerData<K, T>> getListeners() {
        return this.listeners;
    }

    protected int getListenerCount() {
        return this.listenerCount;
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
