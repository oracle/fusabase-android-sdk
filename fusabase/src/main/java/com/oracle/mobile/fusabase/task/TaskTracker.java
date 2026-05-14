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
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * A class representing a task tracker, which extends the Task class.
 * <p>
 * This class provides additional functionality for tracking the state of a task, including setting the result, exception, and completion status.
 *
 * @param <T> The type of result produced by the task.
 */
public class TaskTracker<T> extends Task<T> {

    /**
     * The result of the task.
     */
    protected T result;
    /**
     * The exception that caused the task to fail, or null if the task did not fail.
     */
    protected Exception exception;
    protected CompletableFuture<?> completableFuture;

    private final HashMap<Listener, WeakReference<Activity>> listenerActivity;
    private final List<OnSuccessListener<? super T>> onSuccessListener;
    private final List<OnFailureListener> onFailureListener;
    private final List<OnCompleteListener<? super T>> onCompleteListener;
    private final List<OnCanceledListener> onCanceledListener;
    private boolean isComplete;
    private boolean isCanceled;
    private boolean isSuccessful;
    private TaskTracker<? super T> instance;
    private Handler listenerExecutor;

    /**
     * Constructs a new task tracker.
     */
    protected TaskTracker() {
        this.isComplete = false;
        this.exception = null;
        this.isSuccessful = false;
        this.isCanceled = false;
        this.listenerExecutor = new Handler(Looper.getMainLooper());
        this.instance = this;
        this.onCanceledListener = new ArrayList<>();
        this.onSuccessListener = new ArrayList<>();
        this.onFailureListener = new ArrayList<>();
        this.onCompleteListener = new ArrayList<>();
        this.listenerActivity = new HashMap<>();
    }


    /**
     * Sets the result of the task.
     *
     * @param result The result of the task.
     */
    protected void setResult(T result) {
        this.result = result;
    }

    protected void setFuture(@NonNull CompletableFuture<?> completableFuture) {
        this.completableFuture = completableFuture;
    }

    protected void get() throws ExecutionException, InterruptedException {
        this.completableFuture.get();
    }

