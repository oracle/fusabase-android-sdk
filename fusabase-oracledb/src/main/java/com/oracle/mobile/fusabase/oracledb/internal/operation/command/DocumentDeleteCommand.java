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

/** Immutable command for a document delete operation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentDeleteCommand implements OperationCommand<JsonObject> {
    private final List<String> path;
    private final TransactionContext transactionContext;
    private final DocumentTarget target;

    public DocumentDeleteCommand(@NonNull List<String> path)
            throws FusabaseOracledbException {
        this(path, null);
    }

    public DocumentDeleteCommand(@NonNull List<String> path,
                                 @Nullable TransactionContext transactionContext)
            throws FusabaseOracledbException {
        this(path, transactionContext, DocumentTarget.DOCUMENT);
    }

    /** Creates a delete command for an explicit document target. */
    public DocumentDeleteCommand(@NonNull List<String> path,
                                 @Nullable TransactionContext transactionContext,
                                 @NonNull DocumentTarget target)
            throws FusabaseOracledbException {
        DocumentInputValidator.validatePath(path, "document-delete");
        this.path = CommandValues.copyPath(path);
        this.transactionContext = transactionContext;
        this.target = java.util.Objects.requireNonNull(target, "Document target cannot be null");
    }

    @NonNull
    public List<String> getPath() {
        return path;
    }

    @Nullable
    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    @NonNull
    public DocumentTarget getTarget() {
        return target;
    }
}
