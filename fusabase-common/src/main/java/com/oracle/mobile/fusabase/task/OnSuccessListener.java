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

/**
 * Interface definition for a callback to be invoked when a task completes successfully.
 *
 * Implementations of this interface should provide a concrete implementation of the {@link #onSuccess(Object)} method,
 * which will be called when the task succeeds.
 *
 * @param <T> The type of result produced by the task.
 */
public interface OnSuccessListener <T> extends Listener{

     /**
     * Notification that the task has completed successfully.
     *
     * This method will be invoked when the task finishes executing without encountering any errors.
     * The provided {@code result} parameter contains the result of the task, which can be used to perform further processing or display the result to the user.
     *
     * @param result The result of the task, guaranteed to be non-null.
     */
     public void onSuccess(T result);
}
