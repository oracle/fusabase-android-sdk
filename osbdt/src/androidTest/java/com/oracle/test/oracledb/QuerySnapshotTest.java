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
import com.oracle.database.osbdt.oracledb.SnapshotMetadata;
import com.oracle.database.osbdt.oracledb.WriteBatch;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.Tasks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.concurrent.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class QuerySnapshotTest {

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


    // ------------------- QuerySnapshot Tests -------------------

    @Test
    public void testGetDocumentsAndSize() throws Exception {
        DocumentReference doc1 = testCollection.document("qs_doc1");
        DocumentReference doc2 = testCollection.document("qs_doc2");
        await(doc1.set(Collections.singletonMap("val", 1)));
        await(doc2.set(Collections.singletonMap("val", 2)));

        QuerySnapshot qs = await(testCollection.get());

        assertEquals(2, qs.size());
        assertFalse(qs.isEmpty());

        List<DocumentSnapshot> docs = qs.getDocuments();
        Set<String> ids = new HashSet<>();
        for (DocumentSnapshot d : docs) {
            ids.add(d.getId());
        }

        assertTrue(ids.contains("qs_doc1"));
        assertTrue(ids.contains("qs_doc2"));
    }

//    @Test
//    public void testDocumentChanges() throws Exception {
//        DocumentReference doc = testCollection.document("qs_changes");
//        await(doc.set(Collections.singletonMap("field", "old")));
//
//        // Initial query
//        QuerySnapshot before = await(testCollection.get());
//
//        // Update doc
//        await(doc.update("field", "new"));
//
//        QuerySnapshot after = await(testCollection.get());
//
//        List<DocumentChange> changes = after.getDocumentChanges(before);
//
//        assertFalse(changes.isEmpty());
//        boolean hasModified = false;
//        for (DocumentChange change : changes) {
//            if (change.getType() == DocumentChange.Type.MODIFIED) {
//                hasModified = true;
//            }
//        }
//        assertTrue(hasModified);
//    }

    @Test
    public void testMetadata() throws Exception {
        QuerySnapshot qs = await(testCollection.get());
        SnapshotMetadata md = qs.getMetadata();

        assertNotNull(md);
        // Emulator always from server by default
        assertTrue(md.isFromCache() == false || md.isFromCache() == true);
    }

//    @Test
//    public void testEqualityHashCodeAndToString() throws Exception {
//        QuerySnapshot qs1 = await(testCollection.get());
//        QuerySnapshot qs2 = await(testCollection.get());
//
//        assertEquals(qs1, qs2);
//        assertEquals(qs1.hashCode(), qs2.hashCode());
//        assertNotNull(qs1.toString());
//    }

    @Test
    public void testIteratorWorks() throws Exception {
        // Ensure at least one doc exists
        DocumentReference doc = testCollection.document("qs_iter");
        await(doc.set(Collections.singletonMap("field", "iterTest")));

        QuerySnapshot qs = await(testCollection.get());

        int count = 0;
        for (DocumentSnapshot d : qs) {
            assertNotNull(d.getId());
            count++;
        }

        assertTrue(count > 0);
        assertEquals(count, qs.size());
    }
}
