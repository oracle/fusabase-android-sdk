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


package com.oracle.mobile.fusabase.oracledb.internal.validation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.SetOptions;
import com.oracle.mobile.fusabase.oracledb.internal.operation.OperationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.BatchWriteCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentDeleteCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentGetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentSetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentTarget;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentUpdateCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.QueryCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;

import java.util.List;
import java.util.Map;

import jakarta.json.JsonObject;

/**
 * Creates validated, immutable operation commands.
 *
 * <p>The factory is intentionally independent of HTTP, offline persistence, and QueryHelper.
 * This gives later execution layers one stable boundary for input validation and normalization.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class OperationCommandFactory {
    private static final String TAG = "OracledbCommands";

    @NonNull
    public DocumentGetCommand createGet(@NonNull List<String> path,
                                        @NonNull JsonObject payload)
            throws FusabaseOracledbException {
        return create("document-get", () -> new DocumentGetCommand(path, payload));
    }

    @NonNull
    public DocumentGetCommand createGet(@NonNull List<String> path,
                                        @NonNull JsonObject payload,
                                        @Nullable TransactionContext context)
            throws FusabaseOracledbException {
        return create("document-get", () -> new DocumentGetCommand(path, payload, context));
    }

    @NonNull
    public DocumentSetCommand createSet(@NonNull List<String> path,
                                        @NonNull Object data,
                                        @NonNull SetOptions options)
            throws FusabaseOracledbException {
        return create("document-set", () -> new DocumentSetCommand(path, data, options));
    }

    @NonNull
    public DocumentSetCommand createSet(@NonNull List<String> path,
                                        @NonNull Object data,
                                        @NonNull SetOptions options,
                                        @Nullable TransactionContext context)
            throws FusabaseOracledbException {
        return create("document-set", () -> new DocumentSetCommand(path, data, options, context));
    }

    @NonNull
    public DocumentSetCommand createAdd(@NonNull List<String> path,
                                        @NonNull Object data)
            throws FusabaseOracledbException {
        return create("document-add", () -> new DocumentSetCommand(path, data, null, null,
                DocumentTarget.DOCUMENT, false));
    }

    /** Creates an overwrite command for replayed persisted data. */
    @NonNull
    public DocumentSetCommand createOverwrite(@NonNull List<String> path,
                                              @NonNull Object data)
            throws FusabaseOracledbException {
        return createOverwrite(path, data, null);
    }

    /** Creates an overwrite command while preserving an optional replay transaction context. */
    @NonNull
    public DocumentSetCommand createOverwrite(@NonNull List<String> path,
                                              @NonNull Object data,
                                              @Nullable TransactionContext context)
            throws FusabaseOracledbException {
        return create("document-overwrite", () -> new DocumentSetCommand(path, data, null, context,
                DocumentTarget.DOCUMENT, false, false));
    }

    @NonNull
    public DocumentSetCommand createSet(@NonNull List<String> path,
                                        @NonNull Object data,
                                        @Nullable SetOptions options,
                                        @Nullable TransactionContext context,
                                        @NonNull DocumentTarget target,
                                        boolean oidNotGenerated)
            throws FusabaseOracledbException {
        return create("document-set", () -> new DocumentSetCommand(path, data, options, context,
                target, oidNotGenerated));
    }

    @NonNull
    public DocumentUpdateCommand createUpdate(@NonNull List<String> path,
                                              @NonNull Map<String, Object> data)
            throws FusabaseOracledbException {
        return create("document-update", () -> new DocumentUpdateCommand(path, data));
    }

    @NonNull
    public DocumentUpdateCommand createUpdate(@NonNull List<String> path,
                                              @NonNull Map<String, Object> data,
                                              @Nullable TransactionContext context)
            throws FusabaseOracledbException {
        return create("document-update", () -> new DocumentUpdateCommand(path, data, context));
    }

    @NonNull
    public DocumentUpdateCommand createUpdate(@NonNull List<String> path,
                                              @NonNull Map<String, Object> data,
                                              @Nullable TransactionContext context,
                                              @NonNull DocumentTarget target,
                                              boolean wildcardPath,
                                              @Nullable JsonObject params)
            throws FusabaseOracledbException {
        return create("document-update", () -> new DocumentUpdateCommand(path, data, context,
                target, wildcardPath, params));
    }

    @NonNull
    public DocumentDeleteCommand createDelete(@NonNull List<String> path)
            throws FusabaseOracledbException {
        return create("document-delete", () -> new DocumentDeleteCommand(path));
    }

    @NonNull
    public DocumentDeleteCommand createDelete(@NonNull List<String> path,
                                              @Nullable TransactionContext context)
            throws FusabaseOracledbException {
        return create("document-delete", () -> new DocumentDeleteCommand(path, context));
    }

    @NonNull
    public DocumentDeleteCommand createDelete(@NonNull List<String> path,
                                              @Nullable TransactionContext context,
                                              @NonNull DocumentTarget target)
            throws FusabaseOracledbException {
        return create("document-delete", () -> new DocumentDeleteCommand(path, context, target));
    }

    @NonNull
    public QueryCommand createQuery(@NonNull List<String> path,
                                    @NonNull JsonObject payload)
            throws FusabaseOracledbException {
        return create("query", () -> new QueryCommand(path, payload));
    }

    @NonNull
    public QueryCommand createQuery(@NonNull List<String> path,
                                    @NonNull JsonObject payload,
                                    @Nullable TransactionContext context)
            throws FusabaseOracledbException {
        return create("query", () -> new QueryCommand(path, payload, context));
    }

    @NonNull
    public BatchWriteCommand createBatch(
            @NonNull List<? extends OperationCommand<?>> operations)
            throws FusabaseOracledbException {
        return create("batch-write", () -> new BatchWriteCommand(operations));
    }

    @NonNull
    public TransactionCommand createTransaction(
            @NonNull List<? extends OperationCommand<?>> operations,
            @NonNull TransactionContext context)
            throws FusabaseOracledbException {
        return create("transaction", () -> new TransactionCommand(operations, context));
    }

    @NonNull
    private <T> T create(@NonNull String operation,
                         @NonNull CommandCreation<T> creation)
            throws FusabaseOracledbException {
        FusabaseLogger.v(TAG, "Validating " + operation + " command input");
        try {
            T command = creation.create();
            FusabaseLogger.i(TAG, "Created immutable " + operation + " command");
            return command;
        } catch (FusabaseOracledbException e) {
            FusabaseLogger.w(TAG, "Rejected " + operation + " command with code " +
                    e.getErrorCode());
            throw e;
        } catch (RuntimeException e) {
            FusabaseLogger.e(TAG, "Could not normalize " + operation + " command input", e);
            throw new FusabaseOracledbException(
                    "Invalid input for " + operation + " operation",
                    FusabaseOracledbException.Code.INVALID_ARGUMENT,
                    e);
        }
    }

    private interface CommandCreation<T> {
        @NonNull
        T create() throws FusabaseOracledbException;
    }
}
