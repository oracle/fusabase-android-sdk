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
import androidx.annotation.Nullable;

/**
 * Represents a Google authentication provider.
 * <p>
 * This class extends the AuthProvider abstract class and provides additional functionality for handling Google authentication.
 *</p>
 */
public class GoogleAuthProvider extends FederatedAuthProvider {

    /**
     * The sign-in method for Google authentication.
     */
    public static final String PROVIDER_ID = "google";

    /**
     * The sign-in method for Google authentication.
     */
    public static final String GOOGLE_SIGN_IN_METHOD = "google";

    private final String AUTH_URL;

    /**
     * Constructs a new GoogleAuthProvider object.
     *
     * @param auth {@code FusabaseAuth} instance
     */
    public GoogleAuthProvider(@NonNull FusabaseAuth auth) {
        super();
        this.AUTH_URL = AuthProvider.buildSocialAuthUrl(auth, PROVIDER_ID);
    }

    /**
     * Gets the authorization URL for Google authentication only for IDCS.
     * We have moved the base auth type to SDK based login for Google.
     *
     * @return The authorization URL.
     */
    protected String getAuthUrl() {
        return this.AUTH_URL;
    }

    @NonNull
    @Override
    String getProviderId() {
        return PROVIDER_ID;
    }

    /**
     * Helps in creating an AuthCredential from the Google ID Token.
     * At least one of the token must be non-null.
     * @param idToken {@code String} ID Token
     * @param accessToken {@code String} Access Token
     * @return AuthCredential for the provided token
     */
    @NonNull
    public static AuthCredential getCredential(@Nullable String idToken, @Nullable String accessToken)
            throws IllegalArgumentException{

        if(idToken == null && accessToken == null)
            throw new IllegalArgumentException("Either IdToken or AccessToken must be non null.");
        return new GoogleAuthCredential(PROVIDER_ID, GOOGLE_SIGN_IN_METHOD, idToken);
    }
}
