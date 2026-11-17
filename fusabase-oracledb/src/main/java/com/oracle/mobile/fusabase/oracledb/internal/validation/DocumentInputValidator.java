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


package com.oracle.mobile.fusabase.oracledb.internal.validation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.oracledb.FusabaseOracledbException;
import com.oracle.mobile.fusabase.oracledb.SetOptions;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Validates operation inputs before they can be turned into a request command. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentInputValidator {
    private static final String TAG = "OracledbInput";

    private DocumentInputValidator() { }

    /** Validates a document or collection path without logging its contents. */
    public static void validatePath(@Nullable List<String> path,
                                    @NonNull String operation)
            throws FusabaseOracledbException {
        if (path == null || path.isEmpty()) {
            throw invalidArgument(operation, "A non-empty path is required");
        }
        for (String segment : path) {
            if (segment == null || segment.trim().isEmpty()) {
                throw invalidArgument(operation, "Path segments must not be empty");
            }
        }
    }

    /** Validates a set or update document value before normalization. */
    public static void validateDocumentData(@Nullable Object data,
                                             @NonNull String operation)
            throws FusabaseOracledbException {
        if (data == null) {
            throw invalidArgument(operation, "Document data must not be null");
        }
    }

    /** Validates update field names using the existing map-update contract. */
    public static void validateUpdateData(@Nullable Map<String, Object> data,
                                          @NonNull String operation)
            throws FusabaseOracledbException {
        if (data == null) {
            throw invalidArgument(operation, "Update data must not be null");
        }
        for (String field : data.keySet()) {
            if (!isValidUpdateField(field)) {
                throw invalidArgument(operation, "Invalid update field");
            }
        }
    }

    /** Snapshots set options so later caller mutations cannot affect a command. */
    @NonNull
    public static List<String> copyMergeFields(@Nullable SetOptions options,
                                               @NonNull String operation)
            throws FusabaseOracledbException {
        if (options == null) {
            return Collections.emptyList();
        }
        List<String> mergeFields = options.getMergeFields();
        if (mergeFields == null) {
            return Collections.emptyList();
        }
        List<String> copy = new ArrayList<>(mergeFields.size());
        for (String field : mergeFields) {
            if (!isValidUpdateField(field)) {
                throw invalidArgument(operation, "Invalid merge field");
            }
            copy.add(field);
        }
        return Collections.unmodifiableList(copy);
    }

    private static boolean isValidUpdateField(@Nullable String field) {
        if (field == null || field.trim().isEmpty() || field.startsWith(".") ||
                field.endsWith(".")) {
            return false;
        }
        String[] segments = field.split("\\.", -1);
        for (String segment : segments) {
            if (segment.trim().isEmpty()) {
                return false;
            }
            for (char invalidCharacter : "~*/[]".toCharArray()) {
                if (segment.indexOf(invalidCharacter) >= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @NonNull
    private static FusabaseOracledbException invalidArgument(@NonNull String operation,
                                                              @NonNull String message) {
        FusabaseLogger.w(TAG, "Rejected " + operation + " input: " + message);
        return new FusabaseOracledbException(message,
                FusabaseOracledbException.Code.INVALID_ARGUMENT);
    }
}
