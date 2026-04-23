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

import android.os.Parcel;

import androidx.annotation.*;

import java.util.Objects;

/**
 * Represents a Google authentication credential.
 *<p>
 * This class extends the AuthCredential abstract class and provides additional functionality for handling Google authentication.
 *</p>
 */
public class GoogleAuthCredential extends AuthCredential {
    @NonNull
    private final String provider;
    @NonNull
    private final String signInMethod;
    @NonNull
    private final String accessToken;

    /**
     * Constructs a new GoogleAuthCredential object.
     *
     * @param provider The provider associated with this credential.
     * @param signInMethod The sign-in method associated with this credential.
     * @param accessToken The access token associated with this credential.
     */
    protected GoogleAuthCredential(@NonNull String provider,
                                   @NonNull String signInMethod,
                                   @NonNull String accessToken) {
        this.provider = provider;
        this.signInMethod = signInMethod;
        this.accessToken = accessToken;
    }

    /**
     * Gets the provider associated with this credential.
     *
     * @return The provider.
     */
    public @NonNull String getProvider() {
        return provider;
    }

    /**
     * Gets the sign-in method associated with this credential.
     *
     * @return The sign-in method.
     */
    public @NonNull String getSignInMethod() {
        return signInMethod;
    }

    @NonNull
    @Override
    protected String getIdToken() {
        return this.accessToken;
    }

}
