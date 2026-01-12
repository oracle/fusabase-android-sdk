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

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.OsbdtException;
import com.oracle.database.osbdt.OsbdtNetworkException;
import com.oracle.database.osbdt.http.HttpRequestHelper;
import com.oracle.database.osbdt.http.HttpResponse;
import com.oracle.database.osbdt.logger.OsbdtLogger;
import com.oracle.database.osbdt.utils.Utils;

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

    protected final static String TAG = "OsbdtAuth";

    protected ONPREMConfig config;
    protected IdToken authToken;
    protected IdToken accessToken;
    protected OsbdtUser user;
    protected String refreshToken;

    protected ONPREMUserHelper(@NonNull ONPREMConfig config, IdToken authToken, @NonNull IdToken accessToken, @NonNull String refreshToken, @NonNull OsbdtUser user) {
        super();
        this.config = config;
        this.authToken = authToken;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    @NonNull
    protected IdToken getIdTokenHelper (@NonNull boolean forceRefresh) throws OsbdtException {
        if (forceRefresh || !this.validateAccessToken()) {
            this.refreshAccessToken();
        }
        return this.accessToken;
    }

    @NonNull
    @Override
    protected JsonObject unlinkHelper(OsbdtUser osbdtUser, String provider) {
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    @NonNull
    @Override
    protected JsonObject reauthenticateAndRetrieveDataHelper(OsbdtUser osbdtUser, AuthCredential credential) {
        return JsonValue.EMPTY_JSON_OBJECT;
    }


    @NonNull
    protected String refreshAccessToken () throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request for refreshing token to osbdt");

        JsonObject payload = Json.createObjectBuilder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", this.refreshToken)
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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i(TAG, "Access Token refreshed for user "
                    + user.getDisplayName() + " successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();

            this.accessToken = new IdToken(result.getString("access_token"), null);
            this.refreshToken = result.getString("refresh_token");

            this.user.accessToken = this.accessToken;

            this.user.updateUserDetailsInPreference(this.user.auth.getApp().getApplicationContext(),
                accessToken.getToken(),
                refreshToken);

            return this.refreshToken;
        }

        OsbdtLogger.e(TAG, "Failed to refresh access token for user from " +
                " Osbdt with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    protected boolean validateAccessToken() {
        return this.accessToken.expirationTime >= Instant.now().toEpochMilli() / 1000 + 60 * 5;
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
    throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request for updating profile to osbdt");

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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i(TAG, "Profile updated for user " + user.getDisplayName() + " successfully.");
            JsonObjectBuilder responseBuilder = Json.createObjectBuilder();

        if(profileChangeRequest.getDisplayName() != null)
            responseBuilder.add("displayName", profileChangeRequest.getDisplayName());
        if(profileChangeRequest.getPhoneNumber() != null)
            responseBuilder.add("phoneNumber", profileChangeRequest.getPhoneNumber());
        if(profileChangeRequest.getPhotoUri() != null)
            responseBuilder.add("photoUrl", profileChangeRequest.getPhotoUri());
        return responseBuilder.build();
        }

        OsbdtLogger.e(TAG, "Failed to update profile of user from" +
                " Osbdt with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());

    }

    @NonNull
    protected JsonObject updatePasswordHelper (@NonNull OsbdtUser user,
                                               @NonNull String newPassword,
                                               @NonNull String oldPassword)
    throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request for updating password to osbdt");

        if(oldPassword.isEmpty())
        {
            OsbdtLogger.e("Current user not recently logged in.");
            throw new OsbdtAuthRecentLoginRequiredException(
                    OsbdtAuthException.Code.INVALID_ARGUMENT.toString(),
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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i(TAG, "Password updated for user " + user.getDisplayName() + " successfully.");
            return Json.createObjectBuilder().add("success", 1).build();
        }

        OsbdtLogger.e(TAG, "Failed to update password for user from" +
                " Osbdt with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    protected void sendEmailVerificationHelper (@NonNull String email,
                                                      @NonNull String id)
    throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request for email verification to osbdt");
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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }


        if (response.getStatus()) {
            OsbdtLogger.i(TAG, "Email sent for verification at " + email + " successfully.");
            return;
        }

        OsbdtLogger.e(TAG, "Failed to send email for verification from" +
                " Osbdt with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtAuthEmailException(OsbdtAuthException.Code.INVALID_ARGUMENT.toString(),
                response.getError());
    }

}
