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
