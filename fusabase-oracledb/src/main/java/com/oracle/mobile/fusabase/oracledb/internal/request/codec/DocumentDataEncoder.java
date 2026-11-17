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


package com.oracle.mobile.fusabase.oracledb.internal.request.codec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.Timestamp;
import com.oracle.mobile.fusabase.oracledb.Blob;
import com.oracle.mobile.fusabase.oracledb.FieldValue;
import com.oracle.mobile.fusabase.oracledb.FieldValueOperationEncoder;
import com.oracle.mobile.fusabase.oracledb.internal.validation.DocumentInputNormalizer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/** Converts normalized document values and field operations into JSON-compatible values. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentDataEncoder {
    private static final String SERVER_TIMESTAMP_MARKER = "#servertimestamp";

    private final EmbeddingEncoder embeddingEncoder;

    public DocumentDataEncoder() {
        this(new EmbeddingEncoder());
    }

    public DocumentDataEncoder(@NonNull EmbeddingEncoder embeddingEncoder) {
        this.embeddingEncoder = java.util.Objects.requireNonNull(embeddingEncoder,
                "Embedding encoder cannot be null");
    }

    /** Encodes a map without retaining or mutating its caller-owned values. */
    @NonNull
    public JsonObject encodeMap(@NonNull Map<String, ?> values) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("Document map keys must be strings");
            }
            builder.add(entry.getKey(), encodeValue(entry.getValue()));
        }
        return builder.build();
    }

    /** Encodes one document value recursively. */
    @NonNull
    @SuppressWarnings("unchecked")
    public JsonValue encodeValue(@Nullable Object value) {
        if (value == null) {
            return JsonValue.NULL;
        }
        if (value instanceof JsonValue) {
            return (JsonValue) value;
        }
        if (value instanceof Blob) {
            return encodeValue(((Blob) value).toBytes());
        }
        if (value instanceof byte[]) {
            List<Long> bytes = new ArrayList<>(((byte[]) value).length);
            for (byte item : (byte[]) value) {
                bytes.add((long) (item & 0xFF));
            }
            return encodeValue(bytes);
        }
        if (value instanceof Date) {
            return Json.createValue(new Timestamp((Date) value).getString());
        }
        if (value instanceof Timestamp) {
            return Json.createValue(((Timestamp) value).getString());
        }
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                throw new IllegalArgumentException("Document numbers must be finite");
            }
            // Preserve the existing DataReader wire representation for numeric values.
            return Json.createValue(number);
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? JsonValue.TRUE : JsonValue.FALSE;
        }
        if (value instanceof Character || value instanceof String || value.getClass().isEnum()) {
            return Json.createValue(String.valueOf(value));
        }
        if (embeddingEncoder.isEmbeddingInput(value)) {
            return encodeMap(embeddingEncoder.encode(value));
        }
        if (value instanceof FieldValue) {
            throw new IllegalArgumentException(
                    "FieldValue must be encoded as a top-level document operation");
        }
        if (value instanceof Map) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("Document map keys must be strings");
                }
                builder.add((String) entry.getKey(), encodeValue(entry.getValue()));
            }
            return builder.build();
        }
        if (value instanceof Collection) {
            jakarta.json.JsonArrayBuilder builder = Json.createArrayBuilder();
            for (Object item : (Collection<?>) value) {
                builder.add(encodeValue(item));
            }
            return builder.build();
        }
        if (value.getClass().isArray()) {
            jakarta.json.JsonArrayBuilder builder = Json.createArrayBuilder();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                builder.add(encodeValue(Array.get(value, index)));
            }
            return builder.build();
        }

        // Command creation already applies the canonical POJO conversion. This fallback keeps
        // the encoder useful for direct internal callers without exposing reflection elsewhere.
        return encodeMap(DocumentInputNormalizer.toMap(value));
    }

    /** Converts an existing remote JSON document into a mutable map for V1 merge preparation. */
    @NonNull
    public Map<String, Object> decodeObject(@NonNull JsonObject value) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> entry : value.entrySet()) {
            result.put(entry.getKey(), decodeValue(entry.getValue()));
        }
        return result;
    }

    /** Applies the V1 full-document FieldValue semantics and records server timestamps. */
    @SuppressWarnings("unchecked")
    public void applyV1Updates(@NonNull Map<String, Object> document,
                               @NonNull Map<String, Object> updates,
                               @NonNull List<String> serverTimestampFields) {
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String field = entry.getKey();
            if (field == null || field.trim().isEmpty()) {
                throw new IllegalArgumentException("Update field must not be empty");
            }
            List<String> segments = splitFieldPath(field);
            Map<String, Object> target = document;
            for (int index = 0; index < segments.size() - 1; index++) {
                String segment = segments.get(index);
                Object nested = target.get(segment);
                if (!(nested instanceof Map)) {
                    nested = new LinkedHashMap<String, Object>();
                    target.put(segment, nested);
                }
                target = (Map<String, Object>) nested;
            }
            String finalField = segments.get(segments.size() - 1);
            Object value = entry.getValue();
            if (value instanceof FieldValue) {
                applyFieldValue(target, finalField, (FieldValue) value, serverTimestampFields);
            } else if (value instanceof Blob) {
                target.put(finalField, ((Blob) value).toBytes());
            } else {
                target.put(finalField, value);
            }
        }
    }

    /** Encodes the V2 update-operation array, including embedding operations first. */
    @NonNull
    public List<Map<String, Object>> encodeV2UpdateOperations(
            @NonNull Map<String, Object> updates) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> embeddings = new LinkedHashMap<>();
        List<String> embeddingDeletes = new ArrayList<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (embeddingEncoder.isEmbeddingInput(entry.getValue())) {
                embeddings.put(entry.getKey(), embeddingEncoder.encode(entry.getValue()));
            } else if (entry.getValue() instanceof FieldValue &&
                    "deleteVector".equals(FieldValueOperationEncoder.operationName(
                            (FieldValue) entry.getValue()))) {
                embeddingDeletes.add(entry.getKey());
            }
        }
        if (!embeddings.isEmpty()) {
            Map<String, Object> operation = new LinkedHashMap<>();
            operation.put("field", "$embeddings");
            operation.put("op", "set");
            operation.put("value", embeddings);
            operation.put("valueType", "mapValue");
            result.add(operation);
        }
        if (!embeddingDeletes.isEmpty()) {
            Map<String, Object> operation = new LinkedHashMap<>();
            operation.put("field", "$embeddings");
            operation.put("op", "delete");
            operation.put("value", embeddingDeletes);
            operation.put("valueType", "arrayValue");
            result.add(operation);
        }

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (embeddingEncoder.isEmbeddingInput(entry.getValue()) ||
                    (entry.getValue() instanceof FieldValue &&
                            "deleteVector".equals(FieldValueOperationEncoder.operationName(
                                    (FieldValue) entry.getValue())))) {
                continue;
            }
            Map<String, Object> operation = new LinkedHashMap<>();
            operation.put("field", entry.getKey());
            Object value = entry.getValue();
            if (value instanceof FieldValue) {
                FieldValue fieldValue = (FieldValue) value;
                String operationName = FieldValueOperationEncoder.operationName(fieldValue);
                Object[] values = FieldValueOperationEncoder.values(fieldValue);
                operation.put("op", operationName);
                if ("arrayUnion".equals(operationName) ||
                        "arrayRemove".equals(operationName)) {
                    operation.put("value", Arrays.asList(values));
                } else if (values.length == 0) {
                    operation.put("value", "");
                } else {
                    operation.put("value", values[0]);
                }
            } else {
                operation.put("op", "set");
                operation.put("value", value instanceof Blob
                        ? ((Blob) value).toBytes() : value);
            }
            result.add(operation);
        }
        return result;
    }

    @NonNull
    private static List<String> splitFieldPath(@NonNull String field) {
        String[] rawSegments = field.split("\\.", -1);
        List<String> segments = new ArrayList<>(rawSegments.length);
        for (String segment : rawSegments) {
            if (segment.trim().isEmpty()) {
                throw new IllegalArgumentException("Update field path contains an empty segment");
            }
            segments.add(segment);
        }
        return segments;
    }

    private static void applyFieldValue(@NonNull Map<String, Object> target,
                                        @NonNull String field,
                                        @NonNull FieldValue value,
                                        @NonNull List<String> serverTimestampFields) {
        String operation = FieldValueOperationEncoder.operationName(value);
        Object[] values = FieldValueOperationEncoder.values(value);
        switch (operation) {
            case "delete":
            case "deleteVector":
                target.remove(field);
                break;
            case "increment":
                target.put(field, increment(target.get(field), values));
                break;
            case "arrayUnion":
                target.put(field, updateArray(target.get(field), values, true));
                break;
            case "arrayRemove":
                target.put(field, updateArray(target.get(field), values, false));
                break;
            case "servertimestamp":
                target.put(field, SERVER_TIMESTAMP_MARKER);
                serverTimestampFields.add(field);
                break;
            default:
                target.put(field, Arrays.asList(values));
                break;
        }
    }

    @NonNull
    private static Object increment(@Nullable Object current, @NonNull Object[] values) {
        if (values.length == 0 || !(values[0] instanceof Number)) {
            throw new IllegalArgumentException("Increment requires a numeric value");
        }
        Number delta = (Number) values[0];
        if (current == null) {
            return delta;
        }
        if (!(current instanceof Number)) {
            throw new IllegalArgumentException("Cannot increment a non-numeric field");
        }
        Number original = (Number) current;
        if (delta instanceof Double || delta instanceof Float ||
                original instanceof Double || original instanceof Float) {
            return original.doubleValue() + delta.doubleValue();
        }
        return original.longValue() + delta.longValue();
    }

    @NonNull
    private static List<Object> updateArray(@Nullable Object current,
                                             @NonNull Object[] values,
                                             boolean union) {
        if (!(current instanceof List)) {
            throw new IllegalArgumentException("Array FieldValue requires an existing list");
        }
        List<Object> updated = new ArrayList<>((List<Object>) current);
        if (union) {
            for (Object value : values) {
                if (!updated.contains(value)) {
                    updated.add(value);
                }
            }
        } else {
            for (Object value : values) {
                updated.remove(value);
            }
        }
        return updated;
    }

    @Nullable
    private Object decodeValue(@NonNull JsonValue value) {
        switch (value.getValueType()) {
            case NULL:
                return null;
            case TRUE:
                return true;
            case FALSE:
                return false;
            case STRING:
                return ((JsonString) value).getString();
            case NUMBER:
                JsonNumber number = (JsonNumber) value;
                return number.isIntegral() ? number.longValue() : number.doubleValue();
            case ARRAY:
                List<Object> array = new ArrayList<>();
                for (JsonValue item : (JsonArray) value) {
                    array.add(decodeValue(item));
                }
                return array;
            case OBJECT:
                return decodeObject((JsonObject) value);
            default:
                throw new IllegalArgumentException("Unsupported JSON value type");
        }
    }
}
