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

package com.oracle.mobile.fusabase.auth;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseException;

/**
 * Exception thrown when there is an issue with authentication.
 */
public class FusabaseAuthException extends FusabaseException {

    /**
     * Enumerates possible error codes for authentication exceptions.
     */
    protected enum Code {
        INVALID_ARGUMENT(400, "invalid-argument"),
        UNAUTHENTICATED(401, "unauthenticated"),
        PERMISSION_DENIED(403, "permission-denied"),
        NOT_FOUND(404, "not-found"),
        ABORTED(499, "aborted"),
        INTERNAL(500, "internal"),
        NOT_IMPLEMENTED(501, "not-implemented"),
        UNKNOWN_ERROR(0, "unknown"),
        IO_EXCEPTION(1000, "io-error"),
        FILE_NOT_FOUND(1001, "file-not-found"),
        CANNOT_CREATE_FILE(1002, "cannot-create-file"),
        CANCELLED(1003, "cancelled"),
        NETWORK_ERROR(1004, "FUSABASE encountered a network error." +
                " Kindly retry when the network is available.");

        private final int code;
        private final String message;

        Code(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public static Code fromCode(int code) {
            for (Code error : values()) {
                if (error.getCode() == code) {
                    return error;
                }
            }
            return UNKNOWN_ERROR;
        }
    }

    /**
     * Error code associated with this exception.
     */
    public final String errorCode;

     /**
     * Constructs a new FusabaseAuthException with the specified error code and detail message.
     *
     * @param errorCode    Error code.
     * @param detailMessage Detail message.
     */
    public FusabaseAuthException (@NonNull String errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    /**
     * Gets the error code associated with this exception.
     *
     * @return Error code.
     */
    public String getErrorCode() {
        return errorCode;
    }
}
