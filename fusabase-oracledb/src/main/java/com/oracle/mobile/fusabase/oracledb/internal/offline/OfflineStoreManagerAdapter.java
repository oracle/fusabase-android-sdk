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
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.operation.OperationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentDeleteCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentGetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentSetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentUpdateCommand;
import com.oracle.mobile.fusabase.oracledb.internal.request.codec.DocumentDataEncoder;
import com.oracle.mobile.fusabase.oracledb.offline.OfflineCachedDocument;
import com.oracle.mobile.fusabase.oracledb.offline.OfflineStoreManager;
import com.oracle.mobile.fusabase.task.Task;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/** Bridges normalized commands to the existing public offline-store lifecycle. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class OfflineStoreManagerAdapter implements OfflineStorePort {
    private static final String TAG = "OfflineStoreAdapter";

    private final FusabaseOracledb database;
    private final DocumentDataEncoder dataEncoder;

    public OfflineStoreManagerAdapter(@NonNull FusabaseOracledb database) {
        this(database, new DocumentDataEncoder());
    }

    OfflineStoreManagerAdapter(@NonNull FusabaseOracledb database,
                               @NonNull DocumentDataEncoder dataEncoder) {
        this.database = Objects.requireNonNull(database, "OracleDB client cannot be null");
        this.dataEncoder = Objects.requireNonNull(dataEncoder,
                "Document data encoder cannot be null");
    }

    /** Reads a document command from the overlay-aware local cache. */
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <TResult> CompletionStage<TResult> read(@NonNull OperationCommand<TResult> command) {
        Objects.requireNonNull(command, "Operation command cannot be null");
        if (!(command instanceof DocumentGetCommand)) {
            return failed(unsupported("Local read is not implemented for this command"));
        }

        try {
            DocumentGetCommand getCommand = (DocumentGetCommand) command;
            OfflineCachedDocument cached = OfflineStoreManager.getCachedDocument(
                    database, pathString(getCommand.getPath()));
            JsonObject payload = toReadPayload(cached, getCommand.getPath());
            FusabaseLogger.d(TAG, "Read document command from local persistence");
            return completed((TResult) payload);
        } catch (RuntimeException failure) {
            FusabaseLogger.e(TAG, "Local document read failed");
            return failed(new FusabaseOracledbException(
                    "Offline local read failed",
                    FusabaseOracledbException.Code.INTERNAL,
                    failure));
        }
    }

    /** Enqueues a supported document mutation through OfflineStoreManager. */
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <TResult> CompletionStage<TResult> enqueue(@NonNull OperationCommand<TResult> command) {
        Objects.requireNonNull(command, "Operation command cannot be null");
        try {
            Task<Void> task;
            if (command instanceof DocumentSetCommand) {
                DocumentSetCommand set = (DocumentSetCommand) command;
                task = OfflineStoreManager.enqueueSet(database, pathString(set.getPath()),
                        set.getData(), set.isMerge());
            } else if (command instanceof DocumentUpdateCommand) {
                DocumentUpdateCommand update = (DocumentUpdateCommand) command;
                task = OfflineStoreManager.enqueuePatch(database, pathString(update.getPath()),
                        update.getData());
            } else if (command instanceof DocumentDeleteCommand) {
                DocumentDeleteCommand delete = (DocumentDeleteCommand) command;
                task = OfflineStoreManager.enqueueDelete(database, pathString(delete.getPath()));
            } else {
                return failed(unsupported("Offline enqueue is not implemented for this command"));
            }
            FusabaseLogger.v(TAG, "Persisting normalized document mutation locally");
            return bridge(task, (TResult) Json.createObjectBuilder().build());
        } catch (RuntimeException failure) {
            FusabaseLogger.e(TAG, "Offline mutation could not be enqueued");
            return failed(new FusabaseOracledbException(
                    "Offline mutation could not be enqueued",
                    FusabaseOracledbException.Code.INVALID_ARGUMENT,
                    failure));
        }
    }

    @NonNull
    private JsonObject toReadPayload(@Nullable OfflineCachedDocument cached,
                                     @NonNull List<String> path) {
        if (cached == null || cached.getState() == OfflineCachedDocument.State.NO_DOCUMENT) {
            return Json.createObjectBuilder()
                    .add("ret", Json.createArrayBuilder().build())
                    .build();
        }

        JsonObject document = dataEncoder.encodeMap(cached.getData());
        jakarta.json.JsonObjectBuilder source = Json.createObjectBuilder()
                .add("DOCUMENT", document)
                .add("OID", lastPathSegment(path));
        if (!cached.getVersion().isEmpty()) {
            source.add("VERSION", cached.getVersion());
        }
        JsonObject record = Json.createObjectBuilder().add("osons", source.build()).build();
        return Json.createObjectBuilder()
                .add("ret", Json.createArrayBuilder().add(record).build())
                .build();
    }

    @NonNull
    private static String pathString(@NonNull List<String> path) {
        return String.join("/", path);
    }

    @NonNull
    private static String lastPathSegment(@NonNull List<String> path) {
        return path.isEmpty() ? "" : path.get(path.size() - 1);
    }

    @NonNull
    private static FusabaseOracledbException unsupported(@NonNull String message) {
        FusabaseLogger.w(TAG, message);
        return new FusabaseOracledbException(message,
                FusabaseOracledbException.Code.NOT_IMPLEMENTED);
    }

    @NonNull
    private static <TResult> CompletionStage<TResult> bridge(@NonNull Task<Void> task,
                                                               @Nullable TResult result) {
        CompletableFuture<TResult> completion = new CompletableFuture<>();
        task.addOnCompleteListener(completedTask -> {
            Exception failure = completedTask.getException();
            if (failure == null && completedTask.isSuccessful()) {
                completion.complete(result);
            } else {
                completion.completeExceptionally(failure == null
                        ? new FusabaseOracledbException(
                        "Offline mutation did not complete",
                        FusabaseOracledbException.Code.INTERNAL)
                        : failure);
            }
        });
        return completion;
    }

    @NonNull
    private static <TResult> CompletionStage<TResult> completed(@Nullable TResult result) {
        return CompletableFuture.completedFuture(result);
    }

    @NonNull
    private static <TResult> CompletionStage<TResult> failed(@NonNull Throwable failure) {
        CompletableFuture<TResult> completion = new CompletableFuture<>();
        completion.completeExceptionally(failure);
        return completion;
    }
}
