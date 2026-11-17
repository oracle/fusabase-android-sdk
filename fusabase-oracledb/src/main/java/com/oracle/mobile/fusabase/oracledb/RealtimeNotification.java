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


package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.NonNull;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/** Parses the realtime notification envelope without mixing envelope values into document data. */
final class RealtimeNotification {
    private RealtimeNotification() { }

    static boolean hasRootMetadata(@NonNull JsonObject notification) {
        return notification.get("_metadata") instanceof JsonObject;
    }

    @NonNull
    static JsonObject changedData(@NonNull JsonObject notification) {
        JsonObject data = notification.getJsonObject("changedData");
        return data == null ? jakarta.json.Json.createObjectBuilder().build() : data;
    }

    static String oid(@NonNull JsonObject notification) {
        return metadataValue(notification, "OID", "OID", null);
    }

    static String version(@NonNull JsonObject notification) {
        return metadataValue(notification, "VERSION", "VERSION", "etag");
    }

    static String asOf(@NonNull JsonObject notification) {
        return metadataValue(notification, "ASOF", null, "asof");
    }

    static String created(@NonNull JsonObject notification) {
        return metadataValue(notification, "CREATED", "CREATED", null);
    }

    static String lastModified(@NonNull JsonObject notification) {
        return metadataValue(notification, "LAST_MODIFIED", "LAST_MODIFIED", null);
    }

    static String parentOid(@NonNull JsonObject notification) {
        return metadataValue(notification, "PARENT_OID", "parent_oid", null);
    }

    /**
     * Uses the current root envelope whenever it is present. Legacy payload fields are consulted
     * only when that envelope is absent, so application data is never treated as metadata under
     * the current protocol.
     */
    @NonNull
    private static String metadataValue(@NonNull JsonObject notification, @NonNull String rootKey,
                                        String legacyBodyKey, String legacyMetadataKey) {
        if (hasRootMetadata(notification)) {
            return stringValue(notification.getJsonObject("_metadata"), rootKey);
        }

        JsonObject body = changedData(notification);
        String bodyValue = legacyBodyKey == null ? "" : stringValue(body, legacyBodyKey);
        if (!bodyValue.isEmpty() || legacyMetadataKey == null) {
            return bodyValue;
        }
        JsonObject legacyMetadata = body.getJsonObject("_metadata");
        return stringValue(legacyMetadata, legacyMetadataKey);
    }

    @NonNull
    private static String stringValue(JsonObject source, @NonNull String key) {
        if (source == null || !source.containsKey(key) || source.isNull(key)) {
            return "";
        }
        JsonValue value = source.get(key);
        return value instanceof JsonString ? ((JsonString) value).getString() : value.toString();
    }
}
