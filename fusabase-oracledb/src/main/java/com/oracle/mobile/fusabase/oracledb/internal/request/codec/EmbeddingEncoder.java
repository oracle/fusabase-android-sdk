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


package com.oracle.mobile.fusabase.oracledb.internal.request.codec;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.DenseVector;
import com.oracle.mobile.fusabase.oracledb.SparseVector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Encodes dense and sparse document embeddings into the V2 wire shape. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class EmbeddingEncoder {

    public boolean isEmbeddingInput(Object value) {
        return value instanceof DenseVector || value instanceof SparseVector;
    }

    @NonNull
    public Map<String, Object> encode(@NonNull Object value) {
        if (value instanceof DenseVector) {
            DenseVector dense = (DenseVector) value;
            List<Double> values = copyDenseValues(dense.getValues(),
                    "Embedding values must be a numeric array.");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "dense");
            result.put("values", values);
            return result;
        }
        if (value instanceof SparseVector) {
            SparseVector sparse = (SparseVector) value;
            if (sparse.getDimension() <= 0) {
                throw new IllegalArgumentException(
                        "Sparse embedding dimension must be an integer greater than 0.");
            }
            if (sparse.getIndices() == null || sparse.getValues() == null ||
                    sparse.getIndices().size() != sparse.getValues().size()) {
                throw new IllegalArgumentException(
                        "Sparse embedding indices and values must have the same length.");
            }
            List<Integer> indices = new ArrayList<>(sparse.getIndices().size());
            for (Integer index : sparse.getIndices()) {
                if (index == null || index < 0) {
                    throw new IllegalArgumentException(
                            "Sparse embedding indices must be non-negative integers.");
                }
                indices.add(index);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "sparse");
            result.put("dimension", sparse.getDimension());
            result.put("indices", indices);
            result.put("values", copyDenseValues(sparse.getValues(),
                    "Sparse embedding values must be a numeric array."));
            return result;
        }
        throw new IllegalArgumentException("Unsupported embedding value");
    }

    @NonNull
    private static List<Double> copyDenseValues(@NonNull List<Double> source,
                                                @NonNull String message) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        List<Double> values = new ArrayList<>(source.size());
        for (Double value : source) {
            if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException(message);
            }
            values.add(value);
        }
        return values;
    }
}
