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

import com.oracle.mobile.fusabase.utils.SecureString;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;

class OAuthCredentialImpl extends OAuthCredential {

    private final String providerId;
    private final SecureString secureAccessToken;
    private final SecureString secureIdToken;
    private final String nonce;

    public OAuthCredentialImpl (@NonNull String providerId,
                                @Nullable String accessToken,
                                @Nullable String idToken,
                                @Nullable String nonce) {
        this.providerId = providerId;
        this.secureAccessToken = accessToken != null ? createSecureString(accessToken) : null;
        this.secureIdToken = idToken != null ? createSecureString(idToken) : null;
        this.nonce = nonce;
    }

    private SecureString createSecureString(String token) {
        try {
            return new SecureString(token);
        } catch (GeneralSecurityException ex) {
            return null;
        }
    }

    @Nullable
    @Override
    public String getAccessToken() {
        return this.secureAccessToken != null ? this.secureAccessToken.getDecryptedString() : null;
    }

    @Nullable
    @Override
    public String getIdToken() {
        return this.secureIdToken != null ? this.secureIdToken.getDecryptedString() : null;
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
