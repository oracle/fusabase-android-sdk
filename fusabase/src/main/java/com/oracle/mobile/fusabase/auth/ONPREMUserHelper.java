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
    @Override
    protected JsonObject unlinkHelper(FusabaseUser fusabaseUser, String provider) {
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    @NonNull
    @Override
    protected JsonObject reauthenticateAndRetrieveDataHelper(FusabaseUser fusabaseUser, AuthCredential credential) {
        return JsonValue.EMPTY_JSON_OBJECT;
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

            requestHelper.createHttpRequest(this.config.getAuthBaseEndpoint(ONPREMConfig.SEND_EMAIL_VERIFICATION),
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
