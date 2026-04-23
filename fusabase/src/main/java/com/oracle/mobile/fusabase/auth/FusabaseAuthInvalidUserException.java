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

/**
 * Exception thrown when there is an issue with a user's account.
 */
public class FusabaseAuthInvalidUserException extends FusabaseAuthException{

    protected enum Code {
        ERROR_USER_DISABLED(1001, "This user is disabled from BAAS Console."),
        ERROR_USER_NOT_FOUND(1002, "This user doesn't exists."),
        ERROR_USER_TOKEN_EXPIRED(1003, "This user's token has expired."),
        ERROR_INVALID_USER_TOKEN(1004, "This user's token is invalid."),
        UNKNOWN_ERROR(9999, "Unknown error encountered.");

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

        public static FusabaseAuthInvalidUserException.Code fromCode(int code) {
            for (FusabaseAuthInvalidUserException.Code error : values()) {
                if (error.getCode() == code) {
                    return error;
                }
            }
            return UNKNOWN_ERROR;
        }
    }

    /**
     * Constructs a new FusabaseAuthInvalidUserException with the specified error code and detail message.
     *
     * @param errorCode    Error code associated with the exception.
     * @param detailMessage Detailed message describing the exception.
     */
    public FusabaseAuthInvalidUserException(@NonNull String errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
