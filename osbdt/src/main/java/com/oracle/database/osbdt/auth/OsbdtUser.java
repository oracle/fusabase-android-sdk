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

package com.oracle.database.osbdt.auth;

import static com.oracle.database.osbdt.auth.OsbdtAuth.PREFERENCES_NAME;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.database.osbdt.oracledb.OsbdtOracledbException;
import com.oracle.database.osbdt.OsbdtException;
import com.oracle.database.osbdt.OsbdtNetworkException;
import com.oracle.database.osbdt.logger.OsbdtLogger;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.TaskCompletionSource;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * Abstract base class representing an osbdt user.
 */
public abstract class OsbdtUser implements UserInfo {

    private final static String TAG = "OsbdtAuth";

    protected UserHelper userHelper;
    protected Config config;
    @Nullable
    protected final String password;
    protected String displayName;
    protected String email;
    protected String photoUrl;
    protected String phoneNumber;
    protected String accessRefreshToken;
    protected IdToken accessToken;
    protected String refreshAuthnToken;
    protected String authnToken;
    protected String idcsAccessToken;
    protected String idcsRefreshToken;
    protected String snapshotAccessToken;
    protected String snapshotRefreshToken;
    protected final OsbdtAuth auth;
    protected OsbdtUser instance;

    /**
     * Constructs an OsbdtUser instance from the given JSON data, authentication object, and password.
     *
     * @param data   JSON data containing user information
     * @param auth   authentication object
     * @param password password associated with the user account
     */
    protected OsbdtUser(@NonNull JsonObject data,
                        @NonNull OsbdtAuth auth,
                        @Nullable String password) {
        this.instance = this;
        this.config = auth.getConfig();
        this.auth = auth;
        this.password = password;
        if (Objects.equals(this.config.authType, "idcs")) {
            this.idcsAccessToken = data.getString("idcsAccessToken");
            this.idcsRefreshToken = data.getString("idcsRefreshToken");
            this.userHelper = new IDCSUserHelper((IDCSConfig) this.config,
                    data.containsKey("authnToken") && data.getString("authnToken") != null
                        ? new IdToken(data.getString("authnToken"), "idcs") :
                        null,
                    data.getString("authnRefreshToken", null),
                    data.containsKey("accessToken") && data.getString("accessToken") != null ?
                        new IdToken(data.getString("accessToken"), "idcs") :
                    null,
                    data.getString("refreshToken"),
                    data.containsKey("idcsAccessToken") && data.getString("idcsAccessToken") != null ?
                        new IdToken(data.getString("idcsAccessToken"), "idcs") :
                    null,
                    data.getString("idcsRefreshToken"),
                    this);
            IdToken token = new IdToken(data.getString("accessToken"), "idcs");
            if ( token.validateAccessToken() )
                this.accessToken = token;
            else {
                try {
                    this.accessRefreshToken = this.userHelper.refreshAccessToken();
                } catch (Exception e) {
                    OsbdtLogger.e(TAG, "Cannot refresh user id token because + " +
                        e.getMessage());
                }
            }

        } else {
            this.userHelper = new ONPREMUserHelper(
                    (ONPREMConfig) this.config,
                    null,
                    new IdToken(data.getString("accessToken"), "base"),
                    data.getString("refreshToken"),
                    this);
            IdToken token = new IdToken(data.getString("accessToken"), "base");
            if ( token.validateAccessToken() )
                this.accessToken = token;
            else {
                try {
                    this.accessRefreshToken = this.userHelper.refreshAccessToken();
                } catch (Exception e) {
                    OsbdtLogger.e(TAG, "Cannot refresh user id token because + " +
                        e.getMessage());
                }
            }
        }
    }

    protected void updateUserDetailsInPreference (@NonNull Context context,
                                                  @NonNull String accessToken,
                                                  @NonNull String refreshToken) {

        try {
            JsonObject userDetails = this.auth.authHelper.getUserDetails(new GetTokenResult(new IdToken(accessToken,
                this.auth.getApp().getOptions().getAuthType())));
            JsonObject userData = Json.createObjectBuilder()
                .add("userEmail", userDetails.containsKey("emails") ?
                    userDetails.getJsonArray("emails").getJsonObject(0).getString("value") :
                    userDetails.getString("email"))
                .add("userDetails", userDetails)
                .add("authnToken", this.authnToken)
                .add("authnRefreshToken", this.refreshAuthnToken)
                .add("idcsAccessToken", this.idcsAccessToken)
                .add("idcsRefreshToken", this.idcsRefreshToken)
                .add("accessToken", accessToken)
                .add("refreshToken", refreshToken)
                .build();
            this.userHelper.saveUserDetails(context,
                userData, PREFERENCES_NAME);
        } catch (Exception e) {
            OsbdtLogger.e(TAG, "Cannot store updated user details. Following error occurred." + e.getMessage());
        }
    }

