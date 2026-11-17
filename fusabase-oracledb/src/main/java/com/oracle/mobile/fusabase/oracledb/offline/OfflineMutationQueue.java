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

import java.util.ArrayList;
import java.util.List;

/** Durable, user-scoped mutation queue and document-to-batch index. */
final class OfflineMutationQueue {
    void add(@NonNull SQLiteDatabase db, @NonNull OfflineMutationBatch batch) {
        ContentValues values = new ContentValues();
        values.put("user_id", batch.getUserId()); values.put("batch_id", batch.getBatchId());
        values.put("local_write_time_ms", batch.getLocalWriteTimeMillis()); values.put("source", batch.getSource());
        values.put("mutations_json", OfflineValueSerializer.encode(batch.mutationsForStorage()));
        values.put("base_documents_json", OfflineValueSerializer.encode(new ArrayList<>()));
        db.insertOrThrow(OfflineDatabase.TABLE_MUTATIONS, null, values);
        for (String path : batch.getAffectedPaths()) {
            ContentValues index = new ContentValues(); index.put("user_id", batch.getUserId());
            index.put("document_path", path); index.put("batch_id", batch.getBatchId());
            db.insertOrThrow(OfflineDatabase.TABLE_DOCUMENT_MUTATIONS, null, index);
        }
    }

    @NonNull List<OfflineMutationBatch> getAll(@NonNull SQLiteDatabase db, @NonNull String userId) {
        return read(db, "user_id = ?", new String[]{userId});
    }

    @NonNull List<OfflineMutationBatch> getForDocument(@NonNull SQLiteDatabase db, @NonNull String userId,
                                                         @NonNull String path) {
        return read(db, "user_id = ? AND batch_id IN (SELECT batch_id FROM "
                + OfflineDatabase.TABLE_DOCUMENT_MUTATIONS + " WHERE user_id = ? AND document_path = ?)",
                new String[]{userId, userId, path});
    }

    void remove(@NonNull SQLiteDatabase db, @NonNull String userId, long batchId) {
        db.delete(OfflineDatabase.TABLE_MUTATIONS, "user_id = ? AND batch_id = ?",
                new String[]{userId, Long.toString(batchId)});
    }

    @NonNull @SuppressWarnings("unchecked")
    private List<OfflineMutationBatch> read(SQLiteDatabase db, String selection, String[] args) {
        List<OfflineMutationBatch> result = new ArrayList<>();
        try (Cursor cursor = db.query(OfflineDatabase.TABLE_MUTATIONS, null, selection, args,
                null, null, "batch_id ASC")) {
            while (cursor.moveToNext()) {
                Object stored = OfflineValueSerializer.decode(cursor.getString(cursor.getColumnIndexOrThrow("mutations_json")));
                result.add(OfflineMutationBatch.fromStorage(cursor.getString(cursor.getColumnIndexOrThrow("user_id")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("batch_id")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("local_write_time_ms")),
                        cursor.getString(cursor.getColumnIndexOrThrow("source")), stored));
            }
        }
        return result;
    }
}
