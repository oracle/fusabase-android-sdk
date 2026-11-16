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


package com.oracle.mobile.fusabase.oracledb.internal.request.endpoint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/** API-version choices used to resolve versioned OracleDB endpoints. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum OracledbApiVersion {
    V1("1.0", "/v1"),
    V2("2.0", "/v2"),
    UNVERSIONED("", "");

    private final String configuredValue;
    private final String pathSegment;

    OracledbApiVersion(@NonNull String configuredValue,
                       @NonNull String pathSegment) {
        this.configuredValue = configuredValue;
        this.pathSegment = pathSegment;
    }

    /** Matches QueryHelper's current behavior: only exactly 2.0 selects V2. */
    @NonNull
    public static OracledbApiVersion fromConfiguredValue(@Nullable String configuredValue) {
        return "2.0".equals(configuredValue) ? V2 : V1;
    }

    @NonNull
    public String getConfiguredValue() {
        return configuredValue;
    }

    @NonNull
    public String getPathSegment() {
        return pathSegment;
    }
}
