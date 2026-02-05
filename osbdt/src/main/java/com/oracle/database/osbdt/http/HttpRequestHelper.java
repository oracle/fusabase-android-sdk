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
////-----------------------------------------------------------------------------

package com.oracle.database.osbdt.http;

import android.util.Log;

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtException;
import com.oracle.database.osbdt.logger.OsbdtLogger;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.TlsVersion;
import okio.Buffer;
import okio.BufferedSource;

import java.util.Collections;

public class HttpRequestHelper {

    private final static String TAG = "OsbdtHTTP";

    static class ExponentialBackoffInterceptor implements Interceptor {
        private final int maxRetries;
        private final long delayMillis;

        public ExponentialBackoffInterceptor(int maxRetries, long delayMillis) {
            this.maxRetries = maxRetries;
            this.delayMillis = delayMillis;
        }

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            int attempt = 0;

            while (attempt < this.maxRetries) {
                try {
                    response = chain.proceed(request);

                    if (response.isSuccessful() || response.code() < 500) {
                        return response;
                    }

                    if (attempt + 1 < maxRetries)
                        Log.d("osbdtHttp", "Request failed, retrying...");

                } catch (IOException e) {
                    if (attempt >= maxRetries - 1) {
                        throw e;
                    }
                }

                attempt++;

                long backoff = delayMillis * (long) Math.pow(2, attempt); // Exponential backoff

                try {
                    if (attempt < this.maxRetries) {
                        Log.d("ExponentialBackoff", "Retry attempt " + attempt + " after " + backoff + " ms");
                        TimeUnit.MILLISECONDS.sleep(backoff);
                        if(response != null) {
                            response.close();
                        }
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            if (response == null) {
                throw new IOException("Failed after " + this.maxRetries + " retries");
            }
            return response;
        }
    }

    private final OkHttpClient httpClient;
    private Request request = null;
    private int retries;
    private String requestBodyForLogging = null;

    public HttpRequestHelper() {
        this(1);
    }

    public HttpRequestHelper(int retries) {
        this.retries = retries;
        this.httpClient = createHttpClient(retries);
    }

    /**
     * Creates an OkHttpClient configured based on the global self-signed certificate setting.
     */
    private OkHttpClient createHttpClient(int retries) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(new ExponentialBackoffInterceptor(retries, 100));

        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                .build();
        builder.connectionSpecs(Collections.singletonList(spec));

        // Check global setting for self-signed certificates
        if (OsbdtApp.allowsSelfSignedCertificates()) {
            configureSelfSignedCertificates(builder);
        }

        return builder.build();
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

            OsbdtLogger.w(TAG, "WARNING: HttpRequestHelper configured to accept self-signed certificates. " +
                    "This reduces security and should only be used for testing.");

        } catch (Exception e) {
            OsbdtLogger.e(TAG, "Failed to configure self-signed certificate support", e);
            throw new RuntimeException("Failed to configure SSL for self-signed certificates", e);
        }
    }

    /* Create request with url and body */
    @NonNull
    public HttpRequestHelper createHttpRequest(@NonNull String url,
                                               @NonNull String method,
                                               @NonNull String body) throws OsbdtException {
        this.request = buildRequest(url,
                method,
                null,
                null,
                body);

        return this;
    }

    /* Create request with header and body */
    @NonNull
    public HttpRequestHelper createHttpRequest(@NonNull String url,
                                               @NonNull String method,
                                               @NonNull Map<String, String> headers,
                                               @NonNull String body) {
        this.request = this.buildRequest(url,
                method,
                headers,
                null,
                body);
        return this;
    }

    /* Create request with header , queryParameter and body */
    @NonNull
    public HttpRequestHelper createHttpRequest(@NonNull String url,
                                               @NonNull String method,
                                               @NonNull Map<String, String> headers,
                                               @NonNull Map<String, String> queryParameter,
                                               @NonNull String body) throws OsbdtException {
        this.request = buildRequest(url,
                method,
                headers,
                queryParameter,
                body);

        return this;
    }

    @NonNull
    public HttpRequestHelper createHttpRequest(@NonNull String url,
                                               @NonNull String method,
                                               @NonNull Map<String, String> headers,
                                               @NonNull Map<String, String> queryParameter,
                                               @NonNull RequestBody body) throws OsbdtException {
        this.request = buildRequest(url, method, headers, queryParameter, body);
        return this;
    }

    /* Create request with header and null body */
    @NonNull
    public HttpRequestHelper createHttpRequest(@NonNull String url,
                                               @NonNull String method,
                                               @NonNull Map<String, String> headers) {
        this.request = buildRequest(url,
                method,
                headers,
                null,
                null);
        return this;
    }

    /* Create request with header , queryParameter and null body */
    @NonNull
    public HttpRequestHelper createHttpRequest(@NonNull String url,
                                               @NonNull String method,
                                               @NonNull Map<String, String> headers,
                                               @NonNull Map<String, String> queryParameter) throws OsbdtException {
        this.request = buildRequest(url,
                method,
                headers,
                queryParameter,
                null);
        return this;
    }

