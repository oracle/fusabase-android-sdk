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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;

/**
 * Exception class for Oracle Database Storage operations in the FUSABASE SDK.
 *
 * <p>This exception is thrown by various storage operations when errors occur,
 * providing structured error information including error codes and detailed messages.
 * It extends the base {@link FusabaseException} and adds specific error codes for
 * different types of storage operation failures.</p>
 *
 * <p>The exception includes predefined error codes for common scenarios such as
 * authentication failures, permission issues, network errors, and internal errors.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * StorageException error = new StorageException(
 *     "Upload failed", StorageException.Code.INTERNAL);
 * }</pre>
 */
public class StorageException extends FusabaseException {

    /**
     * Enumeration of error codes for different types of storage operation failures.
     * Each error code includes an HTTP-style status code and a descriptive message.
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageException.Code code = StorageException.Code.INTERNAL;
     * }</pre>
     */
    public enum Code {
        /**
         *  Invalid argument provided to a storage operation
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.INVALID_ARGUMENT;
         * }</pre>
         */
        INVALID_ARGUMENT(400, "invalid-argument"),

        /**
         *  User is not authenticated
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.UNAUTHENTICATED;
         * }</pre>
         */
        UNAUTHENTICATED(401, "unauthenticated"),

        /**
         *  User does not have permission for the requested operation
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.PERMISSION_DENIED;
         * }</pre>
         */
        PERMISSION_DENIED(403, "permission-denied"),

        /**
         *  Requested resource was not found
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.NOT_FOUND;
         * }</pre>
         */
        NOT_FOUND(404, "not-found"),

        /**
         *  Operation was aborted, typically due to a conflict
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.ABORTED;
         * }</pre>
         */
        ABORTED(499, "aborted"),

        /**
         *  Internal server error occurred
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.INTERNAL;
         * }</pre>
         */
        INTERNAL(500, "internal"),

        /**
         *  Requested operation is not implemented
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.NOT_IMPLEMENTED;
         * }</pre>
         */
        NOT_IMPLEMENTED(501, "not-implemented"),

        /**
         *  Unknown error occurred
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.UNKNOWN_ERROR;
         * }</pre>
         */
        UNKNOWN_ERROR(0, "unknown"),

        /**
         *  Input/Output operation failed
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.IO_EXCEPTION;
         * }</pre>
         */
        IO_EXCEPTION(1000, "io-error"),

        /**
         *  Required file was not found
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.FILE_NOT_FOUND;
         * }</pre>
         */
        FILE_NOT_FOUND(1001, "file-not-found"),

        /**
         *  File could not be created
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.CANNOT_CREATE_FILE;
         * }</pre>
         */
        CANNOT_CREATE_FILE(1002, "cannot-create-file"),

        /**
         *  Operation was cancelled
         *
         * <p>Example:</p>
         * <pre>{@code
         * String token = StorageReference.CANCELLED;
         * }</pre>
         */
        CANCELLED(1003, "cancelled");

        /** The numeric error code */
        private final int code;

        /** The descriptive error message */
        private final String message;

        /**
         * Constructs a new error code with the specified numeric code and message.
         *
         * @param code The numeric error code
         * @param message The descriptive error message
         */
        Code(int code, String message) {
            this.code = code;
            this.message = message;
        }

        /**
         * Returns the numeric error code.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * StorageException.Code code = StorageException.Code.INTERNAL;
         * }</pre>
         *
         * @return The error code number
         */
        public int getCode() {
            return code;
        }

        /**
         * Returns the descriptive error message.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * StorageException.Code code = StorageException.Code.INTERNAL;
         * }</pre>
         *
         * @return The error message
         */
        public String getMessage() {
            return message;
        }

        /**
         * Returns the error code enum value for the given numeric code.
         * If no matching code is found, returns UNKNOWN_ERROR.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * StorageException.Code code = StorageException.Code.INTERNAL;
         * }</pre>
         *
         * @param code The numeric error code to look up
         * @return The corresponding Code enum value, or UNKNOWN_ERROR if not found
         */
        public static Code fromCode(int code) {
            for (Code error : values()) {
                if (error.getCode() == code) {
                    return error;
                }
            }
            return UNKNOWN_ERROR;
        }
    }

    /** The specific error code for this exception */
    @NonNull
    private final Code code;

    /** The underlying cause of this exception, if any */
    @Nullable
    private Throwable cause;

    /**
     * Creates a new StorageException with the specified message and error code.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageException error = new StorageException(
     *     "Upload failed", StorageException.Code.INTERNAL);
     * }</pre>
     *
     * @param detailMessage The detailed error message describing what went wrong
     * @param code The error code indicating the type of failure
     */
    public StorageException(@NonNull String detailMessage, @NonNull Code code) {
        super(detailMessage);
        this.code = code;
    }

    /**
     * Creates a new StorageException with the specified message, error code, and cause.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageException error = new StorageException(
     *     "Upload failed", StorageException.Code.INTERNAL);
     * }</pre>
     *
     * @param detailMessage The detailed error message describing what went wrong
     * @param code The error code indicating the type of failure
     * @param cause The underlying cause of this exception, or null if none
     */
    public StorageException(@NonNull String detailMessage,
                            @NonNull Code code,
                            @Nullable Throwable cause) {
        super(detailMessage);
        this.code = code;
        this.cause = cause;
    }

    /**
     * Legacy constructor for backward compatibility.
     *
     * @param error The exception that caused this StorageException
     * @param code The error code
     */
    @Deprecated
    protected StorageException(Exception error, StorageErrorCode code) {
        super(error.getMessage());
        this.code = Code.fromCode(code.getCode());
        this.cause = error;
    }

    /**
     * Returns the error code associated with this exception.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageException error = new StorageException(
     *     "Upload failed", StorageException.Code.INTERNAL);
     * }</pre>
     *
     * @return The Code enum value representing the specific error type
     */
    @NonNull
    public Code getErrorCode() {
        return this.code;
    }

    /**
     * Returns the cause of this exception, or null if the cause is nonexistent or unknown.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * StorageException error = new StorageException(
     *     "Upload failed", StorageException.Code.INTERNAL);
     * }</pre>
     *
     * @return The cause of this exception, or null
     */
    @Nullable
    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
