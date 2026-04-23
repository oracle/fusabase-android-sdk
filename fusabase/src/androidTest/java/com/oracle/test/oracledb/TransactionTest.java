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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static java.util.Collections.singletonMap;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.mobile.fusabase.auth.FusabaseAuth;
import com.oracle.mobile.fusabase.oracledb.CollectionReference;
import com.oracle.mobile.fusabase.oracledb.DocumentReference;
import com.oracle.mobile.fusabase.oracledb.DocumentSnapshot;
import com.oracle.mobile.fusabase.oracledb.FieldPath;
import com.oracle.mobile.fusabase.oracledb.FieldValue;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.oracledb.QuerySnapshot;
import com.oracle.mobile.fusabase.oracledb.SetOptions;
import com.oracle.mobile.fusabase.oracledb.Transaction;
import com.oracle.mobile.fusabase.oracledb.TransactionOptions;
import com.oracle.mobile.fusabase.oracledb.WriteBatch;
import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.models.IDCSOptions;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.Tasks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TransactionTest {

    private static FusabaseOracledb db;
    private static CollectionReference testCollection;

    private static final String projectId = "43C68B185E9732FEE0631F965E641907";
    private static final String schema = "user1";
    private static FusabaseAuth auth;
    private static final String ordsHost = "http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/";

    @BeforeClass
    public static void setUpClass() throws ExecutionException, InterruptedException {
        FusabaseOptions.Builder optionsBuilder = new FusabaseOptions.Builder();
        optionsBuilder.setAuthType("idcs");
        IDCSOptions.Builder idcsOptionsBuilder = new IDCSOptions.Builder();
        idcsOptionsBuilder.setClientId("44d4a5a58a80476e90389ab9f403db87");
        idcsOptionsBuilder.setClientSecret("idcscs-3a5ba1c4-b496-443d-ac9e-5efd7125d5c2");
        idcsOptionsBuilder.setDomainURL("https://idcs-360c1204d11f41d3836e7fb2f1769ca7.identity.oraclecloud.com");
        idcsOptionsBuilder.setSelfRegistrationProfile("f336e6256b9644059663394fe75d7494");
        optionsBuilder.setIDCSOptions(idcsOptionsBuilder.build());
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("43C68B185E9732FEE0631F965E641907");
        optionsBuilder.setAppId("43C6C814E61A4D68E0631F965E642FFE");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_CZXMDZFGRFGEITN");
        optionsBuilder.setApiVersion("2.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("43C6C6DC03224D43E0631F965E6456DC");
        optionsBuilder.setUseSocket(true);

        FusabaseApp app;

        try{
            app = FusabaseApp.getInstance();
        } catch (Exception e)
        {
            Log.w("DocumentReferenceTest", "Cannot find default instance. Creating the default instance.");
            app = FusabaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext()
                , optionsBuilder.build());
        }

        db = FusabaseOracledb.getInstance(app);
        auth = FusabaseAuth.getInstance(app);
        testCollection = db.collection("city");
//        Tasks.await(auth.signInWithEmailAndPassword("mohitv123@gmail.com", "MohitV@123"));
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
        }
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
        return Tasks.await(task);
    }

    // ------------------- Transaction Tests -------------------

    @Test
    public void testEnsureNoOverwriteDuringConcurrentUpdatesWithRunTransaction() throws Exception {
        addSecurityRules("city");
//        QuerySnapshot snapshot = await(testCollection.get());
//        if(snapshot != null) {
//            for (DocumentSnapshot doc : snapshot.getDocuments()) {
//                await(testCollection.document(doc.getId()).delete());
//            }
//        }
        DocumentReference cityRef = testCollection.document("LA");
        // Set initial data
        await(cityRef.set(new HashMap<String, Object>() {{
            put("name", "Los Angeles");
            put("state", "CA");
            put("country", "USA");
            put("capital", false);
            put("population", 4000000);
            put("regions", Arrays.asList("west_coast", "socal"));
        }}));

        // Run two concurrent transactions
        Task<Long> transaction1 = db.runTransaction(transaction -> {
            DocumentSnapshot doc = transaction.get(cityRef);
            transaction.update(cityRef, "population", doc.getLong("population") + 10000);
            return doc.getLong("population") + 10000;
        });

        Log.d("Test", "1 Transaction called");
        Task<Long> transaction2 = db.runTransaction(transaction -> {
            DocumentSnapshot doc = transaction.get(cityRef);
            transaction.update(cityRef, "population", doc.getLong("population") + 20000);
            return doc.getLong("population") + 20000;
        });

         Log.d("Test", "2 Transaction called");
        // Wait for both transactions to complete
        Long val1 = await(transaction1);
         Log.d("Test", "1 Transaction value " + val1);
        Long val2 = await(transaction2);
        Log.d("Test", "2 Transaction value " + val2);

        Log.d("Test", "Calling get");
        // Verify result
        DocumentSnapshot updatedDoc = await(cityRef.get());
        Log.d("OracleDbTests", "Updated document: " + updatedDoc.getData());

        assertTrue(updatedDoc.exists());
        assertEquals(4030000L, updatedDoc.getLong("population").longValue()); // Final population after concurrent updates

        // Clean up
//        await(cityRef.delete());
    }

    @Test
    public void testRunTransactionUpdateDocumentWhenNotExists() throws Exception {
        // Create a reference to the SF doc.
        addSecurityRules("city");
        DocumentReference sfDocRef = testCollection.document("SF");

        await(db.runTransaction(transaction -> {
            DocumentSnapshot sfDoc = transaction.get(sfDocRef);
            if (sfDoc.exists()) {
                throw new RuntimeException("Document exists!");
            }
            return null;
        }));

        Log.d("OracleDbTests", "Transaction successfully committed!");
    }

    @Test
    public void testSimpleTransactionSetAndGet_fixed() throws Exception {
        DocumentReference docRef = testCollection.document("txn1");

        // Do a proper read-before-write transaction.
        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot pre = transaction.get(docRef); // read first (ok if missing)
            transaction.set(docRef, singletonMap("count", 1));
            return null;
        }));

        // Verify after transaction completes
        DocumentSnapshot out = await(docRef.get());
        assertTrue(out.exists());
        assertEquals(Long.valueOf(1), out.getLong("count"));
    }

    // ---------- 2) UPDATE EXISTING VALUE ----------

    @Test
    public void testTransactionUpdate_fixed() throws Exception {
        addSecurityRules("city");
        DocumentReference docRef = testCollection.document("txn2");
        await(docRef.set(singletonMap("counter", 5)));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snap = transaction.get(docRef); // read first
            Long current = Objects.requireNonNull(snap.getLong("counter"));
            transaction.update(docRef, "counter", current + 1);
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertEquals(Long.valueOf(6), snapshot.getLong("counter"));
    }

    // ---------- 3) DELETE ----------

    @Test
    public void testTransactionDelete_fixed() throws Exception {
        DocumentReference docRef = testCollection.document("txn3");
        await(docRef.set(singletonMap("field", "value")));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            // read first is not strictly required before delete, but keep a read to follow the rule consistently
            transaction.get(docRef);
            transaction.delete(docRef);
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertFalse(snapshot.exists());
    }

    // ---------- 4) FAILURE ON MISSING DOC / UPDATE ----------

