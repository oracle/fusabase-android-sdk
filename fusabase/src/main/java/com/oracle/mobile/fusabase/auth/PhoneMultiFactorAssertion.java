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

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Asserts ownership of a phone number second factor.
 *
 * <p>Obtain an instance with {@link PhoneMultiFactorGenerator#getAssertion(PhoneAuthCredential)}
 * after the user completes an SMS verification challenge.
 */
public class PhoneMultiFactorAssertion extends MultiFactorAssertion {
    private final PhoneAuthCredential credential;

    PhoneMultiFactorAssertion(@NonNull PhoneAuthCredential credential) {
        this.credential = credential;
    }

    /**
     * Returns the factor identifier for phone MFA.
     *
     * @return {@link PhoneMultiFactorGenerator#FACTOR_ID}
     */
    @Override
    public @NonNull String getFactorId() { return PhoneMultiFactorGenerator.FACTOR_ID; }

    @NonNull PhoneAuthCredential getCredential() { return credential; }

    @NonNull PhoneMultiFactorAssertion withSession(@NonNull MultiFactorSession session) {
        return new PhoneMultiFactorAssertion(credential.withSession(session));
    }

    @Override
    @NonNull JsonObject toJson() {
        MultiFactorSession session = credential.getSession();
        if (session == null) {
            throw new IllegalStateException("MFA session is required for phone multi-factor assertions.");
        }

        if (credential.isEnrollment()) {
            return Json.createObjectBuilder()
                    .add("verification_id", credential.getVerificationId())
                    .add("code", credential.getSmsCode())
                    .add("challengeId", session.getChallengeId())
                    .add("mfaSessionToken", session.getMfaSessionToken())
                    .build();
        }

        return Json.createObjectBuilder()
                .add("factor_id", credential.getVerificationId())
                .add("code", credential.getSmsCode())
                .add("challengeId", session.getChallengeId())
                .add("mfaSessionToken", session.getMfaSessionToken())
                .build();
    }
}