    protected abstract @NonNull String getRefreshToken();

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
            boolean refreshed = false;
            if (forceRefresh || !this.userHelper.validateAccessToken()) {
                this.userHelper.refreshAccessToken();
                refreshed = true;
            }
            GetTokenResult tokenResult = new GetTokenResult(this.accessToken);
            taskCompletionSource.setResult(tokenResult);
            if (refreshed) {
                this.auth.informIdTokenSubscribers();
            }
            } catch (OsbdtException e) {
                throw new RuntimeException(e);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;

    }

    @NonNull
    protected String getAccessToken() {
        return this.accessToken.getToken();
    }

    /**
     * Returns the provider ID of the user.
     *
     * @return provider ID
     */
    public abstract @NonNull String getProviderId();

    public abstract @NonNull OsbdtUserMetadata getMetadata();
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
            } catch (OsbdtException e) {
                throw new RuntimeException(e);
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
                this.accessRefreshToken = this.userHelper.refreshAccessToken();
                this.accessToken = this.userHelper.getIdTokenHelper(false);
                JsonObject userData = this.auth.authHelper.getAuthenticationDetails(email,
                    "",
                    Json.createObjectBuilder()
                        .add("access_token", this.accessToken.getToken())
                        .add("refresh_token", this.accessRefreshToken)
                        .add("authn_token", this.authnToken)
                        .add("authn_refresh_token", this.refreshAuthnToken)
                        .add("idcs_access_token", this.idcsAccessToken)
                        .add("idcs_refresh_token", this.idcsRefreshToken)
                        .build());
                this.auth.currentUser = new OsbdtUserImpl(userData,
                    this.auth,
                    "");
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
            } catch (OsbdtException e) {
                throw new RuntimeException(e);
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

        if (this.getEmail() == null) {
            OsbdtLogger.e("OsbdtUser", "Cannot send email for verification." +
                    " No email is provided.");
            throw new RuntimeException("Email is null. Cannot send email for verification.");
        }

        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                this.userHelper.sendEmailVerificationHelper(this.getEmail(), this.getUid());
            } catch (OsbdtException e) {
                throw new RuntimeException(e);
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
                taskCompletionSource.setResult(null);
            } catch (OsbdtException e) {
                // If reload fails, keep current values
                OsbdtLogger.e(TAG, "Reload failed, keeping current user data: " + e.getMessage());
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
     * Reauthenticates the user with the given credential.
     *
     * @param credential the credential to reauthenticate with
     * @return task representing the reauthentication operation
     */
    @NonNull
    public Task<Void> reauthenticate(@NonNull AuthCredential credential) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject res = this.auth.authHelper.signInWithCredentialHelper(credential, false);
                JsonObject userDetails = this.auth.authHelper.getUserDetails(
                    new GetTokenResult(
                        new IdToken(
                            res.getString("access_token"), this.auth.getApp().getOptions().getAuthType())));
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

                            case "google.com":
                                return new GoogleAuthCredential(
                                    credential.getProvider(),
                                    credential.getSignInMethod(),
                                    res.getString("accessToken")
                                );

                            case "github.com":
                                return new GithubAuthCredential(
                                    credential.getProvider(),
                                    credential.getSignInMethod(),
                                    res.getString("accessToken")
                                );

                            case "facebook.com":
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
                    public OsbdtUser getUser() {
                        return new OsbdtUserImpl(userData,
                            auth.authHelper.getOsbdtAuth(),
                            "");
                    }
                };

                OsbdtLogger.d("SocialAuth", "Stored user data from signInWithCredential");

                this.auth.authHelper.saveUserDetails(this.auth.getApp().getApplicationContext(),
                    userData, PREFERENCES_NAME);

                taskCompletionSource.setResult(null);
                this.auth.informAuthSubscribers();
                this.auth.informIdTokenSubscribers();

            } catch (Exception e) {
                OsbdtLogger.e(TAG, "Exception encountered while performing signInWithCredential operation");
                Exception exception = e;
                if(!(exception instanceof OsbdtOracledbException))
                    exception = new OsbdtNetworkException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage());
                throw new CompletionException(exception);
            }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Reauthenticates the user with the given credential, and returns the profile data for that account.
     *
     * @param credential the credential to reauthenticate with
     * @return task representing the reauthentication operation with profile data
     */
    @NonNull
    public Task<AuthResult> reauthenticateAndRetrieveData (@NonNull AuthCredential credential) {
        return this.auth.signInWithCredential(credential);
    }

    /**
     * Links the user using the mobile browser (either a Custom Chrome Tab or the device's default browser) to the given provider.
     *
     * @param activity the activity context
     * @param federatedAuthProvider the provider to link with
     * @return task representing the link operation
     */
    @NonNull
    public Task<AuthResult> startActivityForLinkWithProvider(
        @NonNull Activity activity,
        @NonNull FederatedAuthProvider federatedAuthProvider) {
        return this.auth.startActivityForSignInWithProvider(activity, federatedAuthProvider);
    }

    /**
     * Reauthenticates the user using the mobile browser (either a Custom Chrome Tab or the device's default browser) using the given provider.
     *
     * @param activity the activity context
     * @param federatedAuthProvider the provider to reauthenticate with
     * @return task representing the reauthentication operation
     */
    @NonNull
    public Task<AuthResult> startActivityForReauthenticateWithProvider(
        @NonNull Activity activity,
        @NonNull FederatedAuthProvider federatedAuthProvider) {
        return this.auth.startActivityForSignInWithProvider(activity, federatedAuthProvider);
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
