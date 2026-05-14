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

package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Represents an aggregate query that can be performed on Oracledb.
 */
public class AggregateQuery {

    /**
     * The underlying query object.
     */
    @NonNull
    public final Query query;

    /**
     * Helper for performing query operations.
     */
    private final QueryHelper queryHelper;

    /**
     * List of registered event listeners for query snapshots.
     */
    private final List<EventListener<QuerySnapshot>> registeredListeners;

    /**
     * List of explicit order clauses.
     */
    private final List<OrderBy> explicitOrder;

    /**
     * List of joins in the query.
     */
    private final List<String> joins;

    /**
     * List of aggregate fields.
     */
    private final List<AggregateField> aggregate;

    /**
     * List of filter conditions.
     */
    private final List<Filter> conditions;

    /**
     * Request type or version.
     */
    private int rt;

    /**
     * Maximum number of results to return.
     */
    private final long limit;

    /**
     * Path segments for the query.
     */
    private final List<String> path;

    /**
     * Constructs an AggregateQuery with the specified query.
     *
     * @param query the base query to aggregate
     */
    protected AggregateQuery(@NonNull Query query) {
        this.query = query;
        this.path = this.query.path;
        this.explicitOrder = this.query.explicitOrder;
        this.conditions = this.query.conditions;
        this.joins = this.query.joins;
        this.aggregate = this.query.aggregate;
        this.limit = this.query.limit;
        this.registeredListeners = new ArrayList<>();
        FusabaseOracledbSettings settings = this.query.getOradb().getOracledbSettings();
        this.queryHelper = new QueryHelper(settings,
                settings.getOptions(),
                query.oradb);
    }

    /**
     * Creates the parameters for the aggregate query as a JsonObject.
     *
     * @return the JsonObject containing query parameters
     */
    @NonNull
    private JsonObject getParams() {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();

        if (this.query.isDualityView()) {
            payloadBuilder.add("dv_name", this.path.get(0));
            if (this.path.size() > 1) {
                payloadBuilder.add("oid", this.path.get(1));
            }
        } else {
            payloadBuilder.add("path", Json.createArrayBuilder(this.path));
        }

        payloadBuilder.add("conditions", this.conditions.stream()
                        .map(Filter::getJsonObject)
                        .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                        .build())
                .add("explicitOrder", this.explicitOrder.stream()
                        .map(OrderBy::getJsonObject)
                        .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                        .build())
                .add("joins", this.joins.stream()
                        .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                        .build())
                .add("limit", this.limit)
                .add("rt", this.rt)
                .add("aggregate", this.aggregate.stream()
                        .map(AggregateField::getJsonObject)
                        .collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)
                        .build())
                .add("col_group", "")
                .add("options", Json.createObjectBuilder().build());

        // Query paths may contain sensitive identifiers; do not log them.
        FusabaseLogger.d("oracledb", "AggregateQuery params built");
        return payloadBuilder.build();
    }

    /**
     * Compares this AggregateQuery to the specified object for equality.
     *
     * @param object the object to compare
     * @return true if the objects are equal, false otherwise
     */
    public boolean equals(Object object) {
        if (!(object instanceof AggregateQuery)) {
            return false;
        }

        AggregateQuery other = (AggregateQuery) object;
        return query.equals(other.query) &&
                aggregate.equals(other.aggregate) &&
                registeredListeners.equals(other.registeredListeners) &&
                explicitOrder.equals(other.explicitOrder) &&
                conditions.equals(other.conditions) &&
                joins.equals(other.joins) &&
                limit == other.limit &&
                rt == other.rt &&
                path.equals(other.path);
    }

    /**
     * Executes the aggregate query and returns the result from the server.
     *
     * @return a task that resolves to the aggregate query snapshot
     */
    @NonNull
    public Task<AggregateQuerySnapshot> get() {
        return this.get(AggregateSource.SERVER);
    }

    /**
     * Executes the prepared query and gets its result from the specified source.
     *
     * @param source the source from which the result is to be fetched
     * @return a task that resolves to the aggregate query snapshot
     */
    @NonNull
    public Task<AggregateQuerySnapshot> get(@NonNull AggregateSource source) {
        Objects.requireNonNull(source, "Invalid source provided");
        TaskCompletionSource<AggregateQuerySnapshot> taskCompletionSource = new TaskCompletionSource<>();
        Task<AggregateQuerySnapshot> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            JsonObject result = null;
            try {
                result = this.queryHelper.fetchDocs(this.query.path,
                        this.getParams(),
                        new TransactionOperation());
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof FusabaseOracledbException))
                    exception = new FusabaseOracledbException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), FusabaseOracledbException.Code.INTERNAL);
                throw new CompletionException(exception);
            }

            result = result.getJsonArray("ret").getJsonObject(0).getJsonObject("result");
            taskCompletionSource.setResult(new AggregateQuerySnapshot(this, DataReader.getJsonObjectDataInMap(result)));
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Gets a Query instance from this AggregateQuery.
     *
     * @return the underlying query
     */
    @NonNull
    public Query getQuery() {
        return this.query;
    }

    /**
     * Returns a hash code value for this AggregateQuery.
     *
     * @return the hash code
     */
    public int hashCode() {
        return Objects.hash(query,
                explicitOrder,
                joins,
                aggregate,
                registeredListeners,
                limit,
                rt,
                path,
                conditions);
    }
}
