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
import androidx.annotation.Nullable;

import java.util.Locale;

import jakarta.json.JsonObject;

final class MultiFactorInfoFactory {
    private MultiFactorInfoFactory() {}

    static @NonNull MultiFactorInfo fromJson(@NonNull JsonObject response) throws FusabaseAuthException {
        String uid = firstString(response, "factor_id", "factorId");
        if (uid == null || uid.trim().isEmpty()) {
            throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "MFA factor response is missing factor_id.");
        }

        String provider = firstString(response, "provider");
        String factorId = normalizeFactorId(provider != null ? provider : firstString(response, "method"));
        String displayName = firstString(response, "display_name", "displayName");
        if (PhoneMultiFactorGenerator.FACTOR_ID.equals(factorId)) {
            return new PhoneMultiFactorInfo(uid, displayName,
                    displayName == null ? "" : displayName);
        }

        if (TotpMultiFactorGenerator.FACTOR_ID.equals(factorId)) {
            return new TotpMultiFactorInfo(uid, displayName);
        }

        throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                "Unsupported MFA factor type: " + factorId);
    }

    static @NonNull String normalizeFactorId(@Nullable String raw) {
        if (raw == null) return "";
        String value = raw.toLowerCase(Locale.US);
        if ("sms".equals(value) || "twilio".equals(value)) {
            return PhoneMultiFactorGenerator.FACTOR_ID;
        }
        if ("totp".equals(value)) {
            return TotpMultiFactorGenerator.FACTOR_ID;
        }
        return value;
    }

    private static @Nullable String firstString(@NonNull JsonObject object, @NonNull String... keys) {
        for (String key : keys) {
            if (object.containsKey(key) && !object.isNull(key)) {
                try {
                    return object.getString(key);
                } catch (ClassCastException ignored) {
                    return object.get(key).toString().replace("\"", "");
                }
            }
        }
        return null;
    }
}
