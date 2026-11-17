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

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.offline.OfflineCachedDocument;
import com.oracle.mobile.fusabase.oracledb.offline.OfflineStoreManager;
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
 *
 * <p>Example:</p>
 * <pre>{@code
 * FusabaseOracledb db = FusabaseOracledb.getInstance();
 * CollectionReference recipes = db.collection("recipes");
 * Query query = recipes.whereEqualTo("category", "Drinks");
 * query.get();
 * }</pre>
 */
public class Query extends Reference {

    private static final String TAG = "FusabaseOradb";

    /**
     * The Oracledb database instance.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * String path = query.getPath();
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * query.get();
     * }</pre>
     */
    public enum Direction {
        /**
         * Ascending order.
         *
         * <p>Example:</p>
         * <pre>{@code
         * FusabaseOracledb db = FusabaseOracledb.getInstance();
         * CollectionReference recipes = db.collection("recipes");
         * Query query = recipes.whereEqualTo("category", "Drinks");
         * String path = query.getPath();
         * }</pre>
         */
        ASCENDING("asc"),

        /**
         * Descending order.
         *
         * <p>Example:</p>
         * <pre>{@code
         * FusabaseOracledb db = FusabaseOracledb.getInstance();
         * CollectionReference recipes = db.collection("recipes");
         * Query query = recipes.whereEqualTo("category", "Drinks");
         * String path = query.getPath();
         * }</pre>
         */
        DESCENDING("desc");

        /**
         * String representation of the direction.
         */
        final String text;

        Direction(String text) {
            this.text = text;
        }

