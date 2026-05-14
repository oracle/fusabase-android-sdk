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

package com.oracle.mobile.fusabase.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * Represents metadata associated with a storage object in the cloud storage service.
 *
 * This class encapsulates information about a stored object including its name, size,
 * content type, timestamps, and other metadata. It provides a builder pattern for
 * constructing metadata instances and methods to access metadata properties.
 *
 * <p>StorageMetadata instances are typically created from server responses or
 * constructed using the {@link Builder} class when uploading objects.</p>
 */
public class StorageMetadata {

    /**
     * Validates the content type string.
     *
     * @param contentType the content type to validate
     * @throws IllegalArgumentException if the content type is invalid
     */
    private static void validateContentType(@NonNull String contentType) {
        if (contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be empty");
        }
        if (contentType.length() > 255) {
            throw new IllegalArgumentException("Content type is too long");
        }

        String trimmed = contentType.trim();
        int slashIndex = trimmed.indexOf('/');
        if (slashIndex <= 0) {
            throw new IllegalArgumentException("Content type must start with a type followed by '/'");
        }
        if (slashIndex == trimmed.length() - 1) {
            throw new IllegalArgumentException("Content type must have a subtype after '/'");
        }
        if (trimmed.indexOf('/', slashIndex + 1) != -1) {
            throw new IllegalArgumentException("Content type must contain only one '/'");
        }

        String type = trimmed.substring(0, slashIndex);
        String subtype = trimmed.substring(slashIndex + 1);

        if (type.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type 'type' part cannot be empty");
        }
        if (subtype.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type 'subtype' part cannot be empty");
        }

        // Check for invalid characters (basic check)
        if (contentType.contains("\n") || contentType.contains("\r") || contentType.contains("\t")) {
            throw new IllegalArgumentException("Content type cannot contain whitespace characters");
        }
    }

    @Nullable private String bucket;
    @Nullable private String contentType;
    private long size;
    private long updatedTimeMillis;
    private long creationTimeMillis;
    @Nullable private String name;
    @Nullable private String md5Hash;
    @Nullable private String path;
    @Nullable private StorageReference storageReference;
    /**
     * A builder class for creating {@link StorageMetadata} instances.
     */
    public static class Builder
    {
        private String bucket;
        private String name;
        private long size;
        private long updatedTimeMillis;
        private long creationTimeMillis;
        private String md5Hash;
        private String contentType;
        private String path;
        private StorageReference storageReference;

        /**
         * Constructs a new builder instance
         *
         */
        public Builder() {
        }

        /**
         * Sets the content Type of the object.
         *
         * @param contentType of the object
         * @return this builder instance
         * @throws IllegalArgumentException if the content type is invalid
         */
        public Builder setContentType(@NonNull String contentType) {
            validateContentType(contentType);
            this.contentType = contentType;
            return this;
        }

        /**
         * Sets the name of the object.
         *
         * @param name of the object
         * @return this builder instance
         */
        public Builder setName(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the bucket name.
         *
         * @param bucket the bucket name
         * @return this builder instance
         */
        public Builder setBucket(@NonNull String bucket) {
            this.bucket = bucket;
            return this;
        }

        /**
         * Sets the size of the object.
         *
         * @param size the size of the object
         * @return this builder instance
         */
        public Builder setSize(long size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the creation time of the object.
         *
         * @param creationTimeMillis the creation time of the object
         * @return this builder instance
         */
        public Builder setCreationTimeMillis(long creationTimeMillis) {
            this.creationTimeMillis = creationTimeMillis;
            return this;
        }

        /**
         * Sets the update time of the object.
         *
         * @param updatedTimeMillis the creation time of the object
         * @return this builder instance
         */
        public Builder setUpdatedTimeMillis(long updatedTimeMillis) {
            this.updatedTimeMillis = updatedTimeMillis;
            return this;
        }

        /**
         * Sets the md5sum of the object.
         *
         * @param md5sum the md5sum of the object
         * @return this builder instance
         */
        public Builder setMd5Hash(@NonNull String md5sum) {
            this.md5Hash = md5sum;
            return this;
        }

        /**
         * Sets the path of the object.
         *
         * @param path the path of the object
         * @return this builder instance
         */
        public Builder setPath(@NonNull String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the storageReference of the object.
         *
         * @param storageReference the storageReference of the object
         * @return this builder instance
         */
        public Builder setStorageReference(@NonNull StorageReference storageReference) {
            this.storageReference = storageReference;
            return this;
        }

        /**
         * Builds a new {@link StorageMetadata} instance using the current builder settings.
         *
         * @return a new {@link StorageMetadata} instance
         */
        public StorageMetadata build() {
            return new StorageMetadata(this);
        }
    }

    /**
     * Constructs a new {@link StorageMetadata} instance using the provided builder.
     *
     * @param builder the builder instance
     */
    protected StorageMetadata (Builder builder) {
        this.bucket = builder.bucket;
        this.contentType = builder.contentType;
        this.name = builder.name;
        this.md5Hash = builder.md5Hash;
        this.size = builder.size;
        this.path = builder.path;
        this.storageReference = builder.storageReference;
        this.creationTimeMillis = builder.creationTimeMillis;
        this.updatedTimeMillis = builder.updatedTimeMillis;
    }

    /**
     * Constructs a new {@link StorageMetadata} instance using the provided builder.
     */
    public StorageMetadata () {
    }

    protected JsonObject getJsonObject () {
       JsonObjectBuilder metadataObjectBuilder = Json.createObjectBuilder();
       if(this.bucket != null)
           metadataObjectBuilder.add("bucket", this.bucket);
       if(this.contentType != null)
           metadataObjectBuilder.add("contentType", this.contentType);
       if(this.name != null)
           metadataObjectBuilder.add("name", this.name);
       if(this.md5Hash != null)
           metadataObjectBuilder.add("md5Hash", this.md5Hash);
       if(this.size != 0)
           metadataObjectBuilder.add("size", size);
       if(this.path != null)
           metadataObjectBuilder.add("path", path);
       return metadataObjectBuilder.build();
    }

    /**
     * Returns the bucket name associated with this metadata.
     *
     * @return the bucket name, or null if not specified
     */
    @Nullable public String getBucket(){
        return this.bucket;
    }

    /**
     * Returns the content type associated with this metadata.
     *
     * @return the content type, or null if not specified
     */
    @Nullable public String getContentType(){
        return this.contentType;
    }

    protected void setContentType(@NonNull String contentType) {
        validateContentType(contentType);
        this.contentType = contentType;
    }
    @Nullable public String getMd5Hash(){
        return this.md5Hash;
    }

    @Nullable public String getName(){
        return this.name;
    }

    @Nullable public String getPath() {
        return this.path;
    }

    @Nullable public StorageReference getReference() {
        return this.storageReference;
    }

    public long getSizeBytes(){
        return this.size;
    }

    public long getCreationTimeMillis() {
        return this.creationTimeMillis;
    }

    public long getUpdatedTimeMillis() {
        return this.updatedTimeMillis;
    }
}
