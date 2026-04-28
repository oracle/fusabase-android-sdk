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

import com.oracle.mobile.fusabase.Timestamp;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

/**
 * Utility class for reading and converting data between different formats,
 * including POJOs, Maps, and JSON values.
 */
class DataReader {

    /**
     * Converts the provided data into a HashMap, handling nested structures and POJOs.
     *
     * @param data the data to convert
     * @return the converted HashMap
     */
    @NonNull
    public static HashMap<String, Object> convertDataInHashMap(@NonNull Object data) {
        HashMap<String, Object> updatedData = new HashMap<>();
        Map<String, Object> dataMap;
        if (data instanceof Map) {
            dataMap = (Map<String, Object>) data;
            dataMap.forEach((key, value) -> {
                // Handle null values explicitly
                if (value == null) {
                    updatedData.put(key, null);
                } else if (value instanceof HashMap) {
                    updatedData.put(key, convertDataInHashMap(value));
                } else if (value instanceof Blob) {
                    // Convert Blob to byte array for storage
                    updatedData.put(key, ((Blob) value).toBytes());
                } else if (value instanceof String
                        || value instanceof Integer
                        || value instanceof Double
                        || value instanceof Date
                        || value instanceof Long
                        || value instanceof Timestamp
                        || value instanceof FieldValue
                        || value instanceof DenseVector
                        || value instanceof SparseVector) {
                    // These primitive types and special objects are handled directly
                    updatedData.put(key, value);
                } else if (value instanceof List) {
                    List<Object> processedList = ((List<?>) value).stream().map((item) -> {
                        // Handle null items in lists
                        if (item == null) {
                            return null;
                        } else if (item instanceof byte[]) {
                            return item;
                        } else if (item instanceof String
                                || item instanceof Integer
                                || item instanceof Double
                                || item instanceof Date
                                || item instanceof Long
                                || item instanceof Timestamp
                                || item instanceof FieldValue
                                || item instanceof DenseVector
                                || item instanceof SparseVector) {
                            return item;
                        } else if (item instanceof Map) {
                            return convertDataInHashMap(item);
                        } else if (isPojo(item.getClass())) {
                            return convertDataInHashMap(item);
                        }
                        return item;
                    }).collect(Collectors.toList());
                    updatedData.put(key, processedList);
                } else if (isPojo(value.getClass())) {
                    updatedData.put(key, convertDataInHashMap(value));
                } else {
                    updatedData.put(key, value);
                }
            });
        } else if (data != null && isPojo(data.getClass())) {
            return getPOJOInHashMap(data);
        } else if (data == null) {
            return new HashMap<>();
        } else {
            FusabaseLogger.e("Unsupported data type provided to convertDataInHashMap: " + data.getClass());
            return new HashMap<>();
        }
        return updatedData;
    }

    /**
     * Converts a POJO into a HashMap.
     *
     * @param data the POJO to convert
     * @return the HashMap representation
     */
    public static HashMap<String, Object> getPOJOInHashMap(Object data) {
        return serializePojoToHashMap(data, data.getClass());
    }

    /**
     * Converts data into a JsonValue.
     *
     * @param data the data to convert
     * @return the JsonValue representation
     */
    public static JsonValue getDataInJsonValue(Object data) {
        return getJsonValueForObject(data);
    }

