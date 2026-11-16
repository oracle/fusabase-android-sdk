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


package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

/** Compares only realtime versions whose ordering is unambiguous. */
final class RealtimeVersion {
    private RealtimeVersion() { }

    static boolean isIncomingOlder(@Nullable String current, @Nullable String incoming) {
        if (current == null || current.isEmpty() || incoming == null || incoming.isEmpty()) {
            return false;
        }
        try {
            return new BigDecimal(incoming).compareTo(new BigDecimal(current)) < 0;
        } catch (NumberFormatException ignored) {
            // Fall through to ISO-8601 timestamps, matching the JS Date.parse fallback.
        }
        Long currentTime = parseTimestamp(current);
        Long incomingTime = parseTimestamp(incoming);
        return currentTime != null && incomingTime != null && incomingTime < currentTime;
    }

    @Nullable
    private static Long parseTimestamp(String value) {
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (RuntimeException ignored) {
            try {
                return OffsetDateTime.parse(value).toInstant().toEpochMilli();
            } catch (RuntimeException ignoredAgain) {
                return null;
            }
        }
    }
}
