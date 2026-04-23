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

import java.util.Objects;

import jakarta.json.JsonObject;

/**
 * Represents a single write operation within a batch or transaction.
 * Contains the operation type, target document, data, and options.
 */
class WriteOperation {

    /** The type of write operation to perform. */
    private final OperationType operation;

    /** Reference to the document being operated on. */
    private final DocumentReference docRef;

    /** Data to be written (for SET and UPDATE operations). */
    @Nullable
    private final Object data;

    @Nullable
    private final JsonObject params;

    @Nullable
    private final BulkUpdate bulkUpdate;

    /** Options for the write operation (for SET operations). */
    @Nullable
    private final SetOptions options;

    /** Document version for optimistic locking. */
    private final String version;

    /**
     * Enumeration of supported write operation types.
     */
    enum OperationType {
        /** Set operation - create or replace a document. */
        SET,
        /** Delete operation - remove a document. */
        DELETE,
        /** Update operation - modify fields in a document. */
        UPDATE,
        BULK_UPDATE
    }

    /**
     * Constructor for creating WriteOperation instances.
     *
     * @param operation the type of write operation
     * @param docRef reference to the target document
     * @param data the data to write (null for DELETE operations)
     * @param options write options (null for UPDATE and DELETE operations)
     * @param version document version for optimistic locking
     */
    WriteOperation(@NonNull OperationType operation,
                   DocumentReference docRef,
                   @Nullable Object data,
                   @Nullable SetOptions options,
                   String version,
                   @Nullable BulkUpdate bulkUpdate,
                   @Nullable JsonObject params)
    {
        Objects.requireNonNull(operation, "Operation cannot be null");
        Objects.requireNonNull(docRef, "DocumentReference cannot be null");
        this.operation = operation;
        this.docRef = docRef;
        this.data = data;
        this.options = options;
        this.version = version;
        this.params = params;
        this.bulkUpdate = bulkUpdate;
    }

    /**
     * Returns the type of write operation.
     *
     * @return the operation type
     */
    public OperationType getOperation() {
        return this.operation;
    }

    /**
     * Returns the document reference for this operation.
     *
     * @return the document reference
     */
    public DocumentReference getDocRef() {
        return this.docRef;
    }

    /**
     * Returns the data to be written.
     *
     * @return the write data, or null for DELETE operations
     */
    @Nullable
    public Object getData() {
        return this.data;
    }

    /**
     * Returns the set options for this operation.
     *
     * @return the set options, or null for UPDATE and DELETE operations
     */
    @Nullable
    public SetOptions getOptions() {
        return this.options;
    }

    /**
     * Returns the document version for optimistic locking.
     *
     * @return the document version, or null if not applicable
     */
    @Nullable
    public String getVersion() {
        return this.version;
    }

    @Nullable
    public JsonObject getParams() { return this.params;}

    @Nullable
    public BulkUpdate getBulkUpdate() { return this.bulkUpdate;}
}
