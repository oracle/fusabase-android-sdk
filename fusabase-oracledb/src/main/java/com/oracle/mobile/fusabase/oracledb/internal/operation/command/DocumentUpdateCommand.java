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
import com.oracle.mobile.fusabase.oracledb.internal.validation.DocumentInputNormalizer;
import com.oracle.mobile.fusabase.oracledb.internal.validation.DocumentInputValidator;

import java.util.List;
import java.util.Map;

import jakarta.json.JsonObject;

/** Immutable command for a document patch/update operation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentUpdateCommand implements OperationCommand<JsonObject> {
    private final List<String> path;
    private final Map<String, Object> data;
    private final TransactionContext transactionContext;
    private final DocumentTarget target;
    private final boolean wildcardPath;
    private final JsonObject params;

    public DocumentUpdateCommand(@NonNull List<String> path,
                                 @NonNull Map<String, Object> data)
            throws FusabaseOracledbException {
        this(path, data, null);
    }

    public DocumentUpdateCommand(@NonNull List<String> path,
                                 @NonNull Map<String, Object> data,
                                 @Nullable TransactionContext transactionContext)
            throws FusabaseOracledbException {
        this(path, data, transactionContext, DocumentTarget.DOCUMENT, false, null);
    }

    /** Creates an update command with explicit payload-shaping context. */
    public DocumentUpdateCommand(@NonNull List<String> path,
                                 @NonNull Map<String, Object> data,
                                 @Nullable TransactionContext transactionContext,
                                 @NonNull DocumentTarget target,
                                 boolean wildcardPath,
                                 @Nullable JsonObject params)
            throws FusabaseOracledbException {
        DocumentInputValidator.validatePath(path, "document-update");
        DocumentInputValidator.validateUpdateData(data, "document-update");
        this.path = CommandValues.copyPath(path);
        this.data = DocumentInputNormalizer.toImmutableMap(data);
        this.transactionContext = transactionContext;
        this.target = java.util.Objects.requireNonNull(target, "Document target cannot be null");
        this.wildcardPath = wildcardPath;
        this.params = params;
    }

    @NonNull
    public List<String> getPath() {
        return path;
    }

    @NonNull
    public Map<String, Object> getData() {
        return DocumentInputNormalizer.toImmutableMap(data);
    }

    @Nullable
    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    @NonNull
    public DocumentTarget getTarget() {
        return target;
    }

    public boolean usesWildcardPath() {
        return wildcardPath;
    }

    @Nullable
    public JsonObject getParams() {
        return params;
    }
}
