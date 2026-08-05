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

import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Sends SMS OTPs and creates phone credentials for MFA. */
public final class PhoneAuthProvider {
    /** Provider identifier for phone authentication. */
    public static final String PROVIDER_ID = "phone";
    /** Sign-in method identifier for phone authentication. */
    public static final String PHONE_SIGN_IN_METHOD = "phone";
    private static final Map<String, CredentialContext> credentialContexts =
            Collections.synchronizedMap(new HashMap<>());

    private PhoneAuthProvider() { }

    /**
     * Receives SMS verification state changes.
     *
     * <p>ORDS invokes {@link #onCodeSent(String, ForceResendingToken)} after it starts an SMS
     * challenge, or {@link #onVerificationFailed(Exception)} when it cannot start one.
     */
    public abstract static class OnVerificationStateChangedCallbacks {
        /**
         * Called when a phone credential is automatically verified.
         *
         * <p>ORDS does not support automatic SMS retrieval, so this callback is not invoked.
         *
         * @param credential the automatically verified credential
         */
        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) { }

        /**
         * Called when the SMS challenge cannot be started.
         *
         * @param exception the failure returned while starting the challenge
         */
        public abstract void onVerificationFailed(@NonNull Exception exception);

        /**
         * Called after ORDS issues the SMS challenge.
         *
         * @param verificationId the identifier used to create a phone credential
         * @param token an opaque compatibility token
         */
        public abstract void onCodeSent(@NonNull String verificationId,
                                        @NonNull ForceResendingToken token);
    }

    /**
     * Opaque compatibility token supplied with {@link OnVerificationStateChangedCallbacks#onCodeSent}.
     *
     * <p>ORDS does not currently expose Firebase-style SMS resend-token semantics.
     */
    public static final class ForceResendingToken {
        static final ForceResendingToken INSTANCE = new ForceResendingToken();
        private ForceResendingToken() { }
    }

    /**
     * Starts an SMS enrollment or sign-in challenge.
     *
     * <p>For enrollment, the task result is the ORDS factor ID used to create a phone credential.
     * For sign-in, it is the enrolled factor ID selected for the challenge.
     *
     * @param options the SMS enrollment or sign-in options
     * @return a task that completes with the factor identifier
     */
    public static @NonNull Task<String> verifyPhoneNumber(@NonNull PhoneAuthOptions options) {
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
        Task<String> task = taskCompletionSource.getTask();
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                MfaServiceHelper service = resolveService(options.getSession(), options.getAuth());
                PhoneMultiFactorInfo hint = options.getMultiFactorHint();
                if (hint != null) {
                    if (!PhoneMultiFactorGenerator.FACTOR_ID.equals(hint.getFactorId())) {
                        throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                                "multiFactorHint must be a phone MFA factor.");
                    }
                    taskCompletionSource.setResult(service.startSmsSignIn(options.getSession(), hint.getUid()));
                } else {
                    String phoneNumber = options.getPhoneNumber();
                    if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                        throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                                "phoneNumber is required for SMS enrollment.");
                    }
                    MfaServiceHelper.SmsEnrollment enrollment = service.startSmsEnrollment(
                            options.getSession(), phoneNumber, options.getCountryCode());
                    credentialContexts.put(enrollment.verificationId,
                            new CredentialContext(enrollment.session, true));
                    taskCompletionSource.setResult(enrollment.verificationId);
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return "";
        });
        OnVerificationStateChangedCallbacks callbacks = options.getCallbacks();
        if (callbacks != null) {
            task.addOnSuccessListener(verificationId ->
                    callbacks.onCodeSent(verificationId, ForceResendingToken.INSTANCE));
            task.addOnFailureListener(callbacks::onVerificationFailed);
        }
        taskCompletionSource.handleFuture(future);
        return task;
    }

    private static @NonNull MfaServiceHelper resolveService(@NonNull MultiFactorSession session,
                                                            @NonNull FusabaseAuth auth) throws FusabaseAuthException {
        if (session.getService() != null) {
            return session.getService();
        }
        if (auth == null) {
            throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "Auth instance is required for phone MFA.");
        }
        return MfaServiceHelper.forSignInChallenge(auth);
    }

    /** Creates a credential using the context retained for the SMS challenge. */
    public static @NonNull PhoneAuthCredential getCredential(@NonNull String verificationId,
                                                              @NonNull String code) {
        CredentialContext context = credentialContexts.remove(verificationId);
        if (context != null) {
            return new PhoneAuthCredential(verificationId, code, context.session, context.enrollment);
        }
        return new PhoneAuthCredential(verificationId, code, null, false);
    }

    /** Creates an SMS enrollment credential associated with an MFA session. */
    public static @NonNull PhoneAuthCredential getCredential(@NonNull String verificationId,
                                                              @NonNull String code,
                                                              @NonNull MultiFactorSession session) {
        return new PhoneAuthCredential(verificationId, code, session, true);
    }

    /** Creates an SMS credential with an explicit enrollment or sign-in context. */
    public static @NonNull PhoneAuthCredential getCredential(@NonNull String verificationId,
                                                              @NonNull String code,
                                                              @NonNull MultiFactorSession session,
                                                              boolean enrollment) {
        return new PhoneAuthCredential(verificationId, code, session, enrollment);
    }

    private static final class CredentialContext {
        private final MultiFactorSession session;
        private final boolean enrollment;

        private CredentialContext(@NonNull MultiFactorSession session, boolean enrollment) {
            this.session = session;
            this.enrollment = enrollment;
        }
    }
}
