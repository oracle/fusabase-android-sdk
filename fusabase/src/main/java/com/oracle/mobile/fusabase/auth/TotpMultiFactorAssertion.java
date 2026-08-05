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

import jakarta.json.Json;
import jakarta.json.JsonObject;

/** An assertion that proves possession of a TOTP MFA factor. */
public class TotpMultiFactorAssertion extends MultiFactorAssertion {
    private final String factorUid;
    private final String code;
    private final MultiFactorSession session;
    private final boolean enrollment;

    TotpMultiFactorAssertion(@NonNull String factorUid,
                             @NonNull String code,
                             @Nullable MultiFactorSession session,
                             boolean enrollment) {
        this.factorUid = factorUid;
        this.code = code;
        this.session = session;
        this.enrollment = enrollment;
    }

    @Override
    /** Returns the TOTP factor type. */
    public @NonNull String getFactorId() {
        return TotpMultiFactorGenerator.FACTOR_ID;
    }

    @NonNull String getFactorUid() { return factorUid; }
    @NonNull String getCode() { return code; }
    @Nullable MultiFactorSession getSession() { return session; }

    @NonNull TotpMultiFactorAssertion withSession(@NonNull MultiFactorSession session) {
        return new TotpMultiFactorAssertion(factorUid, code, session, enrollment);
    }
    boolean isEnrollment() { return enrollment; }

    @Override
    @NonNull JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("factor_id", factorUid)
                .add("code", code)
                .add("challengeId", session.getChallengeId())
                .add("mfaSessionToken", session.getMfaSessionToken())
                .build();
    }
}
