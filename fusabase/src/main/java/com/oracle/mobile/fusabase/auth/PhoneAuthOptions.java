// Copyright (c) 2015, 2026, Oracle and/or its affiliates.

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
 * Configures an SMS verification flow.
 *
 * <p>Use this class to start SMS MFA enrollment or resolve an SMS MFA sign-in challenge with
 * {@link PhoneAuthProvider#verifyPhoneNumber(PhoneAuthOptions)}.
 */
public final class PhoneAuthOptions {
    private final FusabaseAuth auth;
    private final String phoneNumber;
    private final String countryCode;
    private final MultiFactorSession session;
    private final PhoneMultiFactorInfo hint;
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;

    private PhoneAuthOptions(@NonNull FusabaseAuth auth, @Nullable String phoneNumber,
                             @Nullable String countryCode, @NonNull MultiFactorSession session,
                             @Nullable PhoneMultiFactorInfo hint,
                             @Nullable PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
        this.auth = auth;
        this.phoneNumber = phoneNumber;
        this.countryCode = countryCode;
        this.session = session;
        this.hint = hint;
        this.callbacks = callbacks;
    }

    /**
     * Returns a builder associated with the default authentication instance.
     *
     * @return a builder for SMS verification options
     */
    public static @NonNull Builder newBuilder() {
        return new Builder(FusabaseAuth.getInstance());
    }

    /**
     * Returns a builder associated with the specified authentication instance.
     *
     * @param auth the authentication instance for the SMS verification flow
     * @return a builder for SMS verification options
     */
    public static @NonNull Builder newBuilder(@NonNull FusabaseAuth auth) {
        return new Builder(auth);
    }

    @Nullable String getPhoneNumber() { return phoneNumber; }
    @Nullable String getCountryCode() { return countryCode; }
    @NonNull MultiFactorSession getSession() { return session; }
    @Nullable PhoneMultiFactorInfo getMultiFactorHint() { return hint; }
    @NonNull FusabaseAuth getAuth() { return auth; }
    @Nullable PhoneAuthProvider.OnVerificationStateChangedCallbacks getCallbacks() { return callbacks; }

    /**
     * Builds options for SMS MFA enrollment or sign-in.
     *
     * <p>For enrollment, set a phone number and MFA session. For sign-in, set a phone factor hint
     * and MFA session.
     */
    public static final class Builder {
        private final FusabaseAuth auth;
        private String phoneNumber;
        private String countryCode;
        private MultiFactorSession session;
        private PhoneMultiFactorInfo hint;
        private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;

        private Builder(@NonNull FusabaseAuth auth) { this.auth = auth; }

        /**
         * Sets the phone number to enroll as an SMS MFA factor.
         *
         * @param value the phone number to enroll
         * @return this builder
         */
        public @NonNull Builder setPhoneNumber(@NonNull String value) { phoneNumber = value; return this; }

        /**
         * Sets the country calling code for SMS MFA enrollment.
         *
         * @param value the country calling code, for example {@code +91}
         * @return this builder
         */
        public @NonNull Builder setCountryCode(@NonNull String value) { countryCode = value; return this; }

        /**
         * Sets the MFA session for enrollment or sign-in.
         *
         * @param value the MFA session associated with the operation
         * @return this builder
         */
        public @NonNull Builder setMultiFactorSession(@NonNull MultiFactorSession value) { session = value; return this; }

        /**
         * Selects the enrolled phone factor used to resolve an MFA sign-in challenge.
         *
         * @param value the phone factor selected by the user
         * @return this builder
         */
        public @NonNull Builder setMultiFactorHint(@NonNull PhoneMultiFactorInfo value) { hint = value; return this; }

        /**
         * Sets callbacks for SMS challenge issuance and failure.
         *
         * @param value the callbacks to receive challenge state changes
         * @return this builder
         */
        public @NonNull Builder setCallbacks(
                @NonNull PhoneAuthProvider.OnVerificationStateChangedCallbacks value) {
            callbacks = value;
            return this;
        }

        /**
         * Creates the configured SMS verification options.
         *
         * @return the configured SMS verification options
         * @throws IllegalArgumentException if the MFA session is missing, or an enrollment flow
         *         does not include a phone number
         */
        public @NonNull PhoneAuthOptions build() {
            if (session == null) throw new IllegalArgumentException("multiFactorSession is required.");
            if (hint == null && (phoneNumber == null || phoneNumber.trim().isEmpty())) {
                throw new IllegalArgumentException("phoneNumber is required for enrollment.");
            }
            return new PhoneAuthOptions(auth, phoneNumber, countryCode, session, hint, callbacks);
        }
    }
}
