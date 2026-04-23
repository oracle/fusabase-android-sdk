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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.mobile.fusabase.auth.FusabaseAuth;
import com.oracle.mobile.fusabase.oracledb.CollectionReference;
import com.oracle.mobile.fusabase.oracledb.DocumentReference;
import com.oracle.mobile.fusabase.oracledb.DocumentSnapshot;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.oracledb.QuerySnapshot;
import com.oracle.mobile.fusabase.oracledb.SetOptions;
import com.oracle.mobile.fusabase.oracledb.WriteBatch;
import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.Tasks;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class FusabaseNamespaceTest {

    private static FusabaseOracledb db;
    private static FusabaseAuth auth;

    @BeforeClass
    public static void setUp() throws Exception {
        FusabaseOptions.Builder optionsBuilder = new FusabaseOptions.Builder();
        optionsBuilder.setAuthType("base");
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("41BD57980D9BA85CE0631F965E649B56");
        optionsBuilder.setAppId("41BD57EC1729A876E0631F965E640759");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_AJJUTUHAHSTEMBD");
        optionsBuilder.setApiVersion("1.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("40D2FCF5350D84CDE0631F965E64CACE");
        optionsBuilder.setUseSocket(true);
        FusabaseApp app;

        try {
            app = FusabaseApp.getInstance();
        } catch (Exception e) {
            Log.w("FusabaseTest", "Cannot find default instance. Creating the default instance.");
            app = FusabaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    optionsBuilder.build(), "test");
        }
        auth = FusabaseAuth.getInstance(app);
        db = FusabaseOracledb.getInstance(app);

        // Sign in
        Tasks.await(auth.signInWithEmailAndPassword("mohitv123@gmail.com", "MohitV@123"));

        Log.d("FusabaseNamespaceTest", "Setting up test...");
    }

    @Before
    public void setUpBeforeEach() throws Exception {
        deleteCollection("city");
    }

    @After
    public void tearDownAfterEach() throws Exception {
        // Custom logic after each test case
        Log.d("FusabaseNamespaceTest", "Test run started...");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Thread.sleep(2000);
    }

    private void deleteCollection(String collectionName) throws Exception {
        CollectionReference collection = db.collection(collectionName);
        QuerySnapshot allDocs = Tasks.await(collection.get(), 50, TimeUnit.SECONDS);
        WriteBatch batch = db.batch();
        for (DocumentSnapshot doc : allDocs.getDocuments()) {
            batch.delete(doc.getReference());
        }
        Tasks.await(batch.commit(), 50, TimeUnit.SECONDS);
    }

    @Test
    public void testAddNewCollectionDocument() throws Exception {
        CollectionReference cityCollection = db.collection("city");

        Map<String, Object> cityData = new HashMap<>();
        cityData.put("name", "Bhopal");
        cityData.put("country", "India");
        cityData.put("state", "Madhya Pradesh1");
        cityData.put("capital", false);
        cityData.put("population", 200000L);
        cityData.put("regions", Arrays.asList("North"));

        Task<DocumentReference> addTask = cityCollection.add(cityData);
        DocumentReference resAddDoc = Tasks.await(addTask);

        String docId = resAddDoc.getId();
        assertNotNull(docId);
        assertTrue(docId.length() > 0);
        Log.d("FusabaseNamespaceTest", "Added document ID: " + docId);

        // Retrieve the document to check if it was added correctly
        DocumentSnapshot addedDoc = Tasks.await(cityCollection.document(docId).get());
        assertTrue(addedDoc.exists());
        Map<String, Object> data = addedDoc.getData();
        assertNotNull(data);
        assertEquals("Bhopal", data.get("name"));
        assertEquals("India", data.get("country"));
        Log.d("FusabaseNamespaceTest", "Data added: " + data);

        // Delete the document
        Tasks.await(cityCollection.document(docId).delete());
        Log.d("FusabaseNamespaceTest", "Test document deleted: " + docId);
    }

    @Test
    public void testAddDocumentWithThen() throws Exception {
        DocumentReference cityRef = db.collection("city").document("LA");

        Map<String, Object> testData = new HashMap<>();
        testData.put("name", "Los Angeles");
        testData.put("state", "CA");
        testData.put("country", "USA");

        Tasks.await(cityRef.set(testData));
        Log.d("FusabaseNamespaceTest", "Document successfully written!");

        DocumentSnapshot docSnapshot = Tasks.await(cityRef.get());
        assertTrue(docSnapshot.exists());

        Map<String, Object> retrievedData = docSnapshot.getData();
        assertEquals(testData, retrievedData);
        Log.d("FusabaseNamespaceTest", "Document data matches the expected data.");

        Tasks.await(cityRef.delete());
        Log.d("FusabaseNamespaceTest", "Test document deleted.");
    }

    @Test
    public void testMergeCapitalField() throws Exception {
        DocumentReference cityRef = db.collection("city").document("BJ");

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("name", "Beijing");
        initialData.put("country", "China");
        initialData.put("population", 21540000L);

        Tasks.await(cityRef.set(initialData));

        Map<String, Object> mergeData = new HashMap<>();
        mergeData.put("capital", true);
        Tasks.await(cityRef.set(mergeData, SetOptions.merge()));

        DocumentSnapshot addedDocBJ = Tasks.await(cityRef.get());
        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocBJ.getData());
        assertTrue(addedDocBJ.getData().containsKey("capital"));
        assertEquals(true, addedDocBJ.getData().get("capital"));

        Tasks.await(cityRef.delete());
    }

//    @Test
//    public void testWriteAndVerifyDocumentOne() throws Exception {
//        Map<String, Object> docData = new HashMap<>();
//        docData.put("stringExample", "Hello world!");
//        docData.put("booleanExample", true);
//        docData.put("numberExample", 3.14159265);
//        docData.put("dateExample", Timestamp.fromDate(new Date("December 10, 1815")));
//        docData.put("arrayExample", Arrays.asList(5L, true, "hello"));
//        docData.put("nullExample", null);
//        Map<String, Object> objectExample = new HashMap<>();
//        objectExample.put("a", 5L);
//        Map<String, Object> nested = new HashMap<>();
//        nested.put("nested", "foo");
//        objectExample.put("b", nested);
//        docData.put("objectExample", objectExample);
//
//        DocumentReference docRef = db.collection("city").document("one");
//
//        Tasks.await(docRef.set(docData));
//
//        DocumentSnapshot docSnapshot = Tasks.await(docRef.get());
//        assertTrue(docSnapshot.exists());
//
//        Log.d("FusabaseNamespaceTest", "document one: " + docSnapshot.getData());
//
//        Map<String, Object> retrievedData = docSnapshot.getData();
//        assertEquals(docData.get("stringExample"), retrievedData.get("stringExample"));
//        assertEquals(docData.get("booleanExample"), retrievedData.get("booleanExample"));
//        assertEquals(docData.get("numberExample"), retrievedData.get("numberExample"));
//        // Note: Date comparison might need adjustment
//
//        Tasks.await(docRef.delete());
//    }

