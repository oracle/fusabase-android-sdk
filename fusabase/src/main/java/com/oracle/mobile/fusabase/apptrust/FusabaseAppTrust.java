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

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;
import com.oracle.mobile.fusabase.utils.Utils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * Coordinates App Trust token acquisition and caching for a {@link FusabaseApp}.
 *
 * <p>Install an {@link AppTrustProviderFactory} before calling {@link #getAppTrustToken(boolean)}.
 * When no valid cached token is available, this class obtains a backend-issued nonce, requests an
 * attestation assertion, and exchanges it for a backend App Trust token. Cached tokens are
 * persisted with the SDK's Android KeyStore-backed AES-GCM preference encryption.</p>
 *
 * <p>Instances are scoped to the supplied named {@link FusabaseApp}.</p>
 */
public final class FusabaseAppTrust {
    private static final String TAG = "FusabaseAppTrust";
    // Utils encrypts this preference file with its own Android KeyStore AES-GCM key.
    private static final String PREFS_NAME = "Fusabase_APPTRUST";
    private static final String TOKEN_FIELD = "token";
    private static final String EXPIRE_TIME_MILLIS_FIELD = "expireTimeMillis";
    private static final String APP_TRUST_SERVICE_PATH = "_/baas-services/appcheck";
    private static final String NONCE_ENDPOINT = "nonce";
    private static final String NONCE_RESPONSE_FIELD = "nonce";
    private static final String API_KEY_QUERY_PARAMETER = "apiKey";
    private static final String HTTP_METHOD_POST = "POST";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String EMPTY_JSON_OBJECT = "{}";
    private static final long EXPIRY_SKEW_MILLIS = 60_000L;
    private static final ScheduledExecutorService AUTO_REFRESH_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, FusabaseAppTrust> INSTANCES = new ConcurrentHashMap<>();

    private final FusabaseApp app;
    private final CopyOnWriteArraySet<AppTrustListener> listeners = new CopyOnWriteArraySet<>();
    private volatile AppTrustProvider appTrustProvider;
    private volatile AppTrustToken cachedToken;
    private volatile boolean tokenAutoRefreshEnabled;
    private volatile ScheduledFuture<?> autoRefreshFuture;

    private FusabaseAppTrust(@NonNull FusabaseApp app) {
        this.app = app;
        this.cachedToken = loadTokenFromPrefs();
    }

    /**
     * Listener notified when this instance obtains a new App Trust token.
     *
     * <p>Listeners are invoked on the thread that completes token acquisition. Listener failures
     * are isolated so that other listeners and the token request can complete normally.</p>
     */
    public interface AppTrustListener {
        /**
         * Called when a new App Trust token becomes current for this instance.
         *
         * @param token the newly acquired App Trust token
         */
        void onAppTrustTokenChanged(@NonNull AppTrustToken token);
    }

    /**
     * Returns the App Trust coordinator for the default Fusabase app.
     *
     * @return the coordinator for {@link FusabaseApp#getInstance()}
     */
    @NonNull
    public static FusabaseAppTrust getInstance() {
        return getInstance(FusabaseApp.getInstance());
    }

    /**
     * Returns the App Trust coordinator for the supplied app.
     *
     * @param app the initialized Fusabase app whose App Trust configuration is used
     * @return the coordinator associated with {@code app}'s name
     */
    @NonNull
    public static FusabaseAppTrust getInstance(@NonNull FusabaseApp app) {
        return INSTANCES.computeIfAbsent(app.getName(), ignored -> new FusabaseAppTrust(app));
    }

    /**
     * Installs a factory that provides App Trust tokens for this app.
     *
     * <p>This overload leaves automatic token refresh disabled. Use the overload with
     * {@code isTokenAutoRefreshEnabled} or {@link #setTokenAutoRefreshEnabled(boolean)} to enable
     * it.</p>
     *
     * @param factory the factory used to create the App Trust provider
     */
    public void installAppTrustProviderFactory(@NonNull AppTrustProviderFactory factory) {
        installAppTrustProviderFactory(factory, false);
    }

    /**
     * Installs a factory that provides App Trust tokens for this app and configures auto-refresh.
     *
     * @param factory the factory used to create the App Trust provider
     * @param isTokenAutoRefreshEnabled whether valid tokens should be refreshed before expiry
     */
    public void installAppTrustProviderFactory(@NonNull AppTrustProviderFactory factory,
                                               boolean isTokenAutoRefreshEnabled) {
        cancelAutoRefresh();
        this.appTrustProvider = factory.create(app);
        this.cachedToken = null;
        setTokenAutoRefreshEnabled(isTokenAutoRefreshEnabled);
    }

    /**
     * Registers a listener for changes to this instance's current App Trust token.
     *
     * @param listener the listener to register
     */
    public void addAppTrustListener(@NonNull AppTrustListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered App Trust token listener.
     *
     * @param listener the listener to remove
     */
    public void removeAppTrustListener(@NonNull AppTrustListener listener) {
        listeners.remove(listener);
    }

    /**
     * Enables or disables refreshing valid App Trust tokens before they expire.
     *
     * @param isTokenAutoRefreshEnabled {@code true} to refresh valid tokens before expiry
     */
    public void setTokenAutoRefreshEnabled(boolean isTokenAutoRefreshEnabled) {
        tokenAutoRefreshEnabled = isTokenAutoRefreshEnabled;
        cancelAutoRefresh();
        if (isTokenAutoRefreshEnabled) {
            AppTrustToken token = getCachedTokenIfValid();
            if (token != null) {
                scheduleAutoRefresh(token);
            } else {
                getAppTrustToken(false);
            }
        }
    }

    /**
     * Returns a valid cached token or obtains a fresh token through the installed implementations.
     *
     * @param forceRefresh {@code true} to bypass a valid cached token and obtain a new one;
     *                     {@code false} to return a valid cached token when available
     * @return a task that completes with an App Trust token or fails when App Trust is not
     *         installed, nonce issuance, attestation, or token exchange fails
     */
    @NonNull
    public Task<AppTrustToken> getToken(boolean forceRefresh) {
        return getAppTrustToken(forceRefresh);
    }

    /**
     * Returns a valid cached token or obtains a fresh token through the installed provider.
     *
     * @param forceRefresh {@code true} to bypass a valid cached token and obtain a new one;
     *                     {@code false} to return a valid cached token when available
     * @return a task that completes with an App Trust token or fails when token acquisition fails
     */
    @NonNull
    public Task<AppTrustToken> getAppTrustToken(boolean forceRefresh) {
        TaskCompletionSource<AppTrustToken> source = new TaskCompletionSource<>();
        AppTrustProvider tokenProvider = appTrustProvider;
        AppTrustToken current = getCachedTokenIfValid();
        if (!forceRefresh && current != null) {
            source.setResult(current);
            return source.getTask();
        }
        if (tokenProvider != null) {
            tokenProvider.getToken()
                    .addOnSuccessListener(token -> completeTokenRequest(source, token))
                    .addOnFailureListener(error ->
                            completeFailure(source, "App Trust token acquisition failed", error));
            return source.getTask();
        }

        source.setException(new FusabaseAppTrustException(
                "FusabaseAppTrust is not installed. Call installAppTrustProviderFactory(...) "
                        + "before requesting a token."));
        return source.getTask();
    }

    @NonNull
    Task<AppTrustToken> requestTokenFromPlayIntegrity(
            @NonNull PlayIntegrityAttestationProvider attestationProvider,
            @NonNull FusabaseAppTrustTokenExchange tokenExchange) {
        TaskCompletionSource<AppTrustToken> source = new TaskCompletionSource<>();
        fetchNonceFromBackend().addOnSuccessListener(nonce -> {
            attestationProvider.attest(nonce)
                    .addOnSuccessListener(attestation -> tokenExchange.exchange(attestation)
                            .addOnSuccessListener(source::setResult)
                            .addOnFailureListener(error ->
                                    completeFailure(source, "App Trust token exchange failed", error)))
                    .addOnFailureListener(error ->
                            completeFailure(source, "Play Integrity attestation failed", error));
        }).addOnFailureListener(error ->
                completeFailure(source, "App Trust nonce request failed", error));
        return source.getTask();
    }

    /**
     * Requests a limited-use App Trust token for a backend endpoint with replay protection.
     *
     * @return a task that completes with a limited-use App Trust token or fails when no provider
     *         that supports limited-use tokens is installed
     */
    @NonNull
    public Task<AppTrustToken> getLimitedUseAppTrustToken() {
        AppTrustProvider provider = appTrustProvider;
        if (provider != null) {
            return provider.getLimitedUseToken();
        }
        TaskCompletionSource<AppTrustToken> source = new TaskCompletionSource<>();
        source.setException(new FusabaseAppTrustException(
                "Limited-use App Trust tokens require an AppTrustProviderFactory."));
        return source.getTask();
    }

    /**
     * Returns the cached token only when it remains valid beyond the expiry safety window.
     *
     * @return the valid cached token, or {@code null} when no token is cached or it is close to
     *         expiry
     */
    public AppTrustToken getCachedTokenIfValid() {
        AppTrustToken token = cachedToken;
        return token != null && token.getExpireTimeMillis() - EXPIRY_SKEW_MILLIS > System.currentTimeMillis()
                ? token : null;
    }

    private void completeTokenRequest(@NonNull TaskCompletionSource<AppTrustToken> source,
                                      @NonNull AppTrustToken token) {
        cachedToken = token;
        persistToken(token);
        scheduleAutoRefresh(token);
        notifyListeners(token);
        source.setResult(token);
    }

    private void scheduleAutoRefresh(@NonNull AppTrustToken token) {
        if (!tokenAutoRefreshEnabled) {
            return;
        }
        cancelAutoRefresh();
        long delayMillis = Math.max(0L, token.getExpireTimeMillis() - System.currentTimeMillis()
                - EXPIRY_SKEW_MILLIS);
        autoRefreshFuture = AUTO_REFRESH_EXECUTOR.schedule(() -> getAppTrustToken(true),
                delayMillis, TimeUnit.MILLISECONDS);
    }

    private void cancelAutoRefresh() {
        ScheduledFuture<?> future = autoRefreshFuture;
        if (future != null) {
            future.cancel(false);
            autoRefreshFuture = null;
        }
    }

    private void notifyListeners(@NonNull AppTrustToken token) {
        for (AppTrustListener listener : listeners) {
            try {
                listener.onAppTrustTokenChanged(token);
            } catch (RuntimeException ignored) {
                FusabaseLogger.w(TAG, "App Trust token listener failed");
            }
        }
    }

    /** Requests a nonce from the App Trust service for the configured project. */
    @NonNull
    private Task<String> fetchNonceFromBackend() {
        TaskCompletionSource<String> source = new TaskCompletionSource<>();
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                FusabaseOptions options = app.getOptions();
                String url = Utils.urlBuilder(options.getOrdsHost(), APP_TRUST_SERVICE_PATH,
                        options.getProjectId(), NONCE_ENDPOINT);
                url = Utils.addQueryParameterToURL(url, API_KEY_QUERY_PARAMETER, options.getAppId());
                FusabaseLogger.i(TAG, "Requesting App Trust nonce for project=" + options.getProjectId());
                HttpResponse response = new HttpRequestHelper(1).createHttpRequest(url, HTTP_METHOD_POST,
                                Collections.singletonMap(CONTENT_TYPE_HEADER, APPLICATION_JSON), EMPTY_JSON_OBJECT)
                        .executeRequest();
                if (!response.getStatus()) {
                    FusabaseLogger.i(TAG, "App Trust nonce response status=" + response.getCode());
                    throw new FusabaseAppTrustException("Failed to fetch App Trust nonce: HTTP "
                            + response.getCode());
                }
                try (JsonReader reader = Json.createReader(new java.io.StringReader(response.getResponse()))) {
                    JsonObject body = reader.readObject();
                    String nonce = body.getString(NONCE_RESPONSE_FIELD, "");
                    if (nonce.isEmpty()) {
                        throw new FusabaseAppTrustException("App Trust nonce response is missing nonce");
                    }
                    FusabaseLogger.i(TAG, "App Trust nonce response status=" + response.getCode());
                    source.setResult(nonce);
                    return "";
                }
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        });
        source.handleFuture(future);
        return source.getTask();
    }

    /** Persists a successfully issued token using the App Trust preference namespace. */
    private void persistToken(@NonNull AppTrustToken token) {
        try {
            JsonObject data = Json.createObjectBuilder().add(TOKEN_FIELD, token.getToken())
                    .add(EXPIRE_TIME_MILLIS_FIELD, token.getExpireTimeMillis()).build();
            Utils.savePreferenceData(app.getApplicationContext(), data.toString(), TOKEN_FIELD, PREFS_NAME);
        } catch (GeneralSecurityException | IOException error) {
            FusabaseLogger.e(TAG, "Unable to persist App Trust token", error);
        }
    }

    /** Loads and validates the previously persisted App Trust token, if any. */
    private AppTrustToken loadTokenFromPrefs() {
        try {
            String data = Utils.getPreferenceData(app.getApplicationContext(), TOKEN_FIELD, PREFS_NAME);
            if (data == null || data.trim().isEmpty()) {
                return null;
            }
            try (JsonReader reader = Json.createReader(new java.io.StringReader(data))) {
                JsonObject token = reader.readObject();
                String value = token.getString(TOKEN_FIELD, "");
                if (value.isEmpty() || !token.containsKey(EXPIRE_TIME_MILLIS_FIELD)) {
                    return null;
                }
                return new AppTrustToken(value,
                        token.getJsonNumber(EXPIRE_TIME_MILLIS_FIELD).longValue());
            }
        } catch (Exception error) {
            FusabaseLogger.w(TAG, "Unable to load cached App Trust token");
            return null;
        }
    }

    private static void completeFailure(@NonNull TaskCompletionSource<?> source, @NonNull String message,
                                        @NonNull Exception error) {
        FusabaseLogger.e(TAG, message, error);
        source.setException(error);
    }
}
