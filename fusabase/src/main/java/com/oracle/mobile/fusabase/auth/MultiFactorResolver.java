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

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Provides the information and operations required to complete a pending multi-factor sign-in.
 *
 * <p>Obtain a resolver from {@link FusabaseAuthMultiFactorException#getResolver()} after the
 * primary sign-in flow reports a {@link FusabaseAuthMultiFactorException}. Use
 * {@link #getHints()} to select a factor and {@link #resolveSignIn(MultiFactorAssertion)} to
 * submit the verified second-factor assertion.
 */
public abstract class MultiFactorResolver {
    private final FusabaseAuth auth;
    private final MultiFactorSession session;
    private final List<MultiFactorInfo> hints;
    private final MfaServiceHelper service;

    MultiFactorResolver(@NonNull FusabaseAuth auth,
                        @NonNull MultiFactorSession session,
                        @NonNull List<MultiFactorInfo> hints) {
        this.auth = auth;
        this.session = session;
        this.hints = new ArrayList<>(hints);
        if (session.getService() != null) {
            this.service = session.getService();
            return;
        }
        this.service = MfaServiceHelper.forSignInChallenge(auth);
    }

    /**
     * Returns the session associated with the pending multi-factor sign-in.
     *
     * @return the MFA session for this sign-in challenge
     */
    public @NonNull MultiFactorSession getSession() { return session; }

    /**
     * Returns the Fusabase authentication instance that owns this sign-in challenge.
     *
     * @return the Fusabase authentication instance
     */
    public @NonNull FusabaseAuth getFusabaseAuth() { return auth; }

    /**
     * Returns the enrolled factors that can verify the pending sign-in.
     *
     * @return the available second-factor hints
     */
    public @NonNull List<MultiFactorInfo> getHints() {
        return Collections.unmodifiableList(hints);
    }

    /**
     * Returns a task that verifies a second-factor assertion and completes the pending sign-in.
     *
     * @param assertion the verified assertion for the selected MFA factor
     * @return task that completes with the authenticated user result
     */
    public @NonNull Task<AuthResult> resolveSignIn(@NonNull MultiFactorAssertion assertion) {
        TaskCompletionSource<AuthResult> taskCompletionSource = new TaskCompletionSource<>();
        Task<AuthResult> task = taskCompletionSource.getTask();
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                MultiFactorAssertion assertionToVerify = assertion;
                if (assertion instanceof TotpMultiFactorAssertion) {
                    TotpMultiFactorAssertion totpAssertion = (TotpMultiFactorAssertion) assertion;
                    if (!totpAssertion.isEnrollment() && totpAssertion.getSession() == null) {
                        assertionToVerify = totpAssertion.withSession(session);
                    }
                } else if (assertion instanceof PhoneMultiFactorAssertion) {
                    PhoneMultiFactorAssertion phoneAssertion = (PhoneMultiFactorAssertion) assertion;
                    if (!phoneAssertion.getCredential().isEnrollment()
                            && phoneAssertion.getCredential().getSession() == null) {
                        assertionToVerify = phoneAssertion.withSession(session);
                    }
                }
                taskCompletionSource.setResult(completeMfaSignIn(service.verifyMfaChallenge(assertionToVerify)));
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    @NonNull
    private AuthResult completeMfaSignIn(@NonNull JsonObject tokenResponse) throws FusabaseException {
        if (auth == null) {
            throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "Auth instance is required to complete MFA sign-in.");
        }
        JsonObjectBuilder tokenBuilder = Json.createObjectBuilder()
                .add("access_token", tokenResponse.getString("access_token"));
        if (tokenResponse.containsKey("refresh_token") && !tokenResponse.isNull("refresh_token")) {
            tokenBuilder.add("refresh_token", tokenResponse.getString("refresh_token", ""));
        }
        JsonObject userData = auth.authHelper.getAuthenticationDetails("", "", tokenBuilder.build());
        AuthResult result = new AuthResult() {
            @Nullable
            @Override
            public AuthCredential getCredential() {
                return null;
            }

            @Nullable
            @Override
            public FusabaseUser getUser() {
                return new FusabaseUserImpl(userData, auth, "");
            }
        };
        auth.updateCurrentUser(result.getUser());
        auth.authHelper.saveUserDetails(auth.getApp().getApplicationContext(), userData,
                FusabaseAuth.PREFERENCES_NAME);
        auth.informAuthSubscribers();
        auth.informIdTokenSubscribers();
        return result;
    }
}
