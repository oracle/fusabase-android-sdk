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

package com.oracle.mobile.fusabase.oracledb.offline;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Ordered group of local mutations awaiting backend acknowledgement. */
final class OfflineMutationBatch {
    private final String userId;
    private final long batchId;
    private final long localWriteTimeMillis;
    private final String source;
    private final List<OfflineMutation> mutations;

    OfflineMutationBatch(@NonNull String userId, long batchId, long localWriteTimeMillis,
                         @NonNull String source, @NonNull List<OfflineMutation> mutations) {
        if (batchId <= 0 || mutations.isEmpty()) throw new IllegalArgumentException("Invalid mutation batch");
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.batchId = batchId;
        this.localWriteTimeMillis = localWriteTimeMillis;
        this.source = Objects.requireNonNull(source, "Mutation source cannot be null");
        this.mutations = Collections.unmodifiableList(new ArrayList<>(mutations));
    }

    @NonNull String getUserId() { return userId; }
    long getBatchId() { return batchId; }
    long getLocalWriteTimeMillis() { return localWriteTimeMillis; }
    @NonNull String getSource() { return source; }
    @NonNull List<OfflineMutation> getMutations() { return mutations; }

    @NonNull List<String> getAffectedPaths() {
        List<String> paths = new ArrayList<>();
        for (OfflineMutation mutation : mutations) if (!paths.contains(mutation.getPath())) paths.add(mutation.getPath());
        return paths;
    }

    @NonNull List<Map<String, Object>> mutationsForStorage() {
        List<Map<String, Object>> records = new ArrayList<>();
        for (OfflineMutation mutation : mutations) records.add(mutation.toStorage());
        return records;
    }

    @NonNull @SuppressWarnings("unchecked")
    static OfflineMutationBatch fromStorage(@NonNull String userId, long batchId, long writeTime,
                                            @NonNull String source, @NonNull Object stored) {
        List<OfflineMutation> mutations = new ArrayList<>();
        for (Object entry : (List<Object>) stored) mutations.add(OfflineMutation.fromStorage((Map<String, Object>) entry));
        return new OfflineMutationBatch(userId, batchId, writeTime, source, mutations);
    }
}
