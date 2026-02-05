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
import static com.oracle.database.osbdt.auth.IDCSConfig.AUTHENTICATE_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.ME_PASSWORD_RESETTER_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.ME_PASSWORD_RESET_REQUESTOR_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.ME_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.OAUTH2_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.REVOKE_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.SDK_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.SSO_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.TOKEN_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.USER_LOGOUT_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.USER_TOKEN_VALIDATOR_PATH;
import static com.oracle.database.osbdt.auth.IDCSConfig.V1_PATH;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.database.osbdt.oracledb.OsbdtOracledbException;
import com.oracle.database.osbdt.OsbdtException;
import com.oracle.database.osbdt.logger.OsbdtLogger;
import com.oracle.database.osbdt.OsbdtNetworkException;
import com.oracle.database.osbdt.http.HttpRequestHelper;
import com.oracle.database.osbdt.http.HttpResponse;
import com.oracle.database.osbdt.utils.Utils;

import java.io.StringReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;

class IDCSAuthHelper extends AuthHelper {

    private final IDCSConfig config;
    private final String encodedSecret;
    private IdToken bearerToken;
    private final OsbdtAuth auth;

    private static final String IDCS_SNAPSHOT_SCOPE = "baas-snapshot";
    private static final String IDCS_ACCESS_SCOPE = "urn:opc:idm:__myscopes__";
    IDCSAuthHelper(@NonNull IDCSConfig config,
                   @NonNull OsbdtAuth auth) {
        this.config = config;
        this.encodedSecret = Base64.getEncoder()
                .encodeToString(
                        String.format("%s:%s",
                                this.config.clientId,
                                this.config.clientSecret).getBytes());
        this.auth = auth;
    }

    @NonNull
    protected IDCSConfig getConfig() {
        return this.config;
    }

    @NonNull
    protected OsbdtAuth getOsbdtAuth() {
        return this.auth;
    }

