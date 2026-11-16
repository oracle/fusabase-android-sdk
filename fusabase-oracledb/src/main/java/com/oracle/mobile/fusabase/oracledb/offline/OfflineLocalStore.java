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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.oracledb.OfflineFieldValueEvaluator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Coordinates mutation batches, remote documents, and materialized user overlays. */
final class OfflineLocalStore {
    static final class WriteResult {
        final long batchId; final List<String> affectedPaths;
        WriteResult(long batchId, List<String> affectedPaths) {
            this.batchId = batchId; this.affectedPaths = affectedPaths;
        }
    }

    private final OfflineDatabase database;
    private final OfflineRemoteDocumentCache remoteDocuments;
    private final OfflineMutationQueue mutationQueue;
    private final OfflineOverlayCache overlays;
    private String userId;

    OfflineLocalStore(@NonNull OfflineDatabase database, @NonNull String userId) {
        this.database = database;
        this.remoteDocuments = new OfflineRemoteDocumentCache(database);
        this.mutationQueue = new OfflineMutationQueue();
        this.overlays = new OfflineOverlayCache();
        this.userId = userId;
    }

    void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    @NonNull WriteResult writeLocally(@NonNull List<OfflineMutation> mutations, @NonNull String source) {
        if (mutations.isEmpty()) throw new IllegalArgumentException("Mutations cannot be empty");
        return transaction(db -> {
            long batchId = nextBatchId(db);
            OfflineMutationBatch batch = new OfflineMutationBatch(userId, batchId,
                    System.currentTimeMillis(), source, mutations);
            mutationQueue.add(db, batch);
            List<String> paths = batch.getAffectedPaths();
            for (String path : paths) recomputeOverlay(db, path);
            return new WriteResult(batchId, paths);
        });
    }

    @Nullable OfflineCachedDocument getLocalDocument(@NonNull String path) {
        return transaction(db -> getLocalDocument(db, path));
    }

    @NonNull List<OfflineCachedDocument> getLocalDocumentsWithPrefix(@NonNull String pathPrefix) {
        return transaction(db -> {
            Map<String, OfflineCachedDocument> documents = new LinkedHashMap<>();
            for (OfflineCachedDocument document : remoteDocuments.getAllWithPrefix(db, pathPrefix)) {
                documents.put(document.getPath(), document);
            }
            for (OfflineOverlayCache.Overlay overlay : overlays.getAllWithPrefix(db, userId, pathPrefix)) {
                OfflineCachedDocument remote = remoteDocuments.get(db, overlay.path);
                documents.put(overlay.path, new OfflineCachedDocument(overlay.path, overlay.data,
                        remote == null ? "" : remote.getVersion(),
                        remote == null ? null : remote.getUpdateTimeMillis(),
                        remote == null ? null : remote.getCommitTimeMillis(),
                        remote == null ? null : remote.getReadTimeMillis(), overlay.state, true,
                        false, remote == null ? 0L : remote.getSequenceNumber()));
            }
            return new ArrayList<>(documents.values());
        });
    }

    @NonNull List<OfflineMutationBatch> getPendingBatches() {
        return transaction(db -> mutationQueue.getAll(db, userId));
    }

    void saveRemoteDocument(@NonNull OfflineCachedDocument document) {
        transaction(db -> { remoteDocuments.add(db, document); recomputeOverlay(db, document.getPath()); return null; });
    }

    void pruneRemoteDocuments(long maxBytes) {
        transaction(db -> { remoteDocuments.pruneToSize(db, maxBytes); return null; });
    }

    @NonNull List<String> acknowledgeBatch(long batchId) {
        return transaction(db -> {
            OfflineMutationBatch batch = findBatch(db, batchId);
            if (batch == null) return new ArrayList<>();
            List<String> paths = batch.getAffectedPaths();
            long now = System.currentTimeMillis();
            for (String path : paths) {
                // Commit only this batch, not overlays from later queued writes.
                OfflineCachedDocument localDocument = applyMutations(db, path,
                        java.util.Collections.singletonList(batch));
                if (localDocument == null) continue;
                remoteDocuments.add(db, new OfflineCachedDocument(path, localDocument.getData(),
                        localDocument.getVersion(), localDocument.getUpdateTimeMillis(), now, now,
                        localDocument.getState(), false, true, now));
            }
            mutationQueue.remove(db, userId, batchId);
            for (String path : paths) recomputeOverlay(db, path);
            return new ArrayList<>(paths);
        });
    }

    @NonNull List<String> rejectBatch(long batchId) {
        return removeBatchAndRecompute(batchId, true);
    }

