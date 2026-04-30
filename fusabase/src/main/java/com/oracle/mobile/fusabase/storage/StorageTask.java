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
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.task.OnCompleteListener;
import com.oracle.mobile.fusabase.task.OnFailureListener;
import com.oracle.mobile.fusabase.task.OnSuccessListener;
import com.oracle.mobile.fusabase.task.OnCanceledListener;
import com.oracle.mobile.fusabase.task.Task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class representing a task related to cloud storage operations.
 *
 * @param <ResultT> The type of result produced by this task.
 */
public abstract class StorageTask<ResultT extends StorageTask.ProvideError> extends ControllableTask<ResultT> {

    public interface ProvideError {
        abstract Exception getError();
    }

    /**
     * Base class for snapshots of tasks.
     */
    public class SnapshotBase implements StorageTask.ProvideError {

        private Exception error;
        private StorageTask<ResultT> task;
        private StorageReference storageRef;
        private long totalBytes;
        private long totalBytesTransferred;

        /**
         * Constructs a new snapshot base.
         *
         * @param storageRef The storage reference.
         */
        SnapshotBase (@NonNull StorageReference storageRef) {
            this.storageRef = storageRef;
        }

        @Override
        public Exception getError() {
            return error;
        }

        /**
         * Returns the task associated with this snapshot.
         *
         * @return The task.
         */
        @NonNull
        public StorageTask<ResultT> getTask() {
            return this.task;
        }


         /**
         * Returns the total bytes transferred.
         *
         * @return The total bytes transferred.
         */
        public long getTotalBytes() {
            return this.totalBytes;
        }

        /**
         * Returns the total bytes transferred.
         *
         * @return The total bytes transferred.
         */
        public long getTotalBytesTransferred() {
            return this.totalBytesTransferred;
        }

        /**
         * Returns the storage reference associated with this snapshot.
         *
         * @return The storage reference.
         */
        @NonNull
        public StorageReference getStorage() {
            return this.storageRef;
        }

        /**
         * Sets the error associated with this snapshot.
         *
         * @param error The error.
         */
        protected void setError(@NonNull Exception error) {
            this.error = error;
        }
    }

    private static int PROGRESS_LISTENER_EXECUTION_INTERVAL = 200;
    private StorageReference storageRef;
    private ResultT result;

    private boolean isCanceled;
    private boolean isPaused;
    private boolean isComplete;
    private boolean isFailed;
    private boolean isInProgress;
    private boolean isSuccessful;

    private StorageTask<? super ResultT> instance;

    private AtomicBoolean keepRunning;
    private final Object lock;
    private SnapshotBase snapshot;
    private OnCanceledListener canceledListener;
    private OnCompleteListener<? super ResultT> completeListener;
    private OnSuccessListener<? super ResultT> successListener;
    private OnProgressListener<ResultT> progressListener;
    private OnFailureListener failureListener;
    private OnPausedListener<ResultT> pausedListener;

    private Exception exception;

    private final Handler listenerExecutor;

    protected StorageTask(@NonNull StorageReference storageRef) {
        this.isCanceled = false;
        this.isPaused = false;
        this.isComplete = false;
        this.isInProgress = false;
        this.isFailed = false;
        this.keepRunning = new AtomicBoolean(false);
        this.snapshot = new SnapshotBase( storageRef );
        this.storageRef = storageRef;
        this.listenerExecutor = new Handler(Looper.getMainLooper());
        this.lock = new Object();
        this.instance = this;
    }

    protected Handler getListenerExecutor() { return this.listenerExecutor;}

    protected  OnProgressListener<ResultT> getProgressListener() {return this.progressListener;}

    /**
     * Returns the storage reference associated with this task.
     *
     * @return The storage reference.
     */
    protected StorageReference getStorageReference(){
        return this.storageRef;
    }

    protected AtomicBoolean getKeepRunning() {return this.keepRunning;}

    protected Object getLock() { return this.lock;}

    /**
     * Adds a listener that is called if the task is canceled.
     *
     * @param listener The listener.
     * @return This task.
     */
    @NonNull
    public StorageTask<ResultT> addOnCanceledListener(@NonNull OnCanceledListener listener) {
        this.canceledListener = listener;
        return this;
    }

