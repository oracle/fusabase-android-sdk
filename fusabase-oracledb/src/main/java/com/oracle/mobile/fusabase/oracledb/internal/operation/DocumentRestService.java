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

import com.oracle.mobile.fusabase.oracledb.internal.error.DocumentErrorMapper;
import com.oracle.mobile.fusabase.oracledb.internal.error.OperationErrorMapper;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.BatchWriteCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentDeleteCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentGetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentSetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentUpdateCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.QueryCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionCommand;
import com.oracle.mobile.fusabase.oracledb.internal.request.DocumentRequestProvider;
import com.oracle.mobile.fusabase.oracledb.internal.response.mapper.DocumentResponseMapper;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.QueryResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentReadResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.processor.DocumentResponseProcessor;
import com.oracle.mobile.fusabase.oracledb.internal.response.reader.JsonRawResponseReader;
import com.oracle.mobile.fusabase.oracledb.internal.response.reader.ResponseReader;
import com.oracle.mobile.fusabase.oracledb.internal.transport.RestResponse;
import com.oracle.mobile.fusabase.oracledb.internal.transport.RestTransport;
import com.oracle.mobile.fusabase.oracledb.internal.error.InternalOperationException;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Objects;

/** Orchestrates request creation, transport execution, response reading, mapping, and processing. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentRestService {
    private static final String TAG = "OracledbRestService";

    private final DocumentRequestProvider requestProvider;
    private final RestTransport transport;
    private final ResponseReader<JsonObject> responseReader;
    private final DocumentResponseMapper responseMapper;
    private final DocumentResponseProcessor responseProcessor;
    private final OperationErrorMapper errorMapper;

    public DocumentRestService(@NonNull DocumentRequestProvider requestProvider,
                               @NonNull RestTransport transport) {
        this(requestProvider, transport, new JsonRawResponseReader(),
                new DocumentResponseMapper(), new DocumentResponseProcessor(),
                new DocumentErrorMapper());
    }

    public DocumentRestService(@NonNull DocumentRequestProvider requestProvider,
                               @NonNull RestTransport transport,
                               @NonNull ResponseReader<JsonObject> responseReader,
                               @NonNull DocumentResponseMapper responseMapper,
                               @NonNull DocumentResponseProcessor responseProcessor,
                               @NonNull OperationErrorMapper errorMapper) {
        this.requestProvider = Objects.requireNonNull(requestProvider,
                "Document request provider cannot be null");
        this.transport = Objects.requireNonNull(transport, "REST transport cannot be null");
        this.responseReader = Objects.requireNonNull(responseReader,
                "Response reader cannot be null");
        this.responseMapper = Objects.requireNonNull(responseMapper,
                "Response mapper cannot be null");
        this.responseProcessor = Objects.requireNonNull(responseProcessor,
                "Response processor cannot be null");
        this.errorMapper = Objects.requireNonNull(errorMapper, "Error mapper cannot be null");
    }

    /** Executes one normalized command and returns its processed response model. */
    @NonNull
    public DocumentResponse execute(@NonNull OperationCommand<?> command)
            throws FusabaseOracledbException {
        Objects.requireNonNull(command, "Operation command cannot be null");
        try {
            FusabaseLogger.v(TAG, "Creating REST request for normalized operation");
            RestResponse response = transport.execute(requestProvider.create(command));
            boolean authorizationMasked = isMaskedAuthorizationRead(response, command);
            JsonObject payload = read(response, command);
            DocumentResponse mapped = map(command, payload);
            if (authorizationMasked && mapped instanceof DocumentReadResponse) {
                DocumentReadResponse read = (DocumentReadResponse) mapped;
                mapped = new DocumentReadResponse(read.getPayload(), read.getRecords(),
                        read.getDocument(), read.getOid(), read.getVersion(), read.exists(), true);
            }
            if (authorizationMasked && mapped instanceof QueryResponse) {
                QueryResponse query = (QueryResponse) mapped;
                mapped = new QueryResponse(query.getPayload(), query.getRecords(), true);
            }
            DocumentResponse processed = responseProcessor.process(mapped);
            FusabaseLogger.i(TAG, "Completed normalized REST operation");
            return processed;
        } catch (FusabaseOracledbException failure) {
            throw failure;
        } catch (Exception failure) {
            FusabaseLogger.e(TAG, "REST operation failed after request preparation");
            throw errorMapper.map(failure);
        }
    }

    @NonNull
    private JsonObject read(@NonNull RestResponse response,
                            @NonNull OperationCommand<?> command)
            throws InternalOperationException, FusabaseOracledbException {
        // Document reads intentionally conceal both absent and forbidden documents as empty.
        // Collection queries also conceal forbidden results.
        // ORDS delete compatibility: a 403 is treated as an already-absent document.
        // Apply this at the shared REST boundary so direct deletes and offline replay agree.
        if (!response.getStatus() && response.getStatusCode() == 403
                && command instanceof DocumentDeleteCommand) {
            DocumentDeleteCommand delete = (DocumentDeleteCommand) command;
            FusabaseLogger.w(TAG, "Delete returned 403; treating document as already removed");
            return Json.createObjectBuilder()
                    .add("OID", delete.getPath().get(delete.getPath().size() - 1))
                    .build();
        }
        if (!response.getStatus() &&
                ((command instanceof DocumentGetCommand &&
                        (response.getStatusCode() == 403 || response.getStatusCode() == 404)) ||
                 (command instanceof QueryCommand && response.getStatusCode() == 403))) {
            FusabaseLogger.w(TAG, "Read returned an empty result");
            return Json.createObjectBuilder()
                    .add("ret", Json.createArrayBuilder().build())
                    .build();
        }
        return responseReader.read(response);
    }

    private static boolean isMaskedAuthorizationRead(@NonNull RestResponse response,
                                                     @NonNull OperationCommand<?> command) {
        return !response.getStatus() &&
                (command instanceof DocumentGetCommand || command instanceof QueryCommand) &&
                response.getStatusCode() == 403;
    }

    @NonNull
    private DocumentResponse map(@NonNull OperationCommand<?> command,
                                 @NonNull JsonObject payload)
            throws InternalOperationException {
        if (command instanceof DocumentGetCommand) {
            return responseMapper.mapRead(payload);
        }
        if (command instanceof QueryCommand) {
            return responseMapper.mapQuery(payload);
        }
        if (command instanceof DocumentSetCommand || command instanceof DocumentUpdateCommand) {
            return responseMapper.mapWrite(payload);
        }
        if (command instanceof DocumentDeleteCommand) {
            return responseMapper.mapDelete(payload);
        }
        if (command instanceof BatchWriteCommand) {
            return responseMapper.mapBatchWrite(payload);
        }
        if (command instanceof TransactionCommand) {
            return responseMapper.mapTransaction(payload);
        }
        throw new IllegalArgumentException("Unsupported operation command response type");
    }

}
