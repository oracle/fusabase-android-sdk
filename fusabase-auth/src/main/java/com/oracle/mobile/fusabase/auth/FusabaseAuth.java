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

import static com.oracle.mobile.fusabase.auth.Config.IDCS_AUTH_TYPE;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.*;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.FusabaseNetworkException;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.task.*;
import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.internal.AuthInteropRegistry;
import com.oracle.mobile.fusabase.internal.AuthInterop;
import com.oracle.mobile.fusabase.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * Manages authentication state and provides methods for signing in, signing out, and managing users.
 *
 * <p>Example:</p>
 * <pre>{@code
 * FusabaseAuth auth = FusabaseAuth.getInstance();
 * auth.signInWithEmailAndPassword(email, password)
 *     .addOnSuccessListener(result -> {
 *         FusabaseUser user = result.getUser();
 *     });
 * }</pre>
 */
public class FusabaseAuth {

    private final static String TAG = "FusabaseAuth";
    protected final static String PREFERENCES_NAME = "FusabasePreferences";
    protected final static String LOGGED_IN_USER_KEY = "LOGGED_IN_USER";

    private final FusabaseApp app;
    private static FusabaseAuth instance;
    private final static HashMap<String, FusabaseAuth> AUTH_INSTANCES = new HashMap<>();
    private final List<AuthStateListener> authSubscribers = new ArrayList<>();
    private final List<IdTokenListener> IdTokenSubscribers = new ArrayList<>();
    private final Config config;

    /**
     * Currently signed-in user.
     *
     * <p>Example: Given a FusabaseAuth instance.</p>
     * <pre>{@code
     * FusabaseUser currentUser = FusabaseAuth.getInstance().currentUser;
     * }</pre>
     */
    public volatile @Nullable FusabaseUser currentUser;


    protected final AuthHelper authHelper;

    private FusabaseAuth(FusabaseApp app) {
        this.app = app;
        switch (app.fusabaseOptions.getAuthType()) {
            case "base": case "ldap":
                this.config = new ONPREMConfig(app.fusabaseOptions.getAppId(),
                        app.fusabaseOptions.getAuthId(),
                        app.fusabaseOptions.getAuthType(),
                        app.fusabaseOptions.getOrdsHost(),
                        app.fusabaseOptions.getProjectId());
                this.authHelper = new ONPREMAuthHelper((ONPREMConfig) config, this);
                break;
            case "idcs":
                this.config = new IDCSConfig(
                        app.fusabaseOptions.getAppId(),
                        app.fusabaseOptions.getProjectId(),
                        app.fusabaseOptions.getAuthType(),
                        app.fusabaseOptions.getOrdsHost(),
                        app.fusabaseOptions.getDomainURL());
                this.authHelper = new IDCSAuthHelper((IDCSConfig) this.config, this);
                break;
            default:
                throw new IllegalArgumentException("Unidentified authType received." +
                        " Cannot identify the Authentication type to move forward with.");
        }

        if(loggedInUserExists(app.getApplicationContext()))
        {
            try {
                this.currentUser = new FusabaseUserImpl(
                        this.authHelper
                                .readUserDetails(app.getApplicationContext(),
                                        getLoggedInUserExists(app.getApplicationContext()),
                                        PREFERENCES_NAME),
                        this,
                        null);
                this.informAuthSubscribers();
                this.informIdTokenSubscribers();
            } catch (FusabaseException e)
            {
                FusabaseLogger.e(TAG, "Cannot read logged in user details because " +
                        e.getCause());
            }
        }else {
            FusabaseLogger.d(TAG, "No Logged in user exists");
        }
    }

