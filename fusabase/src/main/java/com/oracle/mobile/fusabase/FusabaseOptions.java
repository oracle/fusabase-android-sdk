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

package com.oracle.mobile.fusabase;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.models.IDCSOptions;
import com.oracle.mobile.fusabase.models.Options;

import java.util.Objects;

/**
 * Represents the configuration options for the Fusabase SDK.
 * <p>
 * This class encapsulates various settings required to connect to and configure Fusabase services,
 * including authentication credentials, project and app identifiers, server endpoints, and more.
 * </p>
 * <p>
 * Instances of FusabaseOptions are immutable and should be created using the {@link Builder} class.
 * </p>
 */
public class FusabaseOptions {

    /**
     * The underlying options object containing the configuration data.
     */
    private final Options options;

    /**
     * Constructs a new FusabaseOptions instance with the provided options.
     *
     * @param options The options object containing configuration data.
     */
    public FusabaseOptions(Options options) {
        this.options = options;
    }

    /**
     * Returns the hash code for this FusabaseOptions instance.
     *
     * @return The hash code based on the underlying options.
     */
    @Override
    public int hashCode() {
        return options.hashCode();
    }

    /**
     * Checks if the provided object is equal to this FusabaseOptions instance.
     * Two FusabaseOptions instances are considered equal if they have the same underlying options.
     *
     * @param obj The object to compare with this instance.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FusabaseOptions)) {
            return false;
        }
        FusabaseOptions that = (FusabaseOptions) obj;
        return Objects.equals(options, that.options);
    }

    /**
     * Gets the ORDS host URL.
     *
     * @return The ORDS host URL.
     */
    @NonNull
    public String getOrdsHost() {
        return this.options.getOrdsHost();
    }

    /**
     * Gets the IDCS Domain URL.
     *
     * @return The IDCS domain URL.
     */
    @NonNull
    public String getDomainURL() { return this.options.getDomainURL();}
    /**
     * Gets the project ID.
     *
     * @return The project ID.
     */
    @NonNull
    public String getProjectId() {
        return this.options.getProjectId();
    }

    /**
     * Gets the app ID.
     *
     * @return The app ID.
     */
    @NonNull
    public String getAppId() {
        return this.options.getAppId();
    }

    /**
     * Gets the app name.
     *
     * @return The app name.
     */
    @NonNull
    public String getAppName() {
        return this.options.getAppName();
    }

    /**
     * Gets the authentication ID.
     *
     * @return The authentication ID.
     */
    public String getAuthId() {
        return this.options.getAuthId();
    }

    /**
     * Gets the authentication type.
     *
     * @return The authentication type.
     */
    public String getAuthType() {
        return this.options.getAuthType();
    }

    /**
     * Gets the objects type.
     *
     * @return The objects type.
     */
    public String getObjectsType() {
        return this.options.getObjectsType();
    }

    /**
     * Gets the storage bucket name.
     *
     * @return The storage bucket name.
     */
    public String getStorageBucket() {
        return this.options.getStorageBucket();
    }

    /**
     * Gets the client ID.
     *
     * @return The client ID.
     */
    public String getClientId() {
        return this.options.getClientId();
    }

    /**
     * Gets the client secret.
     *
     * @return The client secret.
     */
    public String getClientSecret() {
        return this.options.getClientSecret();
    }

    /**
     * Gets the schema.
     *
     * @return The schema.
     */
    public String getSchema() {
        return this.options.getSchema();
    }

    /**
     * Gets the apiVersion.
     *
     * @return The apiVersion.
     */
    public String getApiVersion() {
        return this.options.getApiVersion();
    }

    /**
     * Gets the useSocket config. Returns true if using WebSocket for onSnapshot
     * else false while using Long polling.
     * @return The useSocket
     */
    public boolean isUseSocket() {
        return this.options.isUseSocket();
    }

    /**
     * Gets the enableLogging config. Returns true if logging is enabled
     * else false.
     * @return The enableLogging
     */
    public boolean isEnableLogging() {
        return this.options.isEnableLogging();
    }

    /**
     * Gets the upload chunk size for file uploads.
     *
     * @return The upload chunk size in bytes.
     */
    public int getUploadChunkSize() {
        return this.options.getUploadChunkSize();
    }

    /**
     * Gets the long polling interval for real-time updates.
     *
     * @return The long polling interval in milliseconds.
     */
    public long getLongPollingInterval() {
        return this.options.getLongPollingInterval();
    }

    /**
     * Gets the allowsSelfSignedCertificates config. Returns true if self-signed certificates
     * are allowed for testing purposes, else false for secure production use.
     * <p>
     * <strong>WARNING:</strong> This option should only be enabled for testing and development.
     * Allowing self-signed certificates reduces security and should never be used in production.
     * </p>
     *
     * @return The allowsSelfSignedCertificates setting
     */
    public boolean allowsSelfSignedCertificates() {
        return this.options.allowsSelfSignedCertificates();
    }

