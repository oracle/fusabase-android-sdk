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

import static com.oracle.mobile.fusabase.task.Tasks.await;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.FusabaseNetworkException;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.utils.Utils;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

class ONPREMAuthHelper extends AuthHelper {

    protected ONPREMConfig config;
    protected FusabaseAuth auth;


    ONPREMAuthHelper(@NonNull ONPREMConfig config, @NonNull FusabaseAuth auth) {
        this.config = config;
        this.auth = auth;
    }

    @Nullable
    protected JsonObject getFusabaseToken(@NonNull JsonObject authCredential, @NonNull JsonObject profileCredential) throws FusabaseException {
        return null; // On-prem doesn't need token exchange
    }

    @NonNull
    protected ONPREMConfig getConfig() {
        return this.config;
    }

    @NonNull
    protected FusabaseAuth getFusabaseAuth() {
        return this.auth;
    }

    @NonNull
    protected JsonObject getAuthenticationDetails (@NonNull String email,
            @NonNull String password,
            @NonNull JsonObject tokenResult) {

        String accessToken = tokenResult.getString("access_token");
        String refreshToken = tokenResult.getString("refresh_token");
        JsonObject userDetails = this.getUserDetails(new GetTokenResult(new IdToken(accessToken, "base")));

        FusabaseLogger.i(TAG, "Fetched details for user " +
                userDetails.getString("displayName") + '.');

        return Json.createObjectBuilder()
                .add("userEmail", email)
                .add("userDetails", userDetails)
                .add("authnToken", JsonValue.NULL)
                .add("accessToken", accessToken)
                .add("refreshToken", refreshToken)
                .build();
    }

