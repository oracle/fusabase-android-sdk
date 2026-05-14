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

import static com.oracle.mobile.fusabase.oracledb.DataReader.isPojo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.Timestamp;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a snapshot of a document fetched from the Oracledb database.
 * Provides access to document data, metadata, and reference information for read operations.
 * A document snapshot can represent either an existing document with data or a non-existent document.
 */
public class DocumentSnapshot extends Snapshot {

    private static final String TAG = "DocumentSnapshot";

    /**
     * The database instance this snapshot belongs to.
     */
    private final FusabaseOracledb oradb;

    /**
     * The document data, or null if the document doesn't exist.
     */
    private final @Nullable Document doc;

    /**
     * The path to the document in the database.
     */
    private final String path;

    /**
     * The reference to the document.
     */
    private final Reference docRef;

    /**
     * The version identifier of the document.
     */
    private final String version;

    /**
     * Metadata about this snapshot (cache status, pending writes, etc.).
     */
    public final SnapshotMetadata metadata;

    /**
     * Creates a DocumentSnapshot instance representing a document fetched from the database.
     *
     * @param oradb            the database instance this snapshot belongs to
     * @param doc              the document data, or null if the document doesn't exist
     * @param docRef           the reference to the document
     * @param path             the path to the document in the database
     * @param isFromCache      whether this snapshot was retrieved from cache
     * @param hasPendingWrites whether this snapshot has pending writes
     */
    protected DocumentSnapshot(@NonNull FusabaseOracledb oradb,
                               @Nullable Document doc,
                               @Nullable Reference docRef,
                               @NonNull String path,
                               boolean isFromCache,
                               boolean hasPendingWrites) {
        FusabaseLogger.i(TAG, "Creating DocumentSnapshot for path: " + path + ", exists: " + (doc != null) +
                      ", fromCache: " + isFromCache + ", hasPendingWrites: " + hasPendingWrites);
        this.oradb = oradb;
        this.metadata = new SnapshotMetadata(hasPendingWrites, isFromCache);
        this.doc = doc;
        this.path = path;
        this.docRef = docRef;
        this.version = doc != null ? doc.getVersion() : TransactionOperation.DEFAULT_VERSION;
        FusabaseLogger.d(TAG, "DocumentSnapshot created successfully for document ID: " +
                      (doc != null ? doc.getId() : "null"));
    }

    /**
     * Checks if the specified field exists in this document snapshot.
     *
     * @param field the name of the field to check
     * @return true if the field exists and has a non-null value, false otherwise
     */
    public boolean contains(@NonNull String field) {
        Objects.requireNonNull(field, "Invalid field provided");
        FusabaseLogger.d(TAG, "DocumentSnapshot.contains called with field: " + field);
        boolean result = contains(new FieldPath(field));
        FusabaseLogger.d(TAG, "DocumentSnapshot.contains returning: " + result);
        return result;
    }

