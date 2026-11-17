// Copyright (c) 2015, 2026, Oracle and/or its affiliates.

package com.oracle.mobile.fusabase.oracledb.internal.request.model;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.OracledbApiVersion;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.RestOperation;

import java.util.Objects;

/**
 * Non-secret metadata describing a prepared request.
 *
 * <p>Only header presence is recorded. Authorization and transaction header values are kept out
 * of this metadata so it can safely be used by diagnostics and error handling.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RequestMetadata {
    private final RestOperation operation;
    private final OracledbApiVersion apiVersion;
    private final int retryAttempts;
    private final boolean authorizationIncluded;
    private final boolean transactionHeaderIncluded;
    private final boolean contentTypeIncluded;

    public RequestMetadata(@NonNull RestOperation operation,
                           @NonNull OracledbApiVersion apiVersion,
                           int retryAttempts,
                           boolean authorizationIncluded,
                           boolean transactionHeaderIncluded,
                           boolean contentTypeIncluded) {
        this.operation = Objects.requireNonNull(operation, "REST operation cannot be null");
        this.apiVersion = Objects.requireNonNull(apiVersion, "API version cannot be null");
        if (retryAttempts < 1) {
            throw new IllegalArgumentException("Retry attempts must be positive");
        }
        this.retryAttempts = retryAttempts;
        this.authorizationIncluded = authorizationIncluded;
        this.transactionHeaderIncluded = transactionHeaderIncluded;
        this.contentTypeIncluded = contentTypeIncluded;
    }

    @NonNull
    public RestOperation getOperation() {
        return operation;
    }

    @NonNull
    public OracledbApiVersion getApiVersion() {
        return apiVersion;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public boolean hasAuthorization() {
        return authorizationIncluded;
    }

    public boolean hasTransactionHeader() {
        return transactionHeaderIncluded;
    }

    public boolean hasContentType() {
        return contentTypeIncluded;
    }
}
