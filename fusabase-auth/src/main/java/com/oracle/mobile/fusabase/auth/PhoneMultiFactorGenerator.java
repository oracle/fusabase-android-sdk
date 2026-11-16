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

/**
 *  Creates phone/SMS MFA assertions.
 *
 * <p>Example:</p>
 * <pre>{@code
 * PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, smsCode);
 * PhoneMultiFactorAssertion assertion = PhoneMultiFactorGenerator.getAssertion(credential);
 * resolver.resolveSignIn(assertion);
 * }</pre>
 */
public class PhoneMultiFactorGenerator {
    /**
     *  Factor type identifier for SMS MFA.
     *
     * <p>Example:</p>
     * <pre>{@code
     * String identifier = PhoneMultiFactorGenerator.FACTOR_ID;
     * }</pre>
     */
    public static final String FACTOR_ID = "phone";

    private PhoneMultiFactorGenerator() {}

    /**
     *  Creates an MFA assertion from a verified SMS credential.
     *
     * <p>Example: Given a pending phone MFA sign-in resolver and a verification ID from verifyPhoneNumber.</p>
     * <pre>{@code
     * PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, smsCode);
     * PhoneMultiFactorAssertion assertion = PhoneMultiFactorGenerator.getAssertion(credential);
     * resolver.resolveSignIn(assertion);
     * }</pre>
     */
    public static @NonNull PhoneMultiFactorAssertion getAssertion(@NonNull PhoneAuthCredential credential) {
        return new PhoneMultiFactorAssertion(credential);
    }
}
