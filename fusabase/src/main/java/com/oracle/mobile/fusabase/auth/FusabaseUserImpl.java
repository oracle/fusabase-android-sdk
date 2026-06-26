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

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

class FusabaseUserImpl extends FusabaseUser {

    private String providerId;
    private String uid;
    private boolean isAnonymous;
    private @Nullable String ocid;
    private boolean emailVerified;
    private FusabaseUserMetadata userMetadata;
    private UserHelper userHelper;
    private FusabaseAuth auth;

    protected FusabaseUserImpl(@NonNull JsonObject Data,
                            @NonNull FusabaseAuth auth,
                            @Nullable String password) {

        super(Data, auth, password);

        JsonObject userDetails = Data.getJsonObject("userDetails");

        this.email = userDetails.containsKey("email")
            ? userDetails.getString("email")
            : Data.getString("userEmail");
        this.uid = userDetails.getString("id");
        this.isAnonymous = false;
        this.providerId = userDetails.getString("idp_name");
        this.emailVerified = Boolean.parseBoolean(userDetails.get("email_verified").toString());
        this.displayName = userDetails.containsKey("displayName") ?
            userDetails.getString("displayName"):
            null;

        if (userDetails.containsKey("phoneNumber")) {
            this.phoneNumber = userDetails.getString("phoneNumber");
        } else if (userDetails.containsKey("phoneNumbers")) {
            JsonArray arr = userDetails.getJsonArray("phoneNumbers");
            for (JsonValue val : arr) {
                JsonObject phoneObj = val.asJsonObject();

                String type = phoneObj.getString("type", null);
                if ("home".equalsIgnoreCase(type)) {
                    this.phoneNumber = phoneObj.getString("value", null);
                    break;
                }
            }
        }

        if (userDetails.containsKey("photoUrl")) {
            this.photoUrl = userDetails.getString("photo");
        } else if (userDetails.containsKey("photo")) {
            JsonArray arr = userDetails.getJsonArray("photo");
            for (JsonValue val : arr) {
                JsonObject phoneObj = val.asJsonObject();

                String type = phoneObj.getString("type", null);
                if ("home".equalsIgnoreCase(type)) {
                    this.photoUrl = phoneObj.getString("value", null);
                    break;
                }
            }
        }

        this.userMetadata = new FusabaseUserMetadata() {
            @Override
            public long getCreationTimestamp() {
                String raw = Data.getJsonObject("userDetails").containsKey("creation_time") ?
                    Data.getJsonObject("userDetails").getString("lastSignIn") :
                    Data.getJsonObject("userDetails").containsKey("meta") ?
                        Data.getJsonObject("userDetails").getJsonObject("meta").getString("lastSignIn", "") :
                        "";
                return parseToEpochMillis(raw);
            }

            @Override
            public long getLastSignInTimestamp() {
                String raw = Data.getJsonObject("userDetails").containsKey("lastSignIn") ?
                    Data.getJsonObject("userDetails").getString("lastSignIn") :
                    Data.getJsonObject("userDetails").containsKey("meta") ?
                        Data.getJsonObject("userDetails").getJsonObject("meta").getString("lastSignIn", "") :
                        "";
                return raw.isEmpty() ? -1 : parseToEpochMillis(raw);
            }

            /**
             * Parses an RFC 3339 / ISO-8601 timestamp (with or without zone info)
             * into epoch milliseconds.
             */
            private long parseToEpochMillis(String timestamp) {
                try {
                    // Try parsing directly — works if it includes 'Z' or an offset (+00:00, etc.)
                    return Instant.parse(timestamp).toEpochMilli();
                } catch (Exception e) {
                    try {
                        // If no timezone provided, assume UTC
                        LocalDateTime ldt = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
                    } catch (Exception e2) {
                        // Try parsing human-readable format like 'Fri Nov 21 18:29:03 GMT+05:30 2025'
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");
                        ZonedDateTime zdt = ZonedDateTime.parse(timestamp, formatter);
                        return zdt.toInstant().toEpochMilli();
                    }
                }
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(@NonNull Parcel parcel, int i) {
                return;
            }
        };
    }

    @NonNull
    protected String getRefreshToken() {
        try {
            JsonObject data = loadUserData();
            return data.containsKey("refreshToken") ? data.getString("refreshToken") : "";
        } catch (FusabaseException e) {
            return "";
        }
    }

    @NonNull
    protected String getAccessToken() {
        try {
            JsonObject data = loadUserData();
            return data.containsKey("accessToken") ? data.getString("accessToken") : "";
        } catch (FusabaseException e) {
            return "";
        }
    }

    @Nullable
    @Override
    public String getPhoneNumber() {
        return super.phoneNumber;
    }

    @Nullable
    @Override
    public String getDisplayName() {
        return super.displayName;
    }

    @Nullable
    @Override
    public String getEmail() {
        return this.email;
    }

    @Nullable
    @Override
    public String getPhotoUrl() {
        return super.photoUrl;
    }

    @NonNull
    @Override
    public String getProviderId() {
        return this.providerId;
    }

    @NonNull
    @Override
    public FusabaseUserMetadata getMetadata() {
        return this.userMetadata;
    }
    @NonNull
    @Override
    public String getUid() {
        return this.uid;
    }

    @Override
    public boolean isAnonymous() {
        return this.isAnonymous;
    }

    public boolean isEmailVerified() {
        return this.emailVerified;
    }
}
