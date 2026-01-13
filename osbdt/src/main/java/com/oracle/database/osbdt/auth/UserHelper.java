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

import static com.oracle.database.osbdt.auth.OsbdtAuth.LOGGED_IN_USER_KEY;
import static com.oracle.database.osbdt.auth.OsbdtAuth.PREFERENCES_NAME;

import android.content.Context;

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.OsbdtException;
import com.oracle.database.osbdt.logger.OsbdtLogger;
import com.oracle.database.osbdt.utils.Utils;

import java.io.IOException;
import java.security.GeneralSecurityException;

import jakarta.json.JsonObject;

abstract class UserHelper {
    private final String TAG = "OsbdtAuth";
    protected final OsbdtUser user;

    protected UserHelper(@NonNull OsbdtUser user) {
        this.user = user;
    }

    protected  void saveUserDetails (@NonNull Context context,
                                     @NonNull JsonObject userDetails,
                                     @NonNull String prefsName) throws OsbdtException {

        String userId = userDetails.getJsonObject("userDetails").getString("id");

        try {
            this.saveDataToPreferences(context, LOGGED_IN_USER_KEY, userId);
            Utils.saveJsonObjectToPrefs(context, userId, userDetails, prefsName);
            OsbdtLogger.d(TAG, "Stored User detail successfully.");
        } catch (Exception e) {
            OsbdtLogger.e("Cannot store user data " + e.getMessage());
            throw new OsbdtException ( "Cannot store user data");
        }
    }

    protected void saveDataToPreferences(@NonNull Context context,
                                         @NonNull String key,
                                         @NonNull String value)
        throws OsbdtException
    {
        try {
            Utils.savePreferenceData(context,
                value,
                key,
                PREFERENCES_NAME);

        } catch (GeneralSecurityException | IOException e) {
            OsbdtLogger.w(TAG, "Unable to store user data");
            throw new OsbdtException("Cannot store user data");
        }
    }

    @NonNull
    protected abstract boolean validateAccessToken();

    @NonNull
    protected abstract void sendEmailVerificationHelper(@NonNull String email,
                                                              @NonNull String id)
            throws OsbdtException;

    @NonNull
    protected abstract JsonObject updateProfile(@NonNull UserProfileChangeRequest userProfile)
            throws OsbdtException;

    @NonNull
    protected abstract JsonObject updatePasswordHelper(@NonNull OsbdtUser user,
                                                       @NonNull String newPassword,
                                                       @NonNull String oldPassword)
            throws OsbdtException;

    protected abstract String refreshAccessToken()
            throws OsbdtException;

    @NonNull
    protected abstract IdToken getIdTokenHelper(@NonNull boolean forceRefresh)
            throws OsbdtException;

    @NonNull
    protected abstract JsonObject unlinkHelper(OsbdtUser osbdtUser, String provider);

    @NonNull
    protected abstract JsonObject reauthenticateAndRetrieveDataHelper(OsbdtUser osbdtUser, AuthCredential credential);

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
        } catch (OsbdtException e) {
            return "";
        }
    }

}
