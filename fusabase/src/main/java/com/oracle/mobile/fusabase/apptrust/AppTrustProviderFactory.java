// Copyright (c) 2015, 2026, Oracle and/or its affiliates.

package com.oracle.mobile.fusabase.apptrust;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseApp;

/** A factory that creates an {@link AppTrustProvider} for a Fusabase application. */
public interface AppTrustProviderFactory {
    /**
     * Returns the App Trust provider associated with the supplied app, creating it when needed.
     *
     * @param app the initialized Fusabase application
     * @return the provider for {@code app}
     */
    @NonNull
    AppTrustProvider create(@NonNull FusabaseApp app);
}