//    @Test
//    public void testTransactionFailureOnMissingDoc_fixed() {
//        DocumentReference docRef = testCollection.document("txn4");
//
//        try {
//            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
//                DocumentSnapshot snap = transaction.get(docRef); // read (will be missing)
//                assertFalse(snap.exists());
//                // Update on a non-existing doc should fail with NOT_FOUND
//                transaction.update(docRef, "missing", 1);
//                return null;
//            }));
//            fail("Expected exception when updating a missing document");
//        } catch (Exception e) {
//            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
//            assertTrue(msg.contains("not found") || msg.contains("no document to update"));
//        }
//    }

    // ---------- 5) VIOLATION: WRITE BEFORE READ (should fail) ----------

//    @Test
//    public void testTransactionWriteBeforeRead_disallowed() {
//        DocumentReference docRef = testCollection.document("txn_write_before_read");
//
//        try {
//            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
//                transaction.set(docRef, singletonMap("x", 1)); // write first (invalid)
//                transaction.get(docRef); // read after write (should throw)
//                return null;
//            }));
//        } catch (Exception e) {
//            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
//            assertTrue(
//                    msg.contains("all reads") || msg.contains("before any writes") || msg.contains("reads must be") // SDK wording varies
//            );
//        }
//    }

    // ---------- 6) CONCURRENCY / RETRIES: INCREMENT FROM MULTIPLE CLIENTS ----------

