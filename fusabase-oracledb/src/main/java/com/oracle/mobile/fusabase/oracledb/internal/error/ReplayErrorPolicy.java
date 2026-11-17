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
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/** Conservative replay policy that retries only failures known to be transient. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ReplayErrorPolicy implements ReplayPolicy {
    @NonNull
    @Override
    public ReplayAction classify(@NonNull Throwable failure) {
        Objects.requireNonNull(failure, "Replay failure cannot be null");
        Throwable cause = unwrap(failure);
        if (cause instanceof InternalOperationException) {
            return ((InternalOperationException) cause).isRetryable()
                    ? ReplayAction.RETRY : ReplayAction.REJECT;
        }
        if (cause instanceof FusabaseOracledbException) {
            FusabaseOracledbException.Code code =
                    ((FusabaseOracledbException) cause).getErrorCode();
            if (code == FusabaseOracledbException.Code.NETWORK_ERROR ||
                    code == FusabaseOracledbException.Code.IO_EXCEPTION) {
                return ReplayAction.RETRY;
            }
        }
        if (cause instanceof IOException) {
            return ReplayAction.RETRY;
        }
        return ReplayAction.REJECT;
    }

    @NonNull
    private static Throwable unwrap(@NonNull Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof ExecutionException) &&
                current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
