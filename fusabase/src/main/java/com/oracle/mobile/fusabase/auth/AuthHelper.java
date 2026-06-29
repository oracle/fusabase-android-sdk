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
import com.oracle.mobile.fusabase.FusabaseNetworkException;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.utils.Utils;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

abstract class AuthHelper {

    protected final static String TAG = "FusabaseAuth";

    @NonNull
    protected JsonObject registerUser(@NonNull String email,
                                      @NonNull String password)
            throws FusabaseException
    {
        JsonObject payload = Json.createObjectBuilder()
                .add("first_name", "-")
                .add("last_name", "-")
                .add("email", email)
                .add("password", password)
                .build();

        JsonObject result = executeAuthJsonRequest(
                "Registering user through ORDS",
                Config.SELF_REGISTER_EP,
                "POST",
                jsonHeaders(),
                apiKeyQueryParameters(),
                payload.toString());

        return normalizeRegistrationResponse(email, password, result);
    }

    @NonNull
    protected JsonObject authenticateUser(@NonNull String email,
                                          @NonNull String password)
            throws FusabaseException
    {
        JsonObject payload = Json.createObjectBuilder()
                .add("grant_type", "user_credentials")
                .add("username", email)
                .add("password", password)
                .build();

        JsonObject result = executeAuthJsonRequest(
                "Authenticating user through ORDS",
                Config.AUTHENTICATE_REST_EP,
                "POST",
                jsonHeaders(),
                apiKeyQueryParameters(),
                payload.toString());

        return normalizeAuthTokenResponse(result);
    }

    @NonNull
    protected JsonObject sendPasswordResetEmailHelper(@NonNull String email)
            throws FusabaseException
    {
        Map<String, String> queryParameters = apiKeyQueryParameters();
        queryParameters.put("email", email);
        queryParameters.put("requesttype", "resetpwd");

        executeAuthJsonRequest(
                "Sending password reset email through ORDS",
                Config.SEND_PASSWORD_RESET_EMAIL,
                "GET",
                jsonHeaders(),
                queryParameters,
                null);
        return Json.createObjectBuilder().add("success", 1).build();
    }

    @NonNull
    protected JsonObject performSignOut(@NonNull String refreshToken)
            throws FusabaseException
    {
        JsonObject result = this.revokeRefreshToken(refreshToken);
        if (result.containsKey("success")) {
            FusabaseLogger.d(TAG, "User signedOut successfully.");
        } else {
            FusabaseLogger.e(TAG, "User cannot be signed Out.");
        }
        return result;
    }

    @NonNull
    protected JsonObject verifyPasswordResetCodeHelper(@NonNull String code)
            throws FusabaseException
    {
        Map<String, String> queryParameters = apiKeyQueryParameters();
        queryParameters.put("code", code);
        queryParameters.put("requesttype", "resetpwd");

        return executeAuthJsonRequest(
                "Verifying password reset code through ORDS",
                Config.VERIFY_PASSWORD_RESET_CODE,
                "GET",
                jsonHeaders(),
                queryParameters,
                null);
    }

    @NonNull
    protected JsonObject confirmPasswordResetHelper(@NonNull String code,
                                                    @NonNull String newPassword)
            throws FusabaseException
    {
        String storedEmail = this.readDataFromPreferences(
                this.getFusabaseAuth().getApp().getApplicationContext(), "password_reset_email");
        if (storedEmail == null || storedEmail.isEmpty()) {
            throw new FusabaseException("No password reset request found. Please request a password reset first.");
        }

        JsonObject payload = Json.createObjectBuilder()
                .add("password", newPassword)
                .add("code", code)
                .build();

        Map<String, String> queryParameters = apiKeyQueryParameters();
        queryParameters.put("email", storedEmail);

        return executeAuthJsonRequest(
                "Confirming password reset through ORDS",
                Config.CONFIRM_PASSWORD_RESET,
                "POST",
                jsonHeaders(),
                queryParameters,
                payload.toString());
    }