        /**
         * Uses this Oracle Database API.
         *
         * <p>Example: Given compatible values named as shown.</p>
         * <pre>{@code
         * String text = value.toString();
         * }</pre>
         */@Override
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
        this.path = path.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(path.split("/")));
        this.collectionId = this.path.isEmpty() ? "" : this.path.get(this.path.size() - 1);
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * ListenerRegistration registration = query.addSnapshotListener((snapshot, error) -> { });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * ListenerRegistration registration = query.addSnapshotListener((snapshot, error) -> { });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * ListenerRegistration registration = query.addSnapshotListener((snapshot, error) -> { });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * ListenerRegistration registration = query.addSnapshotListener((snapshot, error) -> { });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * ListenerRegistration registration = query.addSnapshotListener((snapshot, error) -> { });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * ListenerRegistration registration = query.addSnapshotListener((snapshot, error) -> { });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * ListenerRegistration registration = query.addSnapshotListener((snapshot, error) -> { });
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * AggregateQuery aggregate = query.aggregate(AggregateField.count("recipeCount"));
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * AggregateQuery count = query.count("recipeCount");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query paged = query.endAt("Tea");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query paged = query.endAt("Tea");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query paged = query.endBefore("Tea");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query paged = query.endBefore("Tea");
     * }</pre>
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
     *
     * <p>Example: Given compatible values named as shown.</p>
     * <pre>{@code
     * boolean same = first.equals(second);
     * }</pre>
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
     *
     * <p>Example: Given compatible values named as shown.</p>
     * <pre>{@code
     * int hash = value.hashCode();
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * query.get();
     * }</pre>
     *
     * @return a Task resolving to the QuerySnapshot
     */
    public @NonNull Task<QuerySnapshot> get() {
        return get(Source.DEFAULT);
    }

    /**
     * Executes the query with the specified source.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * query.get();
     * }</pre>
     *
     * @param source the source to query from
     * @return a Task resolving to the QuerySnapshot
     */
    public @NonNull Task<QuerySnapshot> get(@NonNull Source source) {
        Objects.requireNonNull(source, "Invalid source provided");
        if (source == Source.CACHE) {
            return getFromLocalCache();
        }
        if (!oradb.isNetworkEnabled()) {
            return source == Source.DEFAULT ? getFromLocalCache() : oradb.networkDisabledTask();
        }
        return get(source, false);
    }

    @NonNull
    private Task<QuerySnapshot> getFromLocalCache() {
        TaskCompletionSource<QuerySnapshot> taskCompletionSource = new TaskCompletionSource<>();
        Task<QuerySnapshot> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                if (!joins.isEmpty() || !aggregate.isEmpty() || isDualityView ||
                        vectorSearchField != null || findNearestQuery != null) {
                    throw new FusabaseOracledbException("Query cache reads are not implemented for joins, " +
                            "aggregates, duality views, or vector search.",
                            FusabaseOracledbException.Code.NOT_IMPLEMENTED);
                }

                String prefix = isColGroup ? "" : String.join("/", path) + "/";
                List<OfflineCachedDocument> cachedDocuments =
                        OfflineStoreManager.getCachedDocumentsWithPrefix(oradb, prefix);
                this.querySnap = OfflineSnapshotFactory.fromCachedQuery(this, cachedDocuments);
                taskCompletionSource.setResult(this.querySnap);
            } catch (Exception e) {
                Exception exception = e;
                if (!(exception instanceof FusabaseOracledbException)) {
                    exception = new FusabaseOracledbException(exception.getMessage() == null ?
                            "Internal Error Occurred" : exception.getMessage(),
                            FusabaseOracledbException.Code.INTERNAL);
                }
                throw new CompletionException(exception);
            }
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
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
                // Match the JavaScript SDK: a failed collection/query request resolves to an
                // empty query snapshot rather than exposing the transport failure to a one-shot
                // query caller.
                FusabaseLogger.w(TAG, "Query request failed; returning an empty snapshot");
                result = Json.createObjectBuilder()
                        .add("ret", Json.createArrayBuilder())
                        .build();
            }

            this.querySnap = getSnapFromData(result);
            for (DocumentSnapshot documentSnapshot : this.querySnap.getDocuments()) {
                if (documentSnapshot.exists()) {
                    OfflineStoreManager.saveRemoteDocument(oradb,
                            documentSnapshot.getReference().getPath(),
                            documentSnapshot.getData() == null ? Collections.emptyMap() :
                                    documentSnapshot.getData(),
                            documentSnapshot.getVersion());
                }
            }
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
        QuerySnapshot currentDocSnap = (QuerySnapshot) currentSnap;
        String incomingVersion = RealtimeNotification.version(changedDataNotification);

        // Check for stale updates
        for (QueryDocumentSnapshot queryDocSnap : currentDocSnap.getQueryDocumentSnapshots()) {
            Document queryDoc = queryDocSnap.getDocument();
            if (queryDoc != null && queryDoc.getRowId().equals(changedDataNotification.getString("rowId"))) {
                if (RealtimeVersion.isIncomingOlder(queryDoc.getVersion(), incomingVersion)) {
                    return null;
                }
            }
        }
        QuerySnapshot snapshot = new QuerySnapshot(this, currentDocSnap, changedDataNotification);
        return snapshot.hasDocumentChanges() ? snapshot : null;
    }

    /**
     * Creates a QuerySnapshot from JSON data.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * String path = query.getPath();
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * String path = query.getPath();
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * String path = query.getPath();
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * String path = query.getPath();
     * }</pre>
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

        boolean isJoinQuery = !this.joins.isEmpty();
        payloadBuilder.add("limit", this.limit);
        if (isJoinQuery) {
            // Join queries are scoped by their view name rather than a collection path. Keep
            // this wire shape aligned with the JavaScript SDK and the v2 runQuery contract.
            payloadBuilder.add("col_group", "")
                    .add("snapshot", this.rt);
        } else {
            payloadBuilder.add("rt", this.rt);
        }

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

        if(snapshot && !isJoinQuery)
                payloadBuilder.add("snapshot", 1);

        if(isJoinQuery)
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query limited = query.limit(20);
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereIsNull("archivedAt");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereIsNull("archivedAt");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query limited = query.limitToLast(20);
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query ordered = query.orderBy("title");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query ordered = query.orderBy("title");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query ordered = query.orderBy("title");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query ordered = query.orderBy("title");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query paged = query.startAfter("Tea");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query paged = query.startAfter("Tea");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query paged = query.startAt("Tea");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query paged = query.startAt("Tea");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.where(Filter.equalTo("category", "Drinks"));
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query nearest = query.findNearest("embedding", FindNearestQuery.dense(Arrays.asList(0.22, 0.93)));
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query nearest = query.findNearest("embedding", FindNearestQuery.dense(Arrays.asList(0.22, 0.93)));
     * }</pre>
     */
    @NonNull
    public Query findNearest(@NonNull String field,
                             @NonNull FindNearestQuery query) {
        return findNearest(field, query, null);
    }

    /**
     * Adds a where array contains filter.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereArrayContains("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereArrayContains("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereArrayContainsAny("category", Arrays.asList("Drinks", "Desserts"));
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereArrayContainsAny("category", Arrays.asList("Drinks", "Desserts"));
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereEqualTo("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereEqualTo("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereGreaterThan("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereGreaterThan("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereGreaterThanOrEqualTo("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereGreaterThanOrEqualTo("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereIn("category", Arrays.asList("Drinks", "Desserts"));
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereIn("category", Arrays.asList("Drinks", "Desserts"));
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereLessThan("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereLessThan("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereLessThanOrEqualTo("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereLessThanOrEqualTo("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereLike("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereLike("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereNotEqualTo("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereNotEqualTo("category", "Drinks");
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereNotIn("category", Arrays.asList("Drinks", "Desserts"));
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * Query filtered = query.whereNotIn("category", Arrays.asList("Drinks", "Desserts"));
     * }</pre>
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
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * CollectionReference recipes = db.collection("recipes");
     * Query query = recipes.whereEqualTo("category", "Drinks");
     * String path = query.getPath();
     * }</pre>
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
