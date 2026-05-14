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

import java.util.Objects;

/**
 * Represents a collection at the Oracledb. This reference can be used to perform queries,
 * add documents, and create document references.
 */
public class CollectionReference extends Query {

    /**
     * The parent document reference, or null if this is a top-level collection.
     */
    private final DocumentReference parent;

    /**
     * The type of this collection.
     */
    private final Query.CollectionType type;

    /**
     * The ID of this collection.
     */
    private final String collectionId;

    /**
     * Creates an instance of CollectionReference.
     *
     * @param path the path to the collection
     * @param parent the parent document reference, or null
     * @param oradb the database instance
     */
    protected CollectionReference(@NonNull String path,
                                  @Nullable DocumentReference parent,
                                  @NonNull FusabaseOracledb oradb) {
        super(oradb, path, false, false);

        this.type = CollectionType.COLLECTION;

        String collectionId = this.path.remove(this.path.size() - 1);
        if (this.path.isEmpty()) {
            this.parent = null;
        } else {
            this.parent = parent == null ?
                    new DocumentReference(String.join("/", this.path),
                            null,
                            oradb,
                            true) :
                    parent;
        }
        this.collectionId = collectionId;
        this.path.add(collectionId);
    }

    /**
     * Creates a new document in the collection with an auto-generated ID and returns its reference.
     *
     * @return the document reference
     */
    @NonNull
    public DocumentReference document() {
        return new DocumentReference(String.join("/", path),
                this,
                oradb,
                false);
    }

    /**
     * Creates a reference to the document with the provided path.
     *
     * @param documentPath the path to the document
     * @return the document reference
     */
    @NonNull
    public DocumentReference document(@NonNull String documentPath) {
        Objects.requireNonNull(documentPath, "Invalid documentPath provided");

        if(documentPath.isEmpty())
            throw new IllegalArgumentException("Empty documentPath");

        return new DocumentReference(String.join("/", this.path) + "/" + documentPath,
                this,
                oradb,
                true);
    }

    /**
     * Adds a document to the collection referenced.
     *
     * @param data the payload to add
     * @return a task that resolves to the document reference of the added document
     */
    @NonNull
    public Task<DocumentReference> add(@NonNull Object data) {
        return this.document().add(data);
    }

    /**
     * Gets the ID of the collection referred to by this collection reference.
     *
     * @return the collection ID
     */
    @NonNull
    public String getId() {
        return collectionId;
    }

    /**
     * Gets the reference to the parent of the collection. The parent can be null if it is
     * the top-level collection or a DocumentReference.
     *
     * @return null or the DocumentReference of the parent
     */
    @Nullable
    public DocumentReference getParent() {
        return parent;
    }

    /**
     * Gets the path of the collection.
     *
     * @return the path as a string
     */
    @NonNull
    public String getPath() {
        return String.join("/", this.path);
    }
}
