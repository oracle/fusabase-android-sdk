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
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.Timestamp;
import com.oracle.mobile.fusabase.utils.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link DocumentSnapshot} that is returned as part of a query result.
 *
 * <p>QueryDocumentSnapshot provides additional functionality for mapping document data
 * to Java objects using reflection. It extends {@link DocumentSnapshot} and adds methods
 * for converting document data to custom POJO (Plain Old Java Object) classes.</p>
 *
 * <p>This class is particularly useful when working with query results that need to be
 * mapped to strongly-typed Java objects. It supports automatic field mapping based on
 * field names and handles special annotations like {@link ServerTimestamp} for timestamp fields.</p>
 */
public class QueryDocumentSnapshot extends DocumentSnapshot {

    /** The underlying document containing the data */
    @NonNull
    private final Document document;

    /**
     * Constructs a new QueryDocumentSnapshot with the specified parameters.
     *
     * @param oradb The database instance
     * @param doc The document containing the data
     * @param docRef Reference to the document
     * @param path The path to the document
     * @param isFromCache Whether this snapshot came from cache
     * @param hasPendingWrites Whether there are pending writes for this document
     */
    protected QueryDocumentSnapshot(FusabaseOracledb oradb,
                                    @NonNull Document doc,
                                    @NonNull DocumentReference docRef,
                                    String path,
                                    boolean isFromCache,
                                    boolean hasPendingWrites) {
        super(oradb, doc, docRef, path, isFromCache, hasPendingWrites);
        this.document = doc;
    }

    /**
     * Returns the data contained in this document snapshot as a map.
     *
     * @return A map containing the document's field names and values
     */
    @NonNull
    public Map<String, Object> getData() {
        return this.document.getData();
    }

    /**
     * Returns the underlying document object.
     *
     * @return The document containing the data, or null if not available
     */
    @Nullable
    protected Document getDocument() {
        return this.document;
    }

    /**
     * Converts this document snapshot to a custom Java object using reflection.
     * This method maps document fields to object properties based on field names,
     * automatically calling appropriate setter methods. It supports special handling
     * for fields annotated with {@link ServerTimestamp}.
     *
     * @param <T> The type of object to convert to
     * @param valueType The class of the object to create
     * @return A new instance of the specified class populated with document data
     * @throws RuntimeException if the conversion fails or if ServerTimestamp fields
     *         are not of supported types (java.util.Date or com.oracle.mobile.fusabase.Timestamp)
     */
    @Override
    @NonNull
    public <T> T toObject(@NonNull Class<T> valueType) {
        try {
            T obj = valueType.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, Object> entry : this.document.getData().entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();

                // Creating the field set method name
                String setMethodName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

                Field field = valueType.getDeclaredField(fieldName);

                if (field.isAnnotationPresent(ServerTimestamp.class)) {
                    if (field.getType().getName().equals("java.util.Date")) {
                        fieldValue = Utils.convertStringToDate(this.document.getCreated());
                    } else if (field.getType().getName().equals("com.oracle.mobile.fusabase.Timestamp")) {
                        fieldValue = new Timestamp(Instant.parse(this.document.getCreated()));
                    } else {
                        throw new RuntimeException("Field annotated with @ServerTimestamp can " +
                                "be either of type java.util.Date or com.oracle.mobile.fusabase.Timestamp" +
                                " not " + field.getType().getName());
                    }
                }

                try {
                    Method setter = null;
                    Class<?> valueClassType = fieldValue.getClass();

                    // Handling the primitive type and their wrappers
                    if (fieldValue instanceof Boolean) {
                        setter = inferBooleanSetter(valueType, setMethodName);
                    } else if (fieldValue instanceof Number) {
                        setter = inferNumberSetter(valueType, setMethodName, (Number) fieldValue);
                    } else if (fieldValue instanceof Character) {
                        setter = inferCharSetter(valueType, setMethodName);
                    } else {
                        setter = valueType.getMethod(setMethodName, valueClassType);
                    }

                    if (setter != null) {
                        setter.invoke(obj, convertValue(setter.getParameterTypes()[0], fieldValue));
                    } // Call the setter
                } catch (NoSuchMethodException ignored) {
                    // If the setter doesn't exist, then we will leave that field
                }
            }
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping to object", e);
        }
    }

    /**
     * Compares this QueryDocumentSnapshot with another object for equality.
     * Two QueryDocumentSnapshots are considered equal if they have the same
     * underlying document and the same parent DocumentSnapshot properties.
     *
     * @param o The object to compare with this QueryDocumentSnapshot
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof QueryDocumentSnapshot) {
            QueryDocumentSnapshot other = (QueryDocumentSnapshot) o;
            return other.document.equals(this.document) &&
                    super.equals(o);
        }
        return false;
    }

    /**
     * Returns a hash code value for this QueryDocumentSnapshot.
     * The hash code is computed based on the document and parent class hash codes.
     *
     * @return A hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(document.hashCode(), super.hashCode());
    }
}
