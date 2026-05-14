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

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Represents a document entity in the Oracledb database.
 * A document encapsulates the data content, metadata, and location information for database operations.
 * Documents are the basic units of data storage and retrieval in the Oracledb system, containing
 * key-value pairs of data along with system-managed metadata such as timestamps and version information.
 */
public class Document {

    /**
     * The document data as key-value pairs.
     */
    private final HashMap<String, Object> data;

    /**
     * The timestamp when the document was created.
     */
    private final String created;

    /**
     * The timestamp when the document was last modified.
     */
    private final String lastModified;

    /**
     * The version identifier of the document.
     */
    private final String version;

    /**
     * The unique object identifier for the document.
     */
    private final String objectId;

    /**
     * The path segments representing the document's location in the database.
     */
    private final List<String> path;

    /**
     * The index position of the document in query results.
     */
    private final Long index;

    /**
     * The row identifier for database operations.
     */
    private final String rowId;

    /**
     * The asof timestamp for change detection in relational views.
     */
    private final String asof;

    /**
     * Constructs a Document instance.
     *
     * @param data         the document data as key-value pairs
     * @param objectId     the unique object identifier
     * @param created      the creation timestamp
     * @param lastModified the last modification timestamp
     * @param version      the version identifier
     * @param asof         the asof timestamp for change detection
     * @param index        the index position in query results
     * @param path         the path segments to the document
     * @param rowId        the row identifier for database operations
     */
    protected Document(@Nullable HashMap<String, Object> data,
                       @NonNull String objectId,
                       @NonNull String created,
                       @NonNull String lastModified,
                       @NonNull String version,
                       @NonNull String asof,
                       @Nullable Long index,
                       @NonNull List<String> path,
                       @NonNull String rowId) {
        this.data = data;
        this.objectId = objectId;
        this.created = created;
        this.lastModified = lastModified;
        this.version = version;
        this.asof = asof;
        this.path = path;
        this.index = index;
        this.rowId = rowId;
    }

    /**
     * Gets a field value from the document using a field path.
     * Supports nested field access using dot notation.
     *
     * @param path the field path to access
     * @return the field value, or null if not found
     */
    @Nullable
    protected Object getField(@NonNull FieldPath path) {
        List<String> pathSegments = path.getSegments();
        HashMap<String, Object> innerHashMap = this.data;
        Object innerData = null;

        for (int i = 0; i < pathSegments.size(); i++) {
            if (innerHashMap == null) {
                throw new RuntimeException("Invalid path " + pathSegments.toString() + " provided for getting field.");
            }

            innerData = innerHashMap.get(pathSegments.get(i));

            if (innerData instanceof HashMap) {
                innerHashMap = (HashMap<String, Object>) innerData;
            }
        }
        return innerData;
    }

    /**
     * Gets the document data as a map of key-value pairs.
     *
     * @return the document data
     */
    @Nullable
    protected HashMap<String, Object> getData() {
        return data;
    }

//    /**
//     * Gets the path of the document as a string.
//     *
//     * @return the document path
//     */
//    @NonNull
//    protected String getPath() {
//        return String.join("/", path);
//    }

    /**
     * Gets the unique identifier of the document.
     *
     * @return the document ID
     */
    @NonNull
    protected String getId() {
        return objectId;
    }

    /**
     * Gets the version identifier of the document.
     *
     * @return the document version
     */
    @NonNull
    protected String getVersion() {
        return version;
    }

    /**
     * Gets the creation timestamp of the document.
     *
     * @return the creation timestamp
     */
    @NonNull
    protected String getCreated() {
        return created;
    }

    /**
     * Gets the last modification timestamp of the document.
     *
     * @return the last modification timestamp
     */
    @NonNull
    protected String getLastModified() {
        return lastModified;
    }

    /**
     * Gets the row identifier for database operations.
     *
     * @return the row ID
     */
    @NonNull
    protected String getRowId() {
        return this.rowId;
    }

    /**
     * Gets the index position of the document in query results.
     *
     * @return the document index
     */
    @Nullable
    protected Long getIndex() {
        return index;
    }

    /**
     * Gets the asof timestamp for change detection.
     *
     * @return the asof timestamp
     */
    @NonNull
    protected String getAsof() {
        return asof;
    }

    /**
     * Compares this document with another object for equality.
     * Two documents are considered equal if all their properties match.
     * Null-safe comparison is used for all nullable fields.
     *
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Document document = (Document) o;

        if (!Objects.equals(data, document.data)) {
            return false;
        }
        if (!Objects.equals(created, document.created)) {
            return false;
        }
        if (!Objects.equals(lastModified, document.lastModified)) {
            return false;
        }
        if (!Objects.equals(version, document.version)) {
            return false;
        }
        if (!Objects.equals(objectId, document.objectId)) {
            return false;
        }
        if (!Objects.equals(path, document.path)) {
            return false;
        }
        if (!Objects.equals(index, document.index)) {
            return false;
        }
        if (!Objects.equals(asof, document.asof)) {
            return false;
        }
        return Objects.equals(rowId, document.rowId);
    }

    /**
     * Returns a hash code value for this document.
     * The hash code is computed based on all document properties.
     *
     * @return a hash code value for this document
     */
    @Override
    public int hashCode() {
        return Objects.hash(data, created, lastModified, version, objectId, path, index, asof, rowId);
    }

    /**
     * Returns a string representation of this document.
     * Includes key properties for debugging and logging purposes.
     *
     * @return a string representation of this document
     */
    @NonNull
    @Override
    public String toString() {
        return "Document{" +
                "objectId='" + objectId + '\'' +
                ", version='" + version + '\'' +
                ", created='" + created + '\'' +
                ", lastModified='" + lastModified + '\'' +
                ", path=" + path +
                ", index=" + index +
                ", data=" + (data != null ? data.size() + " fields" : "null") +
                '}';
    }
}
