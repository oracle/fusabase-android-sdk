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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.JsonObject;

/**
 * Abstract base class representing an fusabase user.
 */
public abstract class FusabaseUser implements UserInfo {

    private final static String TAG = "FusabaseAuth";

    protected UserHelper userHelper;
    protected Config config;
    @Nullable
    protected final String password;
    protected String displayName;
    protected String email;
    protected String photoUrl;
    protected String phoneNumber;
    protected final FusabaseAuth auth;
    protected FusabaseUser instance;

    /**
     * Loads user data from persistent storage.
     *
     * @return JsonObject containing user data
     * @throws FusabaseException if loading fails
     */
    protected JsonObject loadUserData() throws FusabaseException {
        return auth.authHelper.readUserDetails(
            auth.getApp().getApplicationContext(),
            getUid(),
            FusabaseAuth.PREFERENCES_NAME
        );
    }

    /**
     * Constructs an FusabaseUser instance from the given JSON data, authentication object, and password.
     *
     * @param data   JSON data containing user information
     * @param auth   authentication object
     * @param password password associated with the user account
     */
    protected FusabaseUser(@NonNull JsonObject data,
                        @NonNull FusabaseAuth auth,
                        @Nullable String password) {
        this.instance = this;
        this.config = auth.getConfig();
        this.auth = auth;
        this.password = password;
        if (Objects.equals(this.config.authType, "idcs")) {
            this.userHelper = new IDCSUserHelper((IDCSConfig) this.config, this);
        } else {
            this.userHelper = new ONPREMUserHelper((ONPREMConfig) this.config, this);
        }
    }

    protected abstract @NonNull String getRefreshToken();

    protected abstract @NonNull String getAccessToken();
    /**
     * Returns the display name of the user.
     *
     * @return display name
     */
    public abstract @Nullable String getDisplayName();

    /**
     * Returns the photo url of the user.
     *
     * @return photo url
     */
    public abstract @Nullable String getPhotoUrl();

    /**
     * Returns the Phone Number of the user.
     *
     * @return phone Number
     */
    public abstract @Nullable String getPhoneNumber();

     /**
     * Returns the email address of the user.
     *
     * @return email address
     */
    public abstract @Nullable String getEmail();

    /**
     * Returns a task that retrieves the access token for the user.
     *
     * @param forceRefresh whether to force a refresh of the access token
     * @return task that retrieves the access token
     */
    public @NonNull Task<GetTokenResult> getIdToken(boolean forceRefresh) {

        TaskCompletionSource<GetTokenResult> taskCompletionSource = new TaskCompletionSource<>();
        Task<GetTokenResult> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                if (!forceRefresh) {
                    // Fetch access token from keystore
                    JsonObject userData = loadUserData();
                    String accessTokenStr = userData.getString("accessToken");
                    IdToken accessToken = new IdToken(accessTokenStr, auth.getApp().getOptions().getAuthType());
                    taskCompletionSource.setResult(new GetTokenResult(accessToken));
                } else {
                    // Fetch refresh token from keystore and refresh
                    String newAccessTokenStr = userHelper.refreshAccessToken();
                    IdToken newToken = new IdToken(newAccessTokenStr, auth.getApp().getOptions().getAuthType());
                    taskCompletionSource.setResult(new GetTokenResult(newToken));
                    this.auth.informIdTokenSubscribers();
                }
            } catch (FusabaseException e) {
                throw new CompletionException(e);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;

    }

    /**
     * Returns the provider ID of the user.
     *
     * @return provider ID
     */
    public abstract @NonNull String getProviderId();

    public abstract @NonNull FusabaseUserMetadata getMetadata();
    /**
     * Returns the unique ID of the user.
     *
     * @return unique ID
     */
    public abstract @NonNull String getUid();

    /**
     * Checks if the user is anonymous.
     *
     * @return true if the user is anonymous, false otherwise
     */
    public abstract boolean isAnonymous();

