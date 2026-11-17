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

import com.oracle.mobile.fusabase.oracledb.internal.logging.OperationLogContext;

import java.util.Objects;

/** Structured, non-public failure used between internal operation layers. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class InternalOperationException extends Exception {
    @NonNull
    private final ErrorCategory category;
    private final boolean retryable;
    @Nullable
    private final OperationLogContext logContext;

    public InternalOperationException(@NonNull String message,
                                      @NonNull ErrorCategory category,
                                      boolean retryable,
                                      @Nullable OperationLogContext logContext) {
        this(message, category, retryable, logContext, null);
    }

    public InternalOperationException(@NonNull String message,
                                      @NonNull ErrorCategory category,
                                      boolean retryable,
                                      @Nullable OperationLogContext logContext,
                                      @Nullable Throwable cause) {
        super(Objects.requireNonNull(message, "Failure message cannot be null"), cause);
        this.category = Objects.requireNonNull(category, "Error category cannot be null");
        this.retryable = retryable;
        this.logContext = logContext;
    }

    @NonNull
    public ErrorCategory getCategory() {
        return category;
    }

    public boolean isRetryable() {
        return retryable;
    }

    @Nullable
    public OperationLogContext getLogContext() {
        return logContext;
    }
}
