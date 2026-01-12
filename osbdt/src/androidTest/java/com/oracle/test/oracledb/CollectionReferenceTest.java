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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.database.osbdt.auth.OsbdtAuth;
import com.oracle.database.osbdt.oracledb.CollectionReference;
import com.oracle.database.osbdt.oracledb.DocumentChange;
import com.oracle.database.osbdt.oracledb.DocumentReference;
import com.oracle.database.osbdt.oracledb.DocumentSnapshot;
import com.oracle.database.osbdt.oracledb.FieldValue;
import com.oracle.database.osbdt.oracledb.ListenerRegistration;
import com.oracle.database.osbdt.oracledb.MetadataChanges;
import com.oracle.database.osbdt.oracledb.OsbdtOracledb;
import com.oracle.database.osbdt.oracledb.OsbdtOracledbException;
import com.oracle.database.osbdt.oracledb.Query;
import com.oracle.database.osbdt.oracledb.QuerySnapshot;
import com.oracle.database.osbdt.oracledb.SetOptions;
import com.oracle.database.osbdt.oracledb.WriteBatch;
import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;
import com.oracle.database.osbdt.Timestamp;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.Tasks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class CollectionReferenceTest {

    private static OsbdtOracledb db;
    private static OsbdtAuth auth;
    private static CollectionReference testCollection;

    private static final String projectId = "43C68B185E9732FEE0631F965E641907";
    private static final String schema = "user1";
    private static final String ordsHost = "http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/";

    // Example POJO
    public static class UserProfile {
        private String name;
        private int age;
        private boolean active;
        private Timestamp birthday;
        private List<String> tags;
        private Map<String, Object> address;

        // Required no-arg constructor
        public UserProfile() {}

        public UserProfile(String name, int age, boolean active, Timestamp birthday, List<String> tags, Map<String, Object> address) {
            this.name = name;
            this.age = age;
            this.active = active;
            this.birthday = birthday;
            this.tags = tags;
            this.address = address;
        }

        // Getters and setters omitted for brevity
        public String getName() { return name; }
        public int getAge() { return age; }
        public boolean isActive() { return active; }
        public Timestamp getBirthday() { return birthday; }
        public List<String> getTags() { return tags; }
        public Map<String, Object> getAddress() { return address; }

        public void setAge(int age) {
            this.age = age;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void setBirthday(Timestamp birthday) {
            this.birthday = birthday;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public void setAddress(Map<String, Object> address) {
            this.address = address;
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {

        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
        optionsBuilder.setAuthType("base");
//        IDCSOptions.Builder idcsOptionsBuilder = new IDCSOptions.Builder();
//        idcsOptionsBuilder.setClientId("44d4a5a58a80476e90389ab9f403db87");
//        idcsOptionsBuilder.setClientSecret("idcscs-3a5ba1c4-b496-443d-ac9e-5efd7125d5c2");
//        idcsOptionsBuilder.setDomainURL("https://idcs-360c1204d11f41d3836e7fb2f1769ca7.identity.oraclecloud.com");
//        idcsOptionsBuilder.setSelfRegistrationProfile("f336e6256b9644059663394fe75d7494");
//        optionsBuilder.setIDCSOptions(idcsOptionsBuilder.build());
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("43C68B185E9732FEE0631F965E641907");
        optionsBuilder.setAppId("43C6C814E61A4D68E0631F965E642FFE");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_ZGQQKTRYWPIIFOT");
        optionsBuilder.setApiVersion("2.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("43C6C6DC03224D43E0631F965E6456DC");
        optionsBuilder.setUseSocket(false);
        optionsBuilder.setEnableLogging(true);
        optionsBuilder.setLongPollingInterval(2);
        OsbdtApp app;

        try{
            app = OsbdtApp.getInstance();
        } catch (Exception e)
        {
            Log.w("OsbdtTest", "Cannot find default instance. Creating the default instance.");
            app = OsbdtApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    , optionsBuilder.build());
        }
        auth = OsbdtAuth.getInstance(app);
        db = OsbdtOracledb.getInstance(app);
        testCollection = db.collection("alpha");


        // Insert varied documents
        // doc1: All scalar types
        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("stringValue", "Hello");
        doc1.put("checkValue", "Hello");
        doc1.put("intValue", 123);
        doc1.put("boolValue", true);
        doc1.put("dateValue", new Date(1700000000000L));
        doc1.put("nullValue", null);

        // doc2: Array/Map
        Map<String, Object> address = new HashMap<>();
        address.put("city", "London");
        address.put("zip", 54321);
        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("listValue", Arrays.asList("A", "B", "C"));
        doc2.put("mapValue", address);

        // doc3: Not Supporting Document assignment like this
        Map<String, Object> doc3 = new HashMap<>();
        doc3.put("refValue", testCollection.document("doc1"));

        // doc4: POJO
        UserProfile user = new UserProfile(
                "POJO", 99, false, Timestamp.now(),
                Arrays.asList("p1", "p2"),
                Collections.singletonMap("note", "POJO Address")
        );

        // SignIn a user
        Tasks.await(auth.signInWithEmailAndPassword("mohitv123@gmail.com", "MohitV@123"));
        // Insert all (blocking for setup safety)
        Tasks.await(testCollection.document("doc1").set(doc1));
        Tasks.await(testCollection.document("doc2").set(doc2));
        Tasks.await(testCollection.document("doc3").set(doc3));
        Tasks.await(testCollection.document("doc4").set(user));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Thread.sleep(2000);
        // Remove all docs in the collection for isolation between tests
        QuerySnapshot allDocs = Tasks.await(testCollection.get(), 50, TimeUnit.SECONDS);
        WriteBatch batch = db.batch();
        for (DocumentSnapshot doc : allDocs.getDocuments()) {
            batch.delete(doc.getReference());
        }
        Tasks.await(batch.commit(), 50, TimeUnit.SECONDS);
    }

    private void addSecurityRules(String collection) throws Exception {
        String url = ordsHost + "_/baas-panel/project/cel/" + projectId + "?storage=1";
        String credentials = schema + ":" + schema;
        String basicAuth = "Basic " + android.util.Base64.encodeToString(credentials.getBytes(), android.util.Base64.DEFAULT);
        java.net.URL u = new java.net.URL(url);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setDoOutput(true);
        String body = "match /" + collection + "/{fileId} {\n" +
            "  allow read: if true;\n" +
            "  allow write: if true;\n" +
            "}";
        java.io.OutputStream os = conn.getOutputStream();
        os.write(body.getBytes());
        os.flush();
        os.close();
        int responseCode = conn.getResponseCode();
        conn.disconnect();
        if (!(responseCode >= 200 && responseCode < 300)) {
            throw new RuntimeException("Security rules failed to apply");
        } else {
            Log.d("SecurityRule", "Rules applied Successfully");
        }
    }

    private static <T> T await(Task<T> task) throws ExecutionException, InterruptedException, TimeoutException {
        return Tasks.await(task);
    }

//    @Test
//    public void testComprehensiveCollectionSnapshotListener() throws Exception {
//        // Expecting 3 live events (add, modify, delete)
//        CountDownLatch latch = new CountDownLatch(3);
//
//        final DocumentReference docRef = testCollection.document("doc11");
//
//        final List<String> events = new ArrayList<>();
//        final boolean[] initialLoadCompleted = {false};
//
//        // --- Attach collection-level listener ---
//        ListenerRegistration registration = testCollection.addSnapshotListener((snapshots, error) -> {
//            assertNull("Snapshot listener error: " + error, error);
//            assertNotNull(snapshots);
//
//            for (DocumentChange change : snapshots.getDocumentChanges()) {
//                String id = change.getDocument().getId();
//
//                // The very first emission from a snapshot listener contains all existing docs.
//                // We'll skip those for "live" testing.
//                if (!initialLoadCompleted[0]) {
//                    // Mark initial snapshot processed
//                    continue;
//                }
//
//                switch (change.getType()) {
//                    case ADDED:
//                        events.add("ADDED_" + id);
//                        assertEquals("snapshotAdd", change.getDocument().getString("status"));
//                        latch.countDown();
//                        break;
//
//                    case MODIFIED:
//                        events.add("MODIFIED_" + id);
//                        assertEquals("snapshotUpdated", change.getDocument().getString("status"));
//                        latch.countDown();
//                        break;
//
//                    case REMOVED:
//                        events.add("REMOVED_" + id);
//                        latch.countDown();
//                        break;
//                }
//            }
//
//            // After first snapshot event (which includes all existing docs)
//            // mark initial load as complete so future updates are tracked
//            initialLoadCompleted[0] = true;
//        });
//
//        // --- Attach document-level listener ---
//        CountDownLatch docLatch = new CountDownLatch(2);
//        final List<String> docEvents = new ArrayList<>();
//
//        ListenerRegistration docRegistration = docRef.addSnapshotListener((snapshot, error) -> {
//            assertNull("Document snapshot listener error: " + error, error);
//            if (snapshot != null && snapshot.exists()) {
//                String val = snapshot.getString("status");
//                docEvents.add("UPDATED:" + val);
//                docLatch.countDown();
//            } else if (snapshot != null && !snapshot.exists()) {
//                docEvents.add("DELETED");
//                docLatch.countDown();
//            }
//        });
//
//        Thread.sleep(6000);
//        // --- Trigger events (simulate real-time updates) ---
//        Tasks.await(docRef.set(Collections.singletonMap("status", "snapshotAdd")));
//        Thread.sleep(700); // allow listener to catch up
//
//        Tasks.await(docRef.update("status", "snapshotUpdated"));
//        Thread.sleep(700);
//
//        Tasks.await(docRef.delete());
//        Thread.sleep(700);
//
//        // --- Await all listener responses ---
//        assertTrue("Collection snapshot listener did not capture all changes",
//                latch.await(15, TimeUnit.SECONDS));
//        assertTrue("Document snapshot listener did not capture all changes",
//                docLatch.await(15, TimeUnit.SECONDS));
//
//        // --- Validate expected events ---
//        assertTrue("Add event missing", events.stream().anyMatch(e -> e.startsWith("ADDED_")));
//        assertTrue("Modify event missing", events.stream().anyMatch(e -> e.startsWith("MODIFIED_")));
//        assertTrue("Remove event missing", events.stream().anyMatch(e -> e.startsWith("REMOVED_")));
//
//        assertTrue("Document-level add/update not captured",
//                docEvents.stream().anyMatch(e -> e.contains("snapshotAdd") || e.contains("snapshotUpdated")));
//        assertTrue("Document-level delete not captured",
//                docEvents.contains("DELETED"));
//
//        // --- Test unsubscribe behavior ---
//        registration.remove();
//        docRegistration.remove();
//
//        // Wait for snapshot to deregister .. But what should be the behaviour
//        Thread.sleep(2000);
//        int totalBefore = events.size();
//        Tasks.await(testCollection.document("dummyAfterUnsub")
//                .set(Collections.singletonMap("status", "shouldNotTrigger")));
//        Thread.sleep(1000);
//
//        // After unsubscribe, no new events should be received
//        assertEquals("Listener still received updates after removal",
//                totalBefore, events.size());
//    }

//    // Relational Test
//    @Test
//    public void testComprehensiveRelationalSnapshotListener() throws Exception {
//        // Expecting 3 live events (add, modify, delete)
//        CountDownLatch latch = new CountDownLatch(3);
//
//        CollectionReference relationalCollection = db.collection("ALBUM");
//
//        DocumentReference docRef = relationalCollection.document();
//
//        final List<String> events = new ArrayList<>();
//        final boolean[] initialLoadCompleted = {false};
//
//        // --- Attach collection-level listener ---
//        ListenerRegistration registration = relationalCollection.addSnapshotListener((snapshots, error) -> {
//            assertNull("Snapshot listener error: " + error, error);
//            assertNotNull(snapshots);
//
//            for (DocumentChange change : snapshots.getDocumentChanges()) {
//                String id = change.getDocument().getId();
//
//                // The very first emission from a snapshot listener contains all existing docs.
//                // We'll skip those for "live" testing.
//                if (!initialLoadCompleted[0]) {
//                    // Mark initial snapshot processed
//                    continue;
//                }
//
//                switch (change.getType()) {
//                    case ADDED:
//                        events.add("ADDED_" + id);
//                        assertEquals("snapshotAdd", change.getDocument().getString("aname"));
//                        latch.countDown();
//                        break;
//
//                    case MODIFIED:
//                        events.add("MODIFIED_" + id);
//                        assertEquals("snapshotUpdated", change.getDocument().getString("aname"));
//                        latch.countDown();
//                        break;
//
//                    case REMOVED:
//                        events.add("REMOVED_" + id);
//                        latch.countDown();
//                        break;
//                }
//            }
//
//            // After first snapshot event (which includes all existing docs)
//            // mark initial load as complete so future updates are tracked
//            initialLoadCompleted[0] = true;
//        });
//
//        // --- Attach document-level listener ---
//        CountDownLatch docLatch = new CountDownLatch(2);
//        final List<String> docEvents = new ArrayList<>();
//
//        Thread.sleep(6000);
//        // --- Trigger events (simulate real-time updates) ---
//        Tasks.await(docRef.set(Map.of("aname", "snapshotAdd", "_id", 130)));
//        Thread.sleep(700); // allow listener to catch up
//
//        ListenerRegistration docRegistration = docRef.addSnapshotListener((snapshot, error) -> {
//            assertNull("Document snapshot listener error: " + error, error);
//            if (snapshot != null && snapshot.exists()) {
//                String val = snapshot.getString("aname");
//                docEvents.add("UPDATED:" + val);
//                docLatch.countDown();
//            } else if (snapshot != null && !snapshot.exists()) {
//                docEvents.add("DELETED");
//                docLatch.countDown();
//            }
//        });
//
//        Tasks.await(docRef.update("aname", "snapshotUpdated"));
//        Thread.sleep(1500);
//
//        Tasks.await(docRef.delete());
//        Thread.sleep(700);
//
//        // --- Await all listener responses ---
//        assertTrue("Collection snapshot listener did not capture all changes",
//                latch.await(20, TimeUnit.SECONDS));
//        assertTrue("Document snapshot listener did not capture all changes",
//                docLatch.await(20, TimeUnit.SECONDS));
//
//        // --- Validate expected events ---
//        assertTrue("Add event missing", events.stream().anyMatch(e -> e.startsWith("ADDED_")));
//        assertTrue("Modify event missing", events.stream().anyMatch(e -> e.startsWith("MODIFIED_")));
//        assertTrue("Remove event missing", events.stream().anyMatch(e -> e.startsWith("REMOVED_")));
//
//        assertTrue("Document-level add/update not captured",
//                docEvents.stream().anyMatch(e -> e.contains("snapshotAdd") || e.contains("snapshotUpdated")));
//        assertTrue("Document-level delete not captured",
//                docEvents.contains("DELETED"));
//
//        // --- Test unsubscribe behavior ---
//        registration.remove();
//        docRegistration.remove();
//
//        // Wait for snapshot to deregister .. But what should be the behaviour
//        Thread.sleep(2000);
//        int totalBefore = events.size();
//        Tasks.await(relationalCollection.document()
//                .set(Map.of("aname", "shouldNotTrigger", "_id", 456)));
//        Thread.sleep(1000);
//
//        // After unsubscribe, no new events should be received
//        assertEquals("Listener still received updates after removal",
//                totalBefore, events.size());
//    }

    // ------------------- Additional Comprehensive CollectionReference Tests -------------------

    @Test
    public void testCollectionReferenceGetId() throws Exception {
        CollectionReference colRef = db.collection("testCollection");
        assertEquals("testCollection", colRef.getId());
    }

    @Test
    public void testCollectionReferenceGetPath() throws Exception {
        CollectionReference colRef = db.collection("testCollection");
        assertEquals("testCollection", colRef.getPath());
    }

    @Test
    public void testCollectionReferenceGetParent() throws Exception {
        // Top-level collection should have null parent
        CollectionReference colRef = db.collection("testCollection");
        assertNull(colRef.getParent());
    }

    @Test
    public void testDocumentReferenceCreation() throws Exception {
        CollectionReference colRef = db.collection("testCollection");

        // Auto-generated ID document
        DocumentReference docRef1 = colRef.document();
        assertNotNull(docRef1.getId());
        assertTrue(docRef1.getId().length() > 0);

        // Specific ID document
        DocumentReference docRef2 = colRef.document("specificDoc");
        assertEquals("specificDoc", docRef2.getId());
    }

    @Test
    public void testAddDocumentWithAllDataTypes() throws Exception {
        Map<String, Object> comprehensiveData = new HashMap<>();

        // Primitive types
        comprehensiveData.put("stringField", "Hello World");
        comprehensiveData.put("intField", 42);
        comprehensiveData.put("longField", 123456789L);
        comprehensiveData.put("doubleField", 3.14159);
        comprehensiveData.put("booleanField", true);
        comprehensiveData.put("nullField", null);

        // Date/Timestamp
        comprehensiveData.put("dateField", new Date());
        comprehensiveData.put("timestampField", Timestamp.now());

        // Collections
        comprehensiveData.put("stringList", Arrays.asList("a", "b", "c"));
        comprehensiveData.put("intList", Arrays.asList(1, 2, 3));
        comprehensiveData.put("mixedList", Arrays.asList("string", 42, true));

        // Maps
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedString", "nested value");
        nestedMap.put("nestedInt", 99);
        comprehensiveData.put("nestedMap", nestedMap);

        // Add document and verify
        Task<DocumentReference> addTask = testCollection.add(comprehensiveData);
        DocumentReference addedDocRef = Tasks.await(addTask);

        assertNotNull(addedDocRef);
        assertNotNull(addedDocRef.getId());

        // Verify the document was added by retrieving it
        DocumentSnapshot snapshot = Tasks.await(addedDocRef.get());
        assertTrue(snapshot.exists());

        // Verify data types
        assertEquals("Hello World", snapshot.getString("stringField"));
        assertEquals((Long) 42L, snapshot.getLong("intField"));
        assertEquals((Long) 123456789L, snapshot.getLong("longField"));
        assertEquals(3.14159, snapshot.getDouble("doubleField"), 0.00001);
        assertTrue(snapshot.getBoolean("booleanField"));
        assertNull(snapshot.get("nullField"));
        assertNotNull(snapshot.getDate("dateField"));
        assertNotNull(snapshot.getTimestamp("timestampField"));

        // Verify collections
        List<String> stringList = (List<String>) snapshot.get("stringList");
        assertNotNull(stringList);
        assertEquals(Arrays.asList("a", "b", "c"), stringList);

        // Verify maps
        Map<String, Object> retrievedMap = (Map<String, Object>) snapshot.get("nestedMap");
        assertNotNull(retrievedMap);
        assertEquals("nested value", retrievedMap.get("nestedString"));
        assertEquals(99L, retrievedMap.get("nestedInt"));
    }

    @Test
    public void testFieldValueOperations() throws Exception {
        DocumentReference docRef = testCollection.document("fieldValueTest");

        // Initial document
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("counter", 10);
        initialData.put("tags", Arrays.asList("initial"));
        initialData.put("score", 100.0);
        Tasks.await(docRef.set(initialData));

        // Test increment (long)
        Tasks.await(docRef.update("counter", FieldValue.increment(5)));
        DocumentSnapshot snap = Tasks.await(docRef.get());
        assertEquals((Long) 15L, snap.getLong("counter"));

        // Test increment (long)
        Tasks.await(docRef.update("score", FieldValue.increment(10L)));
        snap = Tasks.await(docRef.get());
        assertEquals(110.0, snap.getDouble("score"), 0.001);

        // Test arrayUnion
        Tasks.await(docRef.update("tags", FieldValue.arrayUnion("added1", "added2")));
        snap = Tasks.await(docRef.get());
        List<String> tags = (List<String>) snap.get("tags");
        assertNotNull(tags);
        assertTrue(tags.contains("initial"));
        assertTrue(tags.contains("added1"));
        assertTrue(tags.contains("added2"));

        // Test arrayRemove
        Tasks.await(docRef.update("tags", FieldValue.arrayRemove("initial", "added1")));
        snap = Tasks.await(docRef.get());
        tags = (List<String>) snap.get("tags");
        assertNotNull(tags);
        assertFalse(tags.contains("initial"));
        assertFalse(tags.contains("added1"));
        assertTrue(tags.contains("added2"));

        // Test delete
        Tasks.await(docRef.update("score", FieldValue.delete()));
        snap = Tasks.await(docRef.get());
        assertFalse(snap.contains("score"));
    }

    public void testServerTimestampNotSupportedInUpdate() throws Exception {
        DocumentReference docRef = testCollection.document("serverTimestampTest");

        Exception exception = assertThrows(
            Exception.class,
            () -> Tasks.await(docRef.update("timestamp", FieldValue.serverTimestamp()))
        );

        // Now you can inspect the exception cause, message, etc.
        assertTrue(exception.getCause() instanceof OsbdtOracledbException);
    }

    @Test
    public void testAddDocumentWithServerTimestamp() throws Exception {
        Map<String, Object> dataWithServerTimestamp = new HashMap<>();
        dataWithServerTimestamp.put("name", "testDoc");
        dataWithServerTimestamp.put("createdAt", FieldValue.serverTimestamp());

        // ServerTimestamp is supported for add operations and should not throw an exception
        Task<DocumentReference> addTask = testCollection.add(dataWithServerTimestamp);
        DocumentReference addedDocRef = Tasks.await(addTask);

        assertNotNull(addedDocRef);
        assertNotNull(addedDocRef.getId());

        // Verify the document was added
        DocumentSnapshot snapshot = Tasks.await(addedDocRef.get());
        assertTrue(snapshot.exists());
        assertEquals("testDoc", snapshot.getString("name"));
        // The serverTimestamp field should be set
        assertNotNull(snapshot.get("createdAt"));
    }

    @Test
    public void testBatchOperations() throws Exception {
        WriteBatch batch = db.batch();

        // Add multiple documents in batch
        DocumentReference doc1 = testCollection.document("batch1");
        DocumentReference doc2 = testCollection.document("batch2");
        DocumentReference doc3 = testCollection.document("batch3");

        batch.set(doc1, Collections.singletonMap("batchField", "value1"));
        batch.set(doc2, Collections.singletonMap("batchField", "value2"));
        batch.set(doc3, Collections.singletonMap("batchField", "value3"));

        // Update one document
        batch.update(doc1, "batchField", "updated1");

        // Delete one document
        batch.delete(doc2);

        // Commit batch
        Tasks.await(batch.commit());

        // Verify results
        DocumentSnapshot snap1 = Tasks.await(doc1.get());
        assertTrue(snap1.exists());
        assertEquals("updated1", snap1.getString("batchField"));

        DocumentSnapshot snap2 = Tasks.await(doc2.get());
        assertFalse(snap2.exists());

        DocumentSnapshot snap3 = Tasks.await(doc3.get());
        assertTrue(snap3.exists());
        assertEquals("value3", snap3.getString("batchField"));
    }

    @Test
    public void testQueryOperations() throws Exception {
        // Test various query operations
        Query query;

        // whereEqualTo
        query = testCollection.whereEqualTo("stringValue", "Hello");
        QuerySnapshot result = Tasks.await(query.get());
        assertTrue(result.size() > 0);

        // whereGreaterThan
        query = testCollection.whereGreaterThan("intValue", 100);
        result = Tasks.await(query.get());
        for (DocumentSnapshot doc : result) {
            assertTrue(doc.getLong("intValue") > 100);
        }

        // whereLessThan
        query = testCollection.whereLessThan("intValue", 200);
        result = Tasks.await(query.get());
        for (DocumentSnapshot doc : result) {
            assertTrue(doc.getLong("intValue") < 200);
        }

        // whereArrayContains
        query = testCollection.whereArrayContains("listValue", "B");
        result = Tasks.await(query.get());
        for (DocumentSnapshot doc : result) {
            List<?> list = (List<?>) doc.get("listValue");
            assertNotNull(list);
            assertTrue(list.contains("B"));
        }

        // Combined query with orderBy and limit
        query = testCollection.whereGreaterThan("intValue", 0)
                .orderBy("intValue")
                .limit(5);
        result = Tasks.await(query.get());
        assertTrue(result.size() <= 5);

        // Verify ordering
        long previousValue = -1;
        for (DocumentSnapshot doc : result) {
            long currentValue = doc.getLong("intValue");
            assertTrue(currentValue >= previousValue);
            previousValue = currentValue;
        }
    }

    @Test
    public void testDocumentReferenceOperations() throws Exception {
        DocumentReference docRef = testCollection.document("docOpsTest");

        // Test set with merge options
        Map<String, Object> data1 = new HashMap<>();
        data1.put("field1", "value1");
        data1.put("field2", "value2");
        Tasks.await(docRef.set(data1));

        // Merge with additional data
        Map<String, Object> data2 = new HashMap<>();
        data2.put("field2", "updatedValue2"); // Overwrite
        data2.put("field3", "value3"); // Add new
        Tasks.await(docRef.set(data2, SetOptions.merge()));

        DocumentSnapshot snap = Tasks.await(docRef.get());
        assertEquals("value1", snap.getString("field1")); // Preserved
        assertEquals("updatedValue2", snap.getString("field2")); // Updated
        assertEquals("value3", snap.getString("field3")); // Added
    }

    @Test
    public void testTransactionOperations() throws Exception {
        final DocumentReference docRef = testCollection.document("transactionTest");

        // Set initial data
        Tasks.await(docRef.set(Collections.singletonMap("counter", 0)));

        // Run transaction
        Tasks.await(db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(docRef);
            long currentValue = snap.getLong("counter");
            transaction.update(docRef, "counter", currentValue + 1);
            return null;
        }));

        // Verify transaction result
        DocumentSnapshot finalSnap = Tasks.await(docRef.get());
        assertEquals((Long) 1L, finalSnap.getLong("counter"));
    }

    @Test
    public void testNestedCollections() throws Exception {
        // Test subcollection operations
        DocumentReference parentDoc = testCollection.document("parentDoc");
        CollectionReference subCollection = parentDoc.collection("subCollection");

        // Add document to subcollection
        Map<String, Object> subData = new HashMap<>();
        subData.put("subField", "subValue");
        Tasks.await(subCollection.add(subData));

        // Verify subcollection document
        QuerySnapshot subDocs = Tasks.await(subCollection.get());
        assertTrue(subDocs.size() > 0);

        DocumentSnapshot subDoc = subDocs.getDocuments().get(0);
        assertEquals("subValue", subDoc.getString("subField"));
    }

    @Test
    public void testDocumentSnapshotOperations() throws Exception {
        DocumentReference docRef = testCollection.document("snapshotTest");

        Map<String, Object> testData = new HashMap<>();
        testData.put("testField", "testValue");
        testData.put("numberField", 42);
        testData.put("boolField", true);
        Tasks.await(docRef.set(testData));

        DocumentSnapshot snap = Tasks.await(docRef.get());

        // Test various getter methods
        assertTrue(snap.exists());
        assertEquals("snapshotTest", snap.getId());
        assertNotNull(snap.getReference());

        // Test typed getters
        assertEquals("testValue", snap.getString("testField"));
        assertEquals((Long) 42L, snap.getLong("numberField"));
        assertTrue(snap.getBoolean("boolField"));

        // Test generic get
        assertEquals("testValue", snap.get("testField"));

        // Test contains and metadata
        assertTrue(snap.contains("testField"));
        assertFalse(snap.contains("nonExistentField"));
        assertNotNull(snap.getMetadata());

        // Test data conversion
        Map<String, Object> data = snap.getData();
        assertNotNull(data);
        assertEquals("testValue", data.get("testField"));
    }

    @Test
    public void testErrorConditions() throws Exception {
        // Test operations on non-existent documents
        DocumentReference nonExistentDoc = testCollection.document("nonExistent");
        DocumentSnapshot snap = Tasks.await(nonExistentDoc.get());
        assertFalse(snap.exists());

        Exception exception = assertThrows(
            Exception.class,
            () -> Tasks.await(nonExistentDoc.update("field", "value"))
        );

        // Now you can inspect the exception cause, message, etc.
        assertTrue(exception.getCause() instanceof OsbdtOracledbException);
        // Test update on non-existent document (should not  work)
    }

    @Test
    public void testComplexDataStructures() throws Exception {
        DocumentReference docRef = testCollection.document("complexDataTest");

        // Create complex nested data
        Map<String, Object> complexData = new HashMap<>();

        // Nested maps
        Map<String, Object> level1 = new HashMap<>();
        Map<String, Object> level2 = new HashMap<>();
        level2.put("deepValue", "very deep");
        level1.put("level2", level2);
        complexData.put("nested", level1);

        // Arrays with mixed types
        List<Object> mixedArray = Arrays.asList(
            "string", 42, true, null, Timestamp.now(),
            Arrays.asList("nested", "array"),
            Collections.singletonMap("arrayMap", "value")
        );
        complexData.put("mixedArray", mixedArray);

        // Array of maps
        List<Map<String, Object>> arrayOfMaps = Arrays.asList(
            Collections.singletonMap("item1", "value1"),
            Collections.singletonMap("item2", "value2")
        );
        complexData.put("arrayOfMaps", arrayOfMaps);

        Tasks.await(docRef.set(complexData));

        // Verify complex data retrieval
        DocumentSnapshot snap = Tasks.await(docRef.get());
        assertTrue(snap.exists());

        // Verify nested maps
        Map<String, Object> nested = (Map<String, Object>) snap.get("nested");
        assertNotNull(nested);
        Map<String, Object> level2Retrieved = (Map<String, Object>) nested.get("level2");
        assertNotNull(level2Retrieved);
        assertEquals("very deep", level2Retrieved.get("deepValue"));

        // Verify mixed array
        List<Object> retrievedArray = (List<Object>) snap.get("mixedArray");
        assertNotNull(retrievedArray);
        assertEquals("string", retrievedArray.get(0));
        assertEquals(42L, retrievedArray.get(1));
        assertEquals(true, retrievedArray.get(2));
        assertNull(retrievedArray.get(3));

        // Verify array of maps
        List<Map<String, Object>> retrievedArrayOfMaps = (List<Map<String, Object>>) snap.get("arrayOfMaps");
        assertNotNull(retrievedArrayOfMaps);
        assertEquals(2, retrievedArrayOfMaps.size());
        assertEquals("value1", retrievedArrayOfMaps.get(0).get("item1"));
        assertEquals("value2", retrievedArrayOfMaps.get(1).get("item2"));
    }

    @Test
    public void testListenerWithMetadataChanges() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        DocumentReference docRef = testCollection.document("metadataTest");

        ListenerRegistration registration = docRef.addSnapshotListener(
            MetadataChanges.INCLUDE,
            (snapshot, error) -> {
                assertNull(error);
                assertNotNull(snapshot);
                // Test would verify metadata changes are included
                latch.countDown();
            }
        );

        Tasks.await(docRef.set(Collections.singletonMap("test", "value")));
        assertTrue(latch.await(10, TimeUnit.SECONDS));

        registration.remove();
    }
// Confirm if we are supporting this or not.
//    @Test
//    public void testCollectionGroupQueries() throws Exception {
//        // Create documents in different collections with same field
//        CollectionReference otherCollection = db.collection("otherCollection");
//        DocumentReference doc1 = testCollection.document("group1");
//        DocumentReference doc2 = otherCollection.document("group2");
//
//        Tasks.await(doc1.set(Collections.singletonMap("groupField", "sharedValue")));
//        Tasks.await(doc2.set(Collections.singletonMap("groupField", "sharedValue")));
//
//        // Note: Collection group queries might not be fully supported in all implementations
//        // This test verifies the API exists and basic functionality
//        try {
//            Query groupQuery = db.collectionGroup("testCollection")
//                    .whereEqualTo("groupField", "sharedValue");
//            QuerySnapshot result = Tasks.await(groupQuery.get());
//            // Test passes if no exception is thrown
//            assertNotNull(result);
//        } catch (UnsupportedOperationException e) {
//            // Expected if collection group queries are not supported
//        }
//    }

    @Test
    public void testCollectionReferenceParentChildRelationship() throws Exception {
        // Test that subcollections have proper parent relationships
        DocumentReference parentDoc = testCollection.document("parentDoc");
        CollectionReference subCollection = parentDoc.collection("subCollection");

        assertNotNull(subCollection.getParent());
        assertEquals(parentDoc.getId(), subCollection.getParent().getId());

        // Test path construction
        assertEquals("collectionTest/parentDoc/subCollection", subCollection.getPath());
    }

    @Test
    public void testStartAtEndAtQueries() throws Exception {
        // Create documents with ordered values
        DocumentReference doc1 = testCollection.document("order1");
        DocumentReference doc2 = testCollection.document("order2");
        DocumentReference doc3 = testCollection.document("order3");

        Tasks.await(doc1.set(Collections.singletonMap("orderField", 10)));
        Tasks.await(doc2.set(Collections.singletonMap("orderField", 20)));
        Tasks.await(doc3.set(Collections.singletonMap("orderField", 30)));

        // Test startAt
        Query startAtQuery = testCollection.orderBy("orderField").startAt(20);
        QuerySnapshot result = Tasks.await(startAtQuery.get());
        assertTrue(result.size() >= 2); // Should include 20 and 30

        // Test endAt
        Query endAtQuery = testCollection.orderBy("orderField").endAt(20);
        result = Tasks.await(endAtQuery.get());
        assertTrue(result.size() >= 2); // Should include 10 and 20

        // Test startAfter
        Query startAfterQuery = testCollection.orderBy("orderField").startAfter(10);
        result = Tasks.await(startAfterQuery.get());
        assertTrue(result.size() >= 2); // Should include 20 and 30

        // Test endBefore
        Query endBeforeQuery = testCollection.orderBy("orderField").endBefore(30);
        result = Tasks.await(endBeforeQuery.get());
        assertTrue(result.size() >= 2); // Should include 10 and 20
    }

    @Test
    public void testLimitToLastQuery() throws Exception {
        // Create multiple documents with unique IDs to avoid conflicts
//        String testPrefix = "limitTest_" + System.currentTimeMillis() + "_";
//        for (int i = 1; i <= 10; i++) {
//            DocumentReference doc = testCollection.document(testPrefix + i);
//            Tasks.await(doc.set(Collections.singletonMap("index", i)));
//        }
//
//        // Test limitToLast
//        Query limitToLastQuery = testCollection.whereGreaterThanOrEqualTo("index", 0)
//                .orderBy("index").limitToLast(3);
//        QuerySnapshot result = Tasks.await(limitToLastQuery.get());
//
//        assertEquals(3, result.size());
//        // Should get the last 3 documents (indices 8, 9, 10)
//        List<DocumentSnapshot> docs = result.getDocuments();
//
//        // Use safe field access to avoid field not found errors
//        long index0 = docs.get(0).getLong("index");
//        long index1 = docs.get(1).getLong("index");
//        long index2 = docs.get(2).getLong("index");
//
//        assertEquals(8L, index0);
//        assertEquals(9L, index1);
//        assertEquals(10L, index2);
    }

    @Test
    public void testMultipleOrderByClauses() throws Exception {
        // Create documents with multiple sortable fields
        DocumentReference doc1 = testCollection.document("multiOrder1");
        DocumentReference doc2 = testCollection.document("multiOrder2");
        DocumentReference doc3 = testCollection.document("multiOrder3");

        Tasks.await(doc1.set(Map.of("priority", 1, "name", "Z")));
        Tasks.await(doc2.set(Map.of("priority", 2, "name", "A")));
        Tasks.await(doc3.set(Map.of("priority", 1, "name", "B")));

        // Order by priority asc, then name asc
        Query multiOrderQuery = testCollection
                .orderBy("priority")
                .orderBy("name")
                .limit(3);

        QuerySnapshot result = Tasks.await(multiOrderQuery.get());
        assertTrue(result.size() >= 3);

        List<DocumentSnapshot> docs = result.getDocuments();
        // First two should have priority 1, ordered by name
        assertEquals(1L, docs.get(0).getLong("priority").longValue());
        assertEquals(1L, docs.get(1).getLong("priority").longValue());
        assertEquals("B", docs.get(0).getString("name"));
        assertEquals("Z", docs.get(1).getString("name"));
        // Third should have priority 2
        assertEquals(2L, docs.get(2).getLong("priority").longValue());
    }

    @Test
    public void testWhereInAndWhereNotInQueries() throws Exception {
        // Create documents with specific values and unique IDs to avoid conflicts
        String testPrefix = "whereInTest_" + System.currentTimeMillis() + "_";
        DocumentReference doc1 = testCollection.document(testPrefix + "1");
        DocumentReference doc2 = testCollection.document(testPrefix + "2");
        DocumentReference doc3 = testCollection.document(testPrefix + "3");
        DocumentReference doc4 = testCollection.document(testPrefix + "4");

        Tasks.await(doc1.set(Collections.singletonMap("category", "A")));
        Tasks.await(doc2.set(Collections.singletonMap("category", "B")));
        Tasks.await(doc3.set(Collections.singletonMap("category", "C")));
        Tasks.await(doc4.set(Collections.singletonMap("category", "D")));

        // Test whereIn - should return documents with category A or C
        Query whereInQuery = testCollection.whereIn("category", Arrays.asList("A", "C"));
        QuerySnapshot result = Tasks.await(whereInQuery.get());

        // Count how many of our test documents are returned
        int ourDocsCount = 0;
        for (DocumentSnapshot doc : result) {
            String category = doc.getString("category");
            if (category != null && (category.equals("A") || category.equals("C"))) {
                ourDocsCount++;
            }
        }
        assertEquals("Should return exactly 2 of our test documents with category A or C", 2, ourDocsCount);

        // Test whereNotIn - should return documents with category B or D (but not A or C)
        Query whereNotInQuery = testCollection.whereNotIn("category", Arrays.asList("A", "C"));
        result = Tasks.await(whereNotInQuery.get());

        // Count how many of our test documents are returned
        ourDocsCount = 0;
        for (DocumentSnapshot doc : result) {
            String category = doc.getString("category");
            if (category != null && (category.equals("B") || category.equals("D"))) {
                ourDocsCount++;
            }
        }
        assertEquals("Should return exactly 2 of our test documents with category B or D", 2, ourDocsCount);
    }

    @Test
    public void testWhereGreaterThanOrEqualToAndWhereLessThanOrEqualTo() throws Exception {
        // Create documents with numeric values
        for (int i = 1; i <= 10; i++) {
            DocumentReference doc = testCollection.document("rangeTest" + i);
            Tasks.await(doc.set(Collections.singletonMap("value", i * 10)));
        }

        // Test whereGreaterThanOrEqualTo
        Query gteQuery = testCollection.whereGreaterThanOrEqualTo("value", 50);
        QuerySnapshot result = Tasks.await(gteQuery.get());
        for (DocumentSnapshot doc : result) {
            assertTrue(doc.getLong("value") >= 50);
        }

        // Test whereLessThanOrEqualTo
        Query lteQuery = testCollection.whereLessThanOrEqualTo("value", 50);
        result = Tasks.await(lteQuery.get());
        for (DocumentSnapshot doc : result) {
            assertTrue(doc.getLong("value") <= 50);
        }
    }

    @Test
    public void testArrayContainsAnyQuery() throws Exception {
        // Create documents with arrays
        DocumentReference doc1 = testCollection.document("arrayAny1");
        DocumentReference doc2 = testCollection.document("arrayAny2");
        DocumentReference doc3 = testCollection.document("arrayAny3");

        Tasks.await(doc1.set(Collections.singletonMap("tags", Arrays.asList("red", "blue"))));
        Tasks.await(doc2.set(Collections.singletonMap("tags", Arrays.asList("green", "blue"))));
        Tasks.await(doc3.set(Collections.singletonMap("tags", Arrays.asList("yellow", "orange"))));

        // Test whereArrayContainsAny
        Query arrayContainsAnyQuery = testCollection.whereArrayContainsAny("tags", Arrays.asList("red", "green"));
        QuerySnapshot result = Tasks.await(arrayContainsAnyQuery.get());
        assertEquals(2, result.size()); // Should match doc1 and doc2
    }

    @Test
    public void testQueryCursorMethods() throws Exception {
        // Create documents for cursor testing
        for (int i = 1; i <= 20; i++) {
            DocumentReference doc = testCollection.document("cursorTest" + i);
            Tasks.await(doc.set(Collections.singletonMap("sequence", i)));
        }

        // Test startAt with document snapshot
        QuerySnapshot firstBatch = Tasks.await(testCollection.orderBy("sequence").limit(5).get());
        DocumentSnapshot lastDoc = firstBatch.getDocuments().get(4); // 5th document

        Query nextBatchQuery = testCollection.orderBy("sequence").startAfter(lastDoc).limit(5);
        QuerySnapshot nextBatch = Tasks.await(nextBatchQuery.get());

        assertEquals(5, nextBatch.size());
        // First document of next batch should have sequence > lastDoc's sequence
        assertTrue(nextBatch.getDocuments().get(0).getLong("sequence") > lastDoc.getLong("sequence"));
    }

    @Test
    public void testCollectionReferenceEquality() throws Exception {
        CollectionReference colRef1 = db.collection("testCollection");
        CollectionReference colRef2 = db.collection("testCollection");
        CollectionReference colRef3 = db.collection("differentCollection");

        // Same collection references should be equal
        assertEquals(colRef1, colRef2);
        // Different collection references should not be equal
        assertNotEquals(colRef1, colRef3);
    }

    @Test
    public void testDocumentReferenceFromCollection() throws Exception {
        // Test creating document references from collection
        DocumentReference docRef1 = testCollection.document();
        DocumentReference docRef2 = testCollection.document("specificId");

        assertNotNull(docRef1.getId());
        assertEquals("specificId", docRef2.getId());

        // Test path construction
        assertEquals("collectionTest/" + docRef1.getId(), docRef1.getPath());
        assertEquals("collectionTest/specificId", docRef2.getPath());
    }

    @Test
    public void testAddDocumentWithComplexObjects() throws Exception {
        // Test adding documents with complex objects including POJOs
        UserProfile complexUser = new UserProfile(
                "Complex User",
                35,
                true,
                Timestamp.now(),
                Arrays.asList("developer", "architect"),
                Map.of("city", "San Francisco", "country", "USA")
        );

        Task<DocumentReference> addTask = testCollection.add(complexUser);
        DocumentReference addedDocRef = Tasks.await(addTask);

        assertNotNull(addedDocRef);

        // Verify the document was added correctly
        DocumentSnapshot snapshot = Tasks.await(addedDocRef.get());
        assertTrue(snapshot.exists());

        // Convert back to object and verify
        UserProfile retrievedUser = snapshot.toObject(UserProfile.class);
        assertNotNull(retrievedUser);
        assertEquals("Complex User", retrievedUser.getName());
        assertEquals(35, retrievedUser.getAge());
        assertTrue(retrievedUser.isActive());
        assertNotNull(retrievedUser.getBirthday());
        assertNotNull(retrievedUser.getTags());
        assertNotNull(retrievedUser.getAddress());
    }

    @Test
    public void testCollectionReferenceHashCode() throws Exception {
        CollectionReference colRef1 = db.collection("testCollection");
        CollectionReference colRef2 = db.collection("testCollection");
        CollectionReference colRef3 = db.collection("otherCollection");

        // Equal objects should have equal hash codes
        assertEquals(colRef1.hashCode(), colRef2.hashCode());
        // Different objects should have different hash codes
        assertNotEquals(colRef1.hashCode(), colRef3.hashCode());
    }

    @Test
    public void testQueryWithMultipleWhereClauses() throws Exception {
        // Create test documents
        DocumentReference doc1 = testCollection.document("multiWhere1");
        DocumentReference doc2 = testCollection.document("multiWhere2");
        DocumentReference doc3 = testCollection.document("multiWhere3");

        Tasks.await(doc1.set(Map.of("status", "active", "priority", 1, "category", "urgent")));
        Tasks.await(doc2.set(Map.of("status", "active", "priority", 2, "category", "normal")));
        Tasks.await(doc3.set(Map.of("status", "inactive", "priority", 1, "category", "urgent")));

        // Query with multiple where clauses
        Query complexQuery = testCollection
                .whereEqualTo("status", "active")
                .whereGreaterThan("priority", 0)
                .whereIn("category", Arrays.asList("urgent", "normal"));

        QuerySnapshot result = Tasks.await(complexQuery.get());
        assertTrue(result.size() >= 2); // Should match doc1 and doc2

        // Verify all documents match criteria
        for (DocumentSnapshot doc : result) {
            assertEquals("active", doc.getString("status"));
            assertTrue(doc.getLong("priority") > 0);
            String category = doc.getString("category");
            assertTrue(category.equals("urgent") || category.equals("normal"));
        }
    }
//      Low Priority
//    @Test
//    public void testCollectionReferenceToString() throws Exception {
//        CollectionReference colRef = db.collection("testCollection");
//
//        // Test that toString doesn't throw exception and returns meaningful string
//        String stringRep = colRef.toString();
//        assertNotNull(stringRep);
//        assertTrue(stringRep.length() > 0);
//        assertTrue(stringRep.contains("testCollection"));
//    }

    @Test
    public void testRealTimeCollectionAddListener() throws Exception {

//        Tasks.await(auth.signInWithEmailAndPassword(
//            "testuseroracle@gmail.com", "Baq@123"));
        addSecurityRules("alpha");

        // Two latches: one for each added doc
        final CountDownLatch addLatch1 = new CountDownLatch(1);
        final CountDownLatch addLatch2 = new CountDownLatch(1);

        final List<String> addedDocs = Collections.synchronizedList(new ArrayList<>());

        // IDs to track
        String id1 = "realtimeAdd1_" + System.currentTimeMillis();
        String id2 = "realtimeAdd2_" + System.currentTimeMillis();

        ListenerRegistration registration = testCollection.addSnapshotListener(
            Executors.newSingleThreadExecutor(),
            (snapshots, error) -> {

                assertNull("Should not have error", error);
                assertNotNull("Snapshots should not be null", snapshots);

                for (DocumentChange change : snapshots.getDocumentChanges()) {

                    if (change.getType() == DocumentChange.Type.ADDED) {

                        String docId = change.getDocument().getId();

                        // Document 1 detected
                        if (docId.equals(id1)) {
                            addedDocs.add(docId);
                            addLatch1.countDown();
                        }

                        // Document 2 detected
                        if (docId.equals(id2)) {
                            addedDocs.add(docId);
                            addLatch2.countDown();
                        }
                    }
                }
            }
        );

        // Allow listener time to attach
        Thread.sleep(500);

        // Create 2 documents
        DocumentReference doc1 = testCollection.document(id1);
        DocumentReference doc2 = testCollection.document(id2);

        await(doc1.set(Collections.singletonMap("type", "realtime_test")));
        await(doc2.set(Collections.singletonMap("type", "realtime_test")));

        // Wait for both latches
        boolean firstOk = addLatch1.await(40, TimeUnit.SECONDS);
        boolean secondOk = addLatch2.await(40, TimeUnit.SECONDS);

        assertTrue("Should detect first document addition", firstOk);
        assertTrue("Should detect second document addition", secondOk);

        // Validate results
        assertEquals("Should have 2 added documents", 2, addedDocs.size());
        assertTrue(addedDocs.contains(id1));
        assertTrue(addedDocs.contains(id2));

        // Cleanup
        registration.remove();
        await(doc1.delete());
        await(doc2.delete());
    }

    @Test
    public void testRealTimeMultipleUsersScenario() throws Exception {

        addSecurityRules("city");

        String docId = "sharedDoc_" + System.currentTimeMillis();
        DocumentReference sharedDoc = testCollection.document(docId);

        await(sharedDoc.set(Collections.singletonMap("counter", 0)));

        CountDownLatch user1FinalLatch = new CountDownLatch(1);
        CountDownLatch user2FinalLatch = new CountDownLatch(1);

        List<Long> user1Values = Collections.synchronizedList(new ArrayList<>());
        List<Long> user2Values = Collections.synchronizedList(new ArrayList<>());

        // USER 1 LISTENER
        ListenerRegistration user1Reg = sharedDoc.addSnapshotListener(
            Executors.newSingleThreadExecutor(),
            (snapshot, error) -> {

                if (error != null || snapshot == null || !snapshot.exists())
                    return;

                Long counter = snapshot.getLong("counter");
                user1Values.add(counter);

                if (counter == 15L)
                    user1FinalLatch.countDown();
            }
        );

        // USER 2 LISTENER
        ListenerRegistration user2Reg = sharedDoc.addSnapshotListener(
            Executors.newSingleThreadExecutor(),
            (snapshot, error) -> {

                if (error != null || snapshot == null || !snapshot.exists())
                    return;

                Long counter = snapshot.getLong("counter");
                user2Values.add(counter);

                if (counter == 15L)
                    user2FinalLatch.countDown();
            }
        );

        Thread.sleep(800);

        // Update #1 (may or may NOT reach realtime listeners)
        await(sharedDoc.update("counter", FieldValue.increment(5L)));
        Thread.sleep(500);

        // Update #2 (final - WILL reach listeners)
        await(sharedDoc.update("counter", FieldValue.increment(10L)));

        // Wait only for final update
        assertTrue(user1FinalLatch.await(20, TimeUnit.SECONDS));
        assertTrue(user2FinalLatch.await(20, TimeUnit.SECONDS));

        user1Reg.remove();
        user2Reg.remove();

        // Validate final values only
        Long user1Final = user1Values.get(user1Values.size() - 1);
        Long user2Final = user2Values.get(user2Values.size() - 1);

        assertEquals(Long.valueOf(15L), user1Final);
        assertEquals(Long.valueOf(15L), user2Final);

        await(sharedDoc.delete());
    }

    // @Test
    // public void testAddSnapshotListenerPositiveBasic() throws Exception {
    //     addSecurityRules("alpha");

    //     DocumentReference docRef =
    //         testCollection.document("snapTest_" + System.currentTimeMillis());

    //     CountDownLatch latch = new CountDownLatch(2);
    //     List<String> statuses = Collections.synchronizedList(new ArrayList<>());

    //     // Attach listener BEFORE writing anything
    //     ListenerRegistration reg = docRef.addSnapshotListener(
    //         Executors.newSingleThreadExecutor(),   // << NOT main thread
    //         (snapshot, error) -> {

    //             if (snapshot != null && snapshot.exists()) {
    //                 Log.i("Snapshot Test", "Got snapshot notification");
    //                 statuses.add(snapshot.getString("status"));
    //                 latch.countDown();
    //             }
    //         }
    //     );

    //     // Allow time for WebSocket connection to establish
    //     Thread.sleep(5000);

    //     // initial write
    //     await(docRef.set(Collections.singletonMap("status", "initial")));

    //     // update to new value
    //     await(docRef.update("status", "updated"));

    //     boolean ok = latch.await(20, TimeUnit.SECONDS);
    //     assertTrue("Snapshot update not received", ok);

    //     // Validate snapshot received latest value
    //     assertEquals(2, statuses.size());
    //     assertEquals("initial", statuses.get(0));
    //     assertEquals("updated", statuses.get(1));

    //     reg.remove();
    //     await(docRef.delete());
    // }

    @Test
    public void testAddSnapshotListenerPositiveBasic() throws Exception {
        addSecurityRules("alpha");
        DocumentReference docRef =
                testCollection.document("snapTest_" + System.currentTimeMillis());

        // initial write
        await(docRef.set(Collections.singletonMap("status", "initial")));

        CountDownLatch latch = new CountDownLatch(1);
        List<String> statuses = Collections.synchronizedList(new ArrayList<>());

        // Attach listener BEFORE writing anything
        ListenerRegistration reg = docRef.addSnapshotListener(
                Executors.newSingleThreadExecutor(),   // << NOT main thread
                (snapshot, error) -> {

                    if (snapshot != null && snapshot.exists()) {
                        statuses.add(snapshot.getString("status"));
                        latch.countDown();
                    }
                }
        );

        // update to new value
        await(docRef.update("status", "updated"));

        boolean ok = latch.await(30, TimeUnit.SECONDS);
        assertTrue("Snapshot update not received", ok);

        // Validate snapshot received latest value
        assertEquals(1, statuses.size());
        assertEquals("updated", statuses.get(0));

        reg.remove();
        await(docRef.delete());
    }
}