    protected boolean isActivityAlive(@Nullable Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private void registerListenerWithActivity(@NonNull Activity activity,
                                              @NonNull OnActivityDestroyed onActivityDestroyed) {

        Application application = activity.getApplication();
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(Activity a) {
                if (a == activity) {
                    onActivityDestroyed.removeListener();
                    application.unregisterActivityLifecycleCallbacks(this);
                }
            }
        });

    }

    /**
     * Adds a listener to be notified when the task succeeds.
     *
     * @param listener The listener to add.
     * @return This task tracker, allowing for chaining of method calls.
     */
    @NonNull
    public Task<T> addOnSuccessListener(@NonNull OnSuccessListener<? super T> listener) {
        this.onSuccessListener.add(listener);
        return this;
    }

    /**
     * Adds a listener to be notified when the task succeeds, tied to the lifecycle of the specified activity.
     *
     * @param activity The activity whose lifecycle this listener is tied to.
     * @param listener The listener to add.
     * @return This task tracker, allowing for chaining of method calls.
     */
    @Override
    @NonNull
    public Task<T> addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener<? super T> listener) {
        registerListenerWithActivity(activity, new OnActivityDestroyed() {
            @Override
            public void removeListener() {
                onSuccessListener.remove(listener);
            }
        });

        listenerActivity.put(listener, new WeakReference<>(activity));

        this.onSuccessListener.add(listener);

        return this;
    }

    /**
     * Adds a listener to be notified when the task fails.
     *
     * @param failureListener The listener to add.
     * @return This task tracker, allowing for chaining of method calls.
     */
    @NonNull
    public Task<T> addOnFailureListener(@NonNull OnFailureListener failureListener) {
        this.onFailureListener.add(failureListener);
        return this;
    }

    /**
     * Adds a listener to be notified when the task fails, tied to the lifecycle of the specified activity.
     *
     * @param activity        The activity whose lifecycle this listener is tied to.
     * @param failureListener The listener to add.
     * @return This task tracker, allowing for chaining of method calls.
     */
    @NonNull
    public Task<T> addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener failureListener) {
        this.registerListenerWithActivity(activity, new OnActivityDestroyed() {
            @Override
            public void removeListener() {
                onFailureListener.remove(failureListener);
            }
        });

        this.listenerActivity.put(failureListener,  new WeakReference<>(activity));

        return this.addOnFailureListener(failureListener);
    }

    /**
     * Adds a listener to be notified when the task completes.
     *
     * @param completeListener The listener to add.
     * @return This task tracker, allowing for chaining of method calls.
     */
    @NonNull
    public Task<T> addOnCompleteListener(@NonNull OnCompleteListener<? super T> completeListener) {
        this.onCompleteListener.add(completeListener);
        return this;
    }

    /**
     * Adds a listener to be notified when the task completes, tied to the lifecycle of the specified activity.
     *
     * @param activity         The activity whose lifecycle this listener is tied to.
     * @param completeListener The listener to add.
     * @return This task tracker, allowing for chaining of method calls.
     */
    @NonNull
    public Task<T> addOnCompleteListener(@NonNull Activity activity,
                                         @NonNull OnCompleteListener<? super T> completeListener) {
        this.registerListenerWithActivity(activity, new OnActivityDestroyed() {
            @Override
            public void removeListener() {
                onCompleteListener.remove(completeListener);
            }
        });

        this.listenerActivity.put(completeListener, new WeakReference<>(activity));

        return this.addOnCompleteListener(completeListener);
    }

    /**
     * Adds a listener to be notified when the task is canceled.
     *
     * @param listener The listener to add.
     * @return This task tracker, allowing for chaining of method calls.
     */
    @NonNull
    public Task<T> addOnCanceledListener(@NonNull OnCanceledListener listener) {
        this.onCanceledListener.add(listener);
        return this;
    }

    /**
     * Adds a listener to be notified when the task is canceled, tied to the lifecycle of the specified activity.
     *
     * @param activity The activity whose lifecycle this listener is tied to.
     * @param listener The listener to add.
     * @return This task tracker, allowing for chaining of method calls.
     */
    @NonNull
    public Task<T> addOnCanceledListener(@NonNull Activity activity, @NonNull OnCanceledListener listener) {
        this.registerListenerWithActivity(activity, new OnActivityDestroyed() {
            @Override
            public void removeListener() {
                onCanceledListener.remove(listener);
            }
        });

        this.listenerActivity.put(listener, new WeakReference<>(activity));

        return this.addOnCanceledListener(listener);
    }

    /**
     * Returns the result of the task.
     *
     * @return The result of the task.
     */
    public T getResult() {
        return result;
    }

    /**
     * Sets the exception that caused the task to fail.
     *
     * @param e The exception that caused the task to fail.
     */
    protected void setException(Exception e) {
        this.exception = e;
    }

    /**
     * Notifies the success listener that the task has succeeded.
     */
    protected void onSuccess() {
        for (OnSuccessListener<? super T> listener : onSuccessListener) {
            if (listenerActivity.get(listener) == null || isActivityAlive(listenerActivity.get(listener).get())) {
                listenerExecutor.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onSuccess(result);
                    }
                });
            }
        }
    }

    /**
     * Notifies the failure listener that the task has failed.
     * All the listeners will be executed sequentially
     * @param e The exception that caused the task to fail.
     */
    protected void onFailure(Exception e) {
        for (OnFailureListener listener : onFailureListener) {
            if (listenerActivity.get(listener) == null || isActivityAlive(listenerActivity.get(listener).get())) {
                listenerExecutor.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onFailure(e);
                    }
                });
            }
        }
    }

    /**
     * Notifies the completion listener that the task has completed.
     * All the listeners will be executed sequentially
     */
    protected void onComplete() {
        for (OnCompleteListener<? super T> listener : onCompleteListener) {
            if (listenerActivity.get(listener) == null || isActivityAlive(listenerActivity.get(listener).get())) {
                listenerExecutor.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onComplete((Task) instance);
                    }
                });
            }
        }
    }

    /**
     * Notifies the completion listener that the task has completed.
     */
    protected void onCancelled() {

        for (OnCanceledListener listener : onCanceledListener) {
            if (listenerActivity.get(listener) == null || isActivityAlive(listenerActivity.get(listener).get())) {
                listenerExecutor.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onCanceled();
                    }
                });
            }
        }
    }

    /**
     * Returns the exception that caused the task to fail, or null if the task did not fail.
     *
     * @return The exception that caused the task to fail, or null if the task did not fail.
     */
    @Override
    public Exception getException() {
        return this.exception;
    }

    /**
     * Returns true if the task has completed, false otherwise.
     *
     * @return True if the task has completed, false otherwise.
     */
    @Override
    public boolean isComplete() {
        return this.isComplete;
    }

    /**
     * Returns true if the task has been canceled, false otherwise.
     *
     * @return True if the task has been canceled, false otherwise.
     */
    @Override
    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * Returns true if the task has completed successfully, false otherwise.
     *
     * @return True if the task has completed successfully, false otherwise.
     */
    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }

    /**
     * Sets the task as complete.
     */
    protected void setComplete() {
        this.isComplete = true;
    }

    /**
     * Sets the task as successful.
     */
    protected void setSuccessful() {
        this.isSuccessful = true;
    }

    /**
     * Sets the task as canceled.
     */
    protected void setCanceled() {
        this.isCanceled = true;
    }
}
