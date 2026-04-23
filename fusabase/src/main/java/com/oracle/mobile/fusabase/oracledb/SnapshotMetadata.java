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

/**
 * Contains metadata about a snapshot, including information about whether
 * the snapshot was served from cache and whether there are pending writes.
 */
public class SnapshotMetadata {
    /** Indicates whether this snapshot was served from cache. */
    public final boolean isFromCache;

    /** Indicates whether there are pending writes for this snapshot. */
    private final boolean hasPendingWrites;

    /**
     * Private constructor for creating SnapshotMetadata instances.
     *
     * @param hasPendingWrites true if there are pending writes
     * @param isFromCache true if the snapshot was served from cache
     */
    SnapshotMetadata(boolean hasPendingWrites, boolean isFromCache) {
        this.hasPendingWrites = hasPendingWrites;
        this.isFromCache = isFromCache;
    }

    /**
     * Compares this SnapshotMetadata with another object for equality.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    public boolean equals(Object obj)
    {
        if(obj instanceof SnapshotMetadata)
        {
            SnapshotMetadata metadata = (SnapshotMetadata) obj;
            return metadata.isFromCache == this.isFromCache
                    && metadata.hasPendingWrites == this.hasPendingWrites;
        }
        return  false;
    }

    /**
     * Returns a hash code value for this SnapshotMetadata.
     * @return int hash code value.
     */
    public int hashCode() {
        return java.util.Objects.hash(isFromCache, hasPendingWrites);
    }

    /**
     * Returns whether there are pending writes for this snapshot.
     *
     * @return true if there are pending writes, false otherwise
     */
    public boolean hasPendingWrites()
    {
        return hasPendingWrites;
    }

    /**
     * Returns whether this snapshot was served from cache.
     *
     * @return true if the snapshot was served from cache, false otherwise
     */
    public boolean isFromCache()
    {
        return isFromCache;
    }
}
