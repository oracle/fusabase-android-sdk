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

import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Creates TOTP MFA secrets and assertions. */
public final class TotpMultiFactorGenerator {
    /** Factor type identifier for authenticator-app MFA. */
    public static final String FACTOR_ID = "totp";

    private TotpMultiFactorGenerator() {}

    /**
     * Starts TOTP enrollment and returns the secret required to configure an authenticator app.
     *
     * @param session the MFA enrollment session
     * @return a task that completes with the generated TOTP secret
     */
    public static @NonNull Task<TotpSecret> generateSecret(@NonNull MultiFactorSession session) {
        return generateSecret(session, null);
    }

    /**
     * Starts TOTP enrollment with an optional display name for IDCS authenticator factors.
     *
     * <p>OnPrem applies a factor display name when enrollment is verified. IDCS requires it
     * when enrollment starts.
     *
     * @param session the MFA enrollment session
     * @param displayName the optional IDCS authenticator display name
     * @return a task that completes with the generated TOTP secret
     */
    public static @NonNull Task<TotpSecret> generateSecret(@NonNull MultiFactorSession session,
                                                            @Nullable String displayName) {
        TaskCompletionSource<TotpSecret> taskCompletionSource = new TaskCompletionSource<>();
        Task<TotpSecret> task = taskCompletionSource.getTask();
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                taskCompletionSource.setResult(
                        MfaServiceHelper.forSession(session).startTotpEnrollment(session, displayName));
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /** Creates an assertion that completes TOTP enrollment. */
    public static @NonNull TotpMultiFactorAssertion getAssertionForEnrollment(@NonNull TotpSecret secret,
                                                                           @NonNull String oneTimePassword) {
        validateCode(oneTimePassword);
        return new TotpMultiFactorAssertion(secret.getEnrollmentId(), oneTimePassword, secret.getSession(), true);
    }

    /** Creates an assertion that verifies an enrolled TOTP factor during sign-in. */
    public static @NonNull TotpMultiFactorAssertion getAssertionForSignIn(@NonNull String enrollmentId,
                                                                       @NonNull String oneTimePassword) {
        validateCode(oneTimePassword);
        if (enrollmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("enrollmentId must not be empty.");
        }
        return new TotpMultiFactorAssertion(enrollmentId, oneTimePassword, null, false);
    }

    /** Creates a sign-in assertion explicitly bound to an MFA session for internal resolution. */
    static @NonNull TotpMultiFactorAssertion getAssertionForSignIn(@NonNull String enrollmentId,
                                                                     @NonNull String oneTimePassword,
                                                                     @NonNull MultiFactorSession session) {
        validateCode(oneTimePassword);
        if (enrollmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("enrollmentId must not be empty.");
        }
        return new TotpMultiFactorAssertion(enrollmentId, oneTimePassword, session, false);
    }

    private static void validateCode(@NonNull String code) {
        if (code.trim().isEmpty()) {
            throw new IllegalArgumentException("TOTP code must not be empty.");
        }
    }
}
