// Copyright (c) 2015, 2026, Oracle and/or its affiliates.

package com.oracle.mobile.fusabase.apptrust;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.task.Task;

/** App Trust provider that obtains assertions from Google Play Integrity. */
final class PlayIntegrityAppTrustProvider implements AppTrustProvider {
    private final FusabaseApp app;
    private final PlayIntegrityAttestationProvider attestationProvider;
    private final FusabaseAppTrustTokenExchange tokenExchange;

    PlayIntegrityAppTrustProvider(@NonNull FusabaseApp app) {
        this.app = app;
        this.attestationProvider = new PlayIntegrityAttestationProvider(app.getApplicationContext());
        this.tokenExchange = new FusabaseAppTrustTokenExchange(app);
    }

    @Override
    @NonNull
    public Task<AppTrustToken> getToken() {
        return FusabaseAppTrust.getInstance(app)
                .requestTokenFromPlayIntegrity(attestationProvider, tokenExchange);
    }
}
