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

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.http.WebSocketCallback;
import com.oracle.mobile.fusabase.http.WebSocketClient;
import com.oracle.mobile.fusabase.internal.AuthSession;
import com.oracle.mobile.fusabase.internal.AuthInteropRegistry;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.EndpointCatalog;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.SnapshotAuthorizationResponse;
import com.oracle.mobile.fusabase.task.OnCompleteListener;
import com.oracle.mobile.fusabase.task.Task;

import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Manages WebSocket connections for real-time data synchronization.
 * Handles listener registration, message processing, and connection lifecycle
 * for snapshot listeners that require real-time updates.
 */
class WebSocketController {

    /** Log tag for debugging. */
    private static final String TAG = "FusabaseOradb";
    private static final int MAX_REALTIME_MESSAGE_QUEUE_COUNT = 1000;
    private static final int MAX_REALTIME_MESSAGE_QUEUE_BYTES = 1024 * 1024;

    /** Singleton instance of the WebSocket controller. */
    private static final ConcurrentHashMap<FusabaseOracledb, WebSocketController>
            INSTANCES = new ConcurrentHashMap<>();
    private final AuthSession authSession;
    private AuthSession.State observedSession;
    private final AuthSession.Subscription authSubscription;
    private volatile boolean disposed;

    /** Internal transport for managing the connection. */
    private final WebSocketTransport webSocketTransport;

    /** Receives events from the current transport. */
    private final WebSocketCallback webSocketCallback;

    /** Serializes retry and connectivity state transitions. */
    private final ScheduledExecutorService recoveryExecutor;

    /** Monitors validated default-network connectivity. */
    private final ConnectivityMonitor connectivityMonitor;

    /** Ensures that only one connection attempt can be active. */
    private AuthSession.State authorizingSession;

    private ScheduledFuture<?> retryFuture;
    private int retryAttempt;
    private boolean validatedOnline = true;
    private volatile long currentConnectionGeneration;

    interface StatePublisher {
        void publish(RealtimeConnectionState state, int retryAttempt, RealtimeConnectionReason reason);
    }
    private final Callable<String> connectionUrl;
    private final StatePublisher statePublisher;

    /** Queue for incoming WebSocket messages. */
    private final Queue<JsonObject> messageQueue;
    private int messageQueueBytes;

    /** Map of query IDs to their registered listeners. */
    private final HashMap<Integer, List<ListenerData<? extends Reference, ? extends Snapshot>>> registeredQuery;
    private final HashMap<Integer, JsonObject> registrationPayloads;

    /**
     * Registers a query listener with the WebSocket controller.
     *
     * @param queryId the ID of the query to register
     * @param listenerData the listener data to register
     */
    protected synchronized void registerQueryWithController (Integer queryId,
                                                @NonNull ListenerData<? extends Reference, ? extends Snapshot> listenerData,
                                                @NonNull JsonObject registrationPayload){

        if (disposed || !listenerData.isActive() || authSession.current().userId == null) return;
        List<ListenerData<? extends Reference, ? extends Snapshot>>  listenerDataList = this.registeredQuery.getOrDefault(queryId,  new ArrayList<>());
        listenerDataList.add(listenerData);
        this.registeredQuery.put(queryId, listenerDataList);
        this.registrationPayloads.put(queryId, registrationPayload);
        this.connectivityMonitor.start();
        if (validatedOnline) {
            statePublisher.publish(
                    RealtimeConnectionState.CONNECTING, retryAttempt,
                    RealtimeConnectionReason.NONE);
        }
    }

    void subscribeQuery(Integer queryId,
                        ListenerData<? extends Reference, ? extends Snapshot> listener,
                        JsonObject payload) {
        synchronized (this) {
            onAuthSessionChanged();
            if (disposed || !listener.isActive() || authSession.current().userId == null) return;
            boolean newQuery = shouldRegisterQuery(queryId);
            registerQueryWithController(queryId, listener, payload);
            WebSocketClient.State state = webSocketTransport.getState();
            if (state == WebSocketClient.State.OPEN) {
                if (newQuery) webSocketTransport.sendMessage(payload.toString());
                else putRegistrationSuccessMessage(queryId);
                return;
            }
            if (state == WebSocketClient.State.CONNECTING) return;
        }
        establishWebSocketConnection();
    }

