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

import java.util.List;

/**
 * Validation helper for vector APIs.
 */
final class VectorValidators {

    private VectorValidators() {}

    static void validateDenseVector(@NonNull List<Double> values, @NonNull String message) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        for (Double value : values) {
            if (value == null || Double.isNaN(value)) {
                throw new IllegalArgumentException(message);
            }
        }
    }

    static void validateSparseEmbedding(@NonNull SparseVector sparse) {
        if (sparse.getDimension() <= 0) {
            throw new IllegalArgumentException("Sparse embedding dimension must be an integer greater than 0.");
        }
        if (sparse.getIndices() == null || sparse.getValues() == null ||
                sparse.getIndices().size() != sparse.getValues().size()) {
            throw new IllegalArgumentException("Sparse embedding indices and values must have the same length.");
        }
        for (Integer index : sparse.getIndices()) {
            if (index == null || index < 0) {
                throw new IllegalArgumentException("Sparse embedding indices must be non-negative integers.");
            }
        }
        validateDenseVector(sparse.getValues(), "Sparse embedding values must be a numeric array.");
    }

    static void validateVectorSearchQuery(@NonNull FindNearestQuery query) {
        boolean hasDense = query.getVector() != null;
        boolean hasSparse = query.getSparse() != null;
        if ((hasDense && hasSparse) || (!hasDense && !hasSparse)) {
            throw new IllegalArgumentException("Vector search query must contain exactly one of vector or sparse.");
        }
        if (hasDense) {
            validateDenseVector(query.getVector(), "Vector search query.vector must be a numeric array.");
        }
        if (hasSparse) {
            validateSparseEmbedding(query.getSparse());
        }
    }

    static void validateVectorSearchOptions(FindNearestOptions options) {
        if (options == null) {
            return;
        }
        if (options.getTopK() != null && options.getTopK() <= 0) {
            throw new IllegalArgumentException("vectorSearch topK must be a positive integer.");
        }
        if (options.getThreshold() != null && Double.isNaN(options.getThreshold())) {
            throw new IllegalArgumentException("vectorSearch threshold must be a valid number.");
        }
    }
}
