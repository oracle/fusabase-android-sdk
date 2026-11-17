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
 *
 * <p>Example:</p>
 * <pre>{@code
 * FusabaseOracledbSettings settings = FusabaseOracledbSettings
 *     .newBuilder(FusabaseApp.getInstance().getOptions())
 *     .setPersistentCacheEnabled(true).build();
 * }</pre>
 */
public class FusabaseOracledbSettings {

    /**
     *  Default maximum size of the persistent local cache. 
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledbSettings settings = FusabaseOracledbSettings
     *     .newBuilder(FusabaseApp.getInstance().getOptions())
     *     .setPersistentCacheEnabled(true).build();
     * }</pre>
     */
    public static final long DEFAULT_CACHE_SIZE_BYTES = 100L * 1024L * 1024L;

    /**
     *  Disables size-based cache eviction. 
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledbSettings settings = FusabaseOracledbSettings
     *     .newBuilder(FusabaseApp.getInstance().getOptions())
     *     .setPersistentCacheEnabled(true).build();
     * }</pre>
     */
    public static final long CACHE_SIZE_UNLIMITED = -1L;

    /** Timeout duration for long polling operations in milliseconds */
    private final long longPollingTimeout;

    /** Maximum number of retry attempts for failed operations */
    private final int maxAttempt;

    /** General FUSABASE options affecting connection and authentication behavior */
    private final FusabaseOptions options;

    /** Whether the default persistent local cache is enabled. */
    private final boolean persistentCacheEnabled;

