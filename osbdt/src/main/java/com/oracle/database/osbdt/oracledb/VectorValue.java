package com.oracle.database.osbdt.oracledb;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Helper factory methods for vector payloads.
 */
public final class VectorValue {

    private VectorValue() {}

    @NonNull
    public static DenseEmbedding denseEmbedding(@NonNull List<Double> values) {
        VectorValidators.validateDenseVector(values, "denseEmbedding values must be a numeric array.");
        return new DenseEmbedding(values);
    }

    @NonNull
    public static SparseEmbedding sparseEmbedding(int dimension,
                                                  @NonNull List<Integer> indices,
                                                  @NonNull List<Double> values) {
        SparseEmbedding sparse = new SparseEmbedding(dimension, indices, values);
        VectorValidators.validateSparseEmbedding(sparse);
        return sparse;
    }
}
