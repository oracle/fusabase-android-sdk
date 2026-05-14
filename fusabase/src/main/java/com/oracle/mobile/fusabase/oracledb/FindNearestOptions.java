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

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Optional parameters for find-nearest queries.
 */
public class FindNearestOptions {
    private final VectorMetric metric;
    private final Integer topK;
    private final Double threshold;

    private FindNearestOptions(@Nullable VectorMetric metric, @Nullable Integer topK, @Nullable Double threshold) {
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

        public FindNearestOptions build() {
            return new FindNearestOptions(metric, topK, threshold);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FindNearestOptions)) return false;
        FindNearestOptions that = (FindNearestOptions) o;
        return metric == that.metric && Objects.equals(topK, that.topK) && Objects.equals(threshold, that.threshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metric, topK, threshold);
    }
}