    /** Maximum persistent local cache size in bytes, or {@link #CACHE_SIZE_UNLIMITED}. */
    private final long cacheSizeBytes;

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
        this(new Builder(options)
                .setLongPollingTimeout(interval)
                .setMaxAttempt(maxAttempt));
    }

    private FusabaseOracledbSettings(@NonNull Builder builder) {
        this.options = builder.options;
        this.longPollingTimeout = builder.longPollingTimeout;
        this.maxAttempt = builder.maxAttempt;
        this.persistentCacheEnabled = builder.persistentCacheEnabled;
        this.cacheSizeBytes = builder.cacheSizeBytes;
    }

    /**
     * Returns the FUSABASE options containing connection and authentication settings.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledbSettings settings = FusabaseOracledbSettings
     *     .newBuilder(FusabaseApp.getInstance().getOptions())
     *     .setPersistentCacheEnabled(true).build();
     * }</pre>
     *
     * @return The FusabaseOptions instance
     */
    public FusabaseOptions getOptions() {
        return this.options;
    }

    /**
     * Returns the timeout duration for long polling operations.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledbSettings settings = FusabaseOracledbSettings
     *     .newBuilder(FusabaseApp.getInstance().getOptions())
     *     .setPersistentCacheEnabled(true).build();
     * }</pre>
     *
     * @return The long polling timeout in milliseconds
     */
    public long getLongPollingTimeout() {
        return this.longPollingTimeout;
    }

    /**
     * Returns the maximum number of retry attempts for failed operations.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledbSettings settings = FusabaseOracledbSettings
     *     .newBuilder(FusabaseApp.getInstance().getOptions())
     *     .setPersistentCacheEnabled(true).build();
     * }</pre>
     *
     * @return The maximum number of retry attempts
     */
    public int getMaxAttempt() {
        return this.maxAttempt;
    }

    /**
     * Returns whether the persistent local cache is enabled for this database client.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledbSettings settings = FusabaseOracledbSettings
     *     .newBuilder(FusabaseApp.getInstance().getOptions())
     *     .setPersistentCacheEnabled(true).build();
     * }</pre>
     *
     * @return {@code true} when persistent local cache is enabled
     */
    public boolean isPersistentCacheEnabled() {
        return persistentCacheEnabled;
    }

    /**
     * Returns the configured maximum persistent local cache size.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledbSettings settings = FusabaseOracledbSettings
     *     .newBuilder(FusabaseApp.getInstance().getOptions())
     *     .setPersistentCacheEnabled(true).build();
     * }</pre>
     *
     * @return cache size in bytes, or {@link #CACHE_SIZE_UNLIMITED}
     */
    public long getCacheSizeBytes() {
        return cacheSizeBytes;
    }

    /**
     * Creates a settings builder with persistent local cache disabled by default.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledbSettings settings = FusabaseOracledbSettings
     *     .newBuilder(FusabaseApp.getInstance().getOptions())
     *     .setPersistentCacheEnabled(true).build();
     * }</pre>
     *
     * @param options application options used by database operations
     * @return a new settings builder
     */
    @NonNull
    public static Builder newBuilder(@NonNull FusabaseOptions options) {
        return new Builder(options);
    }

    /**
     *  Builder for {@link FusabaseOracledbSettings}. 
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledbSettings settings = FusabaseOracledbSettings
     *     .newBuilder(FusabaseApp.getInstance().getOptions())
     *     .setPersistentCacheEnabled(true).build();
     * }</pre>
     */
    public static final class Builder {

        private final FusabaseOptions options;
        private long longPollingTimeout = 29L;
        private int maxAttempt = 1;
        private boolean persistentCacheEnabled = false;
        private long cacheSizeBytes = DEFAULT_CACHE_SIZE_BYTES;

        /**
         * Creates a builder for the supplied application options.
         *
         *
         * <p>Example: Obtain this value through the public factory or operation shown below.</p>
         * <pre>{@code
         * FusabaseOracledbSettings settings = FusabaseOracledbSettings
         *     .newBuilder(FusabaseApp.getInstance().getOptions())
         *     .setPersistentCacheEnabled(true).build();
         * }</pre>
         *
         * @param options application options used by database operations
         */
        public Builder(@NonNull FusabaseOptions options) {
            this.options = options;
        }

        /**
         * Sets the long-polling interval in milliseconds.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * FusabaseOracledbSettings settings = FusabaseOracledbSettings
         *     .newBuilder(FusabaseApp.getInstance().getOptions())
         *     .setPersistentCacheEnabled(true).build();
         * }</pre>
         *
         * @param timeout long-polling interval in milliseconds
         * @return this builder
         */
        @NonNull
        public Builder setLongPollingTimeout(long timeout) {
            if (timeout <= 0) {
                throw new IllegalArgumentException("Long polling timeout must be positive");
            }
            this.longPollingTimeout = timeout;
            return this;
        }

        /**
         * Sets the maximum number of request attempts.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * FusabaseOracledbSettings settings = FusabaseOracledbSettings
         *     .newBuilder(FusabaseApp.getInstance().getOptions())
         *     .setPersistentCacheEnabled(true).build();
         * }</pre>
         *
         * @param attempts maximum number of attempts
         * @return this builder
         */
        @NonNull
        public Builder setMaxAttempt(int attempts) {
            if (attempts <= 0) {
                throw new IllegalArgumentException("Maximum attempts must be positive");
            }
            this.maxAttempt = attempts;
            return this;
        }

        /**
         * Enables or disables persistent local cache.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * FusabaseOracledbSettings settings = FusabaseOracledbSettings
         *     .newBuilder(FusabaseApp.getInstance().getOptions())
         *     .setPersistentCacheEnabled(true).build();
         * }</pre>
         *
         * @param enabled whether persistent local cache is enabled
         * @return this builder
         */
        @NonNull
        public Builder setPersistentCacheEnabled(boolean enabled) {
            this.persistentCacheEnabled = enabled;
            return this;
        }

        /**
         * Sets the maximum persistent local cache size.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * FusabaseOracledbSettings settings = FusabaseOracledbSettings
         *     .newBuilder(FusabaseApp.getInstance().getOptions())
         *     .setPersistentCacheEnabled(true).build();
         * }</pre>
         *
         * @param sizeBytes positive cache size in bytes, or {@link #CACHE_SIZE_UNLIMITED}
         * @return this builder
         */
        @NonNull
        public Builder setCacheSizeBytes(long sizeBytes) {
            if (sizeBytes != CACHE_SIZE_UNLIMITED && sizeBytes <= 0) {
                throw new IllegalArgumentException(
                        "Cache size must be positive or CACHE_SIZE_UNLIMITED");
            }
            this.cacheSizeBytes = sizeBytes;
            return this;
        }

        /**
         * Builds immutable database settings.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * FusabaseOracledbSettings settings = FusabaseOracledbSettings
         *     .newBuilder(FusabaseApp.getInstance().getOptions())
         *     .setPersistentCacheEnabled(true).build();
         * }</pre>
         *
         * @return configured settings
         */
        @NonNull
        public FusabaseOracledbSettings build() {
            return new FusabaseOracledbSettings(this);
        }
    }
}
