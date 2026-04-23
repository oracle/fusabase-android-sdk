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

import static com.oracle.mobile.fusabase.auth.AuthProvider.API_KEY;
import static com.oracle.mobile.fusabase.auth.AuthProvider.BAAS_SERVICES;
import static com.oracle.mobile.fusabase.auth.AuthProvider.DEVICE;
import static com.oracle.mobile.fusabase.auth.AuthProvider.GITHUB;
import static com.oracle.mobile.fusabase.auth.AuthProvider.IDCS_PATH;
import static com.oracle.mobile.fusabase.auth.AuthProvider.IDM_PATH;
import static com.oracle.mobile.fusabase.auth.AuthProvider.METHOD;
import static com.oracle.mobile.fusabase.auth.AuthProvider.MOBILE;
import static com.oracle.mobile.fusabase.auth.AuthProvider.ONPREM_PATH;
import static com.oracle.mobile.fusabase.auth.AuthProvider.SOCIAL;
import static com.oracle.mobile.fusabase.auth.AuthProvider.SOCIAL_IDP;
import static com.oracle.mobile.fusabase.auth.AuthProvider.UNDERSCORE_PATH;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.utils.Utils;

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
        if (auth.getConfig().authType.equals("idcs")) {
            this.AUTH_URL = Utils.addQueryParameterToURL(Utils.urlBuilder(auth.getConfig().domainURL,
                            UNDERSCORE_PATH,
                            BAAS_SERVICES,
                            IDM_PATH,
                            IDCS_PATH,
                            auth.getConfig().projectId,
                            SOCIAL),
                    DEVICE, MOBILE,
                    API_KEY, auth.getConfig().appId
            );
        } else
            this.AUTH_URL = Utils.addQueryParameterToURL(Utils.urlBuilder(auth.getConfig().domainURL,
                            Config.UNDERSCORE_PATH,
                            BAAS_SERVICES,
                            Config.IDM_PATH,
                            ONPREM_PATH,
                            auth.getConfig().projectId,
                            SOCIAL_IDP),
                    METHOD, PROVIDER_ID,
                    DEVICE, MOBILE,
                    API_KEY, auth.getConfig().appId
            );
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