    @NonNull
    private Request buildRequest(@NonNull String url,
                                 String method,
                                 Map<String, String> headers,
                                 Map<String, String> queryParameters,
                                 Object body) {
        HttpUrl.Builder httpUrlBuilder = HttpUrl.parse(url).newBuilder();

        if (queryParameters != null) {
            for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
                httpUrlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        Request.Builder requestBuilder = new Request.Builder().url(httpUrlBuilder.build());

        try {
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.header(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e)
        {
            OsbdtLogger.d("HTTPHelper", e.getMessage());
        }

        switch (method.toUpperCase()) {
            case "GET":
                if (body != null)
                    OsbdtLogger.w(TAG, "Invalid method for network call received.");
                requestBuilder.get();
                break;
            case "POST":
            case "PUT":
            case "PATCH":
                RequestBody requestBody = body instanceof RequestBody ? (RequestBody) body : body != null ?
                        RequestBody.create(((String)body).getBytes(),
                                MediaType.parse(headers != null ?
                                        headers.getOrDefault("Content-Type", "application/json; charset=utf-8")
                                        : "application/json; charset=utf-8"
                                )) :
                        RequestBody.create(null, new byte[0]);
                // Store body for logging if it's a string
                if (body instanceof String) {
                    this.requestBodyForLogging = (String) body;
                } else {
                    this.requestBodyForLogging = null;
                }
                if ("POST".equalsIgnoreCase(method)) {
                    requestBuilder.post(requestBody);
                } else if("PUT".equalsIgnoreCase(method)) {
                    requestBuilder.put(requestBody);
                } else {
                    requestBuilder.patch(requestBody);
                }
                break;
            case "DELETE":
                requestBuilder.delete();
                break;
            default:
                // Not throwing an exception just logging the error.
                OsbdtLogger.e(TAG, "Unknown Operation for network call encountered.");
        }

        return requestBuilder.build();
    }

    /**
     * Logs all details of the HTTP request for debugging purposes.
     */
    private void logHttpRequest() {
        if (request == null) return;

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("HTTP Request Details:\n");
        logMessage.append("Method: ").append(request.method()).append("\n");
        logMessage.append("URL: ").append(request.url().toString()).append("\n");

        // Log headers
        if (request.headers().size() > 0) {
            logMessage.append("Headers:\n");
            for (String name : request.headers().names()) {
                logMessage.append("  ").append(name).append(": ").append(request.headers().get(name)).append("\n");
            }
        } else {
            logMessage.append("Headers: None\n");
        }

        // Log body if available
        if (requestBodyForLogging != null && !requestBodyForLogging.isEmpty()) {
            logMessage.append("Body: ").append(requestBodyForLogging);
        } else if (request.body() != null) {
            logMessage.append("Body: Present (binary or custom RequestBody)");
        } else {
            logMessage.append("Body: None");
        }

        OsbdtLogger.d(TAG, logMessage.toString());
    }

    public HttpResponse executeRequest() throws OsbdtException {
        try {

            if (request == null) {
                throw new OsbdtException("Request is not initialized." +
                        " Kindly create a request first using buildRequest().");
            }

            // Log HTTP request details
            logHttpRequest();

            Response response = this.httpClient.newCall(this.request).execute();

            if (!response.isSuccessful()) {
                String responseMessage = response.message();
                int responseCode = response.code();
                Log.d("osbdtHttp", "Request failed, with code " + response.code() +
                        " with message " + responseMessage + " following response received " +
                        response.body().string());
                response.close();
                return new HttpResponse(false, null, null,
                        "Unexpected code " + response, responseCode);
            }

            BufferedSource source = response.body().source();
            Buffer buffer = new Buffer();
            source.readAll(buffer);
            byte[] responseBytes = buffer.readByteArray();

            // Create a new ResponseBody with the copied bytes
            ResponseBody responseBody = ResponseBody.Companion.create(responseBytes,
                    response.body().contentType());
            response.close();

            return new HttpResponse(true,
                    responseBody,
                    responseBytes,
                    null,
                    response.code());

        } catch (IOException e) {
            return new HttpResponse(false,
                    null,
                    null,
                    e.getMessage(),
                    1000);
        }
    }

    public HttpResponse executeRequestStreaming() throws OsbdtException {
        try {

            if (request == null) {
                throw new OsbdtException("Request is not initialized." +
                        " Kindly create a request first using buildRequest().");
            }

            // Log HTTP request details
            logHttpRequest();

            Response response = this.httpClient.newCall(this.request).execute();

            if (!response.isSuccessful()) {
                String responseMessage = response.message();
                int responseCode = response.code();
                Log.d("osbdtHttp", "Request failed, with code " + response.code() +
                        " with message " + responseMessage + " following response received " +
                        response.body().string());
                response.close();
                return new HttpResponse(false, null, null,
                        "Unexpected code " + response, responseCode);
            }

            // For streaming, return the response body as is without consuming it
            return new HttpResponse(true,
                    response.body(),
                    null,
                    null,
                    response.code());
            // Note: the response is not closed; the caller must close the ResponseBody

        } catch (IOException e) {
            return new HttpResponse(false,
                    null,
                    null,
                    e.getMessage(),
                    1000);
        }
    }
}