    @NonNull
    protected JsonObject reloadUser(@NonNull FusabaseUser fusabaseUser)
            throws FusabaseException
    {
        GetTokenResult accessToken = new GetTokenResult(fusabaseUser.userHelper.getIdTokenHelper(true));
        return this.getUserDetails(accessToken);
    }

    @NonNull
    protected JsonObject performCodeExchange(@NonNull String authCode, @NonNull String codeVerifier)
        throws FusabaseException
    {
        JsonObject payload = Json.createObjectBuilder()
                .add("code", authCode)
                .add("code_verifier", codeVerifier)
                .build();

        return executeAuthJsonRequest(
                "Exchanging redirect code through ORDS",
                Config.GET_REDIRECT_RESULT,
                "POST",
                new HashMap<>(),
                apiKeyQueryParameters(),
                payload.toString());
    }

    @NonNull
    abstract protected JsonObject signInWithCredentialHelper (@NonNull AuthCredential authCredential, boolean link)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject socialUnlinkHelper (@NonNull String provider)
            throws FusabaseException;

    @NonNull
    abstract protected JsonObject getUserDetails(@NonNull GetTokenResult accessToken)
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

    @NonNull
    protected JsonObject normalizeAuthTokenResponse(@NonNull JsonObject result) {
        return Json.createObjectBuilder()
                .add("access_token", result.getString("access_token"))
                .add("refresh_token", result.getString("refresh_token"))
                .build();
    }

    @NonNull
    protected JsonObject normalizeRegistrationResponse(@NonNull String email,
                                                       @NonNull String password,
                                                       @NonNull JsonObject result)
            throws FusabaseException {
        return normalizeAuthTokenResponse(result);
    }

    @NonNull
    protected JsonObject revokeRefreshToken(@NonNull String refreshToken)
            throws FusabaseException {
        JsonObject payload = Json.createObjectBuilder()
                .add("token", refreshToken)
                .build();

        executeAuthJsonRequest(
                "Revoking refresh token through ORDS",
                Config.REVOKE_REFRESH_TOKEN,
                "PUT",
                new HashMap<>(),
                apiKeyQueryParameters(),
                payload.toString());
        return Json.createObjectBuilder().add("success", 1).build();
    }

    @NonNull
    protected JsonObject executeAuthJsonRequest(@NonNull String logAction,
                                                @NonNull String endpoint,
                                                @NonNull String method,
                                                @NonNull Map<String, String> headers,
                                                @Nullable Map<String, String> queryParameters,
                                                @Nullable String payload)
            throws FusabaseException {
        String url = this.getConfig().getAuthBaseEndpoint(endpoint);
        return executeAuthJsonRequestToUrl(logAction, url, endpoint, method, headers, queryParameters, payload);
    }

    @NonNull
    protected JsonObject executeAuthJsonRequestToUrl(@NonNull String logAction,
                                                     @NonNull String url,
                                                     @Nullable String endpoint,
                                                     @NonNull String method,
                                                     @NonNull Map<String, String> headers,
                                                     @Nullable Map<String, String> queryParameters,
                                                     @Nullable String payload)
            throws FusabaseException {
        FusabaseLogger.d(TAG, logAction);

        HttpRequestHelper requestHelper = new HttpRequestHelper();
        if (queryParameters == null) {
            if (payload == null) {
                requestHelper.createHttpRequest(url, method, headers);
            } else {
                requestHelper.createHttpRequest(url, method, headers, payload);
            }
        } else if (payload == null) {
            requestHelper.createHttpRequest(url, method, headers, queryParameters);
        } else {
            requestHelper.createHttpRequest(url, method, headers, queryParameters, payload);
        }

        HttpResponse response;
        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            if (response.getResponse() == null || response.getResponse().isEmpty()) {
                return Json.createObjectBuilder().add("success", 1).build();
            }
            try (JsonReader reader = Json.createReader(new StringReader(response.getResponse()))) {
                return reader.readObject();
            }
        }

