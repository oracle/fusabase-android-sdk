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

import static com.oracle.database.osbdt.auth.IDCSConfig.ADMIN_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.ME_EMAIL_VERIFIER;
import static com.oracle.database.osbdt.auth.IDCSConfig.ME_PASSWORD_CHANGER_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.ME_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.OAUTH2_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.TOKEN_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.V1_PATH;

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.OsbdtException;
import com.oracle.database.osbdt.logger.OsbdtLogger;
import com.oracle.database.osbdt.OsbdtNetworkException;
import com.oracle.database.osbdt.http.HttpRequestHelper;
import com.oracle.database.osbdt.http.HttpResponse;
import com.oracle.database.osbdt.utils.Utils;

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

    private final String TAG = "OsbdtAuth";
    private final IDCSConfig config;
    private IdToken authnToken;
    private String authnRefreshToken;
    private IdToken accessToken;
    private String accessRefreshToken;

    private IdToken idcsAccessToken;
    private String idcsRefreshToken;
    private IdToken snapshotAccessToken;
    private String snapshotRefreshToken;
    private final String encodedSecret;
    private final OsbdtUser user;
    private final IDCSAuthHelper authHelper;
    protected IDCSUserHelper(@NonNull IDCSConfig config,
                             IdToken authToken,
                             @NonNull String authnRefreshToken,
                             IdToken accessToken,
                             String accessRefreshToken,
                             IdToken snapshotAccessToken,
                             String snapshotRefreshToken,
                             @NonNull OsbdtUser user) {
        super();
        this.config = config;
        this.authnToken = authToken;
        this.authnRefreshToken = authnRefreshToken;
        this.accessToken = accessToken;
        this.accessRefreshToken = accessRefreshToken;
        this.snapshotAccessToken = snapshotAccessToken;
        this.snapshotRefreshToken = snapshotRefreshToken;
        this.encodedSecret = Base64.getEncoder()
                .encodeToString(String.format("%s:%s", this.config.clientId, this.config.clientSecret).getBytes());
        this.user = user;
        this.authHelper = (IDCSAuthHelper) user.auth.authHelper;
    }

    @NonNull
    protected IdToken getIdTokenHelper (boolean forceRefresh) throws OsbdtException {
        if (forceRefresh || !this.validateAccessToken()) {
            this.refreshAccessToken();
        }
        return this.idcsAccessToken;
    }

    @NonNull
    @Override
    protected JsonObject unlinkHelper(OsbdtUser osbdtUser, String provider) {
        return JsonObject.EMPTY_JSON_OBJECT;
    }

    @NonNull
    @Override
    protected JsonObject reauthenticateAndRetrieveDataHelper(OsbdtUser osbdtUser, AuthCredential credential) {
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    protected JsonObject refreshIDCSAccessToken (@NonNull String refreshToken) throws OsbdtException {
        OsbdtLogger.d(TAG, "Sending request for refreshing token to idcs");

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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i(TAG, "Refreshed user successfully from idcs");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        OsbdtLogger.e(TAG, "Failed to refresh user from" +
            " idcs with response code " + response.getCode() + " with the following error " +
            response.getError());
        throw new OsbdtException(response.getError());
    }
    protected String refreshSnapshotAccessToken () throws OsbdtException {
        JsonObject result = this.refreshIDCSAccessToken(this.snapshotRefreshToken);
        this.snapshotAccessToken = new IdToken(result.getString("access_token"), null);
        this.snapshotRefreshToken = result.getString("refresh_token");
        this.authnToken = new IdToken(result.getString("authn_token"), "password");
        this.authnRefreshToken = result.getString("authn_refresh_token");
        this.user.snapshotAccessToken = this.snapshotAccessToken.getToken();
        this.user.snapshotRefreshToken = this.snapshotRefreshToken;
        return this.snapshotRefreshToken;
    }

    protected String refreshAccessToken () throws OsbdtException{
        JsonObject result = this.refreshIDCSAccessToken(idcsRefreshToken);
        JsonObject authCredential = Json.createObjectBuilder()
            .add("authn_token", this.authnToken.getToken())
            .add("refresh_token", this.authnRefreshToken)
            .build();
        JsonObject osbdtCredential = this.authHelper.getOsbdtToken(authCredential ,result);
        this.accessToken = new IdToken(osbdtCredential.getString("access_token"), this.accessToken.signInProvider);
        this.accessRefreshToken = osbdtCredential.getString("refresh_token");
        this.authnToken = new IdToken(osbdtCredential.getString("authn_token"), this.accessToken.signInProvider);
        this.authnRefreshToken = osbdtCredential.getString("authn_refresh_token");
        this.idcsAccessToken = new IdToken(osbdtCredential.getString("idcs_access_token"), this.idcsAccessToken.signInProvider);
        this.idcsRefreshToken = osbdtCredential.getString(osbdtCredential.getString("idcs_refresh_token"));
        this.user.accessToken = this.accessToken;
        this.user.accessRefreshToken = this.accessRefreshToken;
        return this.accessRefreshToken;
    }

    protected boolean validateAccessToken() {
        return this.accessToken.expirationTime >= Instant.now().toEpochMilli() / 1000 + 60 * 5;
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
    throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request for updating user profile to idcs");

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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i(TAG, "Updated user profile successfully from idcs");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        OsbdtLogger.e(TAG, "Failed to update user profile from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    protected JsonObject updatePasswordHelper (@NonNull OsbdtUser user,
                                               @NonNull String newPassword,
                                               @NonNull String oldPassword)
    throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request for updating user password to idcs");

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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.d(TAG, "Update password executed successfully");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        OsbdtLogger.e(TAG, "Failed to update user password from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    protected void sendEmailVerificationHelper (@NonNull String email,
                                                      @NonNull String id)
    throws OsbdtException {

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
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.d(TAG, "Send email verification request send successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            OsbdtLogger.d(TAG, "Send email verification response -> " + result.toString());
            reader.close();
            return;
        }

        OsbdtLogger.e(TAG, "Failed to send request for email verification from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }
}