    private synchronized void onAuthSessionChanged() {
        if (disposed) return;
        AuthSession.State latest = authSession.current();
        if (latest == observedSession) return;
        observedSession = latest;
        ++currentConnectionGeneration;
        authorizingSession = null;
        webSocketTransport.cancel();
        cancelRetry();
        registrationPayloads.clear();
        registeredQuery.clear();
        messageQueue.clear();
        messageQueueBytes = 0;
        retryAttempt = 0;
        connectivityMonitor.stop();
        statePublisher.publish(RealtimeConnectionState.DISCONNECTED,
                0, RealtimeConnectionReason.AUTH_CHANGED);
    }

    /**
     * Deregisters a query listener from the WebSocket controller.
     *
     * @param queryId the ID of the query to deregister
     * @param listenerData the listener data to remove
     */
    protected synchronized void deregisterQuery (Integer queryId, @NonNull ListenerData<? extends Reference, ? extends Snapshot> listenerData){

        List<ListenerData<? extends Reference, ? extends Snapshot>>  listenerDataList = this.registeredQuery.get(queryId);

        if(listenerDataList == null)
            return;
        listenerDataList.remove(listenerData);

        if(listenerDataList.isEmpty())
            this.registeredQuery.remove(queryId);
        else
            this.registeredQuery.put(queryId, listenerDataList);

        if (this.registeredQuery.isEmpty()) {
            this.registrationPayloads.remove(queryId);
            cancelRetry();
            this.connectivityMonitor.stop();
            this.validatedOnline = false;
        }
    }

    /**
     * Checks if a query should be deregistered based on listener count.
     *
     * @param queryId the query ID to check
     * @return true if the query should be deregistered, false otherwise
     */
    protected synchronized boolean shouldDeregisterQuery (Integer queryId) {
        return !this.registeredQuery.containsKey(queryId);
    }

    /**
     * Checks if the WebSocket connection should be closed.
     *
     * @return true if no queries are registered, false otherwise
     */
    protected synchronized boolean shouldCloseConnection () {
        return this.registeredQuery.isEmpty();
    }

    /**
     * Checks if a query should be registered.
     *
     * @param queryId the query ID to check
     * @return true if the query should be registered, false otherwise
     */
    protected synchronized boolean shouldRegisterQuery (Integer queryId) {
        return !this.registeredQuery.containsKey(queryId);
    }

    /**
     * Adds a registration success message to the queue.
     *
     * @param queryId the query ID that was successfully registered
     */
    protected synchronized void putRegistrationSuccessMessage (Integer queryId) {
        JsonObject messageObject = Json.createObjectBuilder()
            .add("queryId", queryId)
            .add("success", 1)
            .build();
        if (enqueueMessage(messageObject)) {
            processQueue(queryId);
        }
    }

    /**
     * Gets the singleton instance of WebSocketController.
     *
     * @param oracledb the Oradb instance
     * @return the WebSocketController instance
     */
    protected static WebSocketController getInstance(@NonNull FusabaseOracledb oracledb) {
        return INSTANCES.computeIfAbsent(oracledb, WebSocketController::new);
    }

    /** Creates an isolated controller for package-private deterministic tests. */
    static WebSocketController createForTesting(@NonNull FusabaseOracledb oracledb,
                                                @NonNull WebSocketTransportFactory transportFactory) {
        return new WebSocketController(oracledb, transportFactory,
                new ConnectivityMonitor(validated -> { }));
    }

    /**
     * Private constructor for creating WebSocketController instances.
     *
     * @param oracledb the Oradb instance this controller belongs to
     */
    private WebSocketController(@NonNull FusabaseOracledb oracledb) {
        this(oracledb, new DefaultWebSocketTransportFactory(), null);
    }

    private WebSocketController(@NonNull FusabaseOracledb oracledb,
                                @NonNull WebSocketTransportFactory transportFactory,
                                ConnectivityMonitor injectedConnectivityMonitor) {
        this(AuthInteropRegistry.session(oracledb.getApp()), transportFactory,
                listener -> injectedConnectivityMonitor == null
                        ? new ConnectivityMonitor(oracledb.getApp().getApplicationContext(), listener)
                        : injectedConnectivityMonitor,
                () -> createConnectionUrl(oracledb), oracledb::publishRealtimeConnectionState);
    }

