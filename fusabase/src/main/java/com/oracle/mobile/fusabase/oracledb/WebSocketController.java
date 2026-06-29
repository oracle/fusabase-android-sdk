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

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.http.WebSocketCallback;
import com.oracle.mobile.fusabase.http.WebSocketClient;
import com.oracle.mobile.fusabase.utils.Utils;
import com.oracle.mobile.fusabase.task.OnCompleteListener;
import com.oracle.mobile.fusabase.task.Task;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * Manages WebSocket connections for real-time data synchronization.
 * Handles listener registration, message processing, and connection lifecycle
 * for snapshot listeners that require real-time updates.
 */
class WebSocketController {

    /** Log tag for debugging. */
    private static final String TAG = "FusabaseOradb";

    /** Singleton instance of the WebSocket controller. */
    private static WebSocketController instance;

    /** ORDS real-time service endpoint path. */
    private static final String ORDS_REALTIME = "/ords/baas-realtime/";

    /** WebSocket client for managing the connection. */
    private final WebSocketClient webSocketClient;

    /** Oradb instance this controller belongs to. */
    private final FusabaseOracledb oracledb;

    /** Queue for incoming WebSocket messages. */
    private final Queue<JsonObject> messageQueue;

    /** Map of query IDs to their registered listeners. */
    private final HashMap<Integer, List<ListenerData<? extends Reference, ? extends Snapshot>>> registeredQuery;

    /** Flag indicating if WebSocket connection is currently being established. */
    private boolean webSocketConnectionInProgress;

    /**
     * Registers a query listener with the WebSocket controller.
     *
     * @param queryId the ID of the query to register
     * @param listenerData the listener data to register
     */
    protected void registerQueryWithController (Integer queryId, @NonNull ListenerData<? extends Reference, ? extends Snapshot> listenerData){

        List<ListenerData<? extends Reference, ? extends Snapshot>>  listenerDataList = this.registeredQuery.getOrDefault(queryId,  new ArrayList<>());
        listenerDataList.add(listenerData);
        this.registeredQuery.put(queryId, listenerDataList);
    }

    /**
     * Deregisters a query listener from the WebSocket controller.
     *
     * @param queryId the ID of the query to deregister
     * @param listenerData the listener data to remove
     */
    protected void deregisterQuery (Integer queryId, @NonNull ListenerData<? extends Reference, ? extends Snapshot> listenerData){

        List<ListenerData<? extends Reference, ? extends Snapshot>>  listenerDataList = this.registeredQuery.get(queryId);

        if(listenerDataList == null)
            return;
        listenerDataList.remove(listenerData);

        if(listenerDataList.isEmpty())
            this.registeredQuery.remove(queryId);
        else
            this.registeredQuery.put(queryId, listenerDataList);
    }

    /**
     * Checks if a query should be deregistered based on listener count.
     *
     * @param queryId the query ID to check
     * @return true if the query should be deregistered, false otherwise
     */
    protected boolean shouldDeregisterQuery (Integer queryId) {
        return !this.registeredQuery.containsKey(queryId);
    }

    /**
     * Checks if the WebSocket connection should be closed.
     *
     * @return true if no queries are registered, false otherwise
     */
    protected boolean shouldCloseConnection () {
        return this.registeredQuery.isEmpty();
    }

    /**
     * Checks if a query should be registered.
     *
     * @param queryId the query ID to check
     * @return true if the query should be registered, false otherwise
     */
    protected boolean shouldRegisterQuery (Integer queryId) {
        return !this.registeredQuery.containsKey(queryId);
    }

    /**
     * Adds a registration success message to the queue.
     *
     * @param queryId the query ID that was successfully registered
     */
    protected void putRegistrationSuccessMessage (Integer queryId) {
        JsonObject messageObject = Json.createObjectBuilder()
            .add("queryId", queryId)
            .add("success", 1)
            .build();
        this.messageQueue.add(messageObject);
        processQueue(queryId);
    }

    /**
     * Gets the singleton instance of WebSocketController.
     *
     * @param oracledb the Oradb instance
     * @return the WebSocketController instance
     */
    protected static WebSocketController getInstance(@NonNull FusabaseOracledb oracledb) {
        if(instance == null)
        {
            instance = new WebSocketController(oracledb);
        }
        return instance;
    }

    /**
     * Private constructor for creating WebSocketController instances.
     *
     * @param oracledb the Oradb instance this controller belongs to
     */
    private WebSocketController(@NonNull FusabaseOracledb oracledb) {
        this.oracledb = oracledb;
        this.webSocketConnectionInProgress = false;
        this.messageQueue = new LinkedList<>();
        // Perform DB cleanups here
        WebSocketCallback webSocketCallback = new WebSocketCallback() {
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
                    FusabaseLogger.e(TAG, "Error encountered while parsing message " + e.getMessage());
                    FusabaseLogger.d(TAG, "Ignoring received message.");
                    return;
                }

                messageQueue.add(messageObject);
                processQueue(messageObject.getInt("queryId"));
            }

