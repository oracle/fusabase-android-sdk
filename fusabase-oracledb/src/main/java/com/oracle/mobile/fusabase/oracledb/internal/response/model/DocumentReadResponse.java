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


package com.oracle.mobile.fusabase.oracledb.internal.response.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Objects;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/** Typed result for a document read, retaining the raw envelope for later domain conversion. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentReadResponse implements DocumentResponse {
    private final JsonObject payload;
    private final JsonArray records;
    @Nullable
    private final JsonObject document;
    @Nullable
    private final String oid;
    @Nullable
    private final String version;
    private final boolean exists;
    private final boolean authorizationMasked;

    public DocumentReadResponse(@NonNull JsonObject payload,
                                @NonNull JsonArray records,
                                @Nullable JsonObject document,
                                @Nullable String oid,
                                @Nullable String version,
                                boolean exists) {
        this(payload, records, document, oid, version, exists, false);
    }

    public DocumentReadResponse(@NonNull JsonObject payload,
                                @NonNull JsonArray records,
                                @Nullable JsonObject document,
                                @Nullable String oid,
                                @Nullable String version,
                                boolean exists,
                                boolean authorizationMasked) {
        this.payload = Objects.requireNonNull(payload, "Response payload cannot be null");
        this.records = Objects.requireNonNull(records, "Response records cannot be null");
        this.document = document;
        this.oid = oid;
        this.version = version;
        this.exists = exists;
        this.authorizationMasked = authorizationMasked;
    }

    @NonNull
    @Override
    public JsonObject getPayload() {
        return payload;
    }

    @NonNull
    public JsonArray getRecords() {
        return records;
    }

    @Nullable
    public JsonObject getDocument() {
        return document;
    }

    @Nullable
    public String getOid() {
        return oid;
    }

    @Nullable
    public String getVersion() {
        return version;
    }

    public boolean exists() {
        return exists;
    }

    /** Returns whether this empty read represents a masked authorization denial. */
    public boolean isAuthorizationMasked() {
        return authorizationMasked;
    }
}
