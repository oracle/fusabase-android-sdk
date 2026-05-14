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

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.FusabaseOptions;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a cloud storage instance, providing access to BAAS Storage functionality.
 *
 * This class serves as the entry point for interacting with Oracle's cloud storage service.
 * It manages storage operations such as uploading, downloading, and managing files in
 * cloud storage buckets. Each Storage instance is associated with a specific FusabaseApp
 * and provides methods to create references to storage locations.
 *
 * <p>Storage instances are typically obtained through factory methods that ensure
 * proper initialization and singleton behavior per app instance.</p>
 */
public class Storage {

    /** The FusabaseApp instance associated with this storage. */
    private final FusabaseApp app;
    /** Singleton instance for default storage access. */
    private static Storage instance;

    /** Map holding all Storage instances keyed by app name. */
    private static final Map<String, Storage> INSTANCES = new HashMap<>();

    /** Configuration options for this storage instance. */
    private final FusabaseOptions options;
    /** Name of the storage bucket associated with this instance. */
    private final String bucketName;

    /**
     * Constructs a new storage instance.
     *
     * @param app The FusabaseApp associated with this storage instance.
     */
    protected Storage(@NonNull FusabaseApp app) {
        this.app = app;
        this.options = app.getOptions();
        this.bucketName = app.fusabaseOptions.getStorageBucket();
        instance = this;
        INSTANCES.put(app.name, this);
    }

    /**
     * Gets the name of the bucket associated with this storage instance.
     *
     * @return The bucket name.
     */
    @NonNull
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Method to get the app associated with this storage instance.
     *
     * @return FusabaseApp
     */
    @NonNull
    public FusabaseApp getApp() {
        return this.app;
    }

    /**
     * Method to get the instance of Storage of default fusabase app.
     *
     * @return Storage
     */
    public static Storage getInstance() throws FusabaseException {
        return getInstance(FusabaseApp.getInstance());
    }

    /**
     * Method to get the storage instance associated with provided app.
     *
     * @param app FusabaseApp
     * @return Storage
     */
    public static Storage getInstance(@NonNull FusabaseApp app) {
        if (INSTANCES.get(app.getName()) != null)
            return INSTANCES.get(app.getName());

        INSTANCES.put(app.getName(), new Storage(app));

        return getInstance(app);
    }

    @NonNull
    public StorageReference getReferenceFromUrl(@NonNull String url)
            throws IllegalArgumentException {
        int index = url.indexOf("/o/");
        if (index == -1) {
            throw new IllegalArgumentException("Invalid URL format: missing /o/ path segment");
        }
        String encodedPath = url.substring(index + 3);
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL encoding");
        }
        return new StorageReference(this, decodedPath);
    }
    /**
     * Creates a new StorageReference initialized at the root Fusabase Storage location.
     *
     * @return The StorageReference instance.
     */
    @NonNull
    public StorageReference getReference() {
        return new StorageReference(this, "");
    }

    /**
     * Creates a new StorageReference initialized with a child Fusabase Storage location.
     *
     * @param location The location.
     * @return The StorageReference instance.
     */
    @NonNull
    public StorageReference getReference(@NonNull String location) {
        return new StorageReference(this, location);
    }

}
