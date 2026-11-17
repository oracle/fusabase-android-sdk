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


package com.oracle.mobile.fusabase.oracledb.internal.request.codec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.oracledb.internal.operation.command.DocumentTarget;
import com.oracle.mobile.fusabase.oracledb.internal.operation.command.TransactionContext;

import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/** Small shared payload-shape helpers; it owns no transport or mutable operation state. */
final class DocumentPayloadSupport {
    private static final String DOES_NOT_EXIST_VERSION = "0";

    private DocumentPayloadSupport() { }

    @NonNull
    static JsonObjectBuilder addTargetPath(@NonNull JsonObjectBuilder builder,
                                           @NonNull List<String> path,
                                           @NonNull DocumentTarget target,
                                           boolean wildcard,
                                           boolean removeLastPathSegment) {
        List<String> effectivePath = new ArrayList<>(path);
        if (removeLastPathSegment && !effectivePath.isEmpty()) {
            effectivePath.remove(effectivePath.size() - 1);
        }
        if (target == DocumentTarget.DUALITY_VIEW) {
            builder.add("dv_name", effectivePath.get(0));
            if (effectivePath.size() > 1) {
                builder.add("oid", effectivePath.get(1));
            }
            return builder;
        }
        JsonArrayBuilder pathBuilder = Json.createArrayBuilder(effectivePath);
        if (wildcard) {
            pathBuilder.add("*");
        }
        builder.add("path", pathBuilder.build());
        return builder;
    }

    static boolean isTemporaryPath(@NonNull List<String> path) {
        return !path.isEmpty() && path.get(path.size() - 1).contains("BAAS_DOC_TEMP_ID=");
    }

    static void addVersionIfApplicable(@NonNull JsonObjectBuilder builder,
                                       @Nullable TransactionContext context) {
        if (context == null || context.getTransactionName().isEmpty() ||
                context.getVersion() == null ||
                DOES_NOT_EXIST_VERSION.equals(context.getVersion())) {
            return;
        }
        builder.add("version", context.getVersion());
    }

    @Nullable
    static TransactionContext readContext(@Nullable TransactionContext context) {
        if (context == null || context.getEndTrans() == 0) {
            return context;
        }
        return new TransactionContext(context.getBeginTrans(), 0,
                context.getTransactionName(), context.getMaxAttempts(), context.getVersion());
    }

    static boolean hasNonEmptyConditions(@Nullable JsonObject params) {
        return params != null && params.containsKey("conditions") &&
                params.getJsonArray("conditions") != null &&
                !params.getJsonArray("conditions").isEmpty();
    }
}
