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

import com.oracle.mobile.fusabase.utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import jakarta.json.JsonObject;

/**
 * Represents the result of a token retrieval operation, containing the token
 * and associated metadata extracted from an ID token.
 *
 * This class encapsulates information from a JWT token including timestamps,
 * claims, and provider details. It is typically created from an {@link IdToken}
 * and provides convenient access to token properties.
 */
public class GetTokenResult {

    /** The authentication provider used for sign-in. */
    private final String provider;
    /** The timestamp when the token was issued, in seconds since Unix epoch. */
    private final long issuedAtTime;
    /** The timestamp when the token expires, in seconds since Unix epoch. */
    private final long expirationTime;
    /** The sign-in provider (may be different from provider in some cases). */
    private final String signInProvider;
    /** The timestamp when authentication occurred, in seconds since Unix epoch. */
    private final long authTime;

    /**
     * A map of claims extracted from the token.
     */
    public final Map<String, Object> claims = new HashMap<>();
    /**
     * The token itself, or null if no token was obtained.
     */
    public final @Nullable String token;

     /**
     * Constructs a new GetTokenResult object from an IdToken.
     *
     * @param idToken The IdToken to extract information from.
     */
    protected GetTokenResult (@NonNull IdToken idToken) {
        this.token = idToken.getToken();
        this.provider = idToken.signInProvider == null ? "password" : idToken.signInProvider;
        JsonObject parsedJWT = (JsonObject) Utils.parseJWT(token);
        this.issuedAtTime = new BigDecimal(parsedJWT.getJsonNumber("iat").toString())
                .setScale(0, RoundingMode.HALF_UP).longValue();
        this.expirationTime = new BigDecimal (parsedJWT.getJsonNumber("exp").toString())
                .setScale(0, RoundingMode.HALF_UP).longValue();
        this.signInProvider = provider;
        this.authTime = new BigDecimal(parsedJWT.getJsonNumber("iat").toString())
                .setScale(0, RoundingMode.HALF_UP).longValue();
        claims.put("auth_time", new BigDecimal (parsedJWT.getJsonNumber("iat").toString())
                .setScale(0, RoundingMode.HALF_UP).longValue());
        claims.put("exp", new BigDecimal (parsedJWT.getJsonNumber("exp").toString())
                .setScale(0, RoundingMode.HALF_UP).longValue());
        claims.put("iat", new BigDecimal (parsedJWT.getJsonNumber("iat").toString())
                .setScale(0, RoundingMode.HALF_UP).longValue());
        claims.put("sub", parsedJWT.getString("sub"));
    }

    /**
     * Gets the authentication timestamp, in seconds since the Unix epoch.
     *
     * @return The authentication timestamp.
     */
    public long getAuthTimestamp() {
        return this.authTime;
    }

    /**
     * Gets the map of claims extracted from the token.
     *
     * @return The map of claims.
     */
    public @NonNull Map<String, Object> getClaims () {
        return this.claims;
    }

    /**
     * Gets the expiration timestamp, in seconds since the Unix epoch.
     *
     * @return The expiration timestamp.
     */
    public long getExpirationTimestamp () {
        return this.expirationTime;
    }

    /**
     * Gets the issued-at timestamp, in seconds since the Unix epoch.
     *
     * @return The issued-at timestamp.
     */
    public long getIssuedAtTimestamp() {
        return this.issuedAtTime;
    }

    /**
     * Gets the sign-in provider associated with this token.
     *
     * @return The sign-in provider, or null if not available.
     */
    public @Nullable String getSignInProvider() {
        return this.provider;
    }

    /**
     * Gets the token itself, or null if no token was obtained.
     *
     * @return The token, or null.
     */
    public @Nullable String getToken() {
        return this.token;
    }

}
