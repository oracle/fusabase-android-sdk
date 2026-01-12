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

package com.oracle.test.oracledb;

import static org.junit.Assert.*;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;
import com.oracle.database.osbdt.oracledb.CollectionReference;
import com.oracle.database.osbdt.oracledb.DocumentReference;
import com.oracle.database.osbdt.oracledb.DocumentSnapshot;
import com.oracle.database.osbdt.oracledb.OsbdtOracledb;
import com.oracle.database.osbdt.oracledb.QuerySnapshot;
import com.oracle.database.osbdt.oracledb.WriteBatch;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.Tasks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OsbdtOracledbTest {

    private static OsbdtOracledb db;
    private static CollectionReference testCollection;

    @BeforeClass
    public static void setUpClass() {
        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
        optionsBuilder.setAuthType("base");
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("3C292A543E4AD6A8E0631F965E64EEE9");
        optionsBuilder.setAppId("3C292ADD4CCBD6C7E0631F965E64552C");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_CZXMDZFGRFGEITN");
        optionsBuilder.setApiVersion("1.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("3AA63EF01E17D2E1E0631F965E6455B5");

        OsbdtApp app;

        try{
            app = OsbdtApp.getInstance();
        } catch (Exception e)
        {
            Log.w("OsbdtTest", "Cannot find default instance. Creating the default instance.");
            app = OsbdtApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    , optionsBuilder.build());
        }

        db = OsbdtOracledb.getInstance(app);
        testCollection = db.collection("collectionTest");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Task<QuerySnapshot> fetchTask = testCollection.get();
        QuerySnapshot querySnapshot = Tasks.await(fetchTask, 5, TimeUnit.SECONDS);

        WriteBatch batch = db.batch();
        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            batch.delete(doc.getReference());
        }

        Task<Void> deleteTask = batch.commit();
        Tasks.await(deleteTask, 5, TimeUnit.SECONDS);
    }

    private static <T> T await(Task<T> task) throws ExecutionException, InterruptedException, TimeoutException {
        return Tasks.await(task, 5, TimeUnit.SECONDS);
    }


    // ------------------- OsbdtOradb Tests -------------------

    @Test
    public void testGetInstanceSingleton() {
        OsbdtOracledb db2 = OsbdtOracledb.getInstance();
        assertSame(db, db2);
    }

    @Test
    public void testCollectionAndDocumentReference() {
        CollectionReference col = db.collection("collectionTest");
        DocumentReference doc = col.document("fs_testDoc");

        assertEquals("collectionTest", col.getId());
        assertEquals("fs_testDoc", doc.getId());
    }

    @Test
    public void testBatchOperation() throws Exception {
        DocumentReference doc = db.collection("collectionTest").document("fs_batch");
        WriteBatch batch = db.batch();
        batch.set(doc, Collections.singletonMap("name", "BatchTest"));
        await(batch.commit());

        DocumentSnapshot snap = await(doc.get());
        assertEquals("BatchTest", snap.getString("name"));
    }

    @Test
    public void testRunTransaction() throws Exception {
        DocumentReference doc = db.collection("collectionTest").document("fs_tx");
        await(doc.set(Collections.singletonMap("count", 1)));

        await(db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(doc);
            long current = snapshot.getLong("count");
            transaction.update(doc, "count", current + 1);
            return null;
        }));

        DocumentSnapshot snap = await(doc.get());
        assertEquals(Long.valueOf(2), snap.getLong("count"));
    }

//    @Test
//    public void testClearPersistence() throws Exception {
//        // Should complete without error
//        await(db.clearPersistence());
//    }
//
//    @Test
//    public void testEnableDisableNetwork() throws Exception {
//        await(db.disableNetwork());
//        await(db.enableNetwork());
//    }
//
//    @Test
//    public void testFirestoreSettings() {
//        OsbdtOradbSettings settings = new OsbdtOradbSettings.Builder()
//                .setCacheSizeBytes(OsbdtOradbSettings.CACHE_SIZE_UNLIMITED)
//                .build();
//        db.setFirestoreSettings(settings);
//
//        OsbdtOradbSettings applied = db.getFirestoreSettings();
//        assertEquals(OsbdtOradbSettings.CACHE_SIZE_UNLIMITED, applied.getCacheSizeBytes());
//    }

    @Test
    public void testEqualityHashCodeAndToString() {
        OsbdtOracledb db2 = OsbdtOracledb.getInstance();
        assertEquals(db, db2);
        assertEquals(db.hashCode(), db2.hashCode());
        assertNotNull(db.toString());
    }
}

