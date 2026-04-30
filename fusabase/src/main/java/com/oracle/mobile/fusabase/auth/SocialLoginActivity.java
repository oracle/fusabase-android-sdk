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

import static com.oracle.mobile.fusabase.auth.FusabaseAuth.PREFERENCES_NAME;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.utils.Utils;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.util.concurrent.CompletableFuture;

/**
 * Android Activity for handling social login authentication flows.
 *
 * This activity manages the OAuth/OIDC authentication flow by launching custom tabs
 * for user authentication with external identity providers (Google, Facebook, etc.).
 * It handles the callback from the authentication provider and completes the
 * authentication process using PKCE (Proof Key for Code Exchange) for security.
 *
 * The activity is designed to be launched as a single-top activity and automatically
 * finishes itself after processing the authentication callback.
 */
public class SocialLoginActivity extends AppCompatActivity {

    /** Task completion source for managing the authentication result. */
    private static TaskCompletionSource<AuthResult> taskSource;
    /** Future for handling asynchronous authentication completion. */
    private static CompletableFuture<String> future;
    /** Helper class for managing authentication operations. */
    private static AuthHelper authHelper;
    /** The federated authentication provider being used. */
    private static FederatedAuthProvider authProvider;
    /** Logging tag for this class. */
    private final static String TAG = "FusabaseAuth";

    /**
     * Called when the activity is created to handle the authentication callback.
     *
     * This method processes the callback URI received from the authentication provider,
     * saves the callback data to preferences for recovery purposes, and completes
     * the authentication flow asynchronously. The activity automatically finishes
     * itself after processing.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri data = getIntent().getData();

        if(data == null)
        {
            FusabaseLogger.e(TAG, "Received callback data is empty. Sign in cannot be performed");
            return;
        }

        // Callback URI may contain sensitive authorization codes; never log it.
        FusabaseLogger.d(TAG, "Received callback data.");

        // Save code in Prefs so that if this activity was killed, the getPendingAuthResult()
        // can pick off where the user left off
        try {
            Utils.savePreferenceData(FusabaseAuth.getInstance().getApp().context,
                    data.toString(),
                    "fusabase_CALLBACK_DATA",
                    PREFERENCES_NAME);

        } catch  (Exception e) {
            FusabaseLogger.d(TAG, "Unable to store callback data in the preferences " + e.getMessage());
            return;
        }

        if(authHelper == null) {
            FusabaseLogger.w(TAG, "Earlier activity was killed/closed. The login can now only" +
                    "be processed by getPendingAuthResult()");
        }

        CompletableFuture.runAsync(() -> {
            authHelper.completeSocialLoginWithOAuthProvider(taskSource,
                    authHelper,
                    future);
        });

        finish();
    }

    /**
     * Creates and launches a custom tab for social login authentication.
     *
     * This method initializes the authentication flow by generating PKCE parameters,
     * storing them in preferences, and launching a custom tab with the authentication
     * URL. It returns a Task that will complete when the authentication flow finishes.
     *
     * @param app The FusabaseApp instance.
     * @param activity The calling activity.
     * @param provider The federated authentication provider to use.
     * @param helper The AuthHelper for managing the authentication process.
     * @param link Whether this is a linking operation (true) or sign-in (false).
     * @return A Task that completes with the AuthResult when authentication finishes.
     */
    protected static Task<AuthResult> createCustomTabForSignInWithProvider(@NonNull FusabaseApp app,
                                                                           @NonNull Activity activity,
                                                                           @NonNull FederatedAuthProvider provider,
                                                                           @NonNull AuthHelper helper,
                                                                           boolean link) {
        authHelper = helper;
        authProvider = provider;
        taskSource = new TaskCompletionSource<>();
        future = new CompletableFuture<>();
        taskSource.handleFuture(future);

        String codeVerifier = "";
        String codeChallenge = "";
        String codeChallengeMethod = "S256";
        try {

        codeVerifier = PKCEHelper.generateCodeVerifier();
        codeChallenge = PKCEHelper.generateCodeChallenge(codeVerifier);

        authHelper.saveDataToPreferences(authHelper
                .getFusabaseAuth()
                .getApp()
                .getApplicationContext(),
                "fusabase_CODE_VERIFIER",
                codeVerifier);

        authHelper.saveDataToPreferences(authHelper
                 .getFusabaseAuth()
                 .getApp().getApplicationContext(),
                 "fusabase_IS_LINK",
                 String.valueOf(link));

        authHelper.saveDataToPreferences(authHelper
                .getFusabaseAuth()
                .getApp()
                .getApplicationContext(),
                "fusabase_AUTH_PROVIDER_ID",
                provider.getProviderId());

        } catch (Exception e) {
            FusabaseLogger.d("SocialAuth", "Unable to store auth Initialization data in the preferences");
        }

        String updatedAuthUrl;
        if (app.fusabaseOptions.getAuthType().equals("idcs")) {
            updatedAuthUrl = Utils.addQueryParameterToURL(provider.getAuthUrl(),
                    "code_challenge", codeChallenge,
                    "code_challenge_method", codeChallengeMethod);
        } else {
            updatedAuthUrl = Utils.addQueryParameterToURL(provider.getAuthUrl(),
                    "code_challenge", codeChallenge,
                    "code_challenge_method", codeChallengeMethod,
                    "link", String.valueOf(link ? 1 : 0));
        }

        openCustomTab(activity, updatedAuthUrl);
        return taskSource.getTask();
    }

    /**
     * Launches a custom tab with the specified authentication URL.
     *
     * This method creates a CustomTabsIntent with specific flags to ensure proper
     * behavior in the authentication flow, then launches the URL in the custom tab.
     *
     * @param activity The activity from which to launch the custom tab.
     * @param authUrl The authentication URL to load in the custom tab.
     */
    private static void openCustomTab(@NonNull Activity activity,
                                      @NonNull String authUrl) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.setFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_TASK
        );
        customTabsIntent.launchUrl(activity, Uri.parse(authUrl));
    }
}
