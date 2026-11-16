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


package com.oracle.mobile.fusabase.oracledb.internal.component;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.offline.DocumentRemoteMutationExecutor;
import com.oracle.mobile.fusabase.oracledb.internal.offline.OfflineReplayRegistry;
import com.oracle.mobile.fusabase.oracledb.internal.offline.OfflineStoreManagerAdapter;
import com.oracle.mobile.fusabase.oracledb.internal.offline.OfflineStorePort;
import com.oracle.mobile.fusabase.oracledb.internal.offline.RemoteMutationExecutor;
import com.oracle.mobile.fusabase.oracledb.internal.operation.DocumentOperationCoordinator;
import com.oracle.mobile.fusabase.oracledb.internal.operation.DocumentRestService;
import com.oracle.mobile.fusabase.oracledb.internal.operation.SnapshotAuthorizationService;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentGetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;
import com.oracle.mobile.fusabase.oracledb.internal.request.DocumentRequestProvider;
import com.oracle.mobile.fusabase.oracledb.internal.request.OracledbDocumentRequestProvider;
import com.oracle.mobile.fusabase.oracledb.internal.request.OracledbSnapshotAuthorizationRequestProvider;
import com.oracle.mobile.fusabase.oracledb.internal.request.codec.RemoteReadPort;
import com.oracle.mobile.fusabase.oracledb.internal.error.DocumentErrorMapper;
import com.oracle.mobile.fusabase.oracledb.internal.response.mapper.DocumentResponseMapper;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentReadResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.processor.DocumentResponseProcessor;
import com.oracle.mobile.fusabase.oracledb.internal.response.reader.JsonRawResponseReader;
import com.oracle.mobile.fusabase.oracledb.internal.transport.HttpRequestHelperTransport;
import com.oracle.mobile.fusabase.oracledb.internal.transport.RestTransport;
import com.oracle.mobile.fusabase.oracledb.internal.validation.OperationCommandFactory;

import com.oracle.mobile.fusabase.oracledb.internal.request.header.AuthorizationHeaderProvider;
import com.oracle.mobile.fusabase.oracledb.internal.request.header.AuthInteropAuthorizationHeaderProvider;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.OracledbEndpointResolver;
import java.util.Objects;

import jakarta.json.JsonObject;

/**
 * Internal composition root for one OracleDB client.
 *
 * <p>The graph owns one request, transport, response, and operation coordinator per OracleDB
 * client. Public operation classes obtain the coordinator through their existing client.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class OracledbComponentGraph {
    private final FusabaseOracledb client;
    private final DocumentRequestProvider requestProvider;
    private final RestTransport transport;
    private final DocumentRestService restService;
    private final SnapshotAuthorizationService snapshotAuthorizationService;
    private final OfflineStorePort offlineStore;
    private final RemoteMutationExecutor remoteMutationExecutor;
    private final OperationCommandFactory commandFactory;
    private final DocumentOperationCoordinator coordinator;

    private OracledbComponentGraph(@NonNull FusabaseOracledb client,
                                   @NonNull FusabaseOptions options,
                                   boolean registerOfflineComponents) {
        this.client = Objects.requireNonNull(client, "OracleDB client cannot be null");
        DeferredRemoteReadPort remoteReadPort = new DeferredRemoteReadPort();
        AuthorizationHeaderProvider auth =
                new AuthInteropAuthorizationHeaderProvider(client.getApp());
        this.requestProvider = new OracledbDocumentRequestProvider(
                options, auth, remoteReadPort);
        this.transport = new HttpRequestHelperTransport();
        this.restService = new DocumentRestService(requestProvider, transport,
                new JsonRawResponseReader(), new DocumentResponseMapper(),
                new DocumentResponseProcessor(), new DocumentErrorMapper());
        this.snapshotAuthorizationService = new SnapshotAuthorizationService(
                new OracledbSnapshotAuthorizationRequestProvider(
                        new OracledbEndpointResolver(options),
                        options.getAppId(), client.getOracledbSettings().getMaxAttempt(), auth), transport);
        remoteReadPort.bind(restService);
        this.commandFactory = new OperationCommandFactory();
        this.offlineStore = new OfflineStoreManagerAdapter(client);
        this.remoteMutationExecutor = new DocumentRemoteMutationExecutor(restService,
                commandFactory);
        this.coordinator = new DocumentOperationCoordinator(restService, offlineStore,
                remoteMutationExecutor, commandFactory);
        if (registerOfflineComponents) {
            OfflineReplayRegistry.register(client, remoteMutationExecutor);
        }
    }

    /** Creates the graph owned by one OracleDB client. */
    @NonNull
    public static OracledbComponentGraph create(@NonNull FusabaseOracledb client) {
        return new OracledbComponentGraph(client, client.getApp().getOptions(), true);
    }

    /** Creates an isolated graph for an internal caller using an explicit request configuration. */
    @NonNull
    public static OracledbComponentGraph create(@NonNull FusabaseOracledb client,
                                                @NonNull FusabaseOptions options) {
        return new OracledbComponentGraph(client, options, false);
    }

    /** Returns the coordinator owned by this OracleDB client. */
    @NonNull
    public DocumentOperationCoordinator getCoordinator() {
        return coordinator;
    }

    /** Returns the snapshot authorization service owned by this OracleDB client. */
    @NonNull
    public SnapshotAuthorizationService getSnapshotAuthorizationService() {
        return snapshotAuthorizationService;
    }

    /**
     * Breaks the request-provider/service composition cycle used by API v1 merge preparation.
     * The port is bound before any public operation can execute, while construction remains
     * deterministic and dependency-injected.
     */
    private static final class DeferredRemoteReadPort implements RemoteReadPort {
        private DocumentRestService restService;

        void bind(@NonNull DocumentRestService service) {
            if (restService != null) {
                throw new IllegalStateException("Remote read port is already bound");
            }
            restService = Objects.requireNonNull(service, "REST service cannot be null");
        }

        @NonNull
        @Override
        public JsonObject read(@NonNull java.util.List<String> path,
                               @NonNull JsonObject payload,
                               @Nullable TransactionContext context)
                throws FusabaseOracledbException {
            if (restService == null) {
                throw new FusabaseOracledbException(
                        "Remote read port is not initialized",
                        FusabaseOracledbException.Code.INTERNAL);
            }
            DocumentReadResponse response = (DocumentReadResponse) restService.execute(
                    new DocumentGetCommand(path, payload, context));
            return response.getPayload();
        }
    }

}
