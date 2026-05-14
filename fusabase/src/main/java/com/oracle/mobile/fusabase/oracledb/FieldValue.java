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

import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents special field values for database operations that require server-side processing.
 * Provides factory methods for creating sentinel values that instruct the server to perform
 * specific operations like array manipulation, field deletion, numeric incrementation, and
 * server timestamp insertion.
 */
public class FieldValue {

    /**
     * Enumeration of supported field value operations that require server-side processing.
     */
    protected enum Operation {
        /**
         * Operation to remove specified elements from an array field.
         */
        ARRAY_REMOVE("arrayRemove"),
        /**
         * Operation to add specified elements to an array field.
         */
        ARRAY_UNION("arrayUnion"),
        /**
         * Operation to delete a field from the document.
         */
        DELETE("delete"),
        /**
         * Operation to increment a numeric field by a specified value.
         */
        INCREMENT("increment"),
        /**
         * Operation to set a field to the server's current timestamp.
         */
        SERVER_TIMESTAMP("servertimestamp"),
        /**
         * Operation to delete a vector embedding key.
         */
        DELETE_VECTOR("deleteVector");

        /**
         * The string representation of this operation.
         */
        private final String operation;

        /**
         * Creates an Operation with the specified string value.
         *
         * @param operation the string representation of the operation
         */
        Operation(String operation) {
            this.operation = operation;
        }

        /**
         * Gets the string representation of this operation.
         *
         * @return the operation string
         */
        public String getOperationInString() {
            return operation;
        }
    }

    /**
     * The operation this FieldValue represents.
     */
    private final Operation operation;

    /**
     * The values associated with the operation.
     */
    private final Object[] values;

    /**
     * Creates a FieldValue instance with the specified operation and values.
     *
     * @param operation the operation this FieldValue represents
     * @param values    the values associated with the operation
     */
    FieldValue(@NonNull Operation operation, @NonNull Object[] values) {
        this.operation = operation;
        this.values = values;
    }

    /**
     * Returns a sentinel value for use with set() or update() operations.
     * Instructs the server to remove the specified elements from any existing array field.
     *
     * @param elements the elements to remove from the array
     * @return a FieldValue representing the array removal operation
     */
    @NonNull
    public static FieldValue arrayRemove(Object... elements) {
        Objects.requireNonNull(elements, "Invalid elements provided");
        FusabaseLogger.d("FieldValue.arrayRemove called with " + elements.length + " elements");
        FieldValue result = new FieldValue(Operation.ARRAY_REMOVE, elements);
        FusabaseLogger.d("FieldValue.arrayRemove created FieldValue for ARRAY_REMOVE operation");
        return result;
    }

    /**
     * Returns a sentinel value for use with set() or update() operations.
     * Instructs the server to merge the specified elements with any existing array field.
     *
     * @param elements the elements to add to the array
     * @return a FieldValue representing the array union operation
     */
    @NonNull
    public static FieldValue arrayUnion(Object... elements) {
        Objects.requireNonNull(elements, "Invalid elements provided");
        FusabaseLogger.d("FieldValue.arrayUnion called with " + elements.length + " elements");
        FieldValue result = new FieldValue(Operation.ARRAY_UNION, elements);
        FusabaseLogger.d("FieldValue.arrayUnion created FieldValue for ARRAY_UNION operation");
        return result;
    }

    /**
     * Returns a sentinel value for use with update() operations.
     * Instructs the server to delete the specified field from the document.
     *
     * @return a FieldValue representing the field deletion operation
     */
    @NonNull
    public static FieldValue delete() {
        FusabaseLogger.d("FieldValue.delete called");
        FieldValue result = new FieldValue(Operation.DELETE, new Object[]{});
        FusabaseLogger.d("FieldValue.delete created FieldValue for DELETE operation");
        return result;
    }

    /**
     * Returns a sentinel value for use with set() or update() operations.
     * Instructs the server to increment the field's current numeric value by the specified amount.
     *
     * @param l the value to increment by
     * @return a FieldValue representing the increment operation
     */
    @NonNull
    public static FieldValue increment(long l) {
        FusabaseLogger.d("FieldValue.increment called with long value: " + l);
        FieldValue result = new FieldValue(Operation.INCREMENT, new Object[]{l});
        FusabaseLogger.d("FieldValue.increment created FieldValue for INCREMENT operation");
        return result;
    }

    /**
     * Returns a sentinel value for use with set() or update() operations.
     * Instructs the server to increment the field's current numeric value by the specified amount.
     *
     * @param l the value to increment by
     * @return a FieldValue representing the increment operation
     */
    @NonNull
    public static FieldValue increment(double l) {
        FusabaseLogger.d("FieldValue.increment called with double value: " + l);
        FieldValue result = new FieldValue(Operation.INCREMENT, new Object[]{l});
        FusabaseLogger.d("FieldValue.increment created FieldValue for INCREMENT operation");
        return result;
    }

    /**
     * Returns a sentinel value for use with set() or update() operations.
     * Instructs the server to insert the current server timestamp into the field.
     *
     * @return a FieldValue representing the server timestamp operation
     */
    @NonNull
    public static FieldValue serverTimestamp() {
        FusabaseLogger.d("FieldValue.serverTimestamp called");
        FieldValue result = new FieldValue(Operation.SERVER_TIMESTAMP, new Object[]{});
        FusabaseLogger.d("FieldValue.serverTimestamp created FieldValue for SERVER_TIMESTAMP operation");
        return result;
    }

    /**
     * Returns a sentinel value for use with vector update operations.
     * Instructs the server to delete the corresponding key from $embeddings in v2 APIs.
     *
     * @return a FieldValue representing the vector deletion operation
     */
    @NonNull
    public static FieldValue deleteVector() {
        FusabaseLogger.d("FieldValue.deleteVector called");
        FieldValue result = new FieldValue(Operation.DELETE_VECTOR, new Object[]{});
        FusabaseLogger.d("FieldValue.deleteVector created FieldValue for DELETE_VECTOR operation");
        return result;
    }

    /**
     * Gets the operation this FieldValue represents.
     *
     * @return the operation type
     */
    @NonNull
    protected FieldValue.Operation getOperation() {
        return this.operation;
    }

    /**
     * Gets the values associated with this FieldValue operation.
     *
     * @return the operation values
     */
    @NonNull
    protected Object[] getValues() {
        return this.values;
    }

    /**
     * Compares this FieldValue with another object for equality.
     * Two FieldValue objects are equal if they have the same operation and values.
     *
     * @param o the object to compare with this FieldValue
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof FieldValue) {
            FieldValue other = (FieldValue) o;
            return Arrays.equals(this.values, other.values) &&
                    this.operation.equals(other.operation);
        }
        return false;
    }

    /**
     * Returns the hash code for this FieldValue.
     * The hash code is based on the operation and values.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(this.values), this.operation);
    }
  }
