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

package com.oracle.mobile.fusabase.apptrust;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Internal attestation provider backed by Google Play Integrity. */
final class PlayIntegrityAttestationProvider {
    private static final String TAG = "PlayIntegrityProvider";

    private final IntegrityManager integrityManager;
    private final Long cloudProjectNumber;

    PlayIntegrityAttestationProvider(@NonNull Context context) {
        this.integrityManager = IntegrityManagerFactory.create(context.getApplicationContext());
        this.cloudProjectNumber = null;
    }

    PlayIntegrityAttestationProvider(@NonNull Context context, long cloudProjectNumber) {
        this.integrityManager = IntegrityManagerFactory.create(context.getApplicationContext());
        this.cloudProjectNumber = cloudProjectNumber;
    }

    @NonNull
    public Task<AppAttestation> attest(@NonNull String nonce) {
        TaskCompletionSource<AppAttestation> source = new TaskCompletionSource<>();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        source.handleFuture(completion);
        FusabaseLogger.i(TAG, "Requesting Play Integrity attestation");
        com.google.android.gms.tasks.Task<IntegrityTokenResponse> integrityTask =
                integrityManager.requestIntegrityToken(buildRequest(nonce, cloudProjectNumber));
        integrityTask
                .addOnSuccessListener(response -> {
                    FusabaseLogger.i(TAG, "Play Integrity attestation received");
                    source.setResult(new AppAttestation(
                            AppAttestation.Type.PLAY_INTEGRITY, response.token(), nonce));
                    completion.complete(null);
                })
                .addOnFailureListener(error -> {
                    FusabaseLogger.e(TAG, "Play Integrity attestation failed", error);
                    completion.completeExceptionally(new CompletionException(
                            new FusabaseAppTrustException("Play Integrity attestation failed", error)));
                });
        return source.getTask();
    }

    /** Builds the Google request with the backend nonce and optional explicit project number. */
    static IntegrityTokenRequest buildRequest(@NonNull String nonce, Long cloudProjectNumber) {
        IntegrityTokenRequest.Builder builder = IntegrityTokenRequest.builder().setNonce(nonce);
        if (cloudProjectNumber != null) {
            builder.setCloudProjectNumber(cloudProjectNumber);
        }
        return builder.build();
    }
}
