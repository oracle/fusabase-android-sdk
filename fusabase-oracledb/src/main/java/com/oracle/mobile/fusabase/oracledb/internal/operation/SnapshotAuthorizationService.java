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


package com.oracle.mobile.fusabase.oracledb.internal.operation;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.error.DocumentErrorMapper;
import com.oracle.mobile.fusabase.oracledb.internal.error.ErrorCategory;
import com.oracle.mobile.fusabase.oracledb.internal.error.InternalOperationException;
import com.oracle.mobile.fusabase.oracledb.internal.error.OperationErrorMapper;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.SnapshotAuthorizationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.request.SnapshotAuthorizationRequestProvider;
import com.oracle.mobile.fusabase.oracledb.internal.response.mapper.ResponseMapper;
import com.oracle.mobile.fusabase.oracledb.internal.response.mapper.SnapshotAuthorizationResponseMapper;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.SnapshotAuthorizationResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.reader.JsonRawResponseReader;
import com.oracle.mobile.fusabase.oracledb.internal.response.reader.ResponseReader;
import com.oracle.mobile.fusabase.oracledb.internal.transport.RestResponse;
import com.oracle.mobile.fusabase.oracledb.internal.transport.RestTransport;

import java.util.Objects;

import jakarta.json.JsonObject;

/** Executes and processes the real-time snapshot authorization operation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SnapshotAuthorizationService {
    private static final String TAG = "OracledbSnapshotService";

    private final SnapshotAuthorizationRequestProvider requestProvider;
    private final RestTransport transport;
    private final ResponseReader<JsonObject> responseReader;
    private final ResponseMapper<JsonObject, SnapshotAuthorizationResponse> responseMapper;
    private final OperationErrorMapper errorMapper;

    public SnapshotAuthorizationService(@NonNull SnapshotAuthorizationRequestProvider requestProvider,
                                        @NonNull RestTransport transport) {
        this(requestProvider, transport, new JsonRawResponseReader(),
                new SnapshotAuthorizationResponseMapper(), new DocumentErrorMapper());
    }

    public SnapshotAuthorizationService(@NonNull SnapshotAuthorizationRequestProvider requestProvider,
                                        @NonNull RestTransport transport,
                                        @NonNull ResponseReader<JsonObject> responseReader,
                                        @NonNull ResponseMapper<JsonObject,
                                                SnapshotAuthorizationResponse> responseMapper,
                                        @NonNull OperationErrorMapper errorMapper) {
        this.requestProvider = Objects.requireNonNull(requestProvider,
                "Snapshot request provider cannot be null");
        this.transport = Objects.requireNonNull(transport, "REST transport cannot be null");
        this.responseReader = Objects.requireNonNull(responseReader,
                "Response reader cannot be null");
        this.responseMapper = Objects.requireNonNull(responseMapper,
                "Response mapper cannot be null");
        this.errorMapper = Objects.requireNonNull(errorMapper, "Error mapper cannot be null");
    }

    @NonNull
    public SnapshotAuthorizationResponse authorize(@NonNull String authPath)
            throws FusabaseOracledbException {
        try {
            FusabaseLogger.v(TAG, "Creating snapshot authorization request");
            SnapshotAuthorizationCommand command = new SnapshotAuthorizationCommand(authPath);
            RestResponse response = transport.execute(requestProvider.create(command));
            JsonObject payload;
            try {
                payload = responseReader.read(response);
            } catch (InternalOperationException failure) {
                // A test or alternate transport may supply a response without an operation
                // context. Preserve the HTTP status directly in that case.
                if (failure.getCategory() == ErrorCategory.HTTP) {
                    throw errorMapper.mapHttpStatus(response.getStatusCode());
                }
                throw failure;
            }
            SnapshotAuthorizationResponse result = responseMapper.map(payload);
            FusabaseLogger.i(TAG, "Snapshot authorization response processed");
            return result;
        } catch (FusabaseOracledbException failure) {
            throw failure;
        } catch (Exception failure) {
            FusabaseLogger.e(TAG, "Snapshot authorization operation failed");
            throw errorMapper.map(failure);
        }
    }
}
