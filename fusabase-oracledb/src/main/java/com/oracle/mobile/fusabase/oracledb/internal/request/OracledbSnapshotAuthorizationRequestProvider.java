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


package com.oracle.mobile.fusabase.oracledb.internal.request;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.SnapshotAuthorizationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.OracledbApiVersion;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.OracledbEndpointResolver;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.RestHttpMethod;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.RestOperation;
import com.oracle.mobile.fusabase.oracledb.internal.request.header.AuthorizationHeaderProvider;
import com.oracle.mobile.fusabase.oracledb.internal.request.header.PreferenceAuthorizationHeaderProvider;
import com.oracle.mobile.fusabase.oracledb.internal.request.model.RequestMetadata;
import com.oracle.mobile.fusabase.oracledb.internal.request.model.RestRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Creates the existing snapshot-authorization request without executing transport. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class OracledbSnapshotAuthorizationRequestProvider
        implements SnapshotAuthorizationRequestProvider {
    private static final String TAG = "OracledbSnapshotRequest";

    private final OracledbEndpointResolver endpointResolver;
    private final String appId;
    private final int retryAttempts;
    private final AuthorizationHeaderProvider authorizationHeaderProvider;

    /** Creates the production provider using the existing preference-backed auth source. */
    public OracledbSnapshotAuthorizationRequestProvider(@NonNull FusabaseOptions options,
                                                       @NonNull Context context,
                                                       int retryAttempts) {
        this(new OracledbEndpointResolver(options), options.getAppId(), retryAttempts,
                new PreferenceAuthorizationHeaderProvider(context));
    }

    /** Creates an injectable provider for deterministic request tests. */
    public OracledbSnapshotAuthorizationRequestProvider(
            @NonNull OracledbEndpointResolver endpointResolver,
            @NonNull String appId,
            int retryAttempts,
            @NonNull AuthorizationHeaderProvider authorizationHeaderProvider) {
        this.endpointResolver = Objects.requireNonNull(endpointResolver,
                "Endpoint resolver cannot be null");
        this.appId = Objects.requireNonNull(appId, "App ID cannot be null");
        if (retryAttempts < 1) {
            throw new IllegalArgumentException("Retry attempts must be positive");
        }
        this.retryAttempts = retryAttempts;
        this.authorizationHeaderProvider = Objects.requireNonNull(authorizationHeaderProvider,
                "Authorization header provider cannot be null");
    }

    @NonNull
    @Override
    public RestRequest create(@NonNull SnapshotAuthorizationCommand command)
            throws FusabaseOracledbException {
        Objects.requireNonNull(command, "Snapshot authorization command cannot be null");
        String authorizationHeader = authorizationHeaderProvider.getAuthorizationHeader();
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            FusabaseLogger.w(TAG, "Snapshot authorization request has no credential");
            throw new FusabaseOracledbException("User Not logged in",
                    FusabaseOracledbException.Code.UNAUTHENTICATED);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", authorizationHeader);
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("apiKey", appId);

        RequestMetadata metadata = new RequestMetadata(RestOperation.AUTHORIZE_SNAPSHOT,
                OracledbApiVersion.UNVERSIONED, retryAttempts, true, false, false);
        FusabaseLogger.v(TAG, "Prepared snapshot authorization request intent");
        FusabaseLogger.i(TAG, "Prepared GET request for snapshot authorization");
        return new RestRequest(command, RestOperation.AUTHORIZE_SNAPSHOT, RestHttpMethod.GET,
                endpointResolver.resolveSnapshotAuthorization(command.getAuthPath()), headers,
                queryParameters, null, metadata);
    }
}