     /**
     * Adds a listener for authentication state changes.
     *
     * <p>The listener is called in the UI thread on the following events:</p>
     * <ul>
     *   <li>Right after the listener has been registered</li>
     *   <li>When a user signs in</li>
     *   <li>When the current user signs out</li>
     *   <li>When the current user changes</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseAuth.AuthStateListener listener = auth -> {
     *     FusabaseUser user = auth.getCurrentUser();
     *     // Update the UI for the current user, which may be null.
     * };
     * FusabaseAuth.getInstance().addAuthStateListener(listener);
     * }</pre>
     *
     * @param listener Listener to add.
     */
    public void addAuthStateListener(@NonNull AuthStateListener listener) {
        authSubscribers.add(listener);
        authStateChanged(listener);
    }

    /**
     * Adds a listener for ID token changes.
     *
     * <p>The listener is called in the UI thread on the following events:</p>
     * <ul>
     *   <li>Right after the listener has been registered</li>
     *   <li>When a user signs in</li>
     *   <li>When the current user signs out</li>
     *   <li>When the current user changes</li>
     *   <li>When there is a change in the current user's token</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseAuth.IdTokenListener listener = auth -> {
     *     FusabaseUser user = auth.getCurrentUser();
     *     // Update the UI for the current user, which may be null.
     * };
     * FusabaseAuth.getInstance().addIdTokenListener(listener);
     * }</pre>
     *
     * @param listener Listener to add.
     */
    public void addIdTokenListener (@NonNull IdTokenListener listener) {
        IdTokenSubscribers.add(listener);
        IdTokenChanged(listener);
    }

    protected void informAuthSubscribers() {
        FusabaseUser user = this.currentUser;
        AuthInteropRegistry.session(app).update(user == null ? null : user.getUid());
        for (AuthStateListener listener : this.authSubscribers) {
            authStateChanged(listener);
        }
    }

    protected void informIdTokenSubscribers () {
        for (IdTokenListener listener : this.IdTokenSubscribers) {
            IdTokenChanged(listener);
        }
    }

