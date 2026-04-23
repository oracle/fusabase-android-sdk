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

package com.oracle.mobile.fusabase;

import androidx.annotation.NonNull;

/**
 * Represents an exception in the Fusabase SDK, serving as the base class for other specific exception types.
 * <p>
 * This exception is thrown to indicate errors that occur during the operation of the Fusabase SDK.
 * It provides a way to handle and propagate error information with optional cause details.
 * </p>
 */
public class FusabaseException extends Exception {

    /**
     * Constructs a new FusabaseException with the specified detail message.
     *
     * @param message The detail message explaining the exception.
     */
    public FusabaseException(@NonNull String message) {
        super(message);
    }

    /**
     * Constructs a new FusabaseException with the specified detail message and cause.
     *
     * @param message The detail message explaining the exception.
     * @param cause   The cause of this exception.
     */
    public FusabaseException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }
}