    /**
     * Adds a listener that is called if the task is canceled.
     *
     * @param activity The activity.
     * @param listener The listener.
     * @return This task.
     */
    @NonNull public StorageTask<ResultT> addOnCanceledListener(@NonNull Activity activity, @NonNull OnCanceledListener listener){
        LifecycleOwner lifecycleOwner = ((LifecycleOwner) activity);
        lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                canceledListener = null;
            }
        });
        this.canceledListener = listener;
        return this;
    }

    /**
     * Adds a listener that is called when the task succeeds or fails.
     *
     * @param listener The listener.
     * @return This task.
     */
    @NonNull
    public StorageTask<ResultT> addOnCompleteListener(@NonNull OnCompleteListener<? super ResultT> listener) {
        this.completeListener = listener;
        return this;
    }

    /**
     * Adds a listener that is called when the task succeeds or fails.
     *
     * @param activity The activity.
     * @param listener The listener.
     * @return This task.
     */
    @NonNull public StorageTask<ResultT> addOnCompleteListener(@NonNull Activity activity, @NonNull OnCompleteListener<? super ResultT> listener){
        LifecycleOwner lifecycleOwner = ((LifecycleOwner) activity);
        lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                completeListener = null;
            }
        });
        this.completeListener = listener;
        return this;
    }

    /**
     * Adds a listener that is called if the task fails.
     *
     * @param listener The listener.
     * @return This task.
     */
    @NonNull
    public StorageTask<ResultT> addOnFailureListener(@NonNull OnFailureListener listener) {
        this.failureListener = listener;
        return this;
    }

     /**
     * Adds a listener that is called if the task fails.
     *
     * @param activity The activity.
     * @param listener The listener.
     * @return This task.
     */
    @NonNull
    public StorageTask<ResultT> addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener listener) {
        LifecycleOwner lifecycleOwner = ((LifecycleOwner) activity);
        lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                failureListener = null;
            }
        });
        this.failureListener = listener;
        return this;
    }

    /**
     * Adds a listener that is called when the task becomes paused.
     *
     * @param listener The listener.
     * @return This task.
     */
    @NonNull
    public StorageTask<ResultT> addOnPausedListener(@NonNull OnPausedListener<ResultT> listener) {
        this.pausedListener = listener;
        return this;
    }

    /**
     * Adds a listener that is called when the task becomes paused.
     *
     * @param activity The activity.
     * @param listener The listener.
     * @return This task.
     */
    @NonNull
    public StorageTask<ResultT> addOnPausedListener(@NonNull Activity activity, @NonNull OnPausedListener<ResultT> listener) {
        LifecycleOwner lifecycleOwner = ((LifecycleOwner) activity);
        lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                pausedListener = null;
            }
        });
        this.pausedListener = listener;
        return this;
    }

    /**
     * Adds a listener that is called periodically while the task executes.
     *
     * @param listener The listener.
     * @return This task.
     */
    @NonNull
    public StorageTask<ResultT> addOnProgressListener(@NonNull OnProgressListener<ResultT> listener) {
        this.progressListener = listener;
        return this;
    }

    /**
     * Adds a listener that is called periodically while the task executes.
     *
     * @param activity The activity.
     * @param listener The listener.
     * @return This task.
     */
    @NonNull
    public StorageTask<ResultT> addOnProgressListener(@NonNull Activity activity, @NonNull OnProgressListener<ResultT> listener) {
        LifecycleOwner lifecycleOwner = ((LifecycleOwner) activity);
        lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                progressListener = null;
            }
        });
        this.progressListener = listener;
        return this;
    }

    /**
     * Adds a listener that is called if the task completes successfully.
     *
     * @param listener The listener.
     * @return This task.
     */
    @NonNull
    public StorageTask<ResultT> addOnSuccessListener(@NonNull OnSuccessListener<? super ResultT> listener) {
        this.successListener = listener;
        return this;
    }

    /**
     * Adds a listener that is called if the task completes successfully.
     *
     * @param activity The activity.
     * @param listener The listener.
     * @return This task.
     */
    @NonNull
    public StorageTask<ResultT> addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener<? super ResultT> listener) {
        LifecycleOwner lifecycleOwner = ((LifecycleOwner) activity);
        lifecycleOwner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                successListener = null;
            }
        });
        this.successListener = listener;
        return this;
    }

    /**
     * Cancels the task.
     *
     * @return True if the task was canceled, false if it has already completed.
     */
    public boolean cancel() {
        if (this.isComplete || this.isFailed)
            return false;
        // Thread might be sleeping. Need to wake the thread in order
        // to acknowledge task cancellation.
        if (this.isPaused) {
            this.setResumed();
            this.onResume();
        }

        this.keepRunning.set(false);
        this.setCanceled();
        // Set the task as complete with a cancellation exception to make await() fail immediately
        this.setException(new RuntimeException("Task was cancelled"));
        this.setComplete();
        this.onCanceled();
        return true;
    }

    /**
     * Returns the exception that caused the task to fail.
     *
     * @return The exception, or null if the task did not fail.
     */
    @Nullable
    public Exception getException() {
        return this.exception;
    }

    /**
     * Returns the result of the task, if it has already completed.
     *
     * @return The result.
     */
    @NonNull
    public ResultT getResult() {
        return this.result;
    }

