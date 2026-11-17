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

package com.oracle.mobile.fusabase.storage;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.task.TaskTracker;

/**
 * An abstract class representing a cancellable task that can execute in the background.
 *
 *
 * <p>Example:</p>
 * <pre>{@code
 * Storage storage = Storage.getInstance(FusabaseApp.getInstance());
 * StorageReference image = storage.getReference("recipes/tea.png");
 * UploadTask upload = image.putBytes(fileBytes);
 * boolean canceled = upload.cancel();
 * }</pre>
 *
 * @param <StateT> The type of state associated with this task.
 */
public abstract class CancellableTask<StateT> extends TaskTracker<StateT> {

    /**
     * Constructs a new instance o f CancellableTask.
     *
     * <p>Example: Obtain this value through the public operation shown below.</p>
     * <pre>{@code
     * Storage storage = Storage.getInstance(FusabaseApp.getInstance());
     * StorageReference image = storage.getReference("recipes/tea.png");
     * UploadTask upload = image.putBytes(fileBytes);
     * boolean canceled = upload.cancel();
     * }</pre>
     */
    public CancellableTask() {
    }

    /**
     * Adds a listener that is called periodically while the task executes.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * Storage storage = Storage.getInstance(FusabaseApp.getInstance());
     * StorageReference image = storage.getReference("recipes/tea.png");
     * UploadTask upload = image.putBytes(fileBytes);
     * upload.addOnProgressListener(snapshot -> {
     *     long transferred = snapshot.getBytesTransferred();
     * });
     * }</pre>
     *
     * @param listener The listener to add.
     * @return This task instance for chaining.
     */
    public abstract @NonNull CancellableTask<StateT> addOnProgressListener(@NonNull OnProgressListener<StateT> listener);

    /**
     * Adds a listener that is called periodically while the task executes. The listener will be executed on the UI thread of the provided activity.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * Storage storage = Storage.getInstance(FusabaseApp.getInstance());
     * StorageReference image = storage.getReference("recipes/tea.png");
     * UploadTask upload = image.putBytes(fileBytes);
     * upload.addOnProgressListener(snapshot -> {
     *     long transferred = snapshot.getBytesTransferred();
     * });
     * }</pre>
     *
     * @param activity The activity whose UI thread will be used to execute the listener.
     * @param listener The listener to add.
     * @return This task instance for chaining.
     */
    public abstract @NonNull CancellableTask<StateT> addOnProgressListener(
            @NonNull Activity activity,
            @NonNull OnProgressListener<StateT> listener
    );

    /**
     * Adds a listener that is called periodically while the task executes. The listener will be executed on the provided executor.
     *
     * @param executor The executor that will be used to execute the listener.
     * @param listener The listener to add.
     * @return This task instance for chaining.
     */
    // public abstract @NonNull CancellableTask<StateT> addOnProgressListener(
    //         @NonNull Executor executor,
    //         @NonNull OnProgressListener<StateT> listener
    // );

    /**
     * Attempts to cancel the task.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * Storage storage = Storage.getInstance(FusabaseApp.getInstance());
     * StorageReference image = storage.getReference("recipes/tea.png");
     * UploadTask upload = image.putBytes(fileBytes);
     * boolean changed = upload.cancel();
     * }</pre>
     *
     * @return True if the task was successfully canceled, false otherwise.
     */
    public abstract boolean cancel();

    /**
     * Checks if the task has been canceled.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * Storage storage = Storage.getInstance(FusabaseApp.getInstance());
     * StorageReference image = storage.getReference("recipes/tea.png");
     * UploadTask upload = image.putBytes(fileBytes);
     * boolean canceled = upload.cancel();
     * }</pre>
     *
     * @return True if the task has been canceled, false otherwise.
     */
    public abstract boolean isCanceled();

    /**
     * Checks if the task is currently in progress.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * Storage storage = Storage.getInstance(FusabaseApp.getInstance());
     * StorageReference image = storage.getReference("recipes/tea.png");
     * UploadTask upload = image.putBytes(fileBytes);
     * boolean canceled = upload.cancel();
     * }</pre>
     *
     * @return True if the task is in progress, false otherwise.
     */
    public abstract boolean isInProgress();
}