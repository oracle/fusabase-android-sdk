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

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Represents a query to retrieve documents from a collection in the Oracledb database.
 * Provides methods to filter, order, limit, and execute queries.
 */
public class Query extends Reference {

    /**
     * The Oracledb database instance.
     */
    public final FusabaseOracledb oradb;

    /**
     * Listener manager for snapshot listeners.
     */
    protected ListenerManager<Query, QuerySnapshot> listenerManager;

    /**
     * Explicit order by clauses.
     */
    final List<OrderBy> explicitOrder;

    /**
     * Join operations.
     */
    final List<String> joins;

    /**
     * Aggregate fields.
     */
    final List<AggregateField> aggregate;

    /**
     * Filter conditions.
     */
    final List<Filter> conditions;
    /**
     * Find-nearest field (v2 only).
     */
    String vectorSearchField;
    /**
     * Find-nearest query payload (v2 only).
     */
    FindNearestQuery findNearestQuery;
    /**
     * Find-nearest options (v2 only).
     */
    FindNearestOptions findNearestOptions;

    final boolean isColGroup;

    /**
     * Column names to select in the query.
     */
    final List<String> columns;
    /**
     * Real-time flag.
     */
    private int rt;

    /**
     * Limit on the number of results.
     */
    long limit;

    /**
     * Path segments.
     */
    List<String> path;

    /**
     * Collection ID.
     */
    final String collectionId;

    /**
     * Current query snapshot.
     */
    private QuerySnapshot querySnap;

    /**
     * Indicates if this is a duality view.
     */
    private final boolean isDualityView;

    /**
     * Enumeration representing different types of collections.
     */
    protected enum CollectionType {
        /**
         * Duality view collection.
         */
        DUALITY_VIEW_COLLECTION,
        /**
         * Standard collection.
         */
        COLLECTION
    }

    /**
     * Enumeration for query ordering direction.
     */
    public enum Direction {
        /**
         * Ascending order.
         */
        ASCENDING("asc"),

        /**
         * Descending order.
         */
        DESCENDING("desc");

        /**
         * String representation of the direction.
         */
        final String text;

