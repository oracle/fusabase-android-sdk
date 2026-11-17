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

import com.oracle.mobile.fusabase.task.OnCompleteListener;
import com.oracle.mobile.fusabase.task.Task;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *  Dispatches local offline view changes to active snapshot listeners. 
 *
 * <p>Example:</p>
 * <pre>{@code
 * FusabaseOracledb db = FusabaseOracledb.getInstance();
 * }</pre>
 */
public final class OfflineEventBus {
    private static final CopyOnWriteArrayList<ListenerData<? extends Reference, ? extends Snapshot>>
            LISTENERS = new CopyOnWriteArrayList<>();

    private OfflineEventBus() { }

    static void register(@NonNull ListenerData<? extends Reference, ? extends Snapshot> listenerData) {
        LISTENERS.addIfAbsent(listenerData);
    }

    static void unregister(@NonNull ListenerData<? extends Reference, ? extends Snapshot> listenerData) {
        LISTENERS.remove(listenerData);
    }

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * FusabaseOracledb db = FusabaseOracledb.getInstance();
     * }</pre>
     */public static void notifyLocalChanges(@NonNull FusabaseOracledb db,
                                          @NonNull List<String> affectedPaths) {
        for (ListenerData<? extends Reference, ? extends Snapshot> listenerData : LISTENERS) {
            Reference reference = listenerData.getReference();
            if (reference.getOradb() != db || !mightObserve(reference, affectedPaths)) {
                continue;
            }
            reference.get(Source.CACHE).addOnCompleteListener(new OnCompleteListener<Snapshot>() {
                @Override
                public void onComplete(Task<Snapshot> task) {
                    if (task.isSuccessful()) {
                        listenerData.executeListener(task.getResult(), null);
                    }
                }
            });
        }
    }

    private static boolean mightObserve(@NonNull Reference reference,
                                        @NonNull List<String> affectedPaths) {
        String referencePath = reference.getPath();
        if (reference instanceof DocumentReference) {
            return affectedPaths.contains(referencePath);
        }
        String collectionPrefix = referencePath.isEmpty() ? "" : referencePath + "/";
        for (String affectedPath : affectedPaths) {
            if (collectionPrefix.isEmpty() || affectedPath.startsWith(collectionPrefix)) {
                return true;
            }
        }
        return false;
    }
}