    /**
     * Updates the password of the user.
     *
     * @param newPassword new password
     * @return task that updates the password
     */
    public @NonNull Task<Void> updatePassword(@NonNull String newPassword) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject res = this.userHelper.updatePasswordHelper(this, newPassword, this.password);
                taskCompletionSource.setResult(null);
                return "";
            } catch (FusabaseException e) {
                throw new CompletionException(new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(), "Failed to update password. Please try again."));
            }
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Updates the user's profile information.
     *
     * @param request profile update request
     * @return task that updates the profile
     */
    public @NonNull Task<Void> updateProfile(@NonNull UserProfileChangeRequest request) {

        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject res = this.userHelper.updateProfile(request);
                // Update Current Instance
                this.displayName = res.containsKey("displayName") ? res.getString("displayName") : displayName;
                this.photoUrl = res.containsKey("photos")
                    ? res.getJsonArray("photos").get(0).asJsonObject().getString("value")
                    : res.containsKey("photoUrl") ? res.getString("photoUrl") : photoUrl;
                this.phoneNumber = res.containsKey("phoneNumbers")
                    ? res.getJsonArray("phoneNumbers").get(0).asJsonObject().getString("value")
                    : res.containsKey("phoneNumber") ? res.getString("phoneNumber") : phoneNumber;
                taskCompletionSource.setResult(null);
                return "";
            } catch (FusabaseException e) {
                throw new CompletionException(new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(), "Failed to update user profile. Please try again."));
            }
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

     /**
     * Sends an email to user's registered email for verification
     *
     * @return task that verifies the email address
     */
    public @NonNull Task<Void> sendEmailVerification () {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                this.userHelper.sendEmailVerificationHelper(this.getEmail(), this.getUid());
            } catch (FusabaseException e) {
                throw new CompletionException(new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(), "Failed to send email verification. Please try again."));
            }
            taskCompletionSource.setResult(null);
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Reloads the user's data from the server.
     *
     * @return task that reloads the user data
     */
    public @NonNull Task<Void> reload() {

        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {

            try {
                JsonObject data = this.auth.authHelper.reloadUser(this);
                // All the parameters that should be updated in reload will be here.
                this.displayName = data.getString("displayName");
                this.photoUrl = data.containsKey("photoUrl")
                    ? data.getString("photoUrl") : this.photoUrl;
                this.phoneNumber = data.containsKey("phoneNumber")
                    ? data.getString("phoneNumber") : this.phoneNumber;
                // Update the current user in the auth instance to ensure getCurrentUser() returns updated data
                this.auth.currentUser = this;
                taskCompletionSource.setResult(null);
            } catch (FusabaseException e) {
                // If reload fails, keep current values
                FusabaseLogger.e(TAG, "Reload failed, keeping current user data: " + e.getMessage());
                taskCompletionSource.setResult(null);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Attaches the given AuthCredential to the user.
     *
     * @param credential the credential to attach
     * @return task representing the link operation
     */
    @NonNull
    public Task<AuthResult> linkWithCredential(@NonNull AuthCredential credential) {
        return this.auth.signInWithCredentialLinking(credential, true);
    }

    /**
     * Starts an activity to link the current user with a federated authentication provider
     * using the Custom Tabs flow. This method initiates the authentication process in a
     * browser-like interface to allow the user to authenticate with the specified provider
     * and link their account to the current user.
     *
     * @param activity the Android Activity that will host the Custom Tab
     * @param federatedAuthProvider the federated authentication provider to link with
     * @return a Task containing the AuthResult upon successful linking
     * @throws IllegalArgumentException if IDCS auth type is used with IDCS provider
     */
    @NonNull
    public Task<AuthResult> startActivityForLinkWithProvider (
            @NonNull Activity activity,
            @NonNull FederatedAuthProvider federatedAuthProvider) {

        if(this.config.authType.equals(IDCS_AUTH_TYPE))
        {
            if(federatedAuthProvider instanceof IDCSAuthProvider)
                throw new IllegalArgumentException("IDCS Auth Type not supported for linking");
        }

        return SocialLoginActivity.createCustomTabForSignInWithProvider(
                this.auth.getApp(),
                activity,
                federatedAuthProvider,
                this.auth.authHelper,
                true
        );
    }
    /**
     * Detaches credentials from a given provider type from this user.
     *
     * @param provider the provider type
     * @return task representing the unlink operation
     */
    @NonNull
    public Task<AuthResult> unlink(@NonNull String provider) {
       return this.auth.socialUnlink(provider);
    }

}
