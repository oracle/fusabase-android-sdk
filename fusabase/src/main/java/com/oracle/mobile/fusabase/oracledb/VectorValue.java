package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Helper factory methods for vector payloads.
 */
public final class VectorValue {

    private VectorValue() {}

    @NonNull
    public static DenseVector denseVector(@NonNull List<Double> values) {
        VectorValidators.validateDenseVector(values, "denseVector values must be a numeric array.");
        return new DenseVector(values);
    }

    @NonNull
    public static SparseVector sparseVector(int dimension,
                                            @NonNull List<Integer> indices,
                                            @NonNull List<Double> values) {
        SparseVector sparse = new SparseVector(dimension, indices, values);
        VectorValidators.validateSparseEmbedding(sparse);
        return sparse;
    }
}
