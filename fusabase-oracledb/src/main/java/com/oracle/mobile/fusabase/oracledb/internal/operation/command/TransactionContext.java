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


package com.oracle.mobile.fusabase.oracledb.internal.operation.command;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Objects;

/** Immutable transaction metadata captured when a transaction command is created. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class TransactionContext {
    private final int beginTrans;
    private final int endTrans;
    private final String transactionName;
    private final int maxAttempts;
    private final String version;

    public TransactionContext(int beginTrans,
                              int endTrans,
                              @NonNull String transactionName,
                              int maxAttempts,
                              @Nullable String version) {
        this.beginTrans = beginTrans;
        this.endTrans = endTrans;
        this.transactionName = Objects.requireNonNull(transactionName,
                "Transaction name cannot be null");
        this.maxAttempts = maxAttempts;
        this.version = version;
    }

    public int getBeginTrans() {
        return beginTrans;
    }

    public int getEndTrans() {
        return endTrans;
    }

    @NonNull
    public String getTransactionName() {
        return transactionName;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    @Nullable
    public String getVersion() {
        return version;
    }
}
