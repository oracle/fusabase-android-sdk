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

import static com.oracle.mobile.fusabase.auth.IDCSConfig.ADMIN_PATH;
import static com.oracle.mobile.fusabase.auth.IDCSConfig.ME_EMAIL_VERIFIER;
import static com.oracle.mobile.fusabase.auth.IDCSConfig.ME_PASSWORD_CHANGER_PATH;
import static com.oracle.mobile.fusabase.auth.IDCSConfig.ME_PATH;
import static com.oracle.mobile.fusabase.auth.IDCSConfig.OAUTH2_PATH;
import static com.oracle.mobile.fusabase.auth.IDCSConfig.TOKEN_PATH;
import static com.oracle.mobile.fusabase.auth.IDCSConfig.V1_PATH;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.FusabaseNetworkException;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.utils.Utils;

import java.io.StringReader;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

class IDCSUserHelper extends UserHelper {

    private final String TAG = "FusabaseAuth";
    private final IDCSConfig config;
    private final String encodedSecret;
    private final FusabaseUser user;
    private final IDCSAuthHelper authHelper;
    protected IDCSUserHelper(@NonNull IDCSConfig config, @NonNull FusabaseUser user) {
        super(user);
        this.config = config;
        this.encodedSecret = Base64.getEncoder()
                .encodeToString(String.format("%s:%s", this.config.clientId, this.config.clientSecret).getBytes());
        this.user = user;
        this.authHelper = (IDCSAuthHelper) user.auth.authHelper;
    }

    @NonNull
    protected IdToken getIdTokenHelper (boolean forceRefresh) throws FusabaseException {
        if (forceRefresh || !this.validateAccessToken()) {
            refreshAccessToken();
        }
        JsonObject data = user.loadUserData();
        String tokenStr = data.getString("idcsAccessToken");
        return new IdToken(tokenStr, "idcs");
    }

    @NonNull
    @Override
    protected JsonObject unlinkHelper(FusabaseUser fusabaseUser, String provider) {
        return JsonObject.EMPTY_JSON_OBJECT;
    }

