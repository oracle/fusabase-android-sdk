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
 * Class EmailAuthProvider
 * Represents the email password login workflow
 */
public class EmailAuthProvider {

    /**
     * Email password sign in method Id
     */
    public static final String EMAIL_PASSWORD_SIGN_IN_METHOD = "epw";
    /**
     * Provider Id
     */
    public static final String PROVIDER_ID = "epw";

    /**
     * Method to get the Auth credential for the provided email and password
     * @param email {@code String} Email
     * @param password {@code String} Password
     * @return {@code AuthCredential} for the provided credentials
     */
    public static @NonNull AuthCredential getCredential(@NonNull String email, @NonNull String password) {
        return new EmailAuthCredential(PROVIDER_ID,
                EMAIL_PASSWORD_SIGN_IN_METHOD,
                email,
                password);
    }

}
