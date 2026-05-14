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
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.task.Task;

import java.util.List;

import jakarta.json.JsonObject;

/**
 * Abstract base class for database references in the Oracle Database SDK.
 *
 * <p>A Reference represents a pointer to a location in the database, such as a document
 * or collection. This abstract class provides common functionality for all reference types,
 * including path management, data retrieval, and real-time updates.</p>
 *
 * <p>Concrete implementations include:</p>
 * <ul>
 *   <li>{@link DocumentReference} - References to individual documents</li>
 *   <li>{@link CollectionReference} - References to document collections</li>
 *   <li>{@link DualityViewDocReference} - References to duality view documents</li>
 *   <li>{@link DualityViewColReference} - References to duality view collections</li>
 * </ul>
 *
 * <p>This class provides methods for:</p>
 * <ul>
 *   <li>Retrieving data from the database</li>
 *   <li>Processing real-time update notifications</li>
 *   <li>Managing database paths and references</li>
 * </ul>
 */
abstract class Reference {

    /** The Oracle Database instance associated with this reference */
    public final FusabaseOracledb oradb;

    /** Helper class for performing HTTP requests and data operations */
    final QueryHelper queryHelper;

    /**
     * Constructs a new Reference with the specified Oracle Database instance.
     * Initializes the query helper for database operations.
     *
     * @param oradb The Oracle Database instance to associate with this reference
     */
    Reference(@NonNull FusabaseOracledb oradb) {
        this.oradb = oradb;
        FusabaseOracledbSettings settings = oradb.getOracledbSettings();
        this.queryHelper = new QueryHelper(settings,
                settings.getOptions(),
                oradb);
    }

    /**
     * Returns the Oracle Database instance associated with this reference.
     *
     * @return The FusabaseOracledb instance
     */
    @NonNull
    public FusabaseOracledb getOradb() {
        return this.oradb;
    }

    /**
     * Returns the full path to this reference in the database.
     * The path format depends on the concrete implementation (document vs collection).
     *
     * @return The database path as a string
     */
    @NonNull
    abstract public String getPath();

    /**
     * Returns the path segments that make up this reference.
     * Path segments represent the hierarchical structure in the database.
     *
     * @return A list of path segments
     */
    @NonNull
    abstract protected List<String> getPathSegments();

    /**
     * Retrieves data from the database at this reference location.
     * Uses the specified source to determine whether to fetch from cache or server.
     *
     * @param source The source to retrieve data from (CACHE or SERVER)
     * @return A Task containing the resulting Snapshot
     */
    @NonNull
    abstract public Task<? extends Snapshot> get(@NonNull Source source);

    /**
     * Retrieves data from the database with additional snapshot control.
     * The snapshot parameter controls whether to create a new snapshot or reuse existing data.
     *
     * @param source The source to retrieve data from (CACHE or SERVER)
     * @param snapshot Whether to create a new snapshot (true) or allow caching (false)
     * @return A Task containing the resulting Snapshot
     */
    @NonNull
    abstract protected Task<? extends Snapshot> get(@NonNull Source source, boolean snapshot);

    /**
     * Processes real-time notification data to create or update a snapshot.
     * This method handles WebSocket notifications that contain document changes.
     *
     * @param document The notification data containing document changes
     * @param currentSnap The current snapshot to update, or null if creating new
     * @return An updated Snapshot reflecting the notification changes, or null if processing failed
     * @throws FusabaseOracledbException if the notification data is invalid or processing fails
     */
    @Nullable
    abstract protected Snapshot getSnapshotFromNotificationData(@NonNull JsonObject document,
                                                             @NonNull Snapshot currentSnap) throws FusabaseOracledbException;
}