//    Returns a new Task that will be completed with the result of applying the specified Continuation to this Task.
//    @NonNull Task<ContinuationResultT>
//    <ContinuationResultT> continueWith(
//            @NonNull Continuation<ResultT, ContinuationResultT> continuation){
//
//    }

//    Returns a new Task that will be completed with the result of applying the specified Continuation to this Task.
//    @NonNull Task<ContinuationResultT>
//    <ContinuationResultT> continueWithTask(
//            @NonNull Continuation<ResultT, Task<ContinuationResultT>> continuation
//    )

//    Gets the result of the Task, if it has already completed.
//    @NonNull ResultT<X extends Throwable> getResult(@NonNull Class<X> exceptionType){
//
//    }

    //    Returns the current state of the task.
//    @NonNull
//    ResultT getSnapshot() {
////        return this.snapshot;
//    }

    protected void setComplete() {
        this.isComplete = true;
    }

    protected void setSuccessful() {
        this.isSuccessful = true;
    }

    protected void setCanceled() {
        this.isCanceled = true;
    }

    protected void setFailed() {
        this.isFailed = true;
    }

    protected void setPaused() {
        this.isPaused = true;
    }

    protected  void setResumed() { this.isPaused = false;}

    protected void setResult(ResultT result) {
        super.setResult(result);
        this.result = result;
    }

    protected void setInProgress() {
        this.isInProgress = true;
    }

     /**
     * Sets the exception that caused the task to fail.
     *
     * @param error The exception.
     */
    public void setException(@NonNull Exception error) {
        this.exception = error;
    }

    /**
     * Returns true if the task has been canceled.
     *
     * @return True if the task has been canceled, false otherwise.
     */
    public boolean isCanceled() {
        return this.isCanceled;
    }

    /**
     * Returns true if the task is complete.
     *
     * @return True if the task is complete, false otherwise.
     */
    public boolean isComplete() {
        return this.isComplete;
    }

    /**
     * Returns true if the task is currently running.
     *
     * @return True if the task is currently running, false otherwise.
     */
    public boolean isInProgress() {
        return this.isInProgress;
    }

    /**
     * Returns true if the task has been paused.
     *
     * @return True if the task has been paused, false otherwise.
     */
    public boolean isPaused() {
        return this.isPaused;
    }

    /**
     * Returns true if the task has completed successfully.
     *
     * @return True if the task has completed successfully, false otherwise.
     */
    public boolean isSuccessful() {
        return this.isSuccessful;
    }

    /**
     * Pauses the task.
     *
     * @return True if the task was paused, false if it has already completed.
     */
    public boolean pause() {
        if (this.isCanceled) {
            throw new IllegalStateException("Cannot pause a task that is canceled");
        }
        if (this.isComplete || this.isFailed || !this.keepRunning.get())
            return false;

        this.keepRunning.set(false);
        this.setPaused();
        this.onPaused();
        return true;
    }

    /**
     * Resumes a paused task.
     *
     * @return True if the task was resumed, false if it has already completed.
     */
    public boolean resume() {
        if (this.isCanceled) {
            throw new IllegalStateException("Cannot resume a task that is canceled");
        }
        FusabaseLogger.d("Storage", "Upload Task is resuming");
        if (this.isComplete || this.isFailed || this.keepRunning.get())
            return false;

        this.setResumed();
        this.onResume();

        FusabaseLogger.d("Storage", "Upload Task is resumed");
        return true;
    }

