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
 * Represents a Github authentication provider.
 * <p>
 * This class extends the AuthProvider abstract class and provides additional functionality for handling Github authentication.
 * </p>
 */
public class GithubAuthProvider extends FederatedAuthProvider  {
    /**
     * The provider ID for Github authentication.
     */
    public static final String PROVIDER_ID = "github";

    /**
     * The sign-in method for Github authentication.
     */
    public static final String GITHUB_SIGN_IN_METHOD = "github";

    private final String AUTH_URL;

    /**
     * Constructs a new GithubAuthProvider object.
     *
     * @param auth {@code FusabaseAuth} instance
     */
    public GithubAuthProvider(@NonNull FusabaseAuth auth) {
        super();
        this.AUTH_URL = AuthProvider.buildSocialAuthUrl(auth, PROVIDER_ID);
    }

    /**
     * Gets the authorization URL for Github authentication.
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

    @NonNull
    public static AuthCredential getCredential(@NonNull String token) {
        if(token == null)
            throw new IllegalArgumentException("Invalid token provided.");
        return new GithubAuthCredential(PROVIDER_ID, GITHUB_SIGN_IN_METHOD, token);
    }
}
