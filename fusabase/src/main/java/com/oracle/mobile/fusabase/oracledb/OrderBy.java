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

import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Represents an ordering clause for database queries, specifying both the field to order by
 * and the direction of ordering (ascending or descending). This class is used to construct
 * query ordering specifications for database operations.
 */
public class OrderBy {

    /** The ordering direction (ascending or descending) */
    private Query.Direction direction;

    /** The field path to order by */
    final FieldPath field;

    /**
     * Creates a new OrderBy instance with the specified direction and field path.
     *
     * @param direction The ordering direction (ASCENDING or DESCENDING)
     * @param path The field path to order by
     * @return A new OrderBy instance
     */
    public static OrderBy getInstance(Query.Direction direction, FieldPath path) {
        return new OrderBy(path, direction);
    }

    /**
     * Gets the ordering direction.
     *
     * @return The direction of ordering
     */
    public Query.Direction getDirection() {
        return direction;
    }

    /**
     * Gets the field path for ordering.
     *
     * @return The field path to order by
     */
    public FieldPath getField() {
        return field;
    }

    /**
     * Constructs a new OrderBy instance.
     *
     * @param field The field path to order by
     * @param direction The ordering direction
     */
    OrderBy(FieldPath field, Query.Direction direction) {
        this.direction = direction;
        this.field = field;
    }

    /**
     * Converts this OrderBy instance to a JSON object representation.
     *
     * @return A JSON object containing the direction and field information
     */
    @NonNull
    JsonObject getJsonObject() {
        return Json.createObjectBuilder()
                .add("direction", this.direction.toString())
                .add("field", field.toString())
                .build();
    }

    /**
     * Sets the ordering direction.
     *
     * @param direction The new ordering direction
     */
    void setDirection(Query.Direction direction) {
        this.direction = direction;
    }

    /**
     * Compares this OrderBy instance with another object for equality.
     * Two OrderBy instances are equal if they have the same direction and field.
     *
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof OrderBy)) {
            return false;
        }

        OrderBy other = (OrderBy) o;
        return direction == other.direction && field.equals(other.field);
    }

    /**
     * Returns a hash code value for this OrderBy.
     * @return int hash code value.
     */
    public int hashCode() {
        return java.util.Objects.hash(direction, field);
    }
}
