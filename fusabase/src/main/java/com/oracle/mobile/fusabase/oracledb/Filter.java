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

import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.util.List;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Class Filter
 * Represents a condition on one or more fields to filter and refine query results.
 */
public class Filter {

    /**
     * Enumeration of filter operators.
     */
    protected enum Operator {
        /**
         * Not equal operator.
         */
        NOT_EQUAL("!="),

        /**
         * Array contains operator.
         */
        ARRAY_CONTAINS("array-contains"),

        /**
         * Array contains any operator.
         */
        ARRAY_CONTAINS_ANY("array-contains-any"),

        /**
         * Less than operator.
         */
        LESS_THAN("<"),

        /**
         * Less than or equal operator.
         */
        LESS_THAN_OR_EQUAL("<="),

        /**
         * Equal operator.
         */
        EQUAL("="),

        /**
         * In array operator.
         */
        IN("in"),

        /**
         * Not in array operator.
         */
        NOT_IN("not in"),

        /**
         * Greater than operator.
         */
        GREATER_THAN(">"),

        /**
         * Greater than or equal operator.
         */
        GREATER_THAN_OR_EQUAL(">="),

        /**
         * Is null operator.
         */
        IS_NULL("is NULL"),

        LIKE("like");

        /**
         * String representation of the operator.
         */
        final String text;

        /**
         * Constructs an Operator with the given text representation.
         *
         * @param text the string representation of the operator
         */
        Operator(String text) {
            this.text = text;
        }

        /**
         * Returns the string representation of the operator.
         *
         * @return the operator's text representation
         */
        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * The field path for this filter.
     */
    private final FieldPath field;

    /**
     * The operator for this filter.
     */
    private final Operator operator;

    /**
     * The value for this filter.
     */
    private final Object value;

    /**
     * Constructs a Filter instance.
     *
     * @param field    the field path for the filter, or null for composite filters
     * @param operator the filter operator
     * @param value    the filter value, or array of filters for composite operations
     */
    protected Filter(FieldPath field, Operator operator, @Nullable Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    /**
     * Gets the field path for this filter.
     *
     * @return the field path, or null for composite filters
     */
    protected FieldPath getField() {
        return field;
    }

    /**
     * Gets the operator for this filter.
     *
     * @return the filter operator
     */
    protected Operator getOperator() {
        return operator;
    }

    /**
     * Gets the value for this filter.
     *
     * @return the filter value, or null for null-checking operations
     */
    @Nullable
    protected Object getValue() {
        return value;
    }

    /**
     * Converts this filter to its JSON representation for API requests.
     *
     * @return the JSON object representation of this filter
     */
    @NonNull
    protected JsonObject getJsonObject() {
        JsonObjectBuilder builder = Json.createObjectBuilder().add("op", this.operator.text);

        if (this.field != null) {
            builder.add("field", String.join(".", this.field.getSegments()));
            builder.add("value", DataReader.getDataInJsonValue(this.value));
        } else {
            // For composite filters (AND, OR), value contains the array of filters
            builder.add("filters", DataReader.getDataInJsonValue(this.value));
        }

        return builder.build();
    }

    /**
     * Checks if the provided object is equal to this filter.
     *
     * @param o the object to compare
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Filter that = (Filter) o;

        return this.operator == that.operator
                && Objects.equals(this.field, that.field)
                && Objects.equals(this.value, that.value);
    }

    /**
     * Computes the hash code for this filter.
     *
     * @return the hash code based on field, operator, and value
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.field, this.operator, this.value);
    }

    /**
     * Creates a filter that checks if a field is null.
     *
     * @param field the field name
     * @return a new Filter instance
     */
    @NonNull
    public static Filter isNull(@NonNull String field) {
        Objects.requireNonNull(field, "Invalid field provided");
        return isNull(new FieldPath(field));
    }

    /**
     * Creates a filter that checks if a field is null.
     *
     * @param fieldPath the field path
     * @return a new Filter instance
     */
    @NonNull
    public static Filter isNull(@NonNull FieldPath fieldPath) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        return new Filter(fieldPath, Operator.IS_NULL, null);
    }

