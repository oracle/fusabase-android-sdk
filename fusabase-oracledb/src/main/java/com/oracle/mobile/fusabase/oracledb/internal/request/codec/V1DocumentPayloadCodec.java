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


package com.oracle.mobile.fusabase.oracledb.internal.request.codec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.operation.OperationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentDeleteCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentSetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentTarget;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentUpdateCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/** Encodes the current API v1 document request payloads. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class V1DocumentPayloadCodec implements PayloadCodec<OperationCommand<?>> {
    private final DocumentDataEncoder dataEncoder;
    private final RemoteReadPort remoteReadPort;

    public V1DocumentPayloadCodec(@NonNull RemoteReadPort remoteReadPort) {
        this(remoteReadPort, new DocumentDataEncoder());
    }

    public V1DocumentPayloadCodec(@NonNull RemoteReadPort remoteReadPort,
                                  @NonNull DocumentDataEncoder dataEncoder) {
        this.remoteReadPort = java.util.Objects.requireNonNull(remoteReadPort,
                "Remote read port cannot be null");
        this.dataEncoder = java.util.Objects.requireNonNull(dataEncoder,
                "Document data encoder cannot be null");
    }

    @NonNull
    @Override
    public JsonObject encode(@NonNull OperationCommand<?> command)
            throws FusabaseOracledbException {
        if (command instanceof DocumentSetCommand) {
            return encode((DocumentSetCommand) command);
        }
        if (command instanceof DocumentUpdateCommand) {
            return encode((DocumentUpdateCommand) command);
        }
        if (command instanceof DocumentDeleteCommand) {
            return encode((DocumentDeleteCommand) command);
        }
        throw new IllegalArgumentException("Unsupported V1 document command");
    }

    @NonNull
    public JsonObject encode(@NonNull DocumentSetCommand command)
            throws FusabaseOracledbException {
        List<String> serverTimestampFields = new ArrayList<>();
        Map<String, Object> document = prepareSetDocument(command, serverTimestampFields);
        if (document.isEmpty()) {
            throw new FusabaseOracledbException("Invalid Data provided to set()",
                    FusabaseOracledbException.Code.INVALID_ARGUMENT);
        }

        JsonObjectBuilder payload = Json.createObjectBuilder();
        DocumentPayloadSupport.addTargetPath(payload, command.getPath(), command.getTarget(),
                false, command.isOidNotGenerated());
        if (command.hasSetOptions()) {
            payload.add("options", Json.createObjectBuilder()
                    .add("merge", command.isMerge())
                    .add("mergeFields", dataEncoder.encodeValue(command.getMergeFields()))
                    .build());
        } else if (command.getTarget() == DocumentTarget.DOCUMENT &&
                !serverTimestampFields.isEmpty()) {
            payload.add("servertimestamp", dataEncoder.encodeValue(serverTimestampFields));
        }
        payload.add("data", dataEncoder.encodeMap(document));
        DocumentPayloadSupport.addVersionIfApplicable(payload, command.getTransactionContext());
        return payload.build();
    }

    @NonNull
    public JsonObject encode(@NonNull DocumentUpdateCommand command)
            throws FusabaseOracledbException {
        List<String> serverTimestampFields = new ArrayList<>();
        Map<String, Object> document = prepareUpdateDocument(command, serverTimestampFields);
        JsonObjectBuilder payload = Json.createObjectBuilder();
        DocumentPayloadSupport.addTargetPath(payload, command.getPath(), command.getTarget(),
                command.usesWildcardPath(), false);
        payload.add("data", dataEncoder.encodeMap(document));
        DocumentPayloadSupport.addVersionIfApplicable(payload, command.getTransactionContext());
        if (DocumentPayloadSupport.hasNonEmptyConditions(command.getParams())) {
            payload.add("conditions", command.getParams().getJsonArray("conditions"));
        }
        return payload.build();
    }

    @NonNull
    public JsonObject encode(@NonNull DocumentDeleteCommand command)
            throws FusabaseOracledbException {
        JsonObjectBuilder payload = Json.createObjectBuilder();
        DocumentPayloadSupport.addTargetPath(payload, command.getPath(), command.getTarget(),
                false, false);
        DocumentPayloadSupport.addVersionIfApplicable(payload, command.getTransactionContext());
        return payload.build();
    }

    @NonNull
    private Map<String, Object> prepareSetDocument(@NonNull DocumentSetCommand command,
                                                   @NonNull List<String> serverTimestampFields)
            throws FusabaseOracledbException {
        Map<String, Object> document = new LinkedHashMap<>();
        if (requiresSetRead(command)) {
            document.putAll(readExistingDocument(command.getPath(), command.getTarget(),
                    command.getTransactionContext(), command.isAdd()));
        }
        dataEncoder.applyV1Updates(document, command.getData(), serverTimestampFields);
        return document;
    }

    @NonNull
    private Map<String, Object> prepareUpdateDocument(@NonNull DocumentUpdateCommand command,
                                                      @NonNull List<String> serverTimestampFields)
            throws FusabaseOracledbException {
        Map<String, Object> document = new LinkedHashMap<>();
        if (command.getTarget() != DocumentTarget.DUALITY_VIEW &&
                !DocumentPayloadSupport.isTemporaryPath(command.getPath())) {
            document.putAll(readExistingDocument(command.getPath(), command.getTarget(),
                    command.getTransactionContext(), true));
        }
        dataEncoder.applyV1Updates(document, command.getData(), serverTimestampFields);
        return document;
    }

    private boolean requiresSetRead(@NonNull DocumentSetCommand command) {
        return !DocumentPayloadSupport.isTemporaryPath(command.getPath()) &&
                (command.isAdd() || command.isMerge()) &&
                command.getTarget() != DocumentTarget.DUALITY_VIEW;
    }

    @NonNull
    private Map<String, Object> readExistingDocument(@NonNull List<String> path,
                                                     @NonNull DocumentTarget target,
                                                     @Nullable TransactionContext context,
                                                     boolean requireDocument)
            throws FusabaseOracledbException {
        JsonObject readResult = remoteReadPort.read(path,
                pathPayload(path, target), DocumentPayloadSupport.readContext(context));
        JsonArray records = readResult.getJsonArray("ret");
        if (records == null || records.isEmpty()) {
            if (requireDocument) {
                throw new FusabaseOracledbException("Document does not exist",
                        FusabaseOracledbException.Code.NOT_FOUND);
            }
            return new LinkedHashMap<>();
        }
        JsonObject record = records.getJsonObject(0);
        if (!record.containsKey("osons") || record.get("osons").getValueType() !=
                jakarta.json.JsonValue.ValueType.OBJECT) {
            return new LinkedHashMap<>();
        }
        JsonObject osons = record.getJsonObject("osons");
        JsonObject document;
        if (target == DocumentTarget.DUALITY_VIEW) {
            document = osons.containsKey("DATA") ? osons.getJsonObject("DATA") : osons;
        } else if (osons.containsKey("DOCUMENT")) {
            document = osons.getJsonObject("DOCUMENT");
        } else {
            document = osons;
        }
        return dataEncoder.decodeObject(document);
    }

    @NonNull
    private static JsonObject pathPayload(@NonNull List<String> path,
                                          @NonNull DocumentTarget target) {
        JsonObjectBuilder payload = Json.createObjectBuilder();
        DocumentPayloadSupport.addTargetPath(payload, path, target, false, false);
        return payload.build();
    }
}
