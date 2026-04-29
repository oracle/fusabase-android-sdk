package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents a find-nearest query payload. Exactly one of dense or sparse must be set.
 */
public class FindNearestQuery {

    private final List<Double> vector;
    private final SparseVector sparse;

    private FindNearestQuery(@Nullable List<Double> vector, @Nullable SparseVector sparse) {
        this.vector = vector;
        this.sparse = sparse;
    }

    @NonNull
    public static FindNearestQuery dense(@NonNull List<Double> vector) {
        VectorValidators.validateDenseVector(vector, "Vector search query.vector must be a numeric array.");
        return new FindNearestQuery(vector, null);
    }

    @NonNull
    public static FindNearestQuery sparse(@NonNull SparseVector sparse) {
        VectorValidators.validateSparseEmbedding(sparse);
        return new FindNearestQuery(null, sparse);
    }

    @Nullable
    public List<Double> getVector() {
        return vector;
    }

    @Nullable
    public SparseVector getSparse() {
        return sparse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FindNearestQuery)) return false;
        FindNearestQuery that = (FindNearestQuery) o;
        return Objects.equals(vector, that.vector) && Objects.equals(sparse, that.sparse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vector, sparse);
    }
}
