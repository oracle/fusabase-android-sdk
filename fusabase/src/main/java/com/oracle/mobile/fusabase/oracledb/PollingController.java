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
import com.oracle.mobile.fusabase.task.OnCompleteListener;
import com.oracle.mobile.fusabase.task.Task;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller responsible for managing long polling operations for database listeners.
 *
 * <p>This class handles the polling mechanism when WebSocket connections are not available
 * or preferred. It periodically polls the database for changes and notifies registered
 * listeners when data updates occur. The polling interval and behavior can be configured
 * to balance between real-time updates and resource usage.</p>
 *
 * <p>The controller maintains a queue of messages and uses a scheduled executor to perform
 * periodic polling operations. It automatically starts and stops polling based on the
 * number of active listeners.</p>
 *
 * @param <K> The type of Reference being polled (e.g., DocumentReference, CollectionReference)
 * @param <T> The type of Snapshot returned by polling operations
 */
class PollingController<K extends Reference, T extends Snapshot> {

    /** Polling interval in seconds between database checks */
    private final static int POLLING_INTERVAL = 10;

    /** Scheduled executor service for managing polling tasks */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /** Atomic flag indicating whether polling is currently active */
    private AtomicBoolean isPolling = new AtomicBoolean(false);

    /** Queue for storing messages during polling operations */
    private LinkedList<T> messageQueue;

    /** The listener manager that coordinates listener registrations */
    private final ListenerManager<K, T> listenerManager;

    /**
     * Constructs a new PollingController with the specified listener manager.
     *
     * @param manager The ListenerManager that coordinates listener operations
     */
    PollingController(@NonNull ListenerManager<K, T> manager) {
        messageQueue = new LinkedList<>();
        listenerManager = manager;
        FusabaseLogger.d("PollingController", "Initialized");
    }

    /**
     * Called when the listener count changes. Automatically starts or stops polling
     * based on whether there are active listeners.
     */
    protected void listenersUpdated() {
        FusabaseLogger.d("PollingController", "ListenersUpdated");
        if (this.listenerManager.getListenerCount() > 0) {
            startPolling();
        } else {
            stopPolling();
        }
    }

    /**
     * Starts the polling process by scheduling periodic database checks.
     * The polling runs at fixed intervals defined by POLLING_INTERVAL.
     */
    protected void startPolling() {
        FusabaseLogger.d("PollingController", "PollingStarted");
        this.isPolling = new AtomicBoolean(true);
        scheduler.scheduleWithFixedDelay(this::poll, 0, POLLING_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Determines if the provided snapshot contains updated data compared to the listener's previous result.
     * For DocumentSnapshots, compares the data content. For QuerySnapshots, checks if there are document changes.
     *
     * @param snap The snapshot to check for updates
     * @param listenerData The listener data containing the previous result
     * @return true if the snapshot contains new or updated data, false otherwise
     */
    protected boolean resultHasUpdated(@NonNull Snapshot snap, @NonNull ListenerData<K, T> listenerData) {
        try {
            if (snap instanceof DocumentSnapshot) {
                T prev = listenerData.getPreviousResult();
                if (prev == null) {
                    return true;
                }
                Object currData = ((DocumentSnapshot) snap).getData();
                Object prevData = ((DocumentSnapshot) prev).getData();
                return !Objects.equals(currData, prevData);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error accessing DocumentSnapshot data");
        }
        return !((QuerySnapshot) snap).getDocumentChanges().isEmpty();
    }

    /**
     * Performs a polling operation by querying all registered listeners' references.
     * For each listener, fetches the latest data from the server and notifies listeners
     * if changes are detected. Handles both successful responses and error conditions.
     */
    protected void poll() {
        this.listenerManager.getListeners().forEach((listenerData) -> {
            FusabaseLogger.d("PollingController", "SendingPollingRequest");
            listenerData.getReference().get(Source.SERVER, true).addOnCompleteListener(new OnCompleteListener<Snapshot>() {
                @Override
                public void onComplete(Task<Snapshot> task) {
                    if (task.isSuccessful()) {
                        FusabaseLogger.i("PollingController", "PollingRequestSuccessful for "
                                + listenerData.getReference().getPath());
                        if (resultHasUpdated(task.getResult(), listenerData)) {
                            listenerData.executeListener((T) task.getResult(), null);
                        }
                    } else {
                        FusabaseLogger.i("PollingController", "PollingRequestFailed for "
                                + listenerData.getReference().getPath());
                        listenerData.executeListener(null,
                                new FusabaseOracledbException(
                                        Objects.requireNonNull(task.getException().getMessage()),
                                        FusabaseOracledbException.Code.INTERNAL));
                    }
                }
            });

        });
    }

    /**
     * Stops the polling process and shuts down the scheduler.
     * This method should be called when there are no more active listeners.
     */
    protected void stopPolling() {
        FusabaseLogger.i("PollingController", "PollingShuttingDown");
        this.isPolling = new AtomicBoolean(false);
        scheduler.shutdownNow();
    }

}
