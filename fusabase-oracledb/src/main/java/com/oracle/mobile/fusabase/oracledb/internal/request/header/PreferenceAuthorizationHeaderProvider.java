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


package com.oracle.mobile.fusabase.oracledb.internal.request.header;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.utils.Utils;

import jakarta.json.JsonObject;

/**
 * Existing preference-backed bearer-header adapter used by the OracleDB REST path.
 *
 * <p>The credential is read only when the provider creates a request; neither the request
 * metadata nor diagnostics retain or print the credential.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class PreferenceAuthorizationHeaderProvider
        implements AuthorizationHeaderProvider {
    private static final String TAG = "OracledbAuthHeader";
    private final Context context;

    public PreferenceAuthorizationHeaderProvider(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public String getAuthorizationHeader() {
        try {
            String key = Utils.getPreferenceData(context,
                    "LOGGED_IN_USER",
                    "FusabasePreferences");
            if (key != null && !key.isEmpty()) {
                JsonObject userDetails = Utils.loadJsonObjectFromPreferences(
                        context,
                        key,
                        "FusabasePreferences");
                if (userDetails.containsKey("accessToken")) {
                    return "Bearer " + userDetails.getString("accessToken");
                }
            }
        } catch (Exception e) {
            // Do not include exception text: preference/decryption failures may contain secrets.
            FusabaseLogger.w(TAG, "Authorization header could not be resolved");
        }
        FusabaseLogger.v(TAG, "No authorization credential available");
        return "";
    }
}
