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

import static com.oracle.database.osbdt.auth.OsbdtAuth.LOGGED_IN_USER_KEY;
import static com.oracle.database.osbdt.auth.OsbdtAuth.PREFERENCES_NAME;

import android.content.Context;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.database.osbdt.OsbdtException;
import com.oracle.database.osbdt.logger.OsbdtLogger;
import com.oracle.database.osbdt.utils.Utils;
import com.oracle.database.osbdt.task.TaskCompletionSource;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

abstract class AuthHelper {

    protected final static String TAG = "OsbdtAuth";

    @NonNull
    abstract protected JsonObject registerUser (@NonNull String email,
                                                @NonNull String password)
            throws OsbdtException;

    @NonNull
    abstract protected JsonObject authenticateUser(@NonNull String email,
                                                            @NonNull String password)
            throws OsbdtException;

    @NonNull
    abstract protected JsonObject sendPasswordResetEmailHelper(@NonNull String email)
            throws OsbdtException;

    @NonNull
    abstract protected JsonObject performSignOut(@NonNull String refreshToken)
            throws OsbdtException;

    @NonNull
    abstract protected JsonObject verifyPasswordResetCodeHelper(@NonNull String code)
            throws OsbdtException;

    @NonNull
    abstract protected JsonObject confirmPasswordResetHelper (@NonNull String code,
                                                              @NonNull String newPassword)
            throws OsbdtException;

    @NonNull
    abstract protected JsonObject reloadUser (@NonNull OsbdtUser osbdtUser)
            throws OsbdtException;

    @NonNull
    abstract protected JsonObject performCodeExchange (@NonNull String authCode, @NonNull String codeVerifier)
        throws OsbdtException;

    @NonNull
    abstract protected JsonObject signInWithCredentialHelper (@NonNull AuthCredential authCredential, boolean link)
            throws OsbdtException;

    @NonNull
    abstract protected JsonObject socialUnlinkHelper (@NonNull String provider)
            throws OsbdtException;

    @NonNull
    abstract protected JsonObject getUserDetails(@NonNull GetTokenResult accessToken)
            throws OsbdtException;

    @NonNull
    abstract protected OsbdtAuth getOsbdtAuth();

    @NonNull
    abstract protected Config getConfig();

    @NonNull
    abstract protected JsonObject getAuthenticationDetails (@NonNull String email,
                                                   @NonNull String password,
                                                   @NonNull JsonObject data)
            throws OsbdtException;


     protected JsonObject readUserDetails(@NonNull Context context,
                                      @NonNull String baseKey,
                                          @NonNull String prefsName) throws OsbdtException {

        try{
            return Utils.loadJsonObjectFromPreferences(context,
                    baseKey,
                    prefsName);
        } catch (Exception e)
        {
            OsbdtLogger.e("Cannot read saved user details because " + e.getMessage());
            throw new OsbdtException(e.getMessage());
        }
    }

    protected void clearUserDetails (@NonNull Context context,
            @NonNull String userId,
                                     @NonNull String prefsName) throws OsbdtException {
        try {
            Utils.deletePreferenceData(context,
                    LOGGED_IN_USER_KEY, PREFERENCES_NAME);
            Utils.clearDataWithBaseKey(context, userId, prefsName);
            OsbdtLogger.d(TAG, "Cleared user details successfully");
        } catch (Exception e)
        {
            OsbdtLogger.e("Cannot clear user details because " + e.getMessage());
            throw new OsbdtException("Cannot clear user details because " + e.getMessage());
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
                            .getOsbdtAuth()
                            .getApp()
                            .getApplicationContext(),
                    "osbdt_CALLBACK_DATA");

            codeVerifier = authHelper.readDataFromPreferences(authHelper
                            .getOsbdtAuth()
                            .getApp()
                            .getApplicationContext(),
                    "osbdt_CODE_VERIFIER");

            providerId = authHelper.readDataFromPreferences(authHelper
                            .getOsbdtAuth()
                            .getApp()
                            .getApplicationContext(),
                    "osbdt_AUTH_PROVIDER_ID");
            link = Boolean.parseBoolean(authHelper.readDataFromPreferences(authHelper
                            .getOsbdtAuth()
                            .getApp()
                            .getApplicationContext(),
                    "osbdt_IS_LINK"));

        } catch (Exception e) {
            OsbdtLogger.d(TAG, "Unable to get callback data from the preferences " + e.getMessage());
        }

        if (data.isEmpty() || codeVerifier.isEmpty() || providerId.isEmpty()) {
            OsbdtLogger.d(TAG, "No earlier data for login is present.");
            taskSource.setResult(null);
            if(future != null)
                future.complete("");
            return;
        } else {
            OsbdtLogger.d(TAG, "Earlier login attempt data present. Completing login.");

            // Clean earlier login data
            try {
                authHelper.deleteDataFromPreferences(authHelper
                                .getOsbdtAuth()
                                .getApp()
                                .getApplicationContext(),
                        "osbdt_CODE_VERIFIER");
                authHelper.deleteDataFromPreferences(authHelper
                                .getOsbdtAuth()
                                .getApp()
                                .getApplicationContext(),
                        "osbdt_AUTH_PROVIDER_ID");
                authHelper.deleteDataFromPreferences(authHelper
                                .getOsbdtAuth()
                                .getApp()
                                .getApplicationContext(),
                        "osbdt_CALLBACK_DATA");
                authHelper.deleteDataFromPreferences(authHelper
                                .getOsbdtAuth()
                                .getApp()
                                .getApplicationContext(),
                        "osbdt_IS_LINK");
            } catch (Exception e) {
                OsbdtLogger.d(TAG, "Unable to clear earlier login data in the preferences " + e.getMessage());
            }
        }

