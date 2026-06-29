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

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.io.IOException;
import java.util.Map;
import java.util.Locale;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Headers;
import okhttp3.TlsVersion;
import okio.Buffer;
import okio.BufferedSource;

public class HttpRequestHelper {

    private final static String TAG = "FusabaseHTTP";

    // Never log request bodies. Redact sensitive headers and query parameters.
    private static final String BODY_OMITTED = "<omitted>";

    // NOTE: apiKey is intentionally NOT treated as sensitive (per SDK policy).
    private static final String[] SENSITIVE_QUERY_PARAMS = new String[] {
            "code",
            "authCode",
            "authorization_code",
            "token",
            "access_token",
            "refresh_token",
            "id_token",
            "password",
            "client_secret"
    };

    private static final String[] SENSITIVE_HEADERS = new String[] {
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "proxy-authorization"
    };

    private static boolean isSensitiveQueryParamName(@NonNull String name) {
        // Defensive: treat any token-ish parameter name as sensitive.
        String n = name.toLowerCase(Locale.US);
        if (equalsIgnoreCaseAny(n, SENSITIVE_QUERY_PARAMS)) return true;
        // common patterns
        return n.contains("token") || n.contains("secret") || n.contains("password") || n.contains("auth") || n.contains("code");
    }

    private static boolean isSensitiveHeaderName(@NonNull String name) {
        String n = name.toLowerCase(Locale.US);
        if (equalsIgnoreCaseAny(n, SENSITIVE_HEADERS)) return true;
        // Defensive: cover variations.
        return n.contains("authorization") || n.contains("cookie") || n.contains("token") || n.contains("secret");
    }

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
                        FusabaseLogger.d(TAG, "Request failed, retrying...");

                } catch (IOException e) {
                    if (attempt >= maxRetries - 1) {
                        throw e;
                    }
                }

                attempt++;

                long backoff = delayMillis * (long) Math.pow(2, attempt); // Exponential backoff

                try {
                    if (attempt < this.maxRetries) {
                        FusabaseLogger.d(TAG, "Retry attempt " + attempt + " after " + backoff + " ms");
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

    private static boolean equalsIgnoreCaseAny(String value, String[] candidates) {
        if (value == null) return false;
        for (String c : candidates) {
            if (c != null && c.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    @NonNull
    private static String sanitizeUrl(@NonNull HttpUrl url) {
        HttpUrl.Builder builder = url.newBuilder();
        for (String name : url.queryParameterNames()) {
            if (isSensitiveQueryParamName(name)) builder.setQueryParameter(name, "***");
        }
        return builder.build().toString();
    }

    @NonNull
    private static String sanitizeHeaders(@NonNull Headers headers) {
        if (headers.size() == 0) return "Headers: None";
        StringBuilder sb = new StringBuilder();
        sb.append("Headers:\n");
        for (String name : headers.names()) {
            String value = headers.get(name);
            if (isSensitiveHeaderName(name)) value = "***";
            sb.append("  ").append(name).append(": ").append(value).append("\n");
        }
        return sb.toString();
    }

    public HttpRequestHelper() {
        this(1);
    }

    public HttpRequestHelper(int retries) {
        this.retries = retries;
        this.httpClient = createHttpClient(retries);
    }

    private OkHttpClient createHttpClient(int retries) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(new ExponentialBackoffInterceptor(retries, 100));

        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
                .build();
        builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));

        return builder.build();
    }

    /* Create request with url and body */
    @NonNull
    public HttpRequestHelper createHttpRequest(@NonNull String url,
                                               @NonNull String method,
                                               @NonNull String body) throws FusabaseException {
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
                                               @NonNull String body) throws FusabaseException {
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
                                               @NonNull RequestBody body) throws FusabaseException {
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
                                               @NonNull Map<String, String> queryParameter) throws FusabaseException {
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
            FusabaseLogger.d("HTTPHelper", e.getMessage());
        }

        switch (method.toUpperCase()) {
            case "GET":
                if (body != null)
                    FusabaseLogger.w(TAG, "Invalid method for network call received.");
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
                FusabaseLogger.e(TAG, "Unknown Operation for network call encountered.");
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
        logMessage.append("URL: ").append(sanitizeUrl(request.url())).append("\n");
        logMessage.append(sanitizeHeaders(request.headers()));
        // Never log request bodies (they may contain secrets/tokens/passwords)
        logMessage.append("Body: ").append(BODY_OMITTED);

        FusabaseLogger.d(TAG, logMessage.toString());
    }

    private static void logFailedResponse(@NonNull Response response) {
        // Never log response bodies (they may contain secrets or backend diagnostics)
        FusabaseLogger.w(TAG, "Request failed with code " + response.code() + " message=" + response.message());
    }

    public HttpResponse executeRequest() throws FusabaseException {
        // NOTE: This method buffers the full response body into memory.
        // Prefer executeRequestStreaming() for large responses.
        try {

            if (request == null) {
                throw new FusabaseException("Request is not initialized." +
                        " Kindly create a request first using buildRequest().");
            }

            // Log HTTP request details
            logHttpRequest();

            Response response = this.httpClient.newCall(this.request).execute();

            if (!response.isSuccessful()) {
                int responseCode = response.code();
                logFailedResponse(response);
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

    public HttpResponse executeRequestStreaming() throws FusabaseException {
        try {

            if (request == null) {
                throw new FusabaseException("Request is not initialized." +
                        " Kindly create a request first using buildRequest().");
            }

            // Log HTTP request details
            logHttpRequest();

            Response response = this.httpClient.newCall(this.request).execute();

            if (!response.isSuccessful()) {
                int responseCode = response.code();
                logFailedResponse(response);
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
