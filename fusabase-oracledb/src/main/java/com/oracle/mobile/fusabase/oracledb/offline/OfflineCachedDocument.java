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
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable remote document record stored by the offline persistence layer. */
public final class OfflineCachedDocument {

    public enum State {
        FOUND(0),
        NO_DOCUMENT(1);

        private final int storageValue;

        State(int storageValue) {
            this.storageValue = storageValue;
        }

        int getStorageValue() {
            return storageValue;
        }

        @NonNull
        static State fromStorageValue(int value) {
            if (value == FOUND.storageValue) {
                return FOUND;
            }
            if (value == NO_DOCUMENT.storageValue) {
                return NO_DOCUMENT;
            }
            throw new IllegalArgumentException("Unknown offline document state: " + value);
        }
    }

    private final String path;
    private final Map<String, Object> data;
    private final String version;
    private final Long updateTimeMillis;
    private final Long commitTimeMillis;
    private final Long readTimeMillis;
    private final State state;
    private final boolean hasLocalMutations;
    private final boolean hasCommittedMutations;
    private final long sequenceNumber;

    OfflineCachedDocument(@NonNull String path, @Nullable Map<String, Object> data,
                          @NonNull String version, @Nullable Long updateTimeMillis,
                          @Nullable Long commitTimeMillis, @Nullable Long readTimeMillis,
                          @NonNull State state, boolean hasLocalMutations,
                          boolean hasCommittedMutations, long sequenceNumber) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Offline document path cannot be empty");
        }
        if (state == State.FOUND && data == null) {
            throw new IllegalArgumentException("Found offline documents require data");
        }
        this.path = path;
        this.data = data == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(data));
        this.version = Objects.requireNonNull(version, "Offline document version cannot be null");
        this.updateTimeMillis = updateTimeMillis;
        this.commitTimeMillis = commitTimeMillis;
        this.readTimeMillis = readTimeMillis;
        this.state = Objects.requireNonNull(state, "Offline document state cannot be null");
        this.hasLocalMutations = hasLocalMutations;
        this.hasCommittedMutations = hasCommittedMutations;
        this.sequenceNumber = sequenceNumber;
    }

    @NonNull
    public String getPath() {
        return path;
    }

    @Nullable
    public Map<String, Object> getData() {
        return data;
    }

    @NonNull
    public String getVersion() {
        return version;
    }

    @Nullable
    public Long getUpdateTimeMillis() {
        return updateTimeMillis;
    }

    @Nullable
    public Long getCommitTimeMillis() {
        return commitTimeMillis;
    }

    @Nullable
    public Long getReadTimeMillis() {
        return readTimeMillis;
    }

    @NonNull
    public State getState() {
        return state;
    }

    public boolean hasLocalMutations() {
        return hasLocalMutations;
    }

    public boolean hasCommittedMutations() {
        return hasCommittedMutations;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}
