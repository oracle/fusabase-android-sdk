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
import androidx.annotation.RestrictTo;

import java.util.Arrays;
import java.util.Objects;

/**
 * Root-package bridge for the protected FieldValue contract.
 *
 * <p>The payload codecs live in child packages, while FieldValue intentionally keeps its
 * operation details protected. This restricted bridge preserves that public API boundary and
 * prevents the codecs from using reflection to inspect field operations.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * FusabaseOracledb db = FusabaseOracledb.getInstance();
 * }</pre>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class FieldValueOperationEncoder {
    private FieldValueOperationEncoder() { }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * }</pre>
     */@NonNull
    public static String operationName(@NonNull FieldValue value) {
        Objects.requireNonNull(value, "FieldValue cannot be null");
        return value.getOperation().getOperationInString();
    }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * }</pre>
     */@NonNull
    public static Object[] values(@NonNull FieldValue value) {
        Objects.requireNonNull(value, "FieldValue cannot be null");
        return Arrays.copyOf(value.getValues(), value.getValues().length);
    }
}
