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


package com.oracle.mobile.fusabase.oracledb.internal.error;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.logging.OperationLogContext;

import java.io.IOException;
import java.util.Objects;

/** Default mapping from internal failure categories to the stable public error codes. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentErrorMapper implements OperationErrorMapper {
    private static final String SAFE_FAILURE_MESSAGE = "OracleDB operation failed";

    @NonNull
    @Override
    public FusabaseOracledbException map(@NonNull Throwable failure) {
        Objects.requireNonNull(failure, "Failure cannot be null");
        if (failure instanceof FusabaseOracledbException) {
            return (FusabaseOracledbException) failure;
        }

        FusabaseOracledbException.Code code = FusabaseOracledbException.Code.INTERNAL;
        @Nullable OperationLogContext context = null;
        if (failure instanceof InternalOperationException) {
            InternalOperationException operationException = (InternalOperationException) failure;
            context = operationException.getLogContext();
            code = codeFor(operationException.getCategory(), context);
        } else if (failure instanceof IllegalArgumentException) {
            code = FusabaseOracledbException.Code.INVALID_ARGUMENT;
        } else if (failure instanceof IOException) {
            code = FusabaseOracledbException.Code.NETWORK_ERROR;
        }

        return mappedException(code, causeFor(failure));
    }

    @NonNull
    @Override
    public FusabaseOracledbException mapHttpStatus(int statusCode) {
        return mappedException(FusabaseOracledbException.Code.fromCode(statusCode), null);
    }

    @NonNull
    private static FusabaseOracledbException.Code codeFor(@NonNull ErrorCategory category,
                                                           @Nullable OperationLogContext context) {
        switch (category) {
            case VALIDATION:
            case NORMALIZATION:
                return FusabaseOracledbException.Code.INVALID_ARGUMENT;
            case TRANSPORT:
                return FusabaseOracledbException.Code.NETWORK_ERROR;
            case HTTP:
                if (context != null && context.getStatusCode() != null) {
                    return FusabaseOracledbException.Code.fromCode(context.getStatusCode());
                }
                return FusabaseOracledbException.Code.UNKNOWN_ERROR;
            case SERIALIZATION:
            case PROTOCOL:
            case MAPPING:
            case PERSISTENCE:
            case REPLAY:
            case UNKNOWN:
            default:
                return FusabaseOracledbException.Code.INTERNAL;
        }
    }

    @NonNull
    private static FusabaseOracledbException mappedException(
            @NonNull FusabaseOracledbException.Code code, @Nullable Throwable cause) {
        String message = SAFE_FAILURE_MESSAGE + ": " + code.getMessage();
        return new FusabaseOracledbException(message, code, cause);
    }

    @NonNull
    private static Throwable causeFor(@NonNull Throwable failure) {
        Throwable cause = failure.getCause();
        return cause == null ? failure : cause;
    }
}
