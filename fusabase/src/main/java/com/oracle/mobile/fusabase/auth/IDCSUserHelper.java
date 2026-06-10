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

import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

class IDCSUserHelper extends UserHelper {

    protected IDCSUserHelper(@NonNull IDCSConfig config, @NonNull FusabaseUser user) {
        super(user);
    }

    @NonNull
    @Override
    protected JsonObject unlinkHelper(FusabaseUser fusabaseUser, String provider)
            throws FusabaseException {
        throw new FusabaseAuthException(FusabaseAuthException.Code.NOT_IMPLEMENTED.toString(),
                "IDCS unlink is not supported.");
    }

    @NonNull
    @Override
    protected JsonObject reauthenticateAndRetrieveDataHelper(FusabaseUser fusabaseUser, AuthCredential credential)
            throws FusabaseException {
        throw new FusabaseAuthException(FusabaseAuthException.Code.NOT_IMPLEMENTED.toString(),
                "IDCS reauthentication with credential is not supported.");
    }

    @NonNull
    @Override
    protected JsonObject updateProfile(@NonNull UserProfileChangeRequest userProfile)
            throws FusabaseException {
        JsonObject payload = buildProfileUpdatePayload(userProfile);
        if (payload.isEmpty()) {
            return Json.createObjectBuilder().build();
        }

        IdToken accessToken = this.getIdTokenHelper(true);
        java.util.Map<String, String> headers = jsonHeaders();
        headers.put("X-AUTHZ", accessToken.getToken());

        executeUserJsonRequest(
                "Updating IDCS user profile through ORDS",
                Config.UPDATE_PROFILE_HELPER,
                "PUT",
                headers,
                apiKeyQueryParameters(),
                payload.toString());

        return profileUpdateResponse(userProfile);
    }

    @NonNull
    static JsonObject buildProfileUpdatePayload(@NonNull UserProfileChangeRequest userProfile) {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();

        if (userProfile.getDisplayName() != null) {
            payloadBuilder.add("displayName", userProfile.getDisplayName());
        }
        if (userProfile.getPhoneNumber() != null) {
            payloadBuilder.add("phoneNumbers", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("value", userProfile.getPhoneNumber())
                            .add("type", "mobile")
                            .add("primary", true)
                            .build())
                    .build());
        }
        if (userProfile.getPhotoUri() != null) {
            payloadBuilder.add("photos", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("value", userProfile.getPhotoUri())
                            .add("type", "photo")
                            .add("primary", true)
                            .build())
                    .build());
        }

        return payloadBuilder.build();
    }

    @NonNull
    @Override
    protected JsonArray makeOperations(@NonNull UserProfileChangeRequest userProfile) {
        JsonArrayBuilder operationBuilder = Json.createArrayBuilder();

        if (userProfile.getDisplayName() != null
                && !Objects.equals(userProfile.getDisplayName(), this.user.getDisplayName())) {
            operationBuilder.add(Json.createObjectBuilder()
                    .add("op", this.user.getDisplayName() != null ? "replace" : "add")
                    .add("path", "displayName")
                    .add("value", userProfile.getDisplayName())
                    .build());
        }

        if (userProfile.getPhoneNumber() != null
                && !Objects.equals(userProfile.getPhoneNumber(), this.user.getPhoneNumber())) {
            operationBuilder.add(Json.createObjectBuilder()
                    .add("op", this.user.getPhoneNumber() != null ? "replace" : "add")
                    .add("path", "phoneNumber")
                    .add("value", userProfile.getPhoneNumber())
                    .build());
        }

        if (userProfile.getPhotoUri() != null
                && !Objects.equals(userProfile.getPhotoUri(), this.user.getPhotoUrl())) {
            operationBuilder.add(Json.createObjectBuilder()
                    .add("op", this.user.getPhotoUrl() != null ? "replace" : "add")
                    .add("path", "photos")
                    .add("value", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("value", userProfile.getPhotoUri())
                                    .add("type", "photo")
                                    .add("primary", true)
                                    .build())
                            .build())
                    .build());
        }

        return operationBuilder.build();
    }

    protected void sendEmailVerificationHelper(@NonNull String email,
                                               @NonNull String id)
            throws FusabaseException {
        throw new FusabaseAuthException(FusabaseAuthException.Code.NOT_IMPLEMENTED.toString(),
                "IDCS email verification is not supported by ORDS.");
    }
}
