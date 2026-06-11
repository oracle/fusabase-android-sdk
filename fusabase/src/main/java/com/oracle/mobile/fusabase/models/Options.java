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

package com.oracle.mobile.fusabase.models;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.core.FusabaseConstants;
import java.util.Objects;

/**
 * Model class for FusabaseOptions data.
 */
public class Options {
    private final String ordsHost;
    private final String projectId;
    private final String appId;
    private final String authId;
    private final String authType;
    private final String objectsType;
    private final String storageBucket;
    private final String idcsDomainURL;
    private final String schema;
    private final String name;
    private final String apiVersion;
    private final boolean useSocket;
    private final boolean enableLogging;
    private final int uploadChunkSize;
    private final long longPollingInterval;

    public Options(Builder builder) {
        this.ordsHost = builder.ordsHost;
        this.projectId = builder.projectId;
        this.appId = builder.appId;
        this.authId = builder.authId;
        this.authType = builder.authType;
        this.objectsType = builder.objectsType;
        this.storageBucket = builder.storageBucket;
        this.idcsDomainURL = builder.idcsDomainURL;
        this.schema = builder.schema;
        this.name = builder.appName;
        this.apiVersion = builder.apiVersion;
        this.useSocket = builder.useSocket;
        this.enableLogging = builder.enableLogging;
        this.uploadChunkSize = builder.uploadChunkSize;
        this.longPollingInterval = builder.longPollingInterval;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, authType, name, authId, idcsDomainURL,
                projectId, objectsType, ordsHost, schema,
                storageBucket, apiVersion, useSocket, enableLogging, uploadChunkSize, longPollingInterval);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Options)) {
            return false;
        }
        Options that = (Options) obj;
        return Objects.equals(ordsHost, that.ordsHost) &&
                Objects.equals(projectId, that.projectId) &&
                Objects.equals(appId, that.appId) &&
                Objects.equals(authId, that.authId) &&
                Objects.equals(authType, that.authType) &&
                Objects.equals(objectsType, that.objectsType) &&
                Objects.equals(storageBucket, that.storageBucket) &&
                Objects.equals(idcsDomainURL, that.idcsDomainURL) &&
                Objects.equals(schema, that.schema) &&
                Objects.equals(name, that.name) &&
                Objects.equals(apiVersion, that.apiVersion) &&
                useSocket == that.useSocket &&
                enableLogging == that.enableLogging &&
                uploadChunkSize == that.uploadChunkSize &&
                longPollingInterval == that.longPollingInterval;
    }

    /**
     * Gets the ORDS host URL.
     *
     * @return The ORDS host URL.
     */
    @NonNull
    public String getOrdsHost() {
        return this.ordsHost;
    }

    /**
     * Gets the IDCS Domain URL.
     *
     * @return The IDCS domain URL.
     */
    @NonNull
    public String getDomainURL() {
        return this.idcsDomainURL != null ? this.idcsDomainURL : "";
    }

    /**
     * Gets the project ID.
     *
     * @return The project ID.
     */
    @NonNull
    public String getProjectId() {
        return this.projectId;
    }

    /**
     * Gets the app ID.
     *
     * @return The app ID.
     */
    @NonNull
    public String getAppId() {
        return this.appId;
    }

    /**
     * Gets the app name.
     *
     * @return The app name.
     */
    @NonNull
    public String getAppName() {
        return this.name;
    }

    /**
     * Gets the authentication ID.
     *
     * @return The authentication ID.
     */
    public String getAuthId() {
        return this.authId;
    }

    /**
     * Gets the authentication type.
     *
     * @return The authentication type.
     */
    public String getAuthType() {
        return this.authType;
    }

    /**
     * Gets the objects type.
     *
     * @return The objects type.
     */
    public String getObjectsType() {
        return this.objectsType;
    }

    /**
     * Gets the storage bucket name.
     *
     * @return The storage bucket name.
     */
    public String getStorageBucket() {
        return this.storageBucket;
    }

    /**
     * Gets the schema.
     *
     * @return The schema.
     */
    public String getSchema() {
        return this.schema;
    }

    /**
     * Gets the apiVersion.
     *
     * @return The apiVersion.
     */
    public String getApiVersion() {
        return this.apiVersion;
    }

    /**
     * Gets the useSocket config. Returns true if using WebSocket for onSnapshot
     * else false while using Long polling.
     * @return The useSocket
     */
    public boolean isUseSocket() {
        return this.useSocket;
    }

    /**
     * Gets the enableLogging config. Returns true if logging is enabled
     * else false.
     * @return The enableLogging
     */
    public boolean isEnableLogging() {
        return this.enableLogging;
    }

    /**
     * Gets the upload chunk size for file uploads.
     *
     * @return The upload chunk size in bytes.
     */
    public int getUploadChunkSize() {
        return this.uploadChunkSize;
    }

    /**
     * Gets the long polling interval for real-time updates.
     *
     * @return The long polling interval in milliseconds.
     */
    public long getLongPollingInterval() {
        return this.longPollingInterval;
    }

    /**
     * Builder for creating new instances of Options.
     */
    public static class Builder {

        private String ordsHost;
        private String projectId;
        private String appId;
        private String authId;
        private String authType;
        private String objectsType;
        private String storageBucket;
        private String idcsDomainURL;
        private String schema;
        private String appName;
        private String apiVersion;
        private boolean useSocket;
        private boolean enableLogging;
        private int uploadChunkSize;
        private long longPollingInterval;

        /**
         * Constructs a new Builder with default values.
         */
        public Builder() {
            this.uploadChunkSize = FusabaseConstants.DEFAULT_UPLOAD_CHUNK_SIZE;
            this.longPollingInterval = FusabaseConstants.DEFAULT_LONG_POLLING_INTERVAL;
        }

        /**
         * Sets the ORDS host URL.
         *
         * @param ordsHost The ORDS host URL.
         * @return This builder instance.
         */
        public Builder setOrdsHost(@NonNull String ordsHost) {
            this.ordsHost = ordsHost;
            return this;
        }

        /**
         * Sets the project ID.
         *
         * @param projectId The project ID.
         * @return This builder instance.
         */
        public Builder setProjectId(@NonNull String projectId) {
            this.projectId = projectId;
            return this;
        }

        /**
         * Sets the app name.
         *
         * @param appName The app name.
         * @return This builder instance.
         */
        public Builder setAppName(@NonNull String appName) {
            this.appName = appName;
            return this;
        }

        /**
         * Sets the app ID.
         *
         * @param appId The app ID.
         * @return This builder instance.
         */
        public Builder setAppId(@NonNull String appId) {
            this.appId = appId;
            return this;
        }

        /**
         * Sets the authentication ID.
         *
         * @param authId The authentication ID.
         * @return This builder instance.
         */
        public Builder setAuthId(@NonNull String authId) {
            this.authId = authId;
            return this;
        }

        /**
         * Sets the authentication type.
         *
         * @param authType The authentication type.
         * @return This builder instance.
         */
        public Builder setAuthType(@NonNull String authType) {
            this.authType = authType;
            return this;
        }

        /**
         * Sets the objects type.
         *
         * @param objectsType The objects type.
         * @return This builder instance.
         */
        public Builder setObjectType(@NonNull String objectsType) {
            this.objectsType = objectsType;
            return this;
        }

        /**
         * Sets the storage bucket name.
         *
         * @param storageBucket The storage bucket name.
         * @return This builder instance.
         */
        public Builder setStorageBucket(@NonNull String storageBucket) {
            this.storageBucket = storageBucket;
            return this;
        }

        /**
         * Sets the schema.
         *
         * @param schema The schema.
         * @return This builder instance.
         */
        public Builder setSchema(@NonNull String schema) {
            this.schema = schema;
            return this;
        }

        /**
         * Sets the IDCS domain URL.
         *
         * @param idcsDomainURL The IDCS domain URL.
         * @return This builder instance.
         */
        public Builder setIdcsDomainURL(@NonNull String idcsDomainURL) {
            this.idcsDomainURL = idcsDomainURL;
            return this;
        }

        /**
         * Sets the apiVersion.
         *
         * @param apiVersion The apiVersion.
         * @return This builder instance.
         */
        public Builder setApiVersion(@NonNull String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        /**
         * Sets the useSocket property.
         * @param useSocket
         * @return
         */
        public Builder setUseSocket(boolean useSocket){
            this.useSocket = useSocket;
            return this;
        }

        /**
         * Sets the enableLogging property.
         * @param enableLogging
         * @return
         */
        public Builder setEnableLogging(boolean enableLogging){
            this.enableLogging = enableLogging;
            return this;
        }

        /**
         * Sets the upload chunk size for file uploads.
         *
         * @param uploadChunkSize The upload chunk size in bytes.
         * @return This builder instance.
         */
        public Builder setUploadChunkSize(int uploadChunkSize) {
            this.uploadChunkSize = uploadChunkSize;
            return this;
        }

        /**
         * Sets the long polling interval for real-time updates.
         *
         * @param longPollingInterval The long polling interval in milliseconds.
         * @return This builder instance.
         */
        public Builder setLongPollingInterval(long longPollingInterval) {
            this.longPollingInterval = longPollingInterval;
            return this;
        }

        /**
         * Builds the Options object based on the properties set in this builder.
         *
         * @return The constructed Options object.
         */
        public Options build() {
            return new Options(this);
        }
    }
}
