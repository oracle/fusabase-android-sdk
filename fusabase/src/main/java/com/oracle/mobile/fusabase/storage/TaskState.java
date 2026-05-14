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

/**
 * Abstract base class representing the state of a storage task operation.
 *
 * This class defines the possible states that a storage task (such as upload or download)
 * can be in. It uses a generic type parameter to allow state objects to carry
 * task-specific data.
 *
 * @param <T> The type of data associated with the task state
 */
public abstract class TaskState<T extends Object> {

    /**
     * Represents a task that is currently in progress.
     *
     * This state indicates that the storage operation is actively executing
     * and has not yet completed, failed, or been paused.
     *
     * @param <T> The type of data associated with the task
     */
    public final class InProgress<T extends Object> extends TaskState<T> {

    }

    /**
     * Represents a task that has been paused.
     *
     * This state indicates that the storage operation was temporarily suspended
     * and can potentially be resumed at a later time.
     *
     * @param <T> The type of data associated with the task
     */
    public final class Paused<T extends Object> extends TaskState<T> {

    }
}
