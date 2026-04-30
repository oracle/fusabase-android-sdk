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

import static com.oracle.mobile.fusabase.auth.FusabaseAuth.LOGGED_IN_USER_KEY;
import static com.oracle.mobile.fusabase.auth.FusabaseAuth.PREFERENCES_NAME;

import android.content.Context;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.utils.Utils;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

abstract class AuthHelper {

    protected final static String TAG = "FusabaseAuth";

    @NonNull
    abstract protected JsonObject registerUser (@NonNull String email,
                                                @NonNull String password)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject authenticateUser(@NonNull String email,
                                                            @NonNull String password)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject sendPasswordResetEmailHelper(@NonNull String email)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject performSignOut(@NonNull String refreshToken)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject verifyPasswordResetCodeHelper(@NonNull String code)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject confirmPasswordResetHelper (@NonNull String code,
                                                              @NonNull String newPassword)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject reloadUser (@NonNull FusabaseUser fusabaseUser)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject performCodeExchange (@NonNull String authCode, @NonNull String codeVerifier)
        throws FusabaseException;

    @NonNull
    abstract protected JsonObject signInWithCredentialHelper (@NonNull AuthCredential authCredential, boolean link)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject socialUnlinkHelper (@NonNull String provider)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject getUserDetails(@NonNull GetTokenResult accessToken)
            throws FusabaseException;

    @Nullable
    abstract protected JsonObject getFusabaseToken(@NonNull JsonObject authCredential, @NonNull JsonObject profileCredential)
            throws FusabaseException;

    @NonNull
    abstract protected FusabaseAuth getFusabaseAuth();

    @NonNull
    abstract protected Config getConfig();

    @NonNull
    abstract protected JsonObject getAuthenticationDetails (@NonNull String email,
                                                   @NonNull String password,
                                                   @NonNull JsonObject data)
            throws FusabaseException;


     protected JsonObject readUserDetails(@NonNull Context context,
                                      @NonNull String baseKey,
                                          @NonNull String prefsName) throws FusabaseException {

        try{
            return Utils.loadJsonObjectFromPreferences(context,
                    baseKey,
                    prefsName);
        } catch (Exception e)
        {
            FusabaseLogger.e("Cannot read saved user details because " + e.getMessage());
            throw new FusabaseException(e.getMessage());
        }
    }

    protected void clearUserDetails (@NonNull Context context,
            @NonNull String userId,
                                     @NonNull String prefsName) throws FusabaseException {
        try {
            Utils.deletePreferenceData(context,
                    LOGGED_IN_USER_KEY, PREFERENCES_NAME);
            Utils.clearDataWithBaseKey(context, userId, prefsName);
            FusabaseLogger.d(TAG, "Cleared user details successfully");
        } catch (Exception e)
        {
            FusabaseLogger.e("Cannot clear user details because " + e.getMessage());
            throw new FusabaseException("Cannot clear user details because " + e.getMessage());
        }
    }