    /**
     * Helper method to convert an object to JsonValue, used in both single value and array contexts.
     *
     * @param data the object to convert
     * @return the JsonValue representation
     */
    private static JsonValue getJsonValueForObject(Object data) {
        if (data == null) {
            return JsonValue.NULL;
        } else if (data instanceof Map) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            for (Map.Entry<String, ?> entry : ((Map<String, ?>) data).entrySet()) {
                builder.add(entry.getKey(), getJsonValueForObject(entry.getValue()));
            }
            return builder.build();
        } else if (data instanceof List) {
            return getDataInJsonArray((List<Object>) data);
        } else if (data instanceof byte[]) {
            List<Long> byteList = new ArrayList<>();
            for (byte b : (byte[]) data) {
                byteList.add((long) (b & 0xFF));
            }
            return getDataInJsonArray(byteList);
        } else if (data.getClass().isArray()) {
            return getDataInJsonArray(Arrays.asList((Object[]) data));
        } else if (data instanceof Number) {
            return Json.createValue(((Number) data).doubleValue());
        } else if (data instanceof Integer) {
            return Json.createValue(((Integer) data).longValue());
        } else if (data instanceof Long) {
            return Json.createValue((Long) data);
        } else if (data instanceof Double) {
            return Json.createValue((Double) data);
        } else if (data instanceof String) {
            return Json.createValue((String) data);
        } else if (data instanceof Boolean) {
            return (Boolean) data ? JsonValue.TRUE : JsonValue.FALSE;
        } else if (data instanceof Date) {
            return Json.createValue(new Timestamp((Date) data).getString());
        } else if (data instanceof Timestamp) {
            return Json.createValue(((Timestamp) data).getString());
        } else if (isPojo(data.getClass())) {
            return serializePojoToJsonValue(data, data.getClass());
        } else {
            return Json.createValue(data.toString());
        }
    }

    /**
     * Serializes a POJO into a HashMap using reflection.
     *
     * @param pojo the POJO to serialize
     * @param clazz the class of the POJO
     * @return the HashMap representation
     */
    public static HashMap<String, Object> serializePojoToHashMap(Object pojo, Class<?> clazz) {
        HashMap<String, Object> pojoHashMap = new HashMap<>();
        Field[] fields = pojo.getClass().getDeclaredFields();

        for (Field field : fields) {
            String fieldName = field.getName();
            String getterName = (field.getType() == boolean.class ? "is" : "get") +
                    Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                Method getterMethod = clazz.getMethod(getterName); // Find getter method
                Object value = getterMethod.invoke(pojo); // Call the getter
                pojoHashMap.put(field.getName(), value);
            } catch (NoSuchMethodException ignored) {
                // Ignore if there is no method as such
            } catch (InvocationTargetException | IllegalAccessException e) {
                FusabaseLogger.e("Failed to serialize POJO to HashMap: " + e.getMessage());
                return new HashMap<>();
            }
        }

        return pojoHashMap; // Returns a HashMap
    }

    /**
     * Serializes a POJO into a JsonValue using reflection.
     *
     * @param pojo the POJO to serialize
     * @param clazz the class of the POJO
     * @return the JsonValue representation
     */
    public static JsonValue serializePojoToJsonValue(Object pojo, Class<?> clazz) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        Field[] fields = pojo.getClass().getDeclaredFields();

        for (Field field : fields) {
            String fieldName = field.getName();
            String getterName = (field.getType() == boolean.class ? "is" : "get") +
                    Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                Method getterMethod = clazz.getMethod(getterName); // Find getter method
                Object value = getterMethod.invoke(pojo); // Call the getter
                jsonObjectBuilder.add(field.getName(), getDataInJsonValue(value));
            } catch (NoSuchMethodException ignored) {
                // Ignore if there is no method as such
            } catch (InvocationTargetException | IllegalAccessException e) {
                FusabaseLogger.e("Failed to serialize POJO to JsonValue: " + e.getMessage());
                return JsonValue.NULL;
            }
        }

        return jsonObjectBuilder.build(); // Returns a JsonValue
    }

    /**
     * Checks if the class has a public no-argument constructor.
     *
     * @param clazz the class to check
     * @return true if it has a public no-arg constructor, false otherwise
     */
    private static boolean hasPublicNoArgConstructor(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0 && Modifier.isPublic(constructor.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the class is a POJO (has a public no-argument constructor).
     *
     * @param clazz the class to check
     * @return true if it's a POJO, false otherwise
     */
    public static boolean isPojo(Class<?> clazz) {
        return hasPublicNoArgConstructor(clazz);
    }

    /**
     * Converts a list of data into a JsonArray.
     *
     * @param array the list to convert
     * @return the JsonArray
     */
    @NonNull
    public static JsonArray getDataInJsonArray(@NonNull List<?> array) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Object data : array) {
            try {
                builder.add(getJsonValueForObject(data));
            } catch (Exception e) {
                FusabaseLogger.e("Unknown data type in array: " + (data != null ? data.getClass() : "null"));
                builder.add(JsonValue.NULL);
            }
        }
        return builder.build();
    }

    /**
     * Adds an index to the data in the JsonObject.
     *
     * @param data the JsonObject to modify
     * @return the modified JsonObject
     */
    protected static JsonObject addIndexToData(@NonNull JsonObject data) {
        JsonArray retArray = data.getJsonArray("ret");
        JsonArrayBuilder updatedRetArray = Json.createArrayBuilder();

        for (int i = 0; i < retArray.size(); i++) {
            JsonValue item = retArray.get(i);

            if (item.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject itemObj = item.asJsonObject();

                if (itemObj.containsKey("osons") &&
                        itemObj.get("osons").getValueType() == JsonValue.ValueType.OBJECT) {
                    JsonObject osons = itemObj.getJsonObject("osons");

                    JsonObjectBuilder osonsBuilder = Json.createObjectBuilder(osons);
                    osonsBuilder.add("INDEX", i);

                    JsonObjectBuilder updatedItem = Json.createObjectBuilder();
                    for (String key : itemObj.keySet()) {
                        if (key.equals("osons")) {
                            updatedItem.add("osons", osonsBuilder.build());
                        } else {
                            updatedItem.add(key, itemObj.get(key));
                        }
                    }
                    updatedRetArray.add(updatedItem.build());
                } else {
                    // case for joins
                    JsonObjectBuilder dataBuilder = Json.createObjectBuilder();

                    dataBuilder.add("INDEX", i);
                    dataBuilder.add("osons", Json.createObjectBuilder().add("DOCUMENT", itemObj.get("data")));
                    updatedRetArray.add(dataBuilder.build());
                }
            }
        }

        JsonObjectBuilder updatedData = Json.createObjectBuilder();
        for (String key : data.keySet()) {
            if (key.equals("ret")) {
                updatedData.add("ret", updatedRetArray.build());
            }
        }

        return updatedData.build();
    }

    /**
     * Converts a JsonObject into a HashMap.
     *
     * @param data the JsonObject to convert
     * @return the HashMap representation
     */
    @NonNull
    public static HashMap<String, Object> getJsonObjectDataInMap(@NonNull JsonObject data) {
        HashMap<String, Object> resultMap = new HashMap<>();

        for (String key : data.keySet()) {
            JsonValue value = data.get(key);
            resultMap.put(key, DataReader.getJsonValueWithTypeHint(value, key));
        }

        return resultMap;
    }

    /**
     * Checks if the string is a valid timestamp.
     *
     * @param timestamp the string to check
     * @return true if it's a timestamp, false otherwise
     */
    protected static boolean isTimestamp(@NonNull String timestamp) {
        String updatedTimestamp = timestamp.endsWith("Z") ? timestamp : timestamp + "Z";
        try {
            Instant.parse(updatedTimestamp);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Converts a JsonValue into an Object.
     *
     * @param value the JsonValue to convert
     * @return the Object representation
     */
    private static Object getJsonValue(JsonValue value) {
        switch (value.getValueType()) {
            case STRING:
                String literal = value.toString().replace("\"", "");  // Remove quotes from JsonString
                // Check if it's a timestamp
                // if it's a timestamp of format 2025-08-25T10:15:30.123 without Zulu
                // We will assume it's UTC and add Zulu
                // Valid Timestamp 2025-08-25T10:15:30.123Z
                if (isTimestamp(literal)) {
                    // Always return Timestamp for timestamp-like strings to maintain consistency
                    // This covers CREATED, LAST_MODIFIED and other system timestamp fields
                    return literal.endsWith("Z") ?
                            new Timestamp(Instant.parse(literal)) :
                            new Timestamp(Instant.parse(literal + "Z"));
                }
                return literal;
            case NUMBER:
                JsonNumber num = (JsonNumber) value;
                if (num.isIntegral()) {
                    return num.longValue();
                } else {
                    return num.doubleValue();
                }
            case TRUE:
                return true;
            case FALSE:
                return false;
            case NULL:
                return null;
            case ARRAY:
                JsonArray jsonArray = (JsonArray) value;
                List<Object> arrayList = jsonArray.getValuesAs(DataReader::getJsonValue);
                return arrayList; // Recursive for arrays
            case OBJECT:
                return getJsonObjectDataInMap((JsonObject) value); // Recursive for nested objects
            default:
                FusabaseLogger.e("Unsupported JsonValue type: " + value.getValueType());
                return null;
        }
    }

    /**
     * Converts a JsonValue into an Object with type preservation hints.
     * This method attempts to preserve the original type when possible.
     *
     * @param value the JsonValue to convert
     * @param fieldName the field name (used for type hints)
     * @return the Object representation
     */
    public static Object getJsonValueWithTypeHint(JsonValue value, String fieldName) {
        if (fieldName == null) {
            return getJsonValue(value);
        }

        switch (value.getValueType()) {
            case STRING:
                String literal = value.toString().replace("\"", "");
                // Check if it's a timestamp
                if (isTimestamp(literal)) {
                    Timestamp timestamp = literal.endsWith("Z") ?
                            new Timestamp(Instant.parse(literal)) :
                            new Timestamp(Instant.parse(literal + "Z"));

                    // For system fields that are always timestamps, return Timestamp
                    if (isSystemTimestampField(fieldName)) {
                        return timestamp;
                    }

                    // For user fields, we need to determine if it was originally a Date or Timestamp
                    // Since we can't determine the original type from JSON, we return Timestamp
                    // The DocumentSnapshot methods will handle conversion as needed
                    return timestamp;
                }
                return literal;
            default:
                return getJsonValue(value);
        }
    }

    /**
     * Checks if the field name represents a system timestamp field that should always be Timestamp.
     *
     * @param fieldName the field name to check
     * @return true if it's a system timestamp field, false otherwise
     */
    private static boolean isSystemTimestampField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        String upperFieldName = fieldName.toUpperCase();
        return upperFieldName.contains("CREATED") ||
               upperFieldName.contains("LAST_MODIFIED") ||
               upperFieldName.contains("MODIFIED") ||
               upperFieldName.equals("VERSION") ||
               upperFieldName.equals("_METADATA");
    }

    /**
     * Checks if the list represents a byte array (all elements are Longs between 0 and 255).
     *
     * @param list the list to check
     * @return true if it's a byte array, false otherwise
     */
    private static boolean isByteArray(List<Object> list) {
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (Object item : list) {
            if (!(item instanceof Long)) {
                return false;
            }
            long value = (Long) item;
            if (value < 0 || value > 255) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a list of Longs (representing byte values) to a byte array.
     *
     * @param list the list of Longs
     * @return the byte array
     */
    private static byte[] convertToByteArray(List<Object> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = ((Long) list.get(i)).byteValue();
        }
        return bytes;
    }
}
