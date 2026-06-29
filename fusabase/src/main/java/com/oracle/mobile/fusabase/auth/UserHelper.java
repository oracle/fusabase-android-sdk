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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.FusabaseNetworkException;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.utils.Utils;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

abstract class UserHelper {
    private final String TAG = "FusabaseAuth";
    protected final FusabaseUser user;

    protected UserHelper(@NonNull FusabaseUser user) {
        this.user = user;
    }

    protected  void saveUserDetails (@NonNull Context context,
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

    @NonNull
    protected boolean validateAccessToken() {
        try {
            JsonObject data = user.loadUserData();
            String tokenStr = data.containsKey("accessToken") ? data.getString("accessToken") : "";
            if (tokenStr.isEmpty()) return false;
            IdToken token = new IdToken(tokenStr, user.config.getAuthType());
            return token.expirationTime >= Instant.now().toEpochMilli() / 1000 + 60 * 5;
        } catch (Exception e) {
            return false;
        }
    }

    @NonNull
    protected abstract void sendEmailVerificationHelper(@NonNull String email,
                                                              @NonNull String id)
            throws FusabaseException;

    @NonNull
    protected JsonObject updateProfile(@NonNull UserProfileChangeRequest userProfile)
            throws FusabaseException {
        JsonArray operations = makeOperations(userProfile);
        if (operations.isEmpty()) {
            return Json.createObjectBuilder().build();
        }

        JsonObject payload = Json.createObjectBuilder()
                .add("Operations", operations)
                .build();

        IdToken accessToken = this.getIdTokenHelper(true);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/scim+json");
        headers.put("X-AUTHZ", accessToken.getToken());

        executeUserJsonRequest(
                "Updating user profile through ORDS",
                Config.UPDATE_PROFILE_HELPER,
                "PUT",
                headers,
                apiKeyQueryParameters(),
                payload.toString());

        return profileUpdateResponse(userProfile);
    }

    @NonNull
    protected JsonObject updatePasswordHelper(@NonNull FusabaseUser user,
                                              @NonNull String newPassword,
                                              @Nullable String currentPassword)
            throws FusabaseException {
        if (currentPassword == null || currentPassword.isEmpty()) {
            FusabaseLogger.e(TAG, "Current password is required to update password.");
            throw new FusabaseAuthRecentLoginRequiredException(
                    FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "User must reauthenticate before updating password");
        }

        JsonObject payload = Json.createObjectBuilder()
                .add("password", newPassword)
                .add("oldPassword", currentPassword)
                .build();
        IdToken accessToken = this.getIdTokenHelper(false);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-AUTHZ", accessToken.getToken());

        executeUserJsonRequest(
                "Updating user password through ORDS",
                Config.UPDATE_PASSWORD_HELPER,
                "PUT",
                headers,
                apiKeyQueryParameters(),
                payload.toString());

        return Json.createObjectBuilder().add("success", 1).build();
    }

    protected String refreshAccessToken()
            throws FusabaseException {
        FusabaseLogger.d(TAG, "Sending request for refreshing token to fusabase");

        JsonObject userData = user.loadUserData();
        String refreshToken = userData.containsKey("refreshToken") ? userData.getString("refreshToken") : "";
        if (refreshToken.isEmpty()) {
            throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "Authentication session expired. Please sign in again.");
        }

        JsonObject payload = Json.createObjectBuilder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();

        JsonObject result = executeUserJsonRequest(
                "Refreshing user token through ORDS",
                Config.AUTHENTICATE_REST_EP,
                "POST",
                jsonHeaders(),
                apiKeyQueryParameters(),
                payload.toString());

        JsonObject updatedData = mergeRefreshResult(userData, result);
        saveUserDetails(user.auth.getApp().getApplicationContext(), updatedData, FusabaseAuth.PREFERENCES_NAME);
        return updatedData.getString("accessToken");
    }

    @NonNull
    protected IdToken getIdTokenHelper(@NonNull boolean forceRefresh)
            throws FusabaseException {
        if (forceRefresh || !this.validateAccessToken()) {
            refreshAccessToken();
        }
        JsonObject data = user.loadUserData();
        String tokenStr = data.containsKey("accessToken") ? data.getString("accessToken") : "";
        return new IdToken(tokenStr, user.config.getAuthType());
    }

    @NonNull
    protected abstract JsonObject unlinkHelper(FusabaseUser fusabaseUser, String provider)
            throws FusabaseException;

    @NonNull
    protected abstract JsonObject reauthenticateAndRetrieveDataHelper(FusabaseUser fusabaseUser, AuthCredential credential)
            throws FusabaseException;

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
    protected JsonObject mergeRefreshResult(@NonNull JsonObject userData,
                                            @NonNull JsonObject result) {
        return Json.createObjectBuilder(userData)
                .add("accessToken", result.getString("access_token"))
                .add("refreshToken", result.getString("refresh_token"))
                .build();
    }

    @NonNull
    protected JsonObject profileUpdateResponse(@NonNull UserProfileChangeRequest userProfile) {
        JsonObjectBuilder responseBuilder = Json.createObjectBuilder();
        if (userProfile.getDisplayName() != null) {
            responseBuilder.add("displayName", userProfile.getDisplayName());
        }
        if (userProfile.getPhoneNumber() != null) {
            responseBuilder.add("phoneNumber", userProfile.getPhoneNumber());
        }
        if (userProfile.getPhotoUri() != null) {
            responseBuilder.add("photoUrl", userProfile.getPhotoUri());
        }
        return responseBuilder.build();
    }

    @NonNull
    protected JsonObject executeUserJsonRequest(@NonNull String logAction,
                                                @NonNull String endpoint,
                                                @NonNull String method,
                                                @NonNull Map<String, String> headers,
                                                @NonNull Map<String, String> queryParameters,
                                                @Nullable String payload)
            throws FusabaseException {
        FusabaseLogger.d(TAG, logAction);
        HttpRequestHelper requestHelper = new HttpRequestHelper();
        String url = user.config.getAuthBaseEndpoint(endpoint);
        if (payload == null) {
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
        throw buildUserRequestException(endpoint, response);
    }

    @NonNull
    protected FusabaseException buildUserRequestException(@NonNull String endpoint,
                                                         @NonNull HttpResponse response) {
        if (Config.AUTHENTICATE_REST_EP.equals(endpoint)) {
            return new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(),
                    "Failed to refresh authentication session. Please sign in again.");
        }
        if (Config.UPDATE_PASSWORD_HELPER.equals(endpoint)) {
            return new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(),
                    "Failed to update password. Please check your current password and try again.");
        }
        if (Config.UPDATE_PROFILE_HELPER.equals(endpoint)) {
            return new FusabaseAuthException(FusabaseAuthException.Code.INTERNAL.toString(),
                    "Failed to update user profile. Please try again later.");
        }

        String error = response.getError();
        return new FusabaseException(error == null || error.isEmpty()
                ? "Request failed. Please try again later."
                : error);
    }

    @NonNull
    protected Map<String, String> apiKeyQueryParameters() {
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", user.config.getAppId());
        return queryParameters;
    }

    @NonNull
    protected Map<String, String> jsonHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    /**
     * Builds the Authorization header with access token from persistent storage.
     * The token goes out of scope immediately after use for security.
     *
     * @return Complete Authorization header string or empty string if unavailable
     */
    protected String getAuthorizationHeader() {
        try {
            JsonObject data = user.loadUserData();
            String token = data.containsKey("accessToken") ? data.getString("accessToken") : "";
            return token.isEmpty() ? "" : "Bearer " + token;
        } catch (FusabaseException e) {
            return "";
        }
    }

}
