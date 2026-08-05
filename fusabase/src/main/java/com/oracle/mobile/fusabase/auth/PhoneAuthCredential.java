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
 * Represents the result of an SMS verification challenge.
 *
 * <p>Obtain an instance with {@link PhoneAuthProvider#getCredential(String, String)} and use it
 * to create a {@link PhoneMultiFactorAssertion} for MFA enrollment or sign-in resolution.
 */
public class PhoneAuthCredential extends AuthCredential {
    private final String verificationId;
    private final String smsCode;
    private final MultiFactorSession session;
    private final boolean enrollment;

    PhoneAuthCredential(@NonNull String verificationId,
                        @NonNull String smsCode,
                        @Nullable MultiFactorSession session,
                        boolean enrollment) {
        this.verificationId = verificationId;
        this.smsCode = smsCode;
        this.session = session;
        this.enrollment = enrollment;
    }

    /**
     * Returns the provider identifier for phone authentication.
     *
     * @return the phone provider identifier
     */
    @Override
    public @NonNull String getProvider() { return PhoneAuthProvider.PROVIDER_ID; }

    /**
     * Returns the sign-in method identifier for phone authentication.
     *
     * @return the phone sign-in method identifier
     */
    @Override
    public @NonNull String getSignInMethod() { return PhoneAuthProvider.PHONE_SIGN_IN_METHOD; }

    @Override
    protected @NonNull String getIdToken() { return verificationId; }

    /**
     * Returns the verification identifier associated with the SMS challenge.
     *
     * @return the SMS challenge verification identifier
     */
    public @NonNull String getVerificationId() { return verificationId; }

    /**
     * Returns the SMS one-time passcode supplied for verification.
     *
     * @return the SMS one-time passcode
     */
    public @NonNull String getSmsCode() { return smsCode; }
    @Nullable MultiFactorSession getSession() { return session; }
    boolean isEnrollment() { return enrollment; }

    @NonNull PhoneAuthCredential withSession(@NonNull MultiFactorSession session) {
        return new PhoneAuthCredential(verificationId, smsCode, session, enrollment);
    }
}
