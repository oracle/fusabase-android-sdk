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


package com.oracle.mobile.fusabase.oracledb.internal.operation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.SetOptions;
import com.oracle.mobile.fusabase.oracledb.internal.offline.OfflineStorePort;
import com.oracle.mobile.fusabase.oracledb.internal.offline.RemoteMutationExecutor;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentTarget;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentDeleteResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentReadResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentWriteResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.QueryResponse;
import com.oracle.mobile.fusabase.oracledb.internal.validation.OperationCommandFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.json.JsonObject;

/** Internal operation boundary for selecting local, offline, and remote execution routes. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentOperationCoordinator {
    private final DocumentRestService restService;
    private final OfflineStorePort offlineStore;
    private final RemoteMutationExecutor remoteMutationExecutor;
    private final OperationCommandFactory commandFactory;

    public DocumentOperationCoordinator(@NonNull DocumentRestService restService,
                                        @NonNull OfflineStorePort offlineStore,
                                        @NonNull RemoteMutationExecutor remoteMutationExecutor,
                                        @NonNull OperationCommandFactory commandFactory) {
        this.restService = Objects.requireNonNull(restService, "Document REST service cannot be null");
        this.offlineStore = Objects.requireNonNull(offlineStore, "Offline store cannot be null");
        this.remoteMutationExecutor = Objects.requireNonNull(remoteMutationExecutor,
                "Remote mutation executor cannot be null");
        this.commandFactory = Objects.requireNonNull(commandFactory,
                "Operation command factory cannot be null");
    }

    /** Executes a document read through request creation, transport, and response processing. */
    @NonNull
    public DocumentReadResponse read(@NonNull List<String> path,
                                     @NonNull JsonObject payload,
                                     @Nullable TransactionContext context)
            throws FusabaseOracledbException {
        return (DocumentReadResponse) restService.execute(
                commandFactory.createGet(path, payload, context));
    }

    /** Executes a collection, aggregate, vector, or join query through the query command path. */
    @NonNull
    public QueryResponse query(@NonNull List<String> path,
                               @NonNull JsonObject payload,
                               @Nullable TransactionContext context)
            throws FusabaseOracledbException {
        return (QueryResponse) restService.execute(
                commandFactory.createQuery(path, payload, context));
    }

    /** Executes a document create/set through the centralized REST pipeline. */
    @NonNull
    public DocumentWriteResponse set(@NonNull List<String> path,
                                     @NonNull Object data,
                                     @Nullable SetOptions options,
                                     @Nullable TransactionContext context,
                                     @NonNull DocumentTarget target,
                                     boolean oidNotGenerated)
            throws FusabaseOracledbException {
        return (DocumentWriteResponse) restService.execute(
                commandFactory.createSet(path, data, options, context, target, oidNotGenerated));
    }

    /** Executes a document update through the centralized REST pipeline. */
    @NonNull
    public DocumentWriteResponse update(@NonNull List<String> path,
                                        @NonNull Map<String, Object> data,
                                        @Nullable TransactionContext context,
                                        @NonNull DocumentTarget target,
                                        boolean wildcardPath,
                                        @Nullable JsonObject params)
            throws FusabaseOracledbException {
        return (DocumentWriteResponse) restService.execute(
                commandFactory.createUpdate(path, data, context, target, wildcardPath, params));
    }

    /** Executes a document deletion through the centralized REST pipeline. */
    @NonNull
    public DocumentDeleteResponse delete(@NonNull List<String> path,
                                         @Nullable TransactionContext context,
                                         @NonNull DocumentTarget target)
            throws FusabaseOracledbException {
        return (DocumentDeleteResponse) restService.execute(
                commandFactory.createDelete(path, context, target));
    }

    /** Executes a normalized command for internal callers that need a typed response model. */
    @NonNull
    public com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentResponse execute(
            @NonNull OperationCommand<?> command) throws FusabaseOracledbException {
        return restService.execute(command);
    }
}
