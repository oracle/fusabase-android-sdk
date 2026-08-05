// Copyright (c) 2015, 2025, Oracle and/or its affiliates.
package com.oracle.mobile.fusabase.apptrust;

import androidx.annotation.NonNull;

/**
 * @deprecated Use {@link AppTrustToken}. This compatibility type will be removed in a future
 *             major release.
 */
@Deprecated
public final class FusabaseAppTrustToken extends AppTrustToken {
    public FusabaseAppTrustToken(@NonNull String token, long expireTimeMillis) {
        super(token, expireTimeMillis);
    }
}
