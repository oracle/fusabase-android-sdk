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

/**
 * An abstract class representing a controllable task that can be paused and resumed.
 *
 * @param <StateT> The type of state associated with this task.
 */
public abstract class ControllableTask<StateT> extends CancellableTask<StateT> {
    /**
     * Constructs a new instance of ControllableTask.
     */
    public ControllableTask() {
    }
/**
     * Adds a listener that is called when the task becomes paused.
     *
     * @param listener The listener to add.
     * @return This task instance for chaining.
     */
    public abstract @NonNull ControllableTask<StateT> addOnPausedListener(@NonNull OnPausedListener<StateT> listener);

     /**
     * Adds a listener that is called when the task becomes paused. The listener will be executed on the UI thread of the provided activity.
     *
     * @param activity The activity whose UI thread will be used to execute the listener.
     * @param listener The listener to add.
     * @return This task instance for chaining.
     */
    public abstract @NonNull ControllableTask<StateT> addOnPausedListener(@NonNull Activity activity, @NonNull OnPausedListener<StateT> listener);


    // public abstract @NonNull ControllableTask<StateT> addOnPausedListener(
    //         @NonNull Executor executor,
    //         @NonNull OnPausedListener<Object> listener
    // );

    /**
     * Checks if the task is currently paused.
     *
     * @return True if the task is paused, false otherwise.
     */
    public abstract boolean isPaused();

    /**
     * Attempts to pause the task.
     *
     * @return True if the task was successfully paused, false otherwise.
     */
    public abstract boolean pause();

    /**
     * Attempts to resume this task.
     *
     * @return True if the task was successfully resumed, false otherwise.
     */
    public abstract boolean resume();

}
