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
import static com.oracle.mobile.fusabase.auth.AuthProvider.METHOD;
import static com.oracle.mobile.fusabase.auth.AuthProvider.MOBILE;
import static com.oracle.mobile.fusabase.auth.AuthProvider.ONPREM_PATH;
import static com.oracle.mobile.fusabase.auth.AuthProvider.SOCIAL_IDP;
import static com.oracle.mobile.fusabase.auth.Config.IDM_PATH;
import static com.oracle.mobile.fusabase.auth.Config.UNDERSCORE_PATH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.utils.Utils;

import java.util.List;
import java.util.Map;

/**
 * Authentication provider for OAuth 2.0 identity providers.
 *
 * This class provides authentication functionality for various OAuth 2.0 identity providers
 * such as Google, Facebook, GitHub, etc. It extends {@link FederatedAuthProvider} and
 * implements the specific authentication flow for OAuth-based authentication.
 */
public class OAuthProvider extends FederatedAuthProvider {

    /** The unique identifier for this authentication provider. */
    private final String providerId;
    /** The complete authentication URL for OAuth login flow. */
    private final String authUrl;

    /**
     * Constructs a new OAuthProvider with the specified builder configuration.
     *
     * @param builder The builder containing the configuration for this provider.
     */
    private OAuthProvider (@NonNull Builder builder) {
        this.providerId = builder.providerId;
        FusabaseAuth auth = FusabaseAuth.getInstance();
        this.authUrl = Utils.addQueryParameterToURL(Utils.urlBuilder(auth.getConfig().domainURL,
                        UNDERSCORE_PATH,
                        BAAS_SERVICES,
                        IDM_PATH,
                        ONPREM_PATH,
                        auth.getConfig().projectId,
                        SOCIAL_IDP),
                METHOD, providerId,
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
    private OAuthProvider () {
        this.providerId = null;
        this.authUrl = "";
    }

    /**
     * Builder class for constructing OAuthProvider instances.
     *
     * This builder allows for fluent configuration of OAuth authentication provider
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
         * @param providerId The unique identifier for the authentication provider.
         */
        public Builder(@NonNull String providerId) {
            this.providerId = providerId;
        }

        /**
         * Private constructor for internal use.
         */
        private Builder () {
            this.providerId = null;
        }

        /**
         * Adds a custom parameter to the authentication request.
         *
         * @param paramKey The parameter key.
         * @param paramValue The parameter value.
         * @return This builder instance for method chaining.
         */
        public @NonNull OAuthProvider.Builder addCustomParameter(@NonNull String paramKey, @NonNull String paramValue) {
            throw new UnsupportedOperationException("Custom Parameters are not supported");
        }

        /**
         * Sets custom parameters for the authentication request.
         *
         * @param customParameters A map of custom parameters.
         * @return This builder instance for method chaining.
         */
        public @NonNull OAuthProvider.Builder addCustomParameters(@NonNull Map<String, String> customParameters) {
            throw new UnsupportedOperationException("Custom Parameters are not supported");
        }

        /**
         * Builds and returns a new OAuthProvider instance.
         *
         * @return A new OAuthProvider configured with this builder's settings.
         */
        public @NonNull OAuthProvider build() {
            return new OAuthProvider(this);
        }

        /**
         * Sets the OAuth scopes to request during authentication.
         *
         * @param scopes List of OAuth scopes.
         * @return This builder instance for method chaining.
         */
        public @NonNull OAuthProvider.Builder setScopes(@NonNull List<String> scopes) {
            throw new UnsupportedOperationException("Custom Parameters are not supported");
        }
    }

    /**
     * Builder class for constructing OAuth authentication credentials.
     *
     * This builder creates {@link AuthCredential} instances for OAuth authentication
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
         * @return A new OAuthCredentialImpl configured with this builder's settings.
         */
        public @NonNull AuthCredential build() {
            return new OAuthCredentialImpl(this.providerId, null, idToken, rawNonce);
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
     * Creates a new Builder instance for constructing OAuthProvider objects.
     *
     * @param providerId The unique identifier for the authentication provider.
     * @return A new Builder instance.
     * @throws IllegalArgumentException If the providerId is null or empty.
     */
    public static @NonNull OAuthProvider.Builder newBuilder(@NonNull String providerId) throws IllegalArgumentException{

        if (providerId.isEmpty())
        {
            throw new IllegalArgumentException("Empty providerId received. Kindly provide a valid providerId");
        }

        return new OAuthProvider.Builder(providerId);
    }

    /**
     * Creates a new Builder instance for constructing OAuthProvider objects.
     *
     * @param providerId The unique identifier for the authentication provider.
     * @param fusabaseAuth The FusabaseAuth instance (currently unused).
     * @return A new Builder instance.
     * @throws IllegalArgumentException If the providerId is null or empty.
     */
    public static @NonNull OAuthProvider.Builder newBuilder(@NonNull String providerId,
                                                            @NonNull FusabaseAuth fusabaseAuth) throws IllegalArgumentException{
        if (providerId.isEmpty())
        {
            throw new IllegalArgumentException("Empty providerId received. Kindly provide a valid providerId");
        }

        return new OAuthProvider.Builder(providerId);
    }

    /**
     * Creates a new CredentialBuilder instance for constructing authentication credentials.
     *
     * @param providerId The unique identifier for the authentication provider.
     * @return A new CredentialBuilder instance.
     * @throws IllegalArgumentException If the providerId is null or empty.
     */
    public static @NonNull CredentialBuilder newCredentialBuilder(@NonNull String providerId) throws IllegalArgumentException {

        if (providerId.isEmpty())
        {
            throw new IllegalArgumentException("Empty providerId received. Kindly provide a valid providerId");
        }

        return new OAuthProvider.CredentialBuilder(providerId);
    }
}