        String callbackScheme = "baasmobile" + authHelper.getConfig().getAppId().toLowerCase();

        if (data.toString().startsWith(callbackScheme)) {

            String response = URLDecoder.decode(data.toString());
            String authCode = response.substring(response.indexOf("code=") + "code=".length());

            OsbdtLogger.d(TAG, "Received authCode " + authCode);

            JsonObject responseObject;
            JsonObject userDetails;
            JsonObject credentialResponse = Json.createObjectBuilder().build();
            try {
                responseObject = authHelper.performCodeExchange(authCode, codeVerifier);
                if(link)
                {
                    AuthCredential authCredential = getAuthCredential(providerId, responseObject);
                    responseObject = authHelper.signInWithCredentialHelper(authCredential, link);
                }

                userDetails = authHelper.getUserDetails(
                        new GetTokenResult(
                                new IdToken(
                                        responseObject.getString("access_token"), "base")));
            } catch (OsbdtException e) {
                OsbdtLogger.e("Cannot perform code exchange or error parsing the token."+ e.getMessage());
                throw new CompletionException(e);
            }

            final JsonObject userData = Json.createObjectBuilder()
                    .add("userEmail", userDetails.getString("email"))
                    .add("userDetails", userDetails)
                    .add("authnToken", JsonValue.NULL)
                    .add("accessToken", responseObject.getString("access_token"))
                    .add("refreshToken", responseObject.getString("refresh_token"))
                    .build();

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
                public OsbdtUser getUser() {
                    return new OsbdtUserImpl(userData,
                            authHelper.getOsbdtAuth(),
                            "");
                }
            };

            // No need to set current User as the values are same
            if(!link) {
                authHelper.getOsbdtAuth().currentUser = result.getUser();

                try {
                    authHelper.saveUserDetails(authHelper
                                    .getOsbdtAuth()
                                    .getApp()
                                    .getApplicationContext(),
                            userData, PREFERENCES_NAME);
                    OsbdtLogger.d(TAG, "Stored user data in preferences");
                } catch (Exception e) {
                    OsbdtLogger.d(TAG, "Unable to store user data in the preferences");
                }
            }

            taskSource.setResult(result);
            authHelper.getOsbdtAuth().informAuthSubscribers();
            authHelper.getOsbdtAuth().informIdTokenSubscribers();

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

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(@NonNull Parcel parcel, int i) {
            }
        };
        return authCredential;
    }

    protected void saveUserDetails (@NonNull Context context,
                                    @NonNull JsonObject userDetails,
                                    @NonNull String prefsName) throws OsbdtException {

        String userId = userDetails.getJsonObject("userDetails").getString("id");

        try {
            this.saveDataToPreferences(context, LOGGED_IN_USER_KEY, userId);
            Utils.saveJsonObjectToPrefs(context, userId, userDetails, prefsName);
            OsbdtLogger.d(TAG, "Stored User detail successfully.");
        } catch (Exception e) {
            OsbdtLogger.e("Cannot store user data " + e.getMessage());
            throw new OsbdtException ( "Cannot store user data");
        }
    }

    protected void saveDataToPreferences(@NonNull Context context,
                                         @NonNull String key,
                                         @NonNull String value)
            throws OsbdtException
    {
        try {
            Utils.savePreferenceData(context,
                    value,
                    key,
                    PREFERENCES_NAME);

        } catch (GeneralSecurityException | IOException e) {
            OsbdtLogger.w(TAG, "Unable to store user data");
            throw new OsbdtException("Cannot store user data");
        }
    }

    protected void deleteDataFromPreferences(@NonNull Context context,
                                             @NonNull String key)
            throws OsbdtException
    {
        try {
            Utils.deletePreferenceData(context,
                    key,
                    PREFERENCES_NAME);

        } catch (GeneralSecurityException | IOException e) {
            OsbdtLogger.e(TAG, "Cannot clear user data because " + e.getMessage());
            throw new OsbdtException("Cannot clear user data because " + e.getMessage());
        }
    }

    protected String readDataFromPreferences(@NonNull Context context,
                                             @NonNull String key)
            throws OsbdtException
    {
        try {
            return Utils.getPreferenceData(context,
                    key,
                    PREFERENCES_NAME);

        } catch (GeneralSecurityException | IOException e) {
            OsbdtLogger.e(TAG, "Cannot read user data because " + e.getMessage());
            throw new OsbdtException("Cannot read user data because " + e.getMessage());
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
            OsbdtLogger.w(TAG, "Unable to read user data because " + e.getMessage());
        }

        return exists;
    }

}
