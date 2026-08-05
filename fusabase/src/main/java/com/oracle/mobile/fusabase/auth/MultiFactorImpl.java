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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Internal Fusabase implementation of the Firebase-shaped {@link MultiFactor} API. */
class MultiFactorImpl extends MultiFactor {
    private final FusabaseUser user;
    private final MfaServiceHelper service;
    private final List<MultiFactorInfo> enrolledFactors;

    MultiFactorImpl(@NonNull FusabaseUser user) {
        this.user = user;
        this.service = new MfaServiceHelper(user.auth, user);
        this.enrolledFactors = new ArrayList<>(user.getEnrolledFactorsInternal());
    }

    MultiFactorImpl(@NonNull MfaServiceHelper service, @NonNull List<MultiFactorInfo> enrolledFactors) {
        this.user = null;
        this.service = service;
        this.enrolledFactors = new ArrayList<>(enrolledFactors);
    }

    /** Retrieves the enrolled factors from the server and refreshes the local cache. */
    @Override
    public @NonNull Task<List<MultiFactorInfo>> getEnrolledFactors() {
        TaskCompletionSource<List<MultiFactorInfo>> taskCompletionSource = new TaskCompletionSource<>();
        Task<List<MultiFactorInfo>> task = taskCompletionSource.getTask();
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                List<MultiFactorInfo> factors = service.listFactors();
                enrolledFactors.clear();
                enrolledFactors.addAll(factors);
                if (user != null) {
                    user.setEnrolledFactorsInternal(factors);
                }
                taskCompletionSource.setResult(
                        Collections.unmodifiableList(new ArrayList<>(factors)));
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /** Creates an MFA session for enrolling a new factor. */
    @Override
    public @NonNull Task<MultiFactorSession> getSession() {
        TaskCompletionSource<MultiFactorSession> taskCompletionSource = new TaskCompletionSource<>();
        Task<MultiFactorSession> task = taskCompletionSource.getTask();
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                taskCompletionSource.setResult(service.createSession());
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /** Completes MFA factor enrollment. */
    @Override
    public @NonNull Task<Void> enroll(@NonNull MultiFactorAssertion assertion,
                                      @Nullable String displayName) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                MultiFactorInfo factor;
                if (assertion instanceof TotpMultiFactorAssertion) {
                    factor = service.verifyTotpEnrollment((TotpMultiFactorAssertion) assertion, resolveTotpDisplayName(displayName));
                } else if (assertion instanceof PhoneMultiFactorAssertion) {
                    factor = service.verifySmsEnrollment((PhoneMultiFactorAssertion) assertion, displayName);
                } else {
                    throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                            "Unsupported MFA assertion.");
                }
                addOrReplaceFactor(factor);
                if (user != null) user.setEnrolledFactorsInternal(enrolledFactors);
                taskCompletionSource.setResult(null);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /** Removes an enrolled factor by factor object or factor uid. */
    @Override
    public @NonNull Task<Void> unenroll(@NonNull MultiFactorInfo factor) {
        return unenroll((Object) factor);
    }

    /** Removes an enrolled factor by its unique factor ID. */
    @Override
    public @NonNull Task<Void> unenroll(@NonNull String factorUid) {
        return unenroll((Object) factorUid);
    }

    /**
     * Removes an enrolled factor by factor object or factor uid.
     * @deprecated Use the type-safe overload accepting MultiFactorInfo or String.
     */
    @Deprecated
    public @NonNull Task<Void> unenroll(@NonNull Object option) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                String uid;
                if (option instanceof MultiFactorInfo) {
                    uid = ((MultiFactorInfo) option).getUid();
                } else if (option instanceof String) {
                    uid = (String) option;
                } else {
                    throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                            "unenroll requires a MultiFactorInfo or factor uid.");
                }
                if (uid.trim().isEmpty()) {
                    throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                            "factor uid must not be empty.");
                }
                service.deleteFactor(uid);
                removeFactor(uid);
                if (user != null) user.setEnrolledFactorsInternal(enrolledFactors);
                taskCompletionSource.setResult(null);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    private @NonNull String resolveTotpDisplayName(@Nullable String displayName) {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        int totpCount = 0;
        for (MultiFactorInfo factor : enrolledFactors) {
            if (TotpMultiFactorGenerator.FACTOR_ID.equals(factor.getFactorId())) {
                totpCount++;
            }
        }
        return "Authenticator App " + (totpCount + 1);
    }

    private void addOrReplaceFactor(@NonNull MultiFactorInfo factor) {
        removeFactor(factor.getUid());
        enrolledFactors.add(factor);
    }

    private void removeFactor(@NonNull String uid) {
        for (int i = enrolledFactors.size() - 1; i >= 0; i--) {
            if (enrolledFactors.get(i).getUid().equals(uid)) {
                enrolledFactors.remove(i);
            }
        }
    }
}
