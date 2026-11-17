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

package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.oracledb.internal.component.OracledbComponentGraph;
import com.oracle.mobile.fusabase.oracledb.internal.operation.DocumentOperationCoordinator;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentTarget;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentDeleteResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentReadResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.QueryResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentWriteResponse;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Compatibility façade for the existing OracleDB operation callers.
 *
 * <p>REST request creation, HTTP transport, response mapping, and error conversion are delegated
 * to the internal per-client operation graph. The method signatures remain package/protected
 * compatible while callers are migrated incrementally.</p>
 *
 * <p>
 * The class provides methods for:
 * </p>
 * <ul>
 * <li>Forwarding document CRUD operations to the coordinator</li>
 * <li>Preserving transaction metadata and generated document identifiers</li>
 * <li>Retaining the existing protected operation boundary for source compatibility</li>
 * </ul>
 */
class QueryHelper {

    /** Centralized operation boundary used by the compatibility façade methods below. */
    private final DocumentOperationCoordinator coordinator;

    /**
     * Constructs a QueryHelper instance.
     *
     * @param settings the Oracledb settings
     * @param options  the Oracledb options
     * @param oradb    the Oracledb instance
     */
    QueryHelper(@NonNull FusabaseOracledbSettings settings,
            @NonNull FusabaseOptions options,
            @NonNull FusabaseOracledb oradb) {
        Objects.requireNonNull(settings, "OracleDB settings cannot be null");
        Objects.requireNonNull(options, "OracleDB options cannot be null");
        Objects.requireNonNull(oradb, "OracleDB client cannot be null");
        // Normal SDK references share the per-client graph. The explicit-options branch preserves
        // this package-private helper's long-standing ability to characterize an alternate test
        // configuration without changing the public client API.
        this.coordinator = options == oradb.getApp().getOptions()
                ? oradb.componentGraph().getCoordinator()
                : OracledbComponentGraph.create(oradb, options).getCoordinator();
    }

    /**
     * Creates a path JSON object for standard paths.
     *
     * @param path the path segments
     * @return the JSON object with path
     */
    @NonNull
    private JsonObject getPathObject(@NonNull List<String> path) {
        return Json.createObjectBuilder()
                .add("path", Json.createArrayBuilder(path))
                .build();
    }

    /**
     * Creates a path JSON object for duality views.
     *
     * @param path the path segments
     * @return the JSON object with duality view path
     */
    @NonNull
    JsonObject getDualityViewPathObject(@NonNull List<String> path) {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        payloadBuilder.add("dv_name", path.get(0));
        if (path.size() > 1) {
            payloadBuilder.add("oid", path.get(1));
        }
        return payloadBuilder.build();
    }

    /**
     * Creates a transaction header for the compatibility boundary. The actual header value is
     * produced by the request provider from normalized transaction metadata.
     */
    @NonNull
    protected JsonObject createTransactionHeader(int beginTrans,
            int endTrans,
            @NonNull String transactionName) {
        return Json.createObjectBuilder()
                .add("begin_trans", beginTrans)
                .add("end_trans", endTrans)
                .add("trans_name", transactionName)
                .build();
    }

    /**
     * Compatibility façade for existing document-read callers. Request creation, transport,
     * response reading, mapping, and error conversion are owned by the internal operation graph.
     */
    @NonNull
    protected JsonObject fetchDocs(@NonNull List<String> path,
            @NonNull JsonObject payload,
            @NonNull TransactionOperation operation)
            throws FusabaseOracledbException {
        DocumentResponse response = isQueryPayload(payload)
                ? coordinator.query(path, payload, toTransactionContext(operation))
                : coordinator.read(path, payload, toTransactionContext(operation));
        // A masked 403 is exposed as an empty read result, but the server rejected the
        // transaction-opening request. Keep begin_trans set so the first write starts a new
        // transaction rather than continuing the rejected one.
        if (!keepsTransactionStart(response)) {
            clearBeginTransaction(operation);
        }
        return response.getPayload();
    }

    static boolean keepsTransactionStart(@NonNull DocumentResponse response) {
        return (response instanceof DocumentReadResponse &&
                ((DocumentReadResponse) response).isAuthorizationMasked()) ||
                (response instanceof QueryResponse &&
                ((QueryResponse) response).isAuthorizationMasked());
    }

    static boolean isQueryPayload(@NonNull JsonObject payload) {
        // Query requests always carry their condition envelope, including an empty conditions
        // array. Document reads carry only document-read fields such as path and snapshot.
        // Keep every query on QueryCommand so its response/error handling cannot be confused
        // with the compatibility behavior for a single document read.
        return payload.containsKey("conditions");
    }

    /** Compatibility façade for document deletion. */
    @NonNull
    protected JsonObject removeDocs(@NonNull List<String> path,
            @NonNull EntityReference entityReference,
            @NonNull TransactionOperation operation) throws FusabaseException {
        DocumentDeleteResponse response = coordinator.delete(path,
                toTransactionContext(operation), targetFor(entityReference));
        clearBeginTransaction(operation);
        return response.getPayload();
    }

    /** Compatibility façade for document creation and set operations. */
    @NonNull
    protected JsonObject setDocs(@NonNull List<String> path,
            @NonNull Object data,
            @Nullable SetOptions setOptions,
            @NonNull EntityReference entityReference,
            @NonNull TransactionOperation operation) throws FusabaseException {
        entityReference.updatePathIfNecessary();
        boolean oidNotGenerated = entityReference.isOidNotGenerated();
        DocumentWriteResponse response = coordinator.set(path, data, setOptions,
                toTransactionContext(operation), targetFor(entityReference), oidNotGenerated);
        clearBeginTransaction(operation);
        if (oidNotGenerated) {
            removeTemporaryPathSegment(path);
            if (response.getOid() != null) {
                entityReference.updateId(response.getOid());
            }
        }
        return response.getPayload();
    }

    /** Compatibility façade for document update and conditional bulk-update operations. */
    @NonNull
    protected JsonObject updateDocs(@NonNull List<String> path,
            @NonNull Map<String, Object> data,
            @Nullable EntityReference entityReference,
            @NonNull TransactionOperation operation,
            @Nullable JsonObject params) throws FusabaseException {
        DocumentTarget target = entityReference == null ? DocumentTarget.DOCUMENT :
                targetFor(entityReference);
        DocumentWriteResponse response = coordinator.update(path, data,
                toTransactionContext(operation), target, params != null, params);
        clearBeginTransaction(operation);
        return response.getPayload();
    }

    @NonNull
    private TransactionContext toTransactionContext(@NonNull TransactionOperation operation) {
        return new TransactionContext(operation.getBeginTrans(), operation.getEndTrans(),
                operation.getTransactionName(), operation.getOptions().maxAttempts,
                operation.getVersion());
    }

    @NonNull
    private static DocumentTarget targetFor(@NonNull EntityReference entityReference) {
        return entityReference instanceof DualityViewDocReference
                ? DocumentTarget.DUALITY_VIEW : DocumentTarget.DOCUMENT;
    }

    private static void clearBeginTransaction(@NonNull TransactionOperation operation) {
        if (operation.getBeginTrans() == 1) {
            operation.unsetBeginTrans();
        }
    }

    private static void removeTemporaryPathSegment(@NonNull List<String> path) {
        if (!path.isEmpty() && path.get(path.size() - 1).contains("BAAS_DOC_TEMP_ID=")) {
            path.remove(path.size() - 1);
        }
    }
}
