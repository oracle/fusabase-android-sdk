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

/**
 * Represents a change to a document within a query result set.
 * Used to track additions, modifications, and removals of documents in query snapshots,
 * providing information about the document's position changes and the type of change.
 */
public class DocumentChange {

    /**
     * Enumeration defining the types of changes that can occur to documents in a query.
     */
    public enum Type {
        /**
         * Indicates a new document has been added to the query results.
         */
        ADDED,
        /**
         * Indicates an existing document in the query results has been modified.
         */
        MODIFIED,
        /**
         * Indicates a document has been removed from the query results.
         */
        REMOVED
    }

    /**
     * The document snapshot associated with this change.
     */
    public final QueryDocumentSnapshot document;

    /**
     * The new index position of the document in the query results.
     */
    public final int newIndex;

    /**
     * The old index position of the document in the query results.
     * For added documents, this is typically -1.
     */
    public final int oldIndex;

    /**
     * The type of change that occurred (ADDED, MODIFIED, or REMOVED).
     */
    public final DocumentChange.Type type;

    /**
     * Creates a DocumentChange instance representing a change to a document in query results.
     *
     * @param doc      the document snapshot associated with this change
     * @param newIndex the new index position of the document in the query results
     * @param oldIndex the old index position of the document in the query results (-1 for added documents)
     * @param type     the type of change (ADDED, MODIFIED, or REMOVED)
     */
    protected DocumentChange(@NonNull QueryDocumentSnapshot doc,
                             int newIndex,
                             int oldIndex,
                             @NonNull Type type) {
        this.document = doc;
        this.newIndex = newIndex;
        this.oldIndex = oldIndex;
        this.type = type;
    }

    /**
     * Compares this DocumentChange with another object for equality.
     * Two DocumentChange objects are equal if they have the same document, indices, and type.
     *
     * @param obj the object to compare with this DocumentChange
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj != null && obj instanceof DocumentChange) {
            DocumentChange other = (DocumentChange) obj;
            return other.type == this.type &&
                    other.oldIndex == this.oldIndex &&
                    other.newIndex == this.newIndex &&
                    Objects.equals(other.document, this.document);
        }
        return false;
    }

    /**
     * Returns the document snapshot associated with this change.
     *
     * @return the document snapshot
     */
    @NonNull
    public QueryDocumentSnapshot getDocument() {
        return this.document;
    }

    /**
     * Returns the new index position of the document in the query results.
     *
     * @return the new index position
     */
    public int getNewIndex() {
        return this.newIndex;
    }

    /**
     * Returns the old index position of the document in the query results.
     * For added documents, this returns -1.
     *
     * @return the old index position
     */
    public int getOldIndex() {
        return this.oldIndex;
    }

    /**
     * Returns the type of change that occurred.
     *
     * @return the change type (ADDED, MODIFIED, or REMOVED)
     */
    @NonNull
    public DocumentChange.Type getType() {
        return this.type;
    }

    /**
     * Returns the hash code for this DocumentChange instance.
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(document, newIndex, oldIndex, type);
    }

    /**
     * Creates an updated instance of DocumentChange with a new document snapshot.
     * Note: This method currently ignores the updatedType parameter and uses the original type.
     *
     * @param updatedDoc   the updated document snapshot
     * @param updatedType  the updated change type (currently ignored)
     * @return a new DocumentChange instance
     */
    @NonNull
    protected DocumentChange updateDocumentChange(@NonNull QueryDocumentSnapshot updatedDoc,
                                                  @NonNull Type updatedType) {
        Objects.requireNonNull(updatedDoc, "Invalid updatedDoc provided");
        Objects.requireNonNull(updatedType, "Invalid updatedType provided");
        return new DocumentChange(updatedDoc, newIndex, oldIndex, type);
    }
}