    /**
     * Class FusabaseOptions.Builder
     * Builder for creating new instances of FusabaseOptions.
     *
     * Use this class to construct an FusabaseOptions object step-by-step, setting individual properties using the
     * corresponding setter methods. Once all necessary properties have been set, call the {@link #build()} method
     * to create the final FusabaseOptions object.
     */
    public static class Builder {
        private final Options.Builder optionsBuilder = new Options.Builder();

        /**
         * Sets the ORDS host URL.
         *
         * @param ordsHost The ORDS host URL.
         * @return This builder instance.
         */
        public Builder setOrdsHost(@NonNull String ordsHost) {
            optionsBuilder.setOrdsHost(ordsHost);
            return this;
        }

        /**
         * Sets the project ID.
         *
         * @param projectId The project ID.
         * @return This builder instance.
         */
        public Builder setProjectId(@NonNull String projectId) {
            optionsBuilder.setProjectId(projectId);
            return this;
        }

        /**
         * Sets the app name.
         *
         * @param appName The app name.
         * @return This builder instance.
         */
        public Builder setAppName(@NonNull String appName) {
            optionsBuilder.setAppName(appName);
            return this;
        }

        /**
         * Sets the app ID.
         *
         * @param appId The app ID.
         * @return This builder instance.
         */
        public Builder setAppId(@NonNull String appId) {
            optionsBuilder.setAppId(appId);
            return this;
        }

        /**
         * Sets the authentication ID.
         *
         * @param authId The authentication ID.
         * @return This builder instance.
         */
        public Builder setAuthId(@NonNull String authId) {
            optionsBuilder.setAuthId(authId);
            return this;
        }

        /**
         * Sets the authentication type.
         *
         * @param authType The authentication type.
         * @return This builder instance.
         */
        public Builder setAuthType(@NonNull String authType) {
            optionsBuilder.setAuthType(authType);
            return this;
        }

        /**
         * Sets the objects type.
         *
         * @param objectsType The objects type.
         * @return This builder instance.
         */
        public Builder setObjectType(@NonNull String objectsType) {
            optionsBuilder.setObjectType(objectsType);
            return this;
        }

        /**
         * Sets the storage bucket name.
         *
         * @param storageBucket The storage bucket name.
         * @return This builder instance.
         */
        public Builder setStorageBucket(@NonNull String storageBucket) {
            optionsBuilder.setStorageBucket(storageBucket);
            return this;
        }

        /**
         * Sets the schema.
         *
         * @param schema The schema.
         * @return This builder instance.
         */
        public Builder setSchema(@NonNull String schema) {
            optionsBuilder.setSchema(schema);
            return this;
        }

        /**
         * Sets the apiVersion.
         *
         * @param apiVersion The apiVersion.
         * @return This builder instance.
         */
        public Builder setApiVersion(@NonNull String apiVersion) {
            optionsBuilder.setApiVersion(apiVersion);
            return this;
        }

        /**
         * Sets the useSocket property.
         * @param useSocket
         * @return
         */
        public Builder setUseSocket(boolean useSocket){
            optionsBuilder.setUseSocket(useSocket);
            return this;
        }

        /**
         * Sets the enableLogging property.
         * @param enableLogging
         * @return
         */
        public Builder setEnableLogging(boolean enableLogging){
            optionsBuilder.setEnableLogging(enableLogging);
            return this;
        }

        /**
         * Sets the IDCS options.
         *
         * @param idcsOptions The IDCS options.
         * @return This builder instance.
         */
        public Builder setIDCSOptions(@NonNull IDCSOptions idcsOptions) {
            optionsBuilder.setIDCSOptions(idcsOptions);
            return this;
        }

        /**
         * Sets the upload chunk size for file uploads.
         *
         * @param uploadChunkSize The upload chunk size in bytes.
         * @return This builder instance.
         */
        public Builder setUploadChunkSize(int uploadChunkSize) {
            optionsBuilder.setUploadChunkSize(uploadChunkSize);
            return this;
        }

        /**
         * Sets the long polling interval for real-time updates.
         *
         * @param longPollingInterval The long polling interval in milliseconds.
         * @return This builder instance.
         */
        public Builder setLongPollingInterval(long longPollingInterval) {
            optionsBuilder.setLongPollingInterval(longPollingInterval);
            return this;
        }

        /**
         * Sets whether self-signed certificates are allowed for testing purposes.
         * <p>
         * <strong>WARNING:</strong> This should only be set to true for testing and development.
         * Allowing self-signed certificates reduces security and should never be used in production.
         * </p>
         *
         * @param allowsSelfSignedCertificates true to allow self-signed certificates, false for secure production use
         * @return This builder instance.
         */
        public Builder setAllowsSelfSignedCertificates(boolean allowsSelfSignedCertificates) {
            optionsBuilder.setAllowsSelfSignedCertificates(allowsSelfSignedCertificates);
            return this;
        }

        /**
         * Builds the FusabaseOptions object based on the properties set in this builder.
         *
         * @return The constructed FusabaseOptions object.
         */
        public FusabaseOptions build() {
            return new FusabaseOptions(optionsBuilder.build());
        }
    }
}
