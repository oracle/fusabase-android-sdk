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

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutionException;

/**
 * Abstract base class representing a task that produces a result of type T.
 *
 * Tasks are asynchronous operations that can be executed and monitored using various methods provided by this class.
 * Implementations of this class must provide concrete implementations of the abstract methods defined here.
 *
 * @param <T> The type of result produced by this task.
 */
public abstract class Task<T> {

    /**
     * Adds a listener that is called if the task completes successfully.
     *
     * @param listener The listener to add.
     * @return This task, allowing for chaining of method calls.
     */
    @NonNull
    abstract public Task<T> addOnSuccessListener(@NonNull OnSuccessListener<? super T> listener);

    /**
     * Adds a listener that is called if the task completes successfully, tied to the lifecycle of the specified activity.
     *
     * @param activity The activity whose lifecycle this listener is tied to.
     * @param listener The listener to add.
     * @return This task, allowing for chaining of method calls.
     */
    @NonNull
    abstract public Task<T> addOnSuccessListener(@NonNull Activity activity,@NonNull OnSuccessListener<? super T> listener);

    /**
     * Adds a listener that is called if the task fails.
     *
     * @param listener The listener to add.
     * @return This task, allowing for chaining of method calls.
     */
    @NonNull
    abstract public Task<T> addOnFailureListener(@NonNull OnFailureListener listener);
    
    /**
     * Adds a listener that is called if the task fails, tied to the lifecycle of the specified activity.
     *
     * @param activity The activity whose lifecycle this listener is tied to.
     * @param listener The listener to add.
     * @return This task, allowing for chaining of method calls.
     */
    @NonNull
    abstract public Task<T> addOnFailureListener(@NonNull Activity activity,@NonNull OnFailureListener listener);

    /**
     * Adds a listener that is called when the task completes, either successfully or due to an error.
     *
     * @param listener The listener to add.
     * @return This task, allowing for chaining of method calls.
     */
    @NonNull
    abstract public Task<T> addOnCompleteListener(@NonNull OnCompleteListener<? super T> listener);

    /**
     * Adds a listener that is called when the task completes, either successfully or due to an error, tied to the lifecycle of the specified activity.
     *
     * @param activity The activity whose lifecycle this listener is tied to.
     * @param listener The listener to add.
     * @return This task, allowing for chaining of method calls.
     */
    @NonNull
    abstract public Task<T> addOnCompleteListener(@NonNull Activity activity,@NonNull OnCompleteListener<? super T> listener );

    /**
     * Adds a listener that is called if the task is canceled.
     *
     * @param listener The listener to add.
     * @return This task, allowing for chaining of method calls.
     */
    @NonNull
    abstract public Task<T> addOnCanceledListener(@NonNull OnCanceledListener listener);

    /**
     * Adds a listener that is called if the task is canceled, tied to the lifecycle of the specified activity.
     *
     * @param activity The activity whose lifecycle this listener is tied to.
     * @param listener The listener to add.
     * @return This task, allowing for chaining of method calls.
     */
    @NonNull
    abstract public Task<T> addOnCanceledListener(@NonNull Activity activity,@NonNull OnCanceledListener listener);

    /**
     * Returns the exception that caused the task to fail, or null if the task did not fail.
     *
     * @return The exception that caused the task to fail, or null if the task did not fail.
     */
    @Nullable
    abstract public Exception getException();

    /**
     * Returns the result of the task, if it has already completed.
     *
     * @return The result of the task.
     */
    abstract public T getResult();

//    abstract <X extends Throwable> T getResult(@NonNull Class<X> exceptionType);

    /**
     * Returns true if the task has completed, either successfully or due to an error.
     *
     * @return True if the task has completed, false otherwise.
     */
    abstract public boolean isComplete();

    /**
     * Returns true if the task has been canceled.
     *
     * @return True if the task has been canceled, false otherwise.
     */
    abstract public boolean isCanceled();

    /**
     * Returns true if the task has completed successfully.
     *
     * @return True if the task has completed successfully, false otherwise.
     */
    abstract public boolean isSuccessful();

//    @NonNull public <TContinuationResult> Task<TContinuationResult> continueWith(
//            @NonNull Continuation<T, TContinuationResult> continuation
//    ) {
//
//    }
//
//    @NonNull public <TContinuationResult> Task<TContinuationResult> continueWith(
//            @NonNull Executor executor,
//            @NonNull Continuation<T, TContinuationResult> continuation
//    ) {
//
//    }
//
//    @NonNull public <TContinuationResult> Task<TContinuationResult> continueWithTask(
//            @NonNull Continuation<T, Task<TContinuationResult>> continuation
//    ) {
//
//    }
//
//    @NonNull public <TContinuationResult> Task<TContinuationResult> continueWithTask(
//            @NonNull Executor executor,
//            @NonNull Continuation<T, Task<TContinuationResult>> continuation
//    ) {
//
//    }
//
//    @NonNull public <TContinuationResult> Task<TContinuationResult>
//     onSuccessTask(
//            @NonNull SuccessContinuation<T, TContinuationResult> successContinuation
//    ) {
//
//    }

}
