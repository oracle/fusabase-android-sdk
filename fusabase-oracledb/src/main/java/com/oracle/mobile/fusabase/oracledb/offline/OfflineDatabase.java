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

package com.oracle.mobile.fusabase.oracledb.offline;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

/**
 * Owns the durable schema used by the OracleDB offline store.
 *
 * <p>All callers must use {@link #runTransaction(Transaction)} for changes that span more than
 * one table. The local store, rather than this class, owns document and mutation behavior.</p>
 */
final class OfflineDatabase extends SQLiteOpenHelper {

    static final int SCHEMA_VERSION = 1;

    static final String TABLE_REMOTE_DOCUMENTS = "remote_documents";
    static final String TABLE_MUTATIONS = "mutations";
    static final String TABLE_DOCUMENT_MUTATIONS = "document_mutations";
    static final String TABLE_DOCUMENT_OVERLAYS = "document_overlays";
    static final String TABLE_TARGETS = "targets";
    static final String TABLE_TARGET_DOCUMENTS = "target_documents";
    static final String TABLE_TARGET_GLOBALS = "target_globals";

    interface Transaction<T> {
        T run(@NonNull SQLiteDatabase database) throws Exception;
    }

    OfflineDatabase(@NonNull Context context, @NonNull String databaseName) {
        super(context.getApplicationContext(), databaseName, null, SCHEMA_VERSION);
    }

    @Override
    public void onConfigure(@NonNull SQLiteDatabase database) {
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase database) {
        createSchemaV1(database);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            createSchemaV1(database);
        }
    }

    @Override
    public void onDowngrade(@NonNull SQLiteDatabase database, int oldVersion, int newVersion) {
        throw new IllegalStateException("Offline persistence cannot downgrade schema from "
                + oldVersion + " to " + newVersion);
    }

    <T> T runTransaction(@NonNull Transaction<T> operation) throws Exception {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            T result = operation.run(database);
            database.setTransactionSuccessful();
            return result;
        } finally {
            database.endTransaction();
        }
    }

    private void createSchemaV1(@NonNull SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE_REMOTE_DOCUMENTS + " ("
                + "document_path TEXT PRIMARY KEY NOT NULL,"
                + "document_data TEXT,"
                + "document_version TEXT NOT NULL,"
                + "update_time_ms INTEGER,"
                + "commit_time_ms INTEGER,"
                + "read_time_ms INTEGER,"
                + "document_state INTEGER NOT NULL,"
                + "has_local_mutations INTEGER NOT NULL,"
                + "has_committed_mutations INTEGER NOT NULL,"
                + "sequence_number INTEGER NOT NULL)");

        database.execSQL("CREATE TABLE " + TABLE_MUTATIONS + " ("
                + "user_id TEXT NOT NULL,"
                + "batch_id INTEGER NOT NULL,"
                + "local_write_time_ms INTEGER NOT NULL,"
                + "source TEXT NOT NULL,"
                + "mutations_json TEXT NOT NULL,"
                + "base_documents_json TEXT NOT NULL,"
                + "PRIMARY KEY (user_id, batch_id))");

        database.execSQL("CREATE TABLE " + TABLE_DOCUMENT_MUTATIONS + " ("
                + "user_id TEXT NOT NULL,"
                + "document_path TEXT NOT NULL,"
                + "batch_id INTEGER NOT NULL,"
                + "PRIMARY KEY (user_id, document_path, batch_id),"
                + "FOREIGN KEY (user_id, batch_id) REFERENCES " + TABLE_MUTATIONS
                + " (user_id, batch_id) ON DELETE CASCADE)");
        database.execSQL("CREATE INDEX document_mutations_by_batch ON "
                + TABLE_DOCUMENT_MUTATIONS + " (user_id, batch_id)");

        database.execSQL("CREATE TABLE " + TABLE_DOCUMENT_OVERLAYS + " ("
                + "user_id TEXT NOT NULL,"
                + "document_path TEXT NOT NULL,"
                + "largest_batch_id INTEGER NOT NULL,"
                + "document_data TEXT,"
                + "document_state INTEGER NOT NULL,"
                + "PRIMARY KEY (user_id, document_path))");

        database.execSQL("CREATE TABLE " + TABLE_TARGETS + " ("
                + "target_id INTEGER PRIMARY KEY NOT NULL,"
                + "canonical_id TEXT NOT NULL UNIQUE,"
                + "target_type TEXT NOT NULL,"
                + "path TEXT NOT NULL,"
                + "query_json TEXT NOT NULL,"
                + "snapshot_version TEXT,"
                + "sequence_number INTEGER NOT NULL)");

        database.execSQL("CREATE TABLE " + TABLE_TARGET_DOCUMENTS + " ("
                + "target_id INTEGER NOT NULL,"
                + "document_path TEXT NOT NULL,"
                + "read_time_ms INTEGER,"
                + "PRIMARY KEY (target_id, document_path),"
                + "FOREIGN KEY (target_id) REFERENCES " + TABLE_TARGETS
                + " (target_id) ON DELETE CASCADE)");
        database.execSQL("CREATE INDEX target_documents_by_path ON "
                + TABLE_TARGET_DOCUMENTS + " (document_path)");

        database.execSQL("CREATE TABLE " + TABLE_TARGET_GLOBALS + " ("
                + "key TEXT PRIMARY KEY NOT NULL,"
                + "highest_target_id INTEGER NOT NULL,"
                + "last_remote_snapshot_version TEXT,"
                + "sequence_number INTEGER NOT NULL)");
    }
}
