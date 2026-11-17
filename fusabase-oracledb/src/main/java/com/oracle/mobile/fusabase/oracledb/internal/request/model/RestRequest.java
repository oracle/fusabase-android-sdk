// Copyright (c) 2015, 2026, Oracle and/or its affiliates.

package com.oracle.mobile.fusabase.oracledb.internal.request.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.internal.operation.OperationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.RestHttpMethod;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.RestOperation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable REST request intent created before transport execution. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RestRequest {
    private final OperationCommand<?> command;
    private final RestOperation operation;
    private final RestHttpMethod method;
    private final String url;
    private final Map<String, String> headers;
    private final Map<String, String> queryParameters;
    private final String body;
    private final RequestMetadata metadata;

    public RestRequest(@NonNull OperationCommand<?> command,
                       @NonNull RestOperation operation,
                       @NonNull RestHttpMethod method,
                       @NonNull String url,
                       @NonNull Map<String, String> headers,
                       @NonNull Map<String, String> queryParameters,
                       @Nullable String body,
                       @NonNull RequestMetadata metadata) {
        this.command = Objects.requireNonNull(command, "Operation command cannot be null");
        this.operation = Objects.requireNonNull(operation, "REST operation cannot be null");
        this.method = Objects.requireNonNull(method, "HTTP method cannot be null");
        this.url = Objects.requireNonNull(url, "Request URL cannot be null");
        this.headers = immutableCopy(headers, "Request headers cannot be null");
        this.queryParameters = immutableCopy(queryParameters,
                "Request query parameters cannot be null");
        this.body = body;
        this.metadata = Objects.requireNonNull(metadata, "Request metadata cannot be null");
    }

    @NonNull
    public OperationCommand<?> getCommand() {
        return command;
    }

    @NonNull
    public RestOperation getOperation() {
        return operation;
    }

    @NonNull
    public RestHttpMethod getMethod() {
        return method;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @NonNull
    public Map<String, String> getHeaders() {
        return headers;
    }

    @NonNull
    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    @Nullable
    public String getBody() {
        return body;
    }

    @NonNull
    public RequestMetadata getMetadata() {
        return metadata;
    }

    @NonNull
    private static Map<String, String> immutableCopy(@Nullable Map<String, String> values,
                                                      @NonNull String nullMessage) {
        Objects.requireNonNull(values, nullMessage);
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
