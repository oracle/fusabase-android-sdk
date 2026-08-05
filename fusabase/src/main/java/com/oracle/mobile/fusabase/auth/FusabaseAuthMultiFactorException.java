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

import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.util.ArrayList;
import java.util.List;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * Thrown when sign-in requires verification with an enrolled multi-factor authentication factor.
 *
 * <p>Use {@link #getResolver()} to obtain the resolver that completes the sign-in.
 */
public class FusabaseAuthMultiFactorException extends FusabaseAuthException {
    private static final String TAG = "FusabaseMultiFactorException";
    private final FusabaseAuth auth;
    private final MultiFactorSession session;
    private final List<MultiFactorInfo> hints;

    FusabaseAuthMultiFactorException(@NonNull JsonObject response) {
        this(response, null);
    }

    FusabaseAuthMultiFactorException(@NonNull JsonObject response, @Nullable FusabaseAuth auth) {
        super(Code.UNAUTHENTICATED.toString(), "Multi-factor authentication is required.");
        this.auth = auth;
        String challengeId = response.getString("challengeId", response.getString("challenge_id", ""));
        String sessionToken = response.getString("mfaSessionToken", response.getString("mfa_session_token", ""));
        this.session = new MultiFactorSession(challengeId, sessionToken);
        this.hints = parseHints(response);
    }

    /**
     * Returns the resolver that completes this pending multi-factor sign-in.
     *
     * <p>Use the resolver to inspect available factor hints and submit a verified
     * {@link MultiFactorAssertion} through {@link MultiFactorResolver#resolveSignIn(MultiFactorAssertion)}.
     *
     * @return the resolver associated with this sign-in challenge
     * @throws IllegalStateException if this exception was not created by a Fusabase authentication flow
     */
    public @NonNull MultiFactorResolver getResolver() {
        if (auth == null) {
            throw new IllegalStateException("The MFA exception is not associated with FusabaseAuth.");
        }
        return new MultiFactorResolver(auth, session, hints) { };
    }

    /**
     * Returns the session associated with the pending multi-factor sign-in.
     *
     * @return the pending multi-factor session
     */
    public @NonNull MultiFactorSession getSession() { return session; }

    /**
     * Returns the enrolled factors available to verify the pending sign-in.
     *
     * @return a copy of the available factor hints
     */
    public @NonNull List<MultiFactorInfo> getHints() { return new ArrayList<>(hints); }

    private static @NonNull List<MultiFactorInfo> parseHints(@NonNull JsonObject response) {
        List<MultiFactorInfo> result = new ArrayList<>();
        JsonArray factors;
        if (response.containsKey("factors")
                && response.get("factors").getValueType() == JsonValue.ValueType.ARRAY) {
            factors = response.getJsonArray("factors");
        } else {
            return result;
        }
        for (JsonValue value : factors) {
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                try {
                    result.add(MultiFactorInfoFactory.fromJson(value.asJsonObject()));
                } catch (FusabaseAuthException e) {
                    // Keep valid hints available. Do not log the malformed server payload.
                    FusabaseLogger.w(TAG, "Ignoring malformed MFA factor hint.");
                }
            }
        }
        return result;
    }
}
