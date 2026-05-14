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

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration options for setting data in a document or collection.
 * SetOptions determines whether to overwrite existing data completely or merge
 * specific fields with existing data.
 */
public class SetOptions {
    /** Indicates whether merge mode is enabled. */
    private final boolean merge;

    /** List of field paths to merge, or null to merge all fields. */
    private final List<String> mergeFields;

    /** Static instance for overwriting all existing data. */
    static final SetOptions OVERWRITE = new SetOptions(false, null);

    /** Static instance for merging all fields with existing data. */
    private static final SetOptions MERGE_ALL_FIELDS = new SetOptions(true, null);

    /**
     * Private constructor for creating SetOptions instances.
     *
     * @param merge true to enable merge mode, false for overwrite mode
     * @param mergeFields list of field paths to merge, or null to merge all fields
     */
    private SetOptions(boolean merge, @Nullable List<String> mergeFields) {
        this.merge = merge;
        this.mergeFields = mergeFields;
    }

    /**
     * Returns whether merge mode is enabled.
     *
     * @return true if merge mode is enabled, false for overwrite mode
     */
    public boolean isMerge() {
        return merge;
    }

    /**
     * Returns the list of field paths to merge.
     * Returns null if all fields should be merged or if merge mode is disabled.
     *
     * @return list of field paths, or null
     */
    public List<String> getMergeFields() {
        return mergeFields;
    }

    /**
     * Compares this SetOptions with another object for equality.
     *
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SetOptions that = (SetOptions) o;

        if (merge != that.merge) {
            return false;
        }
        return mergeFields != null ? mergeFields.equals(that.mergeFields) : that.mergeFields == null;
    }

    /**
     * Returns the hash code for this SetOptions.
     *
     * @return the hash code value
     */
    public int hashCode()
    {
        int result = (merge ? 1 : 0);
        result = 31 * result + (mergeFields != null ? mergeFields.hashCode() : 0);
        return result;
    }

    /**
     * Creates a SetOptions instance that merges all fields with existing data.
     *
     * @return a SetOptions instance configured for merging all fields
     */
    @NonNull
    public static SetOptions merge()
    {
        return MERGE_ALL_FIELDS;
    }

    /**
     * Creates a SetOptions instance that merges specific fields with existing data.
     *
     * @param fields list of FieldPath objects representing the fields to merge
     * @return a SetOptions instance configured for merging the specified fields
     */
    @NonNull
    public static SetOptions mergeFieldPaths(@NonNull List<FieldPath> fields)
    {
        List<String> fieldList = new ArrayList<>();

        for (FieldPath path : fields)
            fieldList.add(path.toString());

        return new SetOptions(true, fieldList);
    }

    /**
     * Creates a SetOptions instance that merges specific fields with existing data.
     *
     * @param fields array of field names to merge
     * @return a SetOptions instance configured for merging the specified fields
     */
    public static @NonNull SetOptions mergeFields(String[] fields)
    {
        return new SetOptions(true, Arrays.asList(fields));
    }

    /**
     * Creates a SetOptions instance that merges specific fields with existing data.
     *
     * @param fields list of field names to merge
     * @return a SetOptions instance configured for merging the specified fields
     */
    public static @NonNull SetOptions mergeFields(@NonNull List<String> fields)
    {
        return new SetOptions(true, fields);
    }
}
