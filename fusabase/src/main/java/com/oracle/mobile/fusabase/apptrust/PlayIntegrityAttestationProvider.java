// Copyright (c) 2015, 2025, Oracle and/or its affiliates.
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
        IntegrityTokenRequest.Builder builder = IntegrityTokenRequest.builder().setNonce(nonce);
        if (cloudProjectNumber != null) {
            builder.setCloudProjectNumber(cloudProjectNumber);
        }

        FusabaseLogger.i(TAG, "Requesting Play Integrity attestation");
        com.google.android.gms.tasks.Task<IntegrityTokenResponse> integrityTask =
                integrityManager.requestIntegrityToken(builder.build());
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
}
