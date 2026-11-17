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

package com.oracle.mobile.fusabase.oracledb.offline;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.Timestamp;
import com.oracle.mobile.fusabase.oracledb.Blob;
import com.oracle.mobile.fusabase.oracledb.FieldValue;
import com.oracle.mobile.fusabase.oracledb.FieldValueOperationEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Serializes values stored in the offline SQLite schema. */
final class OfflineValueSerializer {

    private static final String TYPE_KEY = "__fusabase_offline_type";
    private static final String VALUE_KEY = "value";
    private static final String TIMESTAMP_TYPE = "timestamp";
    private static final String DATE_TYPE = "date";
    private static final String INSTANT_TYPE = "instant";
    private static final String BYTES_TYPE = "bytes";
    private static final String BLOB_TYPE = "blob";
    private static final String LONG_TYPE = "long";
    private static final String FIELD_VALUE_TYPE = "fieldValue";

    private OfflineValueSerializer() {
    }

    @NonNull
    static String encode(@NonNull Object value) {
        try {
            return new JSONObject().put(VALUE_KEY, toJsonValue(value)).toString();
        } catch (JSONException exception) {
            throw new IllegalArgumentException("Unable to serialize offline value", exception);
        }
    }

    @NonNull
    static Object decode(@NonNull String serialized) {
        try {
            return fromJsonValue(new JSONObject(serialized).get(VALUE_KEY));
        } catch (JSONException exception) {
            throw new IllegalArgumentException("Unable to deserialize offline value", exception);
        }
    }

    @NonNull
    private static Object toJsonValue(Object value) throws JSONException {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value instanceof String || value instanceof Boolean || value instanceof Integer
                || value instanceof Double || value instanceof Float) {
            return value;
        }
        if (value instanceof Long) {
            return typedValue(LONG_TYPE).put(VALUE_KEY, value);
        }
        if (value instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) value;
            JSONObject result = typedValue(TIMESTAMP_TYPE);
            result.put("seconds", timestamp.getSeconds());
            result.put("nanoseconds", timestamp.getNanoseconds());
            return result;
        }
        if (value instanceof Date) {
            return typedValue(DATE_TYPE).put(VALUE_KEY, ((Date) value).getTime());
        }
        if (value instanceof Instant) {
            return typedValue(INSTANT_TYPE).put(VALUE_KEY, value.toString());
        }
        if (value instanceof byte[]) {
            return typedValue(BYTES_TYPE).put(VALUE_KEY,
                    Base64.getEncoder().encodeToString((byte[]) value));
        }
        if (value instanceof Blob) {
            return typedValue(BLOB_TYPE).put(VALUE_KEY,
                    Base64.getEncoder().encodeToString(((Blob) value).toBytes()));
        }
        if (value instanceof FieldValue) {
            FieldValue fieldValue = (FieldValue) value;
            return typedValue(FIELD_VALUE_TYPE)
                    .put("operation", FieldValueOperationEncoder.operationName(fieldValue))
                    .put("values", toJsonValue(Arrays.asList(
                            FieldValueOperationEncoder.values(fieldValue))));
        }
        if (value instanceof Map) {
            JSONObject result = new JSONObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("Offline map keys must be strings");
                }
                result.put((String) entry.getKey(), toJsonValue(entry.getValue()));
            }
            return result;
        }
        if (value instanceof Iterable) {
            JSONArray result = new JSONArray();
            for (Object item : (Iterable<?>) value) {
                result.put(toJsonValue(item));
            }
            return result;
        }
        if (value.getClass().isArray()) {
            JSONArray result = new JSONArray();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                result.put(toJsonValue(Array.get(value, index)));
            }
            return result;
        }
        throw new IllegalArgumentException("Unsupported offline value type: "
                + value.getClass().getName());
    }

    @NonNull
    private static JSONObject typedValue(@NonNull String type) throws JSONException {
        return new JSONObject().put(TYPE_KEY, type);
    }

    @NonNull
    private static Object fromJsonValue(Object value) throws JSONException {
        if (value == JSONObject.NULL) {
            return null;
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            List<Object> result = new ArrayList<>(array.length());
            for (int index = 0; index < array.length(); index++) {
                result.add(fromJsonValue(array.get(index)));
            }
            return result;
        }
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            String type = object.optString(TYPE_KEY, null);
            if (type != null) {
                return decodeTypedValue(type, object);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                result.put(key, fromJsonValue(object.get(key)));
            }
            return result;
        }
        return value;
    }

    @NonNull
    private static Object decodeTypedValue(@NonNull String type, @NonNull JSONObject object)
            throws JSONException {
        switch (type) {
            case TIMESTAMP_TYPE:
                return new Timestamp(object.getLong("seconds"), object.getInt("nanoseconds"));
            case DATE_TYPE:
                return new Date(object.getLong(VALUE_KEY));
            case INSTANT_TYPE:
                return Instant.parse(object.getString(VALUE_KEY));
            case LONG_TYPE:
                return object.getLong(VALUE_KEY);
            case BYTES_TYPE:
                return Base64.getDecoder().decode(object.getString(VALUE_KEY));
            case BLOB_TYPE:
                return Blob.fromBytes(Base64.getDecoder().decode(object.getString(VALUE_KEY)));
            case FIELD_VALUE_TYPE:
                return decodeFieldValue(object);
            default:
                throw new IllegalArgumentException("Unsupported offline value marker: " + type);
        }
    }

    @NonNull
    private static FieldValue decodeFieldValue(@NonNull JSONObject object) throws JSONException {
        String operation = object.optString("operation", null);
        if (operation == null || operation.isEmpty()) {
            throw new IllegalArgumentException("Offline FieldValue operation is missing");
        }
        Object decodedValues = fromJsonValue(object.get("values"));
        if (!(decodedValues instanceof List)) {
            throw new IllegalArgumentException("Offline FieldValue values must be an array");
        }
        List<?> values = (List<?>) decodedValues;
        switch (operation) {
            case "arrayRemove":
                return FieldValue.arrayRemove(values.toArray());
            case "arrayUnion":
                return FieldValue.arrayUnion(values.toArray());
            case "delete":
                requireNoValues(operation, values);
                return FieldValue.delete();
            case "deleteVector":
                requireNoValues(operation, values);
                return FieldValue.deleteVector();
            case "increment":
                if (values.size() != 1 || !(values.get(0) instanceof Number)) {
                    throw new IllegalArgumentException("Offline increment requires one number");
                }
                Number increment = (Number) values.get(0);
                return increment instanceof Double || increment instanceof Float
                        ? FieldValue.increment(increment.doubleValue())
                        : FieldValue.increment(increment.longValue());
            case "servertimestamp":
                requireNoValues(operation, values);
                return FieldValue.serverTimestamp();
            default:
                throw new IllegalArgumentException("Unsupported offline FieldValue operation: "
                        + operation);
        }
    }

    private static void requireNoValues(@NonNull String operation, @NonNull List<?> values) {
        if (!values.isEmpty()) {
            throw new IllegalArgumentException("Offline " + operation + " cannot have values");
        }
    }
}
