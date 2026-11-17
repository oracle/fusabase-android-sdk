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


package com.oracle.mobile.fusabase.oracledb.internal.response.reader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.oracledb.internal.error.ErrorCategory;
import com.oracle.mobile.fusabase.oracledb.internal.error.InternalOperationException;
import com.oracle.mobile.fusabase.oracledb.internal.logging.OperationLogContext;
import com.oracle.mobile.fusabase.oracledb.internal.transport.RestResponse;

import java.io.StringReader;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/** Converts a successful REST response into a JSON object without exposing response contents. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class JsonRawResponseReader implements ResponseReader<JsonObject> {
    private static final String TAG = "OracledbResponseReader";

    @NonNull
    @Override
    public JsonObject read(@NonNull RestResponse response) throws InternalOperationException {
        Objects.requireNonNull(response, "REST response cannot be null");
        OperationLogContext context = responseContext(response);

        if (!response.getStatus()) {
            // Parse a well-formed ORDS error envelope only to validate the server response shape.
            // Its message and fields are intentionally not copied into an exception or a log.
            boolean hasErrorEnvelope = isOrdErrorEnvelope(response.getBody());
            boolean hasErrorMetadata = response.getError() != null;
            FusabaseLogger.w(TAG, hasErrorEnvelope
                    ? "REST response contained an ORDS error envelope"
                    : hasErrorMetadata ? "REST response returned a non-success status with error metadata"
                    : "REST response returned a non-success status");
            throw new InternalOperationException(
                    hasErrorEnvelope ? "ORDS error response received" :
                            "REST response returned a non-success status",
                    ErrorCategory.HTTP,
                    false,
                    context);
        }

        String body = response.getBody();
        if (body.trim().isEmpty()) {
            FusabaseLogger.e(TAG, "REST response body was empty for a JSON operation");
            throw new InternalOperationException(
                    "REST response body was empty",
                    ErrorCategory.PROTOCOL,
                    false,
                    context);
        }

        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject result = reader.readObject();
            FusabaseLogger.d(TAG, "REST response JSON was read successfully");
            return result;
        } catch (RuntimeException failure) {
            FusabaseLogger.e(TAG, "REST response JSON could not be read");
            throw new InternalOperationException(
                    "REST response JSON could not be read",
                    ErrorCategory.PROTOCOL,
                    false,
                    context,
                    failure);
        }
    }

    private static boolean isOrdErrorEnvelope(@Nullable String body) {
        if (body == null || body.trim().isEmpty()) {
            return false;
        }
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject object = reader.readObject();
            return object.containsKey("error") || object.containsKey("message") ||
                    object.containsKey("errorCode") || object.containsKey("code");
        } catch (RuntimeException ignored) {
            FusabaseLogger.d(TAG, "Non-success REST response had no parseable ORDS envelope");
            return false;
        }
    }

    @Nullable
    private static OperationLogContext responseContext(@NonNull RestResponse response) {
        OperationLogContext context = response.getLogContext();
        return context == null ? null : context.withStatusCode(response.getStatusCode());
    }
}
