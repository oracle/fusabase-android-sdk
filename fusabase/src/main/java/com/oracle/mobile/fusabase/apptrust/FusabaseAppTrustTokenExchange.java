// Copyright (c) 2015, 2025, Oracle and/or its affiliates.
package com.oracle.mobile.fusabase.apptrust;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;
import com.oracle.mobile.fusabase.utils.Utils;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/** Internal exchange of a Play Integrity assertion through the App Trust contract. */
final class FusabaseAppTrustTokenExchange {
    private static final String TAG = "FusabaseAppTrustExchange";
    private static final String DEFAULT_ACTION = "fusabase";
    private final FusabaseApp app;
    private final String action;

    FusabaseAppTrustTokenExchange(@NonNull FusabaseApp app) {
        this(app, DEFAULT_ACTION);
    }

    FusabaseAppTrustTokenExchange(@NonNull FusabaseApp app, @NonNull String action) {
        this.app = app;
        this.action = action;
    }

    @NonNull
    public Task<AppTrustToken> exchange(@NonNull AppAttestation attestation) {
        TaskCompletionSource<AppTrustToken> source = new TaskCompletionSource<>();
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                FusabaseOptions options = app.getOptions();
                String url = Utils.urlBuilder(options.getOrdsHost(), "_/baas-services/appcheck",
                        options.getProjectId(), "attest");
                url = Utils.addQueryParameterToURL(url, "apiKey", options.getAppId());
                JsonObject request = Json.createObjectBuilder()
                        .add("provider", "and|" + options.getAppId())
                        .add("attestationToken", attestation.getToken())
                        .add("action", action)
                        .add("challengeNonce", attestation.getNonce())
                        .build();
                FusabaseLogger.i(TAG, "Exchanging Play Integrity attestation for project="
                        + options.getProjectId());
                HttpResponse response = new HttpRequestHelper(1).createHttpRequest(url, "POST",
                                Collections.singletonMap("Content-Type", "application/json"), request.toString())
                        .executeRequest();
                if (!response.getStatus()) {
                    FusabaseLogger.i(TAG, "App Trust exchange response status=" + response.getCode());
                    throw new FusabaseAppTrustException("App Trust exchange failed: HTTP " + response.getCode());
                }
                try (JsonReader reader = Json.createReader(new java.io.StringReader(response.getResponse()))) {
                    JsonObject body = reader.readObject();
                    if (!"allow".equalsIgnoreCase(body.getString("decision", ""))) {
                        throw new FusabaseAppTrustException("App Trust decision was not allow");
                    }
                    String token = body.getString("attestToken", "");
                    if (token.isEmpty()) {
                        throw new FusabaseAppTrustException("App Trust response is missing attestToken");
                    }
                    long expiresAt = parseExpiry(body);
                    FusabaseLogger.i(TAG, "App Trust exchange response status=" + response.getCode()
                            + " decision=allow");
                    source.setResult(new AppTrustToken(token, expiresAt));
                    return "";
                }
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        });
        source.handleFuture(future);
        return source.getTask();
    }

    private static long parseExpiry(@NonNull JsonObject response) {
        if (response.containsKey("expiresAt")) {
            try {
                return Instant.parse(response.getString("expiresAt")).toEpochMilli();
            } catch (Exception ignored) {
                // The documented value is ISO-8601; retain a safe fallback for malformed responses.
            }
        }
        return System.currentTimeMillis() + 3_600_000L;
    }
}