    private void IdTokenChanged(@NonNull IdTokenListener listener) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            listener.onIdTokenChanged(this);
        });
    }

    private void authStateChanged(@NonNull AuthStateListener listener) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            listener.onAuthStateChanged(this);
        });
    }

    private static String getLoggedInUserExists (@NonNull Context context) {

        try{
            return Utils.getPreferenceData(context,
                    LOGGED_IN_USER_KEY,
                    PREFERENCES_NAME);
        } catch (Exception e)
        {
            FusabaseLogger.e(TAG, "Cannot infer if a user has already logged in or not because" +
                    " " + e.getMessage());
            // Failing silently
        }
        return "";
    }

    private static boolean loggedInUserExists (@NonNull Context context) {

        try{
            return !getLoggedInUserExists(context).isEmpty();
        } catch (Exception e)
        {
            FusabaseLogger.e(TAG, "Cannot infer if a user has already logged in or not because" +
                    " " + e.getMessage());
            // Failing Silently
        }
        return false;
    }


    /**
     * Creates a new user with the given email and password.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseAuth auth = FusabaseAuth.getInstance();
     * auth.createUserWithEmailAndPassword(email, password)
     *     .addOnSuccessListener(result -> {
     *         FusabaseUser user = result.getUser();
     *     });
     * }</pre>
     *
     * @param email    User's email address.
     * @param password User's password.
     * @return Task representing the creation operation.
     */
    public @NonNull Task<AuthResult> createUserWithEmailAndPassword (
            @NonNull String email,
            @NonNull String password) {

        TaskCompletionSource<AuthResult> taskCompletionSource = new TaskCompletionSource<>();
        Task<AuthResult> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {

            try {
                JsonObject res = this.authHelper.registerUser(email, password);
                JsonObject userData = this.authHelper.getAuthenticationDetails(email,
                        password,
                        res);
                AuthResult result = new AuthResult() {
                    @Nullable
                    @Override
                    public AuthCredential getCredential() {
                        return new EmailAuthCredential(EmailAuthProvider.PROVIDER_ID,
                                EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD,
                                email,
                                password);
                    }

                    @Nullable
                    @Override
                    public FusabaseUser getUser() {
                        return new FusabaseUserImpl(userData,
                                FusabaseAuth.instance,
                                password);
                    }
                };

                this.currentUser = result.getUser();

                this.authHelper.saveUserDetails(this.app.getApplicationContext(),
                        userData,
                        PREFERENCES_NAME);

                this.informAuthSubscribers();
                this.informIdTokenSubscribers();
                taskCompletionSource.setResult(result);

            } catch (Exception e) {

                FusabaseLogger.e(TAG, "Exception encountered while performing createUserWithPassword operation");
                Exception exception = e;
                if(!(exception instanceof FusabaseException))
                    exception = new FusabaseNetworkException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage());
                throw new CompletionException(exception);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

     /**
     * Gets the Fusabase application instance associated with this FusabaseAuth instance.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseApp app = FusabaseAuth.getInstance().getApp();
     * }</pre>
     *
     * @return Fusabase application instance.
     */
    public @NonNull FusabaseApp getApp() {
        return this.app;
    }

    /**
     * Gets the currently signed-in user.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseAuth auth = FusabaseAuth.getInstance();
     * FusabaseUser user = auth.getCurrentUser();
     * if (user != null) {
     *     String email = user.getEmail();
     * }
     * }</pre>
     *
     * @return Currently signed-in user, or null if none.
     */
    public @Nullable FusabaseUser getCurrentUser() {
        return this.currentUser;
    }

    /**
     * Gets the FusabaseAuth instance associated with the given FusabaseApp. 
     * This a built-in function designed to handle the "Forgot Password" workflow for your users.
     *
     * <p>Example: Initialize FusabaseApp before accessing authentication.</p>
     * <pre>{@code
     * FusabaseAuth auth = FusabaseAuth.getInstance(FusabaseApp.getInstance());
     * }</pre>
     *
     * @param app The FusabaseApp instance.
     * @return The FusabaseAuth instance.
     */
    @NonNull
    public static FusabaseAuth getInstance(@NonNull FusabaseApp app)
    {
        if(FusabaseAuth.AUTH_INSTANCES.get(app.getName()) == null)
        {
            FusabaseLogger.d(TAG, "Creating a new FusabaseAuth instance for the FusabaseApp "
                    + app.name);
            instance = new FusabaseAuth(app);
            FusabaseAuth.AUTH_INSTANCES.put(app.getName(), instance);
        }

        FusabaseAuth auth = Objects.requireNonNull(FusabaseAuth.AUTH_INSTANCES.get(app.getName()));
        AuthInteropRegistry.register(app, new AuthInterop() {
            @Override public String getCurrentUserId() {
                FusabaseUser user = auth.getCurrentUser();
                return user == null ? null : user.getUid();
            }
            @Override public String getAccessToken() {
                FusabaseUser user = auth.getCurrentUser();
                if (user == null) return null;
                try {
                    String token = user.loadUserData().getString("accessToken", "");
                    return user == auth.getCurrentUser() ? token : null;
                } catch (FusabaseException failure) {
                    return null;
                }
            }
        });
        return auth;
    }

    /**
     * Gets the default FusabaseAuth instance.
     *
     * <p>Example: Initialize FusabaseApp before accessing authentication.</p>
     * <pre>{@code
     * FusabaseAuth auth = FusabaseAuth.getInstance();
     * }</pre>
     *
     * @return The default FusabaseAuth instance.
     */
    @Keep
    public static @NonNull FusabaseAuth getInstance()  {

        FusabaseApp app = FusabaseApp.getInstance();
        if (instance == null) {
            FusabaseLogger.d(TAG, "Creating a new FusabaseAuth instance for the FusabaseApp "
                    + app.name);
            instance = new FusabaseAuth(app);
            FusabaseAuth.AUTH_INSTANCES.put(app.getName(), instance);
        }

        return FusabaseAuth.getInstance(app);
    }

     /**
     * Removes a listener for authentication state changes.
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Use the same listener instance that was registered.
     * FusabaseAuth.getInstance().removeAuthStateListener(listener);
     * }</pre>
     *
     * @param listener Listener to remove.
     */
    public void removeAuthStateListener(@NonNull AuthStateListener listener) {
        boolean success = this.authSubscribers.remove(listener);

        if (success)
            FusabaseLogger.d(TAG, "removeAuthListener executed successfully.");
        else
            FusabaseLogger.d(TAG, "Provided AuthStateListener is not registered. No listener to remove.");
    }

     /**
     * Removes a listener for ID token changes.
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Use the same listener instance that was registered.
     * FusabaseAuth.getInstance().removeIdTokenListener(listener);
     * }</pre>
     *
     * @param listener Listener to remove.
     */
    public void removeIdTokenListener(@NonNull IdTokenListener listener) {
        boolean success = this.IdTokenSubscribers.remove(listener);

        if (success)
            FusabaseLogger.d(TAG, "removeIdTokenListener executed successfully.");
        else
            FusabaseLogger.d(TAG, "Provided IdTokenListener is not registered. No listener to remove.");
    }

    /**
     * Sends a password reset email to the specified email address. This
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseAuth auth = FusabaseAuth.getInstance();
     * auth.sendPasswordResetEmail(email).addOnSuccessListener(unused -> {
     *     // Ask the user for the code sent by email.
     * });
     * }</pre>
     *
     * @param email Email address to send the password reset email to.
     * @return Task representing the password reset operation.
     */
    @NonNull
    public Task<Void> sendPasswordResetEmail (@NonNull String email) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                this.authHelper.sendPasswordResetEmailHelper(email);
                // Store the email for security validation throughout the reset flow
                this.authHelper.saveDataToPreferences(this.app.getApplicationContext(),
                    "password_reset_email", email);
            } catch (Exception e) {
                FusabaseLogger.e(TAG, "Exception encountered while performing sendPasswordResetEmail operation");
                Exception exception = e;
                if(!(exception instanceof FusabaseException))
                    exception = new FusabaseNetworkException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage());
                throw new CompletionException(exception);
            }
            taskCompletionSource.setResult(null);
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Gets the pending authentication result from a previous sign-in attempt.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseAuth auth = FusabaseAuth.getInstance();
     * Task<AuthResult> pending = auth.getPendingAuthResult();
     * if (pending != null) {
     *     pending.addOnSuccessListener(result -> {
     *         FusabaseUser user = result.getUser();
     *     });
     * }
     * }</pre>
     *
     * @return Task representing the pending authentication result, or null if none.
     */
    @Nullable
    public Task<AuthResult> getPendingAuthResult() {

        String callbackData = "";
        try {
             callbackData = authHelper.readDataFromPreferences(authHelper
                            .getFusabaseAuth()
                            .getApp()
                            .getApplicationContext(),
                    "fusabase_CALLBACK_DATA");
        } catch (Exception e)
        {
            FusabaseLogger.d("Error encountered while reading data from shared prefs");
            return null;
        }

        FusabaseLogger.d("Retreived Callback data on first check " + callbackData);

        if(callbackData != null && !callbackData.isEmpty()) {
            FusabaseLogger.d("Earlier Callback data is present");
            TaskCompletionSource<AuthResult> taskCompletionSource = new TaskCompletionSource<>();
            Task<AuthResult> task = taskCompletionSource.getTask();
            CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
                this.authHelper.completeSocialLoginWithOAuthProvider(taskCompletionSource, this.authHelper, null);
                return "";
            });
            taskCompletionSource.handleFuture(future);
            return task;
        }

        return null;
    }

    @NonNull
    protected Task<AuthResult> signInWithCredentialLinking (@NonNull AuthCredential credential,
                                                            boolean link) {
        TaskCompletionSource<AuthResult> taskCompletionSource = new TaskCompletionSource<>();
        Task<AuthResult> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject res = this.authHelper.signInWithCredentialHelper(credential, link);
                JsonObject userDetails = this.authHelper.getUserDetails(
                    new GetTokenResult(
                        new IdToken(
                            res.getString("access_token"), this.app.getOptions().getAuthType())));
                JsonObject userData = Json.createObjectBuilder()
                    .add("userEmail", userDetails.containsKey("emails") ?
                        userDetails.getJsonArray("emails").getJsonObject(0).getString("value") :
                        userDetails.getString("email"))
                    .add("userDetails", userDetails)
                    .add("authnToken", JsonValue.NULL)
                    .add("accessToken", res.getString("access_token"))
                    .add("refreshToken", res.getString("refresh_token"))
                    .build();

                AuthResult result = new AuthResult() {
                    @Nullable
                    @Override
                    public AuthCredential getCredential() {
                        switch (credential.getProvider()) {

                            case GoogleAuthProvider.PROVIDER_ID:
                                return new GoogleAuthCredential(
                                    credential.getProvider(),
                                    credential.getSignInMethod(),
                                    res.getString("accessToken")
                                );

                            case GithubAuthProvider.PROVIDER_ID:
                                return new GithubAuthCredential(
                                    credential.getProvider(),
                                    credential.getSignInMethod(),
                                    res.getString("accessToken")
                                );

                            case FacebookAuthProvider.PROVIDER_ID:
                                return new FacebookAuthCredential(
                                    credential.getProvider(),
                                    credential.getSignInMethod(),
                                    res.getString("accessToken")
                                );

                            default:
                                return null;
                        }
                    }

                    @Nullable
                    @Override
                    public FusabaseUser getUser() {
                        String password = credential instanceof EmailAuthCredential
                            ? ((EmailAuthCredential) credential).getPassword()
                            : null;
                        return new FusabaseUserImpl(userData,
                            authHelper.getFusabaseAuth(),
                            password);
                    }
                };

                this.currentUser = result.getUser();

                FusabaseLogger.d("SocialAuth", "Stored user data from signInWithCredential");

                this.authHelper.saveUserDetails(this.app.getApplicationContext(),
                    userData, PREFERENCES_NAME);

                this.informAuthSubscribers();
                this.informIdTokenSubscribers();
                taskCompletionSource.setResult(result);

            } catch (Exception e) {
                FusabaseLogger.e(TAG, "Exception encountered while performing signInWithCredential operation");
                if (e instanceof FusabaseAuthMultiFactorException) {
                    throw new CompletionException(e);
                }
                Exception exception = e;
                if(!(exception instanceof FusabaseException))
                    exception = new FusabaseNetworkException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage());
                throw new CompletionException(exception);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    @NonNull
    protected Task<AuthResult> socialUnlink(@NonNull String provider) {
        TaskCompletionSource<AuthResult> taskCompletionSource = new TaskCompletionSource<>();
        Task<AuthResult> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject res = this.authHelper.socialUnlinkHelper(provider);
                JsonObject userDetails = this.authHelper.getUserDetails(
                        new GetTokenResult(
                                new IdToken(
                                        res.getString("access_token"), this.app.getOptions().getAuthType())));
                JsonObject userData = Json.createObjectBuilder()
                        .add("userEmail", userDetails.containsKey("emails") ?
                                userDetails.getJsonArray("emails").getJsonObject(0).getString("value") :
                                userDetails.getString("email"))
                        .add("userDetails", userDetails)
                        .add("authnToken", JsonValue.NULL)
                        .add("accessToken", res.getString("access_token"))
                        .add("refreshToken", res.getString("refresh_token"))
                        .build();
                String remainingProvider = userDetails.containsKey("idpName") ?
                        userDetails.getString("idpName") :
                        userDetails.getString("idp_name");
                remainingProvider = remainingProvider.equals("UserNamePassword") ? "epw" : remainingProvider;

                AuthResult result = getAuthResult(remainingProvider, res, userData);

                FusabaseLogger.d("SocialAuth", "Stored user data from signInWithCredential");

                this.authHelper.saveUserDetails(this.app.getApplicationContext(),
                    userData, PREFERENCES_NAME);

                this.informAuthSubscribers();
                this.informIdTokenSubscribers();
                taskCompletionSource.setResult(result);

            } catch (Exception e) {
                FusabaseLogger.e(TAG, "Exception encountered while performing signInWithCredential operation");
                Exception exception = e;
                if(!(exception instanceof FusabaseException))
                    exception = new FusabaseNetworkException(exception.getMessage() == null ?
                            "Internal Error Occurred" : exception.getMessage());
                throw new CompletionException(exception);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    @NonNull
    private AuthResult getAuthResult(String remainingProvider, JsonObject res, JsonObject userData) {
        String finalRemainingProvider = remainingProvider;
        AuthResult result = new AuthResult() {
            @Nullable
            @Override
            public AuthCredential getCredential() {
                switch (finalRemainingProvider) {

                    case GoogleAuthProvider.PROVIDER_ID:
                        return new GoogleAuthCredential(
                                remainingProvider,
                                remainingProvider,
                                res.getString("accessToken")
                        );

                    case GithubAuthProvider.PROVIDER_ID:
                        return new GithubAuthCredential(
                                remainingProvider,
                                remainingProvider,
                                res.getString("accessToken")
                        );

                    case FacebookAuthProvider.PROVIDER_ID:
                        return new FacebookAuthCredential(
                                remainingProvider,
                                remainingProvider,
                                res.getString("accessToken")
                        );

                    case EmailAuthProvider.PROVIDER_ID:
                        return new EmailAuthCredential(
                                remainingProvider,
                                remainingProvider,
                                userData.getString("userEmail"),
                                null
                        );
                    default:
                        return null;
                }
            }

            @Nullable
            @Override
            public FusabaseUser getUser() {
                return new FusabaseUserImpl(userData,
                    authHelper.getFusabaseAuth(),
                    null);
            }
        };
        return result;
    }

    /**
     * Signs in with the given credential.
     *
     * <p>Example:</p>
     * <pre>{@code
     * AuthCredential credential = EmailAuthProvider.getCredential(email, password);
     * FusabaseAuth.getInstance().signInWithCredential(credential);
     * }</pre>
     *
     * @param credential The authentication credential.
     * @return Task representing the sign-in operation.
     */
    @NonNull
    public Task<AuthResult> signInWithCredential (@NonNull AuthCredential credential) {
        return signInWithCredentialLinking(credential, false);
    }

     /**
     * Signs in with the specified provider.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseAuth auth = FusabaseAuth.getInstance();
     * GoogleAuthProvider provider = new GoogleAuthProvider(auth);
     * auth.startActivityForSignInWithProvider(activity, provider);
     * }</pre>
     *
     * @param activity   {@code Activity}   Activity context.
     * @param authProvider {@code OAuthProvider} Provider to sign in with.
     * @return Task representing the sign-in operation.
     */
    @NonNull
    public Task<AuthResult> startActivityForSignInWithProvider (
            @NonNull Activity activity,
            @NonNull FederatedAuthProvider authProvider) {

        if(this.config.authType.equals(IDCS_AUTH_TYPE))
        {
            if(!(authProvider instanceof IDCSAuthProvider))
                throw new IllegalArgumentException("IDCS Auth Type supports only IDCSAuthProvider");
        }
        return SocialLoginActivity.createCustomTabForSignInWithProvider(
                this.app,
                activity,
                authProvider,
                this.authHelper,
                false
        );
    }

    /**
     * Signs in with the specified email and password.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseAuth auth = FusabaseAuth.getInstance();
     * auth.signInWithEmailAndPassword(email, password)
     *     .addOnSuccessListener(result -> {
     *         FusabaseUser user = result.getUser();
     *     });
     * }</pre>
     *
     * @param email    Email address to sign in with.
     * @param password Password to sign in with.
     * @return Task representing the sign-in operation.
     */
    public @NonNull Task<AuthResult> signInWithEmailAndPassword (@NonNull String email, @NonNull String password) {

        TaskCompletionSource<AuthResult> taskCompletionSource = new TaskCompletionSource<>();
        Task<AuthResult> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {

            try {

                JsonObject res = this.authHelper.authenticateUser(email, password);
                JsonObject userData = this.authHelper.getAuthenticationDetails(email, password, res);
                AuthResult result = new AuthResult() {
                    @Nullable
                    @Override
                    public AuthCredential getCredential() {
                        return new EmailAuthCredential(EmailAuthProvider.PROVIDER_ID,
                                EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD,
                                email,
                                password);
                    }

                    @Nullable
                    @Override
                    public FusabaseUser getUser() {
                        return new FusabaseUserImpl(userData,
                                FusabaseAuth.instance,
                                password);
                    }
                };

                this.currentUser = result.getUser();

                this.authHelper.saveUserDetails(this.app.getApplicationContext(),
                        userData, PREFERENCES_NAME);

                this.informAuthSubscribers();
                this.informIdTokenSubscribers();
                taskCompletionSource.setResult(result);

            } catch (FusabaseException e) {
                if (e instanceof FusabaseAuthMultiFactorException) {
                    throw new CompletionException(e);
                }
                throw new CompletionException(new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(), "Sign in failed. Please check your credentials and try again."));
            }

            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;

    }

     /**
      * Signs out the current user.
      *
      * <p>Example:</p>
      * <pre>{@code
      * FusabaseAuth auth = FusabaseAuth.getInstance();
      * auth.signOut().addOnSuccessListener(unused -> {
      *     // Show the signed-out screen.
      * });
      * }</pre>
      *
      * <p>Clears the local session and invalidates its OracleDB snapshot listeners before
      * remote logout starts. A remote logout failure does not restore the local session.
      * Register new snapshot listeners after signing in again.</p>
      */
     public @NonNull Task<Void> signOut() {
        TaskCompletionSource<Void> completion = new TaskCompletionSource<>();
        final String refreshToken;
        final String idTokenHint;
        final FusabaseUser user;
        synchronized (this) {
            user = currentUser;
            refreshToken = user == null ? "" : user.getRefreshToken();
            String hint = "";
            if (user != null) {
                try { hint = user.loadUserData().getString("idToken", ""); }
                catch (FusabaseException ignored) { /* Local logout must still complete. */ }
            }
            idTokenHint = hint;
            currentUser = null;
        }
        // No Auth monitor is held while notifying other modules. Invalidate before disk cleanup
        // or remote logout so neither can delay transport cancellation.
        AuthInteropRegistry.session(app).invalidate();
        if (user != null) {
            try { authHelper.clearUserDetails(app.getApplicationContext(), user.getUid(), PREFERENCES_NAME); }
            catch (FusabaseException failure) {
                FusabaseLogger.w(TAG, "Unable to clear persisted user details during logout");
            }
        }
        informAuthSubscribers();
        informIdTokenSubscribers();
        if (refreshToken.isEmpty() && idTokenHint.isEmpty()) {
            completion.setResult(null);
            return completion.getTask();
        }
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                authHelper.performSignOut(refreshToken, idTokenHint);
                completion.setResult(null);
            } catch (FusabaseException failure) {
                throw new CompletionException(new FusabaseAuthException(
                        FusabaseAuthException.Code.INTERNAL.toString(),
                        "Signed out locally, but remote logout failed."));
            }
            return "";
        });
        completion.handleFuture(future);
        return completion.getTask();
    }


    /**
     * Updates the current user.
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Given a user returned by a successful authentication operation:
     * FusabaseAuth.getInstance().updateCurrentUser(user);
     * }</pre>
     *
     * @param user New user to update to.
     */
    public void updateCurrentUser(@Nullable FusabaseUser user) {
        this.currentUser = user;
        this.informAuthSubscribers();
        this.informIdTokenSubscribers();
    }

    /**
     * Verifies a password reset code.
     *
     * <p>Example:</p>
     * <pre>{@code
     * // After sendPasswordResetEmail(email) succeeds on this device:
     * FusabaseAuth.getInstance().verifyPasswordResetCode(code)
     *     .addOnSuccessListener(email -> {
     *         // Ask the user to choose a new password.
     *     });
     * }</pre>
     *
     * @param code Password reset code to verify.
     * @return Task representing the verification operation.
     */
    public Task<String> verifyPasswordResetCode(@NonNull String code) {
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
        Task<String> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            JsonObject res = null;
            try {
                res = this.authHelper.verifyPasswordResetCodeHelper(code);
                String backendUsername = res.getString("username");

                // Security validation: ensure the backend username matches the stored email
                String storedEmail = this.authHelper.readDataFromPreferences(
                    this.app.getApplicationContext(), "password_reset_email");
                if (storedEmail == null || storedEmail.isEmpty()) {
                    throw new FusabaseException("No password reset request found. Please request a password reset first.");
                }
                if (!storedEmail.equals(backendUsername)) {
                    throw new FusabaseException("Password reset code is not valid for the requested email address.");
                }

                taskCompletionSource.setResult(backendUsername);
                return "";
            } catch (FusabaseException e) {
                throw new CompletionException(new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(), "Password reset verification failed. Please check your code and try again."));
            }
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Confirms a password reset.
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Given the emailed reset code and a new password entered by the user:
     * FusabaseAuth.getInstance().confirmPasswordReset(code, newPassword);
     * }</pre>
     *
     * @param code       Password reset code.
     * @param newPassword New password.
     * @return Task representing the confirmation operation.
     */
    public @NonNull Task<Void> confirmPasswordReset(@NonNull String code, @NonNull String newPassword) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject res = this.authHelper.confirmPasswordResetHelper(code, newPassword);
                taskCompletionSource.setResult(null);
                return "";
            } catch (FusabaseException e) {
                throw new CompletionException(new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(), "Password reset failed. Please try again."));
            } finally {
                // Always clear the stored email after the operation
                try {
                    this.authHelper.deleteDataFromPreferences(this.app.getApplicationContext(), "password_reset_email");
                } catch (Exception clearException) {
                    FusabaseLogger.w(TAG, "Failed to clear password reset email from preferences: " + clearException.getMessage());
                }
            }
       });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    protected Config getConfig() {
        return this.config;
    }

    /**
     * Interface for listening to authentication state changes.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseAuth.AuthStateListener listener = auth -> {
     *     FusabaseUser user = auth.getCurrentUser();
     *     // Update the UI for the current user, which may be null.
     * };
     * FusabaseAuth.getInstance().addAuthStateListener(listener);
     * }</pre>
     */
    public interface AuthStateListener {
        /**
         * Called only when the user authentication state changes, such as when a user signs in or signs out.
         *
         * <p>Example:</p>
         * <pre>{@code
         * FusabaseAuth.AuthStateListener listener = auth -> {
         *     FusabaseUser user = auth.getCurrentUser();
         *     // Update the UI for the current user, which may be null.
         * };
         * FusabaseAuth.getInstance().addAuthStateListener(listener);
         * }</pre>
         *
         * @param auth FusabaseAuth instance.
         */
        abstract void onAuthStateChanged(@NonNull FusabaseAuth auth);
    }

    /**
     * Interface for listening to ID token changes.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseAuth.IdTokenListener listener = auth -> {
     *     FusabaseUser user = auth.getCurrentUser();
     *     // Update the UI for the current user, which may be null.
     * };
     * FusabaseAuth.getInstance().addIdTokenListener(listener);
     * }</pre>
     */
    public interface IdTokenListener {

         /**
         * Called only when the ID token changes, such as when a user signs in, signs out, or when the token is refreshed.
         *
         * <p>Example:</p>
         * <pre>{@code
         * FusabaseAuth.IdTokenListener listener = auth -> {
         *     FusabaseUser user = auth.getCurrentUser();
         *     // Update the UI for the current user, which may be null.
         * };
         * FusabaseAuth.getInstance().addIdTokenListener(listener);
         * }</pre>
         *
         * @param auth FusabaseAuth instance.
         */
        abstract void onIdTokenChanged(
                @NonNull FusabaseAuth auth
        );
    }

}