    /**
     * Checks if the specified field path exists in this document snapshot.
     * Supports nested field access using dot notation.
     *
     * @param fieldPath the field path to check
     * @return true if the field path exists and has a non-null value, false otherwise
     */
    public boolean contains(@NonNull FieldPath fieldPath) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        FusabaseLogger.d(TAG, "DocumentSnapshot.contains called with fieldPath: " + fieldPath);
        boolean result = (this.doc != null) && (this.doc.getField(fieldPath) != null);
        FusabaseLogger.d(TAG, "DocumentSnapshot.contains returning: " + result);
        return result;
    }

    /**
     * Compares this DocumentSnapshot with another object for equality.
     * Two DocumentSnapshot objects are equal if they have the same database instance,
     * document data, path, reference, metadata, and version.
     *
     * @param o the object to compare with this DocumentSnapshot
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof DocumentSnapshot) {
            DocumentSnapshot other = (DocumentSnapshot) o;
            return other.metadata.equals(this.metadata) &&
                    other.oradb.equals(this.oradb) &&
                    Objects.equals(other.doc, this.doc) &&
                    other.path.equals(this.path) &&
                    Objects.equals(other.docRef, this.docRef) &&
                    Objects.equals(other.version, this.version);
        }
        return false;
    }

    /**
     * Gets the underlying Document object for this snapshot.
     * Returns null if the document doesn't exist.
     *
     * @return the document object, or null if the document doesn't exist
     */
    @Nullable
    protected Document getDocument() {
        return this.doc;
    }

    /**
     * Checks whether this document snapshot represents an existing document.
     *
     * @return true if the document exists in the database, false otherwise
     */
    public boolean exists() {
        boolean result = this.doc != null;
        FusabaseLogger.d(TAG, "DocumentSnapshot.exists() called, returning: " + result);
        return result;
    }

    /**
     * Method to get the value of the provided field
     *
     * @param field {@code String} Name of the field
     * @return {@code Object} Value of the provided field if it exists otherwise null
     */
    @Nullable
    public Object get(@NonNull String field) {
        return get(new FieldPath(field));
    }

    /**
     * Method to get the value of the provided field at FieldPath
     *
     * @param fieldPath {@code FieldPath} path of the field
     * @return {@code Object} Value at the provided field path if it exists otherwise null
     */
    @Nullable
    public Object get(@NonNull FieldPath fieldPath) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        FusabaseLogger.v(TAG, "DocumentSnapshot.get() called with fieldPath: " + fieldPath);
        Object result = this.doc == null ? null : this.doc.getField(fieldPath);
        FusabaseLogger.v(TAG, "DocumentSnapshot.get() returning value of type: " +
                      (result != null ? result.getClass().getSimpleName() : "null"));
        return result;
    }

    /**
     * Method to get the value of the field with provided class type
     *
     * @param field     {@code String} Name of the field
     * @param valueType {@code Class<T>} Class type
     * @param <T>       Generic type of the expected return value
     * @return {@code T} Value of field with the provided class type
     */
    @Nullable
    public <T> T get(@NonNull String field, @NonNull Class<T> valueType) {
        Object value = get(field);

        if (value == null) {
            return null;
        }

        if (valueType.isInstance(value)) {
            return valueType.cast(value);
        }

        try {
            if (valueType == Integer.class && value instanceof Number) {
                return valueType.cast(((Number) value).intValue());
            } else if (valueType == Long.class && value instanceof Number) {
                return valueType.cast(((Number) value).longValue());
            } else if (valueType == Double.class && value instanceof Number) {
                return valueType.cast(((Number) value).doubleValue());
            } else if (valueType == String.class) {
                return valueType.cast(value.toString());
            } else if (valueType == Date.class && value instanceof Long) {
                return valueType.cast(new Date((Long) value));
            } else if (isPojo(valueType)) {
                return valueType.cast(value);
            }
        } catch (ClassCastException e) {
            return null;
        }

        return null;
    }

    /**
     * Method to get the value of the field with provided class type
     *
     * @param fieldPath {@code FieldPath} Field Path
     * @param valueType {@code Class<T>} Class type
     * @param <T>       Generic type of the expected return value
     * @return {@code T} Value of field with the provided class type
     */
    @Nullable
    public <T> T get(@NonNull FieldPath fieldPath, @NonNull Class<T> valueType) {
        Object value = get(fieldPath);

        if (value == null) {
            return null;
        }

        if (valueType.isInstance(value)) {
            return valueType.cast(value);
        }

        try {
            if (valueType == Integer.class && value instanceof Number) {
                return valueType.cast(((Number) value).intValue());
            } else if (valueType == Long.class && value instanceof Number) {
                return valueType.cast(((Number) value).longValue());
            } else if (valueType == Double.class && value instanceof Number) {
                return valueType.cast(((Number) value).doubleValue());
            } else if (valueType == String.class) {
                return valueType.cast(value.toString());
            } else if (valueType == Date.class && value instanceof Long) {
                return valueType.cast(new Date((Long) value));
            } else if (isPojo(valueType)) {
                return valueType.cast(value);
            }
        } catch (ClassCastException e) {
            return null;
        }

        return null;
    }


    /**
     * Method to get the value of the field as Blob
     *
     * @param field {@code String} Name of the field
     * @return {@code Blob} value of the field as blob
     */
    @Nullable
    public Blob getBlob(@NonNull String field) {
        return (Blob) get(field);
    }

    /**
     * Method to get the value of the field as boolean
     *
     * @param field {@code String} Name of the field
     * @return {@code boolean} value of the field as boolean
     */
    @Nullable
    public Boolean getBoolean(@NonNull String field) {
        Object value = get(field);
        Boolean result = value instanceof Boolean ? (Boolean) value : null;
        FusabaseLogger.v(TAG, "DocumentSnapshot.getBoolean() called for field: " + field + ", returning: " + result);
        return result;
    }

    /**
     * Method to get the data of the document in the form of HashMap with field name as string and
     * values as Object
     *
     * @return {@code HashMap<String, Object>} Document in the form of HashMap
     */
    @Nullable
    public Map<String, Object> getData() {
        FusabaseLogger.i(TAG, "DocumentSnapshot.getData() called for document path: " + this.path);
        if (this.doc == null || this.doc.getData() == null) {
            FusabaseLogger.w(TAG, "DocumentSnapshot.getData() returning null - document or data is null");
            return null;
        }
        Map<String, Object> data = this.doc.getData();
        FusabaseLogger.d(TAG, "DocumentSnapshot.getData() returning map with " +
                      (data != null ? data.size() : 0) + " fields");
        return data;
    }

    /**
     * Method to get Document Id
     *
     * @return {@code String} Document Id
     */
    @NonNull
    public String getId() {
        FusabaseLogger.d(TAG, "DocumentSnapshot.getId() called for path: " + this.path);
        if (this.doc == null) {
            FusabaseLogger.e(TAG, "DocumentSnapshot.getId() failed - document does not exist for path: " + this.path);
            throw new NullPointerException("Document doesn't exist");
        }
        String id = this.doc.getId();
        FusabaseLogger.v(TAG, "DocumentSnapshot.getId() returning: " + id);
        return id;
    }

    @NonNull
    protected String getVersion() {
        return this.version;
    }

    @NonNull
    protected Long getDocIndex () {
        FusabaseLogger.v(TAG, "DocumentSnapshot.getDocIndex() called for path: " + this.path);
        if (this.doc == null) {
            FusabaseLogger.e(TAG, "DocumentSnapshot.getDocIndex() failed - document does not exist for path: " + this.path);
            throw new NullPointerException("Document doesn't exist");
        }
        Long index = this.doc.getIndex();
        FusabaseLogger.v(TAG, "DocumentSnapshot.getDocIndex() returning: " + index);
        return index;
    }
    /**
     * Method to get the value of the field as Date
     *
     * @param field {@code String} Name of the field
     * @return {@code Date} value of the field as date
     */
    @Nullable
    public Date getDate(@NonNull String field) {
        Object val = get(field);
        if (val == null) return null;

        if (val instanceof Date) {
            return (Date) val;
        } else if (val instanceof Timestamp) {
            // Convert Timestamp back to Date
            return ((Timestamp) val).toDate();
        }

        return null;
    }

    /**
     * Method to get the value of the field as Double
     *
     * @param field {@code String} Name of the field
     * @return {@code Double} value of the field as double
     */
    @Nullable
    public Double getDouble(@NonNull String field) {
        Object data = get(field);
        if (data == null) return null;
        if (data instanceof Integer) {
            return ((Integer) data).doubleValue();
        } else if (data instanceof Long) {
            return ((Long) data).doubleValue();
        } else if (data instanceof Double) {
            return (Double) data;
        }
        return null;
    }

    /**
     * Method to get the value of the field as Long
     *
     * @param field {@code String} Name of the field
     * @return {@code Long} value of the field as Long
     */
    @Nullable
    public Long getLong(@NonNull String field) {
        Object data = get(field);
        if (data == null) return null;
        if(data instanceof Integer)
            return ((Integer) data).longValue();
        return (Long) data;
    }

    /**
     * Method to get snapshot metadata for this document snapshot
     *
     * @return {@code SnapshotMetadata}
     */
    @NonNull
    public SnapshotMetadata getMetadata() {
        FusabaseLogger.v(TAG, "DocumentSnapshot.getMetadata() called for path: " + this.path);
        return this.metadata;
    }

    /**
     * Method to get the reference of this document Snapshot
     * It can be {@code DualityViewDocReference} or {@code DocumentReference}
     *
     * @return {@code DocumentReference}
     */
    @NonNull
    public DocumentReference getReference() {
        FusabaseLogger.v(TAG, "DocumentSnapshot.getReference() called for path: " + this.path);
        return (DocumentReference) this.docRef;
    }

    @Nullable
    public String getString(@NonNull String field) {
        Object value = get(field);
        String result = value instanceof String ? (String) value : null;
        FusabaseLogger.v(TAG, "DocumentSnapshot.getString() called for field: " + field + ", returning: " +
                      (result != null ? "\"" + result + "\"" : "null"));
        return result;
    }

    @Nullable
    public Timestamp getTimestamp(@NonNull String field) {
        Object val = get(field);
        if (val == null) return null;

        if (val instanceof Timestamp) {
            return (Timestamp) val;
        } else if (val instanceof String) {
            return new Timestamp(Instant.parse((String) val));
        }

        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.doc == null ? null : this.doc.hashCode(),
                this.path.hashCode(),
                this.oradb,
                this.metadata.hashCode(),
                this.docRef,
                this.version);
    }

    /**
     * Method to create a custom object of provided class type
     *
     * @param valueType {@code Class<T>} Class type of the object
     * @param <T>       Generic type of the expected return value
     * @return {@code T} Object of provided type with fields provided from document data
     */
    @Nullable
    public <T> T toObject(@NonNull Class<T> valueType) {
        Objects.requireNonNull(valueType, "Invalid valueType provided");
        FusabaseLogger.i(TAG, "DocumentSnapshot.toObject() called for class: " + valueType.getSimpleName() +
                      ", document path: " + this.path);

        if(this.doc == null) {
            FusabaseLogger.w(TAG, "DocumentSnapshot.toObject() returning null - document does not exist");
            return null;
        }

        try {
            T obj = valueType.getConstructor().newInstance();
            FusabaseLogger.d(TAG, "DocumentSnapshot.toObject() created instance of: " + valueType.getSimpleName());

            // Only process data if it exists and is not empty
            if (this.doc.getData() != null && !this.doc.getData().isEmpty()) {
                FusabaseLogger.v(TAG, "DocumentSnapshot.toObject() processing " + this.doc.getData().size() + " fields");

                for (Map.Entry<String, Object> entry : this.doc.getData().entrySet()) {
                    String fieldName = entry.getKey();
                    Object fieldValue = entry.getValue();

                    // Skip null values - they cannot be mapped to object fields
                    if (fieldValue == null) {
                        FusabaseLogger.v(TAG, "DocumentSnapshot.toObject() skipping null field: " + fieldName);
                        continue;
                    }

                    // Creating the field set method name
                    String setMethodName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

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
                        } else if(fieldValue instanceof Date) {
                            setter = inferDateSetter(valueType, setMethodName);
                        } else if(fieldValue instanceof Timestamp) {
                            setter = inferTimestampSetter(valueType, setMethodName);
                        } else if(fieldValue instanceof Map) {
                            setter = inferMapSetter(valueType, setMethodName);
                        } else if (fieldValue instanceof List) {
                            setter = inferListSetter(valueType, setMethodName);
                        } else {
                            setter = valueType.getMethod(setMethodName, valueClassType);
                        }

                        if (setter != null) {
                            setter.invoke(obj, convertValue(setter.getParameterTypes()[0], fieldValue));
                            FusabaseLogger.v(TAG, "DocumentSnapshot.toObject() set field: " + fieldName +
                                          " with value type: " + fieldValue.getClass().getSimpleName());
                        } else {
                            FusabaseLogger.w(TAG, "DocumentSnapshot.toObject() no setter found for field: " + fieldName);
                        }
                    } catch (NoSuchMethodException e) {
                        // If the setter doesn't exist, then we will leave that field
                        FusabaseLogger.d(TAG, "DocumentSnapshot.toObject() setter not found for field: " + fieldName +
                                      ", skipping field");
                    }
                }
            } else {
                FusabaseLogger.w(TAG, "DocumentSnapshot.toObject() no data available to map to object");
            }

            FusabaseLogger.i(TAG, "DocumentSnapshot.toObject() successfully created object of type: " +
                          valueType.getSimpleName());
            return obj;
        } catch (Exception e) {
            FusabaseLogger.e(TAG, "DocumentSnapshot.toObject() error mapping to object: " + e.getMessage(), e);
            return null;
        }
    }

    protected <T> Method inferNumberSetter(Class<T> clazz, String setMethodName, Number value) {
        Class<?>[] numberTypes = { int.class, Integer.class, long.class, Long.class,
                double.class, Double.class, float.class, Float.class,
                short.class, Short.class, byte.class, Byte.class };
        for (Class<?> type : numberTypes) {
            try {
                return clazz.getMethod(setMethodName, type);
            } catch (NoSuchMethodException e) {
                FusabaseLogger.v(TAG, "DocumentSnapshot.inferNumberSetter: No setter found for " + setMethodName +
                              " with type " + type.getSimpleName());
            }
        }
        FusabaseLogger.d(TAG, "DocumentSnapshot.inferNumberSetter: No compatible number setter found for " + setMethodName);
        return null;
    }

    protected <T> Method inferTimestampSetter(Class<T> clazz, String setMethodName) {
        Class<?>[] timestampTypes = { Timestamp.class, Date.class, Object.class };
        for (Class<?> type : timestampTypes) {
            try {
                return clazz.getMethod(setMethodName, type);
            } catch (NoSuchMethodException e) {
                FusabaseLogger.v(TAG, "DocumentSnapshot.inferTimestampSetter: No setter found for " + setMethodName +
                              " with type " + type.getSimpleName());
            }
        }
        FusabaseLogger.d(TAG, "DocumentSnapshot.inferTimestampSetter: No compatible timestamp setter found for " + setMethodName);
        return null;
    }

    protected <T> Method inferBooleanSetter(Class<T> clazz, String setMethodName) {
        try {
            return clazz.getMethod(setMethodName, boolean.class);
        } catch (NoSuchMethodException e) {
            FusabaseLogger.v(TAG, "DocumentSnapshot.inferBooleanSetter: No primitive boolean setter found for " + setMethodName);
            try {
                return clazz.getMethod(setMethodName, Boolean.class);
            } catch (NoSuchMethodException e2) {
                FusabaseLogger.v(TAG, "DocumentSnapshot.inferBooleanSetter: No Boolean wrapper setter found for " + setMethodName);
            }
        }
        FusabaseLogger.d(TAG, "DocumentSnapshot.inferBooleanSetter: No compatible boolean setter found for " + setMethodName);
        return null;
    }

    protected <T> Method inferCharSetter(Class<T> clazz, String setMethodName) {
        try {
            return clazz.getMethod(setMethodName, char.class);
        } catch (NoSuchMethodException e) {
            FusabaseLogger.v(TAG, "DocumentSnapshot.inferCharSetter: No primitive char setter found for " + setMethodName);
            try {
                return clazz.getMethod(setMethodName, Character.class);
            } catch (NoSuchMethodException e2) {
                FusabaseLogger.v(TAG, "DocumentSnapshot.inferCharSetter: No Character wrapper setter found for " + setMethodName);
            }
        }
        FusabaseLogger.d(TAG, "DocumentSnapshot.inferCharSetter: No compatible char setter found for " + setMethodName);
        return null;
    }

    protected <T> Method inferDateSetter(Class<T> clazz, String setMethodName) {
        Class<?>[] dateTypes = { Date.class, Timestamp.class, Object.class };
        for (Class<?> type : dateTypes) {
            try {
                return clazz.getMethod(setMethodName, type);
            } catch (NoSuchMethodException e) {
                FusabaseLogger.v(TAG, "DocumentSnapshot.inferDateSetter: No setter found for " + setMethodName +
                              " with type " + type.getSimpleName());
            }
        }
        FusabaseLogger.d(TAG, "DocumentSnapshot.inferDateSetter: No compatible date setter found for " + setMethodName);
        return null;
    }

    protected <T> Method inferListSetter(Class<T> clazz, String setMethodName) {
        Class<?>[] listTypes = { List.class, ArrayList.class };
        for (Class<?> type : listTypes) {
            try {
                return clazz.getMethod(setMethodName, type);
            } catch (NoSuchMethodException e) {
                FusabaseLogger.v(TAG, "DocumentSnapshot.inferListSetter: No setter found for " + setMethodName +
                              " with type " + type.getSimpleName());
            }
        }
        FusabaseLogger.d(TAG, "DocumentSnapshot.inferListSetter: No compatible list setter found for " + setMethodName);
        return null;
    }

    protected Object convertValue(Class<?> targetType, Object value) {
        if (targetType.isPrimitive()) {
            if (targetType == int.class) return ((Number) value).intValue();
            if (targetType == long.class) return ((Number) value).longValue();
            if (targetType == double.class) return ((Number) value).doubleValue();
            if (targetType == float.class) return ((Number) value).floatValue();
            if (targetType == short.class) return ((Number) value).shortValue();
            if (targetType == byte.class) return ((Number) value).byteValue();
            if (targetType == boolean.class) return value;
            if (targetType == char.class) return value;
        } else {
            // Handle wrapper types
            if (targetType == Integer.class) return ((Number) value).intValue();
            if (targetType == Long.class) return ((Number) value).longValue();
            if (targetType == Double.class) return ((Number) value).doubleValue();
            if (targetType == Float.class) return ((Number) value).floatValue();
            if (targetType == Short.class) return ((Number) value).shortValue();
            if (targetType == Byte.class) return ((Number) value).byteValue();
            if (targetType == Boolean.class) return value;
            if (targetType == Character.class) return value;
            // Handle date/timestamp conversions
            if (targetType == Date.class && value instanceof Timestamp) {
                return ((Timestamp) value).toDate();
            }
            if (targetType == Timestamp.class && value instanceof Date) {
                return new Timestamp((Date) value);
            }
        }
        return value; // No conversion needed for other types
    }

    protected <T> Method inferMapSetter(Class<T> clazz, String setMethodName) {
        Class<?>[] mapTypes = { Map.class, HashMap.class };
        for (Class<?> type : mapTypes) {
            try {
                return clazz.getMethod(setMethodName, type);
            } catch (NoSuchMethodException e) {
                FusabaseLogger.v(TAG, "DocumentSnapshot.inferMapSetter: No setter found for " + setMethodName +
                              " with type " + type.getSimpleName());
            }
        }
        FusabaseLogger.d(TAG, "DocumentSnapshot.inferMapSetter: No compatible map setter found for " + setMethodName);
        return null;
    }
}