    protected void completeSocialLoginWithOAuthProvider (@NonNull TaskCompletionSource<AuthResult> taskSource,
                                                         @NonNull AuthHelper authHelper,
                                                         @Nullable CompletableFuture<String> future) {
        String codeVerifier = "";
        String providerId = "";
        String data = "";
        boolean link = false;
        try {
            data = authHelper.readDataFromPreferences(authHelper
                            .getFusabaseAuth()
                            .getApp()
                            .getApplicationContext(),
                    "fusabase_CALLBACK_DATA");

            codeVerifier = authHelper.readDataFromPreferences(authHelper
                            .getFusabaseAuth()
                            .getApp()
                            .getApplicationContext(),
                    "fusabase_CODE_VERIFIER");

            providerId = authHelper.readDataFromPreferences(authHelper
                            .getFusabaseAuth()
                            .getApp()
                            .getApplicationContext(),
                    "fusabase_AUTH_PROVIDER_ID");
            link = Boolean.parseBoolean(authHelper.readDataFromPreferences(authHelper
                            .getFusabaseAuth()
                            .getApp()
                            .getApplicationContext(),
                    "fusabase_IS_LINK"));

        } catch (Exception e) {
            FusabaseLogger.d(TAG, "Unable to get callback data from the preferences " + e.getMessage());
        }

        if (data.isEmpty() || codeVerifier.isEmpty() || providerId.isEmpty()) {
            FusabaseLogger.d(TAG, "No earlier data for login is present.");
            taskSource.setResult(null);
            if(future != null)
                future.complete("");
            return;
        } else {
            FusabaseLogger.d(TAG, "Earlier login attempt data present. Completing login.");

            // Clean earlier login data
            try {
                authHelper.deleteDataFromPreferences(authHelper
                                .getFusabaseAuth()
                                .getApp()
                                .getApplicationContext(),
                        "fusabase_CODE_VERIFIER");
                authHelper.deleteDataFromPreferences(authHelper
                                .getFusabaseAuth()
                                .getApp()
                                .getApplicationContext(),
                        "fusabase_AUTH_PROVIDER_ID");
                authHelper.deleteDataFromPreferences(authHelper
                                .getFusabaseAuth()
                                .getApp()
                                .getApplicationContext(),
                        "fusabase_CALLBACK_DATA");
                authHelper.deleteDataFromPreferences(authHelper
                                .getFusabaseAuth()
                                .getApp()
                                .getApplicationContext(),
                        "fusabase_IS_LINK");
            } catch (Exception e) {
                FusabaseLogger.d(TAG, "Unable to clear earlier login data in the preferences " + e.getMessage());
            }
        }

        boolean isIdcs = authHelper.getConfig().getAuthType().equals("idcs");
        String callbackScheme = "baasmobile" + authHelper.getConfig().getAppId().toLowerCase();

        if (data.toString().startsWith(callbackScheme)) {

            String response = URLDecoder.decode(data.toString());
            String authCode = response.substring(response.indexOf("code=") + "code=".length());

            // authCode is sensitive; never log it.
            FusabaseLogger.d(TAG, "Received authCode (redacted)");

            JsonObject responseObject;
            JsonObject userDetails;
            JsonObject tokensForUser;
            JsonObject credentialResponse = Json.createObjectBuilder().build();
            try {
                responseObject = authHelper.performCodeExchange(authCode, codeVerifier);

                tokensForUser = responseObject;

                if (isIdcs) {
                    // Exchange IDCS tokens for FUSABASE tokens (same as createUserWithEmailAndPassword)
                    tokensForUser = authHelper.getFusabaseToken(responseObject, responseObject);
                    // Link not supported for IDCS
                } else if (link) {
                    AuthCredential authCredential = getAuthCredential(providerId, responseObject);
                    tokensForUser = authHelper.signInWithCredentialHelper(authCredential, link);
                }

                userDetails = authHelper.getUserDetails(
                        new GetTokenResult(
                                new IdToken(
                                        isIdcs ? responseObject.getString("access_token") : tokensForUser.getString("access_token"),
                                        "password")));
            } catch (FusabaseException e) {
                FusabaseLogger.e("Cannot perform code exchange or error parsing the token."+ e.getMessage());
                throw new CompletionException(e);
            }

            JsonObjectBuilder userDataBuilder = Json.createObjectBuilder()
                    .add("userEmail", userDetails.getString("email"))
                    .add("userDetails", userDetails);

            if (isIdcs) {
                userDataBuilder
                    .add("authnToken", tokensForUser.getString("authn_token"))
                    .add("accessToken", tokensForUser.getString("access_token"))
                    .add("refreshToken", tokensForUser.getString("refresh_token"))
                    .add("authnRefreshToken", tokensForUser.getString("authn_refresh_token"))
                    .add("idcsAccessToken", responseObject.getString("access_token"))
                    .add("idcsRefreshToken", responseObject.getString("refresh_token"))
                    .add("idcsAuthnToken", responseObject.getString("id_token"));
            } else {
                userDataBuilder
                    .add("authnToken", JsonValue.NULL)
                    .add("accessToken", tokensForUser.getString("access_token"))
                    .add("refreshToken", tokensForUser.getString("refresh_token"));
            }

            final JsonObject userData = userDataBuilder.build();

            String finalProviderId = providerId;

            AuthResult result = new AuthResult() {
                @Nullable
                @Override
                public AuthCredential getCredential() {
                    return new OAuthCredentialImpl(finalProviderId,
                            userData.getString("access_token"),
                            null,
                            "");
                }

                @Nullable
                @Override
                public FusabaseUser getUser() {
                    return new FusabaseUserImpl(userData,
                            authHelper.getFusabaseAuth(),
                            "");
                }
            };

            // No need to set current User as the values are same
            if(!link) {
                authHelper.getFusabaseAuth().currentUser = result.getUser();

                try {
                    authHelper.saveUserDetails(authHelper
                                    .getFusabaseAuth()
                                    .getApp()
                                    .getApplicationContext(),
                            userData, PREFERENCES_NAME);
                    FusabaseLogger.d(TAG, "Stored user data in preferences");
                } catch (Exception e) {
                    FusabaseLogger.d(TAG, "Unable to store user data in the preferences");
                }
            }

            taskSource.setResult(result);
            authHelper.getFusabaseAuth().informAuthSubscribers();
            authHelper.getFusabaseAuth().informIdTokenSubscribers();

            if(future != null)
                future.complete("");
        }
    }
    @NonNull
    private static AuthCredential getAuthCredential(String providerId, JsonObject responseObject) {
        final String providerName = providerId;
        AuthCredential authCredential = new AuthCredential() {
            @NonNull
            @Override
            public String getProvider() {
                return providerName;
            }

            @NonNull
            @Override
            public String getSignInMethod() {
                return providerName;
            }

            @NonNull
            @Override
            protected String getIdToken() {
                return responseObject.getString("id_token");
            }

        };
        return authCredential;
    }

