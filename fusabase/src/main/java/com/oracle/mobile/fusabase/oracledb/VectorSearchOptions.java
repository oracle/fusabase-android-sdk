package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Optional parameters for vector similarity queries.
 */
public class VectorSearchOptions {
    private final VectorMetric metric;
    private final Integer topK;
    private final Double threshold;

    private VectorSearchOptions(@Nullable VectorMetric metric, @Nullable Integer topK, @Nullable Double threshold) {
        this.metric = metric;
        this.topK = topK;
        this.threshold = threshold;
    }

    @Nullable
    public VectorMetric getMetric() {
        return metric;
    }

    @Nullable
    public Integer getTopK() {
        return topK;
    }

    @Nullable
    public Double getThreshold() {
        return threshold;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private VectorMetric metric;
        private Integer topK;
        private Double threshold;

        public Builder metric(@Nullable VectorMetric metric) {
            this.metric = metric;
            return this;
        }

        public Builder topK(@Nullable Integer topK) {
            this.topK = topK;
            return this;
        }

        public Builder threshold(@Nullable Double threshold) {
            this.threshold = threshold;
            return this;
        }

        public VectorSearchOptions build() {
            return new VectorSearchOptions(metric, topK, threshold);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VectorSearchOptions)) return false;
        VectorSearchOptions that = (VectorSearchOptions) o;
        return metric == that.metric && Objects.equals(topK, that.topK) && Objects.equals(threshold, that.threshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metric, topK, threshold);
    }
}
