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


package com.oracle.mobile.fusabase.oracledb.internal.logging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.OracledbApiVersion;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.RestOperation;
import com.oracle.mobile.fusabase.oracledb.internal.request.model.RequestMetadata;
import com.oracle.mobile.fusabase.oracledb.internal.request.model.RestRequest;

import java.util.Objects;

/**
 * Safe operation metadata for diagnostics.
 *
 * <p>This type deliberately contains no URL, header value, query value, body, document value, or
 * exception message. It is safe to include in internal logs and error classification.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class OperationLogContext {
    private final RestOperation operation;
    private final OracledbApiVersion apiVersion;
    private final int retryAttempts;
    private final boolean authorizationIncluded;
    private final boolean transactionHeaderIncluded;
    private final boolean contentTypeIncluded;
    private final boolean bodyPresent;
    @Nullable
    private final Integer statusCode;

    private OperationLogContext(@NonNull RestOperation operation,
                                @NonNull OracledbApiVersion apiVersion,
                                int retryAttempts,
                                boolean authorizationIncluded,
                                boolean transactionHeaderIncluded,
                                boolean contentTypeIncluded,
                                boolean bodyPresent,
                                @Nullable Integer statusCode) {
        this.operation = Objects.requireNonNull(operation, "REST operation cannot be null");
        this.apiVersion = Objects.requireNonNull(apiVersion, "API version cannot be null");
        if (retryAttempts < 1) {
            throw new IllegalArgumentException("Retry attempts must be positive");
        }
        this.retryAttempts = retryAttempts;
        this.authorizationIncluded = authorizationIncluded;
        this.transactionHeaderIncluded = transactionHeaderIncluded;
        this.contentTypeIncluded = contentTypeIncluded;
        this.bodyPresent = bodyPresent;
        this.statusCode = statusCode;
    }

    /** Creates safe diagnostics from request metadata and never copies secret values. */
    @NonNull
    public static OperationLogContext from(@NonNull RequestMetadata metadata,
                                           boolean bodyPresent) {
        Objects.requireNonNull(metadata, "Request metadata cannot be null");
        return new OperationLogContext(metadata.getOperation(), metadata.getApiVersion(),
                metadata.getRetryAttempts(), metadata.hasAuthorization(),
                metadata.hasTransactionHeader(), metadata.hasContentType(), bodyPresent, null);
    }

    /** Creates safe diagnostics for a prepared request. */
    @NonNull
    public static OperationLogContext from(@NonNull RestRequest request) {
        Objects.requireNonNull(request, "REST request cannot be null");
        return from(request.getMetadata(), request.getBody() != null);
    }

    /** Returns a copy with a response status code, without retaining response content. */
    @NonNull
    public OperationLogContext withStatusCode(int responseStatusCode) {
        return new OperationLogContext(operation, apiVersion, retryAttempts,
                authorizationIncluded, transactionHeaderIncluded, contentTypeIncluded,
                bodyPresent, responseStatusCode);
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

    public boolean hasBody() {
        return bodyPresent;
    }

    @Nullable
    public Integer getStatusCode() {
        return statusCode;
    }

    @NonNull
    @Override
    public String toString() {
        return "OperationLogContext{" +
                "operation=" + operation +
                ", apiVersion=" + apiVersion.getConfiguredValue() +
                ", retryAttempts=" + retryAttempts +
                ", authorizationPresent=" + authorizationIncluded +
                ", transactionHeaderPresent=" + transactionHeaderIncluded +
                ", contentTypePresent=" + contentTypeIncluded +
                ", bodyPresent=" + bodyPresent +
                ", statusCode=" + statusCode +
                '}';
    }
}
