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
import java.util.List;
import java.util.Objects;

/**
 * Represents a path to a field within a document, supporting nested field access.
 * FieldPath allows accessing nested document fields using dot notation (e.g., "user.address.city").
 * Provides methods for creating field paths from strings or field name arrays.
 */
public final class FieldPath {
    /**
     * The segments of the field path, representing nested field names.
     */
    private List<String> segments;

    /**
     * Creates a FieldPath from a list of field name segments.
     *
     * @param segments the ordered list of field name segments
     */
    protected FieldPath(List<String> segments) {
        this.segments = segments;
    }

    /**
     * Creates a FieldPath from a dot-separated string path.
     *
     * @param dotSeparatedPath the field path as a dot-separated string (e.g., "user.address.city")
     */
    protected FieldPath(String dotSeparatedPath) {
        this.segments = Arrays.asList(dotSeparatedPath.split("\\."));
    }

    /**
     * Gets the segments of this field path as a list of strings.
     *
     * @return the ordered list of field name segments
     */
    @NonNull
    protected List<String> getSegments() {
        return this.segments;
    }

    /**
     * Creates a FieldPath for the document ID field.
     * This is a convenience method for accessing the document's unique identifier.
     *
     * @return a FieldPath pointing to the document ID field
     */
    @NonNull
    public static FieldPath documentId() {
        return new FieldPath("OID");
    }

    /**
     * Compares this FieldPath with another object for equality.
     * Two FieldPath objects are equal if they have the same field segments in the same order.
     *
     * @param o the object to compare with this FieldPath
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
        FieldPath fieldPath = (FieldPath) o;
        return segments.equals(fieldPath.segments);
    }

    /**
     * Returns the hash code for this FieldPath.
     * The hash code is based on the field segments.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    /**
     * Creates a FieldPath from the provided field names.
     * Each field name represents a segment in the path to a nested field.
     *
     * @param fieldNames the field names to include in the path
     * @return a new FieldPath instance
     * @throws IllegalArgumentException if any field name is null or empty
     */
    @NonNull
    public static FieldPath of(String... fieldNames) {
        Objects.requireNonNull(fieldNames, "Invalid fieldNames provided");
        FusabaseLogger.d("FieldPath.of called with " + fieldNames.length + " field names");
        if (hasNullOrEmptyArg(fieldNames)) {
            FusabaseLogger.e("FieldPath.of failed: Invalid field name " + Arrays.toString(fieldNames));
            throw new IllegalArgumentException("Invalid field name " + Arrays.toString(fieldNames) +
                " provided to FieldPath.of() method.");
        }
        FieldPath result = new FieldPath(Arrays.asList(fieldNames));
        FusabaseLogger.d("FieldPath.of created FieldPath: " + result);
        return result;
    }

    /**
     * Returns a string representation of this FieldPath.
     * The segments are joined with dots (e.g., "user.address.city").
     * Special characters are escaped to ensure proper representation.
     *
     * @return the string representation of this field path
     */
    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                builder.append(".");
            }
            String escaped = segments.get(i);
            escaped = escaped.replace("\\", "\\\\").replace("`", "\\`");

            builder.append(escaped);
        }
        return builder.toString();
    }

    /**
     * Checks if the provided field names array contains any null or empty values.
     *
     * @param fieldNames the field names to check
     * @return true if any field name is null or empty, false otherwise
     */
    public static boolean hasNullOrEmptyArg(String... fieldNames) {
        return fieldNames == null || Arrays.stream(fieldNames)
                .anyMatch(f -> f == null || f.isEmpty());
    }

}