    private List<String> removeBatchAndRecompute(long batchId, boolean rejectDependents) {
        return transaction(db -> {
            OfflineMutationBatch failed = findBatch(db, batchId);
            if (failed == null) return new ArrayList<>();
            Set<String> paths = new LinkedHashSet<>(failed.getAffectedPaths());
            List<OfflineMutationBatch> toRemove = new ArrayList<>(); toRemove.add(failed);
            if (rejectDependents) {
                for (OfflineMutationBatch candidate : mutationQueue.getAll(db, userId)) {
                    if (candidate.getBatchId() > batchId && intersects(paths, candidate.getAffectedPaths())) {
                        toRemove.add(candidate); paths.addAll(candidate.getAffectedPaths());
                    }
                }
            }
            for (OfflineMutationBatch batch : toRemove) mutationQueue.remove(db, userId, batch.getBatchId());
            for (String path : paths) recomputeOverlay(db, path);
            return new ArrayList<>(paths);
        });
    }

    private boolean intersects(Set<String> left, List<String> right) {
        for (String value : right) if (left.contains(value)) return true;
        return false;
    }

    @Nullable private OfflineMutationBatch findBatch(SQLiteDatabase db, long batchId) {
        for (OfflineMutationBatch batch : mutationQueue.getAll(db, userId)) {
            if (batch.getBatchId() == batchId) return batch;
        }
        return null;
    }

    @Nullable private OfflineCachedDocument getLocalDocument(SQLiteDatabase db, String path) {
        OfflineOverlayCache.Overlay overlay = overlays.get(db, userId, path);
        OfflineCachedDocument remote = remoteDocuments.get(db, path);
        if (overlay == null) return remote;
        return new OfflineCachedDocument(path, overlay.data,
                remote == null ? "" : remote.getVersion(),
                remote == null ? null : remote.getUpdateTimeMillis(),
                remote == null ? null : remote.getCommitTimeMillis(),
                remote == null ? null : remote.getReadTimeMillis(), overlay.state, true, false,
                remote == null ? 0L : remote.getSequenceNumber());
    }

    private void recomputeOverlay(SQLiteDatabase db, String path) {
        List<OfflineMutationBatch> batches = mutationQueue.getForDocument(db, userId, path);
        if (batches.isEmpty()) { overlays.remove(db, userId, path); return; }
        OfflineCachedDocument document = applyMutations(db, path, batches);
        overlays.set(db, new OfflineOverlayCache.Overlay(userId, path,
                batches.get(batches.size() - 1).getBatchId(), document.getData(), document.getState()));
    }

    private OfflineCachedDocument applyMutations(SQLiteDatabase db, String path,
                                                 List<OfflineMutationBatch> batches) {
        OfflineCachedDocument remote = remoteDocuments.get(db, path);
        Map<String, Object> data = remote == null || remote.getData() == null
                ? null : new LinkedHashMap<>(remote.getData());
        OfflineCachedDocument.State state = remote == null ? OfflineCachedDocument.State.NO_DOCUMENT : remote.getState();
        for (OfflineMutationBatch batch : batches) {
            for (OfflineMutation mutation : batch.getMutations()) {
                if (!path.equals(mutation.getPath())) continue;
                if (mutation.getType() == OfflineMutation.Type.DELETE) {
                    data = null; state = OfflineCachedDocument.State.NO_DOCUMENT;
                } else if (mutation.getType() == OfflineMutation.Type.SET) {
                    data = new LinkedHashMap<>(OfflineFieldValueEvaluator.materialize(
                            mutation.getData()));
                    state = OfflineCachedDocument.State.FOUND;
                } else {
                    if (data == null) data = new LinkedHashMap<>();
                    applyPatch(data, mutation.getData()); state = OfflineCachedDocument.State.FOUND;
                }
            }
        }
        return new OfflineCachedDocument(path, data,
                remote == null ? "" : remote.getVersion(),
                remote == null ? null : remote.getUpdateTimeMillis(),
                remote == null ? null : remote.getCommitTimeMillis(),
                remote == null ? null : remote.getReadTimeMillis(), state, false, false,
                remote == null ? 0L : remote.getSequenceNumber());
    }

    @SuppressWarnings("unchecked") private void applyPatch(Map<String, Object> target, Map<String, Object> patch) {
        for (Map.Entry<String, Object> entry : patch.entrySet()) {
            String[] segments = entry.getKey().split("\\."); Map<String, Object> current = target;
            for (int index = 0; index < segments.length - 1; index++) {
                Object child = current.get(segments[index]);
                if (!(child instanceof Map)) { child = new LinkedHashMap<String, Object>(); current.put(segments[index], child); }
                current = (Map<String, Object>) child;
            }
            OfflineFieldValueEvaluator.apply(current, segments[segments.length - 1],
                    entry.getValue());
        }
    }

    private long nextBatchId(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery("SELECT COALESCE(MAX(batch_id), 0) + 1 FROM "
                + OfflineDatabase.TABLE_MUTATIONS + " WHERE user_id = ?", new String[]{userId})) {
            cursor.moveToFirst(); return cursor.getLong(0);
        }
    }

    private <T> T transaction(OfflineDatabase.Transaction<T> operation) {
        try { return database.runTransaction(operation); }
        catch (Exception error) { throw new IllegalStateException("Offline local store operation failed", error); }
    }
}
