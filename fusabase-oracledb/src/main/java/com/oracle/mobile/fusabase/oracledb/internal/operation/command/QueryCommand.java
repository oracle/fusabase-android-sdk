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


package com.oracle.mobile.fusabase.oracledb.internal.operation.command;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.operation.OperationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.validation.DocumentInputValidator;

import java.util.List;

import jakarta.json.JsonObject;

/** Immutable command for a query request. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class QueryCommand implements OperationCommand<JsonObject> {
    private final List<String> path;
    private final JsonObject payload;
    private final TransactionContext transactionContext;

    public QueryCommand(@NonNull List<String> path,
                        @NonNull JsonObject payload)
            throws FusabaseOracledbException {
        this(path, payload, null);
    }

    public QueryCommand(@NonNull List<String> path,
                        @NonNull JsonObject payload,
                        @Nullable TransactionContext transactionContext)
            throws FusabaseOracledbException {
        if (payload == null) {
            throw new FusabaseOracledbException("Query payload must not be null",
                    FusabaseOracledbException.Code.INVALID_ARGUMENT);
        }
        if (isRootJoinQuery(path, payload)) {
            // Root join queries deliberately have no document/collection path. The view name
            // scopes the request and is serialized in the joins payload, matching the JS SDK.
        } else {
            DocumentInputValidator.validatePath(path, "query");
        }
        this.path = CommandValues.copyPath(path);
        this.payload = payload;
        this.transactionContext = transactionContext;
    }

    private static boolean isRootJoinQuery(@NonNull List<String> path,
                                           @NonNull JsonObject payload) {
        if (!path.isEmpty() || !payload.containsKey("joins") || payload.isNull("joins")) {
            return false;
        }
        try {
            String viewName = payload.getJsonObject("joins").getString("view_name", "").trim();
            return !viewName.isEmpty();
        } catch (ClassCastException ignored) {
            return false;
        }
    }

    @NonNull
    public List<String> getPath() {
        return path;
    }

    @NonNull
    public JsonObject getPayload() {
        return payload;
    }

    @Nullable
    public TransactionContext getTransactionContext() {
        return transactionContext;
    }
}
