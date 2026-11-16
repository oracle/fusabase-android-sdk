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
 * Uses this Oracle Backend for Firebase Storage API.
 *
 * <p>Example:</p>
 * <pre>{@code
 * StorageErrorCode code = StorageErrorCode.INTERNAL;
 * }</pre>
 */
public enum StorageErrorCode {
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
INVALID_ARGUMENT(400, "invalid-argument"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
UNAUTHENTICATED(401, "unauthenticated"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
PERMISSION_DENIED(403, "permission-denied"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
NOT_FOUND(404, "not-found"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
ABORTED(499, "aborted"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
INTERNAL(500, "internal"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
NOT_IMPLEMENTED(501, "not-implemented"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
UNKNOWN_ERROR(0, "unknown"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
IO_EXCEPTION(1000, "io-error"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
FILE_NOT_FOUND(1001, "file-not-found"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
CANNOT_CREATE_FILE(1002, "cannot-create-file"),
    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
CANCELLED(1003, "cancelled");

    private final int code;
    private final String message;

    StorageErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
public int getCode() {
        return code;
    }

    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
public String getMessage() {
        return message;
    }

    /**
     * Uses this Oracle Backend for Firebase Storage API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageErrorCode code = StorageErrorCode.INTERNAL;
     * }</pre>
     */
public static StorageErrorCode fromCode(int code) {
        for (StorageErrorCode error : values()) {
            if (error.getCode() == code) {
                return error;
            }
        }
        return UNKNOWN_ERROR;
    }
}
