// Copyright (c) 2015, 2026, Oracle and/or its affiliates.

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
