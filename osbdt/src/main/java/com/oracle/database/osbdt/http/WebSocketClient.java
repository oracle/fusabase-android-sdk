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

package com.oracle.database.osbdt.http;

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.logger.OsbdtLogger;

import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.util.Collections;

public class WebSocketClient {

    private final static String TAG = "osbdt";
    private final static int TIMEOUT = 4*60*1000;
    private final OkHttpClient client;
    private WebSocket webSocket;
    private State state;
    private final WebSocketCallback webSocketCallback;

    public enum State {
        CONNECTING,
        OPEN,
        CLOSING,
        CLOSED,
        FAILED
    }

    public WebSocketClient(@NonNull WebSocketCallback webSocketCallback) {
        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                .build();
        this.client = new OkHttpClient.Builder()
                .pingInterval(7, TimeUnit.MINUTES)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .connectionSpecs(Collections.singletonList(spec))
                .build();
        this.webSocketCallback = webSocketCallback;
        this.state = State.CLOSED;
    }

    @NonNull
    public Request createRequest(@NonNull String url) {
        return new Request.Builder()
                .url(url)
                .build();
    }

    // Check for error connection with the server
    @NonNull
    public WebSocket createWebSocketConnection(Request request) {

        this.state = State.CONNECTING;

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                OsbdtLogger.d(TAG, "Connected to WebSocket server!");
                webSocketCallback.onOpen();
            }

            @Override
            public void onMessage(WebSocket webSocket, String message) {
                OsbdtLogger.d(TAG, "Message received to WebSocket " + message);
                webSocketCallback.onMessage(message);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                OsbdtLogger.d(TAG, "Closing connection to WebSocket " + reason);
                webSocketCallback.onClosing();
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                OsbdtLogger.d(TAG, "Closed connection to WebSocket " + reason);
                webSocketCallback.onClosed();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                OsbdtLogger.d(TAG, "Failure occureed in connection with WebSocket " + t.getMessage());
                webSocketCallback.onFailure();
            }
        });

        this.state = State.OPEN;
        return webSocket;
    }

    public State getState () {
        return this.state;
    }

    public WebSocket establishConnection (@NonNull String URL) {
        Request request = createRequest(URL);
        return createWebSocketConnection(request);
    }

    public void sendMessage(String message) {
        OsbdtLogger.d(TAG, "Sending message through webSocket " + message);
        webSocket.send(message);
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Client closed connection");
        }
    }

}