        Direction(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * Constructs a Query instance.
     *
     * @param oracledb      the Oracledb database instance
     * @param path          the path string for the query
     * @param isDualityView whether this is a duality view query
     * @param isColGroup whether the query is for a collection group
     */
    protected Query(@NonNull FusabaseOracledb oracledb, @NonNull String path, boolean isDualityView, boolean isColGroup) {
        super(oracledb);
        this.oradb = oracledb;
        this.listenerManager = new ListenerManager<>(oradb, this);
        this.path = new ArrayList<>(Arrays.asList(path.split("/")));
        this.collectionId = this.path.get(this.path.size() - 1);
        this.columns = new ArrayList<>();
        this.aggregate = new ArrayList<>();
        this.conditions = new ArrayList<>();
        this.joins = new ArrayList<>();
        this.explicitOrder = new ArrayList<>();
        this.vectorSearchField = null;
        this.findNearestQuery = null;
        this.findNearestOptions = null;
        this.limit = 0;
        this.rt = 0;
        this.querySnap = null;
        this.isDualityView = isDualityView;
        this.isColGroup = isColGroup;
    }

    /**
     * Creates a deep copy of this query.
     *
     * @return a new Query instance with copied properties
     */
    @NonNull
    protected Query deepCopy() {
        Query newQuery = new Query(this.oradb, String.join("/", this.path), this.isDualityView, this.isColGroup);

        // Copy all mutable fields
        newQuery.explicitOrder.addAll(this.explicitOrder);
        newQuery.joins.addAll(this.joins);
        newQuery.aggregate.addAll(this.aggregate);
        newQuery.columns.addAll(this.columns);
        newQuery.limit = this.limit;
        newQuery.conditions.addAll(this.conditions);
        newQuery.vectorSearchField = this.vectorSearchField;
        newQuery.findNearestQuery = this.findNearestQuery;
        newQuery.findNearestOptions = this.findNearestOptions;
        return newQuery;
    }

    /**
     * Adds a snapshot listener to this query.
     *
     * @param listener the event listener for query snapshots
     * @return a listener registration object
     */
    @NonNull
    public ListenerRegistration addSnapshotListener(@NonNull EventListener<QuerySnapshot> listener) {
        return addSnapshotListener(new SnapshotListenOptions.Builder()
                .setExecutor(command -> new Handler(Looper.getMainLooper()).post(command))
                .build(),
                listener);
    }

    /**
     * Adds a snapshot listener scoped to an activity.
     *
     * @param activity the activity to scope the listener to
     * @param listener the event listener for query snapshots
     * @return a listener registration object
     */
    @NonNull
    public ListenerRegistration addSnapshotListener(@NonNull Activity activity,
                                                    @NonNull EventListener<QuerySnapshot> listener) {
        return addSnapshotListener(new SnapshotListenOptions.Builder()
                .setExecutor(command -> new Handler(Looper.getMainLooper()).post(command))
                .setActivity(activity)
                .build(),
                listener);
    }

    /**
     * Adds a snapshot listener with a custom executor.
     *
     * @param executor the executor to run the listener on
     * @param listener the event listener for query snapshots
     * @return a listener registration object
     */
    @NonNull
    public ListenerRegistration addSnapshotListener(@NonNull Executor executor,
                                                    @NonNull EventListener<QuerySnapshot> listener) {
        return addSnapshotListener(new SnapshotListenOptions.Builder().setExecutor(executor)
                .build(),
                listener);
    }

    /**
     * Adds a snapshot listener with metadata changes.
     *
     * @param metadataChanges the metadata changes to include
     * @param listener the event listener for query snapshots
     * @return a listener registration object
     */
    @NonNull
    public ListenerRegistration addSnapshotListener(@NonNull MetadataChanges metadataChanges,
                                                    @NonNull EventListener<QuerySnapshot> listener) {
        return addSnapshotListener(new SnapshotListenOptions.Builder()
                .setExecutor(command -> new Handler(Looper.getMainLooper()).post(command))
                .setMetadataChanges(metadataChanges).build(),
                listener);
    }

    /**
     * Adds a snapshot listener with custom options.
     *
     * @param options the snapshot listen options
     * @param listener the event listener for query snapshots
     * @return a listener registration object
     */
    @NonNull
    public ListenerRegistration addSnapshotListener(@NonNull SnapshotListenOptions options,
                                                    @NonNull EventListener<QuerySnapshot> listener) {
        return listenerManager.registerListener(listener, options);
    }

    /**
     * Adds a snapshot listener scoped to an activity with metadata changes.
     *
     * @param activity the activity to scope the listener to
     * @param metadataChanges the metadata changes to include
     * @param listener the event listener for query snapshots
     * @return a listener registration object
     */
    @NonNull
    public ListenerRegistration addSnapshotListener(
            @NonNull Activity activity,
            @NonNull MetadataChanges metadataChanges,
            @NonNull EventListener<QuerySnapshot> listener) {
        return addSnapshotListener(new SnapshotListenOptions.Builder()
                .setExecutor(command -> new Handler(Looper.getMainLooper()).post(command))
                .setActivity(activity)
                .setMetadataChanges(metadataChanges)
                .build(),
                listener);
    }

    /**
     * Adds a snapshot listener with custom executor and metadata changes.
     *
     * @param executor the executor to run the listener on
     * @param metadataChanges the metadata changes to include
     * @param listener the event listener for query snapshots
     * @return a listener registration object
     */
    @NonNull
    public ListenerRegistration addSnapshotListener(
            @NonNull Executor executor,
            @NonNull MetadataChanges metadataChanges,
            @NonNull EventListener<QuerySnapshot> listener) {
        return addSnapshotListener(new SnapshotListenOptions.Builder()
                .setExecutor(executor)
                .setMetadataChanges(metadataChanges)
                .build(),
                listener);
    }

    /**
     * Fetches a join result via this query.
     *
     * @param viewName name of the join
     * @return a new Query with the join applied
     */
    @NonNull
    protected Query join(@NonNull String viewName) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.joins.add(viewName);
        return updatedQuery;
    }

