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


package com.oracle.mobile.fusabase.oracledb.internal.offline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.internal.operation.OperationCommand;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentTarget;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;
import com.oracle.mobile.fusabase.oracledb.internal.validation.OperationCommandFactory;
import com.oracle.mobile.fusabase.oracledb.offline.OfflineMutation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Converts durable domain mutations into validated internal operation commands. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class PersistedMutationCommandConverter {
    private final OperationCommandFactory commandFactory;

    public PersistedMutationCommandConverter(@NonNull OperationCommandFactory commandFactory) {
        this.commandFactory = Objects.requireNonNull(commandFactory,
                "Operation command factory cannot be null");
    }

    @NonNull
    public OperationCommand<?> convert(@NonNull OfflineMutation mutation)
            throws FusabaseOracledbException {
        return convert(mutation, null);
    }

    @NonNull
    public OperationCommand<?> convert(@NonNull OfflineMutation mutation,
                                       @Nullable TransactionContext transactionContext)
            throws FusabaseOracledbException {
        Objects.requireNonNull(mutation, "Offline mutation cannot be null");
        List<String> path = pathSegments(mutation.getPath());
        switch (mutation.getType()) {
            case SET:
                // Persisted SET mutations represent the public set(data) overwrite operation.
                return commandFactory.createOverwrite(path, mutation.getData(), transactionContext);
            case PATCH:
                return commandFactory.createUpdate(path, mutation.getData(), transactionContext,
                        DocumentTarget.DOCUMENT, false, null);
            case DELETE:
                return commandFactory.createDelete(path, transactionContext, DocumentTarget.DOCUMENT);
            default:
                throw new FusabaseOracledbException(
                        "Unsupported offline mutation type",
                        FusabaseOracledbException.Code.INVALID_ARGUMENT);
        }
    }

    @NonNull
    private static List<String> pathSegments(@NonNull String path)
            throws FusabaseOracledbException {
        if (path.trim().isEmpty()) {
            throw new FusabaseOracledbException(
                    "Offline mutation path cannot be empty",
                    FusabaseOracledbException.Code.INVALID_ARGUMENT);
        }
        String[] segments = path.split("/", -1);
        List<String> result = new ArrayList<>(Arrays.asList(segments));
        for (String segment : result) {
            if (segment == null || segment.isEmpty()) {
                throw new FusabaseOracledbException(
                        "Offline mutation path is invalid",
                        FusabaseOracledbException.Code.INVALID_ARGUMENT);
            }
        }
        return result;
    }
}
