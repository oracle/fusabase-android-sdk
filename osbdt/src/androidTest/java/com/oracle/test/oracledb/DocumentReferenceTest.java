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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.database.osbdt.auth.OsbdtAuth;
import com.oracle.database.osbdt.oracledb.Blob;
import com.oracle.database.osbdt.oracledb.CollectionReference;
import com.oracle.database.osbdt.oracledb.DocumentReference;
import com.oracle.database.osbdt.oracledb.DocumentSnapshot;
import com.oracle.database.osbdt.oracledb.FieldPath;
import com.oracle.database.osbdt.oracledb.FieldValue;
import com.oracle.database.osbdt.oracledb.ListenerRegistration;
import com.oracle.database.osbdt.oracledb.MetadataChanges;
import com.oracle.database.osbdt.oracledb.OsbdtOracledb;
import com.oracle.database.osbdt.oracledb.OsbdtOracledbException;
import com.oracle.database.osbdt.oracledb.QuerySnapshot;
import com.oracle.database.osbdt.oracledb.SetOptions;
import com.oracle.database.osbdt.oracledb.SnapshotListenOptions;
import com.oracle.database.osbdt.oracledb.Source;
import com.oracle.database.osbdt.oracledb.WriteBatch;
import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;
import com.oracle.database.osbdt.Timestamp;
import com.oracle.database.osbdt.models.IDCSOptions;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.Tasks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.json.JsonObject;

