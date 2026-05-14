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

package com.oracle.mobile.fusabase.task;

import androidx.annotation.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * A utility class for managing task completion, providing methods for setting results, exceptions,
 * and handling futures. This class is designed to work in conjunction with {@link TaskTracker}.
 *
 * @see TaskTracker
 */
public class TaskCompletionSource <T>{
    private TaskTracker<T> task;
    private CompletableFuture<?> completableFuture;

    /**
     * Constructs a new TaskCompletionSource instance, initializing the internal task tracker.
     */
    public TaskCompletionSource()
    {
        task = new TaskTracker<>();
    }

    /**
     * Returns the associated task instance.
     *
     * @return The task instance.
     */
    @NonNull
    public Task<T> getTask()
    {
        return task;
    }

    /**
     * Sets the result of the task.
     *
     * @param result The result value.
     */
    public void setResult(T result)
    {
        this.task.setResult(result);
    }

    /**
     * Retrieves the result of the task.
     *
     * @return The result value, or null if not set.
     */
    public T getResult()
    {
        return task.getResult();
    }

    /**
     * Sets an exception on the task.
     *
     * @param e The exception instance.
     */
    public void setException(Exception e)
    {
        this.task.setException(e);
    }

    /**
     * Handles a CompletableFuture, updating the task state accordingly.
     *
     * @param future The CompletableFuture to handle.
     */
    public void handleFuture(CompletableFuture < ? > future)
    {
        this.task.setFuture(future);
        future.whenComplete((result, exception) -> {
            this.task.setComplete();
            if (exception != null) {
                this.task.setException((Exception) exception.getCause());
                this.task.onFailure((Exception) exception.getCause());
            } else {
                this.task.setSuccessful();
                this.task.onSuccess();
            }
            this.task.setComplete();
            this.task.onComplete();
        });
    }

}
