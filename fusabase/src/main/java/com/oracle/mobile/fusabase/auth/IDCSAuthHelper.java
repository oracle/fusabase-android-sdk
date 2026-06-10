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
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

class IDCSAuthHelper extends AuthHelper {

    private final IDCSConfig config;
    private final FusabaseAuth auth;

    IDCSAuthHelper(@NonNull IDCSConfig config,
                   @NonNull FusabaseAuth auth) {
        this.config = config;
        this.auth = auth;
    }

    @NonNull
    protected IDCSConfig getConfig() {
        return this.config;
    }

    @NonNull
    protected FusabaseAuth getFusabaseAuth() {
        return this.auth;
    }

    @NonNull
    protected JsonObject getAuthenticationDetails(@NonNull String email,
                                                  @NonNull String password,
                                                  @NonNull JsonObject res)
            throws FusabaseException {
        String accessToken = res.getString("access_token");
        String refreshToken = res.getString("refresh_token", "");
        JsonObject userDetails = this.getUserDetails(new GetTokenResult(new IdToken(accessToken, "idcs")));

        FusabaseLogger.i(TAG, "Fetched details for user " +
                userDetails.getString("displayName", userDetails.getString("email", email)) + '.');

        return Json.createObjectBuilder()
                .add("userEmail", email)
                .add("userDetails", userDetails)
                .add("accessToken", accessToken)
                .add("refreshToken", refreshToken)
                .build();
    }

    @NonNull
    @Override
    protected JsonObject normalizeAuthTokenResponse(@NonNull JsonObject result) {
        return Json.createObjectBuilder()
                .add("access_token", result.getString("access_token"))
                .add("refresh_token", optionalString(result, "refresh_token", ""))
                .build();
    }

    @NonNull
    @Override
    protected JsonObject normalizeRegistrationResponse(@NonNull String email,
                                                       @NonNull String password,
                                                       @NonNull JsonObject result)
            throws FusabaseException {
        if (result.containsKey("access_token")) {
            return normalizeAuthTokenResponse(result);
        }
        return this.authenticateUser(email, password);
    }

    protected JsonObject getSnapshotAccessToken(@NonNull IdToken authnToken) throws FusabaseException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", String.format("Bearer %s", authnToken.getToken()));

        JsonObject result = executeAuthJsonRequest(
                "Fetching IDCS snapshot token through ORDS",
                IDCSConfig.AUTHORIZE_SNAPSHOT_PATH,
                "GET",
                headers,
                apiKeyQueryParameters(),
                null);

        return Json.createObjectBuilder()
                .add("access_token", result.getString("access_token"))
                .add("refresh_token", result.getString("refresh_token", ""))
                .build();
    }

    @NonNull
    @Override
    protected JsonObject signInWithCredentialHelper(@NonNull AuthCredential authCredential, boolean link) throws FusabaseException {
        throw new FusabaseAuthException(FusabaseAuthException.Code.NOT_IMPLEMENTED.toString(),
                "IDCS sign-in with credential is not supported.");
    }

    @NonNull
    @Override
    protected JsonObject socialUnlinkHelper(@NonNull String provider) throws FusabaseException {
        throw new FusabaseAuthException(FusabaseAuthException.Code.NOT_IMPLEMENTED.toString(),
                "IDCS social unlink is not supported.");
    }

    @NonNull
    protected JsonObject getUserDetails(@NonNull GetTokenResult accessToken) {
        JsonObject data = (JsonObject) Utils.parseJWT(accessToken.getToken());

        return Json.createObjectBuilder()
                .add("displayName", data.getString("user_displayname", data.getString("email", data.getString("sub", ""))))
                .add("email", data.getString("sub", data.getString("email", "")))
                .add("lastSignIn", convertIatToTimestamp(data.getJsonNumber("iat").longValue()))
                .add("id", data.getString("user_id", data.getString("sub", "")))
                .add("creation_time", data.getString("creation_time", ""))
                .add("email_verified", data.containsKey("email_verified") ? data.get("email_verified") : JsonValue.FALSE)
                .add("idp_name", data.getString("idp_name", "idcs"))
                .add("idp_type", data.getString("idp_type", "idcs"))
                .build();
    }

    private static String convertIatToTimestamp(long timestamp) {
        Date date = new Date(timestamp * 1000L);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'000000'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    @NonNull
    private static String optionalString(@NonNull JsonObject data,
                                         @NonNull String key,
                                         @NonNull String defaultValue) {
        if (!data.containsKey(key) || data.isNull(key)) {
            return defaultValue;
        }
        JsonValue value = data.get(key);
        return value.getValueType() == JsonValue.ValueType.STRING ? data.getString(key) : defaultValue;
    }

    @NonNull
    protected JsonObject performSignOut(@NonNull String refreshToken)
            throws FusabaseException {
        FusabaseException signOutFailure = null;

        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                revokeRefreshToken(refreshToken);
            } catch (FusabaseException e) {
                signOutFailure = e;
                FusabaseLogger.w(TAG, "Unable to revoke Fusabase refresh token during IDCS sign out: " + e.getMessage());
            }
        }

        try {
            signOutFromIDCS();
        } catch (FusabaseException e) {
            FusabaseLogger.w(TAG, "Unable to complete IDCS browser-session logout: " + e.getMessage());
        }

        if (signOutFailure != null) {
            throw signOutFailure;
        }
        return Json.createObjectBuilder().add("success", 1).build();
    }

    private void signOutFromIDCS() throws FusabaseException {
        if (this.config.getIdcsDomainURL().isEmpty()) {
            return;
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/scim+json");

        executeAuthStatusRequestToUrl(
                "Signing out from IDCS domain session",
                Utils.urlBuilder(this.config.getIdcsDomainURL(),
                        IDCSConfig.OAUTH2_PATH,
                        IDCSConfig.V1_PATH,
                        IDCSConfig.USER_LOGOUT_PATH),
                null,
                "GET",
                headers,
                null,
                null);
    }

}
