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
import androidx.annotation.RestrictTo;

/**
 * Central catalog of existing ORDS endpoint path segments.
 *
 * <p>These values intentionally mirror the current QueryHelper and WebSocket contracts. URL
 * joining remains delegated to the existing trusted utility in the resolver.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class EndpointCatalog {
    public static final String BAAS_SERVICE = "/_/baas-services";
    public static final String DATABASE = "/database";
    public static final String IDM = "/idm";
    public static final String ONPREM = "/onprem";
    public static final String IDCS = "/idcs";
    public static final String AUTHORIZE_SNAPSHOT = "/authorizeSnapshot";
    public static final String RUN_QUERY = "/runQuery";
    public static final String DELETE_DOCUMENT = "/deleteDocument";
    public static final String SET_DOCUMENT = "/setDocument";
    public static final String UPDATE_DOCUMENT = "/updateDocument";
    public static final String REALTIME = "/ords/baas-realtime/";

    private EndpointCatalog() { }

    @NonNull
    static String endpointPath(@NonNull RestOperation operation) {
        switch (operation) {
            case RUN_QUERY:
                return RUN_QUERY;
            case SET_DOCUMENT:
                return SET_DOCUMENT;
            case UPDATE_DOCUMENT:
                return UPDATE_DOCUMENT;
            case DELETE_DOCUMENT:
                return DELETE_DOCUMENT;
            case AUTHORIZE_SNAPSHOT:
                return AUTHORIZE_SNAPSHOT;
            default:
                throw new IllegalArgumentException("Unsupported REST operation");
        }
    }
}
