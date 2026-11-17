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


package com.oracle.mobile.fusabase.oracledb.internal.response.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.internal.error.ErrorCategory;
import com.oracle.mobile.fusabase.oracledb.internal.error.InternalOperationException;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.BatchWriteResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentDeleteResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentReadResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentWriteResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.QueryResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.TransactionResponse;

import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/** Maps the common ORDS response envelope into operation-specific immutable result models. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentResponseMapper implements ResponseMapper<JsonObject, DocumentResponse> {

    /** Maps a response using document-read semantics. */
    @NonNull
    @Override
    public DocumentReadResponse map(@NonNull JsonObject input) throws InternalOperationException {
        return mapRead(input);
    }

    @NonNull
    public DocumentReadResponse mapRead(@NonNull JsonObject input)
            throws InternalOperationException {
        JsonObject payload = requirePayload(input);
        JsonArray records = records(payload);
        if (records.isEmpty()) {
            return new DocumentReadResponse(payload, records, null, null, null, false);
        }

        JsonObject record = objectAt(records, 0);
        JsonObject source = objectField(record, "osons");
        if (source == null) {
            source = record;
        }
        JsonObject document = objectField(source, "DOCUMENT");
        if (document == null) {
            document = objectField(source, "DATA");
        }
        if (document == null) {
            // Relational responses expose the document fields directly under OSONS.
            document = source;
        }

        String oid = firstString(source, record, payload, "OID", "oid");
        String version = firstString(source, record, payload, "VERSION", "version");
        return new DocumentReadResponse(payload, records, document, oid, version, true);
    }

    @NonNull
    public DocumentWriteResponse mapWrite(@NonNull JsonObject input)
            throws InternalOperationException {
        JsonObject payload = requirePayload(input);
        JsonArray records = records(payload);
        @Nullable JsonObject firstRecord = records.isEmpty() ? null : objectAt(records, 0);
        @Nullable JsonObject source = firstRecord == null ? null : objectField(firstRecord, "osons");
        String oid = firstString(source, firstRecord, payload, "OID", "oid");
        String version = firstString(source, firstRecord, payload, "VERSION", "version");
        return new DocumentWriteResponse(payload, oid, version);
    }

    @NonNull
    public DocumentDeleteResponse mapDelete(@NonNull JsonObject input)
            throws InternalOperationException {
        return new DocumentDeleteResponse(requirePayload(input), true);
    }

    @NonNull
    public QueryResponse mapQuery(@NonNull JsonObject input) throws InternalOperationException {
        JsonObject payload = requirePayload(input);
        return new QueryResponse(payload, records(payload));
    }

    @NonNull
    public BatchWriteResponse mapBatchWrite(@NonNull JsonObject input)
            throws InternalOperationException {
        JsonObject payload = requirePayload(input);
        return new BatchWriteResponse(payload, records(payload));
    }

    @NonNull
    public TransactionResponse mapTransaction(@NonNull JsonObject input)
            throws InternalOperationException {
        return new TransactionResponse(requirePayload(input));
    }

    @NonNull
    private static JsonObject requirePayload(@NonNull JsonObject input) {
        return Objects.requireNonNull(input, "Response payload cannot be null");
    }

    @NonNull
    private static JsonArray records(@NonNull JsonObject payload)
            throws InternalOperationException {
        JsonValue value = payload.get("ret");
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
            return Json.createArrayBuilder().build();
        }
        if (value.getValueType() != JsonValue.ValueType.ARRAY) {
            throw mappingFailure("Response records were not an array", null);
        }
        return value.asJsonArray();
    }

    @NonNull
    private static JsonObject objectAt(@NonNull JsonArray records, int index)
            throws InternalOperationException {
        JsonValue value = records.get(index);
        if (value.getValueType() != JsonValue.ValueType.OBJECT) {
            throw mappingFailure("Response record was not an object", null);
        }
        return value.asJsonObject();
    }

    @Nullable
    private static JsonObject objectField(@NonNull JsonObject object, @NonNull String name) {
        JsonValue value = object.get(name);
        return value != null && value.getValueType() == JsonValue.ValueType.OBJECT
                ? value.asJsonObject() : null;
    }

    @Nullable
    private static String firstString(@Nullable JsonObject first,
                                      @Nullable JsonObject second,
                                      @NonNull JsonObject third,
                                      @NonNull String primaryName,
                                      @NonNull String alternateName) {
        String result = valueAsString(first, primaryName);
        if (result == null) {
            result = valueAsString(first, alternateName);
        }
        if (result == null) {
            result = valueAsString(second, primaryName);
        }
        if (result == null) {
            result = valueAsString(second, alternateName);
        }
        if (result == null) {
            result = valueAsString(third, primaryName);
        }
        return result == null ? valueAsString(third, alternateName) : result;
    }

    @Nullable
    private static String valueAsString(@Nullable JsonObject object, @NonNull String name) {
        if (object == null) {
            return null;
        }
        JsonValue value = object.get(name);
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
            return null;
        }
        if (value.getValueType() == JsonValue.ValueType.STRING) {
            return ((jakarta.json.JsonString) value).getString();
        }
        return value.toString();
    }

    @NonNull
    private static InternalOperationException mappingFailure(@NonNull String message,
                                                             @Nullable Throwable cause) {
        return new InternalOperationException(message, ErrorCategory.MAPPING, false, null, cause);
    }
}
