package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Represents a dense embedding payload.
 */
public class DenseVector {
    private final String type;
    private final List<Double> values;

    DenseVector(@NonNull List<Double> values) {
        this.type = "dense";
        this.values = values;
    }

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    public List<Double> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DenseVector)) return false;
        DenseVector that = (DenseVector) o;
        return Objects.equals(type, that.type) && Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, values);
    }
}
