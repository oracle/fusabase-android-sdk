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

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseOptions;

/**
 * Configuration settings for Oracle Database operations in the FUSABASE SDK.
 *
 * <p>This class encapsulates various settings that control the behavior of database operations,
 * including timeouts, retry policies, and connection options. It provides a centralized way
 * to configure database behavior for different environments and use cases.</p>
 *
 * <p>Settings include long polling timeout configuration, maximum retry attempts for operations,
 * and general FUSABASE options that affect connection and authentication behavior.</p>
 */
public class FusabaseOracledbSettings {

    /** Timeout duration for long polling operations in milliseconds */
    private final long longPollingTimeout;

    /** Maximum number of retry attempts for failed operations */
    private final int maxAttempt;

    /** General FUSABASE options affecting connection and authentication behavior */
    private final FusabaseOptions options;

    /**
     * Constructs a new FusabaseOracledbSettings instance with the specified configuration.
     *
     * @param options The FUSABASE options containing connection and authentication settings
     * @param interval The timeout interval for long polling operations in milliseconds
     * @param maxAttempt The maximum number of retry attempts for failed operations
     */
    FusabaseOracledbSettings(@NonNull FusabaseOptions options,
                          @NonNull long interval,
                          @NonNull int maxAttempt) {
        this.options = options;
        this.longPollingTimeout = interval;
        this.maxAttempt = maxAttempt;
    }

    /**
     * Returns the FUSABASE options containing connection and authentication settings.
     *
     * @return The FusabaseOptions instance
     */
    public FusabaseOptions getOptions() {
        return this.options;
    }

    /**
     * Returns the timeout duration for long polling operations.
     *
     * @return The long polling timeout in milliseconds
     */
    public long getLongPollingTimeout() {
        return this.longPollingTimeout;
    }

    /**
     * Returns the maximum number of retry attempts for failed operations.
     *
     * @return The maximum number of retry attempts
     */
    public int getMaxAttempt() {
        return this.maxAttempt;
    }
}
