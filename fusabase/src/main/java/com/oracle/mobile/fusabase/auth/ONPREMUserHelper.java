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

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.FusabaseNetworkException;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.utils.Utils;

import java.io.StringReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

class ONPREMUserHelper extends UserHelper {

    protected final static String TAG = "FusabaseAuth";

    protected ONPREMConfig config;
    protected FusabaseUser user;

    protected ONPREMUserHelper(@NonNull ONPREMConfig config, @NonNull FusabaseUser user) {
        super(user);
        this.config = config;
        this.user = user;
    }

    @NonNull
    protected IdToken getIdTokenHelper (@NonNull boolean forceRefresh) throws FusabaseException {
        if (forceRefresh || !this.validateAccessToken()) {
            refreshAccessToken();
        }
        JsonObject data = user.loadUserData();
        String tokenStr = data.containsKey("accessToken") ? data.getString("accessToken") : "";
        return new IdToken(tokenStr, "base");
    }

    @NonNull
    @Override
    protected JsonObject unlinkHelper(FusabaseUser fusabaseUser, String provider) {
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    @NonNull
    @Override
    protected JsonObject reauthenticateAndRetrieveDataHelper(FusabaseUser fusabaseUser, AuthCredential credential) {
        return JsonValue.EMPTY_JSON_OBJECT;
    }


    @NonNull
    protected String refreshAccessToken () throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request for refreshing token to fusabase");

        JsonObject userData = user.loadUserData();
        String refreshToken = userData.containsKey("refreshToken") ? userData.getString("refreshToken") : "";
        if (refreshToken.isEmpty()) {
            throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(), "Authentication session expired. Please sign in again.");
        }

