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

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *  Applies FieldValue sentinels to the offline local document view. 
 *
 * <p>Example:</p>
 * <pre>{@code
 * FusabaseOracledb db = FusabaseOracledb.getInstance();
 * }</pre>
 */
public final class OfflineFieldValueEvaluator {
    private OfflineFieldValueEvaluator() { }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * }</pre>
     */public static boolean isFieldValue(Object value) {
        return value instanceof FieldValue;
    }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * }</pre>
     */public static void apply(@NonNull Map<String, Object> target,
                             @NonNull String field,
                             @NonNull Object value) {
        if (!(value instanceof FieldValue)) {
            target.put(field, value);
            return;
        }

        FieldValue fieldValue = (FieldValue) value;
        switch (fieldValue.getOperation()) {
            case DELETE:
            case DELETE_VECTOR:
                target.remove(field);
                break;
            case INCREMENT:
                target.put(field, increment(target.get(field), fieldValue.getValues()[0]));
                break;
            case ARRAY_UNION:
                target.put(field, arrayUnion(target.get(field), fieldValue.getValues()));
                break;
            case ARRAY_REMOVE:
                target.put(field, arrayRemove(target.get(field), fieldValue.getValues()));
                break;
            case SERVER_TIMESTAMP:
                target.put(field, new Timestamp(Instant.now()));
                break;
            default:
                target.put(field, fieldValue.getValues());
                break;
        }
    }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * }</pre>
     */@NonNull
    public static Map<String, Object> materialize(@NonNull Map<String, Object> source) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                result.put(entry.getKey(), materialize((Map<String, Object>) value));
            } else if (value instanceof List) {
                result.put(entry.getKey(), materializeList((List<?>) value));
            } else {
                apply(result, entry.getKey(), value);
            }
        }
        return result;
    }

    @NonNull
    private static List<Object> materializeList(@NonNull List<?> source) {
        List<Object> result = new ArrayList<>();
        for (Object item : source) {
            if (item instanceof Map) {
                result.add(materialize((Map<String, Object>) item));
            } else if (item instanceof List) {
                result.add(materializeList((List<?>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    @NonNull
    private static Object increment(Object current, Object operand) {
        Number incrementBy = operand instanceof Number ? (Number) operand : 0L;
        if (!(current instanceof Number)) {
            return incrementBy;
        }
        Number currentNumber = (Number) current;
        if (currentNumber instanceof Double || currentNumber instanceof Float ||
                incrementBy instanceof Double || incrementBy instanceof Float) {
            return currentNumber.doubleValue() + incrementBy.doubleValue();
        }
        return currentNumber.longValue() + incrementBy.longValue();
    }

    @NonNull
    private static List<Object> arrayUnion(Object current, Object[] additions) {
        List<Object> result = current instanceof List ?
                new ArrayList<>((List<?>) current) : new ArrayList<>();
        LinkedHashSet<Object> existing = new LinkedHashSet<>(result);
        for (Object addition : additions) {
            if (!existing.contains(addition)) {
                result.add(addition);
                existing.add(addition);
            }
        }
        return result;
    }

    @NonNull
    private static List<Object> arrayRemove(Object current, Object[] removals) {
        List<Object> result = current instanceof List ?
                new ArrayList<>((List<?>) current) : new ArrayList<>();
        for (Object removal : removals) {
            result.removeIf(item -> Objects.equals(item, removal));
        }
        return result;
    }
}
