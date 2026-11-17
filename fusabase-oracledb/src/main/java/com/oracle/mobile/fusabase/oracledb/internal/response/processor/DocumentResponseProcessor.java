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


package com.oracle.mobile.fusabase.oracledb.internal.response.processor;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.internal.error.InternalOperationException;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.BatchWriteResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentDeleteResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentReadResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.DocumentWriteResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.QueryResponse;
import com.oracle.mobile.fusabase.oracledb.internal.response.model.TransactionResponse;

import java.util.Objects;

/**
 * Response processing seam for domain side effects.
 *
 * <p>The current implementation validates and returns immutable mapped results. Part 8 will
 * inject cache/listener/public-snapshot completion ports here while keeping those side effects
 * out of the reader and mapper.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentResponseProcessor
        implements ResponseProcessor<DocumentResponse, DocumentResponse> {

    @NonNull
    @Override
    public DocumentResponse process(@NonNull DocumentResponse input)
            throws InternalOperationException {
        return Objects.requireNonNull(input, "Mapped response cannot be null");
    }

    @NonNull
    public DocumentReadResponse processRead(@NonNull DocumentReadResponse input)
            throws InternalOperationException {
        return (DocumentReadResponse) process(input);
    }

    @NonNull
    public DocumentWriteResponse processWrite(@NonNull DocumentWriteResponse input)
            throws InternalOperationException {
        return (DocumentWriteResponse) process(input);
    }

    @NonNull
    public DocumentDeleteResponse processDelete(@NonNull DocumentDeleteResponse input)
            throws InternalOperationException {
        return (DocumentDeleteResponse) process(input);
    }

    @NonNull
    public QueryResponse processQuery(@NonNull QueryResponse input)
            throws InternalOperationException {
        return (QueryResponse) process(input);
    }

    @NonNull
    public BatchWriteResponse processBatchWrite(@NonNull BatchWriteResponse input)
            throws InternalOperationException {
        return (BatchWriteResponse) process(input);
    }

    @NonNull
    public TransactionResponse processTransaction(@NonNull TransactionResponse input)
            throws InternalOperationException {
        return (TransactionResponse) process(input);
    }
}