    /**
     * Creates an aggregate query.
     *
     * @param aggregateField the aggregate field
     * @param aggregateFields additional aggregate fields
     * @return an AggregateQuery
     */
    @NonNull
    public AggregateQuery aggregate(@NonNull AggregateField aggregateField,
                                    @NonNull AggregateField... aggregateFields) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.aggregate.add(aggregateField);
        Collections.addAll(updatedQuery.aggregate, aggregateFields);
        return new AggregateQuery(updatedQuery);
    }

    /**
     * Creates a count aggregate query.
     *
     * @param alias the alias for the count
     * @return an AggregateQuery
     */
    @NonNull
    public AggregateQuery count(@NonNull String alias) {
        Query updatedQuery = this.deepCopy();
        AggregateField aggregateField = new AggregateField.CountAggregateField(alias, new FieldPath(""));
        updatedQuery.aggregate.add(aggregateField);
        return new AggregateQuery(updatedQuery);
    }

    /**
     * Ends the query at the specified field values.
     *
     * @param fieldValues the field values to end at
     * @return a new Query with the end condition
     */
    @NonNull
    public Query endAt(Object... fieldValues) {
        Query updatedQuery = this.deepCopy();

        for (int i = 0; i < fieldValues.length; i++) {
            updatedQuery.conditions.add(Filter.lessThanOrEqualTo(updatedQuery.explicitOrder.get(i).getField(), fieldValues[i]));
        }
        return updatedQuery;
    }

    /**
     * Ends the query before the specified document snapshot.
     *
     * @param snapshot the document snapshot to end at
     * @return a new Query with the end condition
     */
    @NonNull
    public Query endAt(@NonNull DocumentSnapshot snapshot) {
        return this.endAt(snapshot.get(this.explicitOrder.get(0).getField()));
    }

    /**
     * Ends the query before the specified field values.
     *
     * @param fieldValues the field values to end before
     * @return a new Query with the end before condition
     */
    @NonNull
    public Query endBefore(Object... fieldValues) {
        Query updatedQuery = this.deepCopy();

        for (int i = 0; i < fieldValues.length; i++) {
            updatedQuery.conditions.add(Filter.lessThan(updatedQuery.explicitOrder.get(i).getField(), fieldValues[i]));
        }
        return updatedQuery;
    }

    /**
     * Ends the query before the specified document snapshot.
     *
     * @param snapshot the document snapshot to end before
     * @return a new Query with the end before condition
     */
    @NonNull
    public Query endBefore(@NonNull DocumentSnapshot snapshot) {
        return this.endBefore(snapshot.get(this.explicitOrder.get(0).getField()));
    }

    /**
     * Checks equality with another object.
     *
     * @param o the object to compare
     * @return true if equal, false otherwise
     */
    public boolean equals(Object o) {
        if (o instanceof Query) {
            Query query = (Query) o;
            return this.aggregate.equals(query.aggregate)
                    && this.joins.equals(query.joins)
                    && this.path.equals(query.path)
                    && this.limit == query.limit
                    && this.explicitOrder.equals(query.explicitOrder)
                    && this.conditions.equals(query.conditions)
                    && Objects.equals(this.vectorSearchField, query.vectorSearchField)
                    && Objects.equals(this.findNearestQuery, query.findNearestQuery)
                    && Objects.equals(this.findNearestOptions, query.findNearestOptions);
        }
        return false;
    }

    /**
     * Computes the hash code.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(aggregate, joins, path, limit, explicitOrder, conditions,
                vectorSearchField, findNearestQuery, findNearestOptions);
    }

    /**
     * Executes the query with default source.
     *
     * @return a Task resolving to the QuerySnapshot
     */
    public @NonNull Task<QuerySnapshot> get() {
        return get(Source.DEFAULT);
    }

    /**
     * Executes the query with the specified source.
     *
     * @param source the source to query from
     * @return a Task resolving to the QuerySnapshot
     */
    public @NonNull Task<QuerySnapshot> get(@NonNull Source source) {
        return get(source, false);
    }