//    Returns a new Task that will be completed with the result of applying the specified SuccessContinuation to this Task when this Task completes successfully.
//    @NonNull Task<ContinuationResultT>
//    <ContinuationResultT> onSuccessTask(
//            @NonNull SuccessContinuation<ResultT, ContinuationResultT> continuation
//    )




        /**
 * Removes the provided OnCanceledListener from this task's list of listeners.
 *
 * @param listener The listener to remove.
 * @return This task, allowing for chaining of method calls.
 */
    @NonNull
    public StorageTask<ResultT> removeOnCanceledListener(@NonNull OnCanceledListener listener) {
        this.canceledListener = null;
        return this;
    }

    /**
 * Removes the provided OnCompleteListener from this task's list of listeners.
 *
 * @param listener The listener to remove.
 * @return This task, allowing for chaining of method calls.
 */
    @NonNull
    public StorageTask<ResultT> removeOnCompleteListener(@NonNull OnCompleteListener<ResultT> listener) {
        this.completeListener = null;
        return this;
    }

      /**
 * Removes the provided OnFailureListener from this task's list of listeners.
 *
 * @param listener The listener to remove.
 * @return This task, allowing for chaining of method calls.
 */
    @NonNull
    public StorageTask<ResultT> removeOnFailureListener(@NonNull OnFailureListener listener) {
        this.failureListener = null;
        return this;
    }

    /**
 * Removes the provided OnPausedListener from this task's list of listeners.
 *
 * @param listener The listener to remove.
 * @return This task, allowing for chaining of method calls.
 */
    @NonNull
    public StorageTask<ResultT> removeOnPausedListener(@NonNull OnPausedListener<ResultT> listener) {
        this.pausedListener = null;
        return this;
    }

    /**
 * Removes the provided OnProgressListener from this task's list of listeners.
 *
 * @param listener The listener to remove.
 * @return This task, allowing for chaining of method calls.
 */
    @NonNull
    public StorageTask<ResultT> removeOnProgressListener(@NonNull OnProgressListener<ResultT> listener) {
        this.progressListener = null;
        return this;
    }

    /**
 * Removes the provided OnSuccessListener from this task's list of listeners.
 *
 * @param listener The listener to remove.
 * @return This task, allowing for chaining of method calls.
 */
    @NonNull
    public StorageTask<ResultT> removeOnSuccessListener(@NonNull OnSuccessListener<ResultT> listener) {
        this.successListener = null;
        return this;
    }

    protected void setRunning() {
        this.keepRunning.set(true);
    }

    protected  void stopRunning() {
        this.keepRunning.set(false);
    }

    protected void handleFuture(CompletableFuture<?> future) {

        this.setRunning();

        future.whenComplete((result, exception) -> {
            FusabaseLogger.d("Storage", "In handle Future from thread " + Thread.currentThread().getName());

            if (exception != null) {
                FusabaseLogger.e("Task completed exceptionally: " + exception.getMessage());
                this.setException((Exception) exception);
                this.setFailed();
                this.onFailure((Exception) exception);
            } else {
                FusabaseLogger.i("Task completed successfully with result: " + result);
                this.setSuccessful();
                this.onSuccess();
            }

            this.setComplete();
            this.onComplete();
            this.stopRunning();
        });
    }


    protected void onCanceled() {
        FusabaseLogger.d("Storage", "Upload Task is canceled");
        if(this.canceledListener != null) {
            listenerExecutor.post(new Runnable() {
                @Override
                public void run() {
                    canceledListener.onCanceled();
                }
            });
        }
    }

    protected void onFailure(Exception e) {
        if(this.failureListener != null) {
            listenerExecutor.post(new Runnable() {
                @Override
                public void run() {
                    failureListener.onFailure(e);
                }
            });
        }
    }

    protected void onPaused() {
        FusabaseLogger.d("Storage", "Upload Task is actually paused");
        if(this.pausedListener != null){
            listenerExecutor.post(new Runnable() {
                @Override
                public void run() {
                    pausedListener.onPaused(result);
                }
            });
        }
    }

    protected void onResume() {
        FusabaseLogger.d("Storage", "Upload Task is actually resumed");
        synchronized (this.lock) {
            this.keepRunning.set(true);
            this.lock.notifyAll();
        }
    }

    // Executing from a handler already
    protected void onProgress() {
        if(this.progressListener != null)
            this.progressListener.onProgress(result);
    }

    protected void onSuccess() {
        try {
            if (this.progressListener != null) {
                this.listenerExecutor.post(this::onProgress);
            }
        } catch (Exception ignored) {
        }
        if(this.successListener != null){
            listenerExecutor.post(new Runnable() {
                @Override
                public void run() {
                    successListener.onSuccess(result);
                }
            });
        }
    }

    protected void onComplete() {
        if(this.completeListener != null){
            listenerExecutor.post(new Runnable() {
                @Override
                public void run() {
                    completeListener.onComplete((Task)instance);
                }
            });
        }
    }
}