    WebSocketController(AuthSession authSession, WebSocketTransportFactory transportFactory,
                        Function<ConnectivityMonitor.Listener, ConnectivityMonitor> monitorFactory,
                        Callable<String> connectionUrl, StatePublisher statePublisher) {
        this.authSession = authSession;
        this.observedSession = authSession.current();
        this.connectionUrl = connectionUrl;
        this.statePublisher = statePublisher;
        this.recoveryExecutor = Executors.newSingleThreadScheduledExecutor();
        this.connectivityMonitor = monitorFactory.apply(this::onConnectivityChanged);
        this.messageQueue = new LinkedList<>();
        this.messageQueueBytes = 0;
        // Perform DB cleanups here
        this.webSocketCallback = new WebSocketCallback() {
            /**
             * Called when a message is received from the WebSocket.
             *
             * @param message the received message
             */
            @Override
            public void onMessage(@NonNull String message) {

                JsonObject messageObject;

                try (JsonReader jsonReader = Json.createReader(new StringReader(message))) {
                    messageObject = jsonReader.readObject();

                } catch (Exception e) {
                    FusabaseLogger.e(TAG, "Error encountered while parsing realtime message");
                    FusabaseLogger.d(TAG, "Ignoring received message.");
                    return;
                }

                if (!messageObject.containsKey("queryId")
                        || !messageObject.get("queryId").getValueType().equals(JsonValue.ValueType.NUMBER)) {
                    FusabaseLogger.w(TAG, "Ignoring realtime message without a numeric query id");
                    return;
                }

                if (enqueueMessage(messageObject)) {
                    processQueue(messageObject.getInt("queryId"));
                }
            }

            /** Called when the WebSocket connection is opened. */
            @Override
            public void onOpen() {
                FusabaseLogger.d(TAG, "Connection with ords-baas-server established.");
                statePublisher.publish(RealtimeConnectionState.CONNECTED,
                        0, RealtimeConnectionReason.NONE);
                final long openedGeneration = currentConnectionGeneration;
                final AuthSession.State openedSession = authSession.current();
                executeRecovery(() -> {
                    synchronized (WebSocketController.this) {
                        if (disposed || openedGeneration != currentConnectionGeneration || !authSession.isCurrent(openedSession)
                                || openedSession.userId == null) return;
                        retryAttempt = 0;
                        cancelRetry();
                        for (JsonObject registration : registrationPayloads.values()) {
                            webSocketTransport.sendMessage(registration.toString());
                        }
                    }
                });
            }

            /** Called when the WebSocket connection is closed. */
            @Override
            public void onClosed() {
                FusabaseLogger.d(TAG, "Connection with ords-baas-server closed.");
                statePublisher.publish(RealtimeConnectionState.RETRYING,
                        retryAttempt + 1, RealtimeConnectionReason.CLOSED);
                scheduleRetryIfNeeded();
            }

            /** Called when the WebSocket connection is closing. */
            @Override
            public void onClosing() {
                FusabaseLogger.d(TAG, "Connection with ords-baas-server closing.");

            }

            /** Called when the WebSocket connection fails. */
            @Override
            public void onFailure() {
                FusabaseLogger.d(TAG, "Connection with ords-baas-server failed.");
                statePublisher.publish(RealtimeConnectionState.RETRYING,
                        retryAttempt + 1, RealtimeConnectionReason.TRANSPORT_FAILURE);
                scheduleRetryIfNeeded();
            }
        };
        this.webSocketTransport = transportFactory.create();
        this.registeredQuery = new HashMap<>();
        this.registrationPayloads = new HashMap<>();
        // This controller has the same lifetime as its cached database client.
        authSubscription = authSession.subscribe(ignored -> onAuthSessionChanged());
    }

