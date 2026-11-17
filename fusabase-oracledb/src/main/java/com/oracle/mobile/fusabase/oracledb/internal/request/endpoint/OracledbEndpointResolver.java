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

import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.utils.Utils;

import java.util.Objects;

/** Resolves configured ORDS endpoints without executing a request. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class OracledbEndpointResolver {
    private final String ordsHost;
    private final String projectId;

    public OracledbEndpointResolver(@NonNull FusabaseOptions options) {
        Objects.requireNonNull(options, "Fusabase options cannot be null");
        this.ordsHost = Objects.requireNonNull(options.getOrdsHost(),
                "ORDS host cannot be null");
        this.projectId = Objects.requireNonNull(options.getProjectId(),
                "Project ID cannot be null");
    }

    /** Resolves a database endpoint, leaving deleteDocument on its existing unversioned path. */
    @NonNull
    public String resolveDatabase(@NonNull RestOperation operation,
                                  @NonNull OracledbApiVersion apiVersion) {
        Objects.requireNonNull(operation, "REST operation cannot be null");
        Objects.requireNonNull(apiVersion, "API version cannot be null");
        if (operation == RestOperation.AUTHORIZE_SNAPSHOT) {
            throw new IllegalArgumentException("Snapshot authorization is not a database endpoint");
        }
        if (operation == RestOperation.DELETE_DOCUMENT) {
            return Utils.urlBuilder(ordsHost,
                    EndpointCatalog.BAAS_SERVICE,
                    EndpointCatalog.DATABASE,
                    projectId,
                    EndpointCatalog.endpointPath(operation));
        }
        if (apiVersion == OracledbApiVersion.UNVERSIONED) {
            throw new IllegalArgumentException("A versioned operation requires an API version");
        }
        return Utils.urlBuilder(ordsHost,
                EndpointCatalog.BAAS_SERVICE,
                EndpointCatalog.DATABASE,
                projectId,
                apiVersion.getPathSegment(),
                EndpointCatalog.endpointPath(operation));
    }

    /** Resolves the existing WebSocket snapshot-authorization REST endpoint. */
    @NonNull
    public String resolveSnapshotAuthorization(@NonNull String authPath) {
        if (authPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Snapshot authorization path cannot be empty");
        }
        return Utils.urlBuilder(ordsHost,
                EndpointCatalog.BAAS_SERVICE,
                EndpointCatalog.IDM,
                authPath,
                projectId,
                EndpointCatalog.AUTHORIZE_SNAPSHOT);
    }
}
