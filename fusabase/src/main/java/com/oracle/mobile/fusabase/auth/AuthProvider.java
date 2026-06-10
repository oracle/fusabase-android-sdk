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

import com.oracle.mobile.fusabase.utils.Utils;

/**
 * Abstract Class AuthProvider
 * Represents an authentication provider that helps in performing social login
 */
public abstract class AuthProvider {

    protected final static String UNDERSCORE_PATH = "_";
    protected final static String BAAS_SERVICES = "baas-services";
    protected final static String IDM_PATH = "idm";
    protected final static String ONPREM_PATH = "onprem";
    protected final static String SOCIAL_IDP = "socialidp";
    protected final static String AUTH_ID = "auth_id";
    protected final static String APP_ID = "app_id";
    protected final static String API_KEY = "apiKey";
    protected final static String DEVICE = "device";
    protected final static String MOBILE = "mobile";
    protected final static String METHOD = "method";
    protected final static String GOOGLE = "google";
    protected final static String GITHUB = "github";
    protected final static String FACEBOOK = "facebook";
    protected final static String IDCS_PATH = "idcs";
    protected final static String SOCIAL = "social";

    protected abstract String getAuthUrl();

    @NonNull
    protected static String buildSocialAuthUrl(@NonNull FusabaseAuth auth,
                                               @NonNull String providerId) {
        Config config = auth.getConfig();
        if (Config.IDCS_AUTH_TYPE.equals(config.authType)) {
            return Utils.addQueryParameterToURL(Utils.urlBuilder(config.domainURL,
                            UNDERSCORE_PATH,
                            BAAS_SERVICES,
                            IDM_PATH,
                            IDCS_PATH,
                            config.projectId,
                            SOCIAL),
                    DEVICE, MOBILE,
                    API_KEY, config.appId);
        }

        return Utils.addQueryParameterToURL(Utils.urlBuilder(config.domainURL,
                        UNDERSCORE_PATH,
                        BAAS_SERVICES,
                        IDM_PATH,
                        ONPREM_PATH,
                        config.projectId,
                        SOCIAL_IDP),
                METHOD, providerId,
                DEVICE, MOBILE,
                API_KEY, config.appId);
    }

    private final String provider;
    private final String signInMethod;

    protected AuthProvider(@NonNull String provider,
                 @NonNull String signInMethod) {
        this.provider = provider;
        this.signInMethod = signInMethod;
    }

    /**
     * Method to get the provider id
     * @return {@code String}
     */
    public String getProvider() {
        return this.provider;
    }

    /**
     * Method to get the sign in method for the auth provider
     * @return {@code String}
     */
    public String getSignInMethod() {
        return this.signInMethod;
    }
}
