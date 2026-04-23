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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Class ListResult
 * A class representing the result of the list methods call.
 */
public class ListResult {

    /**
     * A list of StorageReference objects representing the items under the referenced path.
     */
    public final List<StorageReference> items;
    /**
     * A String representing the next page token for pagination.
     */
    public final String nextPageToken;
    /**
     *  A list of StorageReference objects representing the prefixes or subfolders under the referenced path.
     */
    public final List<StorageReference> prefixes;

    /**
     * Class Constructor
     *
     * @param storageInstance an {@code Storage} The storage instance associated with the result
     * @param result          an {@code HashMap<String, Object>} Response of the list method calls
     */
    protected ListResult(@NonNull Storage storageInstance, @NonNull HashMap<String, Object> result) {

        this.items = new ArrayList<StorageReference>();
        this.prefixes = new ArrayList<StorageReference>();

        for (HashMap<String, String> item : (List<HashMap<String, String>>) Objects.requireNonNull(result.get("items"))) {
            this.items.add(new StorageReference(storageInstance, Objects.requireNonNull(item.get("name"))));
        }

        for (HashMap<String, String> prefix : (List<HashMap<String, String>>) Objects.requireNonNull(result.get("prefixes"))) {
            this.prefixes.add(new StorageReference(storageInstance, Objects.requireNonNull(prefix.get("name"))));
        }

        this.nextPageToken = String.valueOf(result.get("nextPageToken"));
    }

    /**
     * Method to return the items or the files under the referenced path.
     *
     * @return List<StorageReference>
     */
    @NonNull
    public List<StorageReference> getItems() {
        return this.items;
    }

    /**
     * Method to get the pageToken of the listResult. It is used for paging the
     * result of the list method.
     *
     * @return String
     */
    public String getPageToken() {
        return this.nextPageToken;
    }

    /**
     * Method to return the prefixes or the subfolders under the referenced path.
     *
     * @return List<StorageReference>
     */
    @NonNull
    public List<StorageReference> getPrefixes() {
        return this.prefixes;
    }
}