    @NonNull
    protected IdToken getBearerToken() {

        OsbdtLogger.d(TAG, "Sending request to fetch bearer token form from idcs.");

        HttpRequestHelper requestHelper = new HttpRequestHelper();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        headers.put("Authorization", String.format("Basic %s", this.encodedSecret));

        String payload = "grant_type=client_credentials&scope=urn:opc:idm:__myscopes__";

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.idcsDomainURL,
                        OAUTH2_PATH,
                        V1_PATH,
                        TOKEN_PATH),
                "POST",
                headers,
                payload);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
            if (response.getStatus()) {
                OsbdtLogger.d(TAG, "Bearer token fetched successfully");
                JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
                JsonObject result = reader.readObject();
                reader.close();
                return new IdToken(result.getString("access_token"), "password");
            } else {
                OsbdtLogger.e(TAG, "Invalid response received with response code " +
                    response.getCode() + " with following error " + response.getError());
                throw new OsbdtException(response.getError());
            }
        } catch (Exception e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing network operation");
            Exception exception = e;
            if(!(exception instanceof OsbdtOracledbException))
                exception = new OsbdtNetworkException(exception.getMessage() == null ?
                    "Internal Error Occurred" : exception.getMessage());
            throw new CompletionException(exception);
        }
    }

    @Nullable
    protected JsonObject getAuthForm(@NonNull IdToken bearerToken) throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to fetch auth form from idcs.");

        HttpRequestHelper requestHelper = new HttpRequestHelper();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", String.format("Bearer %s", bearerToken.getToken()));

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.idcsDomainURL,
                        SSO_PATH,
                        V1_PATH,
                        SDK_PATH,
                        AUTHENTICATE_PATH),
                "GET",
                headers);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
            if (response.getStatus()) {
                OsbdtLogger.d(TAG, "Successfully fetched auth form from idcs");
                JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
                JsonObject result = reader.readObject();
                reader.close();
                return result;
            } else {
                OsbdtLogger.e(TAG, "Invalid response received with response code " +
                    response.getCode() + " with following error " + response.getError());
                throw new OsbdtException(response.getError());
            }
        } catch (Exception e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            Exception exception = e;
            if(!(exception instanceof OsbdtOracledbException))
                exception = new OsbdtNetworkException(exception.getMessage() == null ?
                    "Internal Error Occurred" : exception.getMessage());
            throw new CompletionException(exception);
        }
    }

    @NonNull
    protected JsonObject getAuthenticationDetails (@NonNull String email,
                                                   @NonNull String password,
                                                  @NonNull JsonObject res)
    throws OsbdtException {
        try {
            JsonObject userDetails = this.getUserDetails(
                    new GetTokenResult(
                            new IdToken(res.getString("idcs_access_token"), "password")));
            return Json.createObjectBuilder()
                    .add("userEmail", email)
                    .add("userDetails", userDetails)
                    .add("accessToken", res.getString("access_token"))
                    .add("refreshToken", res.getString("refresh_token"))
                    .add("authnToken", res.getString("authn_token"))
                    .add("authnRefreshToken", res.getString("authn_refresh_token"))
                    .add("idcsAccessToken", res.getString("idcs_access_token"))
                    .add("idcsRefreshToken", res.getString("idcs_refresh_token"))
                    .build();
        } catch (Exception e)
        {
            OsbdtLogger.e(TAG, "Cannot fetch user details from idcs.");
            throw new OsbdtException("Cannot fetch user details from idcs because " + e.getMessage());
        }
    }

    @NonNull
    protected JsonObject getOsbdtToken (@NonNull JsonObject authCredential,
                                        @NonNull JsonObject profileCredential) throws OsbdtException {
        OsbdtLogger.d(TAG, "Sending request to exchange idcs token with ORDS.");

        JsonObject payload = Json.createObjectBuilder()
                .add("token", profileCredential.getString("access_token"))
                .build();

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.config.getAppId());

        HttpRequestHelper requestHelper = new HttpRequestHelper();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getDomainURL(),
                        ONPREMConfig.UNDERSCORE_PATH,
                        ONPREMConfig.BAAS_SERVICES_PATH,
                        ONPREMConfig.IDM_PATH,
                        IDCSConfig.IDCS_AUTH_TYPE,
                        this.config.getProjectId(),
                        IDCSConfig.TOKEN_EXCHANGE_PATH),
                "POST",
                headers,
                queryParameters,
                payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
            if (response.getStatus()) {
                JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
                JsonObject result = reader.readObject();
                OsbdtLogger.d(TAG, "Successfully exchanged token with OSBDT.");
                reader.close();

                return Json.createObjectBuilder()
                        .add("access_token", result.getString("access_token"))
                        .add("refresh_token", result.getString("refresh_token", ""))
                        .add("authn_token", authCredential.containsKey("authnToken") ? authCredential.getString("authnToken") :
                                authCredential.getString("authn_token", ""))
                        .add("authn_refresh_token", authCredential.getString("refresh_token", ""))
                        .add("idcs_access_token", profileCredential.getString("access_token"))
                        .add("idcs_refresh_token", profileCredential.getString("refresh_token"))
                        .build();
            } else {
                OsbdtLogger.e(TAG, "Invalid response received with response code " +
                        response.getCode() + " with following error " + response.getError());
                throw new OsbdtException(response.getError());
            }
        } catch (Exception e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            Exception exception = e;
            if(!(exception instanceof OsbdtOracledbException))
                exception = new OsbdtNetworkException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage());
            throw new CompletionException(exception);
        }
    }

    @NonNull
    protected JsonObject authenticateUser (@NonNull String email,
                                           @NonNull String password) throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to authenticate user from idcs.");

        this.bearerToken = (this.bearerToken == null ? getBearerToken() : this.bearerToken);

        JsonObject formResult = getAuthForm(this.bearerToken);

        if (formResult == null) {
            OsbdtLogger.d(TAG, "Form result is not available. Cannot authenticate user.");
            throw new OsbdtException("Null Auth form received from idcs");
        }

        JsonObject payload = Json.createObjectBuilder()
                .add("op", "credSubmit")
                .add("credentials", Json.createObjectBuilder()
                        .add("username", email)
                        .add("password", password).build())
                .add("requestState", formResult.getString("requestState"))
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", String.format("Bearer %s", bearerToken.getToken()));

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.idcsDomainURL,
                SSO_PATH,
                V1_PATH,
                SDK_PATH,
                AUTHENTICATE_PATH),
                "POST",
                headers,
                payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
            if (response.getStatus()) {
                JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
                JsonObject result = reader.readObject();
                OsbdtLogger.d(TAG, "Successfully authenticated user " + email + " from idcs.");
                reader.close();

                JsonObject profileCredentials = this.getAccessToken(
                        new IdToken(result.getString("authnToken"), "password"));

                return this.getOsbdtToken(result, profileCredentials);
            } else {
                OsbdtLogger.e(TAG, "Invalid response received with response code " +
                        response.getCode() + " with following error " + response.getError());
                throw new OsbdtException(response.getError());
            }
        } catch (Exception e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            Exception exception = e;
            if (!(exception instanceof OsbdtOracledbException))
                exception = new OsbdtNetworkException(
                        exception.getMessage() == null ? "Internal Error Occurred" : exception.getMessage());
            throw new CompletionException(exception);
        }
    }

    protected boolean validateAccessToken(IdToken token) {
        return token.expirationTime >= Instant.now().toEpochMilli() / 1000 + 60 * 5;
    }

    protected JsonObject getSnapshotAccessToken(@NonNull IdToken authnToken) throws OsbdtException {
        if(!this.validateAccessToken(authnToken)){
            throw new OsbdtException("User not recently logged in to perform Snapshot with IDCS.");
        }
        return this.getAccessTokenWithScope(authnToken, IDCS_SNAPSHOT_SCOPE);
    }

    protected JsonObject getAccessToken (@NonNull IdToken authnToken) throws OsbdtException {
        if(!this.validateAccessToken(authnToken)){
            throw new OsbdtException("User not recently logged in to perform Snapshot with IDCS.");
        }
        return this.getAccessTokenWithScope(authnToken, IDCS_ACCESS_SCOPE);
    }

    protected JsonObject getAccessTokenWithScope(@NonNull IdToken authnToken,
                                                 @NonNull String scope) {

        OsbdtLogger.d(TAG, "Sending request to authenticate user from idcs.");

        String payload = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&scope="
            + scope
            + "+offline_access&assertion=" + authnToken.getToken();

        HttpRequestHelper requestHelper = new HttpRequestHelper();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        headers.put("Authorization", String.format("Basic %s", this.encodedSecret));

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
            if (response.getStatus()) {
                OsbdtLogger.d(TAG, "Authenticated user from idcs successfully.");
                String responseBody = response.getResponse();
                if (responseBody == null || responseBody.isEmpty()) {
                    throw new OsbdtException("Invalid response from server");
                }
                JsonReader reader = Json.createReader(new StringReader(responseBody));
                JsonObject result = reader.readObject();
                reader.close();
                return Json.createObjectBuilder()
                    .add("access_token", result.getString("access_token"))
                    .add("refresh_token", result.getString("refresh_token"))
                    .build();
            } else {
                OsbdtLogger.e(TAG, "Invalid response received with response code " +
                    response.getCode() + " with following error " + response.getError());
                throw new OsbdtException(response.getError());
            }
        } catch (Exception e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            Exception exception = e;
            if(!(exception instanceof OsbdtOracledbException))
                exception = new OsbdtNetworkException(exception.getMessage() == null ?
                    "Internal Error Occurred" : exception.getMessage());
            throw new CompletionException(exception);
        }
    }

    @NonNull
    protected JsonObject reloadUser(@NonNull OsbdtUser user) throws OsbdtException {
        GetTokenResult accessToken = new GetTokenResult(user.userHelper.getIdTokenHelper(true));
        return this.getUserDetails(accessToken);
    }

    @NonNull
    @Override
    protected JsonObject performCodeExchange(@NonNull String authCode, @NonNull String codeVerifier) throws OsbdtException {
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
                        IDCSConfig.UNDERSCORE_PATH,
                        IDCSConfig.BAAS_SERVICES_PATH,
                        IDCSConfig.IDM_PATH,
                        IDCSConfig.IDCS_AUTH_TYPE,
                        this.config.getProjectId(),
                        IDCSConfig.GET_REDIRECT_RESULT),
                "POST",
                new HashMap<>(),
                queryParameters,
                payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
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
        } catch (Exception e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            Exception exception = e;
            if(!(exception instanceof OsbdtOracledbException))
                exception = new OsbdtNetworkException(exception.getMessage() == null ?
                    "Internal Error Occurred" : exception.getMessage());
            throw new CompletionException(exception);
        }
    }

    @NonNull
    @Override
    protected JsonObject signInWithCredentialHelper(@NonNull AuthCredential authCredential, boolean link) throws OsbdtException {
        return JsonObject.EMPTY_JSON_OBJECT;
    }

    @NonNull
    @Override
    protected JsonObject socialUnlinkHelper(@NonNull String provider) throws OsbdtException {
        return  JsonObject.EMPTY_JSON_OBJECT;
    }

    @NonNull
    protected JsonObject getUserDetails(@NonNull GetTokenResult accessToken) throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to fetch user details from idcs.");
        JsonObject tokenData = (JsonObject) Utils.parseJWT(accessToken.getToken());

        HttpRequestHelper requestHelper = new HttpRequestHelper();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/scim+json");
        headers.put("Authorization", String.format("Bearer %s", accessToken.getToken()));

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                        ADMIN_PATH,
                        V1_PATH,
                        ME_PATH),
                "GET",
                headers);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.d(TAG, "Fetched user details from idcs successfully.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();

            JsonObjectBuilder updatedResultBuilder = Json.createObjectBuilder(result);
            updatedResultBuilder.add("email", result.getJsonArray("emails").getJsonObject(0).getString("value"))
                    .add("email_verified", tokenData.get("email_verified"))
                    .add("idp_name", tokenData.getString("idp_name"))
                    .add("idp_type", tokenData.getString("idp_type"));

            JsonObject meta = Json.createObjectBuilder()
                    .add("lastSignIn",
                            Timestamp.from(Instant.ofEpochSecond(tokenData.getJsonNumber("iat").longValue())).toString())
                    .add("created", result.getJsonObject("meta").getString("created"))
                    .build();
            updatedResultBuilder.add("meta", meta);

            return updatedResultBuilder.build();
        }
        OsbdtLogger.e(TAG, "Failed to fetch user details from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    protected JsonObject registerUser(@NonNull String email,
                                      @NonNull String password) throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to register user at idcs.");

        this.bearerToken = (this.bearerToken == null ? getBearerToken() : this.bearerToken);

        JsonObject payload = Json.createObjectBuilder()
                .add("schemas", Json.createArrayBuilder()
                        .add("urn:ietf:params:scim:schemas:core:2.0:User")
                        .add("urn:ietf:params:scim:schemas:oracle:idcs:extension:selfRegistration:User")
                        .build())
                .add("userName", email)
                .add("name", Json.createObjectBuilder()
                        .add("givenName", "-")
                        .add("familyName", "-").build())
                .add("password", password)
                .add("emails", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("value", email)
                                .add("type", "work")
                                .add("primary", true)
                                .build())
                        .build())
                .add("urn:ietf:params:scim:schemas:oracle:idcs:extension:selfRegistration:User",
                        Json.createObjectBuilder().add("selfRegistrationProfile",
                                Json.createObjectBuilder().add("value", this.config.selfRegistrationProfile)
                                        .build())
                                .add("consentGranted", true)
                                .build())
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/scim+json;charset=UTF-8");
        headers.put("Authorization", String.format("Bearer %s", bearerToken.getToken()));

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                ADMIN_PATH,
                V1_PATH,
                ME_PATH),
                "POST",
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
            OsbdtLogger.i(TAG, "Registered user successfully a idcs.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return this.authenticateUser(email, password);
        }

        OsbdtLogger.e(TAG, "Failed to register user at" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError() + " " + response.getResponse());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    protected JsonObject performSignOut(@NonNull String refreshToken)
            throws OsbdtException {

        if(this.auth.getCurrentUser() == null)
        {
            OsbdtLogger.d(TAG, "No logged in user present for sign out from idcs.");
            return Json.createObjectBuilder()
                .add("success", 1)
                .build();
        }

        OsbdtLogger.d(TAG, "Sending request to sign out user from idcs.");

        HttpRequestHelper requestHelper = new HttpRequestHelper();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/scim+json");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                        OAUTH2_PATH,
                        V1_PATH,
                        USER_LOGOUT_PATH),
                "GET",
                headers);

        HttpResponse response = null;
        String form = "";

        try {
            response = requestHelper.executeRequest();
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            OsbdtLogger.i(TAG, "Signed out user from idcs successfully.");
            try {
                JsonObject data = this.auth.getCurrentUser().loadUserData();
                String osbdtRefreshToken = data.getString("refreshToken", "");
                String idcsRefreshToken = data.getString("idcsRefreshToken", "");
                if (osbdtRefreshToken != null && !osbdtRefreshToken.isEmpty()) {
                    this.revokeRefreshToken(osbdtRefreshToken);
                }
                if (idcsRefreshToken != null && !idcsRefreshToken.isEmpty()) {
                    this.revokeRefreshToken(idcsRefreshToken);
                }
            } catch (OsbdtException e) {
                OsbdtLogger.e(TAG, "Cannot load user data for revoke: " + e.getMessage());
            }
            form = response.getResponse();
            return Json.createObjectBuilder().add("success", 1)
                    .add("form", form)
                    .build();
        }

        OsbdtLogger.e(TAG, "Failed to sign out user from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    protected void revokeRefreshToken (@NonNull String refresh_token)
            throws OsbdtException {
        OsbdtLogger.d(TAG, "Sending request to sign out user from idcs.");
        HttpRequestHelper requestHelper = new HttpRequestHelper();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization ", String.format("Basic %s", this.encodedSecret));
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.put("Accept", "*/*");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                        OAUTH2_PATH,
                        V1_PATH,
                        REVOKE_PATH),
                "POST",
                headers,
                String.format("token=%s", refresh_token));

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (OsbdtException e) {
            OsbdtLogger.e(TAG, "Network error encountered while performing the operation");
            throw new OsbdtNetworkException("Network error encountered due to " + e.getCause());
        }


        if (response.getStatus()) {
            OsbdtLogger.i(TAG, "Successfully signed out user form idcs");
        }

        OsbdtLogger.e(TAG, "Failed to sign out user from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    protected JsonObject sendPasswordResetEmailHelper(@NonNull String email)
            throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to send password reset email from idcs.");
        this.bearerToken = (this.bearerToken == null ? getBearerToken() : this.bearerToken);

        JsonObject payload = Json.createObjectBuilder()
                .add("userName", email)
                .add("notificationType", "email")
                .add("notificationEmailAddress", String.format("****@%s", email.split("@")[1]))
                .add("schemas", Json.createArrayBuilder()
                        .add("urn:ietf:params:scim:schemas:oracle:idcs:MePasswordResetRequestor")
                        .build())
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", String.format("Bearer %s", this.bearerToken.getToken()));
        headers.put("Content-Type", "application/scim+json");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                        ADMIN_PATH,
                        V1_PATH,
                        ME_PASSWORD_RESET_REQUESTOR_PATH),
                "POST",
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
            OsbdtLogger.i(TAG, "Successfully sent password reset email.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }


        OsbdtLogger.e(TAG, "Failed to send password reset email from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    protected JsonObject verifyPasswordResetCodeHelper(@NonNull String token)
            throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to verify password request code from idcs.");
        this.bearerToken = (this.bearerToken == null ? getBearerToken() : this.bearerToken);

        JsonObject payload = Json.createObjectBuilder()
                .add("token", token)
                .add("schemas", Json.createArrayBuilder()
                        .add("urn:ietf:params:scim:schemas:oracle:idcs:UserTokenValidator")
                        .build())
                .build();

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization ", String.format("Bearer %s", this.bearerToken.getToken()));
        headers.put("Content-Type", "application/scim+json");
        headers.put("Accept", "application/json");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                        ADMIN_PATH,
                        V1_PATH,
                        USER_TOKEN_VALIDATOR_PATH),
                "POST",
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
            OsbdtLogger.i(TAG, "Successfully verified password reset code from idcs.");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        OsbdtLogger.e(TAG, "Failed to verify password reset code from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

    @NonNull
    protected JsonObject confirmPasswordResetHelper(@NonNull String token,
                                                    @NonNull String newPassword)
            throws OsbdtException {

        OsbdtLogger.d(TAG, "Sending request to confirm password request code from idcs.");
        this.bearerToken = (this.bearerToken == null ? getBearerToken() : this.bearerToken);

        JsonObject payload = Json.createObjectBuilder()
                .add("token", token)
                .add("password", newPassword)
                .add("schemas", Json.createArrayBuilder()
                        .add("urn:ietf:params:scim:schemas:oracle:idcs:MePasswordResetter")
                        .build())
                .build();
        HttpRequestHelper requestHelper = new HttpRequestHelper();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization ", String.format("Bearer %s", this.bearerToken.getToken()));
        headers.put("Content-Type", "application/scim+json");
        headers.put("Accept", "application/json");

        requestHelper.createHttpRequest(Utils.urlBuilder(this.config.getIdcsDomainURL(),
                        ADMIN_PATH,
                        V1_PATH,
                        ME_PASSWORD_RESETTER_PATH),
                "POST",
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
            OsbdtLogger.i(TAG, "Fetch docs executed successfully");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        } else {
            OsbdtLogger.w(TAG, "getDoc failed with response message " + response.getError());
        }
        OsbdtLogger.e(TAG, "Failed to sign out user from" +
                " idcs with response code " + response.getCode() + " with the following error " +
                response.getError());
        throw new OsbdtException(response.getError());
    }

}
