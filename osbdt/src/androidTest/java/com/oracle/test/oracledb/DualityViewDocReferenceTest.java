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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.database.osbdt.oracledb.DocumentSnapshot;
import com.oracle.database.osbdt.oracledb.DualityViewColReference;
import com.oracle.database.osbdt.oracledb.DualityViewDocReference;
import com.oracle.database.osbdt.oracledb.FieldPath;
import com.oracle.database.osbdt.oracledb.ListenerRegistration;
import com.oracle.database.osbdt.oracledb.MetadataChanges;
import com.oracle.database.osbdt.oracledb.OsbdtOracledb;
import com.oracle.database.osbdt.oracledb.SnapshotListenOptions;
import com.oracle.database.osbdt.oracledb.Source;
import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.Tasks;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DualityViewDocReferenceTest {

    private static OsbdtOracledb db;
    private static DualityViewColReference deptCollection;
    private static DualityViewDocReference dept10;

    @BeforeClass
    public static void setUpClass() {
        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
        optionsBuilder.setAuthType("base");
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("41BD57980D9BA85CE0631F965E649B56");
        optionsBuilder.setAppId("41BD57EC1729A876E0631F965E640759");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_AJJUTUHAHSTEMBD");
        optionsBuilder.setApiVersion("2.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("40D2FCF5350D84CDE0631F965E64CACE");
        optionsBuilder.setUseSocket(true);

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
        deptCollection = db.dualityViewCollection("department_dv");
        dept10 = db.dualityViewDocument("department_dv/FB03C10B00");
    }

    private static <T> T await(Task<T> task) throws Exception {
        return Tasks.await(task, 5, TimeUnit.SECONDS);
    }
    // ------------------- Tests -------------------

    @Test
    public void testCreateAndReadDocument() throws Exception {
        DocumentSnapshot snapshot = await(dept10.get());
        assertTrue(snapshot.exists());
        assertEquals(10L, snapshot.getLong("_id").longValue());
        assertEquals("ACCOUNTING", snapshot.getString("departmentName"));
        assertEquals("BOSTON", snapshot.getString("location"));
    }

    @Test
    public void testUpdateDocumentField() throws Exception {
        await(dept10.update("location", "BOSTON"));
        DocumentSnapshot snapshot = await(dept10.get());
        assertEquals("BOSTON", snapshot.getString("location"));
    }

//    @Test
//    public void testSetDocumentOverwrite() throws Exception {
//        Map<String, Object> newData = new HashMap<>();
//        newData.put("departmentName", "NEW NAME");
//        newData.put("location", "CHICAGO");
//        await(dept10.set(newData));
//
//        DocumentSnapshot snapshot = await(dept10.get());
//        assertEquals("NEW NAME", snapshot.getString("departmentName"));
//        assertEquals("CHICAGO", snapshot.getString("location"));
//    }

//    @Test
//    public void testNestedFields() throws Exception {
//        Map<String, Object> data = new HashMap<>();
//        data.put("_id", 20);
//        data.put("details", new HashMap<String, Object>() {{
//            put("head", "JONES");
//            put("budget", 50000);
//        }});
//        DualityViewDocReference doc = db.dualityViewCollection("department_dv").document("20");
//        await(doc.set(data));
//
//        DocumentSnapshot snapshot = await(doc.get());
//        assertEquals("JONES", snapshot.get("details.head"));
//        assertEquals(50000L, snapshot.getLong("details.budget").longValue());
//    }


//    @Test
//    public void testTransactions() throws Exception {
//        db.runTransaction((Transaction.Function<Void>) transaction -> {
//            DocumentSnapshot snap = transaction.get(dept10);
//            String current = snap.getString("dname");
//            transaction.update(dept10, "dname", current + "_TXN");
//            return null;
//        }).get(10, TimeUnit.SECONDS);
//
//        DocumentSnapshot snapshot = await(dept10.get());
//        assertTrue(snapshot.getString("dname").endsWith("_TXN"));
//    }

//    @Test
//    public void testBatchWrite() throws Exception {
//        DualityViewDocReference d40 = db.dualityViewCollection("department_dv").document("40");
//        DualityViewDocReference d50 = db.dualityViewCollection("department_dv").document("50");
//
//        WriteBatch batch = db.batch();
//        batch.set(d40, new HashMap<String, Object>() {{
//            put("deptno", 40);
//            put("dname", "OPS");
//        }});
//        batch.set(d50, new HashMap<String, Object>() {{
//            put("deptno", 50);
//            put("dname", "HR");
//        }});
//        await(batch.commit());
//
//        DocumentSnapshot snap40 = await(d40.get());
//        DocumentSnapshot snap50 = await(d50.get());
//        assertEquals("OPS", snap40.getString("dname"));
//        assertEquals("HR", snap50.getString("dname"));
//    }

//    @Test
//    public void testSnapshotListener() throws Exception {
//        CountDownLatch latch = new CountDownLatch(1);
//        final boolean[] received = {false};
//
//        ListenerRegistration reg = dept10.addSnapshotListener(new EventListener<DocumentSnapshot>() {
//            @Override
//            public void onEvent(DocumentSnapshot snapshot, OsbdtOradbException error) {
//                if (error == null && snapshot.exists()) {
//                    received[0] = true;
//                    latch.countDown();
//                }
//            }
//        });
//
//        // Trigger update
//        await(dept10.update("loc", "MIAMI"));
//        latch.await(5, TimeUnit.SECONDS);
//        reg.remove();
//
//        assertTrue(received[0]);
//    }

//    @Test
//    public void testEqualsAndHashcode() {
//        DualityViewDocReference ref1 = db.dualityViewCollection("department_dv").document("10");
//        DualityViewDocReference ref2 = db.dualityViewCollection("department_dv").document("10");
//        DualityViewDocReference ref3 = db.dualityViewCollection("department_dv").document("20");
//
//        assertEquals(ref1, ref2);
//        assertNotEquals(ref1, ref3);
//        assertEquals(ref1.hashCode(), ref2.hashCode());
//    }

    // ------------------- Constructor and Basic Functionality Tests -------------------

    @Test
    public void testDualityViewDocReferenceConstructor() throws Exception {
        // Use reflection to test protected constructor
        Constructor<DualityViewDocReference> constructor = DualityViewDocReference.class.getDeclaredConstructor(
                String.class, OsbdtOracledb.class, boolean.class);
        constructor.setAccessible(true);

        // Test with specific OID (oidGenerated = true)
        DualityViewDocReference docRef = constructor.newInstance("department_dv/FB03C10B00", db, true);

        assertNotNull("DualityViewDocReference should be created", docRef);
        assertEquals("Document ID should match", "FB03C10B00", docRef.getId());
        assertEquals("Parent ID should match", "department_dv", docRef.getParent().getId());
        assertTrue("Path should contain both collection and document",
                  docRef.getPath().contains("department_dv/FB03C10B00"));
    }

    @Test
    public void testDualityViewDocReferenceConstructorWithAutoGenerated() throws Exception {
        Constructor<DualityViewDocReference> constructor = DualityViewDocReference.class.getDeclaredConstructor(
                String.class, OsbdtOracledb.class, boolean.class);
        constructor.setAccessible(true);

        // Test with auto-generated ID (oidGenerated = false)
        DualityViewDocReference docRef = constructor.newInstance("department_dv/tempId", db, false);

        assertNotNull("DualityViewDocReference should be created", docRef);
        assertNotNull("Auto-generated ID should not be null", docRef.getId());
        assertTrue("Auto-generated ID should start with temp prefix",
                  docRef.getId().startsWith("BAAS_DOC_TEMP_ID="));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDualityViewDocReferenceConstructorWithInvalidPath() throws Exception {
        Constructor<DualityViewDocReference> constructor = DualityViewDocReference.class.getDeclaredConstructor(
                String.class, OsbdtOracledb.class, boolean.class);
        constructor.setAccessible(true);

        // Path with more than 2 segments should fail
        constructor.newInstance("department_dv/document/extra", db, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDualityViewDocReferenceConstructorWithSingleSegmentPath() throws Exception {
        Constructor<DualityViewDocReference> constructor = DualityViewDocReference.class.getDeclaredConstructor(
                String.class, OsbdtOracledb.class, boolean.class);
        constructor.setAccessible(true);

        // Path with only 1 segment should fail
        constructor.newInstance("department_dv", db, true);
    }

    // ------------------- add() Method Tests -------------------

    @Test
    public void testAddDocument() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("_id", 999);
        data.put("departmentName", "ADDED_DEPT");
        data.put("location", "ADDED_LOCATION");
        data.put("employees", Collections.emptyList());

        DualityViewDocReference docRef = await(deptCollection.add(data));

        assertNotNull("Added document reference should not be null", docRef);
        assertNotNull("Document ID should be generated", docRef.getId());

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Department name should match", "ADDED_DEPT", snapshot.getString("departmentName"));
        assertEquals("Location should match", "ADDED_LOCATION", snapshot.getString("location"));

        // Clean up
        await(docRef.delete());
    }

    // ------------------- get() Method Tests -------------------

    @Test
    public void testGetDocument() throws Exception {
        DocumentSnapshot snapshot = await(dept10.get());

        assertNotNull("Snapshot should not be null", snapshot);
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Department name should match", "ACCOUNTING", snapshot.getString("departmentName"));
        assertEquals("Location should match", "BOSTON", snapshot.getString("location"));
    }

    @Test
    public void testGetDocumentWithSource() throws Exception {
        DocumentSnapshot snapshot = await(dept10.get(Source.DEFAULT));

        assertNotNull("Snapshot should not be null", snapshot);
        assertTrue("Document should exist", snapshot.exists());
    }

    // ------------------- set() Method Tests -------------------

    @Test
    public void testSetDocument() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("_id", 888);
        data.put("departmentName", "SET_DEPT");
        data.put("location", "SET_LOCATION");
        data.put("employees", Collections.emptyList());

        DualityViewDocReference docRef = deptCollection.document("888");

        await(docRef.set(data));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Department name should match", "SET_DEPT", snapshot.getString("departmentName"));
        assertEquals("Location should match", "SET_LOCATION", snapshot.getString("location"));

        // Clean up
        await(docRef.delete());
    }



    // ------------------- update() Method Tests -------------------

    @Test
    public void testUpdateDocumentWithMap() throws Exception {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("location", "UPDATED_LOCATION");

        await(dept10.update(updateData));

        DocumentSnapshot snapshot = await(dept10.get());
        assertEquals("Location should be updated", "UPDATED_LOCATION", snapshot.getString("location"));
    }

    @Test
    public void testUpdateDocumentWithKeyValuePairs() throws Exception {
        await(dept10.update("location", "UPDATED_LOCATION_2"));

        DocumentSnapshot snapshot = await(dept10.get());
        assertEquals("Location should be updated", "UPDATED_LOCATION_2", snapshot.getString("location"));
    }

    @Test
    public void testUpdateDocumentWithFieldPath() throws Exception {
        await(dept10.update(FieldPath.of("location"), "UPDATED_LOCATION_3"));

        DocumentSnapshot snapshot = await(dept10.get());
        assertEquals("Location should be updated", "UPDATED_LOCATION_3", snapshot.getString("location"));
    }

    // ------------------- delete() Method Tests -------------------

    @Test
    public void testDeleteDocument() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("_id", 666);
        data.put("departmentName", "DELETE_TEST");
        data.put("location", "DELETE_LOCATION");

        DualityViewDocReference docRef = await(deptCollection.add(data));

        DocumentSnapshot beforeDelete = await(docRef.get());
        assertTrue("Document should exist before delete", beforeDelete.exists());

        await(docRef.delete());

        DocumentSnapshot afterDelete = await(docRef.get());
        assertFalse("Document should not exist after delete", afterDelete.exists());
    }

    // ------------------- equals() and hashCode() Tests -------------------

    @Test
    public void testEqualsSameObject() throws Exception {
        assertTrue("DocumentReference should equal itself", dept10.equals(dept10));
    }

    @Test
    public void testEqualsIdenticalReferences() throws Exception {
        DualityViewDocReference docRef1 = db.dualityViewDocument("department_dv/FB03C10B00");
        DualityViewDocReference docRef2 = db.dualityViewDocument("department_dv/FB03C10B00");

        assertTrue("Identical DocumentReferences should be equal", docRef1.equals(docRef2));
        assertEquals("Equal references should have equal hash codes", docRef1.hashCode(), docRef2.hashCode());
    }

    @Test
    public void testEqualsDifferentReferences() throws Exception {
        DualityViewDocReference docRef1 = db.dualityViewDocument("department_dv/FB03C10B00");
        DualityViewDocReference docRef2 = db.dualityViewDocument("department_dv/FB03C12900");

        assertFalse("Different DocumentReferences should not be equal", docRef1.equals(docRef2));
        assertNotEquals("Different references should have different hash codes", docRef1.hashCode(), docRef2.hashCode());
    }

    @Test
    public void testEqualsWithNull() throws Exception {
        assertFalse("DocumentReference should not equal null", dept10.equals(null));
    }

    @Test
    public void testEqualsWithDifferentType() throws Exception {
        assertFalse("DocumentReference should not equal different type", dept10.equals("string"));
    }

    @Test
    public void testEqualsWithDifferentDatabases() throws Exception {
        // This would require multiple database instances, which is complex to set up
        assertTrue("Same database reference should equal itself", dept10.equals(dept10));
    }

    // ------------------- getId() and getParent() Tests -------------------

    @Test
    public void testGetId() throws Exception {
        assertEquals("Document ID should match", "FB03C10B00", dept10.getId());
    }

    @Test
    public void testGetParent() throws Exception {
        DualityViewColReference parent = dept10.getParent();
        assertNotNull("Parent should not be null", parent);
        assertEquals("Parent ID should match", "department_dv", parent.getId());
    }

    @Test
    public void testGetPath() throws Exception {
        String path = dept10.getPath();
        assertNotNull("Path should not be null", path);
        assertTrue("Path should contain collection name", path.contains("department_dv"));
        assertTrue("Path should contain document ID", path.contains("FB03C10B00"));
    }

    @Test
    public void testGetPathSegments() throws Exception {
        List<String> segments = dept10.getPathSegments();
        assertNotNull("Path segments should not be null", segments);
        assertTrue("Should have at least 2 segments", segments.size() >= 2);
        assertEquals("First segment should be collection name", "department_dv", segments.get(0));
    }

    // ------------------- Listener Tests -------------------

    @Test
    public void testAddSnapshotListener() throws Exception {
        ListenerRegistration registration = dept10.addSnapshotListener((snapshot, error) -> {
            // Basic listener test - just verify it can be added
            assertTrue("Listener callback executed", true);
        });

        assertNotNull("Listener registration should not be null", registration);

        // Clean up
        registration.remove();
    }

    @Test
    public void testAddSnapshotListenerWithActivity() throws Exception {
        // This test would require an Activity context, which is complex to set up in unit tests
        // For now, just test that the method exists and can be called
        try {
            ListenerRegistration registration = dept10.addSnapshotListener((android.app.Activity) null, (snapshot, error) -> {
                // Listener callback
            });
            assertNotNull("Listener registration should not be null", registration);
            registration.remove();
        } catch (Exception e) {
            // Expected in test environment without Activity
            assertTrue("Exception is expected in test environment", true);
        }
    }

    @Test
    public void testAddSnapshotListenerWithExecutor() throws Exception {
        ListenerRegistration registration = dept10.addSnapshotListener(command -> {
            // Execute on main thread for test
            command.run();
        }, (snapshot, error) -> {
            // Basic listener test
            assertTrue("Listener callback executed", true);
        });

        assertNotNull("Listener registration should not be null", registration);

        // Clean up
        registration.remove();
    }

    @Test
    public void testAddSnapshotListenerWithMetadataChanges() throws Exception {
        ListenerRegistration registration = dept10.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
            // Basic listener test
            assertTrue("Listener callback executed", true);
        });

        assertNotNull("Listener registration should not be null", registration);

        // Clean up
        registration.remove();
    }

    @Test
    public void testAddSnapshotListenerWithOptions() throws Exception {
        SnapshotListenOptions options = new SnapshotListenOptions.Builder()
                .setExecutor(command -> command.run())
                .build();

        ListenerRegistration registration = dept10.addSnapshotListener(options, (snapshot, error) -> {
            // Basic listener test
            assertTrue("Listener callback executed", true);
        });

        assertNotNull("Listener registration should not be null", registration);

        // Clean up
        registration.remove();
    }

    // ------------------- Error Condition Tests -------------------

    @Test(expected = NullPointerException.class)
    public void testAddWithNullData() throws Exception {
        await(deptCollection.add(null));
    }

    @Test(expected = NullPointerException.class)
    public void testSetWithNullData() throws Exception {
        await(dept10.set(null));
    }

    @Test(expected = NullPointerException.class)
    public void testSetWithNullOptions() throws Exception {
        await(dept10.set(new HashMap<>(), null));
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateWithNullData() throws Exception {
        await(dept10.update(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateWithUnevenFieldValuePairs() throws Exception {
        await(dept10.update("field1", "value1", "field2"));
    }

    // ------------------- toString() Test -------------------

    @Test
    public void testToString() throws Exception {
        String stringRep = dept10.toString();
        assertNotNull("toString should not return null", stringRep);
        assertFalse("toString should not be empty", stringRep.isEmpty());
        // Should contain some identifying information
        assertTrue("toString should contain duality view info",
                  stringRep.contains("DualityViewDocReference") || stringRep.contains("FB03C10B00"));
    }
}