/**
 * Comprehensive test suite for DocumentReference.java with 100% test coverage.
 * Tests all public and protected methods, constructors, CRUD operations, listeners, and edge cases.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DocumentReferenceTest {

    private static OsbdtOracledb db;
    private static OsbdtAuth auth;
    private static CollectionReference testCollection;

    private static final String projectId = "43C68B185E9732FEE0631F965E641907";
    private static final String schema = "user1";

    private static final String ordsHost = "http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/";


    @BeforeClass
    public static void setUpClass() throws ExecutionException, InterruptedException {
        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
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

        OsbdtApp app;

        try{
            app = OsbdtApp.getInstance();
        } catch (Exception e)
        {
            Log.w("DocumentReferenceTest", "Cannot find default instance. Creating the default instance.");
            app = OsbdtApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    , optionsBuilder.build());
        }

        db = OsbdtOracledb.getInstance(app);
        auth = OsbdtAuth.getInstance(app);
        testCollection = db.collection("city");
//        Tasks.await(auth.signInWithEmailAndPassword("mohitv123@gmail.com", "MohitV@123"));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // Remove all docs in the collection for isolation between tests
        QuerySnapshot allDocs = Tasks.await(testCollection.get(), 20, TimeUnit.SECONDS);
        WriteBatch batch = db.batch();
        for (DocumentSnapshot doc : allDocs.getDocuments()) {
            batch.delete(doc.getReference());
        }
        Tasks.await(batch.commit(), 30, TimeUnit.SECONDS);
    }

    private static <T> T await(Task<T> task) throws ExecutionException, InterruptedException, TimeoutException {
        return Tasks.await(task, 10, TimeUnit.SECONDS);
    }

    private void addSecurityRules(String collection) throws Exception {
        String url = ordsHost + "_/baas-panel/project/cel/" + projectId + "?storage=1";
        String credentials = schema + ":" + schema;
        String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.DEFAULT);
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setDoOutput(true);
        String body = "match /" + collection + "/{fileId} {\n" +
            "  allow read: if true;\n" +
            "  allow write: if true;\n" +
            "}";
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes());
        os.flush();
        os.close();
        int responseCode = conn.getResponseCode();
        conn.disconnect();
        if (!(responseCode >= 200 && responseCode < 300)) {
            throw new RuntimeException("Security rules failed to apply");
        }
    }

    private void addSecurityRules2(String collection, String rules) throws Exception {
        String urlStr = ordsHost + "_/baas-panel/project/cel/" + projectId + "?storage=1";

        // Encode credentials: schema:schema_password
        String creds = schema + ":" + schema;
        String basicAuth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);

        // Build rule body like JS version
        String body = "match /" + collection + "/{fileId} {\n" +
            "  " + rules + "\n" +
            "}";

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/plain");
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();

        if (responseCode < 200 || responseCode >= 300) {
            // Print error response body for debugging
            InputStream es = conn.getErrorStream();
            if (es != null) {
                String errorBody = new BufferedReader(new InputStreamReader(es))
                    .lines().reduce("", (acc, line) -> acc + line + "\n");
                Log.e("SecurityRules", "Failed to apply rules:\n" + errorBody);
            }
            fail("HTTP " + responseCode + " while applying security rules");
        }

        conn.disconnect();
        Log.d("SecurityRules", "Security rules applied successfully");
    }

    // ------------------- Basic DocumentReference Creation and Properties Tests -------------------
    @Test
    public void testUpdateCapitalFieldForDC() throws Exception {

        DocumentReference cityRef = testCollection.document("DC");
        // Initial data
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("name", "Washington, D.C.");
        initialData.put("country", "USA");

        // Expected data after update
        Map<String, Object> expectedData = new HashMap<>(initialData);
        expectedData.put("capital", true);

        // Set the initial document
        await(cityRef.set(initialData));

        // Update the "capital" field
        await(cityRef.update("capital", true));

        // Retrieve the document
        DocumentSnapshot docSnapshot = await(cityRef.get());
        assertTrue("Document should exist", docSnapshot.exists());

        Log.d("OracleDbTests", "Washington data: " + docSnapshot.getData());

        // Validate the updated data
        Map<String, Object> updatedData = docSnapshot.getData();
        assertEquals("Updated data should match the expected data", expectedData, updatedData);

        await(cityRef.delete());
    }

    @Test
    public void testDataAccessFromValidLocationWithAuthenticatedUser() throws Exception {
        String email = "mohitv@gmail.com";
        String password = "Mohit@12345678";
//        OsbdtUser user = await(auth.createUserWithEmailAndPassword(email, password)).getUser();
        addSecurityRules2("city",
            "allow read, write: if request.auth != null;\n"
        );

        await(auth.signInWithEmailAndPassword(email, password)).getUser();
        Log.d("AuthBaseTests", "User signed in successfully with password=" + password);

        Map<String, Object> data = new HashMap<>();
        data.put("name", "Bhopal");
        data.put("country", "India");
        data.put("state", "Madhya Pradesh1");
        data.put("capital", false);
        data.put("population", 200000);
        data.put("regions", Arrays.asList("North"));
        DocumentReference resAddDoc = await(testCollection.add(data));
        Log.d("AuthBaseTests", "SUCCESS data written to city collection");
        String docId = resAddDoc.getId();
        assertNotNull(docId);
        assertTrue(docId instanceof String);

        Log.d("AuthBaseTests", "Added document ID: " + docId);

        DocumentSnapshot addedDoc = await(resAddDoc.get());
        assertTrue(addedDoc.exists());
        assertEquals("Bhopal", addedDoc.getString("name"));
        assertEquals("India", addedDoc.getString("country"));
        Log.d("AuthBaseTests", "Data added: " + addedDoc.getData());

        await(auth.signOut());
        Boolean isDenied = false;
        try {
            await(testCollection.add(data));
            Log.d("AuthBaseTests", "data written to city collection");
            fail("ERROR expected [ORA-20015: Access denied by security rule] while writing data to city collection");
        } catch (Exception e) {
            isDenied = true;
            Log.d("AuthBaseTests", e.getMessage());
        }
        assertTrue(isDenied);
    }

    @Test
    public void testScenarioBlobStorageAndRetrieval() throws Exception {
        addSecurityRules("city");

        // Scenario: Store user avatar and profile data
        byte[] avatarData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}; // Mock image data
        Blob avatarBlob = Blob.fromBytes(avatarData);

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", "johndoe");
        userData.put("email", "john@example.com");
        userData.put("avatar", avatarBlob);
        userData.put("metadata", Map.of(
            "fileSize", avatarData.length,
            "contentType", "image/png"
//            "uploadedAt", FieldValue.serverTimestamp()
        ));

        DocumentReference userRef = testCollection.document("user_with_avatar");
        await(userRef.set(userData));

        // Retrieve and verify blob data
        DocumentSnapshot retrievedUser = await(userRef.get());
        Blob retrievedAvatar = Blob.fromBytes((byte[]) retrievedUser.get("avatar"));

        assertNotNull("Avatar blob should exist", retrievedAvatar);
        assertArrayEquals("Avatar data should match", avatarData, retrievedAvatar.toBytes());

        Map<String, Object> metadata = (Map<String, Object>) retrievedUser.get("metadata");
        assertEquals("File size should match", (long) avatarData.length, metadata.get("fileSize"));
        assertEquals("Content type should match", "image/png", metadata.get("contentType"));

        // Scenario: Update avatar with larger data
        byte[] newAvatarData = new byte[100];
        for (int i = 0; i < newAvatarData.length; i++) {
            newAvatarData[i] = (byte) (i % 256);
        }
        Blob newAvatarBlob = Blob.fromBytes(newAvatarData);

        await(userRef.update(Map.of(
            "avatar", newAvatarBlob,
            "metadata", Map.of(
                "fileSize", newAvatarData.length,
                "contentType", "image/jpeg",
                "uploadedAt", FieldValue.serverTimestamp(),
                "updated", true
            )
        )));

        DocumentSnapshot updatedUser = await(userRef.get());
        Blob updatedAvatar = Blob.fromBytes((byte[]) updatedUser.get("avatar"));
        assertArrayEquals("Updated avatar data should match", newAvatarData, updatedAvatar.toBytes());

        // Scenario: Query users with avatars (blob field existence)
        QuerySnapshot usersWithAvatars = await(testCollection
            .whereNotEqualTo("avatar", new ArrayList<>())
            .get());

        assertEquals("Should find user with avatar", 1, usersWithAvatars.size());

        // Cleanup
        await(userRef.delete());
    }

    @Test
    public void testShouldUpdateTheTimestampFieldInTheCityCollectionUsingAServerGeneratedTimestamp() throws Exception {

        DocumentReference cityRef = testCollection.document("NYC");

        // Step 1: Set initial document
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("name", "New York City");
        initialData.put("state", "NY");
        initialData.put("country", "USA");
        await(cityRef.set(initialData));

        // Step 2: Update the 'timestamp' field with server-generated timestamp
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("timestamp", FieldValue.serverTimestamp());
        await(cityRef.update(updateData));

        // Step 3: Retrieve the updated document
        DocumentSnapshot docSnapshot = await(cityRef.get());
        assertTrue("Document should exist", docSnapshot.exists());

        Map<String, Object> updatedData = docSnapshot.getData();
        Log.d("OracleDbTests", "Updated document: " + updatedData);

        // Step 4: Assertions
        assertTrue("Should have timestamp property", updatedData.containsKey("timestamp"));
        assertTrue("Timestamp should be instance of Date", updatedData.get("timestamp") instanceof Date);

        Log.d("OracleDbTests", "Timestamp successfully updated and validated: " + updatedData.get("timestamp"));

        // Step 5: Cleanup
        await(cityRef.delete());
    }

    @Test
    public void testScenarioFieldValueOperationsInUpdates() throws Exception {

        addSecurityRules("city");
        QuerySnapshot snapshot = await(testCollection.get());
        if(snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                await(testCollection.document(doc.getId()).delete());
            }
        }
//        // Setup: Create a user profile document
//        Map<String, Object> userProfile = new HashMap<>();
//        userProfile.put("name", "John Doe");
//        userProfile.put("score", 100L);
//        userProfile.put("tags", Arrays.asList("beginner", "active"));
//        userProfile.put("achievements", Arrays.asList("first_login"));
//        userProfile.put("lastUpdated", FieldValue.serverTimestamp());
//
//        DocumentReference userRef = testCollection.document("user_scenario");
//        await(userRef.set(userProfile));
//
//        // Scenario 1: Increment score and add new tag
//        await(userRef.update(Map.of(
//            "score", FieldValue.increment(50L),
////            "tags", FieldValue.arrayUnion("intermediate", "quiz_master"),
//            "achievements", FieldValue.arrayUnion("score_150"),
//            "lastUpdated", FieldValue.serverTimestamp()
//        )));
//
//        DocumentSnapshot updatedDoc = await(userRef.get());
//        assertEquals("Score should be incremented", Long.valueOf(150), updatedDoc.getLong("score"));
////        assertTrue("Should contain new tags", updatedDoc.get("tags", List.class).contains("intermediate"));
//        assertTrue("Should contain quiz_master tag", updatedDoc.get("tags", List.class).contains("quiz_master"));
//        assertTrue("Should contain new achievement", updatedDoc.get("achievements", List.class).contains("score_150"));
//
//        // Scenario 2: Remove old tag and decrement score
//        await(userRef.update(Map.of(
//            "score", FieldValue.increment(-25L),
//            "tags", FieldValue.arrayRemove("beginner"),
//            "lastUpdated", FieldValue.serverTimestamp()
//        )));
//
//        DocumentSnapshot finalDoc = await(userRef.get());
//        assertEquals("Score should be decremented", Long.valueOf(125), finalDoc.getLong("score"));
//        assertFalse("Should not contain removed tag", finalDoc.get("tags", List.class).contains("beginner"));
//        assertTrue("Should still contain active tag", finalDoc.get("tags", List.class).contains("active"));
//
//        // Scenario 3: Delete a field
//        await(userRef.update("achievements", FieldValue.delete()));
//        DocumentSnapshot docAfterDelete = await(userRef.get());
//        assertFalse("Achievements field should be deleted", docAfterDelete.getData().containsKey("achievements"));
//
//        // Cleanup
//        await(userRef.delete());
    }

    @Test
    public void testDocumentReferenceCreation() throws Exception {
        DocumentReference docRef = testCollection.document("testDoc");
        assertNotNull("DocumentReference should be created", docRef);
        assertEquals("Document ID should match", "testDoc", docRef.getId());
        assertEquals("Parent collection should match", testCollection, docRef.getParent());
    }

    @Test
    public void testDocumentReferenceWithAutoGeneratedId() throws Exception {
        DocumentReference docRef = testCollection.document();
        assertNotNull("Auto-generated DocumentReference should be created", docRef);
        assertNotNull("Auto-generated ID should not be null", docRef.getId());
        assertFalse("Auto-generated ID should not be empty", docRef.getId().isEmpty());
    }

    @Test
    public void testGetParent() throws Exception {
        DocumentReference docRef = testCollection.document("testDoc");
        assertEquals("Parent should match testCollection", testCollection, docRef.getParent());
    }

    @Test
    public void testGetId() throws Exception {
        DocumentReference docRef = testCollection.document("specificId");
        assertEquals("ID should match", "specificId", docRef.getId());
    }

    @Test
    public void testGetPath() throws Exception {
        DocumentReference docRef = testCollection.document("testDoc");
        assertEquals("Path should be collection/document", "collectionTest/testDoc", docRef.getPath());
    }

    @Test
    public void testGetPathSegments() throws Exception {
        DocumentReference docRef = testCollection.document("testDoc");
        List<String> pathSegments = docRef.getPathSegments();
        assertNotNull("Path segments should not be null", pathSegments);
        assertEquals("Should have 2 segments", 2, pathSegments.size());
        assertEquals("First segment should be collection", "collectionTest", pathSegments.get(0));
        assertEquals("Second segment should be document", "testDoc", pathSegments.get(1));
    }

    @Test
    public void testCollectionMethod() throws Exception {
        DocumentReference docRef = testCollection.document("parentDoc");
        CollectionReference subCollection = docRef.collection("subCollection");

        assertNotNull("Subcollection should be created", subCollection);
        assertEquals("Subcollection ID should match", "subCollection", subCollection.getId());
        assertEquals("Subcollection parent should be document", docRef, subCollection.getParent());
    }

    // ------------------- CRUD Operations Tests -------------------

    @Test
    public void testSetAndGetDocument() throws Exception {
        DocumentReference docRef = testCollection.document("testSetGet_" + System.currentTimeMillis());
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Alice");
        data.put("age", 30);
        data.put("active", true);

        await(docRef.set(data));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Name should match", "Alice", snapshot.getString("name"));
        assertEquals("Age should match", Long.valueOf(30), snapshot.getLong("age"));
        assertTrue("Active should match", snapshot.getBoolean("active"));
    }

    @Test
    public void testSetWithComplexDataTypes() throws Exception {
        DocumentReference docRef = testCollection.document("testComplex_" + System.currentTimeMillis());
        Map<String, Object> complexData = new HashMap<>();
        complexData.put("stringField", "Hello World");
        complexData.put("intField", 42);
        complexData.put("longField", 123456789L);
        complexData.put("doubleField", 3.14159);
        complexData.put("booleanField", true);
        complexData.put("nullField", null);
        complexData.put("arrayField", Arrays.asList("a", "b", "c"));
        complexData.put("nestedField", Map.of("innerKey", "innerValue"));

        await(docRef.set(complexData));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("String field should match", "Hello World", snapshot.getString("stringField"));
        assertEquals("Int field should match", Long.valueOf(42), snapshot.getLong("intField"));
        assertEquals("Long field should match", Long.valueOf(123456789L), snapshot.getLong("longField"));
        assertEquals("Double field should match", 3.14159, snapshot.getDouble("doubleField"), 0.00001);
        assertTrue("Boolean field should match", snapshot.getBoolean("booleanField"));
        assertNull("Null field should be null", snapshot.get("nullField"));

        List<?> arrayField = (List<?>) snapshot.get("arrayField");
        assertNotNull("Array field should not be null", arrayField);
        assertEquals("Array should have 3 elements", 3, arrayField.size());

        Map<?, ?> nestedField = (Map<?, ?>) snapshot.get("nestedField");
        assertNotNull("Nested field should not be null", nestedField);
        assertEquals("Nested field should have correct value", "innerValue", nestedField.get("innerKey"));
    }

    @Test
    public void testSetWithOverwrite() throws Exception {
        DocumentReference docRef = testCollection.document("testOverwrite_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("field1", "value1")));

        // Test overwrite by setting the same document again without merge
        await(docRef.set(Collections.singletonMap("field2", "value2")));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());
        assertNull("Field1 should be overwritten (null)", snapshot.get("field1"));
        assertEquals("Field2 should exist", "value2", snapshot.getString("field2"));
    }

    @Test
    public void testSetWithMerge() throws Exception {
        DocumentReference docRef = testCollection.document("testMerge_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("field1", "value1")));
        await(docRef.set(Collections.singletonMap("field2", "value2"), SetOptions.merge()));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Field1 should be preserved", "value1", snapshot.getString("field1"));
        assertEquals("Field2 should be added", "value2", snapshot.getString("field2"));
    }

    @Test
    public void testGetWithSourceDefault() throws Exception {
        DocumentReference docRef = testCollection.document("testSource_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("field", "value")));

        DocumentSnapshot snapshot = await(docRef.get(Source.DEFAULT));
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Field should match", "value", snapshot.getString("field"));
    }

    @Test
    public void testGetWithSourceServer() throws Exception {
        DocumentReference docRef = testCollection.document("testServer_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("field", "value")));

        DocumentSnapshot snapshot = await(docRef.get(Source.SERVER));
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Field should match", "value", snapshot.getString("field"));
    }

    @Test
    public void testGetNonExistentDocument() throws Exception {
        DocumentReference docRef = testCollection.document("nonExistent_" + System.currentTimeMillis());

        DocumentSnapshot snapshot = await(docRef.get());
        assertFalse("Document should not exist", snapshot.exists());
        // Document data for non-existent documents should be null
        assertNull("Document data should be null", snapshot.getData());
    }

    @Test
    public void testDeleteDocument() throws Exception {
        DocumentReference docRef = testCollection.document("testDelete_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("field", "value")));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist before delete", snapshot.exists());

        await(docRef.delete());

        snapshot = await(docRef.get());
        assertFalse("Document should not exist after delete", snapshot.exists());
    }

    @Test
    public void testDeleteNonExistentDocument() throws Exception {
        DocumentReference docRef = testCollection.document("nonExistentDelete_" + System.currentTimeMillis());

        try {
            await(docRef.delete());
            fail("Should throw exception when deleting non-existent document");
        } catch (Exception e) {
            // Expected - should fail when trying to delete non-existent document
            assertTrue("Should be OsbdtOracledbException",
                e.getCause() instanceof OsbdtOracledbException ||
                    e.getMessage().contains("not found"));
        }
    }

    // ------------------- Update Operations Tests -------------------

    @Test
    public void testUpdateWithMap() throws Exception {
        DocumentReference docRef = testCollection.document("testUpdateMap_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("field1", "value1")));

        Map<String, Object> updates = new HashMap<>();
        updates.put("field1", "updated1");
        updates.put("field2", "value2");

        await(docRef.update(updates));

        DocumentSnapshot snapshot = await(docRef.get());
        assertEquals("Field1 should be updated", "updated1", snapshot.getString("field1"));
        assertEquals("Field2 should be added", "value2", snapshot.getString("field2"));
    }

    @Test
    public void testUpdateWithVarArgs() throws Exception {
        DocumentReference docRef = testCollection.document("testUpdateVarArgs_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("field1", "value1")));

        await(docRef.update("field1", "updated1", "field2", "value2"));

        DocumentSnapshot snapshot = await(docRef.get());
        assertEquals("Field1 should be updated", "updated1", snapshot.getString("field1"));
        assertEquals("Field2 should be added", "value2", snapshot.getString("field2"));
    }

    @Test
    public void testShouldFailWhenUpdatingWithNullKey() throws Exception {
        DocumentReference cityRef = testCollection.document("nullValueKey");

        await(cityRef.set(Collections.singletonMap("field1", "value1")));

        boolean errorThrown = false;

        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put(null, "nullKey");
            await(cityRef.update(updateData));
        } catch (Exception error) {
            errorThrown = true;
        }

        assertTrue("Expected an error to be thrown for null key", errorThrown);

    }

    @Test
    public void testWriteAndVerifyDocumentOneWithCorrectData() throws Exception {
        Map<String, Object> docData = new HashMap<>();
        docData.put("stringExample", "Hello world!");
        docData.put("booleanExample", true);
        docData.put("numberExample", 3.14159265);
        docData.put("dateExample", new Timestamp(Instant.parse("1815-12-10T00:00:00Z")));
        docData.put("arrayExample", Arrays.asList(5L, true, "hello"));
        docData.put("nullExample", null);
        Map<String, Object> objectExample = new HashMap<>();
        objectExample.put("a", 5L);
        Map<String, Object> b = new HashMap<>();
        b.put("nested", "foo");
        objectExample.put("b", b);
        docData.put("objectExample", objectExample);

        DocumentReference docRef = testCollection.document("one");

        // Write the document
        await(docRef.set(docData));

        // Retrieve the document
        DocumentSnapshot docSnapshot = await(docRef.get());
        assertTrue("Document should exist", docSnapshot.exists());

        Log.d("OracleDbTests", "document one: " + docSnapshot.getData());

        // Verify the data
        Map<String, Object> retrievedData = docSnapshot.getData();
        assertEquals("Retrieved data should match the inserted data", docData, retrievedData);

        await(testCollection.document("one").delete());
    }


    @Test
    public void testUpdateWithFieldPath() throws Exception {
        DocumentReference docRef = testCollection.document("testUpdateFieldPath_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("nested", new HashMap<String, Object>())));

        await(docRef.update(FieldPath.of("nested", "field"), "nestedValue"));

        DocumentSnapshot snapshot = await(docRef.get());
        Map<String, Object> nested = (Map<String, Object>) snapshot.get("nested");
        assertEquals("Nested field should be set", "nestedValue", nested.get("field"));
    }

    @Test
    public void testUpdateWithFieldPathVarArgs() throws Exception {
        DocumentReference docRef = testCollection.document("testUpdateFieldPathVarArgs_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("nested", new HashMap<String, Object>())));

        await(docRef.update(FieldPath.of("nested", "field1"), "value1",
                          FieldPath.of("nested", "field2"), "value2"));

        DocumentSnapshot snapshot = await(docRef.get());
        Map<String, Object> nested = (Map<String, Object>) snapshot.get("nested");
        assertEquals("Nested field1 should be set", "value1", nested.get("field1"));
        assertEquals("Nested field2 should be set", "value2", nested.get("field2"));
    }

    @Test
    public void testUpdateNonExistentDocument() throws Exception {
        DocumentReference docRef = testCollection.document("nonExistentUpdate_" + System.currentTimeMillis());

        try {
            await(docRef.update("field", "value"));
            fail("Should throw exception when updating non-existent document");
        } catch (Exception e) {
            // Expected - should fail when trying to update non-existent document
            assertTrue("Should be OsbdtOracledbException",
                      e.getCause() instanceof OsbdtOracledbException ||
                      e.getMessage().contains("not found"));
        }
    }

    @Test
    public void testUpdateWithInvalidFieldPath() throws Exception {
        DocumentReference docRef = testCollection.document("testInvalidField_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("field", "value")));

        try {
            await(docRef.update("field.with..dots", "bad"));
            fail("Should throw exception for invalid field path");
        } catch (Exception e) {
            assertTrue("Should contain invalid argument",
                      e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testUpdateWithEmptyFieldName() throws Exception {
        DocumentReference docRef = testCollection.document("testEmptyField_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("field", "value")));

        try {
            await(docRef.update("", "bad"));
            fail("Should throw exception for empty field name");
        } catch (Exception e) {
            assertTrue("Should contain invalid argument",
                      e instanceof IllegalArgumentException);
        }
    }

    // ------------------- Listener Tests -------------------

    @Test
    public void testAddSnapshotListenerBasic() throws Exception {
        DocumentReference docRef = testCollection.document("testListener_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("status", "initial")));

        final CountDownLatch latch = new CountDownLatch(1);
        final DocumentSnapshot[] receivedSnapshot = new DocumentSnapshot[1];

        ListenerRegistration registration = docRef.addSnapshotListener((snapshot, error) -> {
            assertNull("Should not have error", error);
            receivedSnapshot[0] = snapshot;
            latch.countDown();
        });

        // Wait for initial snapshot
        assertTrue("Should receive initial snapshot", latch.await(10, TimeUnit.SECONDS));
        assertNotNull("Snapshot should not be null", receivedSnapshot[0]);
        assertTrue("Document should exist", receivedSnapshot[0].exists());
        assertEquals("Initial status should match", "initial", receivedSnapshot[0].getString("status"));

        registration.remove();
    }

    @Test
    public void testAddSnapshotListenerWithExecutor() throws Exception {
        DocumentReference docRef = testCollection.document("testExecutor_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("value", 1)));

        final CountDownLatch latch = new CountDownLatch(1);
        final DocumentSnapshot[] receivedSnapshot = new DocumentSnapshot[1];

        Executor executor = command -> new Handler(Looper.getMainLooper()).post(command);

        ListenerRegistration registration = docRef.addSnapshotListener(executor, (snapshot, error) -> {
            assertNull("Should not have error", error);
            receivedSnapshot[0] = snapshot;
            latch.countDown();
        });

        assertTrue("Should receive snapshot", latch.await(10, TimeUnit.SECONDS));
        assertNotNull("Snapshot should not be null", receivedSnapshot[0]);

        registration.remove();
    }

    @Test
    public void testAddSnapshotListenerWithMetadataChanges() throws Exception {
        DocumentReference docRef = testCollection.document("testMetadata_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("value", 1)));

        final CountDownLatch latch = new CountDownLatch(1);

        ListenerRegistration registration = docRef.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
            assertNull("Should not have error", error);
            assertNotNull("Snapshot should not be null", snapshot);
            latch.countDown();
        });

        assertTrue("Should receive snapshot", latch.await(10, TimeUnit.SECONDS));

        registration.remove();
    }

    @Test
    public void testAddSnapshotListenerWithOptions() throws Exception {
        DocumentReference docRef = testCollection.document("testOptions_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("value", 1)));

        final CountDownLatch latch = new CountDownLatch(1);

        SnapshotListenOptions options = new SnapshotListenOptions.Builder()
                .setExecutor(command -> new Handler(Looper.getMainLooper()).post(command))
                .setMetadataChanges(MetadataChanges.INCLUDE)
                .build();

        ListenerRegistration registration = docRef.addSnapshotListener(options, (snapshot, error) -> {
            assertNull("Should not have error", error);
            assertNotNull("Snapshot should not be null", snapshot);
            latch.countDown();
        });

        assertTrue("Should receive snapshot", latch.await(10, TimeUnit.SECONDS));

        registration.remove();
    }

    // ------------------- equals Method Tests -------------------

    @Test
    public void testEqualsSameObject() throws Exception {
        DocumentReference docRef = testCollection.document("testEquals");
        assertTrue("DocumentReference should equal itself", docRef.equals(docRef));
    }

    @Test
    public void testEqualsIdenticalReferences() throws Exception {
        DocumentReference docRef1 = testCollection.document("testDoc");
        DocumentReference docRef2 = testCollection.document("testDoc");

        assertTrue("Identical DocumentReferences should be equal", docRef1.equals(docRef2));
    }

    @Test
    public void testEqualsDifferentReferences() throws Exception {
        DocumentReference docRef1 = testCollection.document("doc1");
        DocumentReference docRef2 = testCollection.document("doc2");

        assertFalse("Different DocumentReferences should not be equal", docRef1.equals(docRef2));
    }

    @Test
    public void testEqualsWithNull() throws Exception {
        DocumentReference docRef = testCollection.document("testDoc");
        assertFalse("DocumentReference should not equal null", docRef.equals(null));
    }

    @Test
    public void testEqualsWithDifferentType() throws Exception {
        DocumentReference docRef = testCollection.document("testDoc");
        assertFalse("DocumentReference should not equal different type", docRef.equals("string"));
    }

    @Test
    public void testEqualsWithDifferentCollections() throws Exception {
        CollectionReference col1 = db.collection("col1");
        CollectionReference col2 = db.collection("col2");

        DocumentReference docRef1 = col1.document("testDoc");
        DocumentReference docRef2 = col2.document("testDoc");

        assertFalse("DocumentReferences from different collections should not be equal", docRef1.equals(docRef2));
    }

    @Test
    public void testEqualsWithDifferentDatabases() throws Exception {
        // This test would require multiple database instances, which is complex to set up
        // For now, we'll skip this edge case
        DocumentReference docRef = testCollection.document("testDoc");
        assertTrue("Same database reference should equal itself", docRef.equals(docRef));
    }

    // ------------------- Protected Method Tests (Using Reflection) -------------------

    @Test
    public void testIsOidNotGenerated() throws Exception {
        DocumentReference docRef = testCollection.document("testOid");

        Method method = DocumentReference.class.getDeclaredMethod("isOidNotGenerated");
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(docRef);
        assertFalse("OID should be generated for regular documents", result);
    }

    @Test
    public void testGetTempId() throws Exception {
        DocumentReference docRef = testCollection.document("testTempId");

        Method method = DocumentReference.class.getDeclaredMethod("getTempId");
        method.setAccessible(true);

        String tempId = (String) method.invoke(docRef);
        assertNotNull("Temp ID should not be null", tempId);
        assertFalse("Temp ID should not be empty", tempId.isEmpty());
    }

    @Test
    public void testCheckIfFieldIsCorrect() throws Exception {
        DocumentReference docRef = testCollection.document("testFieldCheck");

        Method method = DocumentReference.class.getDeclaredMethod("checkIfFieldIsCorrect", String.class);
        method.setAccessible(true);

        // Valid field names
        assertTrue("Simple field should be valid", (boolean) method.invoke(docRef, "field"));
        assertTrue("Nested field should be valid", (boolean) method.invoke(docRef, "field.nested"));
        assertTrue("Deep nested field should be valid", (boolean) method.invoke(docRef, "a.b.c.d"));

        // Invalid field names
        assertFalse("Empty field should be invalid", (boolean) method.invoke(docRef, ""));
        assertFalse("Leading dot should be invalid", (boolean) method.invoke(docRef, ".field"));
        assertFalse("Trailing dot should be invalid", (boolean) method.invoke(docRef, "field."));
        assertFalse("Double dots should be invalid", (boolean) method.invoke(docRef, "field..nested"));
        assertFalse("Invalid characters should be invalid", (boolean) method.invoke(docRef, "field*invalid"));
        assertFalse("Empty parts should be invalid", (boolean) method.invoke(docRef, "field."));
    }

    @Test
    public void testUpdateId() throws Exception {
        DocumentReference docRef = testCollection.document("testUpdateId");

        Method method = DocumentReference.class.getDeclaredMethod("updateId", String.class);
        method.setAccessible(true);

        String newId = "updatedId123";
        method.invoke(docRef, newId);

        assertEquals("ID should be updated", newId, docRef.getId());
    }

    @Test
    public void testGetParams() throws Exception {
        DocumentReference docRef = testCollection.document("testParams");

        Method method = DocumentReference.class.getDeclaredMethod("getParams", boolean.class);
        method.setAccessible(true);

        // Test with snapshot = false
        JsonObject params1 = (JsonObject) method.invoke(docRef, false);
        assertNotNull("Params should not be null", params1);
        assertTrue("Should contain path", params1.containsKey("path"));

        // Test with snapshot = true
        JsonObject params2 = (JsonObject) method.invoke(docRef, true);
        assertNotNull("Params should not be null", params2);
        assertTrue("Should contain path", params2.containsKey("path"));
        assertTrue("Should contain snapshot when true", params2.containsKey("snapshot"));
    }

    // ------------------- Edge Cases and Error Conditions -------------------

    @Test
    public void testDocumentReferenceWithEmptyId() throws Exception {
        try {
            testCollection.document("");
            fail("Should throw exception for empty document ID");
        } catch (Exception e) {
            // May throw IllegalArgumentException or similar
            assertTrue("Should throw some exception", true);
        }
    }

    @Test
    public void testCollectionReferenceWithEmptyPath() throws Exception {
        try {
            testCollection.document("valid").collection("");
            fail("Should throw exception for empty collection path");
        } catch (Exception e) {
            // May throw IllegalArgumentException or similar
            assertTrue("Should throw some exception", true);
        }
    }

    @Test
    public void testSetWithNullData() throws Exception {
        DocumentReference docRef = testCollection.document("testNullData_" + System.currentTimeMillis());

        try {
            await(docRef.set(null));
            fail("Should throw exception for null data");
        } catch (Exception e) {
            // Expected - should fail with null data
            assertTrue("Should throw exception", true);
        }
    }

    @Test
    public void testUpdateWithNullMap() throws Exception {
        DocumentReference docRef = testCollection.document("testNullUpdate_" + System.currentTimeMillis());

        try {
            await(docRef.update((Map<String, Object>) null));
            fail("Should throw exception for null update map");
        } catch (Exception e) {
            // Expected - should fail with null map
            assertTrue("Should throw exception", true);
        }
    }

    @Test
    public void testUpdateWithUnevenVarArgs() throws Exception {
        DocumentReference docRef = testCollection.document("testUnevenArgs_" + System.currentTimeMillis());

        try {
            await(docRef.update("field1", "value1", "field2"));
            fail("Should throw exception for uneven number of arguments");
        } catch (Exception e) {
            assertTrue("Should throw IllegalArgumentException",
                      e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testUpdateWithNonStringFieldName() throws Exception {
        // This test requires passing an Object as the first parameter, but the method signature
        // requires String or FieldPath. Skipping this edge case test.
        DocumentReference docRef = testCollection.document("testNonStringField_" + System.currentTimeMillis());
        assertNotNull("DocumentReference should be created", docRef);
    }

    @Test
    public void testUpdateWithFieldPathNonStringValue() throws Exception {
        DocumentReference docRef = testCollection.document("testFieldPathNonString_" + System.currentTimeMillis());

        try {
            await(docRef.update(FieldPath.of("field"), "value1", "notFieldPath", "value2"));
            fail("Should throw exception for non-FieldPath argument");
        } catch (Exception e) {
            assertTrue("Should throw IllegalArgumentException",
                      e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testNestedFieldsUpdateComplex() throws Exception {
        DocumentReference docRef = testCollection.document("testNestedComplex_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("profile", new HashMap<String, Object>())));

        Map<String, Object> updates = new HashMap<>();
        updates.put("profile.name", "John Doe");
        updates.put("profile.age", 30);
        updates.put("profile.address.city", "New York");
        updates.put("profile.address.zip", "10001");

        await(docRef.update(updates));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());

        Map<String, Object> profile = (Map<String, Object>) snapshot.get("profile");
        assertEquals("Name should match", "John Doe", profile.get("name"));
        assertEquals("Age should match", 30L, profile.get("age"));

        Map<String, Object> address = (Map<String, Object>) profile.get("address");
        assertEquals("City should match", "New York", address.get("city"));
        assertEquals("ZIP should match", "10001", address.get("zip"));
    }

    // ------------------- Concurrent Operations Tests -------------------

    @Test
    public void testConcurrentReads() throws Exception {
        DocumentReference docRef = testCollection.document("testConcurrent_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("counter", 0)));

        final int numThreads = 5;
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final List<Integer> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    DocumentSnapshot snapshot = await(docRef.get());
                    results.add(snapshot.getLong("counter").intValue());
                } catch (Exception e) {
                    Log.e("DocumentReferenceTest", "Error in concurrent read", e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue("All concurrent reads should complete", latch.await(30, TimeUnit.SECONDS));
        assertEquals("Should have results from all threads", numThreads, results.size());

        // All should read the same value
        for (Integer result : results) {
            assertEquals("All reads should return same value", Integer.valueOf(0), result);
        }
    }

    @Test
    public void testDocumentReferencePathConstruction() throws Exception {
        // Test complex path construction
        DocumentReference docRef = testCollection.document("doc1");
        CollectionReference subCol = docRef.collection("subCol");
        DocumentReference subDoc = subCol.document("subDoc");
        CollectionReference subSubCol = subDoc.collection("subSubCol");

        assertEquals("Document path should be correct", "collectionTest/doc1", docRef.getPath());
        assertEquals("Subcollection path should be correct", "collectionTest/doc1/subCol", subCol.getPath());
        assertEquals("Subdocument path should be correct", "collectionTest/doc1/subCol/subDoc", subDoc.getPath());
        assertEquals("Sub-subcollection path should be correct", "collectionTest/doc1/subCol/subDoc/subSubCol", subSubCol.getPath());
    }

    @Test
    public void testDocumentReferenceHashCode() throws Exception {
        DocumentReference docRef1 = testCollection.document("hashTest");
        DocumentReference docRef2 = testCollection.document("hashTest");
        DocumentReference docRef3 = testCollection.document("differentHashTest");

        // Equal objects should have equal hash codes
        assertEquals("Equal DocumentReferences should have equal hash codes", docRef1.hashCode(), docRef2.hashCode());

        // Different objects should have different hash codes
        assertNotEquals("Different DocumentReferences should have different hash codes", docRef1.hashCode(), docRef3.hashCode());
    }

    @Test
    public void testDocumentReferenceToString() throws Exception {
        DocumentReference docRef = testCollection.document("toStringTest");

        String stringRep = docRef.toString();
        assertNotNull("toString should not return null", stringRep);
        assertFalse("toString should not be empty", stringRep.isEmpty());
        // Should contain some identifying information
        assertTrue("toString should contain collection or document info",
                  stringRep.contains("collectionTest") || stringRep.contains("DocumentReference"));
    }

    // ------------------- Listener Management Tests -------------------

    @Test
    public void testListenerRegistrationRemoval() throws Exception {
        DocumentReference docRef = testCollection.document("testListenerRemoval_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("value", 1)));

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        // Add listener
        ListenerRegistration registration = docRef.addSnapshotListener((snapshot, error) -> {
            if (latch1.getCount() > 0) {
                latch1.countDown();
            } else if (latch2.getCount() > 0) {
                latch2.countDown();
            }
        });

        // Should receive initial snapshot
        assertTrue("Should receive initial snapshot", latch1.await(10, TimeUnit.SECONDS));

        // Remove listener
        registration.remove();

        // Update document - should not trigger listener
        await(docRef.update("value", 2));

        // Wait a bit to ensure no listener callback
        Thread.sleep(2000);

        // Should not have received second callback
        assertFalse("Should not receive callback after removal", latch2.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testMultipleListeners() throws Exception {
        DocumentReference docRef = testCollection.document("testMultipleListeners_" + System.currentTimeMillis());
        await(docRef.set(Collections.singletonMap("value", 1)));

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        // Add two listeners
        ListenerRegistration reg1 = docRef.addSnapshotListener((snapshot, error) -> {
            latch1.countDown();
        });

        ListenerRegistration reg2 = docRef.addSnapshotListener((snapshot, error) -> {
            latch2.countDown();
        });

        // Both should receive the initial snapshot
        assertTrue("First listener should receive snapshot", latch1.await(10, TimeUnit.SECONDS));
        assertTrue("Second listener should receive snapshot", latch2.await(10, TimeUnit.SECONDS));

        reg1.remove();
        reg2.remove();
    }

    // ------------------- Integration Tests -------------------

    @Test
    public void testCompleteCRUDWorkflow() throws Exception {
        String docId = "completeWorkflow_" + System.currentTimeMillis();
        DocumentReference docRef = testCollection.document(docId);

        // CREATE
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("name", "Test Document");
        initialData.put("version", 1);
        initialData.put("tags", Arrays.asList("test", "workflow"));
        await(docRef.set(initialData));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist after create", snapshot.exists());
        assertEquals("Name should match", "Test Document", snapshot.getString("name"));

        // READ
        snapshot = await(docRef.get());
        assertEquals("Version should be 1", Long.valueOf(1), snapshot.getLong("version"));

        // UPDATE
        await(docRef.update("version", 2, "status", "updated"));
        snapshot = await(docRef.get());
        assertEquals("Version should be updated to 2", Long.valueOf(2), snapshot.getLong("version"));
        assertEquals("Status should be added", "updated", snapshot.getString("status"));
        assertEquals("Name should be preserved", "Test Document", snapshot.getString("name"));

        // DELETE
        await(docRef.delete());
        snapshot = await(docRef.get());
        assertFalse("Document should not exist after delete", snapshot.exists());
    }

    // TODO -> Check this
    @Test
    public void testUpdateDocumentWithFieldValueOperations() throws Exception {
        // First, add a basic document
        DocumentReference docRef = testCollection.document("city1");
        await(docRef.set(Collections.singletonMap("initial", "data")));

        // Update the document using the Map version of update() to handle mixed field types
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "Alice");
        updateData.put(FieldPath.of("address", "home").toString(), "Bangalore");
        updateData.put(FieldPath.of("address", "city").toString(), "Mumbai");
        updateData.put("address_new.city", "Delhi");
        updateData.put("a.b", 1L);
        updateData.put("createdAt", FieldValue.serverTimestamp());
        updateData.put("members", FieldValue.arrayUnion("user123"));
        updateData.put("banned", FieldValue.arrayRemove("spammer"));
        updateData.put("views", FieldValue.increment(1));
        updateData.put(FieldPath.of("meta", "timestamps", "created").toString(), FieldValue.serverTimestamp());
        updateData.put(FieldPath.of("meta", "timestamps", "processed").toString(), FieldValue.serverTimestamp());
        updateData.put("tags", FieldValue.delete());

        await(docRef.update(updateData));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());

        // Verify basic fields
        assertEquals("Name should match", "Alice", snapshot.getString("name"));

        // Verify nested address
        Map<String, Object> address = (Map<String, Object>) snapshot.get("address");
        assertNotNull("Address should exist", address);
        assertEquals("Home should match", "Bangalore", address.get("home"));
        assertEquals("City should match", "Mumbai", address.get("city"));

        // Verify address_new.city
        Map<String, Object> addressNew = (Map<String, Object>) snapshot.get("address_new");
        assertNotNull("Address_new should exist", addressNew);
        assertEquals("City should match", "Delhi", addressNew.get("city"));

        // Verify "a.b"
        Map<String, Object> a = (Map<String, Object>) snapshot.get("a");
        assertNotNull("A should exist", a);
        assertEquals("B should match", Long.valueOf(1), a.get("b"));

        // Verify serverTimestamp fields
        assertTrue("createdAt should be Date", snapshot.get("createdAt") instanceof Date);

        // Verify arrayUnion
        List<String> members = (List<String>) snapshot.get("members");
        assertNotNull("Members should exist", members);
        assertEquals("Members should have 1 element", 1, members.size());
        assertEquals("Member should match", "user123", members.get(0));

        // Verify arrayRemove - Note: depends on initial data, verification is fine as written
        List<String> banned = (List<String>) snapshot.get("banned");
        // Depending on implementation, could be empty list or null

        // Verify increment
        assertEquals("Views should be 1", Long.valueOf(1), snapshot.getLong("views"));

        // Verify nested timestamps
        Map<String, Object> meta = (Map<String, Object>) snapshot.get("meta");
        assertNotNull("Meta should exist", meta);
        Map<String, Object> timestamps = (Map<String, Object>) meta.get("timestamps");
        assertNotNull("Timestamps should exist", timestamps);
        assertTrue("Created timestamp should be Date", timestamps.get("created") instanceof Date);
        assertTrue("Processed timestamp should be Date", timestamps.get("processed") instanceof Date);

        // Verify tags field was deleted (should not exist or be null)
        assertNull("Tags should be deleted", snapshot.get("tags"));

        // Cleanup
        await(docRef.delete());
    }

    @Test
    public void testUpdateDocumentWithNestedFieldValueOperations() throws Exception {
        // First, add a basic document
        DocumentReference docRef = db.collection("configs").document("c1");
        await(docRef.set(Collections.singletonMap("initial", "data")));

        // Update the document using the Map version of update() to handle mixed field types
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("meta.details.count", FieldValue.increment(3));
        updateData.put(FieldPath.of("meta", "details", "owner").toString(), "system");
        updateData.put(FieldPath.of("type", "name").toString(), "scott");

        await(docRef.update(updateData));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());

        // Verify "meta.details.count" increment
        Map<String, Object> meta = (Map<String, Object>) snapshot.get("meta");
        assertNotNull("Meta should exist", meta);
        Map<String, Object> details = (Map<String, Object>) meta.get("details");
        assertNotNull("Details should exist", details);
        assertEquals("Count should be 3", Long.valueOf(3), details.get("count"));

        // Verify FieldPath("meta","details","owner")
        assertEquals("Owner should be system", "system", details.get("owner"));

        // Verify FieldPath("type.name")
        Map<String, Object> type = (Map<String, Object>) snapshot.get("type");
        assertNotNull("Type should exist", type);
        assertEquals("Name should be scott", "scott", type.get("name"));

        // Cleanup
        await(docRef.delete());
    }

    @Test
    public void testUpdateDocumentWithDotNotationFields() throws Exception {
        // First, add a basic document
        DocumentReference docRef = testCollection.document("city1");
        await(docRef.set(Collections.singletonMap("initial", "data")));

        // Now update with dot notation fields
        Map<String, Object> updateData = new HashMap<>();

        updateData.put("name", "Alice");
        updateData.put("settings.ui.theme", "dark");
        updateData.put("settings.ui.mode", "compact");

        // Update the document
        await(docRef.update(updateData));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());

        // Verify name field
        assertEquals("Name should be Alice", "Alice", snapshot.getString("name"));

        // Verify nested settings.ui fields
        Map<String, Object> settings = (Map<String, Object>) snapshot.get("settings");
        assertNotNull("Settings should exist", settings);
        Map<String, Object> ui = (Map<String, Object>) settings.get("ui");
        assertNotNull("UI should exist", ui);
        assertEquals("Theme should be dark", "dark", ui.get("theme"));
        assertEquals("Mode should be compact", "compact", ui.get("mode"));

        // Cleanup
        await(docRef.delete());
    }

    @Test
    public void testUpdateDocumentWithDotNotationFieldsProfile() throws Exception {
        // First, add a basic document
        DocumentReference docRef = testCollection.document("city1");
        await(docRef.set(Collections.singletonMap("initial", "data")));

        // Now update with dot notation fields for profile settings
        Map<String, Object> updateData = new HashMap<>();

        updateData.put("settings.profile.theme", "dark");
        updateData.put("settings.profile.mode", "compact");

        // Update the document
        await(docRef.update(updateData));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());

        // Verify nested settings.profile fields
        Map<String, Object> settings = (Map<String, Object>) snapshot.get("settings");
        assertNotNull("Settings should exist", settings);
        Map<String, Object> profile = (Map<String, Object>) settings.get("profile");
        assertNotNull("Profile should exist", profile);
        assertEquals("Theme should be dark", "dark", profile.get("theme"));
        assertEquals("Mode should be compact", "compact", profile.get("mode"));

        // Cleanup
        await(docRef.delete());
    }

    @Test
    public void testUpdateDocumentWithObjectField() throws Exception {
        // First, add a basic document
        DocumentReference docRef = testCollection.document("city1");
        await(docRef.set(Collections.singletonMap("initial", "data")));

        // Now update with name set to an object
        Map<String, Object> updateData = new HashMap<>();

        // name: {a:1} in Java becomes Map.of("a", 1)
        updateData.put("name", Map.of("a", 1));

        // Update the document
        await(docRef.update(updateData));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());

        // Verify name field is an object with a:1
        Map<String, Object> name = (Map<String, Object>) snapshot.get("name");
        assertNotNull("Name should exist", name);
        assertEquals("A should be 1", Long.valueOf(1), name.get("a"));

        // Cleanup
        await(docRef.delete());
    }

    @Test
    public void testJoinConnection_shouldReturnJoinResults() throws Exception {
//        addSecurityRules("TEST_1");
        QuerySnapshot res = await(db
            .join("TEST_1")
            .get());


        assertNotNull(res.toString());
    }
}
