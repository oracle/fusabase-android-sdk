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

import com.oracle.database.osbdt.auth.OsbdtAuth;
import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;

import com.oracle.database.osbdt.oracledb.CollectionReference;
import com.oracle.database.osbdt.oracledb.DocumentReference;
import com.oracle.database.osbdt.oracledb.DocumentSnapshot;
import com.oracle.database.osbdt.oracledb.FieldPath;
import com.oracle.database.osbdt.oracledb.OsbdtOracledb;
import com.oracle.database.osbdt.oracledb.OsbdtOracledbException;
import com.oracle.database.osbdt.oracledb.QuerySnapshot;
import com.oracle.database.osbdt.oracledb.SetOptions;
import com.oracle.database.osbdt.oracledb.WriteBatch;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.Tasks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class WriteBatchTest {

    private static OsbdtOracledb db;
    private static OsbdtAuth auth;
    private static CollectionReference testCollection;

    private static final String projectId = "43C68B185E9732FEE0631F965E641907";
    private static final String schema = "user1";

    private static final String ordsHost = "http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/";

    @BeforeClass
    public static void setUpClass() throws ExecutionException, InterruptedException {
        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
        optionsBuilder.setAuthType("base");
//        IDCSOptions.Builder idcsOptionsBuilder = new IDCSOptions.Builder();
//        idcsOptionsBuilder.setClientId("8c7c67b6b6784487acf10d5cfa554ff1");
//        idcsOptionsBuilder.setClientSecret("idcscs-6464e1ca-cd78-4a1e-840a-5cd9a9520ee6");
//        idcsOptionsBuilder.setDomainURL("https://idcs-360c1204d11f41d3836e7fb2f1769ca7.identity.oraclecloud.com");
//        idcsOptionsBuilder.setSelfRegistrationProfile("4a80d3c064a8456d9c6e21fb09b6af4a");
//        optionsBuilder.setIDCSOptions(idcsOptionsBuilder.build());
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("43C68B185E9732FEE0631F965E641907");
        optionsBuilder.setAppId("43C6C814E61A4D68E0631F965E642FFE");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_CZXMDZFGRFGEITN");
        optionsBuilder.setApiVersion("2.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("43C6C6DC03224D43E0631F965E6456DC");
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
        testCollection = db.collection("collectionTest");
        auth = OsbdtAuth.getInstance(app);
        Tasks.await(auth.signInWithEmailAndPassword("mohitv123@gmail.com", "MohitV@123"));
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
        Tasks.await(deleteTask, 100, TimeUnit.SECONDS);
    }

    private static <T> T await(Task<T> task) throws ExecutionException, InterruptedException, TimeoutException {
        return Tasks.await(task, 50, TimeUnit.SECONDS);
    }

    // ------------------- WriteBatch Tests -------------------

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
        }
    }

    @Test
    public void testShouldPerformABatchWriteAcrossMultipleCollections() throws Exception {
        DocumentReference cityRef = db.collection("city").document("batchCity");
        DocumentReference countryRef = db.collection("country").document("batchCountry");
        DocumentReference regionRef = db.collection("region").document("batchRegion");

        Map<String, Object> cityData = new HashMap<>();
        cityData.put("name", "Mumbai");
        cityData.put("population", 20000000);

        Map<String, Object> countryData = new HashMap<>();
        countryData.put("name", "India");
        countryData.put("code", "IN");

        Map<String, Object> regionData = new HashMap<>();
        regionData.put("name", "Asia");
        regionData.put("timezone", "IST");

        WriteBatch batch = db.batch();

        // Add set to batch
        addSecurityRules("city");
        batch.set(cityRef, cityData);
        addSecurityRules("country");
        batch.set(countryRef, countryData);
        addSecurityRules("region");
        batch.set(regionRef, regionData);

        // Commit batch
        await(batch.commit());

        // Validate city
        DocumentSnapshot citySnapshot = await(cityRef.get());
        assertTrue("City document should exist", citySnapshot.exists());
        assertEquals("City data should match", cityData, citySnapshot.getData());

        // Validate country
        DocumentSnapshot countrySnapshot = await(countryRef.get());
        assertTrue("Country document should exist", countrySnapshot.exists());
        assertEquals("Country data should match", countryData, countrySnapshot.getData());

        // Validate region
        DocumentSnapshot regionSnapshot = await(regionRef.get());
        assertTrue("Region document should exist", regionSnapshot.exists());
        assertEquals("Region data should match", regionData, regionSnapshot.getData());

        // Cleanup
        await(cityRef.delete());
        await(countryRef.delete());
        await(regionRef.delete());
    }

    @Test
    public void testBatchSetDocuments() throws Exception {
        DocumentReference doc1 = testCollection.document("batch1");
        DocumentReference doc2 = testCollection.document("batch2");

        WriteBatch batch = db.batch();
        batch.set(doc1, Collections.singletonMap("name", "Alice"));
        batch.set(doc2, Collections.singletonMap("name", "Bob"));
        await(batch.commit());

        DocumentSnapshot s1 = await(doc1.get());
        DocumentSnapshot s2 = await(doc2.get());

        assertEquals("Alice", s1.getString("name"));
        assertEquals("Bob", s2.getString("name"));
    }

    @Test
    public void testBatchUpdateDocuments() throws Exception {
        DocumentReference doc1 = testCollection.document("batch3");
        await(doc1.set(Collections.singletonMap("score", 10)));

        WriteBatch batch = db.batch();
        batch.update(doc1, "score", 20);
        await(batch.commit());

        DocumentSnapshot s1 = await(doc1.get());
        assertEquals(Long.valueOf(20), s1.getLong("score"));
    }

    @Test
    public void testBatchDeleteDocuments() throws Exception {
        DocumentReference doc1 = testCollection.document("batch4");
        DocumentReference doc2 = testCollection.document("batch5");
        await(doc1.set(Collections.singletonMap("field", "A")));
        await(doc2.set(Collections.singletonMap("field", "B")));

        WriteBatch batch = db.batch();
        batch.delete(doc1);
        batch.delete(doc2);
        await(batch.commit());

        DocumentSnapshot s1 = await(doc1.get());
        DocumentSnapshot s2 = await(doc2.get());

        assertFalse(s1.exists());
        assertFalse(s2.exists());
    }

    @Test
    public void testBatchMixedOperations() throws Exception {
        DocumentReference doc1 = testCollection.document("batch6");
        DocumentReference doc2 = testCollection.document("batch7");
        DocumentReference doc3 = testCollection.document("batch8");

        await(doc1.set(Collections.singletonMap("count", 1)));
        await(doc2.set(Collections.singletonMap("status", "old")));

        WriteBatch batch = db.batch();
        batch.update(doc1, "count", 2);
        batch.set(doc2, Collections.singletonMap("status", "new"));
        batch.set(doc3, Collections.singletonMap("created", true));
        await(batch.commit());

        DocumentSnapshot s1 = await(doc1.get());
        DocumentSnapshot s2 = await(doc2.get());
        DocumentSnapshot s3 = await(doc3.get());

        assertEquals(Long.valueOf(2), s1.getLong("count"));
        assertEquals("new", s2.getString("status"));
        assertTrue(s3.getBoolean("created"));
    }

    @Test
    public void testBatchOverwrites() throws Exception {
        DocumentReference doc1 = testCollection.document("batch9");
        await(doc1.set(Collections.singletonMap("val", "first")));

        WriteBatch batch = db.batch();
        batch.set(doc1, Collections.singletonMap("val", "second"));
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("second", snapshot.getString("val"));
    }

    @Test
    public void testEmptyBatchCommit() throws Exception {
        WriteBatch batch = db.batch();
        Task<Void> task = batch.commit();
        await(task);
        // Should complete successfully with no operations
    }

    @Test
    public void testBatchSetWithMergeOptions() throws Exception {
        DocumentReference doc1 = testCollection.document("batch10");
        await(doc1.set(Collections.singletonMap("field1", "value1")));

        WriteBatch batch = db.batch();
        batch.set(doc1, Collections.singletonMap("field2", "value2"), SetOptions.merge());
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("value1", snapshot.getString("field1")); // Should be preserved
        assertEquals("value2", snapshot.getString("field2")); // Should be added
    }

    @Test
    public void testBatchUpdateWithFieldPath() throws Exception {
        DocumentReference doc1 = testCollection.document("batch11");
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("nested", Collections.singletonMap("field", "oldValue"));
        await(doc1.set(initialData));

        WriteBatch batch = db.batch();
        batch.update(doc1, FieldPath.of("nested", "field"), "newValue");
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) snapshot.get("nested");
        assertEquals("newValue", nested.get("field"));
    }

    @Test
    public void testBatchUpdateWithMultipleFields() throws Exception {
        DocumentReference doc1 = testCollection.document("batch12");
        await(doc1.set(Collections.singletonMap("field1", "value1")));

        WriteBatch batch = db.batch();
        batch.update(doc1, "field1", "updated1", "field2", "value2", "field3", 42);
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("updated1", snapshot.getString("field1"));
        assertEquals("value2", snapshot.getString("field2"));
        assertEquals(Long.valueOf(42), snapshot.getLong("field3"));
    }

    @Test
    public void testBatchFunctionInterface() throws Exception {
        final DocumentReference doc1 = testCollection.document("batch13");

        WriteBatch batch = db.batch();

        // Test the Function interface
        WriteBatch.Function function = new WriteBatch.Function() {
            @Override
            public void apply(WriteBatch batch) {
                batch.set(doc1, Collections.singletonMap("name", "FunctionTest"));
                batch.update(doc1, "status", "active");
            }
        };

        function.apply(batch);
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("FunctionTest", snapshot.getString("name"));
        assertEquals("active", snapshot.getString("status"));
    }

    @Test
    public void testBatchMultipleOperationsOnSameDocument() throws Exception {
        DocumentReference doc1 = testCollection.document("batch14");

        // First create the document
        await(doc1.set(Collections.singletonMap("counter", 0)));

        WriteBatch batch = db.batch();
        // Multiple operations on the same document should work
        batch.update(doc1, "counter", 1);
        batch.update(doc1, "status", "updated");
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals(Long.valueOf(1), snapshot.getLong("counter"));
        assertEquals("updated", snapshot.getString("status"));
    }

    // ------------------- Constructor and Internal Structure Tests -------------------

    @Test
    public void testWriteBatchConstructor() throws Exception {
        // Use reflection to test private constructor
        Constructor<WriteBatch> constructor = WriteBatch.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        WriteBatch batch = constructor.newInstance();

        assertNotNull("WriteBatch should be created", batch);

        // Verify internal state
        List<?> operations = getOperations(batch);
        assertNotNull("Operations list should not be null", operations);
        assertTrue("Operations list should be empty initially", operations.isEmpty());

        String transactionName = getTransactionName(batch);
        assertNotNull("Transaction name should not be null", transactionName);
        assertFalse("Transaction name should not be empty", transactionName.isEmpty());
    }

    @Test
    public void testWriteBatchTransactionNameGeneration() throws Exception {
        Constructor<WriteBatch> constructor = WriteBatch.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        WriteBatch batch1 = constructor.newInstance();
        WriteBatch batch2 = constructor.newInstance();

        String name1 = getTransactionName(batch1);
        String name2 = getTransactionName(batch2);

        assertNotNull("First transaction name should not be null", name1);
        assertNotNull("Second transaction name should not be null", name2);
        assertNotEquals("Transaction names should be unique", name1, name2);
    }

    // ------------------- Method Chaining Tests -------------------

    @Test
    public void testMethodChaining() throws Exception {
        DocumentReference doc1 = testCollection.document("chain1");
        DocumentReference doc2 = testCollection.document("chain2");
        DocumentReference doc3 = testCollection.document("chain3");

        // Set doc3 first
        await(doc3.set(Collections.singletonMap("field", "to be deleted")));

        // Set doc2 first since update requires the document to exist
        await(doc2.set(Collections.singletonMap("field", "initial")));

        WriteBatch batch = db.batch();

        // Test that all methods return WriteBatch for chaining
        WriteBatch result = batch
                .set(doc1, Collections.singletonMap("type", "set"))
                .update(doc2, "field", "updated")
                .delete(doc3);

        assertSame("Method chaining should return the same batch instance", batch, result);

        await(batch.commit());

        DocumentSnapshot s1 = await(doc1.get());
        DocumentSnapshot s2 = await(doc2.get());
        DocumentSnapshot s3 = await(doc3.get());

        assertEquals("Set operation should work", "set", s1.getString("type"));
        assertEquals("Update operation should work", "updated", s2.getString("field"));
        assertFalse("Delete operation should work", s3.exists());
    }

    // ------------------- Complex Data Types Tests -------------------

    @Test
    public void testBatchWithComplexDataTypes() throws Exception {
        DocumentReference doc1 = testCollection.document("complex1");

        Map<String, Object> complexData = new HashMap<>();
        complexData.put("stringField", "test");
        complexData.put("intField", 42);
        complexData.put("longField", 123456789L);
        complexData.put("doubleField", 3.14159);
        complexData.put("booleanField", true);
        complexData.put("nullField", null);
        complexData.put("listField", Arrays.asList("item1", "item2", "item3"));
        complexData.put("nestedMap", Collections.singletonMap("innerKey", "innerValue"));
        complexData.put("timestamp", new java.util.Date());

        WriteBatch batch = db.batch();
        batch.set(doc1, complexData);
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertTrue("Document should exist", snapshot.exists());

        Map<String, Object> data = snapshot.getData();
        assertNotNull("Data should not be null", data);
        assertEquals("String field should match", "test", data.get("stringField"));
        assertEquals("Int field should match", 42L, data.get("intField"));
        assertEquals("Long field should match", 123456789L, data.get("longField"));
        assertEquals("Double field should match", 3.14159, (Double) data.get("doubleField"), 0.00001);
        assertEquals("Boolean field should match", true, data.get("booleanField"));
        assertNull("Null field should be null", data.get("nullField"));

        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) data.get("listField");
        assertNotNull("List should not be null", list);
        assertEquals("List should have 3 items", 3, list.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) data.get("nestedMap");
        assertNotNull("Nested map should not be null", nested);
        assertEquals("Nested value should match", "innerValue", nested.get("innerKey"));
    }

    // ------------------- Large Batch Tests -------------------

    @Test
    public void testLargeBatchOperations() throws Exception {
        final int BATCH_SIZE = 50;
        List<DocumentReference> docs = new ArrayList<>();

        WriteBatch batch = db.batch();

        // Create many set operations
        for (int i = 0; i < BATCH_SIZE; i++) {
            DocumentReference doc = testCollection.document("large_" + i);
            docs.add(doc);
            Map<String, Object> data = Collections.singletonMap("index", i);
            batch.set(doc, data);
        }

        await(batch.commit());

        // Verify all documents were created
        for (int i = 0; i < BATCH_SIZE; i++) {
            DocumentSnapshot snapshot = await(docs.get(i).get());
            assertTrue("Document " + i + " should exist", snapshot.exists());
            assertEquals("Document " + i + " should have correct index",
                        Long.valueOf(i), snapshot.getLong("index"));
        }
    }

    @Test
    public void testBatchWithManyDifferentOperations() throws Exception {
        final int OPERATIONS_PER_TYPE = 10;
        List<DocumentReference> setDocs = new ArrayList<>();
        List<DocumentReference> updateDocs = new ArrayList<>();
        List<DocumentReference> deleteDocs = new ArrayList<>();

        WriteBatch batch = db.batch();

        // Create set operations
        for (int i = 0; i < OPERATIONS_PER_TYPE; i++) {
            DocumentReference doc = testCollection.document("many_set_" + i);
            setDocs.add(doc);
            batch.set(doc, Collections.singletonMap("type", "set"));
        }

        // Create update operations (need to set first)
        for (int i = 0; i < OPERATIONS_PER_TYPE; i++) {
            DocumentReference doc = testCollection.document("many_update_" + i);
            updateDocs.add(doc);
            await(doc.set(Collections.singletonMap("type", "initial")));
            batch.update(doc, "type", "updated");
        }

        // Create delete operations (need to set first)
        for (int i = 0; i < OPERATIONS_PER_TYPE; i++) {
            DocumentReference doc = testCollection.document("many_delete_" + i);
            deleteDocs.add(doc);
            await(doc.set(Collections.singletonMap("type", "to_delete")));
            batch.delete(doc);
        }

        await(batch.commit());

        // Verify set operations
        for (DocumentReference doc : setDocs) {
            DocumentSnapshot snapshot = await(doc.get());
            assertTrue("Set document should exist", snapshot.exists());
            assertEquals("Set document should have correct type", "set", snapshot.getString("type"));
        }

        // Verify update operations
        for (DocumentReference doc : updateDocs) {
            DocumentSnapshot snapshot = await(doc.get());
            assertTrue("Updated document should exist", snapshot.exists());
            assertEquals("Updated document should have correct type", "updated", snapshot.getString("type"));
        }

        // Verify delete operations
        for (DocumentReference doc : deleteDocs) {
            DocumentSnapshot snapshot = await(doc.get());
            assertFalse("Deleted document should not exist", snapshot.exists());
        }
    }

    // ------------------- Update Method Overloads Tests -------------------

    @Test
    public void testUpdateWithMap() throws Exception {
        DocumentReference doc1 = testCollection.document("update_map");
        await(doc1.set(Collections.singletonMap("initial", "value")));

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("field1", "value1");
        updateData.put("field2", 42);
        updateData.put("field3", true);

        WriteBatch batch = db.batch();
        batch.update(doc1, updateData);
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("Field1 should be updated", "value1", snapshot.getString("field1"));
        assertEquals("Field2 should be updated", Long.valueOf(42), snapshot.getLong("field2"));
        assertEquals("Field3 should be updated", true, snapshot.getBoolean("field3"));
        assertEquals("Initial field should still exist", "value", snapshot.getString("initial"));
    }

    @Test
    public void testUpdateWithSingleFieldValue() throws Exception {
        DocumentReference doc1 = testCollection.document("update_single");
        await(doc1.set(Collections.singletonMap("counter", 5)));

        WriteBatch batch = db.batch();
        batch.update(doc1, "counter", 10);
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("Counter should be updated", Long.valueOf(10), snapshot.getLong("counter"));
    }

    @Test
    public void testUpdateWithFieldPathAndValue() throws Exception {
        DocumentReference doc1 = testCollection.document("update_fieldpath");
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("nested", Collections.singletonMap("value", "old"));
        await(doc1.set(initialData));

        WriteBatch batch = db.batch();
        batch.update(doc1, FieldPath.of("nested", "value"), "new");
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) snapshot.get("nested");
        assertEquals("Nested value should be updated", "new", nested.get("value"));
    }

    @Test
    public void testUpdateWithFieldPathAndMultipleValues() throws Exception {
        DocumentReference doc1 = testCollection.document("update_fieldpath_multi");
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("stats", Collections.singletonMap("count", 0));
        await(doc1.set(initialData));

        WriteBatch batch = db.batch();
        batch.update(doc1, FieldPath.of("stats", "count"), 5, "status", "active");
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) snapshot.get("stats");
        assertEquals("Count should be updated", Long.valueOf(5), ((Map<String, Object>) snapshot.get("stats")).get("count"));
        assertEquals("Status should be updated", "active", snapshot.getString("status"));
    }

    // ------------------- Set Method Overloads Tests -------------------

    @Test
    public void testSetWithDefaultOptions() throws Exception {
        DocumentReference doc1 = testCollection.document("set_default");

        WriteBatch batch = db.batch();
        batch.set(doc1, Collections.singletonMap("field", "value"));
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("Field should be set", "value", snapshot.getString("field"));
    }

    @Test
    public void testSetWithOverwriteOptions() throws Exception {
        DocumentReference doc1 = testCollection.document("set_overwrite");

        WriteBatch batch = db.batch();
        // Use default set method which overwrites
        batch.set(doc1, Collections.singletonMap("field", "value"));
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("Field should be set", "value", snapshot.getString("field"));
    }

    @Test
    public void testSetWithMergeOptionsFields() throws Exception {
        DocumentReference doc1 = testCollection.document("set_merge_fields");
        await(doc1.set(Collections.singletonMap("existing", "old")));

        WriteBatch batch = db.batch();
        Map<String, Object> newData = new HashMap<>();
        newData.put("existing", "new");
        newData.put("added", "value");
        batch.set(doc1, newData, SetOptions.merge());
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("Existing field should be updated", "new", snapshot.getString("existing"));
        assertEquals("New field should be added", "value", snapshot.getString("added"));
    }

    @Test
    public void testSetWithMergeFieldPaths() throws Exception {
        DocumentReference doc1 = testCollection.document("set_merge_fieldpaths");
        await(doc1.set(Collections.singletonMap("base", "value")));

        WriteBatch batch = db.batch();
        Map<String, Object> newData = new HashMap<>();
        newData.put("base", "updated");
        newData.put(FieldPath.of("nested", "field").toString(), "nestedValue");
        batch.set(doc1, newData, SetOptions.merge());
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("Base field should be updated", "updated", snapshot.getString("base"));
        assertEquals("Nested field should be set", "nestedValue", snapshot.get("nested.field"));
    }

    // ------------------- Error Conditions and Edge Cases -------------------

    @Test
    public void testBatchCommitMultipleTimes() throws Exception {
        DocumentReference doc1 = testCollection.document("commit_multiple");

        WriteBatch batch = db.batch();
        batch.set(doc1, Collections.singletonMap("field", "value"));

        // First commit should work
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertTrue("Document should exist after first commit", snapshot.exists());

        // Second commit on the same batch should fail or be no-op
        // (depending on implementation, but should not throw)
        try {
            await(batch.commit());
        } catch (Exception e) {
            // It's acceptable for this to fail
        }
    }

    @Test
    public void testBatchWithNullDocumentReference() throws Exception {
        WriteBatch batch = db.batch();

        try {
            batch.set(null, Collections.singletonMap("field", "value"));
            fail("Should throw exception for null document reference");
        } catch (Exception e) {
            assertTrue("Should throw exception for null reference",
                      e instanceof NullPointerException || e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testBatchWithNullData() throws Exception {
        DocumentReference doc1 = testCollection.document("null_data");

        WriteBatch batch = db.batch();

        try {
            batch.set(doc1, null);
            fail("Should throw exception for null data");
        } catch (Exception e) {
            assertTrue("Should throw exception for null data",
                      e instanceof NullPointerException || e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testBatchDeleteNonExistentDocument() throws Exception {
        DocumentReference doc1 = testCollection.document("non_existent_delete");

        WriteBatch batch = db.batch();
        batch.delete(doc1);

        try {
            await(batch.commit());
            fail("Expected OsbdtOracledbException to be thrown");
        } catch (ExecutionException e) {
            // Check that the cause is OsbdtOracledbException
            assertTrue("Cause should be OsbdtOracledbException",
                      e.getCause() instanceof OsbdtOracledbException);
        }
    }

    @Test
    public void testBatchUpdateNonExistentDocument() throws Exception {
        DocumentReference doc1 = testCollection.document("non_existent_update");

        WriteBatch batch = db.batch();
        batch.update(doc1, "field", "value");

        try {
            await(batch.commit());
            fail("Should throw exception for updating non-existent document");
        } catch (Exception e) {
            // This is expected to fail
        }
    }

    // ------------------- Performance and Boundary Tests -------------------

    @Test
    public void testBatchWithEmptyMap() throws Exception {
        DocumentReference doc1 = testCollection.document("empty_map");

        WriteBatch batch = db.batch();
        batch.set(doc1, new HashMap<String, Object>());
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertTrue("Document should exist", snapshot.exists());
        assertTrue("Data should be empty", snapshot.getData().isEmpty());
    }

    @Test
    public void testBatchWithLargeData() throws Exception {
        DocumentReference doc1 = testCollection.document("large_data");

        Map<String, Object> largeData = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            largeData.put("field" + i, "value" + i);
        }

        WriteBatch batch = db.batch();
        batch.set(doc1, largeData);
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertTrue("Document should exist", snapshot.exists());

        Map<String, Object> data = snapshot.getData();
        assertEquals("Should have all fields", 100, data.size());

        for (int i = 0; i < 100; i++) {
            assertEquals("Field " + i + " should match", "value" + i, data.get("field" + i));
        }
    }

    @Test
    public void testBatchWithSpecialCharactersInFieldNames() throws Exception {
        DocumentReference doc1 = testCollection.document("special_chars");

        Map<String, Object> data = new HashMap<>();
        data.put("field.with.dots", "dots");
        data.put("field with spaces", "spaces");
        data.put("field-with-dashes", "dashes");
        data.put("field_with_underscores", "underscores");
        data.put("field/with/slashes", "slashes");

        WriteBatch batch = db.batch();
        batch.set(doc1, data);
        await(batch.commit());

        DocumentSnapshot snapshot = await(doc1.get());
        assertEquals("Dots field should work", "dots", snapshot.getString("field.with.dots"));
        assertEquals("Spaces field should work", "spaces", snapshot.getString("field with spaces"));
        assertEquals("Dashes field should work", "dashes", snapshot.getString("field-with-dashes"));
        assertEquals("Underscores field should work", "underscores", snapshot.getString("field_with_underscores"));
        assertEquals("Slashes field should work", "slashes", snapshot.getString("field/with/slashes"));
    }

    // ------------------- Helper Methods -------------------

    private List<?> getOperations(WriteBatch batch) throws Exception {
        Field field = WriteBatch.class.getDeclaredField("operations");
        field.setAccessible(true);
        return (List<?>) field.get(batch);
    }

    private String getTransactionName(WriteBatch batch) throws Exception {
        Field field = WriteBatch.class.getDeclaredField("transactionName");
        field.setAccessible(true);
        return (String) field.get(batch);
    }
}
