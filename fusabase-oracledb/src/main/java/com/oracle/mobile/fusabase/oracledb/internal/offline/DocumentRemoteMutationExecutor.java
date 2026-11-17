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

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.operation.DocumentRestService;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;
import com.oracle.mobile.fusabase.oracledb.internal.validation.OperationCommandFactory;
import com.oracle.mobile.fusabase.oracledb.offline.OfflineMutation;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Executes persisted mutations through the internal REST pipeline. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentRemoteMutationExecutor implements RemoteMutationExecutor {
    private static final String TAG = "OfflineRemoteExecutor";

    private final DocumentRestService restService;
    private final PersistedMutationCommandConverter commandConverter;

    public DocumentRemoteMutationExecutor(@NonNull DocumentRestService restService,
                                          @NonNull OperationCommandFactory commandFactory) {
        this(restService, new PersistedMutationCommandConverter(commandFactory));
    }

    public DocumentRemoteMutationExecutor(
            @NonNull DocumentRestService restService,
            @NonNull PersistedMutationCommandConverter commandConverter) {
        this.restService = Objects.requireNonNull(restService,
                "Document REST service cannot be null");
        this.commandConverter = Objects.requireNonNull(commandConverter,
                "Persisted mutation converter cannot be null");
    }

    /**
     * Executes one mutation synchronously at the REST boundary and exposes the result as a
     * completion stage for the asynchronous offline drain.
     */
    @NonNull
    @Override
    public CompletionStage<Void> execute(@NonNull OfflineMutation mutation) {
        return execute(mutation, null);
    }

    @NonNull
    @Override
    public CompletionStage<Void> execute(@NonNull OfflineMutation mutation,
                                         @Nullable TransactionContext transactionContext) {
        Objects.requireNonNull(mutation, "Offline mutation cannot be null");
        CompletableFuture<Void> completion = new CompletableFuture<>();
        try {
            restService.execute(commandConverter.convert(mutation, transactionContext));
            FusabaseLogger.d(TAG, "Remote replay operation completed");
            completion.complete(null);
        } catch (FusabaseOracledbException failure) {
            completion.completeExceptionally(failure);
        } catch (RuntimeException failure) {
            FusabaseLogger.e(TAG, "Remote replay command could not be created or executed");
            completion.completeExceptionally(new FusabaseOracledbException(
                    "Offline mutation replay failed",
                    FusabaseOracledbException.Code.INTERNAL,
                    failure));
        }
        return completion;
    }
}
