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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.oracledb.OfflineEventBus;
import com.oracle.mobile.fusabase.oracledb.internal.error.ReplayAction;
import com.oracle.mobile.fusabase.oracledb.internal.error.ReplayErrorPolicy;
import com.oracle.mobile.fusabase.oracledb.internal.error.ReplayPolicy;
import com.oracle.mobile.fusabase.oracledb.internal.offline.RemoteMutationExecutor;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.Objects;

/** Replays locally persisted mutation batches to ORDS in batch order. */
final class OfflineRemoteStore {
    private static final String TAG = "OfflineRemoteStore";
    private static final int REPLAY_MAX_ATTEMPTS = 5;
    private final FusabaseOracledb oracledb;
    private final OfflineLocalStore localStore;
    private final ReplayPolicy replayPolicy;
    private volatile RemoteMutationExecutor remoteMutationExecutor;
    private volatile boolean networkEnabled = true;
    private boolean draining;
    private long resumeGeneration;

    OfflineRemoteStore(@NonNull FusabaseOracledb oracledb,
                       @NonNull OfflineLocalStore localStore,
                       @Nullable RemoteMutationExecutor remoteMutationExecutor) {
        this(oracledb, localStore, remoteMutationExecutor, new ReplayErrorPolicy());
    }

    OfflineRemoteStore(@NonNull FusabaseOracledb oracledb,
                       @NonNull OfflineLocalStore localStore,
                       @Nullable RemoteMutationExecutor remoteMutationExecutor,
                       @NonNull ReplayPolicy replayPolicy) {
        this.oracledb = oracledb;
        this.localStore = localStore;
        this.remoteMutationExecutor = remoteMutationExecutor;
        this.replayPolicy = Objects.requireNonNull(replayPolicy, "Replay policy cannot be null");
    }

    @NonNull synchronized CompletableFuture<Void> resumeNetwork() {
        networkEnabled = true;
        resumeGeneration++;
        FusabaseLogger.v(TAG, "Offline replay drain requested");
        return drain();
    }

    synchronized void pauseNetwork() {
        networkEnabled = false;
        FusabaseLogger.d(TAG, "Offline replay drain paused");
    }

    synchronized void close() {
        networkEnabled = false;
        remoteMutationExecutor = null;
        FusabaseLogger.d(TAG, "Offline replay store closed");
    }

    @NonNull synchronized CompletableFuture<Void> drain() {
        if (draining || !networkEnabled || !oracledb.isNetworkEnabled() ||
                remoteMutationExecutor == null) {
            if (remoteMutationExecutor == null) {
                FusabaseLogger.w(TAG, "Offline replay deferred until its executor is attached");
            }
            return CompletableFuture.completedFuture(null);
        }
        draining = true;
        final long generation = resumeGeneration;
        FusabaseLogger.d(TAG, "Starting offline mutation drain");
        return CompletableFuture.runAsync(() -> {
            try {
                while (networkEnabled && oracledb.isNetworkEnabled()) {
                    OfflineMutationBatch batch = localStore.getPendingBatches().stream().findFirst().orElse(null);
                    if (batch == null) {
                        FusabaseLogger.v(TAG, "Offline mutation drain found no pending batches");
                        return;
                    }
                    FusabaseLogger.v(TAG, "Replaying next offline mutation batch");
                    try {
                        replay(batch);
                        OfflineEventBus.notifyLocalChanges(oracledb,
                                localStore.acknowledgeBatch(batch.getBatchId()));
                        FusabaseLogger.i(TAG, "Acknowledged offline mutation batch");
                    } catch (Exception error) {
                        ReplayAction action = replayPolicy.classify(error);
                        if (action == ReplayAction.REJECT) {
                            FusabaseLogger.w(TAG, "Rejecting terminal offline mutation batch "
                                    + batch.getBatchId());
                            OfflineEventBus.notifyLocalChanges(oracledb,
                                    localStore.rejectBatch(batch.getBatchId()));
                        } else if (action == ReplayAction.RETRY) {
                            FusabaseLogger.w(TAG, "Pausing offline mutation drain after retryable failure");
                            return;
                        } else {
                            FusabaseLogger.w(TAG, "Acknowledging offline mutation batch after executor response");
                            OfflineEventBus.notifyLocalChanges(oracledb,
                                    localStore.acknowledgeBatch(batch.getBatchId()));
                        }
                    }
                }
            } finally {
                synchronized (OfflineRemoteStore.this) {
                    draining = false;
                    // A reconnect or enqueue during an active drain must not lose its wake-up.
                    if (resumeGeneration != generation) drain();
                }
            }
        });
    }

    private void replay(OfflineMutationBatch batch) throws Exception {
        RemoteMutationExecutor executor = remoteMutationExecutor;
        if (executor == null) {
            throw new IllegalStateException("Offline replay executor is not attached");
        }
        int mutationCount = batch.getMutations().size();
        for (int index = 0; index < mutationCount; index++) {
            OfflineMutation mutation = batch.getMutations().get(index);
            TransactionContext transactionContext = new TransactionContext(
                    index == 0 ? 1 : 0,
                    index == mutationCount - 1 ? 1 : 0,
                    "offline-" + batch.getBatchId(),
                    REPLAY_MAX_ATTEMPTS,
                    null);
            CompletionStage<Void> completion = executor.execute(mutation, transactionContext);
            if (completion == null) {
                throw new IllegalStateException("Offline replay executor returned no completion");
            }
            completion.toCompletableFuture().join();
        }
    }
}
