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

import java.util.List;
import java.util.Objects;

/**
 * Represents a find-nearest query payload. Exactly one of dense or sparse must be set.
 *
 * <p>Example:</p>
 * <pre>{@code
 * FindNearestQuery vector = FindNearestQuery.dense(Arrays.asList(0.22, 0.93));
 * }</pre>
 */
public class FindNearestQuery {

    private final List<Double> vector;
    private final SparseVector sparse;

    private FindNearestQuery(@Nullable List<Double> vector, @Nullable SparseVector sparse) {
        this.vector = vector;
        this.sparse = sparse;
    }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FindNearestQuery query = FindNearestQuery.dense(Arrays.asList(0.22, 0.93));
     * }</pre>
     */@NonNull
    public static FindNearestQuery dense(@NonNull List<Double> vector) {
        VectorValidators.validateDenseVector(vector, "Vector search query.vector must be a numeric array.");
        return new FindNearestQuery(vector, null);
    }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FindNearestQuery query = FindNearestQuery.sparse(VectorValue.sparseVector(4, Arrays.asList(0), Arrays.asList(0.22)));
     * }</pre>
     */@NonNull
    public static FindNearestQuery sparse(@NonNull SparseVector sparse) {
        VectorValidators.validateSparseEmbedding(sparse);
        return new FindNearestQuery(null, sparse);
    }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FindNearestQuery query = FindNearestQuery.getVector(VectorValue.sparseVector(4, Arrays.asList(0), Arrays.asList(0.22)));
     * }</pre>
     */@Nullable
    public List<Double> getVector() {
        return vector;
    }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FindNearestQuery query = FindNearestQuery.getSparse(VectorValue.sparseVector(4, Arrays.asList(0), Arrays.asList(0.22)));
     * }</pre>
     */@Nullable
    public SparseVector getSparse() {
        return sparse;
    }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example: Given compatible values named as shown.</p>
     * <pre>{@code
     * boolean same = first.equals(second);
     * }</pre>
     */@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FindNearestQuery)) return false;
        FindNearestQuery that = (FindNearestQuery) o;
        return Objects.equals(vector, that.vector) && Objects.equals(sparse, that.sparse);
    }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example: Given compatible values named as shown.</p>
     * <pre>{@code
     * int hash = value.hashCode();
     * }</pre>
     */@Override
    public int hashCode() {
        return Objects.hash(vector, sparse);
    }
}
