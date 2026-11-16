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

/** A locally queued document mutation. */
public final class OfflineMutation {
    public enum Type { SET, PATCH, DELETE }

    private final Type type;
    private final String path;
    private final Map<String, Object> data;

    public OfflineMutation(@NonNull Type type, @NonNull String path, @Nullable Map<String, Object> data) {
        if (path.isEmpty()) throw new IllegalArgumentException("Mutation path cannot be empty");
        if (type != Type.DELETE && data == null) {
            throw new IllegalArgumentException("Set and patch mutations require data");
        }
        this.type = Objects.requireNonNull(type, "Mutation type cannot be null");
        this.path = path;
        this.data = data == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    @NonNull public Type getType() { return type; }
    @NonNull public String getPath() { return path; }
    @Nullable public Map<String, Object> getData() { return data; }

    @NonNull Map<String, Object> toStorage() {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("type", type.name());
        record.put("path", path);
        record.put("data", data);
        return record;
    }

    @NonNull @SuppressWarnings("unchecked")
    static OfflineMutation fromStorage(@NonNull Map<String, Object> record) {
        Object data = record.get("data");
        return new OfflineMutation(Type.valueOf((String) record.get("type")),
                (String) record.get("path"), data == null ? null : (Map<String, Object>) data);
    }
}