        JsonObject payload = Json.createObjectBuilder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());


        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

            requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                            ONPREMConfig.UNDERSCORE_PATH,
                            ONPREMConfig.BAAS_SERVICES_PATH,
                            ONPREMConfig.IDM_PATH,
                            ONPREMConfig.ON_PREM_PATH,
                            this.config.getProjectId(),
                            ONPREMConfig.AUTHENTICATE_REST_EP),
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
            FusabaseLogger.i(TAG, "Access Token refreshed for user "
                    + user.getDisplayName() + " successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();

            String newAccessToken = result.getString("access_token");
            String newRefreshToken = result.getString("refresh_token");

            JsonObject updatedData = Json.createObjectBuilder(userData)
                .add("accessToken", newAccessToken)
                .add("refreshToken", newRefreshToken)
                .build();

            // Preserve all existing user data while updating tokens
            JsonObject preservedData = Json.createObjectBuilder()
                .add("userEmail", userData.containsKey("userEmail") ? userData.getString("userEmail") : "")
                .add("userDetails", userData.get("userDetails"))
                .add("authnToken", userData.containsKey("authnToken") ? userData.get("authnToken") : JsonValue.NULL)
                .add("accessToken", newAccessToken)
                .add("refreshToken", newRefreshToken)
                .build();

            saveUserDetails(user.auth.getApp().getApplicationContext(), preservedData, FusabaseAuth.PREFERENCES_NAME);

            return newAccessToken;
        }

        FusabaseLogger.e(TAG, "Failed to refresh access token for user from " +
                " Fusabase with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(), "Failed to refresh authentication session. Please sign in again.");
    }

    protected boolean validateAccessToken() {
        try {
            JsonObject data = user.loadUserData();
            String tokenStr = data.containsKey("accessToken") ? data.getString("accessToken") : "";
            if (tokenStr.isEmpty()) return false;
            IdToken token = new IdToken(tokenStr, "base");
            return token.expirationTime >= Instant.now().toEpochMilli() / 1000 + 60 * 5;
        } catch (Exception e) {
            return false;
        }
    }

    @NonNull
    protected JsonArray makeOperations(@NonNull UserProfileChangeRequest userProfile) {

        JsonArrayBuilder operationBuilder = Json.createArrayBuilder();

        if (userProfile.getDisplayName() != null
                && !Objects.equals(userProfile.getDisplayName(), this.user.getDisplayName())) {
            operationBuilder
                    .add(Json.createObjectBuilder()
                            .add("op", this.user.getDisplayName() != null ? "replace" : "add")
                            .add("path", "displayName")
                            .add("value", userProfile.getDisplayName())
                            .build());
        }

        return operationBuilder.build();
    }

    @NonNull
    protected JsonObject updateProfile (@NonNull UserProfileChangeRequest profileChangeRequest)
    throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request for updating profile to fusabase");

        JsonObject payload = Json.createObjectBuilder()
                .add("Operations", makeOperations(profileChangeRequest))
                .build();
        IdToken accessToken = this.getIdTokenHelper(true);
        // Error if no op is available

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());


        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/scim+json");
        headers.put("X-AUTHZ", accessToken.getToken());

            requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                            ONPREMConfig.UNDERSCORE_PATH,
                            ONPREMConfig.BAAS_SERVICES_PATH,
                            ONPREMConfig.IDM_PATH,
                            ONPREMConfig.ON_PREM_PATH,
                            this.config.getProjectId(),
                            ONPREMConfig.UPDATE_PROFILE_HELPER),
                    "PUT",
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
            FusabaseLogger.i(TAG, "Profile updated for user " + user.getDisplayName() + " successfully.");
            JsonObjectBuilder responseBuilder = Json.createObjectBuilder();

        if(profileChangeRequest.getDisplayName() != null)
            responseBuilder.add("displayName", profileChangeRequest.getDisplayName());
        if(profileChangeRequest.getPhoneNumber() != null)
            responseBuilder.add("phoneNumber", profileChangeRequest.getPhoneNumber());
        if(profileChangeRequest.getPhotoUri() != null)
            responseBuilder.add("photoUrl", profileChangeRequest.getPhotoUri());
        return responseBuilder.build();
        }

        FusabaseLogger.e(TAG, "Failed to update profile of user from" +
                " Fusabase with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(), "Failed to update user profile. Please try again later.");

    }

    @NonNull
    protected JsonObject updatePasswordHelper (@NonNull FusabaseUser user,
                                               @NonNull String newPassword,
                                               @NonNull String oldPassword)
    throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request for updating password to fusabase");

        if(oldPassword.isEmpty())
        {
            FusabaseLogger.e("Current user not recently logged in.");
            throw new FusabaseAuthRecentLoginRequiredException(
                    FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "User hasn't recently logged in");
        }

        JsonObject payload = Json.createObjectBuilder()
                .add("password", newPassword)
                .add("oldPassword", oldPassword)
                .build();
        IdToken accessToken = this.getIdTokenHelper(false);

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());


        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-AUTHZ", accessToken.getToken());

            requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                            ONPREMConfig.UNDERSCORE_PATH,
                            ONPREMConfig.BAAS_SERVICES_PATH,
                            ONPREMConfig.IDM_PATH,
                            ONPREMConfig.ON_PREM_PATH,
                            this.config.getProjectId(),
                            ONPREMConfig.UPDATE_PASSWORD_HELPER),
                    "PUT",
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
            FusabaseLogger.i(TAG, "Password updated for user " + user.getDisplayName() + " successfully.");
            return Json.createObjectBuilder().add("success", 1).build();
        }

        FusabaseLogger.e(TAG, "Failed to update password for user from" +
                " Fusabase with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(), "Failed to update password. Please check your current password and try again.");
    }

    protected void sendEmailVerificationHelper (@NonNull String email,
                                                      @NonNull String id)
    throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request for email verification to fusabase");
        IdToken accessToken = this.getIdTokenHelper(false);

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());
        queryParameters.put("email", email);
        queryParameters.put("requesttype", "verifyemail");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", String.format("Bearer %s", accessToken.getToken()));

            requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                            ONPREMConfig.UNDERSCORE_PATH,
                            ONPREMConfig.BAAS_SERVICES_PATH,
                            ONPREMConfig.IDM_PATH,
                            ONPREMConfig.ON_PREM_PATH,
                            this.config.getProjectId(),
                            ONPREMConfig.SEND_EMAIL_VERIFICATION),
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
            FusabaseLogger.i(TAG, "Email sent for verification at " + email + " successfully.");
            return;
        }

        FusabaseLogger.e(TAG, "Failed to send email for verification from" +
                " Fusabase with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseAuthEmailException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                "Unable to send email verification. Please check the email address and try again.");
    }

}