    protected void saveUserDetails (@NonNull Context context,
                                    @NonNull JsonObject userDetails,
                                    @NonNull String prefsName) throws FusabaseException {

        String userId = userDetails.getJsonObject("userDetails").getString("id");

        try {
            this.saveDataToPreferences(context, LOGGED_IN_USER_KEY, userId);
            Utils.saveJsonObjectToPrefs(context, userId, userDetails, prefsName);
            FusabaseLogger.d(TAG, "Stored User detail successfully.");
        } catch (Exception e) {
            FusabaseLogger.e("Cannot store user data " + e.getMessage());
            throw new FusabaseException ( "Cannot store user data");
        }
    }

    protected void saveDataToPreferences(@NonNull Context context,
                                         @NonNull String key,
                                         @NonNull String value)
            throws FusabaseException
    {
        try {
            Utils.savePreferenceData(context,
                    value,
                    key,
                    PREFERENCES_NAME);

        } catch (GeneralSecurityException | IOException e) {
            FusabaseLogger.w(TAG, "Unable to store user data");
            throw new FusabaseException("Cannot store user data");
        }
    }

    protected void deleteDataFromPreferences(@NonNull Context context,
                                             @NonNull String key)
            throws FusabaseException
    {
        try {
            Utils.deletePreferenceData(context,
                    key,
                    PREFERENCES_NAME);

        } catch (GeneralSecurityException | IOException e) {
            FusabaseLogger.e(TAG, "Cannot clear user data because " + e.getMessage());
            throw new FusabaseException("Cannot clear user data because " + e.getMessage());
        }
    }

    protected String readDataFromPreferences(@NonNull Context context,
                                             @NonNull String key)
            throws FusabaseException
    {
        try {
            return Utils.getPreferenceData(context,
                    key,
                    PREFERENCES_NAME);

        } catch (GeneralSecurityException | IOException e) {
            FusabaseLogger.e(TAG, "Cannot read user data because " + e.getMessage());
            throw new FusabaseException("Cannot read user data because " + e.getMessage());
        }
    }

    protected boolean doesKeyExistsInPreference(@NonNull Context context,
                                                @NonNull String key) {

        boolean exists = false;
        try {
            exists =  Utils.getPreferenceData(context,
                    key,
                    PREFERENCES_NAME) != null;

        } catch (GeneralSecurityException | IOException e) {
            FusabaseLogger.w(TAG, "Unable to read user data because " + e.getMessage());
        }

        return exists;
    }

}
