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
 * Abstract base class for all database snapshots in the Oracle Database SDK.
 *
 * <p>A Snapshot represents a point-in-time view of data retrieved from the database.
 * Snapshots provide a consistent view of the database state and are used to access
 * document data, query results, and metadata about the retrieval operation.</p>
 *
 * <p>This abstract class defines the contract that all snapshot implementations must follow,
 * ensuring consistent behavior across different types of database snapshots.</p>
 *
 * <p>Concrete implementations include:</p>
 * <ul>
 *   <li>{@link DocumentSnapshot} - Represents a single document's state</li>
 *   <li>{@link QuerySnapshot} - Represents the results of a query operation</li>
 *   <li>{@link AggregateQuerySnapshot} - Represents aggregated query results</li>
 * </ul>
 */
public abstract class Snapshot {

    /**
     * Compares this snapshot with another object for equality.
     * Implementations should define what constitutes equality for their specific snapshot type.
     *
     * @param obj the object to compare with this snapshot
     * @return true if the objects are considered equal, false otherwise
     */
    abstract public boolean equals(Object obj);

    /**
     * Returns a hash code value for this snapshot.
     * Implementations should ensure that equal snapshots have equal hash codes.
     *
     * @return a hash code value for this snapshot
     */
    abstract public int hashCode();
}
