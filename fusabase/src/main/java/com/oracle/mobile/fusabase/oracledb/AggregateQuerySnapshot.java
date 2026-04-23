// Copyright (c) 2015, 2025 Oracle and/or its affiliates.

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

import java.util.Map;
import java.util.Objects;

/**
 * Represents the result of execution of an AggregateQuery.
 */
public class AggregateQuerySnapshot {

    /**
     * The AggregateQuery instance associated with this snapshot.
     */
    @NonNull
    public final AggregateQuery query;

    /**
     * The aggregated data as a map of alias to value.
     */
    @NonNull
    private final Map<String, Object> data;

    /**
     * Constructs an AggregateQuerySnapshot with the specified query and data.
     *
     * @param query the aggregate query that produced this snapshot
     * @param data the aggregated data
     */
    protected AggregateQuerySnapshot(@NonNull AggregateQuery query,
                                     @NonNull Map<String, Object> data) {
        this.query = query;
        this.data = data;
    }

    /**
     * Gets the result for the specified aggregate field.
     *
     * @param aggregateField the aggregate field
     * @return the result value, or 0L for sum fields if no data, null otherwise
     */
    @Nullable
    public Object get(@NonNull AggregateField aggregateField) {
        Objects.requireNonNull(aggregateField, "Invalid aggregateField provided");
        Object data = this.data.getOrDefault(aggregateField.alias, null);
        if (data == null) {
            return aggregateField instanceof AggregateField.SumAggregateField ? 0L : null;
        }

        if (data instanceof Integer) {
            return ((Integer) data).longValue();
        }
        return data;
    }

    /**
     * Gets the result as a double for the average aggregate field.
     *
     * @param averageAggregateField the average aggregate field
     * @return the result as a Double, or null if no data
     */
    @Nullable
    public Double get(@NonNull AggregateField.AverageAggregateField averageAggregateField) {
        Objects.requireNonNull(averageAggregateField, "Invalid averageAggregateField provided");
        Object data = get((AggregateField) averageAggregateField);
        if (data == null) {
            return null;
        }

        if (data instanceof Float) {
            return ((Float) data).doubleValue();
        }
        if (data instanceof Integer) {
            return ((Integer) data).doubleValue();
        }
        if (data instanceof Long) {
            return ((Long) data).doubleValue();
        }
        return (Double) data;
    }

    /**
     * Gets the result as a long for the count aggregate field.
     *
     * @param countAggregateField the count aggregate field
     * @return the result as a long
     * @throws RuntimeException if no aggregate field exists with the provided alias
     */
    public long get(@NonNull AggregateField.CountAggregateField countAggregateField) {
        Objects.requireNonNull(countAggregateField, "Invalid countAggregateField provided");
        Object data = get((AggregateField) countAggregateField);

        if (data == null) {
            throw new RuntimeException("No aggregate field exists with the provided alias");
        }

        if (data instanceof Integer) {
            return ((Integer) data).longValue();
        }
        return (Long) data;
    }

    /**
     * Gets the count result for the specified alias.
     *
     * @param alias the alias of the count aggregate field
     * @return the result as a long
     * @throws RuntimeException if no aggregate count field exists with the provided alias
     */
    public long getCount(@NonNull String alias) {
        Objects.requireNonNull(alias, "Invalid alias provided");
        AggregateField aggField = new AggregateField.CountAggregateField(alias, new FieldPath(""));
        Object data = get(aggField);

        if (data == null) {
            throw new RuntimeException("No aggregate count field exists with the provided alias");
        }

        if (data instanceof Integer) {
            return ((Integer) data).longValue();
        }
        return (Long) data;
    }

    /**
     * Gets the result of the aggregate field as a double.
     *
     * @param aggregateField the aggregate field
     * @return the result as a Double, or 0.0 for sum fields if no data, null otherwise
     */
    @Nullable
    public Double getDouble(@NonNull AggregateField aggregateField) {
        Objects.requireNonNull(aggregateField, "Invalid aggregateField provided");
        Object data = get(aggregateField);
        if (data == null) {
            if (aggregateField instanceof AggregateField.SumAggregateField) {
                return 0.0;
            } else {
                return null;
            }
        }

        if (data instanceof Float) {
            return ((Float) data).doubleValue();
        }
        if (data instanceof Integer) {
            return ((Integer) data).doubleValue();
        }
        if (data instanceof Long) {
            return ((Long) data).doubleValue();
        }
        return (Double) data;
    }

    /**
     * Gets the result of the aggregate field as a long.
     *
     * @param aggregateField the aggregate field
     * @return the result as a Long, or 0L for sum fields if no data, null otherwise
     */
    @Nullable
    public Long getLong(@NonNull AggregateField aggregateField) {
        Objects.requireNonNull(aggregateField, "Invalid aggregateField provided");
        Object data = get(aggregateField);
        if (data == null) {
            if (aggregateField instanceof AggregateField.SumAggregateField) {
                return 0L;
            } else {
                return null;
            }
        }

        if (data instanceof Integer) {
            return ((Integer) data).longValue();
        }
        return (Long) data;
    }

    /**
     * Gets the aggregate query associated with this snapshot.
     *
     * @return the aggregate query
     */
    @NonNull
    public AggregateQuery getQuery() {
        return this.query;
    }

    /**
     * Compares this AggregateQuerySnapshot to the specified object for equality.
     *
     * @param obj the object to compare
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AggregateQuerySnapshot that = (AggregateQuerySnapshot) obj;
        return Objects.equals(query, that.query) && Objects.equals(data, that.data);
    }

    /**
     * Returns a hash code value for this AggregateQuerySnapshot.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(query, data);
    }
}
