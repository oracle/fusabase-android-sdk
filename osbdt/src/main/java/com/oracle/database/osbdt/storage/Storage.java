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

package com.oracle.database.osbdt.storage;

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtException;
import com.oracle.database.osbdt.OsbdtOptions;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a cloud storage instance, providing access to BAAS Storage functionality.
 *
 * This class serves as the entry point for interacting with Oracle's cloud storage service.
 * It manages storage operations such as uploading, downloading, and managing files in
 * cloud storage buckets. Each Storage instance is associated with a specific OsbdtApp
 * and provides methods to create references to storage locations.
 *
 * <p>Storage instances are typically obtained through factory methods that ensure
 * proper initialization and singleton behavior per app instance.</p>
 */
public class Storage {

    /** Default maximum time to retry chunk uploads in milliseconds. */
    private static final int DEFAULT_MAX_CHUNK_UPLOAD_RETRY = 10000;
    /** Default maximum time to retry uploads in milliseconds. */
    private static final int DEFAULT_MAX_UPLOAD_RETRY = 10000;
    /** Default maximum time to retry operations in milliseconds. */
    private static final int DEFAULT_MAX_OPERATION_RETRY = 10000;

    /** The OsbdtApp instance associated with this storage. */
    private final OsbdtApp app;
    /** Singleton instance for default storage access. */
    private static Storage instance;

    /** Map holding all Storage instances keyed by app name. */
    private static final Map<String, Storage> INSTANCES = new HashMap<>();
    /** Maximum time to retry chunk uploads in milliseconds. */
    private long maxChunkUploadRetry;
    /** Maximum time to retry downloads in milliseconds. */
    private long maxDownloadRetryTimeMillis;
    /** Configuration options for this storage instance. */
    private final OsbdtOptions options;
    /** Name of the storage bucket associated with this instance. */
    private final String bucketName;
    /** Maximum time to retry operations in milliseconds. */
    private long maxOperationRetryTimeMillis;
    /** Maximum time to retry uploads in milliseconds. */
    private long maxUploadRetryTimeMillis;

    /**
     * Constructs a new storage instance.
     *
     * @param app The OsbdtApp associated with this storage instance.
     */
    protected Storage(@NonNull OsbdtApp app) {
        this.app = app;
        this.options = app.getOptions();
        this.bucketName = app.osbdtOptions.getStorageBucket();
        instance = this;
        INSTANCES.put(app.name, this);
    }

    /**
     * Gets the name of the bucket associated with this storage instance.
     *
     * @return The bucket name.
     */
    @NonNull
    String getBucketName() {
        return bucketName;
    }
    //    The OsbdtApp associated with this OsbdtStorage instance.

    /**
     * Method to get the app associated with this storage instance.
     *
     * @return OsbdtApp
     */
    @NonNull
    public OsbdtApp getApp() {
        return this.app;
    }

    /**
     * Method to get the instance of Storage of default osbdt app.
     *
     * @return Storage
     */
    public static Storage getInstance() throws OsbdtException {
        return getInstance(OsbdtApp.getInstance());
    }

    /**
     * Method to get the storage instance associated with provided app.
     *
     * @param app OsbdtApp
     * @return Storage
     */
    public static Storage getInstance(@NonNull OsbdtApp app) {
        if (INSTANCES.get(app.getName()) != null)
            return INSTANCES.get(app.getName());

        INSTANCES.put(app.getName(), new Storage(app));

        return getInstance(app);
    }

    /**
     * Gets the maximum time to retry uploading chunks in milliseconds.
     *
     * @return The maximum time.
     */
    public long getMaxChunkUploadRetry() {
        return this.maxChunkUploadRetry;
    }

    /**
     * Gets the maximum time to retry downloads in milliseconds.
     *
     * @return The maximum time.
     */
    public long getMaxDownloadRetryTimeMillis() {
        return this.maxDownloadRetryTimeMillis;
    }

    /**
     * Gets the maximum time to retry operations in milliseconds.
     *
     * @return The maximum time.
     */
    public long
    getMaxOperationRetryTimeMillis() {
        return this.maxOperationRetryTimeMillis;
    }

    /**
     * Gets the maximum time to retry uploads in milliseconds.
     *
     * @return The maximum time.
     */
    public long getMaxUploadRetryTimeMillis() {
        return this.maxUploadRetryTimeMillis;
    }
    @NonNull
    public StorageReference getReferenceFromUrl(@NonNull String url)
            throws IllegalArgumentException {
        int index = url.indexOf("/o/");
        if (index == -1) {
            throw new IllegalArgumentException("Invalid URL format: missing /o/ path segment");
        }
        String encodedPath = url.substring(index + 3); // after /o/
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL encoding");
        }
        return new StorageReference(this, decodedPath);
    }
    /**
     * Creates a new StorageReference initialized at the root Osbdt Storage location.
     *
     * @return The StorageReference instance.
     */
    @NonNull
    public StorageReference getReference() {
        return new StorageReference(this, "");
    }

    /**
     * Creates a new StorageReference initialized with a child Osbdt Storage location.
     *
     * @param location The location.
     * @return The StorageReference instance.
     */
    @NonNull
    public StorageReference getReference(@NonNull String location) {
        return new StorageReference(this, location);
    }

    /**
     * Sets the maximum time to retry uploading chunks in milliseconds.
     *
     * @param maxChunkRetryMillis The maximum time.
     */
    public void setMaxChunkUploadRetry(long maxChunkRetryMillis) {
        this.maxChunkUploadRetry = maxChunkRetryMillis;
    }

    /**
     * Sets the maximum time to retry downloads in milliseconds.
     *
     * @param maxTransferRetryMillis The maximum time.
     */
    public void setMaxDownloadRetryTimeMillis(long maxTransferRetryMillis) {
        this.maxDownloadRetryTimeMillis = maxTransferRetryMillis;
    }

    /**
     * Sets the maximum time to retry operations in milliseconds.
     *
     * @param maxTransferRetryMillis The maximum time.
     */
    public void setMaxOperationRetryTimeMillis(long maxTransferRetryMillis) {
        this.maxOperationRetryTimeMillis = maxTransferRetryMillis;
    }

    /**
     * Sets the maximum time to retry uploads in milliseconds.
     *
     * @param maxTransferRetryMillis The maximum time.
     */
    public void setMaxUploadRetryTimeMillis(long maxTransferRetryMillis) {
        this.maxUploadRetryTimeMillis = maxTransferRetryMillis;
    }

}
