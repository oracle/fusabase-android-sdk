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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;

/**
 * Exception class for Oracle Database operations in the FUSABASE SDK.
 *
 * <p>This exception is thrown by various database operations when errors occur,
 * providing structured error information including error codes and detailed messages.
 * It extends the base {@link FusabaseException} and adds specific error codes for
 * different types of database operation failures.</p>
 *
 * <p>The exception includes predefined error codes for common scenarios such as
 * authentication failures, permission issues, network errors, and internal errors.</p>
 */
public class FusabaseOracledbException extends FusabaseException {

    /**
     * Enumeration of error codes for different types of database operation failures.
     * Each error code includes an HTTP-style status code and a descriptive message.
     */
    public enum Code {
        /** Invalid argument provided to a database operation */
        INVALID_ARGUMENT(400, "invalid-argument"),

        /** User is not authenticated */
        UNAUTHENTICATED(401, "unauthenticated"),

        /** User does not have permission for the requested operation */
        PERMISSION_DENIED(403, "permission-denied"),

        /** Requested resource was not found */
        NOT_FOUND(404, "not-found"),

        /** Operation was aborted, typically due to a conflict */
        ABORTED(499, "aborted"),

        /** Internal server error occurred */
        INTERNAL(500, "internal"),

        /** Requested operation is not implemented */
        NOT_IMPLEMENTED(501, "not-implemented"),

        /** Unknown error occurred */
        UNKNOWN_ERROR(0, "unknown"),

        /** Input/Output operation failed */
        IO_EXCEPTION(1000, "io-error"),

        /** Required file was not found */
        FILE_NOT_FOUND(1001, "file-not-found"),

        /** File could not be created */
        CANNOT_CREATE_FILE(1002, "cannot-create-file"),

        /** Operation was cancelled */
        CANCELLED(1003, "cancelled"),

        /** Network connectivity error occurred */
        NETWORK_ERROR(1004, "network-error");

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
         * @return The error code number
         */
        public int getCode() {
            return code;
        }

        /**
         * Returns the descriptive error message.
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
     * Creates a new FusabaseOracledbException with the specified message and error code.
     *
     * @param detailMessage The detailed error message describing what went wrong
     * @param code The error code indicating the type of failure
     */
    public FusabaseOracledbException(@NonNull String detailMessage, @NonNull Code code) {
        super(detailMessage);
        this.code = code;
    }

    /**
     * Creates a new FusabaseOracledbException with the specified message, error code, and cause.
     *
     * @param detailMessage The detailed error message describing what went wrong
     * @param code The error code indicating the type of failure
     * @param cause The underlying cause of this exception, or null if none
     */
    public FusabaseOracledbException(@NonNull String detailMessage,
                                  @NonNull Code code,
                                  @Nullable Throwable cause) {
        super(detailMessage);
        this.code = code;
        this.cause = cause;
    }

    /**
     * Returns the error code associated with this exception.
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
     * @return The cause of this exception, or null
     */
    @Nullable
    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