    /**
     * Processes a WebSocket message and notifies appropriate listeners.
     *
     * @param messageObject the JSON message to process
     */
    private boolean processMessage (@NonNull JsonObject messageObject) {

        int queryId = messageObject.getInt("queryId");
        List<ListenerData<? extends Reference, ? extends Snapshot>> listeners = this.registeredQuery.get(queryId);

        if(listeners == null || listeners.isEmpty())
        {
            FusabaseLogger.w(TAG, "Unknown notification received which doesn't belong to any query.");
            return true;
        }

        if(messageObject.containsKey("success"))
        {
            // Once the connection is successful then only emit the first event
            // We need to queue up all the changes before we have a first snapshot of the data
            // We need to synchronize with server to emit event with proper data
           boolean waitingForInitialSnapshot = false;
           for(ListenerData<? extends Reference, ? extends Snapshot> listenerData : listeners) {
               if (!listenerData.isInitialRemoteSnapshotLoaded()) {
                   waitingForInitialSnapshot = true;
                   if (!listenerData.beginInitialRemoteSnapshotLoad()) {
                       continue;
                   }
                   listenerData.getReference().get(Source.SERVER, true).addOnCompleteListener(new OnCompleteListener<Snapshot>() {
                       @Override
                       public void onComplete(Task<Snapshot> task) {
                           if (!listenerData.isActive()) return;
                           if (task.isSuccessful()) {
                               // Emitting event directly
                               listenerData.executeListener(task.getResult(),
                                   null);
                               listenerData.markInitialRemoteSnapshotLoaded();
                               processQueue(queryId);
                           } else {
                               listenerData.clearInitialRemoteSnapshotLoad();
                               listenerData.executeListener(null, new FusabaseOracledbException(
                                       "Cannot fetch the current snapshot for the data at "
                                               + listenerData.getReference().getPath()
                                               + ". Failed to register onSnapshot listener.",
                                       FusabaseOracledbException.Code.INTERNAL));
                           }
                       }
                   });
               }
           }
           // The initial server read is asynchronous. Preserve subsequent frames until it has
           // established the authoritative baseline for this query.
           return !waitingForInitialSnapshot;
        }
        else if (messageObject.containsKey("error"))
        {
            String errorMessage = messageObject.getString("error");
            FusabaseLogger.e(TAG, "Error received from ORDS realtime channel");
            for(ListenerData<? extends Reference, ? extends Snapshot> listenerData : listeners) {
                listenerData.executeListener(null, new FusabaseOracledbException(errorMessage, FusabaseOracledbException.Code.INTERNAL));
            }
        }
        else {
            for (ListenerData<? extends Reference, ? extends Snapshot> listenerData : listeners) {
                // We need to ensure that the first snapshot is exists before any notification is processed
                if (!listenerData.isInitialRemoteSnapshotLoaded()) {
                    return false;
                }

                if (!messageObject.containsKey("rowId") ||
                    !messageObject.containsKey("changedData")) {
                    listenerData.executeListener(null,
                        new FusabaseOracledbException("Invalid response received from BAAS Server", FusabaseOracledbException.Code.INTERNAL));
                    continue;
                }

                try {
                    Snapshot result = listenerData.getReference().getSnapshotFromNotificationData(messageObject, listenerData.getPreviousResult());
                    if (result != null) {
                        listenerData.executeListener(result,
                            null);
                    }
                    // Ignore null, Originates from STALE notification
                } catch (Exception e) {
                    listenerData.executeListener(null,
                        new FusabaseOracledbException(e.getMessage(), FusabaseOracledbException.Code.INTERNAL));
                }
            }
        }
        return true;
    }

    private static String createConnectionUrl(FusabaseOracledb db) throws Exception {
        String authPath = "idcs".equals(db.getOracledbSettings().getOptions().getAuthType())
                ? EndpointCatalog.IDCS : EndpointCatalog.ONPREM;
        SnapshotAuthorizationResponse credential = db.componentGraph()
                .getSnapshotAuthorizationService().authorize(authPath);
        URI host = new URI(db.getOracledbSettings().getOptions().getOrdsHost());
        String scheme = "https".equalsIgnoreCase(host.getScheme()) ? "wss" : "ws";
        String schema = host.getPath().replaceFirst("^/ords/([^/]+)/?$", "$1");
        return new URI(scheme, null, host.getHost(), host.getPort(),
                EndpointCatalog.REALTIME + schema, "authToken=" + credential.getAccessToken(), null).toString();
    }

