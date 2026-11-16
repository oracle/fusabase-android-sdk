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

    private final Long cloudProjectNumber;

    /** Creates a Play Integrity App Trust provider factory using the Play Console association. */
    public PlayIntegrityAppTrustProviderFactory() {
        this.cloudProjectNumber = null;
    }

    /**
     * Creates a provider factory with an explicit Google Cloud project number.
     *
     * <pre>{@code
     * FusabaseAppTrust.getInstance(app).installAppTrustProviderFactory(
     *     new PlayIntegrityAppTrustProviderFactory(123456789012L), true);
     * }</pre>
     *
     * @param cloudProjectNumber numeric Google Cloud project number, not the Fusabase project ID
     * @throws IllegalArgumentException if the project number is not positive
     */
    public PlayIntegrityAppTrustProviderFactory(long cloudProjectNumber) {
        if (cloudProjectNumber <= 0) {
            throw new IllegalArgumentException("Google Cloud project number must be positive");
        }
        this.cloudProjectNumber = cloudProjectNumber;
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
        return new PlayIntegrityAppTrustProvider(app, cloudProjectNumber);
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
