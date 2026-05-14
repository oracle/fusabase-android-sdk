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
import static com.oracle.mobile.fusabase.auth.AuthProvider.IDCS_PATH;
import static com.oracle.mobile.fusabase.auth.AuthProvider.MOBILE;
import static com.oracle.mobile.fusabase.auth.AuthProvider.SOCIAL;
import static com.oracle.mobile.fusabase.auth.Config.IDM_PATH;
import static com.oracle.mobile.fusabase.auth.Config.UNDERSCORE_PATH;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.utils.Utils;

import java.util.List;
import java.util.Map;

/**
 * Authentication provider for Oracle Identity Cloud Service (IDCS).
 *
 * This class provides authentication functionality for Oracle's Identity Cloud Service,
 * allowing users to authenticate through various identity providers configured in IDCS.
 * It extends {@link FederatedAuthProvider} and implements the specific authentication
 * flow for IDCS-based authentication.
 */
public class IDCSAuthProvider extends FederatedAuthProvider {

    /**
     * The sign-in method for Google authentication.
     */
    public static final String PROVIDER_ID = "idcs";

    /**
     * The sign-in method for Google authentication.
     */
    public static final String IDCS_SIGN_IN_METHOD = "idcs";

    /** List of OAuth scopes requested during authentication. */
    private final List<String> scopes;
    /** Custom parameters to be included in the authentication request. */
    private final Map<String, String> customParameters;
    /** The unique identifier for this authentication provider. */
    private final String providerId;
    /** The complete authentication URL for IDCS login flow. */
    private final String authUrl;

    /**
     * Constructs a new IDCSAuthProvider with the specified builder configuration.
     *
     * @param builder The builder containing the configuration for this provider.
     */
    private IDCSAuthProvider (@NonNull Builder builder) {
        this.providerId = builder.providerId;
        this.customParameters = builder.customParameters;
        this.scopes = builder.scopes;
        FusabaseAuth auth = FusabaseAuth.getInstance();
        this.authUrl = Utils.addQueryParameterToURL(Utils.urlBuilder(auth.getConfig().domainURL,
                        UNDERSCORE_PATH,
                        BAAS_SERVICES,
                        IDM_PATH,
                        IDCS_PATH,
                        auth.getConfig().projectId,
                        SOCIAL),
                DEVICE, MOBILE,
                API_KEY, auth.getConfig().appId
        );
    }

    /**
     * {@inheritDoc}
     */
    @NonNull protected String getAuthUrl () {
        return this.authUrl;
    }

    /**
     * Default private constructor for internal use.
     */
    private IDCSAuthProvider () {
        this.providerId = null;
        this.scopes = null;
        this.customParameters = null;
        this.authUrl = "";
    }

    /**
     * Builder class for constructing IDCSAuthProvider instances.
     *
     * This builder allows for fluent configuration of IDCS authentication provider
     * settings including custom parameters and OAuth scopes.
     */
    public static class Builder {

        /** The provider identifier. */
        private final String providerId;
        /** Custom parameters for the authentication request. */
        private Map<String, String> customParameters;
        /** OAuth scopes to request during authentication. */
        private List<String> scopes;

        /**
         * Constructs a new Builder with the specified provider ID.
         *
         */
        public Builder() {
            this.providerId = PROVIDER_ID;
        }

        /**
         * Adds a custom parameter to the authentication request.
         *
         * @param paramKey The parameter key.
         * @param paramValue The parameter value.
         * @return This builder instance for method chaining.
         */
        public @NonNull IDCSAuthProvider.Builder addCustomParameter(@NonNull String paramKey, @NonNull String paramValue) {
            throw new UnsupportedOperationException("Custom Parameters are not supported");
        }

        /**
         * Sets custom parameters for the authentication request.
         *
         * @param customParameters A map of custom parameters.
         * @return This builder instance for method chaining.
         */
        public @NonNull IDCSAuthProvider.Builder addCustomParameters(@NonNull Map<String, String> customParameters) {
            throw new UnsupportedOperationException("Custom Parameters are not supported");
        }

        /**
         * Builds and returns a new IDCSAuthProvider instance.
         *
         * @return A new IDCSAuthProvider configured with this builder's settings.
         */
        public @NonNull IDCSAuthProvider build() {
            return new IDCSAuthProvider(this);
        }

        /**
         * Sets the OAuth scopes to request during authentication.
         *
         * @param scopes List of OAuth scopes.
         * @return This builder instance for method chaining.
         */
        public @NonNull IDCSAuthProvider.Builder setScopes(@NonNull List<String> scopes) {
            throw new UnsupportedOperationException("Custom Scopes are not supported");
        }
    }

    /**
     * Builder class for constructing IDCS authentication credentials.
     *
     * This builder creates {@link AuthCredential} instances for IDCS authentication
     * using ID tokens obtained from the authentication flow.
     */
    public static class CredentialBuilder {

        /** The ID token from the authentication provider. */
        private String idToken;
        /** The raw nonce value used in the authentication flow. */
        private String rawNonce;
        /** The provider identifier. */
        private final String providerId;

        /**
         * Constructs a new CredentialBuilder with the specified provider ID.
         *
         * @param providerId The unique identifier for the authentication provider.
         */
        private CredentialBuilder (@NonNull String providerId){
            this.providerId = providerId;
        }

        /**
         * Builds and returns a new AuthCredential instance.
         *
         * @return A new IDCSAuthCredential configured with this builder's settings.
         */
        public @NonNull AuthCredential build() {
            return new IDCSAuthCredential(this.providerId, this.providerId, idToken);
        }

        /**
         * Sets the ID token and raw nonce for the credential.
         *
         * @param idToken The ID token obtained from the authentication provider.
         * @param rawNonce The raw nonce value (may be null).
         * @return This builder instance for method chaining.
         */
        public @NonNull CredentialBuilder setIdTokenWithRawNonce(@NonNull String idToken, @Nullable String rawNonce) {
            this.idToken = idToken;
            this.rawNonce = rawNonce;
            return this;
        }
    }

    /**
     * {@inheritDoc}
     */
    public @Nullable String getProviderId() {
        return this.providerId;
    }

    /**
     * Creates a new Builder instance for constructing IDCSAuthProvider objects.
     *
     * @return A new Builder instance.
     */
    public static @NonNull IDCSAuthProvider.Builder newBuilder() {

        return new IDCSAuthProvider.Builder();
    }

    /**
     * Creates a new CredentialBuilder instance for constructing authentication credentials.
     *
     * @return A new CredentialBuilder instance.
     */
    public static @NonNull CredentialBuilder newCredentialBuilder() {

        return new IDCSAuthProvider.CredentialBuilder(PROVIDER_ID);
    }
}
