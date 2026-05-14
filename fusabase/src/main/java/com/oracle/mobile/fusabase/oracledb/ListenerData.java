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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.util.concurrent.Executor;

/**
 * A data container class that holds information about a single listener registration.
 * This class encapsulates all the necessary data and state for managing an event listener
 * attached to a database reference, including lifecycle management, execution context,
 * and synchronization state.
 *
 * @param <K> The type of Reference (e.g., DocumentReference, CollectionReference)
 * @param <T> The type of Snapshot (e.g., DocumentSnapshot, QuerySnapshot)
 */
class ListenerData<K extends Reference, T extends Snapshot> {

    @Nullable
    private final Activity activity;

    @NonNull
    private final Executor executor;

    private final int listenerId;

    @NonNull
    private final EventListener<T> listener;

    @NonNull
    private final ListenerRegistration registration;

    @NonNull
    private final ListenerManager<K, T> manager;

    @Nullable
    private final MetadataChanges metadataChanges;

    @NonNull
    protected Reference ref;

    @NonNull
    protected boolean isSynchronized;

    @Nullable
    protected T previousResult;

    /**
     * Constructs a new ListenerData instance with the provided parameters.
     *
     * @param listener The event listener that will be called when data changes
     * @param ref The database reference this listener is attached to
     * @param listenerId Unique identifier for this listener
     * @param manager The ListenerManager that manages this listener
     * @param activity Optional Android Activity for lifecycle management
     * @param executor Executor for running the listener callback
     * @param metadataChanges Configuration for metadata change notifications
     */
    ListenerData(@NonNull EventListener<T> listener,
                 @NonNull K ref,
                 int listenerId,
                 @NonNull ListenerManager<K, T> manager,
                 @Nullable Activity activity,
                 @NonNull Executor executor,
                 @NonNull MetadataChanges metadataChanges) {
        this.activity = activity;
        this.listenerId = listenerId;
        this.listener = listener;
        this.executor = executor;
        this.registration = createListenerRegistration();
        this.manager = manager;
        this.metadataChanges = metadataChanges;
        this.ref = ref;
        this.isSynchronized = false;
        this.previousResult = null;

        FusabaseLogger.d("ListenerData", "Created ListenerData for reference: " + ref.getPath() + ", listenerId: " + listenerId);
    }

    protected int getListenerId() {
        return this.listenerId;
    }

    @NonNull
    public Executor getExecutor() {
        return executor;
    }

    @Nullable
    public MetadataChanges getMetadataChanges() {
        return metadataChanges;
    }

    @NonNull
    public EventListener<T> getListener() {
        return listener;
    }

    @Nullable
    protected Activity getActivity() {
        return activity;
    }

    @NonNull
    protected ListenerRegistration getListenerRegistration() {
        return this.registration;
    }

    @NonNull
    protected Reference getReference() {
        return ref;
    }

    private ListenerRegistration createListenerRegistration() {
        return new ListenerRegistration() {
            @Override
            public void remove() {
                manager.removeListener(ListenerData.this);
            }
        };
    }

    /**
     * Executes the event listener on the specified executor with the given snapshot value and error.
     * Updates the previous result before triggering the listener callback.
     *
     * @param value The snapshot value to pass to the listener
     * @param error Any error that occurred, or null if successful
     */
    protected void executeListener(Snapshot value, FusabaseOracledbException error) {

        // Set the current result as previous result
        this.setPreviousResult((T) value);
        FusabaseLogger.d("ListenerData", "Executing listener for reference: " + ref.getPath() + ", listenerId: " + listenerId +
                (error != null ? ", error: " + error.getMessage() : ", success"));
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                listener.onEvent((T)value, error);
            }
        });

    }

    protected T getPreviousResult () {
        return this.previousResult;
    }

    protected T setPreviousResult (T currentResult) {
        return this.previousResult = currentResult;
    }

    protected boolean isSynchronized() {
        return isSynchronized;
    }

    protected void setSynchronized() {
        this.isSynchronized = true;
    }

}
