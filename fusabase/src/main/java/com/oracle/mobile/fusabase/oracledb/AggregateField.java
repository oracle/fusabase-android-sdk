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

import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Class representing a computation or summary operation that
 * Oracledb can execute on a collection of documents.
 */
public abstract class AggregateField {

    /**
     * The alias for this aggregate field, used to identify the result.
     */
    @NonNull
    public final String alias;

    /**
     * The operator for the aggregate field, such as "avg", "sum", etc.
     */
    @NonNull
    public final String operator;

    /**
     * The field path on which this aggregation is to be applied.
     */
    @NonNull
    private final FieldPath fieldPath;

    /**
     * Protected constructor to create an instance of AggregateField.
     *
     * @param operator the operator for the AggregateField, e.g., "avg", "sum"
     * @param alias the alias for the AggregateField
     * @param fieldPath the path of the field on which this aggregation is to be applied
     */
    protected AggregateField(@NonNull String operator, @NonNull String alias, @NonNull FieldPath fieldPath) {
        this.operator = operator;
        this.alias = alias;
        this.fieldPath = fieldPath;
    }

    /**
     * Class representing the Average Aggregation that can be performed by Oracledb.
     */
    public static class AverageAggregateField extends AggregateField {
        protected AverageAggregateField(@NonNull String alias, @NonNull FieldPath fieldPath) {
            super("avg", alias, fieldPath);
        }
    }

    /**
     * Class representing the Count Aggregation that can be performed by Oracledb.
     */
    public static class CountAggregateField extends AggregateField {
        protected CountAggregateField(@NonNull String alias, @NonNull FieldPath fieldPath) {
            super("count", alias, new FieldPath(""));
        }
    }

    /**
     * Class representing the Sum Aggregation that can be performed by Oracledb.
     */
    public static class SumAggregateField extends AggregateField {
        protected SumAggregateField(@NonNull String alias, @NonNull FieldPath fieldPath) {
            super("sum", alias, fieldPath);
        }
    }

    /**
     * Returns the parameters of this AggregateField as a JsonObject.
     *
     * @return the JsonObject representation
     */
    @NonNull
    protected JsonObject getJsonObject() {
        return Json.createObjectBuilder()
                .add("func", this.operator)
                .add("field", String.join(".", this.fieldPath.getSegments()))
                .add("op_key", alias)
                .build();
    }

    /**
     * Creates an average aggregate field with the specified alias and field name.
     *
     * @param alias the alias for the resulting aggregate
     * @param field the name of the field on which the aggregation is to be performed
     * @return the AverageAggregateField
     * @throws IllegalArgumentException if alias or field is empty
     */
    @NonNull
    public static AverageAggregateField average(@NonNull String alias, @NonNull String field) {
        Objects.requireNonNull(alias, "Invalid alias provided");
        Objects.requireNonNull(field, "Invalid field provided");
        return new AverageAggregateField(alias, new FieldPath(field));
    }

    /**
     * Creates an average aggregate field with the specified alias and field path.
     *
     * @param alias the alias for the resulting aggregate
     * @param fieldPath the FieldPath on which the aggregation is to be performed
     * @return the AverageAggregateField
     * @throws IllegalArgumentException if alias is empty
     */
    @NonNull
    public static AverageAggregateField average(@NonNull String alias, @NonNull FieldPath fieldPath) {
        Objects.requireNonNull(alias, "Invalid alias provided");
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        return new AverageAggregateField(alias, fieldPath);
    }

    /**
     * Creates a sum aggregate field with the specified alias and field name.
     *
     * @param alias the alias for the resulting aggregate
     * @param field the name of the field on which the aggregation is to be performed
     * @return the SumAggregateField
     * @throws IllegalArgumentException if alias or field is empty
     */
    @NonNull
    public static SumAggregateField sum(@NonNull String alias, @NonNull String field) {
        Objects.requireNonNull(alias, "Invalid alias provided");
        Objects.requireNonNull(field, "Invalid field provided");
        return new SumAggregateField(alias, new FieldPath(field));
    }

    /**
     * Creates a sum aggregate field with the specified alias and field path.
     *
     * @param alias the alias for the resulting aggregate
     * @param fieldPath the FieldPath on which the aggregation is to be performed
     * @return the SumAggregateField
     * @throws IllegalArgumentException if alias is empty
     */
    @NonNull
    public static SumAggregateField sum(@NonNull String alias, @NonNull FieldPath fieldPath) {
        Objects.requireNonNull(alias, "Invalid alias provided");
        Objects.requireNonNull(fieldPath, "Invalid fieldPath provided");
        return new SumAggregateField(alias, fieldPath);
    }

    /**
     * Creates a count aggregate field with the specified alias.
     *
     * @param alias the alias for the resulting aggregate
     * @return the CountAggregateField
     * @throws IllegalArgumentException if alias is empty
     */
    @NonNull
    public static CountAggregateField count(@NonNull String alias) {
        Objects.requireNonNull(alias, "Invalid alias provided");
        if (alias.isEmpty()) {
            throw new IllegalArgumentException("Invalid alias provided to count() method.");
        }
        return new CountAggregateField(alias, new FieldPath(""));
    }

    /**
     * Compares this AggregateField to the specified object for equality.
     *
     * @param o the object to compare
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof AggregateField) {
            AggregateField other = (AggregateField) o;
            return other.alias.equals(this.alias) &&
                    other.operator.equals(this.operator) &&
                    other.fieldPath.equals(this.fieldPath);
        }
        return false;
    }

    /**
     * Returns a hash code value for this AggregateField.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.alias, this.operator, this.fieldPath);
    }
}
