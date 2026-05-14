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

package com.oracle.mobile.fusabase.http;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClient {

    private final static String TAG = "fusabase";
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
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .pingInterval(7, TimeUnit.MINUTES)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));

        // Check global setting for self-signed certificates
        if (FusabaseApp.allowsSelfSignedCertificates()) {
            configureSelfSignedCertificates(builder);
        }

        this.client = builder.build();
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
                FusabaseLogger.d(TAG, "Connected to WebSocket server!");
                webSocketCallback.onOpen();
            }

            @Override
            public void onMessage(WebSocket webSocket, String message) {
                // WebSocket messages may contain sensitive payloads; never log contents.
                FusabaseLogger.d(TAG, "Message received from WebSocket (len=" + (message == null ? 0 : message.length()) + ")");
                webSocketCallback.onMessage(message);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                FusabaseLogger.d(TAG, "Closing connection to WebSocket " + reason);
                webSocketCallback.onClosing();
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                FusabaseLogger.d(TAG, "Closed connection to WebSocket " + reason);
                webSocketCallback.onClosed();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                FusabaseLogger.d(TAG, "Failure occureed in connection with WebSocket " + t.getMessage());
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
        // WebSocket messages may contain sensitive payloads; never log contents.
        FusabaseLogger.d(TAG, "Sending message through WebSocket (len=" + (message == null ? 0 : message.length()) + ")");
        webSocket.send(message);
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Client closed connection");
        }
    }

    /**
     * Configures the OkHttpClient to accept self-signed certificates.
     * WARNING: This reduces security and should only be used for testing.
     */
    private void configureSelfSignedCertificates(OkHttpClient.Builder builder) {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            // Accept all client certificates
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                            // Accept all server certificates
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an all-trusting host name verifier
            final HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                   .hostnameVerifier(allHostsValid);

            FusabaseLogger.w(TAG, "WARNING: WebSocketClient configured to accept self-signed certificates. " +
                    "This reduces security and should only be used for testing.");

        } catch (Exception e) {
            FusabaseLogger.e(TAG, "Failed to configure self-signed certificate support", e);
            throw new RuntimeException("Failed to configure SSL for self-signed certificates", e);
        }
    }

}
