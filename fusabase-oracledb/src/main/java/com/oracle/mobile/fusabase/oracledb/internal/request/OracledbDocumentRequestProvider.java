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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.operation.OperationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentDeleteCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentGetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentSetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentUpdateCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.QueryCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.OracledbApiVersion;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.OracledbEndpointResolver;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.RestHttpMethod;
import com.oracle.mobile.fusabase.oracledb.internal.request.endpoint.RestOperation;
import com.oracle.mobile.fusabase.oracledb.internal.request.header.AuthorizationHeaderProvider;
import com.oracle.mobile.fusabase.oracledb.internal.request.header.PreferenceAuthorizationHeaderProvider;
import com.oracle.mobile.fusabase.oracledb.internal.request.header.TransactionHeaderFactory;
import com.oracle.mobile.fusabase.oracledb.internal.request.codec.PayloadCodec;
import com.oracle.mobile.fusabase.oracledb.internal.request.codec.QueryPayloadCodec;
import com.oracle.mobile.fusabase.oracledb.internal.request.codec.RemoteReadPort;
import com.oracle.mobile.fusabase.oracledb.internal.request.codec.V1DocumentPayloadCodec;
import com.oracle.mobile.fusabase.oracledb.internal.request.codec.V2DocumentPayloadCodec;
import com.oracle.mobile.fusabase.oracledb.internal.request.model.RequestMetadata;
import com.oracle.mobile.fusabase.oracledb.internal.request.model.RestRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Creates REST request intents for supported document commands without executing transport.
 *
 * <p>Payload JSON is produced by the format-specific codecs. This provider owns selection of the
 * endpoint, method, headers, query, codec, and request metadata; it never executes transport.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class OracledbDocumentRequestProvider implements DocumentRequestProvider {
    private static final String TAG = "OracledbRequestProvider";

    private final OracledbEndpointResolver endpointResolver;
    private final String appId;
    private final OracledbApiVersion configuredApiVersion;
    private final AuthorizationHeaderProvider authorizationHeaderProvider;
    private final TransactionHeaderFactory transactionHeaderFactory;
    private final PayloadCodec<OperationCommand<?>> v1DocumentPayloadCodec;
    private final PayloadCodec<OperationCommand<?>> v2DocumentPayloadCodec;
    private final PayloadCodec<OperationCommand<?>> queryPayloadCodec;

    /** Creates a provider using the existing preference-backed authorization mechanism. */
    public OracledbDocumentRequestProvider(@NonNull FusabaseOptions options,
                                           @NonNull android.content.Context context) {
        this(options, new PreferenceAuthorizationHeaderProvider(context));
    }

    /** Creates a provider with the production authorization source and V1 read dependency. */
    public OracledbDocumentRequestProvider(@NonNull FusabaseOptions options,
                                           @NonNull android.content.Context context,
                                           @NonNull RemoteReadPort remoteReadPort) {
        this(options, new PreferenceAuthorizationHeaderProvider(context), remoteReadPort);
    }

    /** Creates a provider with an injectable authorization source for deterministic tests. */
    public OracledbDocumentRequestProvider(@NonNull FusabaseOptions options,
                                           @NonNull AuthorizationHeaderProvider
                                                   authorizationHeaderProvider) {
        this(new OracledbEndpointResolver(options), options.getAppId(),
                OracledbApiVersion.fromConfiguredValue(options.getApiVersion()),
                authorizationHeaderProvider, new TransactionHeaderFactory(),
                new UnavailableRemoteReadPort());
    }

    /** Creates a provider with an explicit V1 read-before-merge dependency. */
    public OracledbDocumentRequestProvider(@NonNull FusabaseOptions options,
                                           @NonNull AuthorizationHeaderProvider
                                                   authorizationHeaderProvider,
                                           @NonNull RemoteReadPort remoteReadPort) {
        this(new OracledbEndpointResolver(options), options.getAppId(),
                OracledbApiVersion.fromConfiguredValue(options.getApiVersion()),
                authorizationHeaderProvider, new TransactionHeaderFactory(), remoteReadPort);
    }

    /** Creates a provider with all request dependencies explicitly supplied. */
    public OracledbDocumentRequestProvider(@NonNull OracledbEndpointResolver endpointResolver,
                                           @NonNull String appId,
                                           @NonNull OracledbApiVersion configuredApiVersion,
                                           @NonNull AuthorizationHeaderProvider
                                                   authorizationHeaderProvider,
                                           @NonNull TransactionHeaderFactory
                                                   transactionHeaderFactory) {
        this(endpointResolver, appId, configuredApiVersion, authorizationHeaderProvider,
                transactionHeaderFactory, new UnavailableRemoteReadPort());
    }

    /** Creates a provider with all request and payload dependencies explicitly supplied. */
    public OracledbDocumentRequestProvider(@NonNull OracledbEndpointResolver endpointResolver,
                                           @NonNull String appId,
                                           @NonNull OracledbApiVersion configuredApiVersion,
                                           @NonNull AuthorizationHeaderProvider
                                                   authorizationHeaderProvider,
                                           @NonNull TransactionHeaderFactory
                                                   transactionHeaderFactory,
                                           @NonNull RemoteReadPort remoteReadPort) {
        this.endpointResolver = Objects.requireNonNull(endpointResolver,
                "Endpoint resolver cannot be null");
        this.appId = Objects.requireNonNull(appId, "App ID cannot be null");
        this.configuredApiVersion = Objects.requireNonNull(configuredApiVersion,
                "API version cannot be null");
        this.authorizationHeaderProvider = Objects.requireNonNull(authorizationHeaderProvider,
                "Authorization header provider cannot be null");
        this.transactionHeaderFactory = Objects.requireNonNull(transactionHeaderFactory,
                "Transaction header factory cannot be null");
        Objects.requireNonNull(remoteReadPort, "Remote read port cannot be null");
        this.v1DocumentPayloadCodec = new V1DocumentPayloadCodec(remoteReadPort);
        this.v2DocumentPayloadCodec = new V2DocumentPayloadCodec();
        this.queryPayloadCodec = new QueryPayloadCodec();
    }

    @NonNull
    @Override
    public <TResult> RestRequest create(@NonNull OperationCommand<TResult> command)
            throws FusabaseOracledbException {
        Objects.requireNonNull(command, "Operation command cannot be null");
        RestOperation operation = operationFor(command);
        OracledbApiVersion apiVersion = operation == RestOperation.DELETE_DOCUMENT
                ? OracledbApiVersion.UNVERSIONED : configuredApiVersion;
        RestHttpMethod method = methodFor(operation);
        String url = endpointResolver.resolveDatabase(operation, apiVersion);
        TransactionContext transactionContext = transactionContextFor(command);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-transaction", transactionHeaderFactory.create(transactionContext).toString());
        headers.put("Content-Type", "application/json");

        String authorizationHeader = authorizationHeaderProvider.getAuthorizationHeader();
        boolean authorizationIncluded = authorizationHeader != null &&
                !authorizationHeader.isEmpty();
        if (authorizationIncluded) {
            headers.put("Authorization", authorizationHeader);
        } else {
            FusabaseLogger.w(TAG, "No authorization header added for " + operation);
        }

        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("apiKey", appId);

        String body;
        try {
            body = payloadFor(command, apiVersion).toString();
        } catch (FusabaseOracledbException e) {
            FusabaseLogger.w(TAG, "Payload preparation failed for " + operation + " with code " +
                    e.getErrorCode());
            throw e;
        } catch (RuntimeException e) {
            FusabaseLogger.e(TAG, "Payload preparation failed for " + operation, e);
            throw e;
        }

        int retryAttempts = retryAttemptsFor(transactionContext);
        RequestMetadata metadata = new RequestMetadata(operation, apiVersion, retryAttempts,
                authorizationIncluded, true, true);
        FusabaseLogger.v(TAG, "Prepared " + operation + " request intent");
        FusabaseLogger.i(TAG, "Prepared " + method.getWireName() + " request for " + operation +
                " using " + apiVersion.getConfiguredValue() + " API metadata");
        return new RestRequest(command, operation, method, url, headers, queryParameters,
                body, metadata);
    }

    @NonNull
    private jakarta.json.JsonObject payloadFor(@NonNull OperationCommand<?> command,
                                               @NonNull OracledbApiVersion apiVersion)
            throws FusabaseOracledbException {
        if (command instanceof DocumentGetCommand || command instanceof QueryCommand) {
            return queryPayloadCodec.encode(command);
        }
        if (command instanceof DocumentSetCommand || command instanceof DocumentUpdateCommand ||
                command instanceof DocumentDeleteCommand) {
            // Delete uses the same unversioned payload shape in both API configurations.
            if (command instanceof DocumentDeleteCommand) {
                return v1DocumentPayloadCodec.encode(command);
            }
            return apiVersion == OracledbApiVersion.V2
                    ? v2DocumentPayloadCodec.encode(command)
                    : v1DocumentPayloadCodec.encode(command);
        }
        throw new IllegalArgumentException("Unsupported operation command for payload encoding");
    }

    @NonNull
    private static RestOperation operationFor(@NonNull OperationCommand<?> command) {
        if (command instanceof DocumentGetCommand || command instanceof QueryCommand) {
            return RestOperation.RUN_QUERY;
        }
        if (command instanceof DocumentSetCommand) {
            return RestOperation.SET_DOCUMENT;
        }
        if (command instanceof DocumentUpdateCommand) {
            return RestOperation.UPDATE_DOCUMENT;
        }
        if (command instanceof DocumentDeleteCommand) {
            return RestOperation.DELETE_DOCUMENT;
        }
        FusabaseLogger.e(TAG, "No database endpoint mapping for operation command type");
        throw new IllegalArgumentException("Unsupported operation command for REST request");
    }

    @NonNull
    private static RestHttpMethod methodFor(@NonNull RestOperation operation) {
        switch (operation) {
            case RUN_QUERY:
            case SET_DOCUMENT:
                return RestHttpMethod.POST;
            case UPDATE_DOCUMENT:
            case DELETE_DOCUMENT:
                return RestHttpMethod.PUT;
            default:
                throw new IllegalArgumentException("Unsupported database REST operation");
        }
    }

    @Nullable
    private static TransactionContext transactionContextFor(@NonNull OperationCommand<?> command) {
        if (command instanceof DocumentGetCommand) {
            return ((DocumentGetCommand) command).getTransactionContext();
        }
        if (command instanceof QueryCommand) {
            return ((QueryCommand) command).getTransactionContext();
        }
        if (command instanceof DocumentSetCommand) {
            return ((DocumentSetCommand) command).getTransactionContext();
        }
        if (command instanceof DocumentUpdateCommand) {
            return ((DocumentUpdateCommand) command).getTransactionContext();
        }
        if (command instanceof DocumentDeleteCommand) {
            return ((DocumentDeleteCommand) command).getTransactionContext();
        }
        return null;
    }

    private static int retryAttemptsFor(@Nullable TransactionContext context) {
        if (context == null || context.getTransactionName().isEmpty()) {
            return 1;
        }
        return context.getMaxAttempts();
    }

    /** Default graph wiring has no remote-read executor until the service migration is active. */
    private static final class UnavailableRemoteReadPort implements RemoteReadPort {
        @NonNull
        @Override
        public jakarta.json.JsonObject read(@NonNull java.util.List<String> path,
                                            @NonNull jakarta.json.JsonObject payload,
                                            @Nullable TransactionContext transactionContext)
                throws FusabaseOracledbException {
            throw new FusabaseOracledbException(
                    "Remote read port is not active for V1 payload preparation",
                    FusabaseOracledbException.Code.INTERNAL);
        }
    }
}
