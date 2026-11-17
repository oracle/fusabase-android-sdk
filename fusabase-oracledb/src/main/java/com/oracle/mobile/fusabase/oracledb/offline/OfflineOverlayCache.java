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

/** Stores the materialized local document view for pending mutations. */
final class OfflineOverlayCache {
    static final class Overlay {
        final String userId; final String path; final long largestBatchId;
        final Map<String, Object> data; final OfflineCachedDocument.State state;

        Overlay(@NonNull String userId, @NonNull String path, long largestBatchId,
                @Nullable Map<String, Object> data, @NonNull OfflineCachedDocument.State state) {
            this.userId = userId; this.path = path; this.largestBatchId = largestBatchId;
            this.data = data; this.state = state;
        }
    }

    void set(@NonNull SQLiteDatabase db, @NonNull Overlay overlay) {
        ContentValues values = new ContentValues();
        values.put("user_id", overlay.userId); values.put("document_path", overlay.path);
        values.put("largest_batch_id", overlay.largestBatchId);
        if (overlay.data == null) values.putNull("document_data");
        else values.put("document_data", OfflineValueSerializer.encode(overlay.data));
        values.put("document_state", overlay.state.getStorageValue());
        db.insertWithOnConflict(OfflineDatabase.TABLE_DOCUMENT_OVERLAYS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Nullable @SuppressWarnings("unchecked")
    Overlay get(@NonNull SQLiteDatabase db, @NonNull String userId, @NonNull String path) {
        try (Cursor cursor = db.query(OfflineDatabase.TABLE_DOCUMENT_OVERLAYS, null,
                "user_id = ? AND document_path = ?", new String[]{userId, path}, null, null, null)) {
            if (!cursor.moveToFirst()) return null;
            String json = cursor.getString(cursor.getColumnIndexOrThrow("document_data"));
            Map<String, Object> data = json == null ? null : (Map<String, Object>) OfflineValueSerializer.decode(json);
            return new Overlay(userId, path, cursor.getLong(cursor.getColumnIndexOrThrow("largest_batch_id")),
                    data, OfflineCachedDocument.State.fromStorageValue(
                    cursor.getInt(cursor.getColumnIndexOrThrow("document_state"))));
        }
    }

    @NonNull
    @SuppressWarnings("unchecked")
    List<Overlay> getAllWithPrefix(@NonNull SQLiteDatabase db, @NonNull String userId,
                                   @NonNull String pathPrefix) {
        String escapedPrefix = pathPrefix.replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_");
        try (Cursor cursor = db.query(OfflineDatabase.TABLE_DOCUMENT_OVERLAYS, null,
                "user_id = ? AND document_path LIKE ? ESCAPE '\\'",
                new String[]{userId, escapedPrefix + "%"}, null, null, "document_path")) {
            List<Overlay> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                String json = cursor.getString(cursor.getColumnIndexOrThrow("document_data"));
                Map<String, Object> data = json == null ? null :
                        (Map<String, Object>) OfflineValueSerializer.decode(json);
                result.add(new Overlay(userId,
                        cursor.getString(cursor.getColumnIndexOrThrow("document_path")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("largest_batch_id")),
                        data,
                        OfflineCachedDocument.State.fromStorageValue(
                                cursor.getInt(cursor.getColumnIndexOrThrow("document_state")))));
            }
            return result;
        }
    }

    void remove(@NonNull SQLiteDatabase db, @NonNull String userId, @NonNull String path) {
        db.delete(OfflineDatabase.TABLE_DOCUMENT_OVERLAYS, "user_id = ? AND document_path = ?",
                new String[]{userId, path});
    }
}
