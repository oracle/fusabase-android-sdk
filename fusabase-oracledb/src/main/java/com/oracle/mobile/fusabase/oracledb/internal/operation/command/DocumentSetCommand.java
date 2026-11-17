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
import com.oracle.mobile.fusabase.oracledb.SetOptions;
import com.oracle.mobile.fusabase.oracledb.internal.operation.OperationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.validation.DocumentInputNormalizer;
import com.oracle.mobile.fusabase.oracledb.internal.validation.DocumentInputValidator;

import java.util.List;
import java.util.Map;

import jakarta.json.JsonObject;

/** Immutable command for a document set or merge operation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentSetCommand implements OperationCommand<JsonObject> {
    private final List<String> path;
    private final Map<String, Object> data;
    private final boolean merge;
    private final List<String> mergeFields;
    private final TransactionContext transactionContext;
    private final DocumentTarget target;
    private final boolean oidNotGenerated;
    private final boolean addOperation;

    public DocumentSetCommand(@NonNull List<String> path,
                              @NonNull Object data,
                              @NonNull SetOptions options)
            throws FusabaseOracledbException {
        this(path, data, options, null);
    }

    public DocumentSetCommand(@NonNull List<String> path,
                              @NonNull Object data,
                              @NonNull SetOptions options,
                              @Nullable TransactionContext transactionContext)
            throws FusabaseOracledbException {
        this(path, data, options, transactionContext, DocumentTarget.DOCUMENT, false);
    }

    /** Creates a set command with explicit target and generated-OID state. */
    public DocumentSetCommand(@NonNull List<String> path,
                              @NonNull Object data,
                              @Nullable SetOptions options,
                              @Nullable TransactionContext transactionContext,
                              @NonNull DocumentTarget target,
                              boolean oidNotGenerated)
            throws FusabaseOracledbException {
        this(path, data, options, transactionContext, target, oidNotGenerated,
                options == null);
    }

    /**
     * Creates a set command with an explicit add/set distinction.
     *
     * <p>The distinction is normally derived from whether public callers supplied set options.
     * Replay creates an overwrite command from persisted data, where no public SetOptions instance
     * is available. This restricted constructor keeps that detail inside the internal command
     * model without changing the public SetOptions contract.</p>
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public DocumentSetCommand(@NonNull List<String> path,
                              @NonNull Object data,
                              @Nullable SetOptions options,
                              @Nullable TransactionContext transactionContext,
                              @NonNull DocumentTarget target,
                              boolean oidNotGenerated,
                              boolean addOperation)
            throws FusabaseOracledbException {
        DocumentInputValidator.validatePath(path, "document-set");
        DocumentInputValidator.validateDocumentData(data, "document-set");
        this.path = CommandValues.copyPath(path);
        this.data = DocumentInputNormalizer.toImmutableMap(data);
        this.merge = options != null && options.isMerge();
        this.mergeFields = DocumentInputValidator.copyMergeFields(options, "document-set");
        this.transactionContext = transactionContext;
        this.target = java.util.Objects.requireNonNull(target, "Document target cannot be null");
        this.oidNotGenerated = oidNotGenerated;
        this.addOperation = addOperation;
    }

    @NonNull
    public List<String> getPath() {
        return path;
    }

    @NonNull
    public Map<String, Object> getData() {
        return DocumentInputNormalizer.toImmutableMap(data);
    }

    public boolean isMerge() {
        return merge;
    }

    /** Returns whether this command represents add/create data without set options. */
    public boolean isAdd() {
        return addOperation;
    }

    /** Returns whether set options were supplied, including overwrite options. */
    public boolean hasSetOptions() {
        return !addOperation;
    }

    /** Returns an immutable empty list when all fields are merged. */
    @NonNull
    public List<String> getMergeFields() {
        return mergeFields;
    }

    @Nullable
    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    @NonNull
    public DocumentTarget getTarget() {
        return target;
    }

    public boolean isOidNotGenerated() {
        return oidNotGenerated;
    }
}
