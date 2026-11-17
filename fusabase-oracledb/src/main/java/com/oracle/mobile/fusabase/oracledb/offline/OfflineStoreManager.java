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


package com.oracle.mobile.fusabase.oracledb.offline;

import com.oracle.mobile.fusabase.internal.AuthInterop;
import com.oracle.mobile.fusabase.internal.AuthInteropRegistry;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.oracledb.OfflineEventBus;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.offline.OfflineReplayRegistry;
import com.oracle.mobile.fusabase.oracledb.internal.offline.RemoteMutationExecutor;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Owns the offline component graph for each OracleDB client. */
public final class OfflineStoreManager {
    private static final String TAG = "OfflineStoreManager";
    private static final String ANONYMOUS_USER = "anonymous";
    private static final Map<FusabaseOracledb, OfflineClient> CLIENTS = new WeakHashMap<>();
    private OfflineStoreManager() { }
    public static synchronized void start(FusabaseOracledb db) {
        if (!db.getOracledbSettings().isPersistentCacheEnabled() || CLIENTS.containsKey(db)) return;
        OfflineDatabase database = new OfflineDatabase(db.getApp().getApplicationContext(),
                getDatabaseName(db));
        OfflineLocalStore localStore = new OfflineLocalStore(database, getCurrentUserId(db));
        RemoteMutationExecutor executor = OfflineReplayRegistry.get(db);
        CLIENTS.put(db, new OfflineClient(database, localStore,
                new OfflineRemoteStore(db, localStore, executor)));
        FusabaseLogger.i(TAG, "Started offline persistence for client");
    }
    public static synchronized boolean isEnabled(@NonNull FusabaseOracledb db) {
        start(db);
        return CLIENTS.containsKey(db);
    }
    public static synchronized void enableNetwork(FusabaseOracledb db) {
        OfflineClient client = CLIENTS.get(db);
        if (client != null) {
            FusabaseLogger.i(TAG, "Resuming offline mutation replay");
            client.remoteStore.resumeNetwork();
        }
    }
    public static synchronized void disableNetwork(FusabaseOracledb db) {
        OfflineClient client = CLIENTS.get(db);
        if (client != null) {
            FusabaseLogger.i(TAG, "Pausing offline mutation replay");
            client.remoteStore.pauseNetwork();
        }
    }
    public static synchronized void terminate(FusabaseOracledb db) {
        OfflineClient client = CLIENTS.remove(db);
        OfflineReplayRegistry.remove(db);
        if (client != null) {
            client.remoteStore.close();
            client.database.close();
            FusabaseLogger.i(TAG, "Terminated offline persistence for client");
        }
    }
    public static synchronized void clearPersistence(FusabaseOracledb db) {
        terminate(db);
        db.getApp().getApplicationContext().deleteDatabase(getDatabaseName(db));
    }
    /**
     * Retained for source and binary compatibility with existing operation classes.
     * Replay now uses an internal executor and never enters public write APIs.
     */
    public static boolean isReplaying() { return false; }
    public static Task<Void> enqueueSet(FusabaseOracledb db, String path, Map<String, Object> data, boolean merge) {
        return enqueue(db, Collections.singletonList(new OfflineMutation(
                merge ? OfflineMutation.Type.PATCH : OfflineMutation.Type.SET, path, data)),
                "write");
    }
    public static Task<Void> enqueuePatch(FusabaseOracledb db, String path, Map<String, Object> data) {
        return enqueue(db, Collections.singletonList(new OfflineMutation(
                OfflineMutation.Type.PATCH, path, data)), "write");
    }
    public static Task<Void> enqueueDelete(FusabaseOracledb db, String path) {
        return enqueue(db, Collections.singletonList(new OfflineMutation(
                OfflineMutation.Type.DELETE, path, null)), "write");
    }
    public static Task<Void> enqueueBatch(FusabaseOracledb db, List<OfflineMutation> mutations) {
        return enqueue(db, mutations, "writeBatch");
    }

    @Nullable
    public static synchronized OfflineCachedDocument getCachedDocument(
            @NonNull FusabaseOracledb db, @NonNull String path) {
        OfflineClient client = CLIENTS.get(db);
        if (client == null) {
            start(db);
            client = CLIENTS.get(db);
        }
        updateCurrentUser(db, client);
        return client == null ? null : client.localStore.getLocalDocument(path);
    }

    @NonNull
    public static synchronized List<OfflineCachedDocument> getCachedDocumentsWithPrefix(
            @NonNull FusabaseOracledb db, @NonNull String pathPrefix) {
        OfflineClient client = CLIENTS.get(db);
        if (client == null) {
            start(db);
            client = CLIENTS.get(db);
        }
        updateCurrentUser(db, client);
        return client == null ? Collections.emptyList() :
                client.localStore.getLocalDocumentsWithPrefix(pathPrefix);
    }