            /** Called when the WebSocket connection is opened. */
            @Override
            public void onOpen() {
                FusabaseLogger.d(TAG, "Connection with ords-baas-server established.");
            }

            /** Called when the WebSocket connection is closed. */
            @Override
            public void onClosed() {
                FusabaseLogger.d(TAG, "Connection with ords-baas-server closed.");
                // Perform DB cleanups here
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
            }
        };
        this.webSocketClient = new WebSocketClient(webSocketCallback);
        this.registeredQuery = new HashMap<>();
    }

    /**
     * Processes a WebSocket message and notifies appropriate listeners.
     *
     * @param messageObject the JSON message to process
     */
    private void processMessage (@NonNull JsonObject messageObject) {

        int queryId = messageObject.getInt("queryId");
        List<ListenerData<? extends Reference, ? extends Snapshot>> listeners = this.registeredQuery.get(queryId);

        if(listeners == null || listeners.isEmpty())
        {
            FusabaseLogger.w(TAG, "Unknown notification received which doesn't belong to any query.");
            return;
        }

        if(messageObject.containsKey("success"))
        {
            // Once the connection is successful then only emit the first event
            // We need to queue up all the changes before we have a first snapshot of the data
            // We need to synchronize with server to emit event with proper data
           for(ListenerData<? extends Reference, ? extends Snapshot> listenerData : listeners) {
               if (listenerData.getPreviousResult() == null) {
                   listenerData.getReference().get(Source.SERVER, true).addOnCompleteListener(new OnCompleteListener<Snapshot>() {
                       @Override
                       public void onComplete(Task<Snapshot> task) {
                           if (task.isSuccessful()) {
                               // Emitting event directly
                               listenerData.executeListener(task.getResult(),
                                   null);
                               processQueue(queryId);
                           } else {
                               throw new RuntimeException("Cannot fetch the current snapshot for the " +
                                   "data at " + listenerData.getReference().getPath() + ". Failed to" +
                                   "register onSnapshot listener.");
                           }
                       }
                   });
               }
           }
        }
        else if (messageObject.containsKey("error"))
        {
            String errorMessage = messageObject.getString("error");
            FusabaseLogger.e(TAG, "Error received from ORDS" + errorMessage);
            for(ListenerData<? extends Reference, ? extends Snapshot> listenerData : listeners) {
                listenerData.executeListener(null, new FusabaseOracledbException(errorMessage, FusabaseOracledbException.Code.INTERNAL));
            }
        }
        else {

            boolean isQueued = false;
            for (ListenerData<? extends Reference, ? extends Snapshot> listenerData : listeners) {
                // We need to ensure that the first snapshot is exists before any notification is processed
                if (listenerData.getPreviousResult() == null) {
                    if(!isQueued)
                    {
                        messageQueue.add(messageObject);
                        isQueued = true;
                    }
                    continue;
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

    }

    /**
     * Builds the Authorization header with access token.
     * Decrypts token inline to minimize memory exposure.
     *
     * @return Complete Authorization header string or empty string if unavailable
     * @throws FusabaseOracledbException if token retrieval fails
     */
    @NonNull
    private String buildAuthorizationHeader() throws FusabaseOracledbException {

        try {
            String key = Utils.getPreferenceData(this.oracledb.getApp().getApplicationContext(),
                    "LOGGED_IN_USER",
                    "FusabasePreferences");
            if (key != null && !key.isEmpty()) {
                // Load the user details JSON object and extract the access token
                JsonObject userDetails = Utils.loadJsonObjectFromPreferences(
                        this.oracledb.getApp().getApplicationContext(),
                        key,
                        "FusabasePreferences");
                if (userDetails.containsKey("accessToken")) {
                    return "Bearer " + userDetails.getString("accessToken");
                }
            }
        } catch (Exception e) {
            FusabaseLogger.w("QueryHelper", "Cannot get access token from current user: " + e.getMessage());
        }

        FusabaseLogger.w("QueryHelper", "No access token available");
        return "";
    }

    private JsonObject getSnapshotTokenForAuthPath(@NonNull String authPath) throws FusabaseOracledbException {
        HttpRequestHelper requestHelper = new HttpRequestHelper(oracledb.getOracledbSettings().getMaxAttempt());

        // Populate headers
        Map<String, String> headers = new HashMap<>();

        String authHeader = this.buildAuthorizationHeader();
        if (authHeader.isEmpty()) {
            throw new FusabaseOracledbException("User Not logged in", FusabaseOracledbException.Code.UNAUTHENTICATED);
        }

        headers.put("Authorization", authHeader);

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.oracledb.getOracledbSettings().getOptions().getAppId());

        try {
            // Create the request
            requestHelper.createHttpRequest(Utils.urlBuilder(this.oracledb.getOracledbSettings().getOptions().getOrdsHost(),
                    QueryHelper.BAAS_SERVICE,
                    QueryHelper.IDM,
                    authPath,
                    this.oracledb.getOracledbSettings().getOptions().getProjectId(),
                    QueryHelper.AUTHORIZE_SNAPSHOT),
                "GET",
                headers,
                queryParameters);
        } catch (FusabaseException e) {
            throw new FusabaseOracledbException(e.getMessage(), FusabaseOracledbException.Code.INTERNAL);
        }

        HttpResponse response;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            throw new FusabaseOracledbException(e.getMessage(), FusabaseOracledbException.Code.NETWORK_ERROR, e);
        }

        JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
        JsonObject result = reader.readObject();

        if (response.getStatus())
            FusabaseLogger.i("FusabaseOradb", "authorizeSnapshot for " + authPath);
        else {
            FusabaseLogger.w("FusabaseOradb", "authorizeSnapshot for " + authPath + " failed with response message " + response.getError());
            throw new FusabaseOracledbException("Authorize Snapshot failed with following message " + response.getError(),
                FusabaseOracledbException.Code.fromCode(response.getCode()));
        }

        reader.close();
        return result;
    }

    private JsonObject getBaseSnapshotToken() throws FusabaseOracledbException {
        return getSnapshotTokenForAuthPath(QueryHelper.ONPREM);
    }

    private JsonObject getIDCSSnapshotToken() throws FusabaseOracledbException {
        return getSnapshotTokenForAuthPath(QueryHelper.IDCS);
    }

    /**
     * Retrieves the snapshot token required for WebSocket authentication.
     *
     * @return JSON object containing the snapshot token
     * @throws FusabaseOracledbException if the token cannot be retrieved
     */
    private JsonObject getSnapshotToken() throws FusabaseOracledbException {
        if (this.oracledb.getOracledbSettings().getOptions().getAuthType().equals("idcs")) {
            return this.getIDCSSnapshotToken();
        }
        return this.getBaseSnapshotToken();
    }

    /**
     * Establishes a WebSocket connection for real-time updates.
     * Retrieves authentication token and connects to the ORDS real-time service.
     */
    protected void establishWebSocketConnection () {

        if(this.webSocketConnectionInProgress)
        {
            return;
        } else
            this.webSocketConnectionInProgress = true;

        // Fetch SNAPSHOT_TOKEN first
        JsonObject snapshotCredential;
        try {
            snapshotCredential = this.getSnapshotToken();
        } catch (FusabaseOracledbException e) {
            FusabaseLogger.d(TAG, "Exception encountered while processing request for" +
                    " authorize Snapshot." + e.getMessage());
            throw new RuntimeException("Exception encountered while processing request for" +
                    " authorize Snapshot." + e.getMessage());
        }

        URI ordsHost;
        URI websocketURL;

        try {
            ordsHost = new URI(this.oracledb.getOracledbSettings().getOptions().getOrdsHost());
            String scheme = ordsHost.getScheme().equalsIgnoreCase("https") ? "wss" : "ws";
            String schema = ordsHost.getPath().replaceFirst("^/ords/([^/]+)/?$", "$1");
            websocketURL = new URI(scheme,
                    null,
                    ordsHost.getHost(),
                    ordsHost.getPort(),
                    ORDS_REALTIME + schema,
                    "authToken=" + snapshotCredential.getString("access_token"),
                    null);
        } catch (Exception e) {
            FusabaseLogger.d(TAG, "Exception encountered while processing ordsHost for" +
                    " establishing websocket connection. " + e.getMessage());
            this.webSocketConnectionInProgress = false;
            throw new RuntimeException("Exception encountered while processing ordsHost for" +
                    " establishing websocket connection. " + e.getMessage());
        }

        FusabaseLogger.d(TAG, "Created WS URI " + websocketURL.toString());
        webSocketClient.establishConnection(websocketURL.toString());
        FusabaseLogger.d(TAG, "Websocket connection established");
        this.webSocketConnectionInProgress = false;
    }

    /** Closes the WebSocket connection. */
    protected void closeWebSocketConnection() {
        webSocketClient.close();
    }

    /**
     * Processes queued messages for a specific query.
     *
     * @param queryId the query ID to process messages for
     */
    protected void processQueue (int queryId) {

        Iterator<JsonObject> iterator = messageQueue.iterator();
        while (iterator.hasNext()) {
            JsonObject message = iterator.next();

            if (message.getInt("queryId") == queryId) {
                processMessage(message);
                iterator.remove();
            }
        }
    }

    /**
     * Returns the WebSocket client instance.
     *
     * @return the WebSocket client
     */
    public WebSocketClient getWebSocketClient() {
        return webSocketClient;
    }
}
