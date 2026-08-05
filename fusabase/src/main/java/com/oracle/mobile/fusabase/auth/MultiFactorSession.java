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

/**
 * Represents the session for a multi-factor enrollment or sign-in challenge.
 *
 * <p>Use this session with a factor-specific API; do not persist or expose its token.
 */
public class MultiFactorSession {
    private final String challengeId;
    private final String mfaSessionToken;
    private final MfaServiceHelper service;

    MultiFactorSession(@NonNull String challengeId, @NonNull String mfaSessionToken) {
        this(challengeId, mfaSessionToken, null);
    }

    MultiFactorSession(@NonNull String challengeId,
                       @NonNull String mfaSessionToken,
                       @Nullable MfaServiceHelper service) {
        this.challengeId = challengeId;
        this.mfaSessionToken = mfaSessionToken;
        this.service = service;
    }

    /**
     * Returns the identifier of this MFA challenge.
     *
     * @return the opaque challenge identifier
     */
    public @NonNull String getChallengeId() {
        return challengeId;
    }

    @NonNull String getMfaSessionToken() {
        return mfaSessionToken;
    }

    @Nullable MfaServiceHelper getService() {
        return service;
    }
}