    public static synchronized void saveRemoteDocument(@NonNull FusabaseOracledb db,
                                                       @NonNull String path,
                                                       @NonNull Map<String, Object> data,
                                                       @NonNull String version) {
        OfflineClient client = CLIENTS.get(db);
        if (client == null) {
            start(db);
            client = CLIENTS.get(db);
        }
        if (client == null) {
            return;
        }
        updateCurrentUser(db, client);
        long now = System.currentTimeMillis();
        client.localStore.saveRemoteDocument(new OfflineCachedDocument(path, data, version,
                now, now, now, OfflineCachedDocument.State.FOUND, false, false, now));
        pruneRemoteDocuments(db, client);
    }

    public static synchronized void saveNoDocument(@NonNull FusabaseOracledb db,
                                                   @NonNull String path) {
        OfflineClient client = CLIENTS.get(db);
        if (client == null) {
            start(db);
            client = CLIENTS.get(db);
        }
        if (client == null) {
            return;
        }
        updateCurrentUser(db, client);
        long now = System.currentTimeMillis();
        client.localStore.saveRemoteDocument(new OfflineCachedDocument(path, null, "", null,
                null, now, OfflineCachedDocument.State.NO_DOCUMENT, false, false, now));
        pruneRemoteDocuments(db, client);
    }

    private static synchronized Task<Void> enqueue(FusabaseOracledb db,
                                                   List<OfflineMutation> mutations,
                                                   String sourceName) {
        OfflineClient client = CLIENTS.get(db);
        if (client == null) {
            try {
                start(db);
                client = CLIENTS.get(db);
            } catch (RuntimeException failure) {
                FusabaseLogger.e(TAG, "Offline persistence could not be initialized");
                return failedTask(new FusabaseOracledbException(
                        "Offline persistence initialization failed",
                        FusabaseOracledbException.Code.INTERNAL,
                        failure));
            }
        }
        if (client == null) {
            return failedTask(new FusabaseOracledbException(
                    "Offline persistence is disabled",
                    FusabaseOracledbException.Code.FAILED_PRECONDITION));
        }
        updateCurrentUser(db, client);
        TaskCompletionSource<Void> source = new TaskCompletionSource<>(); final OfflineClient target = client;
        source.handleFuture(CompletableFuture.supplyAsync(() -> {
            try {
                FusabaseLogger.v(TAG, "Persisting offline mutation batch");
                OfflineLocalStore.WriteResult result = target.localStore.writeLocally(mutations,
                        sourceName);
                OfflineEventBus.notifyLocalChanges(db, result.affectedPaths);
                if (db.isNetworkEnabled()) {
                    target.remoteStore.resumeNetwork();
                }
                FusabaseLogger.d(TAG, "Offline mutation batch persisted locally");
                return null;
            } catch (RuntimeException failure) {
                FusabaseLogger.e(TAG, "Offline mutation batch could not be persisted");
                throw new CompletionException(new FusabaseOracledbException(
                        "Offline persistence failed",
                        FusabaseOracledbException.Code.INTERNAL,
                        failure));
            }
        }));
        return source.getTask();
    }

    @NonNull
    private static <T> Task<T> failedTask(@NonNull Exception exception) {
        TaskCompletionSource<T> source = new TaskCompletionSource<>();
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(new CompletionException(exception));
        source.handleFuture(future);
        return source.getTask();
    }
    private static String getDatabaseName(FusabaseOracledb db) {
        return "fusabase-oracledb-" + db.getApp().getOptions().getAppId() + ".db";
    }
    private static void updateCurrentUser(FusabaseOracledb db, OfflineClient client) {
        if (client != null) client.localStore.setUserId(getCurrentUserId(db));
    }
    private static void pruneRemoteDocuments(FusabaseOracledb db, OfflineClient client) {
        if (client != null) {
            client.localStore.pruneRemoteDocuments(db.getOracledbSettings().getCacheSizeBytes());
        }
    }
    private static String getCurrentUserId(FusabaseOracledb db) {
        AuthInterop auth = AuthInteropRegistry.get(db.getApp());
        if (auth == null) {
            return ANONYMOUS_USER;
        }
        try {
            String userId = auth.getCurrentUserId();
            return userId == null || userId.trim().isEmpty() ? ANONYMOUS_USER : userId;
        } catch (RuntimeException error) {
            return ANONYMOUS_USER;
        }
    }
    private static final class OfflineClient {
        final OfflineDatabase database; final OfflineRemoteStore remoteStore; final OfflineLocalStore localStore;
        OfflineClient(OfflineDatabase database, OfflineLocalStore localStore, OfflineRemoteStore remoteStore) { this.database = database; this.localStore = localStore; this.remoteStore = remoteStore; }
    }
}