    @NonNull
    protected JsonObject authenticateUser(@NonNull String email,
                                          @NonNull String password)
            throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to authenticate user to fusabase");
        JsonObject payload = Json.createObjectBuilder()
                .add("grant_type", "user_credentials")
                .add("username", email)
                .add("password", password)
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                        ONPREMConfig.UNDERSCORE_PATH,
                        ONPREMConfig.BAAS_SERVICES_PATH,
                        ONPREMConfig.IDM_PATH,
                        ONPREMConfig.ON_PREM_PATH,
                        this.config.getProjectId(),
                        ONPREMConfig.AUTHENTICATE_REST_EP),
                "POST",
                new HashMap<>(),
                queryParameters,
                payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.i(TAG, "Authenticated User " + email + " successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return Json.createObjectBuilder()
                    .add("access_token", result.getString("access_token"))
                    .add("refresh_token", result.getString("refresh_token"))
                    .build();
        }

        FusabaseLogger.e(TAG, "Cannot authenticate user " + email + " . Response code "
                + response.getCode() + " with the following error " +
                response.getError());

        if(response.getCode() == 401 || response.getCode() == 404 || response.getCode() == 403)
            throw new FusabaseAuthInvalidCredentialsException(
                    FusabaseAuthException.Code.fromCode(response.getCode()).toString()
                    , "Authentication failed. Please check your credentials and try again.");
        throw new FusabaseException("Authentication failed. Please try again later.");
    }

    @NonNull
    public JsonObject reloadUser(@NonNull FusabaseUser user) throws FusabaseException {
        GetTokenResult accessToken = new GetTokenResult(user.userHelper.getIdTokenHelper(true));
        return this.getUserDetails(accessToken);
    }

    @NonNull
    protected JsonObject performCodeExchange (@NonNull String authCode, @NonNull String codeVerifier) throws FusabaseException {
        FusabaseLogger.d(TAG, "Sending request to exchange authCode and codeVerifier to get access " +
                "to fusabase");

        JsonObject payload = Json.createObjectBuilder()
                .add("code", authCode)
                .add("code_verifier", codeVerifier)
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());


        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                        ONPREMConfig.UNDERSCORE_PATH,
                        ONPREMConfig.BAAS_SERVICES_PATH,
                        ONPREMConfig.IDM_PATH,
                        ONPREMConfig.ON_PREM_PATH,
                        this.config.getProjectId(),
                        ONPREMConfig.GET_REDIRECT_RESULT),
                "POST",
                new HashMap<>(),
                queryParameters,
                payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.i(TAG, "Successfully exchanged codes with Fusabase backend.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        FusabaseLogger.e(TAG, "Cannot exchange code with Fusabase Backend"
               + " . Response code "
                + response.getCode() + " with the following error " +
                response.getError());

        if(response.getCode() == 401 || response.getCode() == 404 || response.getCode() == 403)
            throw new FusabaseAuthInvalidCredentialsException(
                    FusabaseAuthException.Code.fromCode(response.getCode()).toString()
                    , "Authentication failed. Please check your credentials and try again.");
        throw new FusabaseException("Code exchange failed. Please try again later.");
    }

    @NonNull
    protected JsonObject signInWithCredentialHelper(@NonNull AuthCredential authCredential,
                                                    boolean link) throws FusabaseException {
        FusabaseLogger.d(TAG, "Sending request to authenticate user with the provided credential " +
                "to fusabase");

        String providerId = authCredential.getProvider();
        Map<String, String> headers = new HashMap<>();

        if(link)
        {
            if(authCredential instanceof SAMLAuthCredential ||
                    authCredential instanceof OAuthCredential ||
                    authCredential instanceof IDCSAuthCredential) {
                FusabaseLogger.d(TAG, "Unsupported credentials provided for linking.");
                throw new FusabaseAuthException(FusabaseAuthException.Code.NOT_IMPLEMENTED.toString(),
                        "Unsupported credentials provided for linking.");
            }

            if (this.auth.getCurrentUser() == null) {

                FusabaseLogger.d(TAG, "User must be recently logged in for linking provider.");
                throw new FusabaseAuthException(FusabaseAuthException.Code.UNAUTHENTICATED.toString(),
                    "User must be recently logged in for linking with provider.");
            }

            String authHeader = this.auth.getCurrentUser().userHelper.getAuthorizationHeader();
            if (!authHeader.isEmpty()) {
                headers.put("Authorization", authHeader);
            }
        }
        
        String password = "";
        if (link && authCredential instanceof EmailAuthCredential) {
            password = ((EmailAuthCredential) authCredential).getPassword();
            if (password == null || password.isEmpty()) {
                throw new FusabaseAuthException(
                    FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "Password must be provided in EmailAuthCredential for linking.");
            }
        }

        JsonObject payload = Json.createObjectBuilder()
                .add("token", authCredential.getIdToken())
                .add("password", password)
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());
        queryParameters.put("method", providerId.split("\\.")[0]);
        queryParameters.put("link", String.valueOf(link ? 1 : 0));

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                        ONPREMConfig.UNDERSCORE_PATH,
                        ONPREMConfig.BAAS_SERVICES_PATH,
                        ONPREMConfig.IDM_PATH,
                        ONPREMConfig.ON_PREM_PATH,
                        this.config.getProjectId(),
                        ONPREMConfig.SIGN_IN_WITH_CREDENTIAL),
                "POST",
                headers,
                queryParameters,
                payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
            if (response.getStatus()) {
                FusabaseLogger.i(TAG, "Signed In with credential " + authCredential.getProvider() + " successfully.");
                JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
                JsonObject result = reader.readObject();
                reader.close();
                return Json.createObjectBuilder()
                    .add("access_token", link ? auth.getCurrentUser().getAccessToken(): result.getString("access_token"))
                    .add("refresh_token", link ? auth.getCurrentUser().getRefreshToken(): result.getString("refresh_token"))
                    .build();
            }
            FusabaseLogger.e(TAG, "Cannot authenticate user with credential with providerId "
                + authCredential.getProvider() + " . Response code "
                + response.getCode() + " with the following error " +
                response.getError());

        if(response.getCode() == 401 || response.getCode() == 404 || response.getCode() == 403)
            throw new FusabaseAuthInvalidCredentialsException(
                    FusabaseAuthException.Code.fromCode(response.getCode()).toString()
                    , "Authentication failed. Please check your credentials and try again.");
        throw new FusabaseException("Sign in failed. Please try again later.");
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }
    }

    @NonNull
    protected JsonObject socialUnlinkHelper(@NonNull String provider) throws FusabaseException {
        FusabaseLogger.d(TAG, "Sending request to unlink user with the provided credential " +
                "to fusabase");

        Map<String, String> headers = new HashMap<>();

        if (provider.startsWith("saml_") ||
            provider.startsWith("oidc_")) {
            FusabaseLogger.d(TAG, "Unsupported credentials provided for linking.");
            throw new FusabaseAuthException(FusabaseAuthException.Code.NOT_IMPLEMENTED.toString(),
                    "Unsupported credentials provided for linking.");
        }

        if (this.auth.getCurrentUser() == null ||
                (provider.equals("epw") && this.auth.getCurrentUser().password == null)) {

            FusabaseLogger.d(TAG, "User must be recently logged in for linking provider.");
            throw new FusabaseAuthException(FusabaseAuthException.Code.UNAUTHENTICATED.toString(),
                    "User must be recently logged in for linking with provider.");
        }

        String authHeader = this.auth.getCurrentUser().userHelper.getAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());
        queryParameters.put("method", provider.split("\\.")[0]);

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                        ONPREMConfig.UNDERSCORE_PATH,
                        ONPREMConfig.BAAS_SERVICES_PATH,
                        ONPREMConfig.IDM_PATH,
                        ONPREMConfig.ON_PREM_PATH,
                        this.config.getProjectId(),
                        ONPREMConfig.SIGN_IN_WITH_CREDENTIAL),
                "DELETE",
                headers,
                queryParameters);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
            if (response.getStatus()) {
                FusabaseLogger.i(TAG, "Unlink  with provider " + provider + " successfully.");
                JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
                JsonObject result = reader.readObject();
                reader.close();
                return Json.createObjectBuilder()
                        .add("access_token", auth.getCurrentUser().getAccessToken())
                        .add("refresh_token",  auth.getCurrentUser().getRefreshToken())
                        .build();
            }
            FusabaseLogger.e(TAG, "Cannot unlink user with providerId "
                    + provider + " . Response code "
                    + response.getCode() + " with the following error " +
                    response.getError());

            if(response.getCode() == 401 || response.getCode() == 404 || response.getCode() == 403)
                throw new FusabaseAuthInvalidCredentialsException(
                        FusabaseAuthException.Code.fromCode(response.getCode()).toString()
                        , "Authentication failed. Please check your credentials and try again.");
            throw new FusabaseException("Unlink operation failed. Please try again later.");
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }
    }


    public static String convertIatToTimestamp(long timestamp) {
        // Multiply by 1000 to convert seconds to milliseconds
        Date date = new Date(timestamp * 1000L);

        // Set up the date format for UTC timezone
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'000000'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Use UTC timezone

        // Format the date into the desired format
        return dateFormat.format(date);
    }

    @NonNull
    public JsonObject getUserDetails(@NonNull GetTokenResult accessToken) {

        JsonObject data = (JsonObject) Utils.parseJWT(accessToken.getToken());

        return Json.createObjectBuilder()
                .add("displayName", data.get("user_displayname"))
//                .add("phoneNumber", data.get("phone_number"))
                .add("email", data.getString("sub"))
                .add("lastSignIn", convertIatToTimestamp(data.getInt("iat")))
                .add("id", data.getString("user_id"))
                .add("creation_time", data.getString("creation_time"))
                .add("email_verified", data.getString("email_verified"))
                .add("idp_name", data.getString("idp_name"))
                .add("idp_type", data.getString("idp_type"))
                .build();

    }

    @NonNull
    public JsonObject registerUser(@NonNull String email,
                                   @NonNull String password)
            throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to register user to fusabase " + email);

        JsonObject payload = Json.createObjectBuilder()
                .add("first_name", "-")
                .add("last_name", "-")
                .add("email", email)
                .add("password", password)
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());
        FusabaseLogger.d("Creating Network Request to register user");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                        ONPREMConfig.UNDERSCORE_PATH,
                        ONPREMConfig.BAAS_SERVICES_PATH,
                        ONPREMConfig.IDM_PATH,
                        ONPREMConfig.ON_PREM_PATH,
                        this.config.getProjectId(),
                        ONPREMConfig.SELF_REGISTER_EP),
                "POST",
                new HashMap<>(),
                queryParameters,
                payload.toString());

        HttpResponse response = null;

        try {
            FusabaseLogger.d("Sending Network Request to register user");
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.i("FusabaseAuth", "Registered User " + email + " successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return Json.createObjectBuilder()
                    .add("access_token", result.getString("access_token"))
                    .add("refresh_token", result.getString("refresh_token"))
                    .build();
        }

        if(response.getCode() == 400)
        {
            throw new FusabaseAuthWeakPasswordException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(), "The password provided does not meet security requirements. Please choose a stronger password.");
        }

        FusabaseLogger.e(TAG, "Failed to register user at" +
                " fusabase with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseException("User registration failed. Please try again later.");
    }

    @NonNull
    public JsonObject performSignOut(@NonNull String refresh_token)
            throws FusabaseException {
        JsonObject result = this.revokeRefreshToken(refresh_token);
             if(result.containsKey("success"))
                 FusabaseLogger.d("FusabaseAuth", "User signedOut successfully.");
             else
                 FusabaseLogger.e("FusabaseAuth", "User cannot be signed Out.");
        return result;
    }

    @NonNull
    public JsonObject revokeRefreshToken (@NonNull String refresh_token)
            throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to sign out user from fusabase");
        JsonObject payload = Json.createObjectBuilder()
                .add("token", refresh_token)
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                        ONPREMConfig.UNDERSCORE_PATH,
                        ONPREMConfig.BAAS_SERVICES_PATH,
                        ONPREMConfig.IDM_PATH,
                        ONPREMConfig.ON_PREM_PATH,
                        this.config.getProjectId(),
                        ONPREMConfig.REVOKE_REFRESH_TOKEN),
                "PUT",
                new HashMap<>(),
                queryParameters,
                payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.i("FusabaseAuth", "Refresh Token revoked successfully.");
            return Json.createObjectBuilder().add("success", 1).build();
        }

        FusabaseLogger.e(TAG, "Failed to sign out user from" +
                " Fusabase with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseException("Sign out failed. Please try again later.");
    }

    @NonNull
    public JsonObject sendPasswordResetEmailHelper(@NonNull String email)
            throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to send password reset email from fusabase");
        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());
        queryParameters.put("email", email);
        queryParameters.put("requesttype", "resetpwd");


        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                        ONPREMConfig.UNDERSCORE_PATH,
                        ONPREMConfig.BAAS_SERVICES_PATH,
                        ONPREMConfig.IDM_PATH,
                        ONPREMConfig.ON_PREM_PATH,
                        this.config.getProjectId(),
                        ONPREMConfig.SEND_PASSWORD_RESET_EMAIL),
                "GET",
                headers,
                queryParameters);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.i("FusabaseAuth", "Password reset email request sent successfully.");
            return Json.createObjectBuilder().add("success", 1).build();
        }

        FusabaseLogger.e(TAG, "Failed to send password reset email request from" +
                " fusabase with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseAuthEmailException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                "Unable to send password reset email. Please check the email address and try again.");
    }

    @NonNull
    public JsonObject verifyPasswordResetCodeHelper(@NonNull String code)
            throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to verify password reset code from fusabase");

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());
        queryParameters.put("code", code);
        queryParameters.put("requesttype", "resetpwd");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                        ONPREMConfig.UNDERSCORE_PATH,
                        ONPREMConfig.BAAS_SERVICES_PATH,
                        ONPREMConfig.IDM_PATH,
                        ONPREMConfig.ON_PREM_PATH,
                        this.config.getProjectId(),
                        ONPREMConfig.VERIFY_PASSWORD_RESET_CODE),
                "GET",
                headers,
                queryParameters);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.i("FusabaseAuth", "Verify password reset request sent successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        FusabaseLogger.e(TAG, "Failed to send verify password reset request " +
                " at Fusabase with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseException("Unable to verify password reset code. Please check the code and try again.");
    }

    @NonNull
    public JsonObject confirmPasswordResetHelper(@NonNull String code,
                                                 @NonNull String newPassword)
            throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to verify password reset code from fusabase");

        String storedEmail = this.readDataFromPreferences(
                this.auth.getApp().getApplicationContext(), "password_reset_email");
        if (storedEmail == null || storedEmail.isEmpty()) {
            throw new FusabaseException("No password reset request found. Please request a password reset first.");
        }

        JsonObject payload = Json.createObjectBuilder()
                .add("password", newPassword)
                .add("code", code)
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());
        queryParameters.put("email", storedEmail);


        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                        ONPREMConfig.UNDERSCORE_PATH,
                        ONPREMConfig.BAAS_SERVICES_PATH,
                        ONPREMConfig.IDM_PATH,
                        ONPREMConfig.ON_PREM_PATH,
                        this.config.getProjectId(),
                        ONPREMConfig.CONFIRM_PASSWORD_RESET),
                "POST",
                headers,
                queryParameters,
                payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.i("FusabaseAuth", "Confirm password reset email request sent successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;

        }

        FusabaseLogger.e(TAG, "Failed to Confirm password reset from" +
                " Fusabase with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseException("Unable to reset password. Please check your reset code and try again.");
    }
}
