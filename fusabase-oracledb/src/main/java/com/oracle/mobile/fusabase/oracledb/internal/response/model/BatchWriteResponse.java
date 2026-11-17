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
import androidx.annotation.RestrictTo;

import java.util.Objects;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/** Typed result for a batch write response. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class BatchWriteResponse implements DocumentResponse {
    private final JsonObject payload;
    private final JsonArray results;

    public BatchWriteResponse(@NonNull JsonObject payload, @NonNull JsonArray results) {
        this.payload = Objects.requireNonNull(payload, "Response payload cannot be null");
        this.results = Objects.requireNonNull(results, "Response results cannot be null");
    }

    @NonNull
    @Override
    public JsonObject getPayload() {
        return payload;
    }

    @NonNull
    public JsonArray getResults() {
        return results;
    }
}
