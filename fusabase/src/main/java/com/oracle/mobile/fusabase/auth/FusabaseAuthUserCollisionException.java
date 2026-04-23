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
 * Exception thrown when there is a collision between two user accounts.
 */
public class FusabaseAuthUserCollisionException extends FusabaseAuthException{

    enum Code {
        ERROR_EMAIL_ALREADY_IN_USE(1001, "Email is already in use by different account."),
        ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL(1002, "This user doesn't exists."),
        ERROR_CREDENTIAL_ALREADY_IN_USE(1003, "Following credential is already in use by another account."),
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

        public static FusabaseAuthUserCollisionException.Code fromCode(int code) {
            for (FusabaseAuthUserCollisionException.Code error : values()) {
                if (error.getCode() == code) {
                    return error;
                }
            }
            return UNKNOWN_ERROR;
        }
    }

    /**
     * Constructs a new FusabaseAuthUserCollisionException with the specified error code and detail message.
     *
     * @param errorCode    Error code associated with the exception.
     * @param detailMessage Detailed message describing the exception.
     */
    public FusabaseAuthUserCollisionException(@NonNull String errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
