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
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.operation.OperationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentDeleteCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentSetCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentUpdateCommand;

import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/** Encodes the current API v2 document request payloads. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class V2DocumentPayloadCodec implements PayloadCodec<OperationCommand<?>> {
    private final DocumentDataEncoder dataEncoder;

    public V2DocumentPayloadCodec() {
        this(new DocumentDataEncoder());
    }

    public V2DocumentPayloadCodec(@NonNull DocumentDataEncoder dataEncoder) {
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
        throw new IllegalArgumentException("Unsupported V2 document command");
    }

    @NonNull
    public JsonObject encode(@NonNull DocumentSetCommand command)
            throws FusabaseOracledbException {
        JsonObjectBuilder payload = Json.createObjectBuilder();
        DocumentPayloadSupport.addTargetPath(payload, command.getPath(), command.getTarget(),
                false, command.isOidNotGenerated());
        if (command.isAdd()) {
            java.util.Map<String, Object> document = new java.util.LinkedHashMap<>();
            List<String> serverTimestampFields = new java.util.ArrayList<>();
            dataEncoder.applyV1Updates(document, command.getData(), serverTimestampFields);
            if (document.isEmpty()) {
                throw new FusabaseOracledbException("Invalid Data provided to set()",
                        FusabaseOracledbException.Code.INVALID_ARGUMENT);
            }
            for (Map.Entry<String, Object> entry : command.getData().entrySet()) {
                if (entry.getValue() instanceof com.oracle.mobile.fusabase.oracledb.FieldValue &&
                        "deleteVector".equals(com.oracle.mobile.fusabase.oracledb
                                .FieldValueOperationEncoder.operationName(
                                        (com.oracle.mobile.fusabase.oracledb.FieldValue)
                                                entry.getValue()))) {
                    throw new FusabaseOracledbException(
                            "deleteVector() is not supported in addDoc/set create payload",
                            FusabaseOracledbException.Code.INVALID_ARGUMENT);
                }
            }
            Map<String, Object> embeddings = new java.util.LinkedHashMap<>();
            Map<String, Object> filtered = new java.util.LinkedHashMap<>();
            EmbeddingEncoder embeddingEncoder = new EmbeddingEncoder();
            for (Map.Entry<String, Object> entry : document.entrySet()) {
                if (embeddingEncoder.isEmbeddingInput(entry.getValue())) {
                    embeddings.put(entry.getKey(), embeddingEncoder.encode(entry.getValue()));
                } else {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            if (!embeddings.isEmpty()) {
                filtered.put("$embeddings", embeddings);
            }
            payload.add("data", dataEncoder.encodeMap(filtered));
            if (command.getTarget() == com.oracle.mobile.fusabase.oracledb.internal.operation
                    .command.DocumentTarget.DOCUMENT && !serverTimestampFields.isEmpty()) {
                payload.add("servertimestamp", dataEncoder.encodeValue(serverTimestampFields));
            }
        } else {
            List<String> serverTimestampFields;
            if (command.isMerge()) {
                addUpdateArray(payload, command.getData());
                serverTimestampFields = java.util.Collections.emptyList();
            } else {
                serverTimestampFields = addSetUpdateArray(payload, command.getData());
            }
            payload.add("options", Json.createObjectBuilder()
                    .add("merge", command.isMerge())
                    .add("mergeFields", dataEncoder.encodeValue(command.getMergeFields()))
                    .build());
            if (!serverTimestampFields.isEmpty()) {
                payload.add("servertimestamp", dataEncoder.encodeValue(serverTimestampFields));
            }
            DocumentPayloadSupport.addVersionIfApplicable(payload, command.getTransactionContext());
        }
        payload.add("apiversion", 2);
        return payload.build();
    }

    @NonNull
    public JsonObject encode(@NonNull DocumentUpdateCommand command)
            throws FusabaseOracledbException {
        JsonObjectBuilder payload = Json.createObjectBuilder();
        DocumentPayloadSupport.addTargetPath(payload, command.getPath(), command.getTarget(),
                command.usesWildcardPath(), false);
        addUpdateArray(payload, command.getData());
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

    private void addUpdateArray(@NonNull JsonObjectBuilder payload,
                                @NonNull Map<String, Object> updates) {
        JsonArrayBuilder array = jakarta.json.Json.createArrayBuilder();
        for (Map<String, Object> operation : dataEncoder.encodeV2UpdateOperations(updates)) {
            array.add(dataEncoder.encodeMap(operation));
        }
        payload.add("data", array.build());
    }

    /**
     * Encodes an overwrite set using the API v2 timestamp convention shared with the JavaScript
     * SDK: server-timestamp fields are placed in the top-level {@code servertimestamp} array.
     */
    @NonNull
    private List<String> addSetUpdateArray(@NonNull JsonObjectBuilder payload,
                                            @NonNull Map<String, Object> updates) {
        List<String> serverTimestampFields = new java.util.ArrayList<>();
        JsonArrayBuilder array = jakarta.json.Json.createArrayBuilder();
        for (Map<String, Object> operation : dataEncoder.encodeV2UpdateOperations(updates)) {
            if ("servertimestamp".equals(operation.get("op"))) {
                serverTimestampFields.add((String) operation.get("field"));
            } else {
                array.add(dataEncoder.encodeMap(operation));
            }
        }
        payload.add("data", array.build());
        return serverTimestampFields;
    }
}