//    @Test
//    public void testTransactionConcurrency_fixed() throws Exception {
//        DocumentReference docRef = testCollection.document("txn5");
//        await(docRef.set(singletonMap("counter", 0)));
//
//        int threadCount = 4;
//        int incrementsPerThread = 5; // total expected = 20
//        CountDownLatch startGate = new CountDownLatch(1);
//        CountDownLatch doneGate = new CountDownLatch(threadCount);
//
//        for (int i = 0; i < threadCount; i++) {
//            new Thread(() -> {
//                try {
//                    startGate.await();
//                    for (int j = 0; j < incrementsPerThread; j++) {
//                        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
//                            DocumentSnapshot snap = transaction.get(docRef);
//                            Long val = Objects.requireNonNull(snap.getLong("counter"));
//                            transaction.update(docRef, "counter", val + 1);
//                            return null;
//                        }));
//                    }
//                } catch (Exception ignored) {
//                } finally {
//                    doneGate.countDown();
//                }
//            }).start();
//        }
//
//        startGate.countDown();
//        doneGate.await();
//
//        DocumentSnapshot snapshot = await(docRef.get());
//        assertEquals(Long.valueOf(threadCount * incrementsPerThread), snapshot.getLong("counter"));
//    }

    // ---------- 7) MULTI-DOC ATOMICITY (sum write) ----------

    @Test
    public void testTransactionWithMultipleOperations_fixed() throws Exception {
        DocumentReference doc1 = testCollection.document("txn6a");
        DocumentReference doc2 = testCollection.document("txn6b");

        await(doc1.set(singletonMap("val", 1)));
        await(doc2.set(singletonMap("val", 10)));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot s1 = transaction.get(doc1);
            DocumentSnapshot s2 = transaction.get(doc2);

            long v1 = Objects.requireNonNull(s1.getLong("val"));
            long v2 = Objects.requireNonNull(s2.getLong("val"));
            long sum = v1 + v2;

            transaction.update(doc1, "val", sum);
            transaction.update(doc2, "val", sum);
            return null;
        }));

        DocumentSnapshot snapshot1 = await(doc1.get());
        DocumentSnapshot snapshot2 = await(doc2.get());

        assertEquals(snapshot1.getLong("val"), snapshot2.getLong("val"));
        assertEquals(Long.valueOf(11), snapshot1.getLong("val"));
    }

    // ---------- 8) ROLLBACK / ABORT (throw inside transaction) ----------

    @Test
    public void testTransactionAbortRollsBack() throws Exception {
        DocumentReference docRef = testCollection.document("txn_abort");
        await(docRef.set(singletonMap("balance", 100)));

        try {
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentSnapshot snap = transaction.get(docRef);
                Long bal = Objects.requireNonNull(snap.getLong("balance"));

                // Decide to abort (e.g., insufficient funds)
                if (bal < 200) {
                    throw new IllegalStateException("Abort transaction");
                }

                // (would update if not aborted)
                transaction.update(docRef, "balance", bal - 200);
                return null;
            }));
            fail("Expected abort");
        } catch (Exception ignored) {
        }

        // Ensure no changes were committed
        DocumentSnapshot out = await(docRef.get());
        assertEquals(Long.valueOf(100), out.getLong("balance"));
    }

    // ---------- 9) INCREMENT FIELDVALUE (works, but still read first in txn) ----------

    @Test
    public void testTransactionFieldValueIncrement() throws Exception {
        DocumentReference docRef = testCollection.document("txn_inc");
        await(docRef.set(singletonMap("n", 0)));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Read something first to satisfy read-before-write
            transaction.get(docRef);
            transaction.update(docRef, "n", FieldValue.increment(3));
            return null;
        }));

        DocumentSnapshot out = await(docRef.get());
        assertEquals(Long.valueOf(3), out.getLong("n"));
    }

    // ---------- 10) READS ARE CONSISTENT SNAPSHOT ----------

    @Test
    public void testTransactionConsistentReadSnapshot() throws Exception {
        DocumentReference doc1 = testCollection.document("txn_consistent_1");
        DocumentReference doc2 = testCollection.document("txn_consistent_2");
        await(doc1.set(singletonMap("v", 1)));
        await(doc2.set(singletonMap("v", 2)));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Both reads happen before any writes; they represent a single consistent snapshot.
            DocumentSnapshot a = transaction.get(doc1);
            DocumentSnapshot b = transaction.get(doc2);
            Long va = Objects.requireNonNull(a.getLong("v"));
            Long vb = Objects.requireNonNull(b.getLong("v"));

            // Update both to the same max
            long max = Math.max(va, vb);
            transaction.update(doc1, "v", max);
            transaction.update(doc2, "v", max);
            return null;
        }));

        DocumentSnapshot out1 = await(doc1.get());
        DocumentSnapshot out2 = await(doc2.get());
        assertEquals(out1.getLong("v"), out2.getLong("v"));
        assertEquals(Long.valueOf(2), out1.getLong("v"));
    }

    // ------------------- Comprehensive Transaction Test Coverage -------------------

    @Test
    public void testTransactionSetWithOverwrite() throws Exception {
        DocumentReference docRef = testCollection.document("txn_set_overwrite");
        await(docRef.set(singletonMap("original", "value")));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.set(docRef, singletonMap("updated", "value"));
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue(snapshot.exists());
        assertFalse(snapshot.contains("original")); // Should be overwritten
        assertEquals("value", snapshot.getString("updated"));
    }

    @Test
    public void testTransactionSetWithMerge() throws Exception {
        DocumentReference docRef = testCollection.document("txn_set_merge");
        await(docRef.set(singletonMap("field1", "value1")));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.set(docRef, singletonMap("field2", "value2"), SetOptions.merge());
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue(snapshot.exists());
        assertEquals("value1", snapshot.getString("field1")); // Should be preserved
        assertEquals("value2", snapshot.getString("field2")); // Should be added
    }

    @Test
    public void testTransactionUpdateWithMap() throws Exception {
        DocumentReference docRef = testCollection.document("txn_update_map");
        await(docRef.set(singletonMap("field1", "value1")));

        Map<String, Object> updates = new HashMap<>();
        updates.put("field1", "updated1");
        updates.put("field2", "value2");

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.update(docRef, updates);
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertEquals("updated1", snapshot.getString("field1"));
        assertEquals("value2", snapshot.getString("field2"));
    }

    @Test
    public void testTransactionUpdateWithSingleField() throws Exception {
        DocumentReference docRef = testCollection.document("txn_update_single");
        await(docRef.set(singletonMap("counter", 5)));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.update(docRef, "counter", 10);
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertEquals(Long.valueOf(10), snapshot.getLong("counter"));
    }

    @Test
    public void testTransactionUpdateWithMultipleFields() throws Exception {
        DocumentReference docRef = testCollection.document("txn_update_multi");
        await(docRef.set(singletonMap("field1", "value1")));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.update(docRef, "field1", "updated1", "field2", "value2", "field3", 42);
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertEquals("updated1", snapshot.getString("field1"));
        assertEquals("value2", snapshot.getString("field2"));
        assertEquals(Long.valueOf(42), snapshot.getLong("field3"));
    }

    @Test
    public void testTransactionUpdateWithFieldPath() throws Exception {
        DocumentReference docRef = testCollection.document("txn_update_fieldpath");
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("nested", singletonMap("field", "oldValue"));
        await(docRef.set(initialData));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.update(docRef, FieldPath.of("nested", "field"), "newValue");
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) snapshot.get("nested");
        assertEquals("newValue", nested.get("field"));
    }

    @Test
    public void testTransactionUpdateWithFieldPathMultiple() throws Exception {
        DocumentReference docRef = testCollection.document("txn_update_fieldpath_multi");
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("stats", singletonMap("count", 0));
        await(docRef.set(initialData));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.update(docRef, FieldPath.of("stats", "count"), 5, "status", "active");
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) snapshot.get("stats");
        assertEquals(Long.valueOf(5), ((Map<String, Object>) snapshot.get("stats")).get("count"));
        assertEquals("active", snapshot.getString("status"));
    }

    @Test
    public void testTransactionDeleteExistingDocument() throws Exception {
        DocumentReference docRef = testCollection.document("txn_delete_existing");
        await(docRef.set(singletonMap("field", "value")));

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.delete(docRef);
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertFalse(snapshot.exists());
    }

    @Test
    public void testTransactionMultipleReadsAndWrites() throws Exception {
        DocumentReference doc1 = testCollection.document("txn_multi_1");
        DocumentReference doc2 = testCollection.document("txn_multi_2");
        DocumentReference doc3 = testCollection.document("txn_multi_3");

        await(doc1.set(singletonMap("val", 10)));
        await(doc2.set(singletonMap("val", 20)));
        // doc3 remains non-existent

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Multiple reads
            DocumentSnapshot s1 = transaction.get(doc1);
            DocumentSnapshot s2 = transaction.get(doc2);
            DocumentSnapshot s3 = transaction.get(doc3);

            // Multiple writes
            transaction.update(doc1, "val", s1.getLong("val") + 1);
            transaction.update(doc2, "val", s2.getLong("val") + 1);
            transaction.set(doc3, singletonMap("created", true));

            return null;
        }));

        DocumentSnapshot result1 = await(doc1.get());
        DocumentSnapshot result2 = await(doc2.get());
        DocumentSnapshot result3 = await(doc3.get());

        assertEquals(Long.valueOf(11), result1.getLong("val"));
        assertEquals(Long.valueOf(21), result2.getLong("val"));
        assertTrue(result3.getBoolean("created"));
    }

    @Test
    public void testTransactionReadNonExistentDocument() throws Exception {
        DocumentReference docRef = testCollection.document("txn_read_nonexistent");

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef);
            assertFalse(snapshot.exists());
            return null;
        }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransactionUpdateWithoutRead() throws Exception {
        DocumentReference docRef = testCollection.document("txn_update_no_read");

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            // This should fail because no read was performed first
            transaction.update(docRef, "field", "value");
            return null;
        }));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransactionDeleteNonExistentDocument() throws Exception {
        DocumentReference docRef = testCollection.document("txn_delete_nonexistent");

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read shows it doesn't exist
            transaction.delete(docRef); // This should fail
            return null;
        }));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransactionUpdateNonExistentDocument() throws Exception {
        DocumentReference docRef = testCollection.document("txn_update_nonexistent");

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read shows it doesn't exist
            transaction.update(docRef, "field", "value"); // This should fail
            return null;
        }));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransactionMergeOnNonExistentDocument() throws Exception {
        DocumentReference docRef = testCollection.document("txn_merge_nonexistent");

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read shows it doesn't exist
            transaction.set(docRef, singletonMap("field", "value"), SetOptions.merge()); // This should fail
            return null;
        }));
    }

    @Test
    public void testTransactionComplexDataStructures() throws Exception {
        DocumentReference docRef = testCollection.document("txn_complex_data");

        Map<String, Object> complexData = new HashMap<>();
        complexData.put("stringField", "test");
        complexData.put("numberField", 42);
        complexData.put("booleanField", true);
        complexData.put("nullField", null);
        complexData.put("arrayField", Arrays.asList("item1", "item2", "item3"));
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("innerField", "innerValue");
        nestedMap.put("innerNumber", 99);
        complexData.put("nestedObject", nestedMap);

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first (will be missing)
            transaction.set(docRef, complexData);
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue(snapshot.exists());

        Map<String, Object> data = snapshot.getData();
        assertEquals("test", data.get("stringField"));
        assertEquals(Long.valueOf(42), data.get("numberField"));
        assertEquals(true, data.get("booleanField"));
        assertNull(data.get("nullField"));

        @SuppressWarnings("unchecked")
        List<String> arrayField = (List<String>) data.get("arrayField");
        assertEquals(3, arrayField.size());
        assertEquals("item1", arrayField.get(0));

        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) data.get("nestedObject");
        assertEquals("innerValue", nested.get("innerField"));
        assertEquals(Long.valueOf(99), nested.get("innerNumber"));
    }

    @Test
    public void testTransactionWithEmptyData() throws Exception {
        DocumentReference docRef = testCollection.document("txn_empty_data");

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.set(docRef, new HashMap<String, Object>());
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue(snapshot.exists());
        assertTrue(snapshot.getData().isEmpty());
    }

    @Test
    public void testTransactionWithLargeData() throws Exception {
        DocumentReference docRef = testCollection.document("txn_large_data");

        Map<String, Object> largeData = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            largeData.put("field" + i, "value" + i);
        }

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.set(docRef, largeData);
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue(snapshot.exists());

        Map<String, Object> data = snapshot.getData();
        assertEquals(50, data.size());

        for (int i = 0; i < 50; i++) {
            assertEquals("value" + i, data.get("field" + i));
        }
    }

    @Test
    public void testTransactionWithSpecialCharacters() throws Exception {
        DocumentReference docRef = testCollection.document("txn_special_chars");

        Map<String, Object> data = new HashMap<>();
        data.put("field.with.dots", "dots");
        data.put("field with spaces", "spaces");
        data.put("field-with-dashes", "dashes");
        data.put("field_with_underscores", "underscores");

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.set(docRef, data);
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertEquals("dots", snapshot.getString("field.with.dots"));
        assertEquals("spaces", snapshot.getString("field with spaces"));
        assertEquals("dashes", snapshot.getString("field-with-dashes"));
        assertEquals("underscores", snapshot.getString("field_with_underscores"));
    }

    @Test
    public void testTransactionReturnValue() throws Exception {
        DocumentReference docRef = testCollection.document("txn_return_value");
        await(docRef.set(singletonMap("counter", 5)));

        Integer result = await(db.runTransaction((Transaction.Function<Integer>) transaction -> {
            DocumentSnapshot snap = transaction.get(docRef);
            Long current = snap.getLong("counter");
            transaction.update(docRef, "counter", current + 1);
            return current.intValue() + 1; // Return the new value
        }));

        assertEquals(Integer.valueOf(6), result);

        DocumentSnapshot snapshot = await(docRef.get());
        assertEquals(Long.valueOf(6), snapshot.getLong("counter"));
    }

    @Test
    public void testTransactionChaining() throws Exception {
        DocumentReference docRef = testCollection.document("txn_chaining");

        await(db.runTransaction((Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            // Test that all methods return Transaction for chaining
            Transaction result = transaction
                    .set(docRef, singletonMap("initial", "value"))
                    .update(docRef, "updated", true)
                    .update(docRef, "counter", 1);
            assertSame("Method chaining should return the same transaction instance", transaction, result);
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertEquals("value", snapshot.getString("initial"));
        assertEquals(true, snapshot.getBoolean("updated"));
        assertEquals(Long.valueOf(1), snapshot.getLong("counter"));
    }

    // ------------------- Constructor and Internal Structure Tests -------------------

    @Test
    public void testTransactionConstructor() throws Exception {
        // Use reflection to test package-private constructor
        Constructor<Transaction> constructor = Transaction.class.getDeclaredConstructor(
                FusabaseOracledb.class, TransactionOptions.class);
        constructor.setAccessible(true);

        TransactionOptions options = new TransactionOptions.Builder().build();
        Transaction transaction = constructor.newInstance(db, options);

        assertNotNull("Transaction should be created", transaction);

        // Verify internal state
        assertNotNull("Oradb should be set", getOradb(transaction));
        assertEquals("Options should match", options, getOptions(transaction));
        assertNotNull("Transaction name should be generated", getTransactionName(transaction));
    }

    @Test
    public void testTransactionUniqueNames() throws Exception {
        Constructor<Transaction> constructor = Transaction.class.getDeclaredConstructor(
                FusabaseOracledb.class, TransactionOptions.class);
        constructor.setAccessible(true);

        TransactionOptions options = new TransactionOptions.Builder().build();

        Transaction txn1 = constructor.newInstance(db, options);
        Transaction txn2 = constructor.newInstance(db, options);

        String name1 = getTransactionName(txn1);
        String name2 = getTransactionName(txn2);

        assertNotNull("First transaction name should not be null", name1);
        assertNotNull("Second transaction name should not be null", name2);
        assertNotEquals("Transaction names should be unique", name1, name2);
    }

    // ------------------- TransactionOptions Tests -------------------

    @Test
    public void testTransactionWithCustomOptions() throws Exception {
        TransactionOptions options = new TransactionOptions.Builder()
                .setMaxAttempts(5)
                .build();

        DocumentReference docRef = testCollection.document("txn_custom_options");

        await(db.runTransaction(options, (Transaction.Function<Void>) transaction -> {
            transaction.get(docRef); // Read first
            transaction.set(docRef, singletonMap("configured", true));
            return null;
        }));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue(snapshot.getBoolean("configured"));
    }

    @Test
    public void testTransactionOptionsBuilder() throws Exception {
        TransactionOptions.Builder builder = new TransactionOptions.Builder();
        TransactionOptions options = builder.setMaxAttempts(10).build();

        assertNotNull("TransactionOptions should be created", options);
        assertEquals("Max attempts should be set", 10, getMaxAttempts(options));
    }

    // ------------------- Helper Methods -------------------

    private FusabaseOracledb getOradb(Transaction transaction) throws Exception {
        java.lang.reflect.Field field = Transaction.class.getDeclaredField("oradb");
        field.setAccessible(true);
        return (FusabaseOracledb) field.get(transaction);
    }

    private TransactionOptions getOptions(Transaction transaction) throws Exception {
        java.lang.reflect.Field field = Transaction.class.getDeclaredField("options");
        field.setAccessible(true);
        return (TransactionOptions) field.get(transaction);
    }

    private String getTransactionName(Transaction transaction) throws Exception {
        java.lang.reflect.Field field = Transaction.class.getDeclaredField("transactionName");
        field.setAccessible(true);
        return (String) field.get(transaction);
    }

    private int getMaxAttempts(TransactionOptions options) throws Exception {
        java.lang.reflect.Field field = TransactionOptions.class.getDeclaredField("maxAttempts");
        field.setAccessible(true);
        return field.getInt(options);
    }
}
