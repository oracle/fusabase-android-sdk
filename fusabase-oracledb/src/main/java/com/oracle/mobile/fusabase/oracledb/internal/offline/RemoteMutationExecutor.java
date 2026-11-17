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


package com.oracle.mobile.fusabase.oracledb.internal.offline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;
import com.oracle.mobile.fusabase.oracledb.offline.OfflineMutation;

import java.util.concurrent.CompletionStage;

/** Internal port for replaying a persisted mutation without recursing through public APIs. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RemoteMutationExecutor {

    /** Executes one persisted mutation against the remote service. */
    @NonNull
    CompletionStage<Void> execute(@NonNull OfflineMutation mutation);

    /**
     * Executes one mutation with the transaction metadata belonging to its offline batch.
     * Existing executors remain source-compatible and may continue to use the single-argument
     * contract until they support batch transaction boundaries.
     */
    @NonNull
    default CompletionStage<Void> execute(@NonNull OfflineMutation mutation,
                                          @Nullable TransactionContext transactionContext) {
        return execute(mutation);
    }
}
