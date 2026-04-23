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

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The main entry point for Oracle Database operations in the FUSABASE (Oracle Services Backend for
 * Database Transactions) SDK. This class provides access to database references, queries,
 * transactions, and batch operations. It serves as the central hub for all database interactions,
 * managing connections and coordinating various database operations.
 *
 * <p>This class follows a singleton pattern per application instance and provides factory methods
 * for creating document and collection references, executing queries, and performing transactions.</p>
 */
public class FusabaseOracledb {

    /** Maximum number of attempts for a single request */
    private static final int REQUEST_MAX_ATTEMPT = 1;

    /** The associated FUSABASE application instance */
    private final FusabaseApp app;

    /** Legacy static instance (deprecated) */
    private static FusabaseOracledb instance;

    /** Map of application name to FusabaseOracledb instances for singleton pattern */
    private static Map<String, FusabaseOracledb> INSTANCES = new HashMap<>();

    /** Secure random number generator for delays */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Settings for this database instance */
    private FusabaseOracledbSettings settings;

    /** Flag indicating if logging is enabled */
    private boolean loggingEnabled;

    /**
     * Constructs a new FusabaseOracledb instance for the given application.
     *
     * @param app The FUSABASE application instance
     */
    FusabaseOracledb(FusabaseApp app) {
        this.app = app;
        this.settings = new FusabaseOracledbSettings(app.getOptions(),
                29,
                REQUEST_MAX_ATTEMPT);
    }

    /**
     * Creates a new WriteBatch instance for performing batch write operations.
     * A WriteBatch can be used to perform multiple write operations atomically.
     *
     * @return A new WriteBatch instance
     */
    @NonNull
    public WriteBatch batch() {
        return new WriteBatch();
    }

    /**
     * Creates and returns a reference to a collection in the database.
     *
     * @param collectionPath The path of the collection
     * @return A CollectionReference pointing to the specified collection
     */
    @NonNull
    public CollectionReference collection(@NonNull String collectionPath) {
        return new CollectionReference(collectionPath, null, this);
    }

    /**
     * Creates a join query for the specified view name.
     * This method creates a query that can perform joins with duality views.
     *
     * @param viewName The name of the view to join with
     * @return A Query instance configured for the join operation
     */
    @NonNull
    public Query join(@NonNull String viewName) {
        Query joinQuery = new Query(this, "", false, false);
        return joinQuery.join(viewName);
    }

    /**
     * Creates and returns a reference to a duality view collection in the database.
     * Duality views provide a relational interface to document collections.
     *
     * @param collectionPath The path of the duality view collection
     * @return A DualityViewColReference pointing to the specified duality view collection
     */
    @NonNull
    public DualityViewColReference dualityViewCollection(@NonNull String collectionPath) {
        return new DualityViewColReference(collectionPath, this);
    }

    /**
     * Creates a reference to a collection group for cross-collection queries.
     * Collection groups allow querying across multiple collections with the same ID.
     *
     * @param collectionId The ID of the collection group
     * @return A Query instance for the collection group
     */
    @NonNull
    public Query collectionGroup(@NonNull String collectionId) {
        return new Query(this, collectionId, false, true);
    }

    /**
     * Creates and returns a reference to a document in the database.
     *
     * @param documentPath The path of the document
     * @return A DocumentReference pointing to the specified document
     */
    @NonNull
    public DocumentReference document(@NonNull String documentPath) {
        return new DocumentReference(documentPath, null, this, true);
    }

    /**
     * Creates and returns a reference to a duality view document in the database.
     * Duality views provide a relational interface to document collections.
     *
     * @param documentPath The path of the duality view document
     * @return A DualityViewDocReference pointing to the specified duality view document
     */
    @NonNull
    public DualityViewDocReference dualityViewDocument(@NonNull String documentPath) {
        return new DualityViewDocReference(documentPath, this, true);
    }

    /**
     * Returns the FUSABASE application instance associated with this database.
     *
     * @return The associated FusabaseApp instance
     */
    @NonNull
    public FusabaseApp getApp() {
        return this.app;
    }

    /**
     * Returns the settings for this database instance.
     *
     * @return The FusabaseOracledbSettings for this instance
     */
    @NonNull
    public FusabaseOracledbSettings getOracledbSettings() {
        return this.settings;
    }