    /**
     * Establishes a WebSocket connection for real-time updates.
     * Retrieves authentication token and connects to the ORDS real-time service.
     */
    protected void establishWebSocketConnection () {
        final long generation;
        final AuthSession.State session;
        synchronized (this) {
            session = authSession.current();
            if (disposed || session.userId == null || registeredQuery.isEmpty()
                    || authorizingSession == session) return;
            authorizingSession = session;
            generation = ++currentConnectionGeneration;
        }
        try {
            String websocketURL = connectionUrl.call();
            synchronized (this) {
                if (disposed || !authSession.isCurrent(session) || generation != currentConnectionGeneration
                        || registeredQuery.isEmpty()) return;
                webSocketTransport.establishConnection(websocketURL,
                        new GenerationCallback(generation, session, webSocketCallback));
            }
        } catch (Exception e) {
            if (!authSession.isCurrent(session)) return;
            FusabaseLogger.e(TAG, "Snapshot authorization failed while opening WebSocket");
            statePublisher.publish(RealtimeConnectionState.FAILED,
                    retryAttempt, RealtimeConnectionReason.TOKEN_FAILURE);
            scheduleRetryIfNeeded();
        } finally {
            synchronized (this) {
                if (generation == currentConnectionGeneration) authorizingSession = null;
            }
        }

        FusabaseLogger.d(TAG, "Created realtime WebSocket URI without logging credentials");
    }

    private void executeRecovery(Runnable action) {
        if (disposed) return;
        try { recoveryExecutor.execute(action); }
        catch (java.util.concurrent.RejectedExecutionException failure) {
            if (!disposed) throw failure;
        }
    }

    static void release(FusabaseOracledb database) {
        WebSocketController controller = INSTANCES.remove(database);
        if (controller != null) controller.shutdown();
    }

    synchronized void shutdown() {
        if (disposed) return;
        disposed = true;
        authSubscription.remove();
        ++currentConnectionGeneration;
        authorizingSession = null;
        webSocketTransport.cancel();
        cancelRetry();
        java.util.List<ListenerData<? extends Reference, ? extends Snapshot>> listeners = new java.util.ArrayList<>();
        for (List<ListenerData<? extends Reference, ? extends Snapshot>> group : registeredQuery.values()) {
            listeners.addAll(group);
        }
        registeredQuery.clear();
        registrationPayloads.clear();
        messageQueue.clear();
        messageQueueBytes = 0;
        for (ListenerData<? extends Reference, ? extends Snapshot> listener : listeners) {
            listener.getListenerRegistration().remove();
        }
        try { connectivityMonitor.stop(); }
        finally { recoveryExecutor.shutdownNow(); }
    }

