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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for working with tasks.
 *
 * <p>This class is intended for internal use only. Developers should use this class very carefully
 * as improper usage might cause several issues such as deadlocks if not used properly.</p>
 */
public class Tasks<T> {

    /**
     * Waits for the given task to complete and returns its result.
     *
     * <p>This method blocks the current thread until the task completes.</p>
     *
     * @param task The task to await.
     * @param <T> The type of the task's result.
     * @return The result of the task.
     * @throws ExecutionException If the task failed with an exception.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    public static <T> T await(@NonNull Task<T> task) throws ExecutionException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        task.addOnCompleteListener(completedTask -> latch.countDown());

        latch.await();

        if (task.isSuccessful()) {
            return task.getResult();
        } else {
            Exception e = task.getException();
            throw new ExecutionException(e);
        }
    }

    /**
     * Waits for the given task to complete within the specified timeout and returns its result.
     *
     * <p>This method blocks the current thread until the task completes or the timeout expires.</p>
     *
     * @param task The task to await.
     * @param timeout The maximum time to wait.
     * @param unit The time unit of the timeout argument.
     * @param <T> The type of the task's result.
     * @return The result of the task.
     * @throws ExecutionException If the task failed with an exception.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     * @throws TimeoutException If the task did not complete within the specified timeout.
     */
    public static <T> T await(Task<T> task, long timeout, TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException {
        final CountDownLatch latch = new CountDownLatch(1);

        task.addOnCompleteListener(completedTask -> latch.countDown());

        boolean finished = latch.await(timeout, unit);

        if (!finished) {
            throw new TimeoutException("Task did not complete within timeout");
        }

        if (task.isSuccessful()) {
            return task.getResult();
        } else {
            Exception e = task.getException();
            throw new ExecutionException(e);
        }
    }
}