        FusabaseLogger.e(TAG, logAction + " failed with response code "
                + response.getCode() + " with the following error " + response.getError());
        throw buildAuthRequestException(endpoint, response);
    }

    @NonNull
    protected JsonObject executeAuthStatusRequestToUrl(@NonNull String logAction,
                                                       @NonNull String url,
                                                       @Nullable String endpoint,
                                                       @NonNull String method,
                                                       @NonNull Map<String, String> headers,
                                                       @Nullable Map<String, String> queryParameters,
                                                       @Nullable String payload)
            throws FusabaseException {
        FusabaseLogger.d(TAG, logAction);

        HttpRequestHelper requestHelper = new HttpRequestHelper();
        if (queryParameters == null) {
            if (payload == null) {
                requestHelper.createHttpRequest(url, method, headers);
            } else {
                requestHelper.createHttpRequest(url, method, headers, payload);
            }
        } else if (payload == null) {
            requestHelper.createHttpRequest(url, method, headers, queryParameters);
        } else {
            requestHelper.createHttpRequest(url, method, headers, queryParameters, payload);
        }

        HttpResponse response;
        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            return Json.createObjectBuilder().add("success", 1).build();
        }

        FusabaseLogger.e(TAG, logAction + " failed with response code "
                + response.getCode() + " with the following error " + response.getError());
        throw buildAuthRequestException(endpoint, response);
    }

    @NonNull
    protected FusabaseException buildAuthRequestException(@Nullable String endpoint,
                                                         @NonNull HttpResponse response) {
        if (response.getCode() == 401 || response.getCode() == 404 || response.getCode() == 403) {
            return new FusabaseAuthInvalidCredentialsException(
                    FusabaseAuthException.Code.fromCode(response.getCode()).toString(),
                    "Authentication failed. Please check your credentials and try again.");
        }
        if (Config.SELF_REGISTER_EP.equals(endpoint) && response.getCode() == 400) {
            return new FusabaseAuthWeakPasswordException(
                    FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "The password provided does not meet security requirements. Please choose a stronger password.");
        }
        if (Config.SEND_PASSWORD_RESET_EMAIL.equals(endpoint)) {
            return new FusabaseAuthEmailException(
                    FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "Unable to send password reset email. Please check the email address and try again.");
        }
        if (Config.VERIFY_PASSWORD_RESET_CODE.equals(endpoint)) {
            return new FusabaseException("Unable to verify password reset code. Please check the code and try again.");
        }
        if (Config.CONFIRM_PASSWORD_RESET.equals(endpoint)) {
            return new FusabaseException("Unable to reset password. Please check your reset code and try again.");
        }
        if (Config.REVOKE_REFRESH_TOKEN.equals(endpoint)) {
            return new FusabaseException("Sign out failed. Please try again later.");
        }

        String error = response.getError();
        return new FusabaseException(error == null || error.isEmpty()
                ? "Authentication request failed. Please try again later."
                : error);
    }

    @NonNull
    protected Map<String, String> apiKeyQueryParameters() {
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.getConfig().getAppId());
        return queryParameters;
    }

    @NonNull
    protected Map<String, String> jsonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }


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

                if (!isIdcs && link) {
                    AuthCredential authCredential = getAuthCredential(providerId, responseObject);
                    tokensForUser = authHelper.signInWithCredentialHelper(authCredential, link);
                }

                userDetails = authHelper.getUserDetails(
                        new GetTokenResult(
                                new IdToken(
                                        tokensForUser.getString("access_token"),
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
                    .add("accessToken", tokensForUser.getString("access_token"))
                    .add("refreshToken", tokensForUser.getString("refresh_token", ""));
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
                            userData.getString("accessToken"),
                            null,
                            "");
                }

                @Nullable
                @Override
                public FusabaseUser getUser() {
                    return new FusabaseUserImpl(userData,
                            authHelper.getFusabaseAuth(),
                            null);
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
