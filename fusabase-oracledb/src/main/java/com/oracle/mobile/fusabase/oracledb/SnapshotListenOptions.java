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

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Configuration options for snapshot listeners. This class allows configuring
 * how snapshot events are delivered, including the executor, metadata changes,
 * and listen source.
 *
 * <p>Example:</p>
 * <pre>{@code
 * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
 *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
 * }</pre>
 */
public final class SnapshotListenOptions {

    /**
     *  The activity associated with this listener, if any. 
     *
     * <p>Example:</p>
     * <pre>{@code
     * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
     *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
     * }</pre>
     */
    @Nullable
    public final Activity activity;

    /**
     *  The executor used to deliver snapshot events. 
     *
     * <p>Example:</p>
     * <pre>{@code
     * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
     *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
     * }</pre>
     */
    @NonNull
    public final Executor executor;

    /**
     *  Configuration for which metadata changes to include in events. 
     *
     * <p>Example:</p>
     * <pre>{@code
     * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
     *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
     * }</pre>
     */
    @NonNull
    public final MetadataChanges metadataChanges;

    /**
     *  The source from which to listen for changes. 
     *
     * <p>Example:</p>
     * <pre>{@code
     * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
     *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
     * }</pre>
     */
    @NonNull
    public final ListenSource source;

    /**
     * Private constructor for creating SnapshotListenOptions from a Builder.
     *
     * @param builder the builder containing the configuration
     */
    private SnapshotListenOptions (@NonNull SnapshotListenOptions.Builder builder) {
        this.activity = builder.getActivity();
        this.metadataChanges = builder.getMetadataChanges() == null ? MetadataChanges.EXCLUDE : builder.getMetadataChanges();
        this.executor = builder.getExecutor();
        this.source = builder.getSource() == null ? ListenSource.DEFAULT : builder.getSource();
    }

    /**
     * Returns the activity associated with this listener.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
     *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
     * }</pre>
     *
     * @return the activity, or null if none was set
     */
    @Nullable
    public Activity getActivity() {
        return activity;
    }

    /**
     * Returns the executor used to deliver snapshot events.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
     *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
     * }</pre>
     *
     * @return the executor
     */
    @NonNull
    public Executor getExecutor() {
        return executor;
    }

    /**
     * Returns the metadata changes configuration.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
     *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
     * }</pre>
     *
     * @return the metadata changes setting
     */
    @NonNull
    public MetadataChanges getMetadataChanges() {
        return metadataChanges;
    }

    /**
     * Compares this SnapshotListenOptions with another object for equality.
     *
     *
     * <p>Example: Given compatible values named as shown.</p>
     * <pre>{@code
     * boolean same = first.equals(second);
     * }</pre>
     *
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnapshotListenOptions that = (SnapshotListenOptions) o;
        return Objects.equals(activity, that.activity) && Objects.equals(executor, that.executor) && metadataChanges == that.metadataChanges && source == that.source;
    }

    /**
     * Returns the hash code for this SnapshotListenOptions.
     *
     *
     * <p>Example: Given compatible values named as shown.</p>
     * <pre>{@code
     * int hash = value.hashCode();
     * }</pre>
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(activity, executor, metadataChanges, source);
    }

    /**
     * Returns the listen source configuration.
     *
     *
     * <p>Example:</p>
     * <pre>{@code
     * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
     *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
     * }</pre>
     *
     * @return the listen source
     */
    @NonNull
    public ListenSource getSource() {
        return source;
    }

    /**
     * Builder class for creating SnapshotListenOptions instances.
     * Provides a fluent API for configuring listener options.
     *
     * <p>Example:</p>
     * <pre>{@code
     * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
     *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
     * }</pre>
     */
    public static class Builder {

        private Activity activity;
        private Executor executor = command -> new Handler(Looper.getMainLooper()).post(command);
        private MetadataChanges metadataChanges;
        private ListenSource source;

        /**
         * Sets the activity for this listener.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
         *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
         * }</pre>
         *
         * @param activity the activity to associate with the listener
         * @return this Builder instance for chaining
         */
        public SnapshotListenOptions.Builder setActivity(@NonNull Activity activity) {
            this.activity = activity;
            return this;
        }

        /**
         * Sets the executor for delivering snapshot events.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
         *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
         * }</pre>
         *
         * @param executor the executor to use for event delivery
         * @return this Builder instance for chaining
         */
        public SnapshotListenOptions.Builder setExecutor (@NonNull Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Sets the metadata changes configuration.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
         *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
         * }</pre>
         *
         * @param metadataChanges the metadata changes setting
         * @return this Builder instance for chaining
         */
        public SnapshotListenOptions.Builder setMetadataChanges (@NonNull MetadataChanges metadataChanges) {
            this.metadataChanges = metadataChanges;
            return this;
        }

        /**
         * Sets the listen source configuration.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
         *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
         * }</pre>
         *
         * @param source the listen source to use
         * @return this Builder instance for chaining
         */
        public SnapshotListenOptions.Builder setSource (@NonNull ListenSource source) {
            this.source = source;
            return this;
        }

        /** @return the configured activity */
        @Nullable
        protected Activity getActivity () {
            return this.activity;
        }

        /** @return the configured executor */
        @NonNull
        protected Executor getExecutor () {
            return this.executor;
        }

        /** @return the configured metadata changes */
        @Nullable
        protected MetadataChanges getMetadataChanges () {
            return this.metadataChanges;
        }

        /** @return the configured listen source */
        @Nullable
        protected ListenSource getSource() {
            return this.source;
        }

        /**
         * Builds and returns a SnapshotListenOptions instance with the configured options.
         *
         *
         * <p>Example:</p>
         * <pre>{@code
         * SnapshotListenOptions options = new SnapshotListenOptions.Builder()
         *     .setMetadataChanges(MetadataChanges.INCLUDE).build();
         * }</pre>
         *
         * @return a new SnapshotListenOptions instance
         */
        public SnapshotListenOptions build() {
            return new SnapshotListenOptions(this); // Pass the builder itself to the User constructor
        }
    }
}
