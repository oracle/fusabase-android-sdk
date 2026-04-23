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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

class SAMLAuthCredentialImpl extends OAuthCredential {

    private final String providerId;
    private final String accessToken;
    private final String idToken;
    private final String nonce;

    public SAMLAuthCredentialImpl (@NonNull String providerId,
                                @Nullable String accessToken,
                                @Nullable String idToken,
                                @Nullable String nonce) {
        this.providerId = providerId;
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.nonce = nonce;
    }

    /**
     * Constructs a new GithubAuthCredential object from a parcel.
     *
     * @param in The parcel containing the data.
     */
    protected SAMLAuthCredentialImpl(Parcel in) {
        this.providerId = Objects.requireNonNull(in.readString());
        this.accessToken = null;
        this.idToken = null;
        this.nonce = null;
    }

    @Nullable
    @Override
    public String getAccessToken() {
        return this.accessToken;
    }

    @Nullable
    @Override
    public String getIdToken() {
        return this.idToken;
    }

    // Will always be null
    @Nullable
    @Override
    public String getSecret() {
        return null;
    }

    @NonNull
    @Override
    public String getProvider() {
        return this.providerId;
    }

    // We will return provider in signInMethod
    @NonNull
    @Override
    public String getSignInMethod() {
        return this.providerId;
    }

}