    @NonNull
    @Override
    protected JsonObject reauthenticateAndRetrieveDataHelper(FusabaseUser fusabaseUser, AuthCredential credential) {
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    protected JsonObject refreshIDCSAccessToken (@NonNull String refreshToken) throws FusabaseException {
        FusabaseLogger.d(TAG, "Sending request for refreshing token to idcs");

        String payload = "grant_type=refresh_token&refresh_token=" + refreshToken + "&scope=offline_access";

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + this.encodedSecret);
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                OAUTH2_PATH,
                V1_PATH,
                TOKEN_PATH),
            "POST",
            headers,
            payload);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.i(TAG, "Refreshed user successfully from idcs");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        FusabaseLogger.e(TAG, "Failed to refresh user from" +
            " idcs with response code " + response.getCode() + " with the following error " +
            response.getError());
        throw new FusabaseException(response.getError());
    }


    protected String refreshAccessToken () throws FusabaseException{
        JsonObject userData = user.loadUserData();
        String idcsRefreshToken = userData.getString("idcsRefreshToken");
        JsonObject result = this.refreshIDCSAccessToken(idcsRefreshToken);
        JsonObject authCredential = Json.createObjectBuilder()
            .add("authn_token", userData.getString("authnToken"))
            .add("refresh_token", userData.getString("authnRefreshToken"))
            .build();
        JsonObject fusabaseCredential = this.authHelper.getFusabaseToken(authCredential ,result);

        String newAccessToken = fusabaseCredential.getString("access_token");
        String newRefreshToken = fusabaseCredential.getString("refresh_token");
        String newAuthnToken = fusabaseCredential.getString("authn_token");
        String newAuthnRefreshToken = fusabaseCredential.getString("authn_refresh_token");
        String newIdcsAccessToken = fusabaseCredential.getString("idcs_access_token");
        String newIdcsRefreshToken = fusabaseCredential.getString("idcs_refresh_token");

        JsonObject updatedData = Json.createObjectBuilder(userData)
            .add("accessToken", newAccessToken)
            .add("refreshToken", newRefreshToken)
            .add("authnToken", newAuthnToken)
            .add("authnRefreshToken", newAuthnRefreshToken)
            .add("idcsAccessToken", newIdcsAccessToken)
            .add("idcsRefreshToken", newIdcsRefreshToken)
            .build();

        saveUserDetails(user.auth.getApp().getApplicationContext(), updatedData, FusabaseAuth.PREFERENCES_NAME);

        return newAccessToken;
    }

    protected boolean validateAccessToken() {
        try {
            JsonObject data = user.loadUserData();
            String tokenStr = data.getString("idcsAccessToken");
            IdToken token = new IdToken(tokenStr, "idcs");
            return token.expirationTime >= Instant.now().toEpochMilli() / 1000 + 60 * 5;
        } catch (Exception e) {
            return false;
        }
    }

    protected JsonArray makeOperations (@NonNull UserProfileChangeRequest userProfile) {

        JsonArrayBuilder operationBuilder = Json.createArrayBuilder();

        if (userProfile.getDisplayName() != null
                && !Objects.equals(userProfile.getDisplayName(), this.user.getDisplayName())) {
            operationBuilder
                    .add(Json.createObjectBuilder()
                            .add("op", (this.user.getDisplayName() != null &&  !this.user.getDisplayName().isEmpty()) ?
                                    (userProfile.getDisplayName() != null && !userProfile.getDisplayName().isEmpty()) ? "replace" : "remove" : "add")
                            .add("path", "displayName")
                            .add("value", userProfile.getDisplayName())
                            .build());
        }
//         Let's see if we are supporting this
        if(userProfile.getPhoneNumber() != null
                && !Objects.equals(userProfile.getPhoneNumber(), this.user.getPhoneNumber()))
        {
            operationBuilder
                    .add(Json.createObjectBuilder()
                            .add("op", (this.user.getPhoneNumber() != null &&  !this.user.getPhoneNumber().isEmpty()) ?
                                    (userProfile.getPhoneNumber() != null && !userProfile.getPhoneNumber().isEmpty()) ? "replace" : "remove" : "add")
                            .add("path", "phoneNumbers")
                            .add("value", Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("value", userProfile.getPhoneNumber())
                                            .add("type", "home")
                                            .build())
                                    .build()));
        }

        if (userProfile.getPhotoUri() != null
                && !Objects.equals(userProfile.getPhotoUri(), this.user.getPhotoUrl())) {
            operationBuilder
                    .add(Json.createObjectBuilder()
                            .add("op", (this.user.getPhotoUrl() != null  &&  !this.user.getPhotoUrl().isEmpty()) ?
                                    (userProfile.getPhotoUri() != null && !userProfile.getPhotoUri().isEmpty()) ? "replace" : "remove" : "add")
                            .add("path", "photos")
                            .add("value", Json.createArrayBuilder()
                                    .add(Json.createObjectBuilder()
                                            .add("value", userProfile.getPhotoUri())
                                            .add("type", "photo")
                                            .build())
                                    .build()));
        }

        return operationBuilder.build();
    }

    @NonNull
    protected JsonObject updateProfile (@NonNull UserProfileChangeRequest userProfile)
    throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request for updating user profile to idcs");

        IdToken accessToken = this.getIdTokenHelper(false);
        JsonObject payload = Json.createObjectBuilder()
                .add("schemas", Json.createArrayBuilder()
                        .add("urn:ietf:params:scim:api:messages:2.0:PatchOp")
                        .build())
                .add("Operations", makeOperations(userProfile))
                .build();

        // Do not send empty operations array
        if(payload.getJsonArray("Operations").isEmpty())
        {
            return Json.createObjectBuilder().build();
        }

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", String.format("Bearer %s", accessToken.getToken()));
        headers.put("Content-Type", "application/scim+json");

            requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                            ADMIN_PATH,
                            V1_PATH,
                            ME_PATH),
                    "PATCH",
                    headers,
                    payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.i(TAG, "Updated user profile successfully from idcs");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        FusabaseLogger.e(TAG, "Failed to update user profile from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseException(response.getError());
    }

    @NonNull
    protected JsonObject updatePasswordHelper (@NonNull FusabaseUser user,
                                               @NonNull String newPassword,
                                               @NonNull String oldPassword)
    throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request for updating user password to idcs");

        JsonObject payload = Json.createObjectBuilder()
                .add("password", newPassword)
                .add("oldPassword", oldPassword)
                .add("schemas", Json.createArrayBuilder()
                        .add("urn:ietf:params:scim:schemas:oracle:idcs:MePasswordChanger")
                        .build())
                .build();

        IdToken accessToken = this.getIdTokenHelper(false);

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", String.format("Bearer %s", accessToken.getToken()));
        headers.put("Content-Type", "application/scim+json");

            requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                            ADMIN_PATH,
                            V1_PATH,
                            ME_PASSWORD_CHANGER_PATH),
                    "PUT",
                    headers,
                    payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Update password executed successfully");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        FusabaseLogger.e(TAG, "Failed to update user password from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseException(response.getError());
    }

    protected void sendEmailVerificationHelper (@NonNull String email,
                                                      @NonNull String id)
    throws FusabaseException {

        JsonObject payload = Json.createObjectBuilder()
                .add("id", id)
                .add("email", email)
                .add("schemas", Json.createArrayBuilder()
                        .add("urn:ietf:params:scim:schemas:oracle:idcs:MeEmailVerifier")
                        .build())
                .add("meta", Json.createObjectBuilder()
                        .add("resourceType", "MeEmailVerifier")
                        .build())
                .build();

        IdToken accessToken = this.getIdTokenHelper(false);

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", String.format("Bearer %s", accessToken.getToken()));
        headers.put("Content-Type", "application/json");

            requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                            ADMIN_PATH,
                            V1_PATH,
                            ME_EMAIL_VERIFIER),
                    "PUT",
                    headers,
                    payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Send email verification request send successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            FusabaseLogger.d(TAG, "Send email verification response -> " + result.toString());
            reader.close();
            return;
        }

        FusabaseLogger.e(TAG, "Failed to send request for email verification from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new FusabaseException(response.getError());
    }
}
