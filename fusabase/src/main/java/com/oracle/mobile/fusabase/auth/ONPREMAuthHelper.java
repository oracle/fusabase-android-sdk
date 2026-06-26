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

            FusabaseUser currentUser = this.auth.getCurrentUser();
            if (currentUser == null) {

                FusabaseLogger.d(TAG, "User must be recently logged in for linking provider.");
                throw new FusabaseAuthException(FusabaseAuthException.Code.UNAUTHENTICATED.toString(),
                    "User must be recently logged in for linking with provider.");
            }

            if (authCredential instanceof EmailAuthCredential) {
                validateEmailCredentialForLinking((EmailAuthCredential) authCredential, currentUser.getEmail());
            }

            String authHeader = currentUser.userHelper.getAuthorizationHeader();
            if (!authHeader.isEmpty()) {
                headers.put("Authorization", authHeader);
            }
        }
        
        String password = "";
        if (link && authCredential instanceof EmailAuthCredential) {
            EmailAuthCredential emailAuthCredential = (EmailAuthCredential) authCredential;
            password = emailAuthCredential.getPassword();
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

    static void validateEmailCredentialForLinking(@NonNull EmailAuthCredential credential,
                                                  @Nullable String currentUserEmail)
            throws FusabaseAuthException {
        String credentialEmail = credential.getEmail();
        if (credentialEmail == null || credentialEmail.trim().isEmpty()) {
            throw new FusabaseAuthException(
                FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                "Email must be provided in EmailAuthCredential for linking.");
        }

        if (currentUserEmail == null || currentUserEmail.trim().isEmpty()) {
            throw new FusabaseAuthException(
                FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                "A user must be logged in for credential linking");
        }

        if (!credentialEmail.trim().equalsIgnoreCase(currentUserEmail.trim())) {
            FusabaseLogger.d(TAG, "EmailAuthCredential email does not match the current user email.");
            throw new FusabaseAuthInvalidCredentialsException(
                FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                "EmailAuthCredential email must match the current user email for linking.");
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
                (provider.equals("epw") && !this.auth.getCurrentUser().hasPassword())) {

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

}
