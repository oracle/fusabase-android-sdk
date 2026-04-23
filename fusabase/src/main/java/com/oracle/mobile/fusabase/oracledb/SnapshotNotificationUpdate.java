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
 * Represents a single update within a snapshot notification.
 * Contains information about the specific row that was updated and the operation performed.
 */
public class SnapshotNotificationUpdate {

   /** The ID of the row that was updated. */
   private final String rowId;

   /** The operation that was performed (e.g., INSERT, UPDATE, DELETE). */
   private final String operation;

   /**
    * Constructor for creating SnapshotNotificationUpdate instances.
    *
    * @param rowId the ID of the updated row
    * @param operation the operation that was performed
    */
   public SnapshotNotificationUpdate(String rowId, String operation) {
        this.rowId = rowId;
        this.operation = operation;
    }

   /**
    * Returns the operation that was performed.
    *
    * @return the operation type
    */
   public String getOperation() {
        return operation;
    }

    /**
     * Returns the ID of the row that was updated.
     *
     * @return the row ID
     */
    public String getRowId() {
        return rowId;
    }
}