    private void onConnectivityChanged(boolean validated) {
        executeRecovery(() -> {
            synchronized (WebSocketController.this) {
                if (disposed || validatedOnline == validated) {
                    return;
                }
                validatedOnline = validated;
                if (!validated) {
                    cancelRetry();
                    statePublisher.publish(RealtimeConnectionState.DISCONNECTED,
                            retryAttempt, RealtimeConnectionReason.NETWORK_UNAVAILABLE);
                    if (!registeredQuery.isEmpty()) {
                        FusabaseLogger.d(TAG, "Validated connectivity lost; pausing WebSocket recovery");
                    }
                    return;
                }
                if (!registeredQuery.isEmpty()) {
                    cancelRetry();
                    statePublisher.publish(RealtimeConnectionState.CONNECTING,
                            retryAttempt, RealtimeConnectionReason.NONE);
                    retryFuture = recoveryExecutor.schedule(this::establishWebSocketConnection,
                            0, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    private void scheduleRetryIfNeeded() {
        AuthSession.State failedSession = authSession.current();
        executeRecovery(() -> {
            synchronized (WebSocketController.this) {
                if (disposed || !authSession.isCurrent(failedSession) || failedSession.userId == null || !validatedOnline || registeredQuery.isEmpty() || retryFuture != null) {
                    return;
                }
                retryAttempt++;
                long cap = Math.min(30_000L, 1_000L << Math.min(retryAttempt - 1, 4));
                long delay = ThreadLocalRandom.current().nextLong(Math.max(1L, cap / 2), cap + 1);
                AuthSession.State scheduledSession = authSession.current();
                retryFuture = recoveryExecutor.schedule(() -> {
                    synchronized (WebSocketController.this) {
                        if (!authSession.isCurrent(scheduledSession)) return;
                        retryFuture = null;
                    }
                    establishWebSocketConnection();
                }, delay, TimeUnit.MILLISECONDS);
            }
        });
    }

    private synchronized void cancelRetry() {
        if (retryFuture != null) {
            retryFuture.cancel(false);
            retryFuture = null;
        }
    }

    /** Drops callbacks delivered by a transport instance superseded by a newer connection. */
    private final class GenerationCallback implements WebSocketCallback {
        private final long generation;
        private final WebSocketCallback delegate;
        private final AuthSession.State session;

        private GenerationCallback(long generation, AuthSession.State session, WebSocketCallback delegate) {
            this.session = session;
            this.generation = generation;
            this.delegate = delegate;
        }

        private boolean isCurrent() {
            return !disposed && generation == currentConnectionGeneration && authSession.isCurrent(session);
        }

        @Override public void onMessage(@NonNull String message) {
            synchronized (WebSocketController.this) {
                if (isCurrent()) delegate.onMessage(message);
            }
        }

        @Override public void onOpen() {
            synchronized (WebSocketController.this) {
                if (isCurrent()) delegate.onOpen();
            }
        }

        @Override public void onClosed() {
            synchronized (WebSocketController.this) {
                if (isCurrent()) delegate.onClosed();
            }
        }

        @Override public void onClosing() {
            synchronized (WebSocketController.this) {
                if (isCurrent()) delegate.onClosing();
            }
        }

        @Override public void onFailure() {
            synchronized (WebSocketController.this) {
                if (isCurrent()) delegate.onFailure();
            }
        }
    }

    /** Closes the WebSocket connection. */
    protected synchronized void closeWebSocketConnection() {
        ++currentConnectionGeneration;
        authorizingSession = null;
        cancelRetry();
        webSocketTransport.close();
    }

    /**
     * Processes queued messages for a specific query.
     *
     * @param queryId the query ID to process messages for
     */
    protected synchronized void processQueue (int queryId) {
        while (true) {
            JsonObject message = pollMessage(queryId);
            if (message == null) {
                return;
            }
            if (!processMessage(message)) {
                enqueueMessage(message);
                return;
            }
        }
    }

    private synchronized boolean enqueueMessage(@NonNull JsonObject message) {
        int messageBytes = message.toString().getBytes(StandardCharsets.UTF_8).length;
        if (messageBytes > MAX_REALTIME_MESSAGE_QUEUE_BYTES
                || messageQueue.size() >= MAX_REALTIME_MESSAGE_QUEUE_COUNT
                || messageQueueBytes + messageBytes > MAX_REALTIME_MESSAGE_QUEUE_BYTES) {
            int queryId = message.containsKey("queryId") ? message.getInt("queryId") : -1;
            FusabaseLogger.e(TAG, "Realtime initial snapshot event queue limit exceeded");
            failQueuedQuery(queryId);
            return false;
        }
        messageQueue.add(message);
        messageQueueBytes += messageBytes;
        return true;
    }

    private synchronized JsonObject pollMessage(int queryId) {
        Iterator<JsonObject> iterator = messageQueue.iterator();
        while (iterator.hasNext()) {
            JsonObject message = iterator.next();
            if (message.containsKey("queryId")
                    && message.get("queryId").getValueType().equals(JsonValue.ValueType.NUMBER)
                    && message.getInt("queryId") == queryId) {
                iterator.remove();
                messageQueueBytes -= message.toString().getBytes(StandardCharsets.UTF_8).length;
                return message;
            }
        }
        return null;
    }

    private void failQueuedQuery(int queryId) {
        List<ListenerData<? extends Reference, ? extends Snapshot>> listeners = registeredQuery.get(queryId);
        if (listeners == null) {
            return;
        }
        FusabaseOracledbException error = new FusabaseOracledbException(
                "Realtime initial snapshot event queue limit exceeded.",
                FusabaseOracledbException.Code.INTERNAL);
        for (ListenerData<? extends Reference, ? extends Snapshot> listenerData : listeners) {
            listenerData.executeListener(null, error);
        }
    }

    /**
     * Returns the internal WebSocket transport.
     *
     * @return the transport used by the controller
     */
    protected WebSocketTransport getWebSocketTransport() {
        return webSocketTransport;
    }

    /** Returns the current callback for deterministic package-private transport tests. */
    WebSocketCallback getWebSocketCallbackForTesting() {
        return webSocketCallback;
    }
}
