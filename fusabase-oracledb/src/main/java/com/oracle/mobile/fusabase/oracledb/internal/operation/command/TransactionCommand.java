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
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.internal.operation.OperationCommand;

import java.util.List;
import java.util.Objects;

import jakarta.json.JsonObject;

/** Immutable ordered transaction command with a snapshotted transaction context. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class TransactionCommand implements OperationCommand<JsonObject> {
    private final List<OperationCommand<?>> operations;
    private final TransactionContext context;

    public TransactionCommand(@NonNull List<? extends OperationCommand<?>> operations,
                              @NonNull TransactionContext context) {
        this.operations = CommandValues.copyCommands(operations);
        this.context = Objects.requireNonNull(context, "Transaction context cannot be null");
    }

    @NonNull
    public List<OperationCommand<?>> getOperations() {
        return operations;
    }

    @NonNull
    public TransactionContext getContext() {
        return context;
    }
}
