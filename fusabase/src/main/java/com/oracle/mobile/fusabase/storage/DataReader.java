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

package com.oracle.mobile.fusabase.storage;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

class DataReader {

    DataReader() {

    }

    public static JsonValue getDataInJsonValue(Object Data) {

        // Should check the entered data for type
        JsonObjectBuilder builder = Json.createObjectBuilder();

        if(Data instanceof HashMap){
            for (Map.Entry<String, ?> entry :  ((HashMap<String, ?>) Data).entrySet()) {
                builder.add(entry.getKey(), getDataInJsonValue(entry.getValue()));
            }
            return builder.build();
        }
        else if(Data instanceof Integer)
        {
            return Json.createValue((int) Data);
        }
        else if(Data instanceof String)
        {
            return Json.createValue((String)Data);
        }
        else if(Data instanceof List)
        {
            return getDataInJsonArray((List<Object>)Data);
        }
        else if(Data instanceof Boolean)
        {
            return (Boolean) Data ? JsonValue.TRUE : JsonValue.FALSE;
        }
        else if(Data instanceof Double){
            return Json.createValue((Double)Data);
        }
        else
        {
            FusabaseLogger.w("Unknown Data type => " + Data.toString());
            // Not Supported Data Type. Should throw an exception.
        }
        // Empty object for now.
        return builder.build();
    }

    @NonNull
    public static JsonArray getDataInJsonArray (@NonNull List<?> array) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Object Data : array) {
            if (Data instanceof HashMap) {
                for (Map.Entry<String, ?> entry : ((HashMap<String, ?>) Data).entrySet()) {
                    builder.add(getDataInJsonValue(entry.getValue()));
                }
                return builder.build();
            } else if (Data instanceof Integer) {
                 builder.add(Json.createValue((int) Data));
            } else if (Data instanceof String) {
                 builder.add(Json.createValue((String) Data));
            } else if (Data instanceof List) {
                builder.add(getDataInJsonArray((List<?>) Data));
            } else if (Data instanceof Boolean) {
                builder.add((Boolean) Data ? JsonValue.TRUE : JsonValue.FALSE);
            } else if (Data instanceof Double) {
                builder.add(Json.createValue((Double) Data));
            }
//        else if(Data instanceof Date)
//        {
//
//        }
            else {
                FusabaseLogger.w("Unknown Data type => " + Data.toString());
                // Not Supported Data Type. Should throw an exception.
            }
        }
        return builder.build();
    }

    @NonNull
    public static HashMap<String, Object> getJsonObjectDataInMap(@NonNull JsonObject data) {
        HashMap<String, Object> resultMap = new HashMap<>();

        for (String key : data.keySet()) {
            JsonValue value = data.get(key);
            resultMap.put(key, DataReader.getJsonValue(value));
        }

        return resultMap;
    }

    private static Object getJsonValue(JsonValue value) {
        switch (value.getValueType()) {
            case STRING:
                return value.toString().replace("\"", ""); // Remove quotes from JsonString
            case NUMBER:
                return ((JsonNumber) value).numberValue(); // Can be BigDecimal, Long, etc.
            case TRUE:
                return true;
            case FALSE:
                return false;
            case NULL:
                return null;
            case ARRAY:
                JsonArray jsonArray = (JsonArray) value;
                List<Object> processedArray = new ArrayList<>();
                jsonArray.forEach(item -> processedArray.add(getJsonValue(item)));
                return processedArray; // Recursive for arrays
            case OBJECT:
                return getJsonObjectDataInMap((JsonObject) value); // Recursive for nested objects
            default:
                throw new IllegalArgumentException("Unsupported JsonValue type: " + value.getValueType());
        }
    }
}
