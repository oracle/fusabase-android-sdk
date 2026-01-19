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

package com.oracle.database.osbdt.oracledb;

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.TaskCompletionSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Represents a bulk update operation on documents in the Oracledb database.
 * Provides methods to filter documents and perform bulk updates.
 */
public class BulkUpdate {

    /**
     * Logging tag for this class.
     */
    private static final String TAG = "OsbdtOradb";

    /**
     * The Oracledb database instance.
     */
    protected final OsbdtOracledb oradb;

    /**
     * Query helper for performing operations.
     */
    protected QueryHelper queryHelper;

    /**
     * The path segments representing the collection's location.
     */
    protected final List<String> path;

    /**
     * Filter conditions for the bulk update.
     */
    private final List<Filter> conditions = new ArrayList<>();

    /**
     * List of operators used to track conflicts (e.g., array-contains vs array-contains-any).
     */
    private final List<String> ops = new ArrayList<>();

    /**
     * Creates a new BulkUpdate instance for the specified collection path.
     *
     * @param db   the Oracledb database instance
     * @param path the path to the collection
     */
    protected BulkUpdate(@NonNull OsbdtOracledb db, @NonNull String path) {
        Objects.requireNonNull(db, "Database cannot be null!");
        Objects.requireNonNull(path, "Path cannot be null!");
        this.oradb = db;
        this.queryHelper = new QueryHelper(db.getOracledbSettings(), db.getApp().getOptions(), db);
        String trimmedPath = path.trim();

        if (trimmedPath.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be empty!");
        }

        this.path = new ArrayList<>(Arrays.asList(trimmedPath.split("/")));

        if (this.path.size() % 2 != 1) {
            throw new IllegalArgumentException("Invalid path for bulk update. Path should point to a collection.");
        }
    }

    /**
     * Creates a deep copy of this BulkUpdate instance.
     *
     * @return a new BulkUpdate instance with copied properties
     */
    @NonNull
    private BulkUpdate deepCopy() {
        BulkUpdate newUpdate = new BulkUpdate(this.oradb, String.join("/", this.path));
        newUpdate.conditions.addAll(this.conditions);
        newUpdate.ops.addAll(this.ops);
        return newUpdate;
    }

    /**
     * Adds a where clause to filter documents for the bulk update.
     *
     * @param filter the filter to apply
     * @return a new BulkUpdate instance with the filter applied
     */
    @NonNull
    public BulkUpdate where(@NonNull Filter filter) {
        String opStr = filter.getOperator().toString();

        if (opStr.equals("array-contains-any")) {
            for (String existingOp : this.ops) {
                if (existingOp.equals("array-contains")) {
                    throw new IllegalArgumentException("array-contains-any can't be used with array-contains.");
                }
            }
        }
        if (opStr.equals("array-contains")) {
            for (String existingOp : this.ops) {
                if (existingOp.equals("array-contains-any")) {
                    throw new IllegalArgumentException("array-contains can't be used with array-contains-any.");
                }
            }
        }

        BulkUpdate newUpdate = this.deepCopy();
        newUpdate.conditions.add(filter);
        newUpdate.ops.add(opStr);

        return newUpdate;
    }

    /**
     * Performs the bulk update operation.
     *
     * @param data the data to update
     * @return a Task representing the asynchronous operation
     */
    @NonNull
    protected Task<Void> update(@NonNull Map<String, Object> data) {
        Objects.requireNonNull(data, "Invalid data provided");

        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {

                JsonObject params = Json.createObjectBuilder()
                    .add("conditions", getConditions())
                    .build();
                TransactionOperation operation = new TransactionOperation();
                this.queryHelper.updateDocs(this.path, data, null, operation, params);

            } catch (Exception e) {
                Exception exception = e;
                if (!(exception instanceof OsbdtOracledbException)) {
                    exception = new OsbdtOracledbException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), OsbdtOracledbException.Code.INTERNAL);
                }
                throw new CompletionException(exception);
            }
            taskCompletionSource.setResult(null);
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Builds the condition parameters for the bulk update request.
     */
    protected JsonObject getConditions() {
        JsonObjectBuilder paramsBuilder = Json.createObjectBuilder();

        JsonArrayBuilder conditionsBuilder = Json.createArrayBuilder();
        for (Filter condition : this.conditions) {
            conditionsBuilder.add(condition.getJsonObject());
        }
        paramsBuilder.add("conditions", conditionsBuilder.build());

        return paramsBuilder.build();
    }

    protected List<String> getPath () {
        return this.path;
    }

}
