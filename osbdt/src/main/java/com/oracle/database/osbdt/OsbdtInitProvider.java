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

package com.oracle.database.osbdt;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.logger.OsbdtLogger;

import java.util.Objects;

/**
 * A ContentProvider that automatically initializes the default OsbdtApp instance when the application starts.
 * <p>
 * This provider is registered in the AndroidManifest.xml and ensures that Osbdt is set up automatically
 * during app launch, eliminating the need for manual initialization in most cases. It also enables
 * logging for the Osbdt SDK.
 * </p>
 * <p>
 * The initialization uses configuration values from the osbdt_config.json file that are automatically
 * merged into the app's resources during the build process.
 * </p>
 */
public class OsbdtInitProvider extends ContentProvider {

    /**
     * Tag used for logging purposes.
     */
    private static final String TAG = "OsbdtInitProvider";

    /**
     * Called when the provider is created. Performs automatic initialization of the Osbdt SDK.
     * <p>
     * This method enables logging, initializes the default OsbdtApp instance using configuration
     * from the osbdt_config.json file, and logs the result. If initialization fails, it logs
     * the error but allows the provider to continue.
     * </p>
     *
     * @return true to indicate successful provider creation.
     */
    @Override
    public boolean onCreate() {
        try{
            OsbdtLogger.ENABLED = true;
            OsbdtApp.initializeApp(Objects.requireNonNull(getContext()));
            OsbdtLogger.v(TAG, "Initialized default app for Osbdt.");
        } catch (Exception e)
        {
            OsbdtLogger.e(TAG, "Cannot automatically initialize Osbdt." + e.getMessage());
        }
        return true;
    }

    /**
     * Not supported. This ContentProvider is only used for initialization.
     *
     * @param uri           The URI to query.
     * @param projection    The list of columns to return.
     * @param selection     The selection criteria.
     * @param selectionArgs The selection arguments.
     * @param sortOrder     The sort order.
     * @return null as queries are not supported.
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    /**
     * Not supported. This ContentProvider is only used for initialization.
     *
     * @param uri The URI whose MIME type is requested.
     * @return null as MIME type resolution is not supported.
     */
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    /**
     * Not supported. This ContentProvider is only used for initialization.
     *
     * @param uri    The URI to insert into.
     * @param values The values to insert.
     * @return null as insertions are not supported.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    /**
     * Not supported. This ContentProvider is only used for initialization.
     *
     * @param uri           The URI to delete from.
     * @param selection     The selection criteria.
     * @param selectionArgs The selection arguments.
     * @return 0 as deletions are not supported.
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    /**
     * Not supported. This ContentProvider is only used for initialization.
     *
     * @param uri           The URI to update.
     * @param values        The values to update.
     * @param selection     The selection criteria.
     * @param selectionArgs The selection arguments.
     * @return 0 as updates are not supported.
     */
    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return 0;
    }
}
