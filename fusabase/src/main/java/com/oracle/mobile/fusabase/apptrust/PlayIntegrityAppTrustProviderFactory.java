// Copyright (c) 2015, 2026, Oracle and/or its affiliates.

package com.oracle.mobile.fusabase.apptrust;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseApp;

/**
 * An {@link AppTrustProviderFactory} that creates Play Integrity App Trust providers.
 *
 * <p>This is the default Android implementation for applications that use Google Play Integrity
 * and the Oracle Backend for Firebase App Trust token-exchange endpoint.</p>
 */
public class PlayIntegrityAppTrustProviderFactory implements AppTrustProviderFactory {
    /** Shared factory instance for installation into {@link FusabaseAppTrust}. */
    public static final PlayIntegrityAppTrustProviderFactory instance =
            new PlayIntegrityAppTrustProviderFactory();

    /** Creates a Play Integrity App Trust provider factory. */
    public PlayIntegrityAppTrustProviderFactory() {
    }

    /**
     * Returns a Play Integrity App Trust provider for the supplied Fusabase app.
     *
     * @param app the initialized Fusabase app
     * @return a provider that obtains App Trust tokens through Play Integrity
     */
    @Override
    @NonNull
    public AppTrustProvider create(@NonNull FusabaseApp app) {
        return new PlayIntegrityAppTrustProvider(app);
    }

    /**
     * Returns the shared Play Integrity App Trust provider factory.
     *
     * @return the factory instance for installation into {@link FusabaseAppTrust}
     */
    @NonNull
    public static PlayIntegrityAppTrustProviderFactory getInstance() {
        return instance;
    }
}
