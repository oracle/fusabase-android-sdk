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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** SQLite-backed cache of the latest server state for each locally known document. */
final class OfflineRemoteDocumentCache {

    private static final String COLUMN_PATH = "document_path";
    private static final String COLUMN_DATA = "document_data";
    private static final String COLUMN_VERSION = "document_version";
    private static final String COLUMN_UPDATE_TIME = "update_time_ms";
    private static final String COLUMN_COMMIT_TIME = "commit_time_ms";
    private static final String COLUMN_READ_TIME = "read_time_ms";
    private static final String COLUMN_STATE = "document_state";
    private static final String COLUMN_HAS_LOCAL_MUTATIONS = "has_local_mutations";
    private static final String COLUMN_HAS_COMMITTED_MUTATIONS = "has_committed_mutations";
    private static final String COLUMN_SEQUENCE_NUMBER = "sequence_number";

    private final OfflineDatabase database;

    OfflineRemoteDocumentCache(@NonNull OfflineDatabase database) {
        this.database = Objects.requireNonNull(database, "Offline database cannot be null");
    }

    void add(@NonNull SQLiteDatabase transaction, @NonNull OfflineCachedDocument document) {
        transaction.insertWithOnConflict(OfflineDatabase.TABLE_REMOTE_DOCUMENTS, null,
                toContentValues(document), SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Nullable
    OfflineCachedDocument get(@NonNull SQLiteDatabase transaction, @NonNull String path) {
        try (Cursor cursor = transaction.query(OfflineDatabase.TABLE_REMOTE_DOCUMENTS, null,
                COLUMN_PATH + " = ?", new String[]{path}, null, null, null)) {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        }
    }

    @NonNull
    List<OfflineCachedDocument> getAllWithPrefix(@NonNull SQLiteDatabase transaction,
                                                  @NonNull String pathPrefix) {
        String escapedPrefix = pathPrefix.replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_");
        try (Cursor cursor = transaction.query(OfflineDatabase.TABLE_REMOTE_DOCUMENTS, null,
                COLUMN_PATH + " LIKE ? ESCAPE '\\'", new String[]{escapedPrefix + "%"},
                null, null, COLUMN_PATH)) {
            List<OfflineCachedDocument> documents = new ArrayList<>();
            while (cursor.moveToNext()) {
                documents.add(fromCursor(cursor));
            }
            return documents;
        }
    }

    void remove(@NonNull SQLiteDatabase transaction, @NonNull String path) {
        transaction.delete(OfflineDatabase.TABLE_REMOTE_DOCUMENTS, COLUMN_PATH + " = ?",
                new String[]{path});
    }

    void pruneToSize(@NonNull SQLiteDatabase transaction, long maxBytes) {
        if (maxBytes < 0) {
            return;
        }
        long currentBytes = getApproximateSizeBytes(transaction);
        if (currentBytes <= maxBytes) {
            return;
        }

        try (Cursor cursor = transaction.rawQuery("SELECT " + COLUMN_PATH + ", "
                        + "COALESCE(LENGTH(" + COLUMN_DATA + "), 0) FROM "
                        + OfflineDatabase.TABLE_REMOTE_DOCUMENTS + " WHERE NOT EXISTS (SELECT 1 FROM "
                        + OfflineDatabase.TABLE_DOCUMENT_OVERLAYS + " WHERE "
                        + OfflineDatabase.TABLE_DOCUMENT_OVERLAYS + ".document_path = "
                        + OfflineDatabase.TABLE_REMOTE_DOCUMENTS + "." + COLUMN_PATH + ") ORDER BY "
                        + COLUMN_SEQUENCE_NUMBER,
                null)) {
            while (currentBytes > maxBytes && cursor.moveToNext()) {
                String path = cursor.getString(0);
                long documentBytes = cursor.getLong(1);
                remove(transaction, path);
                currentBytes -= documentBytes;
            }
        }
    }

    private long getApproximateSizeBytes(@NonNull SQLiteDatabase transaction) {
        try (Cursor cursor = transaction.rawQuery("SELECT COALESCE(SUM("
                + "LENGTH(" + COLUMN_PATH + ") + "
                + "LENGTH(" + COLUMN_VERSION + ") + "
                + "COALESCE(LENGTH(" + COLUMN_DATA + "), 0)), 0) FROM "
                + OfflineDatabase.TABLE_REMOTE_DOCUMENTS, null)) {
            cursor.moveToFirst();
            return cursor.getLong(0);
        }
    }

    private ContentValues toContentValues(@NonNull OfflineCachedDocument document) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PATH, document.getPath());
        if (document.getData() == null) {
            values.putNull(COLUMN_DATA);
        } else {
            values.put(COLUMN_DATA, OfflineValueSerializer.encode(document.getData()));
        }
        values.put(COLUMN_VERSION, document.getVersion());
        putNullableLong(values, COLUMN_UPDATE_TIME, document.getUpdateTimeMillis());
        putNullableLong(values, COLUMN_COMMIT_TIME, document.getCommitTimeMillis());
        putNullableLong(values, COLUMN_READ_TIME, document.getReadTimeMillis());
        values.put(COLUMN_STATE, document.getState().getStorageValue());
        values.put(COLUMN_HAS_LOCAL_MUTATIONS, document.hasLocalMutations() ? 1 : 0);
        values.put(COLUMN_HAS_COMMITTED_MUTATIONS, document.hasCommittedMutations() ? 1 : 0);
        values.put(COLUMN_SEQUENCE_NUMBER, document.getSequenceNumber());
        return values;
    }

    private void putNullableLong(@NonNull ContentValues values, @NonNull String key,
                                 @Nullable Long value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private OfflineCachedDocument fromCursor(@NonNull Cursor cursor) {
        String data = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATA));
        Map<String, Object> decodedData = null;
        if (data != null) {
            Object decoded = OfflineValueSerializer.decode(data);
            if (!(decoded instanceof Map)) {
                throw new IllegalStateException("Cached document data must be an object");
            }
            decodedData = (Map<String, Object>) decoded;
        }
        return new OfflineCachedDocument(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATH)),
                decodedData,
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VERSION)),
                getNullableLong(cursor, COLUMN_UPDATE_TIME),
                getNullableLong(cursor, COLUMN_COMMIT_TIME),
                getNullableLong(cursor, COLUMN_READ_TIME),
                OfflineCachedDocument.State.fromStorageValue(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STATE))),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HAS_LOCAL_MUTATIONS)) != 0,
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HAS_COMMITTED_MUTATIONS)) != 0,
                cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SEQUENCE_NUMBER)));
    }

    @Nullable
    private Long getNullableLong(@NonNull Cursor cursor, @NonNull String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getLong(index);
    }
}
