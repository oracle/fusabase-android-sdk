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

import androidx.annotation.Nullable;

/**
 * Interface for handling events in database operations.
 * Implementations of this interface are called when database events occur,
 * such as document changes, query result updates, or operation completions.
 * The event callback provides either a successful result value or an error,
 * but not both simultaneously.
 *
 * @param <T> the type of value delivered in successful event callbacks
 */
public interface EventListener<T> {

    /**
     * Called when an event occurs in a database operation.
     * This method is invoked with either a successful result value OR an error,
     * but never both. One of the parameters will always be null.
     *
     * @param value the result value if the operation succeeded, null if an error occurred
     * @param error the exception if the operation failed, null if the operation succeeded
     */
    void onEvent(@Nullable T value, @Nullable FusabaseOracledbException error);
}
