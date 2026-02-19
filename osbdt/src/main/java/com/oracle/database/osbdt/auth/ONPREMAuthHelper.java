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

import static com.oracle.database.osbdt.task.Tasks.await;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.database.osbdt.OsbdtException;
import com.oracle.database.osbdt.logger.OsbdtLogger;
import com.oracle.database.osbdt.OsbdtNetworkException;
import com.oracle.database.osbdt.http.HttpRequestHelper;
import com.oracle.database.osbdt.http.HttpResponse;
import com.oracle.database.osbdt.utils.Utils;

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
    protected OsbdtAuth auth;


    ONPREMAuthHelper(@NonNull ONPREMConfig config, @NonNull OsbdtAuth auth) {
        this.config = config;
        this.auth = auth;
    }

    @Nullable
    protected JsonObject getOsbdtToken(@NonNull JsonObject authCredential, @NonNull JsonObject profileCredential) throws OsbdtException {
        return null; // On-prem doesn't need token exchange
    }

    @NonNull
    protected ONPREMConfig getConfig() {
        return this.config;
    }

    @NonNull
    protected OsbdtAuth getOsbdtAuth() {
        return this.auth;
    }

    @NonNull
    protected JsonObject getAuthenticationDetails (@NonNull String email,
            @NonNull String password,
            @NonNull JsonObject tokenResult) {

        String accessToken = tokenResult.getString("access_token");
        String refreshToken = tokenResult.getString("refresh_token");
        JsonObject userDetails = this.getUserDetails(new GetTokenResult(new IdToken(accessToken, "base")));

        OsbdtLogger.i(TAG, "Fetched details for user " +
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
            throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to authenticate user to osbdt");
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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i(TAG, "Authenticated User " + email + " successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return Json.createObjectBuilder()
                    .add("access_token", result.getString("access_token"))
                    .add("refresh_token", result.getString("refresh_token"))
                    .build();
        }

        OsbdtLogger.e(TAG, "Cannot authenticate user " + email + " . Response code "
                + response.getCode() + " with the following error " +
                response.getError());

        if(response.getCode() == 404 || response.getCode() == 403)
            throw new OsbdtAuthInvalidCredentialsException(
                    OsbdtAuthException.Code.fromCode(response.getCode()).toString()
                    , response.getError());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    public JsonObject reloadUser(@NonNull OsbdtUser user) throws OsbdtException {
        GetTokenResult accessToken = new GetTokenResult(user.userHelper.getIdTokenHelper(true));
        return this.getUserDetails(accessToken);
    }

    @NonNull
    protected JsonObject performCodeExchange (@NonNull String authCode, @NonNull String codeVerifier) throws OsbdtException {
        OsbdtLogger.d(TAG, "Sending request to exchange authCode and codeVerifier to get access " +
                "to osbdt");

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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i(TAG, "Successfully exchanged codes with Osbdt backend.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        OsbdtLogger.e(TAG, "Cannot exchange code with Osbdt Backend"
               + " . Response code "
                + response.getCode() + " with the following error " +
                response.getError());

        if(response.getCode() == 404 || response.getCode() == 403)
            throw new OsbdtAuthInvalidCredentialsException(
                    OsbdtAuthException.Code.fromCode(response.getCode()).toString()
                    , response.getError());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    protected JsonObject signInWithCredentialHelper(@NonNull AuthCredential authCredential,
                                                    boolean link) throws OsbdtException {
        OsbdtLogger.d(TAG, "Sending request to authenticate user with the provided credential " +
                "to osbdt");

        String providerId = authCredential.getProvider();
        Map<String, String> headers = new HashMap<>();

        if(link)
        {
            if(authCredential instanceof SAMLAuthCredential ||
                    authCredential instanceof OAuthCredential ||
                    authCredential instanceof IDCSAuthCredential) {
                OsbdtLogger.d(TAG, "Unsupported credentials provided for linking.");
                throw new OsbdtAuthException(OsbdtAuthException.Code.NOT_IMPLEMENTED.toString(),
                        "Unsupported credentials provided for linking.");
            }

            if (this.auth.getCurrentUser() == null) {

                OsbdtLogger.d(TAG, "User must be recently logged in for linking provider.");
                throw new OsbdtAuthException(OsbdtAuthException.Code.UNAUTHENTICATED.toString(),
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
                throw new OsbdtAuthException(
                    OsbdtAuthException.Code.INVALID_ARGUMENT.toString(),
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
                OsbdtLogger.i(TAG, "Signed In with credential " + authCredential.getProvider() + " successfully.");
                JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
                JsonObject result = reader.readObject();
                reader.close();
                return Json.createObjectBuilder()
                    .add("access_token", link ? auth.getCurrentUser().getAccessToken(): result.getString("access_token"))
                    .add("refresh_token", link ? auth.getCurrentUser().getRefreshToken(): result.getString("refresh_token"))
                    .build();
            }
            OsbdtLogger.e(TAG, "Cannot authenticate user with credential with providerId "
                + authCredential.getProvider() + " . Response code "
                + response.getCode() + " with the following error " +
                response.getError());

            if(response.getCode() == 404 || response.getCode() == 403)
                throw new OsbdtAuthInvalidCredentialsException(
                    OsbdtAuthException.Code.fromCode(response.getCode()).toString()
                    , response.getError());
            throw new OsbdtException(response.getError());
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }
    }

    @NonNull
    protected JsonObject socialUnlinkHelper(@NonNull String provider) throws OsbdtException {
        OsbdtLogger.d(TAG, "Sending request to unlink user with the provided credential " +
                "to osbdt");

        Map<String, String> headers = new HashMap<>();

        if (provider.startsWith("saml_") ||
            provider.startsWith("oidc_")) {
            OsbdtLogger.d(TAG, "Unsupported credentials provided for linking.");
            throw new OsbdtAuthException(OsbdtAuthException.Code.NOT_IMPLEMENTED.toString(),
                    "Unsupported credentials provided for linking.");
        }

        if (this.auth.getCurrentUser() == null ||
                (provider.equals("epw") && this.auth.getCurrentUser().password == null)) {

            OsbdtLogger.d(TAG, "User must be recently logged in for linking provider.");
            throw new OsbdtAuthException(OsbdtAuthException.Code.UNAUTHENTICATED.toString(),
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
                OsbdtLogger.i(TAG, "Unlink  with provider " + provider + " successfully.");
                JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
                JsonObject result = reader.readObject();
                reader.close();
                return Json.createObjectBuilder()
                        .add("access_token", auth.getCurrentUser().getAccessToken())
                        .add("refresh_token",  auth.getCurrentUser().getRefreshToken())
                        .build();
            }
            OsbdtLogger.e(TAG, "Cannot unlink user with providerId "
                    + provider + " . Response code "
                    + response.getCode() + " with the following error " +
                    response.getError());

            if(response.getCode() == 404 || response.getCode() == 403)
                throw new OsbdtAuthInvalidCredentialsException(
                        OsbdtAuthException.Code.fromCode(response.getCode()).toString()
                        , response.getError());
            throw new OsbdtException(response.getError());
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
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
            throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to register user to osbdt " + email);

        JsonObject payload = Json.createObjectBuilder()
                .add("first_name", "-")
                .add("last_name", "-")
                .add("email", email)
                .add("password", password)
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());
        OsbdtLogger.d("Creating Network Request to register user");

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
            OsbdtLogger.d("Sending Network Request to register user");
            response = requestHelper.executeRequest();
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i("OsbdtAuth", "Registered User " + email + " successfully.");
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
            throw new OsbdtAuthWeakPasswordException("Weak Password", response.getError());
        }
        
        OsbdtLogger.e(TAG, "Failed to register user at" +
                " osbdt with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    public JsonObject performSignOut(@NonNull String refresh_token)
            throws OsbdtException {
        JsonObject result = this.revokeRefreshToken(refresh_token);
             if(result.containsKey("success"))
                 OsbdtLogger.d("OsbdtAuth", "User signedOut successfully.");
             else
                 OsbdtLogger.e("OsbdtAuth", "User cannot be signed Out.");
        return result;
    }

    @NonNull
    public JsonObject revokeRefreshToken (@NonNull String refresh_token)
            throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to sign out user from osbdt");
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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i("OsbdtAuth", "Refresh Token revoked successfully.");
            return Json.createObjectBuilder().add("success", 1).build();
        }

        OsbdtLogger.e(TAG, "Failed to sign out user from" +
                " Osbdt with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    public JsonObject sendPasswordResetEmailHelper(@NonNull String email)
            throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to send password reset email from osbdt");
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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i("OsbdtAuth", "Password reset email request sent successfully.");
            return Json.createObjectBuilder().add("success", 1).build();
        }

        OsbdtLogger.e(TAG, "Failed to send password reset email request from" +
                " osbdt with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtAuthEmailException(OsbdtAuthException.Code.INVALID_ARGUMENT.toString(),
                response.getError());
    }

    @NonNull
    public JsonObject verifyPasswordResetCodeHelper(@NonNull String code)
            throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to verify password reset code from osbdt");

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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i("OsbdtAuth", "Verify password reset request sent successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        OsbdtLogger.e(TAG, "Failed to send verify password reset request " +
                " at Osbdt with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    public JsonObject confirmPasswordResetHelper(@NonNull String code,
                                                 @NonNull String newPassword)
            throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to verify password reset code from osbdt");

        String storedEmail = this.readDataFromPreferences(
                this.auth.getApp().getApplicationContext(), "password_reset_email");
        if (storedEmail == null || storedEmail.isEmpty()) {
            throw new OsbdtException("No password reset request found. Please request a password reset first.");
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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i("OsbdtAuth", "Confirm password reset email request sent successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;

        }

        OsbdtLogger.e(TAG, "Failed to Confirm password reset from" +
                " Osbdt with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }
}