    @NonNull
    protected Task<QuerySnapshot> get(@NonNull Source source, boolean snapshot) {
        TaskCompletionSource<QuerySnapshot> taskCompletionSource = new TaskCompletionSource<>();
        Task<QuerySnapshot> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            JsonObject result = null;

            try {
                result = this.queryHelper.fetchDocs(this.path,
                    this.getParams(this, snapshot),
                    new TransactionOperation());
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof FusabaseOracledbException))
                    exception = new FusabaseOracledbException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), FusabaseOracledbException.Code.INTERNAL);
                throw new CompletionException(exception);
            }

            this.querySnap = getSnapFromData(result);
            taskCompletionSource.setResult(this.querySnap);
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Gets the snapshot from notification data.
     *
     * @param changedDataNotification the notification data
     * @param currentSnap the current snapshot
     * @return the updated QuerySnapshot or null
     * @throws FusabaseOracledbException if an error occurs
     */
    @Nullable
    @Override
    protected QuerySnapshot getSnapshotFromNotificationData(@NonNull JsonObject changedDataNotification,
                                                            @NonNull Snapshot currentSnap) throws FusabaseOracledbException {
        JsonObject document = changedDataNotification.getJsonObject("changedData");
        QuerySnapshot currentDocSnap = (QuerySnapshot) currentSnap;

        // Check for stale updates
        for (QueryDocumentSnapshot queryDocSnap : currentDocSnap.getQueryDocumentSnapshots()) {
            Document queryDoc = queryDocSnap.getDocument();
            if (queryDoc != null && queryDoc.getRowId().equals(changedDataNotification.getString("rowId"))) {
                if (!document.isEmpty()) {
                    if (document.containsKey("VERSION") && queryDoc.getVersion().compareTo(document.getString("VERSION")) >= 0) {
                        return null;
                    }
                }
            }
        }
        return new QuerySnapshot(this, this.querySnap, changedDataNotification);
    }

    /**
     * Creates a QuerySnapshot from JSON data.
     *
     * @param document the JSON document
     * @return the QuerySnapshot
     */
    @NonNull
    public QuerySnapshot getSnapFromData(@NonNull JsonObject document) {
        JsonObject result = DataReader.addIndexToData(document);
        return new QuerySnapshot(this,
                this.querySnap,
                DataReader.getJsonObjectDataInMap(result));
    }

    /**
     * Gets the path of this query.
     *
     * @return the path as a string
     */
    @NonNull
    public String getPath() {
        return String.join("/", this.path);
    }

    /**
     * Gets the path segments.
     *
     * @return the list of path segments
     */
    @NonNull
    public List<String> getPathSegments() {
        return this.path;
    }

    /**
     * Gets the Oracledb instance.
     *
     * @return the Oracledb instance
     */
    @NonNull
    public FusabaseOracledb getOradb() {
        return this.oradb;
    }

    /**
     * Builds the parameters for the query.
     *
     * @param query the query to build params for
     * @return the JSON object with query parameters
     */
    @NonNull
    protected JsonObject getParams(@NonNull Query query, boolean snapshot) {

        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();

        if (this.isDualityView) {
            if (this.path != null && !this.path.isEmpty()) {
                payloadBuilder.add("dv_name", this.path.get(0));
                if (path.size() > 1) {
                    payloadBuilder.add("oid", this.path.get(1));
                }
            }
        } else if (this.isColGroup) {
                payloadBuilder.add("path", Json.createArrayBuilder().build());
                payloadBuilder.add("col_group", this.path.get(0));
        } else {
            if (this.path != null) {
                payloadBuilder.add("path", Json.createArrayBuilder(this.path));
            }
        }

        // Safely handle null collections
        JsonArrayBuilder conditionsBuilder = Json.createArrayBuilder();
        if (this.conditions != null) {
            this.conditions.stream()
                    .filter(Objects::nonNull)
                    .map(Filter::getJsonObject)
                    .forEach(conditionsBuilder::add);
        }
        payloadBuilder.add("conditions", conditionsBuilder.build());

        JsonArrayBuilder orderBuilder = Json.createArrayBuilder();
        if (this.explicitOrder != null) {
            this.explicitOrder.stream()
                    .filter(Objects::nonNull)
                    .map(OrderBy::getJsonObject)
                    .forEach(orderBuilder::add);
        }
        payloadBuilder.add("explicitOrder", orderBuilder.build());

        payloadBuilder.add("limit", this.limit)
                .add("rt", this.rt);

        JsonArrayBuilder aggregateBuilder = Json.createArrayBuilder();
        if (this.aggregate != null) {
            this.aggregate.stream()
                    .filter(Objects::nonNull)
                    .map(AggregateField::getJsonObject)
                    .forEach(aggregateBuilder::add);
        }
        payloadBuilder.add("aggregate", aggregateBuilder.build())
                .add("options", Json.createObjectBuilder().build());

        JsonArrayBuilder columnsBuilder = Json.createArrayBuilder();
        if (this.columns != null && !this.columns.isEmpty()) {
            this.columns.stream()
                    .filter(Objects::nonNull)
                    .forEach(columnsBuilder::add);
        }
        payloadBuilder.add("column", columnsBuilder.build());

        if(snapshot)
                payloadBuilder.add("snapshot", 1);

        if(!this.joins.isEmpty())
        {
            // Joins Query
            payloadBuilder.add("path", Json.createArrayBuilder());
            payloadBuilder.add("joins", Json.createObjectBuilder().add("view_name", this.joins.get(this.joins.size()-1)));
        }

        if ("2.0".equals(this.oradb.getApp().getOptions().getApiVersion())
                && this.vectorSearchField != null
                && this.findNearestQuery != null) {
            JsonObjectBuilder queryBuilder = Json.createObjectBuilder();
            if (this.findNearestQuery.getVector() != null) {
                JsonArrayBuilder dense = Json.createArrayBuilder();
                for (Double value : this.findNearestQuery.getVector()) {
                    dense.add(value);
                }
                queryBuilder.add("vector", dense);
            }
            if (this.findNearestQuery.getSparse() != null) {
                SparseVector sparseVector = this.findNearestQuery.getSparse();
                JsonArrayBuilder indices = Json.createArrayBuilder();
                for (Integer idx : sparseVector.getIndices()) {
                    indices.add(idx);
                }
                JsonArrayBuilder values = Json.createArrayBuilder();
                for (Double val : sparseVector.getValues()) {
                    values.add(val);
                }
                queryBuilder.add("sparse", Json.createObjectBuilder()
                        .add("type", "sparse")
                        .add("dimension", sparseVector.getDimension())
                        .add("indices", indices)
                        .add("values", values));
            }

            JsonObjectBuilder vectorSearchBuilder = Json.createObjectBuilder()
                    .add("field", this.vectorSearchField)
                    .add("query", queryBuilder);
            if (this.findNearestOptions != null) {
                if (this.findNearestOptions.getMetric() != null) {
                    vectorSearchBuilder.add("metric", this.findNearestOptions.getMetric().name());
                }
                if (this.findNearestOptions.getTopK() != null) {
                    vectorSearchBuilder.add("topK", this.findNearestOptions.getTopK());
                }
                if (this.findNearestOptions.getThreshold() != null) {
                    vectorSearchBuilder.add("threshold", this.findNearestOptions.getThreshold());
                }
            }
            payloadBuilder.add("vectorSearch", vectorSearchBuilder);
        }

        return payloadBuilder.build();
    }

    /**
     * Limits the query results.
     *
     * @param limit the maximum number of results
     * @return a new Query with the limit applied
     */
    @NonNull
    public Query limit(long limit) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.limit = limit;
        return updatedQuery;
    }



    /**
     * Adds a filter that checks if the specified field is null.
     *
     * @param field the field name
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereIsNull(@NonNull String field) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.isNull(field));
        return updatedQuery;
    }

    /**
     * Adds a filter that checks if the specified field is null.
     *
     * @param fieldPath the field path
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereIsNull(@NonNull FieldPath fieldPath) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.isNull(fieldPath));
        return updatedQuery;
    }

    /**
     * Limits to the last results, reversing the order.
     *
     * @param limit the maximum number of results
     * @return a new Query with limit to last applied
     */
    @NonNull
    public Query limitToLast(long limit) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.explicitOrder.forEach(orderBy -> orderBy.setDirection(
                orderBy.getDirection() == Direction.ASCENDING ? Direction.DESCENDING : Direction.ASCENDING));
        updatedQuery.limit = limit;
        return updatedQuery;
    }

    /**
     * Orders the query by a field.
     *
     * @param field the field to order by
     * @return a new Query with the order applied
     */
    @NonNull
    public Query orderBy(@NonNull String field) {
        return this.orderBy(new FieldPath(field));
    }

    /**
     * Orders the query by a field path.
     *
     * @param fieldPath the field path to order by
     * @return a new Query with the order applied
     */
    @NonNull
    public Query orderBy(@NonNull FieldPath fieldPath) {
        return orderBy(fieldPath, Direction.ASCENDING);
    }

    /**
     * Orders the query by a field with direction.
     *
     * @param field the field to order by
     * @param direction the direction to order by
     * @return a new Query with the order applied
     */
    @NonNull
    public Query orderBy(@NonNull String field, @NonNull Query.Direction direction) {
        return orderBy(new FieldPath(field), direction);
    }

    /**
     * Orders the query by a field path with direction.
     *
     * @param field the field path to order by
     * @param direction the direction to order by
     * @return a new Query with the order applied
     */
    @NonNull
    public Query orderBy(@NonNull FieldPath field, @NonNull Query.Direction direction) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.explicitOrder.add(new OrderBy(field, direction));
        return updatedQuery;
    }

    /**
     * Starts the query after the specified field values.
     *
     * @param fieldValues the field values to start after
     * @return a new Query with the start after condition
     */
    @NonNull
    public Query startAfter(Object... fieldValues) {
        Query updatedQuery = this.deepCopy();
        for (int i = 0; i < fieldValues.length; i++) {
            updatedQuery.conditions.add(Filter.greaterThan(updatedQuery.explicitOrder.get(i).getField(), fieldValues[i]));
        }
        return updatedQuery;
    }

    /**
     * Starts the query after the specified document snapshot.
     *
     * @param snapshot the document snapshot to start after
     * @return a new Query with the start after condition
     */
    @NonNull
    public Query startAfter(@NonNull DocumentSnapshot snapshot) {
        return this.startAfter(snapshot.get(this.explicitOrder.get(0).getField()));
    }

    /**
     * Starts the query at the specified field values.
     *
     * @param fieldValues the field values to start at
     * @return a new Query with the start at condition
     */
    @NonNull
    public Query startAt(Object... fieldValues) {
        Query updatedQuery = this.deepCopy();
        for (int i = 0; i < fieldValues.length; i++) {
            updatedQuery.conditions.add(Filter.greaterThanOrEqualTo(updatedQuery.explicitOrder.get(i).getField(), fieldValues[i]));
        }
        return updatedQuery;
    }

    /**
     * Starts the query at the specified document snapshot.
     *
     * @param snapshot the document snapshot to start at
     * @return a new Query with the start at condition
     */
    @NonNull
    public Query startAt(@NonNull DocumentSnapshot snapshot) {
        return this.startAt(snapshot.get(this.explicitOrder.get(0).getField()));
    }

    /**
     * Adds a where filter to the query.
     *
     * @param filter the filter to add
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query where(@NonNull Filter filter) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(filter);
        return updatedQuery;
    }

    /**
     * Adds a find-nearest query constraint for v2 runQuery.
     *
     * @param field vector field name
     * @param query dense or sparse find-nearest query payload
     * @param options optional metric/topK/threshold
     * @return a new Query with find-nearest applied
     */
    @NonNull
    public Query findNearest(@NonNull String field,
                             @NonNull FindNearestQuery query,
                             @Nullable FindNearestOptions options) {
        Objects.requireNonNull(field, "Invalid vector search field");
        Objects.requireNonNull(query, "Invalid vector search query");
        VectorValidators.validateVectorSearchQuery(query);
        VectorValidators.validateVectorSearchOptions(options);

        Query updatedQuery = this.deepCopy();
        updatedQuery.vectorSearchField = field;
        updatedQuery.findNearestQuery = query;
        updatedQuery.findNearestOptions = options;
        return updatedQuery;
    }

    /**
     * Adds a find-nearest query constraint for v2 runQuery.
     */
    @NonNull
    public Query findNearest(@NonNull String field,
                             @NonNull FindNearestQuery query) {
        return findNearest(field, query, null);
    }

    /**
     * Adds a where array contains filter.
     *
     * @param field the field
     * @param value the value
     * @return a new Query
     * @throws FusabaseOracledbException if combined with array-contains-any
     */
    @NonNull
    public Query whereArrayContains(@NonNull String field, @NonNull Object value) throws FusabaseOracledbException {
        for (Filter condition : this.conditions) {
            if (condition.getOperator() == Filter.Operator.ARRAY_CONTAINS_ANY) {
                throw new FusabaseOracledbException("Cannot combine array-contains with array-contains-any in the same query.", FusabaseOracledbException.Code.INVALID_ARGUMENT);
            }
        }
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.arrayContains(field, value));
        return updatedQuery;
    }

    /**
     * Adds a where array contains filter.
     *
     * @param fieldPath the field path
     * @param value the value
     * @return a new Query
     * @throws FusabaseOracledbException if combined with array-contains-any
     */
    @NonNull
    public Query whereArrayContains(@NonNull FieldPath fieldPath, @NonNull Object value) throws FusabaseOracledbException {
        // Check if array-contains-any is already used
        for (Filter condition : this.conditions) {
            if (condition.getOperator() == Filter.Operator.ARRAY_CONTAINS_ANY) {
                throw new FusabaseOracledbException("Cannot combine array-contains with array-contains-any in the same query.", FusabaseOracledbException.Code.INVALID_ARGUMENT);
            }
        }
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.arrayContains(fieldPath, value));
        return updatedQuery;
    }

    /**
     * Adds a where array contains any filter.
     *
     * @param field the field
     * @param values the values
     * @return a new Query
     * @throws FusabaseOracledbException if combined with array-contains
     */
    @NonNull
    public Query whereArrayContainsAny(@NonNull String field, @NonNull List<Object> values) throws FusabaseOracledbException {
        // Check if array-contains is already used
        for (Filter condition : this.conditions) {
            if (condition.getOperator() == Filter.Operator.ARRAY_CONTAINS) {
                throw new FusabaseOracledbException("Cannot combine array-contains with array-contains-any in the same query.", FusabaseOracledbException.Code.INVALID_ARGUMENT);
            }
        }
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.arrayContainsAny(field, values));
        return updatedQuery;
    }

    /**
     * Adds a where array contains any filter.
     *
     * @param fieldPath the field path
     * @param values the values
     * @return a new Query
     * @throws FusabaseOracledbException if combined with array-contains
     */
    @NonNull
    public Query whereArrayContainsAny(@NonNull FieldPath fieldPath, @NonNull List<Object> values) throws FusabaseOracledbException {
        // Check if array-contains is already used
        for (Filter condition : this.conditions) {
            if (condition.getOperator() == Filter.Operator.ARRAY_CONTAINS) {
                throw new FusabaseOracledbException("Cannot combine array-contains with array-contains-any in the same query.", FusabaseOracledbException.Code.INVALID_ARGUMENT);
            }
        }
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.arrayContainsAny(fieldPath, values));
        return updatedQuery;
    }

    /**
     * Adds a where equal to filter.
     *
     * @param field the field
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereEqualTo(@NonNull String field, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.equalTo(field, value));
        return updatedQuery;
    }

    /**
     * Adds a where equal to filter.
     *
     * @param fieldPath the field path
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereEqualTo(@NonNull FieldPath fieldPath, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.equalTo(fieldPath, value));
        return updatedQuery;
    }

    /**
     * Adds a where greater than filter.
     *
     * @param field the field
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereGreaterThan(@NonNull String field, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.greaterThan(field, value));
        return updatedQuery;
    }

    /**
     * Adds a where greater than filter.
     *
     * @param fieldPath the field path
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereGreaterThan(@NonNull FieldPath fieldPath, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.greaterThan(fieldPath, value));
        return updatedQuery;
    }

    /**
     * Adds a where greater than or equal to filter.
     *
     * @param field the field
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereGreaterThanOrEqualTo(@NonNull String field, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.greaterThanOrEqualTo(field, value));
        return updatedQuery;
    }

    /**
     * Adds a where greater than or equal to filter.
     *
     * @param fieldPath the field path
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereGreaterThanOrEqualTo(@NonNull FieldPath fieldPath, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.greaterThanOrEqualTo(fieldPath, value));
        return updatedQuery;
    }

    /**
     * Adds a where in filter.
     *
     * @param field the field
     * @param values the values
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereIn(@NonNull String field, @NonNull List<Object> values) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.inArray(field, values));
        return updatedQuery;
    }

    /**
     * Adds a where in filter.
     *
     * @param fieldPath the field path
     * @param values the values
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereIn(@NonNull FieldPath fieldPath, @NonNull List<Object> values) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.inArray(fieldPath, values));
        return updatedQuery;
    }

    /**
     * Adds a where less than filter.
     *
     * @param field the field
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereLessThan(@NonNull String field, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.lessThan(field, value));
        return updatedQuery;
    }

    /**
     * Adds a where less than filter.
     *
     * @param fieldPath the field path
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereLessThan(@NonNull FieldPath fieldPath, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.lessThan(fieldPath, value));
        return updatedQuery;
    }

    /**
     * Adds a where less than or equal to filter.
     *
     * @param field the field
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereLessThanOrEqualTo(@NonNull String field, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.lessThanOrEqualTo(field, value));
        return updatedQuery;
    }

    /**
     * Adds a where less than or equal to filter.
     *
     * @param fieldPath the field path
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereLessThanOrEqualTo(@NonNull FieldPath fieldPath, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.lessThanOrEqualTo(fieldPath, value));
        return updatedQuery;
    }

    /**
     * Adds a where like filter for pattern matching.
     *
     * @param field  the field name
     * @param value  the pattern to match against
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereLike(@NonNull String field, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.like(field, value));
        return updatedQuery;
    }

    /**
     * Adds a where like filter for pattern matching.
     *
     * @param fieldPath the field path
     * @param value     the pattern to match against
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereLike(@NonNull FieldPath fieldPath, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.like(fieldPath, value));
        return updatedQuery;
    }

    /**
     * Adds a where not equal to filter.
     *
     * @param field the field
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereNotEqualTo(@NonNull String field, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.notEqualTo(field, value));
        return updatedQuery;
    }

    /**
     * Adds a where not equal to filter.
     *
     * @param fieldPath the field path
     * @param value the value
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereNotEqualTo(@NonNull FieldPath fieldPath, @NonNull Object value) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.notEqualTo(fieldPath, value));
        return updatedQuery;
    }

    /**
     * Adds a where not in filter.
     *
     * @param field the field
     * @param values the values
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereNotIn(@NonNull String field, @NonNull List<Object> values) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.notInArray(field, values));
        return updatedQuery;
    }

    /**
     * Adds a where not in filter.
     *
     * @param fieldPath the field path
     * @param values the values
     * @return a new Query with the filter applied
     */
    @NonNull
    public Query whereNotIn(@NonNull FieldPath fieldPath, @NonNull List<Object> values) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.conditions.add(Filter.notInArray(fieldPath, values));
        return updatedQuery;
    }

    /**
     * Specifies columns to return in the query.
     *
     * @param fields the list of column names to select
     * @return a new Query with the specified columns
     */
    @NonNull
    public Query column(@NonNull List<String> fields) {
        Query updatedQuery = this.deepCopy();
        updatedQuery.columns.addAll(fields);
        return updatedQuery;
    }

    /**
     * Checks if this query is for a duality view.
     *
     * @return true if duality view, false otherwise
     */
    protected boolean isDualityView() {
        return isDualityView;
    }
}
