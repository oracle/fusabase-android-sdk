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


package com.oracle.mobile.fusabase.apptrust;

import androidx.annotation.NonNull;

/**
 * Holds a token emitted by Oracle Backend for Firebase App Trust after successful application
 * verification.
 *
 * <p>The token is the backend-issued output of an App Trust verification flow. Its representation
 * is independent of the attestation provider that verified the application.</p>
 */
public class AppTrustToken {
    private final String token;
    private final long expireTimeMillis;

    /**
     * Creates an App Trust token.
     *
     * @param token the raw backend-issued token that attests to the application's identity
     * @param expireTimeMillis the token expiration time in milliseconds since the Unix epoch
     */
    public AppTrustToken(@NonNull String token, long expireTimeMillis) {
        this.token = token;
        this.expireTimeMillis = expireTimeMillis;
    }

    /**
     * Returns the raw token that attests to the application's identity.
     *
     * @return the raw App Trust token
     */
    @NonNull
    public String getToken() {
        return token;
    }

    /**
     * Returns the time at which this token expires.
     *
     * @return the expiration time in milliseconds since the Unix epoch
     */
    public long getExpireTimeMillis() {
        return expireTimeMillis;
    }
}
