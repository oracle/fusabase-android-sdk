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


package com.oracle.mobile.fusabase.oracledb.internal.validation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.Timestamp;
import com.oracle.mobile.fusabase.oracledb.Blob;
import com.oracle.mobile.fusabase.oracledb.DenseVector;
import com.oracle.mobile.fusabase.oracledb.FieldValue;
import com.oracle.mobile.fusabase.oracledb.SparseVector;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts public document input into an isolated internal value tree.
 *
 * <p>This class deliberately does not build JSON or HTTP payloads. Its only responsibility is
 * to turn caller-owned maps, lists, arrays, and POJOs into values that can safely be retained by
 * the offline store or an in-flight operation.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentInputNormalizer {
    private DocumentInputNormalizer() { }

    /**
     * Creates a mutable deep copy of a document-shaped value.
     *
     * @param value a Map or POJO representing document data
     * @return a recursively copied map
     * @throws IllegalArgumentException when the value cannot be represented as document data
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(@NonNull Object value) {
        Object converted = convert(value, new IdentityHashMap<>());
        if (!(converted instanceof Map)) {
            throw new IllegalArgumentException("Offline write data must be a Map or POJO object");
        }
        return (Map<String, Object>) converted;
    }

    /**
     * Creates a recursively immutable copy suitable for retaining in an operation command.
     *
     * @param value a Map or POJO representing document data
     * @return an immutable recursively copied map
     */
    @NonNull
    public static Map<String, Object> toImmutableMap(@NonNull Object value) {
        return freezeMap(toMap(value));
    }

    @Nullable
    private static Object convert(@Nullable Object value,
                                  @NonNull IdentityHashMap<Object, Boolean> seen) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return ((byte[]) value).clone();
        }
        if (value instanceof Date) {
            return new Date(((Date) value).getTime());
        }
        if (isScalar(value)) {
            return value;
        }
        if (seen.containsKey(value)) {
            throw new IllegalArgumentException("Offline write data cannot contain cycles");
        }
        if (value instanceof Map) {
            seen.put(value, true);
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new IllegalArgumentException("Offline write map keys must be strings");
                }
                converted.put((String) entry.getKey(), convert(entry.getValue(), seen));
            }
            seen.remove(value);
            return converted;
        }
        if (value instanceof Collection) {
            seen.put(value, true);
            List<Object> converted = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                converted.add(convert(item, seen));
            }
            seen.remove(value);
            return converted;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            seen.put(value, true);
            List<Object> converted = new ArrayList<>();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                converted.add(convert(Array.get(value, i), seen));
            }
            seen.remove(value);
            return converted;
        }
        seen.put(value, true);
        Map<String, Object> converted = convertPojo(value, seen);
        seen.remove(value);
        return converted;
    }

    private static boolean isScalar(@NonNull Object value) {
        return value instanceof String ||
                value instanceof Number ||
                value instanceof Boolean ||
                value instanceof Character ||
                value instanceof Timestamp ||
                value instanceof Instant ||
                value instanceof FieldValue ||
                value instanceof Blob ||
                value instanceof DenseVector ||
                value instanceof SparseVector ||
                value.getClass().isEnum();
    }

    @NonNull
    private static Map<String, Object> convertPojo(@NonNull Object value,
                                                   @NonNull IdentityHashMap<Object, Boolean> seen) {
        Map<String, Object> converted = new LinkedHashMap<>();
        for (Method method : value.getClass().getMethods()) {
            if (!isPojoGetter(method)) {
                continue;
            }
            try {
                converted.put(getPropertyName(method), convert(method.invoke(value), seen));
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Cannot read offline write property " +
                        method.getName(), e);
            }
        }
        for (Field field : value.getClass().getDeclaredFields()) {
            if (!isPojoField(field) || converted.containsKey(field.getName())) {
                continue;
            }
            try {
                field.setAccessible(true);
                converted.put(field.getName(), convert(field.get(value), seen));
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Cannot read offline write field " +
                        field.getName(), e);
            }
        }
        return converted;
    }

    private static boolean isPojoGetter(@NonNull Method method) {
        if (method.getParameterTypes().length != 0 || method.getReturnType() == Void.TYPE ||
                Modifier.isStatic(method.getModifiers()) ||
                method.getDeclaringClass() == Object.class) {
            return false;
        }
        String name = method.getName();
        return (name.startsWith("get") && name.length() > 3 && !"getClass".equals(name)) ||
                (name.startsWith("is") && name.length() > 2 &&
                        (method.getReturnType() == Boolean.TYPE ||
                                method.getReturnType() == Boolean.class));
    }

    private static boolean isPojoField(@NonNull Field field) {
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers);
    }

    @NonNull
    private static String getPropertyName(@NonNull Method method) {
        String name = method.getName();
        String property = name.startsWith("is") ? name.substring(2) : name.substring(3);
        if (property.length() == 1) {
            return property.toLowerCase();
        }
        return Character.toLowerCase(property.charAt(0)) + property.substring(1);
    }

    @NonNull
    private static Map<String, Object> freezeMap(@NonNull Map<String, Object> map) {
        Map<String, Object> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            frozen.put(entry.getKey(), freeze(entry.getValue()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    @Nullable
    private static Object freeze(@Nullable Object value) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            Map<String, Object> typedMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                typedMap.put((String) entry.getKey(), freeze(entry.getValue()));
            }
            return Collections.unmodifiableMap(typedMap);
        }
        if (value instanceof List) {
            List<Object> frozen = new ArrayList<>();
            for (Object item : (List<?>) value) {
                frozen.add(freeze(item));
            }
            return Collections.unmodifiableList(frozen);
        }
        return value;
    }
}
