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

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.oracledb.CollectionReference;
import com.oracle.mobile.fusabase.oracledb.DocumentReference;
import com.oracle.mobile.fusabase.oracledb.DocumentSnapshot;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.oracledb.QueryDocumentSnapshot;
import com.oracle.mobile.fusabase.oracledb.QuerySnapshot;
import com.oracle.mobile.fusabase.oracledb.WriteBatch;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.Tasks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.concurrent.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class QueryDocumentSnapshotTest {

    private static FusabaseOracledb db;
    private static CollectionReference testCollection;

    @BeforeClass
    public static void setUpClass() {
        FusabaseOptions.Builder optionsBuilder = new FusabaseOptions.Builder();
        optionsBuilder.setAuthType("base");
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("3C292A543E4AD6A8E0631F965E64EEE9");
        optionsBuilder.setAppId("3C292ADD4CCBD6C7E0631F965E64552C");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_CZXMDZFGRFGEITN");
        optionsBuilder.setApiVersion("1.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("3AA63EF01E17D2E1E0631F965E6455B5");

        FusabaseApp app;

        try{
            app = FusabaseApp.getInstance();
        } catch (Exception e)
        {
            Log.w("FusabaseTest", "Cannot find default instance. Creating the default instance.");
            app = FusabaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    , optionsBuilder.build());
        }

        db = FusabaseOracledb.getInstance(app);
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

    private static <T> T await(Task<T> task) throws Exception {
        return Tasks.await(task, 5, TimeUnit.SECONDS);
    }


    // ------------------- QueryDocumentSnapshot Tests -------------------

    @Test
    public void testDocumentExists() throws Exception {
        DocumentReference doc = testCollection.document("qds_doc2");
        await(doc.set(Collections.singletonMap("field", "value")));

        DocumentSnapshot snapshot = await(doc.get());
        assertTrue(snapshot.exists());
    }

    @Test
    public void testDocumentDoesNotExist() throws Exception {
        DocumentReference doc = testCollection.document("non_existent_doc");

        DocumentSnapshot snapshot = await(doc.get());
        assertFalse(snapshot.exists());
    }

    @Test
    public void testGetIdAndReference() throws Exception {
        DocumentReference doc = testCollection.document("qds_doc2");
        await(doc.set(Collections.singletonMap("num", 42)));

        QuerySnapshot qs = await(testCollection.get());
        QueryDocumentSnapshot qds = null;
        for (QueryDocumentSnapshot snap : qs) {
            if (snap.getId().equals("qds_doc2")) {
                qds = snap;
            }
        }

        assertNotNull(qds);
        assertEquals("qds_doc2", qds.getId());
        assertEquals(doc.getPath(), qds.getReference().getPath());
    }

    @Test
    public void testGetFieldValues() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Alice");
        data.put("age", 30);
        DocumentReference doc = testCollection.document("qds_doc3");
        await(doc.set(data));

        QuerySnapshot qs = await(testCollection.get());
        QueryDocumentSnapshot qds = null;
        for (QueryDocumentSnapshot snap : qs) {
            if (snap.getId().equals("qds_doc3")) {
                qds = snap;
            }
        }

        assertNotNull(qds);
        assertEquals("Alice", qds.getString("name"));
        assertEquals(Long.valueOf(30), qds.getLong("age"));

        Map<String, Object> returnedData = qds.getData();
        assertEquals(data.get("name"), returnedData.get("name"));
        assertEquals(Long.valueOf((Integer)data.get("age")), returnedData.get("age"));
    }

//    @Test
//    public void testEqualityHashCodeAndToString() throws Exception {
//        DocumentReference doc = testCollection.document("qds_doc4");
//        await(doc.set(Collections.singletonMap("k", "v")));
//
//        QuerySnapshot qs1 = await(testCollection.get());
//        QuerySnapshot qs2 = await(testCollection.get());
//
//        QueryDocumentSnapshot snap1 = null;
//        QueryDocumentSnapshot snap2 = null;
//        for (QueryDocumentSnapshot s : qs1) {
//            if (s.getId().equals("qds_doc4")) snap1 = s;
//        }
//        for (QueryDocumentSnapshot s : qs2) {
//            if (s.getId().equals("qds_doc4")) snap2 = s;
//        }
//
//        assertNotNull(snap1);
//        assertNotNull(snap2);
//
//        assertEquals(snap1, snap2);
//        assertEquals(snap1.hashCode(), snap2.hashCode());
//    }
}