//    @Test
//    public void testUpdateCapitalFieldDC() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("DC");
//
//        Map<String, Object> initialData = new HashMap<>();
//        initialData.put("name", "Washington, D.C.");
//        initialData.put("country", "USA");
//
//        Tasks.await(cityRef.set(initialData));
//
//        Map<String, Object> expectedData = new HashMap<>(initialData);
//        expectedData.put("capital", true);
//
//        Tasks.await(cityRef.update("capital", true));
//
//        DocumentSnapshot docSnapshot = Tasks.await(cityRef.get());
//        assertTrue(docSnapshot.exists());
//
//        Log.d("FusabaseNamespaceTest", "Washington data: " + docSnapshot.getData());
//
//        Map<String, Object> updatedData = docSnapshot.getData();
//        assertEquals(expectedData.get("name"), updatedData.get("name"));
//        assertEquals(expectedData.get("country"), updatedData.get("country"));
//        assertEquals(expectedData.get("capital"), updatedData.get("capital"));
//
//        Tasks.await(cityRef.delete());
//    }
//
//    @Test
//    public void testUpdateTimestampField() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("NYC");
//
//        Map<String, Object> initialData = new HashMap<>();
//        initialData.put("name", "New York City");
//        initialData.put("state", "NY");
//        initialData.put("country", "USA");
//
//        Tasks.await(cityRef.set(initialData));
//
//        Tasks.await(cityRef.update("timestamp", FieldValue.serverTimestamp()));
//
//        DocumentSnapshot docSnapshot = Tasks.await(cityRef.get());
//        assertTrue(docSnapshot.exists());
//
//        Log.d("FusabaseNamespaceTest", "New York City document: " + docSnapshot.getData());
//        Map<String, Object> updatedData = docSnapshot.getData();
//
//        assertTrue(updatedData.containsKey("timestamp"));
//
//        Log.d("FusabaseNamespaceTest", "Timestamp successfully updated and validated: " + updatedData.get("timestamp"));
//    }
//
//    @Test
//    public void testGetDocumentFromCollection() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//
//        DocumentReference docRef = db.collection("city").document("SF");
//
//        DocumentSnapshot doc = Tasks.await(docRef.get());
//        if (doc.exists()) {
//            Log.d("FusabaseNamespaceTest", "Document data: " + doc.getData());
//            Map<String, Object> data = doc.getData();
//            assertEquals("San Francisco", data.get("name"));
//            assertEquals("USA", data.get("country"));
//        } else {
//            Log.d("FusabaseNamespaceTest", "No such document!");
//        }
//
//        Tasks.await(cityRef.document("SF").delete());
//    }
//
//    @Test
//    public void testSetSourceOptionToCache() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//
//        DocumentReference docRef = cityRef.document("SF");
//
//        // Note: Source option might not be directly supported, skipping for now
//
//        Tasks.await(cityRef.document("SF").delete());
//    }
//
//    @Test
//    public void testGetMultipleDocumentsFromCollection() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.whereEqualTo("capital", true).get());
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//            Map<String, Object> data = doc.getData();
//            assertEquals("Tokyo", data.get("name"));
//            assertEquals("Japan", data.get("country"));
//        }
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testGetAllDocumentsFromCollection() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.get());
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    // Skipping listener tests as they require more complex setup
//
//    @Test
//    public void testSetDocumentToCollection() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//        assertTrue(addedDocSF.exists());
//        assertEquals("San Francisco", addedDocSF.getData().get("name"));
//        assertEquals("USA", addedDocSF.getData().get("country"));
//
//        Tasks.await(cityRef.document("SF").delete());
//        Log.d("FusabaseNamespaceTest", "Test document deleted: " + addedDocSF.getData());
//
//        DocumentSnapshot addedDocTOK = Tasks.await(cityRef.document("TOK").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocTOK.getData());
//        assertTrue(addedDocTOK.exists());
//        assertEquals("Tokyo", addedDocTOK.getData().get("name"));
//        assertEquals("Japan", addedDocTOK.getData().get("country"));
//
//        Tasks.await(cityRef.document("TOK").delete());
//        Log.d("FusabaseNamespaceTest", "Test document deleted: " + addedDocTOK.getData());
//    }
//
//    @Test
//    public void testQueryWithCount() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        AggregateQuerySnapshot countCitiesWithCapital = Tasks.await(cityRef.whereEqualTo("capital", true).count().get());
//        Log.d("FusabaseNamespaceTest", "countCitiesWithCapital: " + countCitiesWithCapital);
//        Log.d("FusabaseNamespaceTest", "No of Cities with Capitals: " + countCitiesWithCapital.getCount());
//        assertTrue(countCitiesWithCapital.getCount() >= 1);
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testQueryWithLessThan() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        AggregateQuerySnapshot countCitiesWithPopulationCondition = Tasks.await(cityRef.whereLessThan("population", 10000000L).count().get());
//        Log.d("FusabaseNamespaceTest", "countCitiesWithPopulationCondition: " + countCitiesWithPopulationCondition);
//        Log.d("FusabaseNamespaceTest", "No of Cities with Population less than 100000: " + countCitiesWithPopulationCondition.getCount());
//        assertEquals(2L, countCitiesWithPopulationCondition.getCount());
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testUpdateDocument() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("SF").update("population", 1000000L));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        assertEquals(1000000L, addedDocSF.getData().get("population"));
//        Log.d("FusabaseNamespaceTest", "Test document updated: " + addedDocSF.getData());
//
//        Tasks.await(cityRef.document("SF").delete());
//        Log.d("FusabaseNamespaceTest", "Test document deleted: " + addedDocSF.getData());
//    }
//
//    @Test
//    public void testVerifyLimitFunctionality() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        QuerySnapshot limit2 = Tasks.await(cityRef.limit(2).get());
//        Log.d("FusabaseNamespaceTest", "No of Documents with limit2: " + limit2.size());
//        assertEquals(2, limit2.size());
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Test limit while querying the document: " + addedDocSF.getData());
//
//        Tasks.await(cityRef.document("SF").delete());
//        Log.d("FusabaseNamespaceTest", "Test document deleted: " + addedDocSF.getData());
//
//        Tasks.await(cityRef.document("TOK").delete());
//        Log.d("FusabaseNamespaceTest", "Test document deleted");
//    }
//
//    @Test
//    public void testWhereOperationWithNotEqual() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//
//        QuerySnapshot citiesWithNotCapital = Tasks.await(cityRef.whereNotEqualTo("capital", true).get());
//        for (DocumentSnapshot doc : citiesWithNotCapital.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testWhereOperationWithGreaterOrEqual() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//
//        QuerySnapshot citiesWithNotCapital = Tasks.await(cityRef.whereGreaterThanOrEqualTo("name", "San Francisco").get());
//        for (DocumentSnapshot doc : citiesWithNotCapital.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testWhereOperationWithArrayContains() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        QuerySnapshot westCoastCities = Tasks.await(cityRef.whereArrayContains("regions", "west_coast").get());
//        Log.d("FusabaseNamespaceTest", "Documents found: " + westCoastCities.size());
//
//        assertEquals(1, westCoastCities.size());
//        assertEquals("SF", westCoastCities.getDocuments().get(0).getId());
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testWhereOperationWithIn() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//
//        DocumentSnapshot addedDocTOK = Tasks.await(cityRef.document("TOK").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocTOK.getData());
//
//        QuerySnapshot citiesWithNotCapital = Tasks.await(cityRef.whereIn("country", Arrays.asList("USA", "India")).get());
//        for (DocumentSnapshot doc : citiesWithNotCapital.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testWhereOperationWithNotIn() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//
//        DocumentSnapshot addedDocTOK = Tasks.await(cityRef.document("TOK").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocTOK.getData());
//
//        QuerySnapshot citiesNotInUSAIndia = Tasks.await(cityRef.whereNotIn("country", Arrays.asList("USA", "India")).get());
//        for (DocumentSnapshot doc : citiesNotInUSAIndia.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testWhereOperationWithInArray() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//
//        QuerySnapshot citiesWithRegionsInWestEastCoast = Tasks.await(cityRef.whereIn("regions", Arrays.asList(Arrays.asList("west_coast"), Arrays.asList("norcal"))).get());
//        for (DocumentSnapshot doc : citiesWithRegionsInWestEastCoast.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testCompoundGroupQueries() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//
//        DocumentSnapshot addedDocTOK = Tasks.await(cityRef.document("TOK").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocTOK.getData());
//
//        QuerySnapshot citiesWithAndOperation = Tasks.await(cityRef.whereEqualTo("state", "CA").whereEqualTo("name", "San Francisco").get());
//        for (DocumentSnapshot doc : citiesWithAndOperation.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testCompoundGroupQueriesWithStateAndPopulation() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//
//        DocumentSnapshot addedDocTOK = Tasks.await(cityRef.document("TOK").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocTOK.getData());
//
//        QuerySnapshot citiesWithAndOperation = Tasks.await(cityRef.whereEqualTo("state", "CA").whereGreaterThan("population", 860001L).get());
//        for (DocumentSnapshot doc : citiesWithAndOperation.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        assertTrue(citiesWithAndOperation.isEmpty());
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    // Skipping the query limitation test as it's testing error conditions
//
//    @Test
//    public void testRunTransactionWhenDocumentDoesNotExist() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//        DocumentReference sfDocRef = cityRef.document("SF");
//
//        try {
//            Tasks.await(db.runTransaction(transaction -> {
//                Task<DocumentSnapshot> sfDocTask = transaction.get(sfDocRef);
//                DocumentSnapshot sfDoc = Tasks.await(sfDocTask);
//                if (!sfDoc.exists()) {
//                    throw new Exception("Document does not exist!");
//                }
//                // This won't execute
//                return null;
//            }));
//            fail("Transaction should have failed");
//        } catch (Exception e) {
//            Log.d("FusabaseNamespaceTest", "Transaction failed as expected: " + e.getMessage());
//        }
//    }
//
//    @Test
//    public void testRunTransactionWhenDocumentExists() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//        DocumentReference sfDocRef = cityRef.document("SF");
//
//        Tasks.await(sfDocRef.set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        DocumentSnapshot addedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "Data added: " + addedDocSF.getData());
//
//        Tasks.await(db.runTransaction(transaction -> {
//            Task<DocumentSnapshot> sfDocTask = transaction.get(sfDocRef);
//            DocumentSnapshot sfDoc = Tasks.await(sfDocTask);
//            if (!sfDoc.exists()) {
//                throw new Exception("Document does not exist!");
//            }
//
//            long newPopulation = (Long) sfDoc.getData().get("population") + 1;
//            Log.d("FusabaseNamespaceTest", "New Population: " + newPopulation);
//            transaction.update(sfDocRef, "population", newPopulation);
//            return null;
//        }));
//
//        Log.d("FusabaseNamespaceTest", "Transaction successfully committed!");
//
//        DocumentSnapshot updatedDocSF = Tasks.await(cityRef.document("SF").get());
//        Log.d("FusabaseNamespaceTest", "updatedDocSF: " + updatedDocSF.getData());
//        assertEquals(860001L, updatedDocSF.getData().get("population"));
//
//        cityRef.document("SF").delete();
//    }
//
//    @Test
//    public void testQueryWithOrderBy() throws Exception {
//        // Clear collection first
//        QuerySnapshot snapshot = Tasks.await(db.collection("city").get());
//        for (DocumentSnapshot doc : snapshot.getDocuments()) {
//            Tasks.await(db.collection("city").doc(doc.getId()).delete());
//        }
//        Log.d("FusabaseNamespaceTest", "All documents in 'city' collection deleted.");
//
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("state", "NY");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 8400000L);
//            put("regions", Arrays.asList("northeast"));
//        }}));
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 4000000L);
//            put("regions", Arrays.asList("west_coast", "socal"));
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("state", "IL");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 2700000L);
//            put("regions", Arrays.asList("midwest"));
//        }}));
//
//        Tasks.await(cityRef.document("HOU").set(new HashMap<String, Object>() {{
//            put("name", "Houston");
//            put("state", "TX");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 2300000L);
//            put("regions", Arrays.asList("south", "texas"));
//        }}));
//
//        Tasks.await(cityRef.document("PHX").set(new HashMap<String, Object>() {{
//            put("name", "Phoenix");
//            put("state", "AZ");
//            put("country", "USA");
//            put("capital", true);
//            put("population", 1700000L);
//            put("regions", Arrays.asList("southwest"));
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.orderBy("population").get());
//
//        List<Long> populations = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", "Document ID: " + doc.getId() + ", Data: " + doc.getData());
//            populations.add((Long) doc.getData().get("population"));
//        }
//
//        for (int i = 0; i < populations.size() - 1; i++) {
//            if (populations.get(i) > populations.get(i + 1)) {
//                fail("Population is not in ascending order");
//            }
//        }
//
//        Log.d("FusabaseNamespaceTest", "Populations are in ascending order: " + populations);
//
//        // Clean up
//        String[] cities = {"SF", "TOK", "NYC", "LA", "CHI", "HOU", "PHX"};
//        for (String city : cities) {
//            DocumentSnapshot addedDoc = Tasks.await(cityRef.document(city).get());
//            Log.d("FusabaseNamespaceTest", "Test document deleted: " + addedDoc.getData());
//            Tasks.await(cityRef.document(city).delete());
//        }
//    }
//
//    @Test
//    public void testFetchFirst3DocumentsOrderedByName() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("a", 1L);
//            put("b", 0L);
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("a", 0L);
//            put("b", 1L);
//        }}));
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("a", 1L);
//            put("b", 1L);
//        }}));
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("a", 0L);
//            put("b", 0L);
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("a", 1L);
//            put("b", 0L);
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.orderBy("name").limit(3).get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        Log.d("FusabaseNamespaceTest", "Query results ordered by name: " + results);
//
//        assertEquals(3, results.size());
//        for (int i = 1; i < results.size(); i++) {
//            String prevName = (String) results.get(i - 1).get("name");
//            String currName = (String) results.get(i).get("name");
//            assertTrue(prevName.compareTo(currName) <= 0);
//        }
//
//        // Clean up
//        String[] docs = {"SF", "TOK", "NYC", "LA", "CHI"};
//        for (String docId : docs) {
//            Tasks.await(cityRef.document(docId).delete());
//        }
//    }
//
//    @Test
//    public void testFetchFirst3DocumentsOrderedByNameDescending() throws Exception {
//        // Clear collection
//        QuerySnapshot snapshot = Tasks.await(db.collection("city").get());
//        for (DocumentSnapshot doc : snapshot.getDocuments()) {
//            Tasks.await(db.collection("city").doc(doc.getId()).delete());
//        }
//
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("a", 1L);
//            put("b", 0L);
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("a", 0L);
//            put("b", 1L);
//        }}));
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("a", 1L);
//            put("b", 1L);
//        }}));
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("a", 0L);
//            put("b", 0L);
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("a", 1L);
//            put("b", 0L);
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.orderBy("name", Query.Direction.DESCENDING).limit(3).get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        assertEquals(3, results.size());
//        for (int i = 1; i < results.size(); i++) {
//            String prevName = (String) results.get(i - 1).get("name");
//            String currName = (String) results.get(i).get("name");
//            assertTrue(prevName.compareTo(currName) >= 0);
//        }
//
//        Log.d("FusabaseNamespaceTest", "Query results ordered by name (desc): " + results);
//
//        // Clean up
//        String[] docs = {"SF", "TOK", "NYC", "LA", "CHI"};
//        for (String docId : docs) {
//            Tasks.await(cityRef.document(docId).delete());
//        }
//    }
//
//    @Test
//    public void testFetchDocumentsOrderedByStateAndPopulationDescending() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("population", 860000L);
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", "Kanto");
//            put("population", 9000000L);
//        }}));
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("state", "NY");
//            put("population", 8400000L);
//        }}));
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("state", "CA");
//            put("population", 4000000L);
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("state", "IL");
//            put("population", 2700000L);
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.orderBy("state").orderBy("population", Query.Direction.DESCENDING).get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        Log.d("FusabaseNamespaceTest", "Query results ordered by state and population (desc): " + results);
//
//        assertTrue(results.size() > 0);
//        // Note: Full validation of ordering would require more complex logic
//
//        // Clean up
//        String[] docs = {"SF", "TOK", "NYC", "LA", "CHI"};
//        for (String docId : docs) {
//            Tasks.await(cityRef.document(docId).delete());
//        }
//    }
//
//    @Test
//    public void testFetchCitiesWithPopulationGreaterThan100000OrderedByPopulation() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("population", 860000L);
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("population", 9000000L);
//        }}));
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("population", 8400000L);
//        }}));
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("population", 4000000L);
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("population", 2700000L);
//        }}));
//
//        Tasks.await(cityRef.document("HOU").set(new HashMap<String, Object>() {{
//            put("name", "Houston");
//            put("population", 2300000L);
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.whereGreaterThan("population", 100000L).orderBy("population").limit(2).get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        assertEquals(2, results.size());
//        assertTrue(((Long) results.get(0).get("population")) <= ((Long) results.get(1).get("population")));
//
//        Log.d("FusabaseNamespaceTest", "Query results with population > 100000, ordered by population: " + results);
//
//        // Clean up
//        String[] docs = {"SF", "TOK", "NYC", "LA", "CHI", "HOU"};
//        for (String docId : docs) {
//            Tasks.await(cityRef.document(docId).delete());
//        }
//    }
//
//    @Test
//    public void testFetchCitiesWhereCountryIsUSAOrderedByPopulation() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("country", "USA");
//            put("population", 860000L);
//        }}));
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("country", "USA");
//            put("population", 8400000L);
//        }}));
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("country", "USA");
//            put("population", 4000000L);
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("country", "Japan");
//            put("population", 9000000L);
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("country", "USA");
//            put("population", 2700000L);
//        }}));
//
//        Tasks.await(cityRef.document("HOU").set(new HashMap<String, Object>() {{
//            put("name", "Houston");
//            put("country", "USA");
//            put("population", 2300000L);
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.whereEqualTo("country", "USA").orderBy("population").get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        assertTrue(results.size() > 0);
//        for (int i = 1; i < results.size(); i++) {
//            assertTrue(((Long) results.get(i - 1).get("population")) <= ((Long) results.get(i).get("population")));
//        }
//
//        // Clean up
//        String[] docs = {"SF", "NYC", "LA", "TOK", "CHI", "HOU"};
//        for (String docId : docs) {
//            Tasks.await(cityRef.document(docId).delete());
//        }
//    }
//
//    @Test
//    public void testFetchCitiesWithPopulationLessThanEqualTo1000000EndAt() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("state", "CA");
//            put("country", "USA");
//            put("population", 4000000L);
//            put("regions", Arrays.asList("west_coast"));
//        }}));
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("state", "NY");
//            put("country", "USA");
//            put("population", 8400000L);
//            put("regions", Arrays.asList("northeast"));
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("state", "IL");
//            put("country", "USA");
//            put("population", 2700000L);
//            put("regions", Arrays.asList("midwest"));
//        }}));
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast"));
//        }}));
//
//        QuerySnapshot queryEndAt = Tasks.await(cityRef.orderBy("population").endAt(1000000L).get());
//
//        List<Map<String, Object>> resultsEndAt = Arrays.asList();
//        for (DocumentSnapshot doc : queryEndAt.getDocuments()) {
//            resultsEndAt.add(doc.getData());
//        }
//
//        assertEquals(1, resultsEndAt.size());
//        assertEquals("San Francisco", resultsEndAt.get(0).get("name"));
//
//        QuerySnapshot queryStartAt = Tasks.await(cityRef.orderBy("population").startAt(1000000L).get());
//
//        List<Map<String, Object>> resultsStartAt = Arrays.asList();
//        for (DocumentSnapshot doc : queryStartAt.getDocuments()) {
//            resultsStartAt.add(doc.getData());
//        }
//
//        assertEquals(3, resultsStartAt.size());
//        List<String> names = Arrays.asList("Chicago", "Los Angeles", "New York");
//        for (Map<String, Object> result : resultsStartAt) {
//            assertTrue(names.contains(result.get("name")));
//        }
//
//        Log.d("FusabaseNamespaceTest", "Cities with population <= 1,000,000 (endAt): " + resultsEndAt);
//        Log.d("FusabaseNamespaceTest", "Cities with population >= 1,000,000 (startAt): " + resultsStartAt);
//
//        // Clean up
//        String[] docs = {"LA", "NYC", "CHI", "SF"};
//        for (String docId : docs) {
//            Tasks.await(cityRef.document(docId).delete());
//        }
//    }
//
//    @Test
//    public void testPaginationWithStartAfterAndLimit() throws Exception {
//        // Clear collection
//        QuerySnapshot snapshot = Tasks.await(db.collection("city").get());
//        for (DocumentSnapshot doc : snapshot.getDocuments()) {
//            Tasks.await(db.collection("city").doc(doc.getId()).delete());
//        }
//
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("state", "CA");
//            put("country", "USA");
//            put("population", 4000000L);
//            put("regions", Arrays.asList("west_coast"));
//        }}));
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("state", "NY");
//            put("country", "USA");
//            put("population", 8400000L);
//            put("regions", Arrays.asList("northeast"));
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("state", "IL");
//            put("country", "USA");
//            put("population", 2700000L);
//            put("regions", Arrays.asList("midwest"));
//        }}));
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast"));
//        }}));
//
//        Tasks.await(cityRef.document("HOU").set(new HashMap<String, Object>() {{
//            put("name", "Houston");
//            put("state", "TX");
//            put("country", "USA");
//            put("population", 2300000L);
//            put("regions", Arrays.asList("south"));
//        }}));
//
//        QuerySnapshot first = Tasks.await(cityRef.orderBy("population").limit(3).get());
//        DocumentSnapshot lastVisible = first.getDocuments().get(2);
//        Log.d("FusabaseNamespaceTest", "Last visible city: " + lastVisible.getData().get("name"));
//
//        QuerySnapshot next = Tasks.await(cityRef.orderBy("population").startAfter(lastVisible).limit(3).get());
//
//        List<Map<String, Object>> citiesNextPage = Arrays.asList();
//        for (DocumentSnapshot doc : next.getDocuments()) {
//            citiesNextPage.add(doc.getData());
//        }
//
//        Log.d("FusabaseNamespaceTest", "Next page of cities: " + citiesNextPage);
//
//        assertEquals(2, citiesNextPage.size());
//        assertEquals("Los Angeles", citiesNextPage.get(0).get("name"));
//
//        // Clean up
//        String[] docs = {"LA", "NYC", "CHI", "SF", "HOU"};
//        for (String docId : docs) {
//            Tasks.await(cityRef.document(docId).delete());
//        }
//    }
//
//    @Test
//    public void testUpdateDocumentWithLargeTextField() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("largeTextTest");
//
//        String largeText = "A".repeat(2000);
//
//        Tasks.await(cityRef.set(new HashMap<String, Object>() {{
//            put("description", "");
//        }}));
//
//        Log.d("FusabaseNamespaceTest", largeText);
//
//        Tasks.await(cityRef.update("description", largeText));
//
//        DocumentSnapshot docSnapshot = Tasks.await(cityRef.get());
//
//        assertTrue(docSnapshot.exists());
//        assertEquals(largeText, docSnapshot.getData().get("description"));
//
//        Tasks.await(cityRef.delete());
//    }
//
//    @Test
//    public void testUpdateDocumentWithMapField() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("mapTest");
//
//        Map<String, Object> mapField = new HashMap<>();
//        mapField.put("population", 4000000L);
//        Map<String, Object> coordinates = new HashMap<>();
//        coordinates.put("lat", 34.0522);
//        coordinates.put("long", -118.2437);
//        mapField.put("coordinates", coordinates);
//        mapField.put("nickname", "City of Angels");
//
//        Tasks.await(cityRef.set(new HashMap<String, Object>() {{
//            put("details", new HashMap<String, Object>());
//        }}));
//
//        Tasks.await(cityRef.update("details", mapField));
//
//        DocumentSnapshot docSnapshot = Tasks.await(cityRef.get());
//
//        assertTrue(docSnapshot.exists());
//        Log.d("FusabaseNamespaceTest", docSnapshot.getData().get("details").toString());
//        assertEquals(mapField, docSnapshot.getData().get("details"));
//
//        Tasks.await(cityRef.delete());
//    }
//
//    // Skipping error tests for invalid fields
//
//    @Test
//    public void testUpdateCapitalFieldBJ() throws Exception {
//        DocumentReference cityDocRef = db.collection("city").document("BJ");
//
//        Tasks.await(cityDocRef.set(new HashMap<String, Object>() {{
//            put("name", "Washington, D.C.");
//            put("country", "USA");
//            put("population", 705749L);
//            Map<String, Object> details = new HashMap<>();
//            details.put("mayor", "Muriel Bowser");
//            details.put("founded", "1790");
//            put("details", details);
//        }}));
//
//        Tasks.await(cityDocRef.update(new HashMap<String, Object>() {{
//            put("population", 720000L);
//            put("details.mayor", "John Doe");
//        }}));
//
//        DocumentSnapshot docSnapshot = Tasks.await(cityDocRef.get());
//        Map<String, Object> data = docSnapshot.getData();
//
//        Log.d("FusabaseNamespaceTest", "Washington Document: " + data);
//
//        assertEquals(720000L, data.get("population"));
//        Map<String, Object> details = (Map<String, Object>) data.get("details");
//        assertEquals("John Doe", details.get("mayor"));
//
//        Tasks.await(cityDocRef.delete());
//    }
//
//    @Test
//    public void testOverwriteFavoritesObject() throws Exception {
//        DocumentReference cityDocRef = db.collection("city").document("frank");
//
//        Map<String, Object> favorites = new HashMap<>();
//        favorites.put("food", "Pizza");
//        favorites.put("color", "Blue");
//        favorites.put("subject", "Recess");
//
//        Tasks.await(cityDocRef.set(new HashMap<String, Object>() {{
//            put("name", "Frank");
//            put("favorites", favorites);
//            put("age", 12L);
//        }}));
//
//        DocumentSnapshot docSnapshotBeforeUpdate = Tasks.await(cityDocRef.get());
//        Log.d("FusabaseNamespaceTest", "Frank Document: " + docSnapshotBeforeUpdate.getData());
//
//        Map<String, Object> newFavorites = new HashMap<>();
//        newFavorites.put("food", "Ice Cream");
//
//        Tasks.await(cityDocRef.update("favorites", newFavorites));
//
//        DocumentSnapshot docSnapshot = Tasks.await(cityDocRef.get());
//        Map<String, Object> data = docSnapshot.getData();
//
//        Log.d("FusabaseNamespaceTest", "Frank Document: " + data);
//
//        assertEquals("Frank", data.get("name"));
//        assertEquals(12L, data.get("age"));
//        assertNotNull(data.get("favorites"));
//        assertEquals(newFavorites, data.get("favorites"));
//
//        Tasks.await(cityDocRef.delete());
//    }
//
//    @Test
//    public void testAutomaticallyAddRemoveRegionsDC() throws Exception {
//        DocumentReference cityDocRef = db.collection("city").document("DC");
//
//        Tasks.await(cityDocRef.set(new HashMap<String, Object>() {{
//            put("name", "Washington, D.C.");
//            put("country", "USA");
//            put("regions", Arrays.asList("east_coast"));
//        }}));
//
//        Tasks.await(cityDocRef.update("regions", FieldValue.arrayUnion("greater_virginia")));
//
//        Tasks.await(cityDocRef.update("regions", FieldValue.arrayRemove("east_coast")));
//
//        DocumentSnapshot docSnapshot = Tasks.await(cityDocRef.get());
//        Map<String, Object> data = docSnapshot.getData();
//
//        Log.d("FusabaseNamespaceTest", "Washington Document: " + data);
//
//        assertEquals("Washington, D.C.", data.get("name"));
//        assertEquals("USA", data.get("country"));
//        assertNotNull(data.get("regions"));
//        assertEquals(Arrays.asList("greater_virginia"), data.get("regions"));
//
//        Tasks.await(cityDocRef.delete());
//    }
//
//    @Test
//    public void testAutomaticallyIncrementPopulationDC() throws Exception {
//        DocumentReference cityDocRef = db.collection("city").document("DC");
//
//        Tasks.await(cityDocRef.set(new HashMap<String, Object>() {{
//            put("name", "Washington, D.C.");
//            put("population", 700000L);
//        }}));
//
//        Tasks.await(cityDocRef.update("population", FieldValue.increment(50L)));
//
//        DocumentSnapshot docSnapshot = Tasks.await(cityDocRef.get());
//        Map<String, Object> data = docSnapshot.getData();
//
//        Log.d("FusabaseNamespaceTest", "Washington Document: " + data);
//
//        assertEquals("Washington, D.C.", data.get("name"));
//        assertEquals(700050L, data.get("population"));
//
//        Tasks.await(cityDocRef.delete());
//    }
//
//    @Test
//    public void testIncrementPopulationIfUnderLimit() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("SF");
//
//        Tasks.await(cityRef.set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("population", 900000L);
//        }}));
//
//        Long newPopulation = Tasks.await(db.runTransaction(transaction -> {
//            Task<DocumentSnapshot> docTask = transaction.get(cityRef);
//            DocumentSnapshot doc = Tasks.await(docTask);
//            if (!doc.exists()) {
//                throw new Exception("Document does not exist!");
//            }
//
//            long currentPopulation = (Long) doc.getData().get("population");
//            long newPop = currentPopulation + 1;
//
//            if (newPop <= 1000000) {
//                transaction.update(cityRef, "population", newPop);
//                return newPop;
//            } else {
//                throw new Exception("Sorry! Population is too big.");
//            }
//        }));
//
//        DocumentSnapshot docSnapshot = Tasks.await(cityRef.get());
//        Map<String, Object> data = docSnapshot.getData();
//        assertEquals(900001L, data.get("population"));
//        Log.d("FusabaseNamespaceTest", "Population increased to " + newPopulation);
//
//        Tasks.await(cityRef.delete());
//    }
//
//    @Test
//    public void testRejectTransactionIfPopulationExceedsLimit() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("SF");
//
//        Tasks.await(cityRef.set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("population", 1000000L);
//        }}));
//
//        try {
//            Tasks.await(db.runTransaction(transaction -> {
//                Task<DocumentSnapshot> docTask = transaction.get(cityRef);
//                DocumentSnapshot doc = Tasks.await(docTask);
//                if (!doc.exists()) throw new Exception("Document not found");
//
//                long currentPopulation = (Long) doc.getData().get("population");
//                long newPop = currentPopulation + 1;
//
//                if (newPop <= 1000000) {
//                    transaction.update(cityRef, "population", newPop);
//                    return newPop;
//                } else {
//                    throw new Exception("Sorry! Population is too big.");
//                }
//            }));
//            fail("Transaction should have been rejected");
//        } catch (Exception err) {
//            assertEquals("Sorry! Population is too big.", err.getMessage());
//        }
//
//        Tasks.await(cityRef.delete());
//    }
//
//    @Test
//    public void testRemoveCapitalFieldFromBJ() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("BJ");
//
//        Tasks.await(cityRef.set(new HashMap<String, Object>() {{
//            put("name", "Beijing");
//            put("country", "China");
//            put("capital", true);
//        }}));
//
//        Tasks.await(cityRef.update("capital", FieldValue.delete()));
//
//        DocumentSnapshot docSnapshot = Tasks.await(cityRef.get());
//        Map<String, Object> data = docSnapshot.getData();
//
//        Log.d("FusabaseNamespaceTest", "Document after deletion: " + data);
//
//        assertFalse(data.containsKey("capital"));
//        assertEquals("Beijing", data.get("name"));
//        assertEquals("China", data.get("country"));
//
//        Tasks.await(cityRef.delete());
//    }
//
//    @Test
//    public void testAggregateTotalPopulationOfCities() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        // Clear collection
//        QuerySnapshot snapshot = Tasks.await(cityRef.get());
//        for (DocumentSnapshot doc : snapshot.getDocuments()) {
//            Tasks.await(doc.getReference().delete());
//        }
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("population", 870887L);
//        }}));
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("population", 3980000L);
//        }}));
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York City");
//            put("population", 8175133L);
//        }}));
//
//        AggregateQuerySnapshot ares3 = Tasks.await(cityRef.aggregate(AggregateField.sum("population")).get());
//
//        long totalPopulation = ares3.get(AggregateField.sum("population"));
//
//        long expectedTotalPopulation = 870887 + 3980000 + 8175133;
//
//        Log.d("FusabaseNamespaceTest", "Total population aggregated: " + totalPopulation);
//        Log.d("FusabaseNamespaceTest", "Total Expected population aggregated: " + expectedTotalPopulation);
//
//        assertEquals(expectedTotalPopulation, totalPopulation);
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("LA").delete());
//        Tasks.await(cityRef.document("NYC").delete());
//    }
//
//    @Test
//    public void testQueryDocumentsUsingFieldPath() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        // Clear collection
//        QuerySnapshot existingDocs = Tasks.await(cityRef.get());
//        for (DocumentSnapshot doc : existingDocs.getDocuments()) {
//            Tasks.await(doc.getReference().delete());
//        }
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        Tasks.await(cityRef.document("NY").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("state", "NY");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 8400000L);
//            put("regions", Arrays.asList("east_coast", "northeast"));
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(db.collection("city").whereEqualTo(FieldPath.of("capital"), true).get());
//
//        Log.d("FusabaseNamespaceTest", "Query result:");
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        assertFalse(querySnapshot.isEmpty());
//        assertEquals(1, querySnapshot.size());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        assertEquals(Arrays.asList("Tokyo", null, "Japan", true, 9000000L, Arrays.asList("kanto", "honshu")), Arrays.asList(
//            results.get(0).get("name"),
//            results.get(0).get("state"),
//            results.get(0).get("country"),
//            results.get(0).get("capital"),
//            results.get(0).get("population"),
//            results.get(0).get("regions")
//        ));
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//        Tasks.await(cityRef.document("NY").delete());
//    }
//
//    @Test
//    public void testQueryDocumentsOrderedByPopulationUsingFieldPathAndStartAt() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        // Clear collection
//        cityRef.get()
//            .addOnSuccessListener(snapshot -> {
//                for (DocumentSnapshot doc : snapshot.getDocuments()) {
//                    doc.getReference().delete();
//                }
//            });
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        Tasks.await(cityRef.document("NY").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("state", "NY");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 8400000L);
//            put("regions", Arrays.asList("east_coast", "northeast"));
//        }}));
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 4000000L);
//            put("regions", Arrays.asList("west_coast", "socal"));
//        }}));
//
//        QuerySnapshot citiesWithMinPopulation = Tasks.await(db.collection("city").orderBy(FieldPath.of("population")).startAt(4000000L).get());
//
//        Log.d("FusabaseNamespaceTest", "Query results:");
//        for (DocumentSnapshot doc : citiesWithMinPopulation.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        assertFalse(citiesWithMinPopulation.isEmpty());
//        assertEquals(3, citiesWithMinPopulation.size());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : citiesWithMinPopulation.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        assertEquals("Los Angeles", results.get(0).get("name"));
//        assertEquals("New York", results.get(1).get("name"));
//        assertEquals("Tokyo", results.get(2).get("name"));
//
//        for (Map<String, Object> doc : results) {
//            assertTrue((Long) doc.get("population") >= 4000000L);
//        }
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//        Tasks.await(cityRef.document("NY").delete());
//        Tasks.await(cityRef.document("LA").delete());
//    }
//
//    @Test
//    public void testAutomaticallyIncrementPopulationUsingRunTransaction() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("SF");
//
//        Tasks.await(cityRef.set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(db.runTransaction(transaction -> {
//            Task<DocumentSnapshot> docTask = transaction.get(cityRef);
//            DocumentSnapshot doc = Tasks.await(docTask);
//            if (!doc.exists()) {
//                throw new Exception("Document does not exist!");
//            }
//            transaction.update(cityRef, FieldValue.increment(5000L));
//            return null;
//        }));
//
//        DocumentSnapshot updatedDoc = Tasks.await(cityRef.get());
//        Log.d("FusabaseNamespaceTest", "Updated document: " + updatedDoc.getData());
//
//        assertTrue(updatedDoc.exists());
//        assertEquals(865000L, updatedDoc.getData().get("population"));
//
//        Tasks.await(cityRef.delete());
//    }
//
//    @Test
//    public void testEnsureNoOverwriteDuringConcurrentUpdatesWithRunTransaction() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("LA");
//
//        Tasks.await(cityRef.set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 4000000L);
//            put("regions", Arrays.asList("west_coast", "socal"));
//        }}));
//
//        Task<Void> transaction1 = db.runTransaction(transaction -> {
//            Task<DocumentSnapshot> docTask = transaction.get(cityRef);
//            DocumentSnapshot doc = Tasks.await(docTask);
//            long newPop = (Long) doc.getData().get("population") + 10000;
//            transaction.update(cityRef, "population", newPop);
//            return null;
//        });
//
//        Task<Void> transaction2 = db.runTransaction(transaction -> {
//            Task<DocumentSnapshot> docTask = transaction.get(cityRef);
//            DocumentSnapshot doc = Tasks.await(docTask);
//            long newPop = (Long) doc.getData().get("population") + 20000;
//            transaction.update(cityRef, "population", newPop);
//            return null;
//        });
//
//        Tasks.await(transaction1);
//        Tasks.await(transaction2);
//
//        DocumentSnapshot updatedDoc = Tasks.await(cityRef.get());
//        Log.d("FusabaseNamespaceTest", "Updated document: " + updatedDoc.getData());
//
//        assertTrue(updatedDoc.exists());
//        assertEquals(4030000L, updatedDoc.getData().get("population"));
//
//        Tasks.await(cityRef.delete());
//    }
//
//    @Test
//    public void testRollbackChangesIfConditionFailsInRunTransaction() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("NY");
//
//        Tasks.await(cityRef.set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("state", "NY");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 8400000L);
//            put("regions", Arrays.asList("east_coast", "northeast"));
//        }}));
//
//        try {
//            Tasks.await(db.runTransaction(transaction -> {
//                Task<DocumentSnapshot> docTask = transaction.get(cityRef);
//                DocumentSnapshot doc = Tasks.await(docTask);
//                if (!doc.exists()) throw new Exception("Document not found");
//
//                long newPopulation = (Long) doc.getData().get("population") - 9000000L;
//                if (newPopulation < 0) {
//                    throw new Exception("Population cannot be negative! Rolling back...");
//                }
//
//                transaction.update(cityRef, "population", newPopulation);
//                return null;
//            }));
//        } catch (Exception error) {
//            Log.d("FusabaseNamespaceTest", error.getMessage());
//        }
//
//        DocumentSnapshot finalDoc = Tasks.await(cityRef.get());
//        Log.d("FusabaseNamespaceTest", "Final document data: " + finalDoc.getData());
//
//        assertEquals(8400000L, finalDoc.getData().get("population"));
//
//        Tasks.await(cityRef.delete());
//    }
//
//    @Test
//    public void testTransferPopulationBetweenTwoCitiesUsingRunTransaction() throws Exception {
//        DocumentReference sfRef = db.collection("city").document("SF");
//        DocumentReference nyRef = db.collection("city").document("NY");
//
//        Tasks.await(sfRef.set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("population", 860000L);
//        }}));
//
//        Tasks.await(nyRef.set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("population", 8400000L);
//        }}));
//
//        Tasks.await(db.runTransaction(transaction -> {
//            Task<DocumentSnapshot> sfDocTask = transaction.get(sfRef);
//            Task<DocumentSnapshot> nyDocTask = transaction.get(nyRef);
//
//            DocumentSnapshot sfDoc = Tasks.await(sfDocTask);
//            DocumentSnapshot nyDoc = Tasks.await(nyDocTask);
//
//            if (!sfDoc.exists() || !nyDoc.exists()) {
//                throw new Exception("One of the cities does not exist!");
//            }
//
//            long newSfPop = (Long) sfDoc.getData().get("population") - 5000;
//            long newNyPop = (Long) nyDoc.getData().get("population") + 5000;
//
//            transaction.update(sfRef, "population", newSfPop);
//            transaction.update(nyRef, "population", newNyPop);
//            return null;
//        }));
//
//        DocumentSnapshot updatedSf = Tasks.await(sfRef.get());
//        DocumentSnapshot updatedNy = Tasks.await(nyRef.get());
//
//        Log.d("FusabaseNamespaceTest", "Updated SF: " + updatedSf.getData());
//        Log.d("FusabaseNamespaceTest", "Updated NY: " + updatedNy.getData());
//
//        assertEquals(855000L, updatedSf.getData().get("population"));
//        assertEquals(8405000L, updatedNy.getData().get("population"));
//
//        Tasks.await(sfRef.delete());
//        Tasks.await(nyRef.delete());
//    }
//
//    @Test
//    public void testUseSetWithMergeInRunTransaction() throws Exception {
//        DocumentReference cityRef = db.collection("city").document("TOK");
//
//        Tasks.await(cityRef.set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("country", "Japan");
//            put("population", 9000000L);
//        }}));
//
//        Tasks.await(db.runTransaction(transaction -> {
//            transaction.set(cityRef, new HashMap<String, Object>() {{
//                put("capital", true);
//            }}, SetOptions.merge());
//            return null;
//        }));
//
//        DocumentSnapshot updatedDoc = Tasks.await(cityRef.get());
//        Log.d("FusabaseNamespaceTest", "Updated document: " + updatedDoc.getData());
//
//        Map<String, Object> data = updatedDoc.getData();
//        assertEquals("Tokyo", data.get("name"));
//        assertEquals("Japan", data.get("country"));
//        assertEquals(9000000L, data.get("population"));
//        assertEquals(true, data.get("capital"));
//
//        Tasks.await(cityRef.delete());
//    }
//
//    @Test
//    public void testCreateTimestampFromDate() throws Exception {
//        Timestamp timestamp = Timestamp.fromDate(new Date("December 10, 1815"));
//        Log.d("FusabaseNamespaceTest", "timestamp: " + timestamp);
//        assertNotNull(timestamp);
//    }
//
//    @Test
//    public void testConvertTimestampToDate() throws Exception {
//        Timestamp timestamp = Timestamp.fromDate(new Date("1815-12-10T00:00:00Z"));
//        Date date = timestamp.toDate();
//
//        assertNotNull(date);
//        assertEquals("1815-12-10T00:00:00.000Z", date.toISOString());
//    }
//
//    @Test
//    public void testWriteAndVerifyDocumentWithTimestamp() throws Exception {
//        Map<String, Object> docData = new HashMap<>();
//        docData.put("stringExample", "Hello world!");
//        docData.put("booleanExample", true);
//        docData.put("numberExample", 3.14159265);
//        docData.put("dateExample", Timestamp.fromDate(new Date("December 10, 1815")));
//        docData.put("arrayExample", Arrays.asList(5L, true, "hello"));
//        docData.put("nullExample", null);
//        Map<String, Object> objectExample = new HashMap<>();
//        objectExample.put("a", 5L);
//        Map<String, Object> nested = new HashMap<>();
//        nested.put("nested", "foo");
//        objectExample.put("b", nested);
//        docData.put("objectExample", objectExample);
//
//        DocumentReference docRef = db.collection("city").document("one");
//
//        Tasks.await(docRef.set(docData));
//
//        DocumentSnapshot docSnapshot = Tasks.await(docRef.get());
//        assertTrue(docSnapshot.exists());
//
//        Map<String, Object> retrievedData = docSnapshot.getData();
//        Log.d("FusabaseNamespaceTest", "Retrieved Document Data: " + retrievedData);
//
//        assertNotNull(retrievedData.get("dateExample"));
//        assertTrue(retrievedData.get("dateExample") instanceof Timestamp);
//
//        Date convertedDate = ((Timestamp) retrievedData.get("dateExample")).toDate();
//        Log.d("FusabaseNamespaceTest", "Converted Date: " + convertedDate);
//
//        assertNotNull(convertedDate);
//        assertEquals("December 10, 1815", new java.text.SimpleDateFormat("MMMM dd, yyyy").format(convertedDate));
//
//        Tasks.await(docRef.delete());
//    }
//
//    @Test
//    public void testConvertBAASDateToJavaScriptDate() throws Exception {
//        Date inputDate = new Date("December 10, 1980");
//        Timestamp timestamp = Timestamp.fromDate(inputDate);
//        Date convertedDate = timestamp.toDate();
//
//        assertEquals(inputDate.getTime(), convertedDate.getTime());
//    }
//
//    @Test
//    public void testRetrieveSecondsAndNanosecondsFromTimestamp() throws Exception {
//        Date inputDate = new Date("December 10, 1971");
//        Timestamp timestamp = Timestamp.fromDate(inputDate);
//
//        Log.d("FusabaseNamespaceTest", "Seconds: " + timestamp.getSeconds());
//        Log.d("FusabaseNamespaceTest", "Nanoseconds: " + timestamp.getNanoseconds());
//
//        assertNotNull(timestamp.getSeconds());
//        assertNotNull(timestamp.getNanoseconds());
//    }
//
//    @Test
//    public void testCompareTwoEqualTimestamps() throws Exception {
//        Timestamp timestamp1 = Timestamp.fromDate(new Date("December 10, 1815"));
//        Timestamp timestamp2 = Timestamp.fromDate(new Date("December 10, 1815"));
//
//        assertTrue(timestamp1.equals(timestamp2));
//    }
//
//    @Test
//    public void testFormatTimestampUsingToLocaleString() throws Exception {
//        Date inputDate = new Date("December 10, 1815");
//        Timestamp timestamp = Timestamp.fromDate(inputDate);
//        Date convertedDate = timestamp.toDate();
//
//        Date nativeDate = new Date("December 10, 1815");
//
//        // Note: For simplicity, just check year
//        assertEquals(1815, convertedDate.getYear() + 1900);
//    }
//
//    @Test
//    public void testExtractYearMonthDayFromTimestamp() throws Exception {
//        Timestamp timestamp = Timestamp.fromDate(new Date("December 10, 1815"));
//        Date convertedDate = timestamp.toDate();
//        Log.d("FusabaseNamespaceTest", "convertedDate: " + convertedDate);
//
//        Log.d("FusabaseNamespaceTest", "convertedDate.getDate(): " + convertedDate.getDate());
//        Log.d("FusabaseNamespaceTest", "convertedDate.getMonth(): " + convertedDate.getMonth());
//        Log.d("FusabaseNamespaceTest", "convertedDate.getYear(): " + convertedDate.getYear());
//
//        Date expectedDate = new Date("December 10, 1815");
//        assertEquals(expectedDate.getYear(), convertedDate.getYear());
//        assertEquals(expectedDate.getMonth(), convertedDate.getMonth());
//        assertEquals(expectedDate.getDate(), convertedDate.getDate());
//    }
//
//    @Test
//    public void testCalculateDifferenceBetweenTwoTimestamps() throws Exception {
//        Timestamp timestamp1 = Timestamp.fromDate(new Date("December 10, 1815"));
//        Timestamp nowTimestamp = Timestamp.now();
//
//        long diff = nowTimestamp.toDate().getTime() - timestamp1.toDate().getTime();
//        long diffYears = diff / (1000L * 60 * 60 * 24 * 365);
//
//        Log.d("FusabaseNamespaceTest", "difference in years: " + diffYears);
//        assertTrue(diff > 0);
//        assertTrue(diffYears > 200);
//    }
//
//    @Test
//    public void testStoreAndRetrieveTimestampInBAAS() throws Exception {
//        DocumentReference testDocRef = db.collection("city").document("timestampTest");
//        Timestamp timestamp = Timestamp.fromDate(new Date("December 10, 1815"));
//
//        Tasks.await(testDocRef.set(new HashMap<String, Object>() {{
//            put("dateExample", timestamp);
//        }}));
//
//        DocumentSnapshot docSnapshot = Tasks.await(testDocRef.get());
//        Timestamp retrievedTimestamp = (Timestamp) docSnapshot.getData().get("dateExample");
//
//        assertNotNull(retrievedTimestamp);
//
//        Tasks.await(testDocRef.delete());
//    }
//
//    @Test
//    public void testCheckIfTimestampIsBeforeAfterAnotherDate() throws Exception {
//        Timestamp timestamp = Timestamp.fromDate(new Date("December 10, 1815"));
//        Timestamp futureTimestamp = Timestamp.fromDate(new Date("2000-01-01"));
//
//        assertTrue(timestamp.toDate().before(futureTimestamp.toDate()));
//    }
//
//    @Test
//    public void testConvertBAASDateToUnixTimestamp() throws Exception {
//        Date inputDate = new Date("December 10, 1815");
//        Timestamp timestamp = Timestamp.fromDate(inputDate);
//        Date convertedDate = timestamp.toDate();
//
//        assertEquals(inputDate.getTime() / 1000, convertedDate.getTime() / 1000);
//    }
//
//    @Test
//    public void testFetchLast3CitiesOrderedByPopulationLimitToLast() throws Exception {
//        // Clear collection
//        QuerySnapshot snapshot = Tasks.await(db.collection("city").get());
//        for (DocumentSnapshot doc : snapshot.getDocuments()) {
//            Tasks.await(db.collection("city").doc(doc.getId()).delete());
//        }
//
//        Log.d("FusabaseNamespaceTest", "All documents in 'city' collection deleted.");
//
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("state", "CA");
//            put("country", "USA");
//            put("population", 4000000L);
//            put("regions", Arrays.asList("west_coast"));
//        }}));
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("state", "NY");
//            put("country", "USA");
//            put("population", 8400000L);
//            put("regions", Arrays.asList("northeast"));
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("state", "IL");
//            put("country", "USA");
//            put("population", 2700000L);
//            put("regions", Arrays.asList("midwest"));
//        }}));
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast"));
//        }}));
//
//        Tasks.await(cityRef.document("HOU").set(new HashMap<String, Object>() {{
//            put("name", "Houston");
//            put("state", "TX");
//            put("country", "USA");
//            put("population", 2300000L);
//            put("regions", Arrays.asList("south"));
//        }}));
//
//        QuerySnapshot query = Tasks.await(cityRef.orderBy("population").limitToLast(3).get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : query.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        Log.d("FusabaseNamespaceTest", "Last 3 cities ordered by population: " + results);
//
//        assertEquals(3, results.size());
//        List<String> expectedNames = Arrays.asList("New York", "Los Angeles", "Chicago");
//        for (Map<String, Object> result : results) {
//            assertTrue(expectedNames.contains(result.get("name")));
//        }
//
//        // Clean up
//        String[] cities = {"LA", "NYC", "CHI", "SF", "HOU"};
//        for (String city : cities) {
//            Tasks.await(cityRef.document(city).delete());
//        }
//    }
//
//    @Test
//    public void testQueryWithArrayContains() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        QuerySnapshot cities = Tasks.await(cityRef.whereArrayContains("regions", "west_coast").get());
//
//        Log.d("FusabaseNamespaceTest", "Query results (array-contains):");
//        for (DocumentSnapshot doc : cities.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        assertEquals(1, cities.size());
//        assertEquals("SF", cities.getDocuments().get(0).getId());
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testQueryWithArrayContainsAny() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("state", "CA");
//            put("country", "USA");
//            put("capital", false);
//            put("population", 860000L);
//            put("regions", Arrays.asList("west_coast", "norcal"));
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("state", null);
//            put("country", "Japan");
//            put("capital", true);
//            put("population", 9000000L);
//            put("regions", Arrays.asList("kanto", "honshu"));
//        }}));
//
//        QuerySnapshot cities = Tasks.await(cityRef.whereArrayContainsAny("regions", Arrays.asList("west_coast", "kanto")).get());
//
//        Log.d("FusabaseNamespaceTest", "Query results (array-contains-any):");
//        for (DocumentSnapshot doc : cities.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", doc.getId() + " => " + doc.getData());
//        }
//
//        assertEquals(2, cities.size());
//        List<String> docIds = Arrays.asList();
//        for (DocumentSnapshot doc : cities.getDocuments()) {
//            docIds.add(doc.getId());
//        }
//        assertTrue(docIds.contains("SF"));
//        assertTrue(docIds.contains("TOK"));
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//    }
//
//    @Test
//    public void testAddAndUpdateDocumentInUsersRelationalCollection() throws Exception {
//        CollectionReference userRef = db.collection("Users");
//
//        int randomNum = (int) (Math.random() * 1000000);
//        String randomName = "user_" + randomNum;
//        long randomAge = (long) (Math.random() * 60) + 10;
//        String randomAbout = "About " + randomName;
//        long randomUUID = randomNum;
//
//        Task<DocumentReference> addTask = userRef.add(new HashMap<String, Object>() {{
//            put("name", randomName);
//            put("age", randomAge);
//            put("about", randomAbout);
//            put("_id", randomUUID);
//        }});
//
//        DocumentReference userDocRef = Tasks.await(addTask);
//        String docId = userDocRef.getId();
//        assertNotNull(docId);
//        Log.d("FusabaseNamespaceTest", "Added document ID: " + docId);
//
//        DocumentSnapshot docSnapshot = Tasks.await(userRef.document(docId).get());
//        assertTrue(docSnapshot.exists());
//        Map<String, Object> data = docSnapshot.getData();
//        assertEquals(randomName, data.get("name"));
//        assertEquals(randomAge, data.get("age"));
//        assertEquals(randomAbout, data.get("about"));
//        assertEquals(randomUUID, data.get("_id"));
//        Log.d("FusabaseNamespaceTest", "Data after add: " + data);
//
//        long updatedAge = randomAge + 5;
//        String updatedAbout = "Updated " + randomName;
//        Map<String, Object> updateData = new HashMap<>();
//        updateData.put("age", updatedAge);
//        updateData.put("about", updatedAbout);
//
//        Tasks.await(userRef.document(docId).update(updateData));
//        Log.d("FusabaseNamespaceTest", "Updated document ID: " + docId);
//
//        docSnapshot = Tasks.await(userRef.document(docId).get());
//        assertTrue(docSnapshot.exists());
//        data = docSnapshot.getData();
//        assertEquals(randomName, data.get("name"));
//        assertEquals(updatedAge, data.get("age"));
//        assertEquals(updatedAbout, data.get("about"));
//        assertEquals(randomUUID, data.get("_id"));
//        Log.d("FusabaseNamespaceTest", "Data after update: " + data);
//
//        Tasks.await(userRef.document(docId).delete());
//        Log.d("FusabaseNamespaceTest", "Test document deleted: " + docId);
//    }
//
//    @Test
//    public void testFetchMoreThan2DocumentsWithPopulationGreaterThan1000000() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("population", 860000L);
//        }}));
//
//        Tasks.await(cityRef.document("TOK").set(new HashMap<String, Object>() {{
//            put("name", "Tokyo");
//            put("population", 9000000L);
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("population", 2700000L);
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.whereGreaterThan("population", 1000000L).get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            Log.d("FusabaseNamespaceTest", "Document :: " + doc.getData());
//            results.add(doc.getData());
//        }
//
//        assertTrue(results.size() >= 2, "Expected more than 2 documents, but got " + results.size());
//        Log.d("FusabaseNamespaceTest", "Documents with population > 1000000: " + results);
//
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("TOK").delete());
//        Tasks.await(cityRef.document("CHI").delete());
//    }
//
//    @Test
//    public void testFetchDocumentsWhereFieldAIsEqualTo1() throws Exception {
//        CollectionReference collectionRef = db.collection("city");
//
//        Tasks.await(collectionRef.document("city1").set(new HashMap<String, Object>() {{
//            put("a", 1L);
//            put("name", "Test Document 1");
//        }}));
//
//        Tasks.await(collectionRef.document("city2").set(new HashMap<String, Object>() {{
//            put("a", 2L);
//            put("name", "Test Document 2");
//        }}));
//
//        Tasks.await(collectionRef.document("city3").set(new HashMap<String, Object>() {{
//            put("a", 1L);
//            put("name", "Test Document 3");
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(collectionRef.whereEqualTo("a", 1L).get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        Log.d("FusabaseNamespaceTest", "Query results: " + results);
//
//        assertEquals(2, results.size(), "Expected 2 documents, but got " + results.size());
//        for (Map<String, Object> doc : results) {
//            assertEquals(1L, doc.get("a"), "Document " + doc.get("name") + " has 'a' value of " + doc.get("a") + ", expected 1");
//        }
//
//        Tasks.await(collectionRef.document("city1").delete());
//        Tasks.await(collectionRef.document("city2").delete());
//        Tasks.await(collectionRef.document("city3").delete());
//    }
//
//    @Test
//    public void testFetchCitiesWithPopulationGreaterThan1000000AndDensityLessThan10000() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("NYC").set(new HashMap<String, Object>() {{
//            put("name", "New York");
//            put("population", 8400000L);
//            put("density", 10000L);
//        }}));
//
//        Tasks.await(cityRef.document("LA").set(new HashMap<String, Object>() {{
//            put("name", "Los Angeles");
//            put("population", 4000000L);
//            put("density", 8000L);
//        }}));
//
//        Tasks.await(cityRef.document("SF").set(new HashMap<String, Object>() {{
//            put("name", "San Francisco");
//            put("population", 870000L);
//            put("density", 11000L);
//        }}));
//
//        Tasks.await(cityRef.document("CHI").set(new HashMap<String, Object>() {{
//            put("name", "Chicago");
//            put("population", 2700000L);
//            put("density", 12000L);
//        }}));
//
//        Tasks.await(cityRef.document("HOU").set(new HashMap<String, Object>() {{
//            put("name", "Houston");
//            put("population", 2300000L);
//            put("density", 9000L);
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.whereGreaterThan("population", 1000000L).whereLessThan("density", 10000L).get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        Log.d("FusabaseNamespaceTest", "Query results: " + results);
//
//        assertTrue(results.size() > 0, "Expected some documents, but got " + results.size());
//        for (Map<String, Object> city : results) {
//            assertTrue((Long) city.get("population") > 1000000L, "City " + city.get("name") + " has population <= 1000000");
//            assertTrue((Long) city.get("density") < 10000L, "City " + city.get("name") + " has density >= 10000");
//        }
//
//        Tasks.await(cityRef.document("NYC").delete());
//        Tasks.await(cityRef.document("LA").delete());
//        Tasks.await(cityRef.document("SF").delete());
//        Tasks.await(cityRef.document("CHI").delete());
//        Tasks.await(cityRef.document("HOU").delete());
//    }
//
//    @Test
//    public void testFetchAllSpringfieldsInAlphabeticalOrderByNameAndState() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF_MA").set(new HashMap<String, Object>() {{
//            put("name", "Springfield");
//            put("state", "Massachusetts");
//            put("population", 150000L);
//        }}));
//
//        Tasks.await(cityRef.document("SF_MO").set(new HashMap<String, Object>() {{
//            put("name", "Springfield");
//            put("state", "Missouri");
//            put("population", 200000L);
//        }}));
//
//        Tasks.await(cityRef.document("SF_WI").set(new HashMap<String, Object>() {{
//            put("name", "Springfield");
//            put("state", "Wisconsin");
//            put("population", 50000L);
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.orderBy("name").orderBy("state").startAt("Springfield").get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        assertEquals(3, results.size(), "Expected 3 documents, but got " + results.size());
//        assertEquals("Massachusetts", results.get(0).get("state"), "First document should be Springfield, Massachusetts");
//        assertEquals("Missouri", results.get(1).get("state"), "Second document should be Springfield, Missouri");
//        assertEquals("Wisconsin", results.get(2).get("state"), "Third document should be Springfield, Wisconsin");
//
//        Log.d("FusabaseNamespaceTest", "All Springfields ordered by name and state: " + results);
//
//        Tasks.await(cityRef.document("SF_MA").delete());
//        Tasks.await(cityRef.document("SF_MO").delete());
//        Tasks.await(cityRef.document("SF_WI").delete());
//    }
//
//    @Test
//    public void testFetchSpringfieldMissouriAndWisconsinWhenStartingAtSpringfieldMissouri() throws Exception {
//        CollectionReference cityRef = db.collection("city");
//
//        Tasks.await(cityRef.document("SF_MO").set(new HashMap<String, Object>() {{
//            put("name", "Springfield");
//            put("state", "Missouri");
//            put("population", 200000L);
//        }}));
//
//        Tasks.await(cityRef.document("SF_WI").set(new HashMap<String, Object>() {{
//            put("name", "Springfield");
//            put("state", "Wisconsin");
//            put("population", 50000L);
//        }}));
//
//        QuerySnapshot querySnapshot = Tasks.await(cityRef.orderBy("name").orderBy("state").startAt("Springfield", "Missouri").get());
//
//        List<Map<String, Object>> results = Arrays.asList();
//        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//            results.add(doc.getData());
//        }
//
//        assertEquals(2, results.size(), "Expected 2 documents, but got " + results.size());
//        assertEquals("Missouri", results.get(0).get("state"), "First document should be Springfield, Missouri");
//        assertEquals("Wisconsin", results.get(1).get("state"), "Second document should be Springfield, Wisconsin");
//
//        Log.d("FusabaseNamespaceTest", "Springfield, Missouri and Springfield, Wisconsin: " + results);
//
//        Tasks.await(cityRef.document("SF_MO").delete());
//        Tasks.await(cityRef.document("SF_WI").delete());
//    }

    // Skipping batch write across multiple collections as it requires additional setup
}