    /**
     * Returns the default FusabaseOracledb instance for the default application.
     * This method uses the default FusabaseApp instance.
     *
     * @return The default FusabaseOracledb instance
     */
    @NonNull
    public static FusabaseOracledb getInstance() {
        return getInstance(FusabaseApp.getInstance());
    }

    /**
     * Returns the FusabaseOracledb instance for the specified application.
     * Uses singleton pattern - creates a new instance if one doesn't exist for the app.
     *
     * @param app The FUSABASE application instance
     * @return The FusabaseOracledb instance for the specified application
     */
    @NonNull
    public static FusabaseOracledb getInstance(@NonNull FusabaseApp app) {
        if (INSTANCES.get(app.getName()) != null) {
            return INSTANCES.get(app.getName());
        }
        INSTANCES.put(app.getName(), new FusabaseOracledb(app));
        return getInstance(app);
    }

    /**
     * Executes the provided update function and commits all write operations within it
     * as a single atomic transaction using default transaction options.
     *
     * @param updateFunction The function containing the transaction operations
     * @param <TResult> The result type returned by the transaction function
     * @return A Task that completes with the transaction result
     */
    @NonNull
    public <TResult> Task<TResult> runTransaction(@NonNull Transaction.Function<TResult> updateFunction) {
        return runTransaction(new TransactionOptions.Builder().build(), updateFunction);
    }

    /**
     * Executes the provided update function and commits all write operations within it
     * as a single atomic transaction with the specified transaction options.
     *
     * @param options The transaction options to use
     * @param updateFunction The function containing the transaction operations
     * @param <TResult> The result type returned by the transaction function
     * @return A Task that completes with the transaction result
     */
    @NonNull
    public <TResult> Task<TResult> runTransaction(@NonNull TransactionOptions options,
                                                  @NonNull Transaction.Function<TResult> updateFunction) {
        Objects.requireNonNull(options, "TransactionOptions cannot be null");
        Objects.requireNonNull(updateFunction, "Transaction function cannot be null");
        TaskCompletionSource<TResult> taskCompletionSource = new TaskCompletionSource<>();
        Task<TResult> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            TResult res = null;
            try {
                int retries = options.maxAttempts;
                while(retries > 0) {
                    try {
                        Transaction transaction = new Transaction(this, options);
                        res = updateFunction.apply(transaction);
                        transaction.apply();
                        break;
                    } catch (Exception e)
                    {
                        FusabaseLogger.d("Exception Caught" + e);
                        if(e instanceof FusabaseOracledbException &&
                                e.getMessage() != null &&
                                !e.getMessage().isEmpty() &&
                                e.getMessage().contains("OID is null"))
                        {
                            retries--;
                            if (retries > 0) {
                                try {
                                    // Add random delay between 0-3 seconds to avoid collision
                                    long delay = (long) (SECURE_RANDOM.nextDouble() * 3000);
                                    FusabaseLogger.d("Retrying with delay " + delay);
                                    CountDownLatch latch = new CountDownLatch(1);
                                    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                                    executor.schedule(latch::countDown, delay, TimeUnit.MILLISECONDS);
                                    latch.await();
                                    executor.shutdown();
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            FusabaseLogger.d("Executing again");
                        } else {
                            throw e;
                        }
                    }
                }
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof FusabaseOracledbException))
                    exception = new FusabaseOracledbException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), FusabaseOracledbException.Code.INTERNAL);
                throw new CompletionException(exception);
            } catch (Throwable th) {
                Exception exception = new FusabaseException(th.getMessage() == null ?
                        "Exception Occured while running transaction." :
                        th.getMessage());
                throw new CompletionException(exception);
            }
            taskCompletionSource.setResult(res);
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Updates the database settings for this instance.
     *
     * @param settings The new settings to apply to this database instance
     */
    public void setOracledbSettings(@NonNull FusabaseOracledbSettings settings) {
        this.settings = settings;
    }

    /**
     * Returns a {@link BulkUpdate} instance for the specified collection path.
     * This allows developers to perform bulk update operations with a where clause.
     *
     * @param path the collection path on which the bulk update will be applied
     * @return a BulkUpdate object to configure and execute the update
     */
    @NonNull
    public BulkUpdate bulkUpdate(@NonNull String path) {
        return new BulkUpdate(this, path);
    }
}