    /**
     * Creates a filter that checks if an array field contains a specific value.
     *
     * @param field the field name of the array
     * @param value the value to check for in the array
     * @return a new Filter instance
     */
    @NonNull
    public static Filter arrayContains(@NonNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Invalid field provided");
        return arrayContains(new FieldPath(field), value);
    }

    /**
     * Creates a filter that checks if an array field contains a specific value.
     *
     * @param fieldPath the field path of the array
     * @param value     the value to check for in the array
     * @return a new Filter instance
     */
    @NonNull
    public static Filter arrayContains(@NonNull FieldPath fieldPath, @Nullable Object value) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        return new Filter(fieldPath, Operator.ARRAY_CONTAINS, value);
    }

    /**
     * Creates a filter that checks if an array field contains any of the specified values.
     *
     * @param field  the field name of the array
     * @param values the list of values to check for in the array
     * @return a new Filter instance
     */
    @NonNull
    public static Filter arrayContainsAny(@NonNull String field, @NonNull List<Object> values) {
        Objects.requireNonNull(field, "Invalid field provided");
        Objects.requireNonNull(values, "Invalid values provided");
        return arrayContainsAny(new FieldPath(field), values);
    }

    /**
     * Creates a filter that checks if an array field contains any of the specified values.
     *
     * @param fieldPath the field path of the array
     * @param values    the list of values to check for in the array
     * @return a new Filter instance
     */
    @NonNull
    public static Filter arrayContainsAny(@NonNull FieldPath fieldPath, @NonNull List<Object> values) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        Objects.requireNonNull(values, "Invalid values provided");
        return new Filter(fieldPath, Operator.ARRAY_CONTAINS_ANY, values);
    }

    /**
     * Creates a filter that checks if a field is equal to a specified value.
     *
     * @param field the field name
     * @param value the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter equalTo(@NonNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Invalid field provided");
        FusabaseLogger.d("Filter.equalTo called with field: " + field + ", value: " + value);
        Filter result = equalTo(new FieldPath(field), value);
        FusabaseLogger.d("Filter.equalTo created EQUAL filter for field: " + field);
        return result;
    }

    /**
     * Creates a filter that checks if a field is equal to a specified value.
     *
     * @param fieldPath the field path
     * @param value     the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter equalTo(@NonNull FieldPath fieldPath, @Nullable Object value) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        FusabaseLogger.d("Filter.equalTo called with fieldPath: " + fieldPath + ", value: " + value);
        Filter result = new Filter(fieldPath, Operator.EQUAL, value);
        FusabaseLogger.d("Filter.equalTo created EQUAL filter for fieldPath: " + fieldPath);
        return result;
    }

    /**
     * Creates a filter that checks if a field is greater than a specified value.
     *
     * @param field the field name
     * @param value the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter greaterThan(@NonNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Invalid field provided");
        return greaterThan(new FieldPath(field), value);
    }

    /**
     * Creates a filter that checks if a field is greater than a specified value.
     *
     * @param fieldPath the field path
     * @param value     the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter greaterThan(@NonNull FieldPath fieldPath, @Nullable Object value) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        return new Filter(fieldPath, Operator.GREATER_THAN, value);
    }

    /**
     * Creates a filter that checks if a field is greater than or equal to a specified value.
     *
     * @param field the field name
     * @param value the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter greaterThanOrEqualTo(@NonNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Invalid field provided");
        return greaterThanOrEqualTo(new FieldPath(field), value);
    }

    /**
     * Creates a filter that checks if a field is greater than or equal to a specified value.
     *
     * @param fieldPath the field path
     * @param value     the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter greaterThanOrEqualTo(@NonNull FieldPath fieldPath, @Nullable Object value) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        return new Filter(fieldPath, Operator.GREATER_THAN_OR_EQUAL, value);
    }

    /**
     * Creates a filter that checks if a field's value is in a specified list of values.
     *
     * @param field  the field name
     * @param values the list of values to check against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter inArray(@NonNull String field, @NonNull List<Object> values) {
        Objects.requireNonNull(field, "Invalid field provided");
        Objects.requireNonNull(values, "Invalid values provided");
        return inArray(new FieldPath(field), values);
    }

    /**
     * Creates a filter that checks if a field's value is in a specified list of values.
     *
     * @param fieldPath the field path
     * @param values    the list of values to check against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter inArray(@NonNull FieldPath fieldPath, @NonNull List<Object> values) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        Objects.requireNonNull(values, "Invalid values provided");
        return new Filter(fieldPath, Operator.IN, values);
    }

    /**
     * Creates a filter that checks if a field is less than a specified value.
     *
     * @param field the field name
     * @param value the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter lessThan(@NonNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Invalid field provided");
        return lessThan(new FieldPath(field), value);
    }

    /**
     * Creates a filter that checks if a field is less than a specified value.
     *
     * @param fieldPath the field path
     * @param value     the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter lessThan(@NonNull FieldPath fieldPath, @Nullable Object value) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        return new Filter(fieldPath, Operator.LESS_THAN, value);
    }

    /**
     * Creates a filter that checks if a field is less than or equal to a specified value.
     *
     * @param field the field name
     * @param value the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter lessThanOrEqualTo(@NonNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Invalid field provided");
        return lessThanOrEqualTo(new FieldPath(field), value);
    }

    /**
     * Creates a filter that checks if a field is less than or equal to a specified value.
     *
     * @param fieldPath the field path
     * @param value     the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter lessThanOrEqualTo(@NonNull FieldPath fieldPath, @Nullable Object value) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        return new Filter(fieldPath, Operator.LESS_THAN_OR_EQUAL, value);
    }

    /**
     * Creates a filter that checks if a field is not equal to a specified value.
     *
     * @param field the field name
     * @param value the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter notEqualTo(@NonNull String field, @Nullable Object value) {
        Objects.requireNonNull(field, "Invalid field provided");
        return notEqualTo(new FieldPath(field), value);
    }

    /**
     * Creates a filter that checks if a field is not equal to a specified value.
     *
     * @param fieldPath the field path
     * @param value     the value to compare against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter notEqualTo(@NonNull FieldPath fieldPath, @Nullable Object value) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        return new Filter(fieldPath, Operator.NOT_EQUAL, value);
    }

    /**
     * Creates a filter that checks if a field's value is not in a specified list of values.
     *
     * @param field  the field name
     * @param values the list of values to check against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter notInArray(@NonNull String field, @NonNull List<Object> values) {
        Objects.requireNonNull(field, "Invalid field provided");
        Objects.requireNonNull(values, "Invalid values provided");
        return notInArray(new FieldPath(field), values);
    }

    /**
     * Creates a filter that checks if a field's value is not in a specified list of values.
     *
     * @param fieldPath the field path
     * @param values    the list of values to check against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter notInArray(@NonNull FieldPath fieldPath, @NonNull List<Object> values) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        Objects.requireNonNull(values, "Invalid values provided");
        return new Filter(fieldPath, Operator.NOT_IN, values);
    }

    /**
     * Creates a filter that performs a LIKE pattern matching operation on a field.
     *
     * @param field  the field name
     * @param values the pattern to match against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter like(@NonNull String field, @NonNull Object values) {
        Objects.requireNonNull(field, "Invalid field provided");
        Objects.requireNonNull(values, "Invalid values provided");
        return like(new FieldPath(field), values);
    }

    /**
     * Creates a filter that performs a LIKE pattern matching operation on a field.
     *
     * @param fieldPath the field path
     * @param values    the pattern to match against
     * @return a new Filter instance
     */
    @NonNull
    public static Filter like(@NonNull FieldPath fieldPath, @NonNull Object values) {
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        Objects.requireNonNull(values, "Invalid values provided");
        return new Filter(fieldPath, Operator.LIKE, values);
    }
}
