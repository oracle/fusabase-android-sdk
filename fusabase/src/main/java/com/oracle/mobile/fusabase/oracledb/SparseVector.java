package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Represents a sparse embedding payload.
 */
public class SparseVector {
    private final String type;
    private final int dimension;
    private final List<Integer> indices;
    private final List<Double> values;

    SparseVector(int dimension, @NonNull List<Integer> indices, @NonNull List<Double> values) {
        this.type = "sparse";
        this.dimension = dimension;
        this.indices = indices;
        this.values = values;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public int getDimension() {
        return dimension;
    }

    @NonNull
    public List<Integer> getIndices() {
        return indices;
    }

    @NonNull
    public List<Double> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SparseVector)) return false;
        SparseVector that = (SparseVector) o;
        return dimension == that.dimension && Objects.equals(type, that.type) &&
                Objects.equals(indices, that.indices) && Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, dimension, indices, values);
    }
}
