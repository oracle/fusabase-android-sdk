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


package com.oracle.mobile.fusabase.oracledb.internal.transport;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.error.ErrorCategory;
import com.oracle.mobile.fusabase.oracledb.internal.error.InternalOperationException;
import com.oracle.mobile.fusabase.oracledb.internal.logging.OperationLogContext;
import com.oracle.mobile.fusabase.oracledb.internal.request.model.RestRequest;

import java.io.IOException;
import java.io.StringReader;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Adapts an immutable REST request intent to the existing HTTP helper.
 *
 * <p>The helper remains the owner of OkHttp configuration, retries, App Trust refresh/retry,
 * timeout behavior, and executor behavior. This adapter owns only translation, safe lifecycle
 * logging, and conversion of helper execution failures to internal failures.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class HttpRequestHelperTransport implements RestTransport {
    private static final String TAG = "OracledbTransport";

    private final HttpRequestHelperFactory requestHelperFactory;

    /** Creates a production transport backed by the existing HTTP helper. */
    public HttpRequestHelperTransport() {
        this(HttpRequestHelper::new);
    }

    HttpRequestHelperTransport(@NonNull HttpRequestHelperFactory requestHelperFactory) {
        this.requestHelperFactory = Objects.requireNonNull(requestHelperFactory,
                "HTTP request helper factory cannot be null");
    }

    @NonNull
    @Override
    public RestResponse execute(@NonNull RestRequest request) throws InternalOperationException {
        Objects.requireNonNull(request, "REST request cannot be null");
        OperationLogContext context = OperationLogContext.from(request);
        FusabaseLogger.v(TAG, "Preparing " + context.getOperation() + " REST transport");
        FusabaseLogger.d(TAG, "Executing " + context);
        FusabaseLogger.d(TAG, describeOutboundRequest(request));

        HttpRequestHelper requestHelper;
        try {
            requestHelper = requestHelperFactory.create(context.getRetryAttempts());
            createRequest(requestHelper, request);
        } catch (FusabaseException | RuntimeException failure) {
            FusabaseLogger.e(TAG, "REST request preparation failed for " + context.getOperation());
            throw new InternalOperationException(
                    "REST request preparation failed",
                    ErrorCategory.SERIALIZATION,
                    false,
                    context,
                    failure);
        }

        final HttpResponse response;
        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException | RuntimeException failure) {
            FusabaseLogger.e(TAG, "REST transport execution failed for " + context.getOperation());
            throw new InternalOperationException(
                    "REST transport execution failed",
                    ErrorCategory.TRANSPORT,
                    true,
                    context,
                    failure);
        }

        if (response == null) {
            FusabaseLogger.e(TAG, "REST transport returned no response for " +
                    context.getOperation());
            throw new InternalOperationException(
                    "REST transport returned no response",
                    ErrorCategory.PROTOCOL,
                    false,
                    context);
        }

        if (!response.getStatus() && response.getCode() ==
                FusabaseOracledbException.Code.NETWORK_ERROR.getCode()) {
            FusabaseLogger.w(TAG, "REST transport reported a retryable network failure for " +
                    context.getOperation());
            throw new InternalOperationException(
                    "REST transport reported a network failure",
                    ErrorCategory.TRANSPORT,
                    true,
                    context.withStatusCode(response.getCode()),
                    networkFailure());
        }

        String body = response.getResponse();
        OperationLogContext responseContext = context.withStatusCode(response.getCode());
        RestResponse restResponse = new RestResponse(response.getStatus(), response.getCode(),
                body == null ? "" : body, response.getError(), responseContext);
        if (response.getStatus()) {
            FusabaseLogger.i(TAG, "REST operation completed for " + responseContext);
        } else {
            FusabaseLogger.w(TAG, "REST operation returned a non-success status for " +
                    responseContext);
        }
        return restResponse;
    }

    private static void createRequest(@NonNull HttpRequestHelper requestHelper,
                                      @NonNull RestRequest request) throws FusabaseException {
        if (request.getBody() == null) {
            requestHelper.createHttpRequest(request.getUrl(), request.getMethod().getWireName(),
                    request.getHeaders(), request.getQueryParameters());
            return;
        }
        requestHelper.createHttpRequest(request.getUrl(), request.getMethod().getWireName(),
                request.getHeaders(), request.getQueryParameters(), request.getBody());
    }

    /**
     * Produces temporary transport diagnostics with a redacted serialized JSON payload.
     */
    @NonNull
    static String describeOutboundRequest(@NonNull RestRequest request) {
        String bodyDescription = "body=none";
        String body = request.getBody();
        if (body != null) {
            bodyDescription = "bodyLength=" + body.length();
            try (JsonReader reader = Json.createReader(new StringReader(body))) {
                bodyDescription += ", bodyJsonFields=" + reader.readObject().keySet();
            } catch (RuntimeException ignored) {
                bodyDescription += ", bodyJsonFields=<non-json>";
            }
            bodyDescription += ", payload=" + sanitizePayload(body);
        }
        return "Outbound REST request: operation=" + request.getOperation()
                + ", method=" + request.getMethod().getWireName()
                + ", url=" + request.getUrl()
                + ", queryParameterNames=" + request.getQueryParameters().keySet()
                + ", headerNames=" + request.getHeaders().keySet()
                + ", " + bodyDescription;
    }

    /** Returns the payload for temporary diagnostics after recursively redacting secret-like keys. */
    @NonNull
    static String sanitizePayload(@NonNull String body) {
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            return redact(reader.readValue()).toString();
        } catch (RuntimeException ignored) {
            return "<non-json>";
        }
    }

    @NonNull
    private static JsonValue redact(@NonNull JsonValue value) {
        switch (value.getValueType()) {
            case OBJECT:
                JsonObjectBuilder object = Json.createObjectBuilder();
                value.asJsonObject().forEach((key, child) -> object.add(key,
                        isSensitiveKey(key) ? Json.createValue("***") : redact(child)));
                return object.build();
            case ARRAY:
                JsonArrayBuilder array = Json.createArrayBuilder();
                value.asJsonArray().forEach(child -> array.add(redact(child)));
                return array.build();
            default:
                return value;
        }
    }

    private static boolean isSensitiveKey(@NonNull String key) {
        String normalized = key.toLowerCase(java.util.Locale.US);
        return normalized.contains("token") || normalized.contains("authorization")
                || normalized.contains("password") || normalized.contains("secret")
                || normalized.contains("credential") || normalized.equals("code");
    }

    @NonNull
    private static IOException networkFailure() {
        return new IOException("REST transport returned a network failure");
    }
}
