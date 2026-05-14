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

import androidx.annotation.Nullable;

/**
 * Abstract base class for OAuth authentication credentials.
 *
 * This class represents OAuth 2.0 credentials containing access tokens and ID tokens
 * obtained from OAuth providers. It extends {@link AuthCredential} and provides
 * the foundation for specific OAuth credential implementations.
 */
public abstract class OAuthCredential extends AuthCredential {

    /** The ID token obtained from the OAuth provider. */
    private final String idToken;
    /** The access token obtained from the OAuth provider. */
    private final String accessToken;

    /**
     * Default constructor for OAuthCredential.
     */
    public OAuthCredential() {
        this.idToken = null;
        this.accessToken = null;
    }

    /**
     * Gets the access token from the OAuth credential.
     *
     * @return The access token, or null if not available.
     */
    public abstract @Nullable String getAccessToken();

    /**
     * Gets the ID token from the OAuth credential.
     *
     * @return The ID token, or null if not available.
     */
    public abstract @Nullable String getIdToken();

    /**
     * Gets the client secret for the OAuth credential.
     *
     * Will be null for this implementation since we only support OAuth 2.0 (OIDC).
     *
     * @return The client secret, or null if not applicable.
     */
    public abstract @Nullable String getSecret();
}
