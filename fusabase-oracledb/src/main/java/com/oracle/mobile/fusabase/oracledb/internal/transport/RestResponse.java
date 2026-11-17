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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.internal.logging.OperationLogContext;

import java.util.Objects;

/** Minimal internal response envelope used before response mapping is extracted. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RestResponse {
    private final boolean status;
    private final int statusCode;
    private final String body;
    @Nullable
    private final String error;
    @Nullable
    private final OperationLogContext logContext;

    public RestResponse(int statusCode, @NonNull String body) {
        this(statusCode >= 200 && statusCode < 300, statusCode, body, null);
    }

    public RestResponse(boolean status, int statusCode, @NonNull String body) {
        this(status, statusCode, body, null);
    }

    public RestResponse(boolean status,
                        int statusCode,
                        @NonNull String body,
                        @Nullable String error) {
        this(status, statusCode, body, error, null);
    }

    public RestResponse(boolean status,
                        int statusCode,
                        @NonNull String body,
                        @Nullable String error,
                        @Nullable OperationLogContext logContext) {
        this.status = status;
        this.statusCode = statusCode;
        this.body = Objects.requireNonNull(body, "Response body cannot be null");
        this.error = error;
        this.logContext = logContext;
    }

    public boolean getStatus() {
        return status;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @NonNull
    public String getBody() {
        return body;
    }

    @Nullable
    public String getError() {
        return error;
    }

    @Nullable
    public OperationLogContext getLogContext() {
        return logContext;
    }

    @Override
    @NonNull
    public String toString() {
        return "RestResponse{" +
                "status=" + status +
                ", statusCode=" + statusCode +
                ", bodyPresent=" + !body.isEmpty() +
                ", errorPresent=" + (error != null) +
                '}';
    }
}
