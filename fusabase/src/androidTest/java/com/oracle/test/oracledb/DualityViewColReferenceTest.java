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
import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.mobile.fusabase.auth.FusabaseAuth;
import com.oracle.mobile.fusabase.oracledb.AggregateField;
import com.oracle.mobile.fusabase.oracledb.AggregateQuerySnapshot;
import com.oracle.mobile.fusabase.oracledb.AggregateSource;
import com.oracle.mobile.fusabase.oracledb.DocumentSnapshot;
import com.oracle.mobile.fusabase.oracledb.DualityViewColReference;
import com.oracle.mobile.fusabase.oracledb.DualityViewDocReference;
import com.oracle.mobile.fusabase.oracledb.FieldValue;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.oracledb.QuerySnapshot;
import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.Tasks;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DualityViewColReferenceTest {

    private static FusabaseOracledb db;
    private static DualityViewColReference deptCollection;
    private static FusabaseAuth auth;

    @BeforeClass
    public static void setUpClass() throws ExecutionException, InterruptedException {
        FusabaseOptions.Builder optionsBuilder = new FusabaseOptions.Builder();
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
        optionsBuilder.setApiVersion("1.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("43C6C6DC03224D43E0631F965E6456DC");
        optionsBuilder.setUseSocket(true);
        optionsBuilder.setUploadChunkSize(3);

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
        deptCollection = db.dualityViewCollection("department_dv");
        auth = FusabaseAuth.getInstance(app);
        Tasks.await(auth.signInWithEmailAndPassword ("mohitv123@gmail.com", "MohitV@123"));
    }

    private static <T> T await(Task<T> task) throws ExecutionException, InterruptedException, TimeoutException {
        return Tasks.await(task, 50, TimeUnit.SECONDS);
    }

    // ------------------- Constructor and Basic Functionality Tests -------------------

    @Test
    public void testDualityViewColReferenceConstructor() throws Exception {
        // Use reflection to test protected constructor
        Constructor<DualityViewColReference> constructor = DualityViewColReference.class.getDeclaredConstructor(
                String.class, FusabaseOracledb.class);
        constructor.setAccessible(true);

        DualityViewColReference colRef = constructor.newInstance("test_duality_view", db);

        assertNotNull("DualityViewColReference should be created", colRef);
        assertEquals("Collection ID should match", "test_duality_view", colRef.getId());
        assertEquals("Path should match", "test_duality_view", colRef.getPath());
        assertNull("Parent should be null for duality view collections", colRef.getParent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDualityViewColReferenceConstructorWithParentPath() throws Exception {
        // Constructor should throw exception if path contains parent segments
        Constructor<DualityViewColReference> constructor = DualityViewColReference.class.getDeclaredConstructor(
                String.class, FusabaseOracledb.class);
        constructor.setAccessible(true);

        // This should fail because duality views are top-level and shouldn't have parent paths
        constructor.newInstance("parent/child_duality_view", db);
    }

    // ------------------- document() Method Tests -------------------

    @Test
    public void testDocumentWithSpecificId() throws Exception {
        DualityViewDocReference docRef = deptCollection.document("FB03C10B00");

        assertNotNull("DocumentReference should be created", docRef);
        assertEquals("Document ID should match", "FB03C10B00", docRef.getId());
        assertEquals("Parent should match collection", deptCollection, docRef.getParent());
        assertTrue("Path should contain collection and document",
                  docRef.getPath().contains("department_dv/FB03C10B00"));
    }

    @Test
    public void testDocumentWithAutoGeneratedId() throws Exception {
        DualityViewDocReference docRef = deptCollection.document();

        assertNotNull("Auto-generated DocumentReference should be created", docRef);
        assertNotNull("Auto-generated ID should not be null", docRef.getId());
        assertFalse("Auto-generated ID should not be empty", docRef.getId().isEmpty());
        assertEquals("Parent should match collection", deptCollection, docRef.getParent());
    }

    // ------------------- add() Method Tests -------------------

    @Test
    public void testAddDocument() throws Exception {
        List<Map<String, Object>> employees = new ArrayList<>();
        Map<String, Object> emp = new HashMap<>();
        emp.put("employeeNumber", 14);
        emp.put("employeeName", "PRATIK_1");
        emp.put("job", "CLERK_1");
        emp.put("salary", 12.1);
        employees.add(emp);

        Map<String, Object> data = new HashMap<>();
        data.put("_id", 23);
        data.put("departmentName", "DBAD");
        data.put("location", "www2");
        data.put("employees", employees);

//        DualityViewDocReference docRef = await(deptCollection.add(data));
        DualityViewDocReference docRef = deptCollection.document("FB03C11600");
        assertNotNull("Added document reference should not be null", docRef);
        assertNotNull("Document ID should be generated", docRef.getId());

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Department name should match", "ADDED_DEPT", snapshot.getString("departmentName"));
        assertEquals("Location should match", "ADDED_LOCATION", snapshot.getString("location"));

        // Clean up
        await(docRef.delete());
    }

    @Test
    public void testAddDocumentWithComplexData() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("_id", 888);
        data.put("departmentName", "COMPLEX_DEPT");
        data.put("location", "COMPLEX_LOCATION");

        List<Map<String, Object>> employees = new ArrayList<>();
        Map<String, Object> emp = new HashMap<>();
        emp.put("employeeNumber", 9999);
        emp.put("employeeName", "COMPLEX_EMP");
        emp.put("job", "COMPLEX_JOB");
        emp.put("salary", 9999.99);
        employees.add(emp);

        data.put("employees", employees);

        DualityViewDocReference docRef = await(deptCollection.add(data));

        DocumentSnapshot snapshot = await(docRef.get());
        assertTrue("Document should exist", snapshot.exists());
        assertEquals("Department name should match", "COMPLEX_DEPT", snapshot.getString("departmentName"));

        List<Map<String, Object>> emps = (List<Map<String, Object>>) snapshot.get("employees");
        assertNotNull("Employees should not be null", emps);
        assertEquals("Should have 1 employee", 1, emps.size());
        assertEquals("Employee name should match", "COMPLEX_EMP", emps.get(0).get("employeeName"));

        // Clean up
        await(docRef.delete());
    }

    // ------------------- equals() and hashCode() Tests -------------------

    @Test
    public void testEqualsSameObject() throws Exception {
        DualityViewColReference colRef = db.dualityViewCollection("department_dv");
        assertTrue("DualityViewColReference should equal itself", colRef.equals(colRef));
    }

    @Test
    public void testEqualsIdenticalCollections() throws Exception {
        DualityViewColReference colRef1 = db.dualityViewCollection("department_dv");
        DualityViewColReference colRef2 = db.dualityViewCollection("department_dv");

        assertTrue("Identical DualityViewColReferences should be equal", colRef1.equals(colRef2));
        assertEquals("Equal collections should have equal hash codes", colRef1.hashCode(), colRef2.hashCode());
    }

    @Test
    public void testEqualsDifferentCollections() throws Exception {
        DualityViewColReference colRef1 = db.dualityViewCollection("department_dv");
        DualityViewColReference colRef2 = db.dualityViewCollection("other_duality_view");

        assertFalse("Different DualityViewColReferences should not be equal", colRef1.equals(colRef2));
        assertNotEquals("Different collections should have different hash codes", colRef1.hashCode(), colRef2.hashCode());
    }

    @Test
    public void testEqualsWithNull() throws Exception {
        DualityViewColReference colRef = db.dualityViewCollection("department_dv");
        assertFalse("DualityViewColReference should not equal null", colRef.equals(null));
    }

    @Test
    public void testEqualsWithDifferentType() throws Exception {
        DualityViewColReference colRef = db.dualityViewCollection("department_dv");
        assertFalse("DualityViewColReference should not equal different type", colRef.equals("string"));
    }

    @Test
    public void testEqualsWithDifferentDatabases() throws Exception {
        // This test would require multiple database instances, which is complex to set up
        DualityViewColReference colRef = db.dualityViewCollection("department_dv");
        assertTrue("Same database reference should equal itself", colRef.equals(colRef));
    }

    // ------------------- Error Condition Tests -------------------

    @Test(expected = NullPointerException.class)
    public void testAddWithNullData() throws Exception {
        await(deptCollection.add(null));
    }

    @Test(expected = NullPointerException.class)
    public void testDocumentWithNullId() throws Exception {
        deptCollection.document(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDocumentWithEmptyId() throws Exception {
        deptCollection.document("");
    }

    // ------------------- toString() Test -------------------

    @Test
    public void testToString() throws Exception {
        DualityViewColReference colRef = db.dualityViewCollection("department_dv");

        String stringRep = colRef.toString();
        assertNotNull("toString should not return null", stringRep);
        assertFalse("toString should not be empty", stringRep.isEmpty());
        // Should contain some identifying information
        assertTrue("toString should contain duality view info",
                  stringRep.contains("DualityViewColReference") || stringRep.contains("department_dv"));
    }

    /** -------------------------
     *  Collection-level tests
     *  -------------------------
     */

    @Test
    public void testCollectionExistsAndHasDocs() throws Exception {
        DualityViewColReference dualCollection = db.dualityViewCollection("department_dv");
        QuerySnapshot snapshot = await(dualCollection.get());
        assertEquals(4, snapshot.size()); // dept: 10,20,30,40
        assertEquals("department_dv", dualCollection.getId());
        assertTrue(dualCollection.getPath().endsWith("department_dv"));
        assertNull(dualCollection.getParent()); // top-level
    }

//    @Test
//    public void testListDocuments() throws Exception {
//        Iterable<DualityViewDocReference> docs = await(deptCollection.listDocuments());
//        List<DualityViewDocReference> docList = new ArrayList<>();
//        docs.forEach(docList::add);
//        assertEquals(4, docList.size());
//    }

    /** -------------------------
     *  Department document tests
     *  -------------------------
     */

    @Test
    public void testDepartmentBasicFields() throws Exception {
        DocumentSnapshot dept10 = await(deptCollection.document("FB03C10B00").get());
        assertEquals("ACCOUNTING", dept10.getString("departmentName"));
        assertEquals("BOSTON", dept10.getString("location"));

        DocumentSnapshot dept40 = await(deptCollection.document("FB03C12900").get());
        assertEquals("OPERATIONS", dept40.getString("departmentName"));
        assertEquals("BOSTON", dept40.getString("location"));
    }

    @Test
    public void testEmployeesNestedArray() throws Exception {
        DocumentSnapshot dept30 = await(deptCollection.document("FB03C11F00").get());
        List<Map<String, Object>> employees = (List<Map<String, Object>>) dept30.get("employees");

        assertNotNull(employees);
        assertFalse(employees.isEmpty());
        assertTrue(employees.stream().anyMatch(e -> "ALLEN".equals(e.get("employeeName"))));
        assertTrue(employees.stream().anyMatch(e -> "MARTIN".equals(e.get("employeeName"))));
    }

    @Test
    public void testNumericPrecisionAndNulls() throws Exception {
        DocumentSnapshot dept30 = await(deptCollection.document("FB03C11F00").get());
        List<Map<String, Object>> employees = (List<Map<String, Object>>) dept30.get("employees");

        Map<String, Object> ward = employees.stream()
                .filter(e -> "WARD".equals(e.get("employeeName")))
                .findFirst().orElse(null);
        assertNotNull(ward);
        assertEquals(1250.00, ((Number) ward.get("salary")).doubleValue(), 0.01);

        // commission should not be present (null in Oracle table)
        Map<String, Object> blake = employees.stream()
                .filter(e -> "BLAKE".equals(e.get("employeeName")))
                .findFirst().orElse(null);
        assertNotNull(blake);
        assertNull(blake.get("comm"));
    }

    @Test
    public void testDateAndTimestampFields() throws Exception {
        DocumentSnapshot dept20 = await(deptCollection.document("FB03C11500").get());
        List<Map<String, Object>> employees = (List<Map<String, Object>>) dept20.get("employees");

        Map<String, Object> smith = employees.stream()
                .filter(e -> "SMITH".equals(e.get("employeeName")))
                .findFirst().orElse(null);
        assertNotNull(smith);

        // hiredate field should exist
        Object hiredate = smith.get("hiredate");
        assertNotNull(hiredate);
    }

    @Test
    public void testOidAndEmployeeNumberFields() throws Exception {
        DocumentSnapshot dept20 = await(deptCollection.document("FB03C11500").get());
        List<Map<String, Object>> employees = (List<Map<String, Object>>) dept20.get("employees");

        for (Map<String, Object> e : employees) {
            assertNotNull(e.get("employeeNumber"));
            assertNotNull(e.get("oid"));
        }
    }

    /** -------------------------
     *  Query and filter tests
     *  -------------------------
     */

    @Test
    public void testWhereEqualToOnDepartment() throws Exception {
        DualityViewColReference dualCollection = db.dualityViewCollection("department_dv");
        QuerySnapshot snapshot = await(dualCollection.whereEqualTo("departmentName", "SALES").get());
        assertEquals(1, snapshot.size());
        assertEquals("CHICAGO", snapshot.getDocuments().get(0).getString("location"));
    }

    // Conditional not supported with array type
//    @Test
//    public void testWhereArrayContainsOnNestedEmployees() throws Exception {
//        QuerySnapshot q = await(deptCollection.whereArrayContains("employees.job", "SALESMAN").get());
//        assertFalse(q.isEmpty());
//
//        boolean found = q.getDocuments().stream()
//                .flatMap(doc -> ((List<Map<String, Object>>) doc.get("employees")).stream())
//                .anyMatch(e -> "SALESMAN".equals(e.get("job")));
//        assertTrue(found);
//    }
    // Confirmed bug, filed for update
//    @Test
//    public void testWhereGreaterThanOnSalary() throws Exception {
//        // Departments with employees earning > 3000
//        QuerySnapshot q = await(deptCollection.whereGreaterThan("employees.salary", 3000).get());
//        assertFalse(q.isEmpty());
//
//        boolean hasHighEarner = q.getDocuments().stream()
//                .flatMap(doc -> ((List<Map<String, Object>>) doc.get("employees")).stream())
//                .anyMatch(e -> ((Number) e.get("salary")).doubleValue() > 3000);
//        assertTrue(hasHighEarner);
//    }

    /** -------------------------
     *  Aggregate and iteration
     *  -------------------------
     */

    @Test
    public void testAggregateCount() throws Exception {
        AggregateQuerySnapshot countSnapshot = await(deptCollection.count("dept_count").get(AggregateSource.SERVER));
        assertEquals(4, countSnapshot.getCount("dept_count"));
    }

    @Test
    public void testAggregateSalarySum() throws Exception {
        AggregateQuerySnapshot countSnapshot = await(deptCollection.aggregate(AggregateField.sum("salary_sum", "employees[1].salary")).get(AggregateSource.SERVER));
        assertEquals(9225, countSnapshot.getCount("salary_sum"));
    }

    @Test
    public void testAggregateSalaryAvg() throws Exception {
        AggregateQuerySnapshot countSnapshot = await(deptCollection.aggregate(AggregateField.average("salary_avg", "employees[1].salary")).get(AggregateSource.SERVER));
        assertEquals(3075, countSnapshot.getCount("salary_avg"));
    }

    @Test
    public void testIterateDepartmentsAndEmployees() throws Exception {
        QuerySnapshot snapshot = await(deptCollection.get());
        for (DocumentSnapshot dept : snapshot.getDocuments()) {
            assertNotNull(dept.getString("departmentName"));
            List<Map<String, Object>> employees = (List<Map<String, Object>>) dept.get("employees");
            assertNotNull(employees);
            for (Map<String, Object> e : employees) {
                assertNotNull(e.get("employeeName"));
                assertTrue(e.containsKey("job"));
            }
        }
    }

    /** -------------------------
     *  Document add/remove tests
     *  -------------------------
     */

    @Test
    public void testAddAndDeleteDepartment() throws Exception {
        Map<String, Object> dept = new HashMap<>();
        dept.put("_id", "21");
        dept.put("departmentName", "TESTING");
        dept.put("location", "REMOTE");
        dept.put("employees", Collections.emptyList());

        DualityViewDocReference docRef = await(deptCollection.add(dept));
        assertNotNull(docRef.getId());
        DocumentSnapshot snap = await(docRef.get());
        assertEquals("TESTING", snap.getString("departmentName"));

        await(docRef.delete());
        DocumentSnapshot deleted = await(docRef.get());
        assertFalse(deleted.exists());
    }

    /** -------------------------
     *  Mutation tests (add, set, update, delete)
     *  -------------------------
     */

    @Test
    public void testAddDepartmentWithEmployees() throws Exception {
        Map<String, Object> newDept = new HashMap<>();
        newDept.put("_id", 22);
        newDept.put("departmentName", "QA");
        newDept.put("location", "REMOTE");

        List<Map<String, Object>> employees = new ArrayList<>();
        Map<String, Object> emp = new HashMap<>();
        emp.put("employeeNumber", 9001);
        emp.put("employeeName", "TESTER");
        emp.put("job", "ENGINEER");
        emp.put("salary", 4200.50);

        emp.put("mgr", 7839);
        employees.add(emp);

        newDept.put("employees", employees);

        DualityViewDocReference docRef = await(deptCollection.add(newDept));
        DocumentSnapshot snap = await(docRef.get());

        assertTrue(snap.exists());
        assertEquals("QA", snap.getString("departmentName"));
        assertEquals("REMOTE", snap.getString("location"));

        List<Map<String, Object>> emps = (List<Map<String, Object>>) snap.get("employees");
        assertEquals(1, emps.size());
        assertEquals("TESTER", emps.get(0).get("employeeName"));

        // Clean up
        await(docRef.delete());
    }

    // set has Confirmed Bug
//    @Test
//    public void testSetDepartmentOverwrite() throws Exception {
//
////        Map<String, Object> dept = new HashMap<>();
////        dept.put("_id", 23);
////        dept.put("departmentName", "SUPPORT");
////        dept.put("location", "ONLINE");
////        dept.put("employees", Collections.emptyList());
////
////        DualityViewDocReference deptDoc = await(deptCollection.add(dept)); // creates new doc
////
////        DocumentSnapshot snap = await(deptDoc.get());
////        assertTrue(snap.exists());
////        assertEquals("SUPPORT", snap.getString("departmentName"));
////
////        // Overwrite with new values
////        dept.put("location", "GLOBAL");
////        await(deptDoc.set(dept));
////
////        DocumentSnapshot updated = await(deptDoc.get());
////        assertEquals("GLOBAL", updated.getString("location"));
////        DualityViewDocReference deptDoc = deptCollection.document("FB03C11800");
//        // Clean up
////        await(deptDoc.delete());
//    }

    @Test
    public void testUpdateDepartmentFields() throws Exception {
        DualityViewDocReference deptDoc = await(deptCollection.add(
                new HashMap<String, Object>() {{
                    put("_id", 78);
                    put("departmentName", "TEMP");
                    put("location", "INITIAL");
                    put("employees", new ArrayList<>());
                }}
        ));

        await(deptDoc.update("location", "UPDATED"));
        DocumentSnapshot snap = await(deptDoc.get());
        assertEquals("UPDATED", snap.getString("location"));

        // Update nested array (employees)
        List<Map<String, Object>> employees = new ArrayList<>();
        employees.add(new HashMap<String, Object>() {{
            put("employeeNumber", 9002);
            put("employeeName", "PATCHER");
            put("job", "DEVOPS");
            put("salary", 3900);
        }});

        await(deptDoc.update("employees", employees));

        DocumentSnapshot snap2 = await(deptDoc.get());
        List<Map<String, Object>> emps = (List<Map<String, Object>>) snap2.get("employees");
        assertEquals(1, emps.size());
        assertEquals("PATCHER", emps.get(0).get("employeeName"));

//        DualityViewDocReference deptDoc = deptCollection.document("FB03C11900");
        // Clean up
        await(deptDoc.delete());
    }

    @Test
    public void testDeleteDepartment() throws Exception {
        DualityViewDocReference deptDoc = await(deptCollection.add(
                new HashMap<String, Object>() {{
                    put("_id", 26);
                    put("departmentName", "DELETE_ME");
                    put("location", "VOID");
                    put("employees", Collections.emptyList());
                }}
        ));

        DocumentSnapshot snap = await(deptDoc.get());
        assertTrue(snap.exists());

        await(deptDoc.delete());

        DocumentSnapshot afterDelete = await(deptDoc.get());
        assertFalse(afterDelete.exists());
    }

    /** -------------------------
     *  Employee-level mutations
     *  -------------------------
     */

    @Test
    public void testAddEmployeeToDepartment() throws Exception {
        DualityViewDocReference deptDoc = await(deptCollection.add(
                new HashMap<String, Object>() {{
                    put("_id", 27);
                    put("departmentName", "ENGINEERING");
                    put("location", "HQ");
                    put("employees", new ArrayList<>());
                }}
        ));

        // Add employee
        Map<String, Object> emp = new HashMap<>();
        emp.put("employeeNumber", 9101);
        emp.put("employeeName", "NEO");
        emp.put("job", "ARCHITECT");
        emp.put("salary", 9999.99);

        await(deptDoc.update("employees", FieldValue.arrayUnion(emp)));

        DocumentSnapshot snap = await(deptDoc.get());
        List<Map<String, Object>> employees = (List<Map<String, Object>>) snap.get("employees");
        assertEquals(1, employees.size());
        assertEquals("NEO", employees.get(0).get("employeeName"));

        // Clean up
        await(deptDoc.delete());
    }

    @Test
    public void testUpdateEmployeeInDepartment() throws Exception {
        List<Map<String, Object>> employees = new ArrayList<>();
        employees.add(new HashMap<String, Object>() {{
            put("employeeNumber", 9102);
            put("employeeName", "TRINITY");
            put("job", "DEV");
            put("salary", 5000);
        }});

        DualityViewDocReference deptDoc = await(deptCollection.add(
                new HashMap<String, Object>() {{
                    put("_id", 28);
                    put("departmentName", "MATRIX");
                    put("location", "SIMULATION");
                    put("employees", employees);
                }}
        ));

        // Fetch and update salary in memory
        DocumentSnapshot before = await(deptDoc.get());
        List<Map<String, Object>> emps = (List<Map<String, Object>>) before.get("employees");
        Map<String, Object> trinity = new HashMap<>(emps.get(0));
        trinity.put("salary", 6000);

        await(deptDoc.update("employees", Arrays.asList(trinity)));

        DocumentSnapshot after = await(deptDoc.get());
        List<Map<String, Object>> empsAfter = (List<Map<String, Object>>) after.get("employees");
        assertEquals(6000L, empsAfter.get(0).get("salary"));

        // Clean up
        await(deptDoc.delete());
    }



    // Request to remove OID from the array , Causing the Array Remove to miss finding the document
//    @Test
//    public void testRemoveEmployeeFromDepartment() throws Exception {
////        Map<String, Object> emp = new HashMap<>();
////        emp.put("employeeNumber", 9103);
////        emp.put("employeeName", "MORPHEUS");
////        emp.put("job", "LEADER");
////        emp.put("salary", 7000);
////
////        DualityViewDocReference deptDoc = await(deptCollection.add(
////                new HashMap<String, Object>() {{
////                    put("_id", 29);
////                    put("departmentName", "COMMAND");
////                    put("location", "SHIP");
////                    put("employees", Arrays.asList(emp));
////                }}
////        ));
////
////        // Remove employee using arrayRemove
////        await(deptDoc.update("employees", FieldValue.arrayRemove(emp)));
////
////        DocumentSnapshot after = await(deptDoc.get());
////        List<Map<String, Object>> employees = (List<Map<String, Object>>) after.get("employees");
////        assertTrue(employees == null || employees.isEmpty());
////        DualityViewDocReference deptDoc = deptCollection.document("FB03C11E00");
//        // Clean up
////        await(deptDoc.delete());
//    }
}



//DROP TABLE dept CASCADE CONSTRAINTS;
//DROP TABLE emp CASCADE CONSTRAINTS;
//
//create table dept (
//        deptno number(2) constraint pk_dept primary key,
//dname varchar2(14),
//loc varchar2(13)
//) ;
//
//create table emp (
//        empno number(4) constraint pk_emp primary key,
//ename varchar2(10),
//job varchar2(9),
//mgr number(4),
//hiredate date,
//sal number(7,2),
//comm number(7,2),
//deptno number(2) constraint fk_deptno references dept
//);
//
//insert into dept values (10,'ACCOUNTING','NEW YORK');
//insert into dept values (20,'RESEARCH','DALLAS');
//insert into dept values (30,'SALES','CHICAGO');
//insert into dept values (40,'OPERATIONS','BOSTON');
//
//insert into emp values (7369,'SMITH','CLERK',7902,to_date('17-12-1980','dd-mm-yyyy'),800,null,20);
//insert into emp values (7499,'ALLEN','SALESMAN',7698,to_date('20-2-1981','dd-mm-yyyy'),1600,300,30);
//insert into emp values (7521,'WARD','SALESMAN',7698,to_date('22-2-1981','dd-mm-yyyy'),1250,500,30);
//insert into emp values (7566,'JONES','MANAGER',7839,to_date('2-4-1981','dd-mm-yyyy'),2975,null,20);
//insert into emp values (7654,'MARTIN','SALESMAN',7698,to_date('28-9-1981','dd-mm-yyyy'),1250,1400,30);
//insert into emp values (7698,'BLAKE','MANAGER',7839,to_date('1-5-1981','dd-mm-yyyy'),2850,null,30);
//insert into emp values (7782,'CLARK','MANAGER',7839,to_date('9-6-1981','dd-mm-yyyy'),2450,null,10);
//insert into emp values (7788,'SCOTT','ANALYST',7566,to_date('13-JUL-87','dd-mm-rr')-85,3000,null,20);
//insert into emp values (7839,'KING','PRESIDENT',null,to_date('17-11-1981','dd-mm-yyyy'),5000,null,10);
//insert into emp values (7844,'TURNER','SALESMAN',7698,to_date('8-9-1981','dd-mm-yyyy'),1500,0,30);
//insert into emp values (7876,'ADAMS','CLERK',7788,to_date('13-JUL-87', 'dd-mm-rr')-51,1100,null,20);
//insert into emp values (7900,'JAMES','CLERK',7698,to_date('3-12-1981','dd-mm-yyyy'),950,null,30);
//insert into emp values (7902,'FORD','ANALYST',7566,to_date('3-12-1981','dd-mm-yyyy'),3000,null,20);
//insert into emp values (7934,'MILLER','CLERK',7782,to_date('23-1-1982','dd-mm-yyyy'),1300,null,10);
//commit;
//
// Security Rule
//insert into baassys.baas_securityrule_metadata(project_id,path,read,write,rget,rlist,rcreate,rupdate,rdelete)
//values('43C68B185E9732FEE0631F965E641907','department_dv',
//        '{"conditions": "1=1","type": [],"field": [],"operation":[]}',
//        '{"conditions": "1=1","type": [],"field": [],"operation":[]}',
//        '{"conditions": "1=1","type": [],"field": [],"operation":[]}',
//        '{"conditions": "1=1","type": [],"field": [],"operation":[]}',
//        '{"conditions": "1=1","type": [],"field": [],"operation":[]}',
//        '{"conditions": "1=1","type": [],"field": [],"operation":[]}',
//        '{"conditions": "1=1","type": [],"field": [],"operation":[]}'
//);
//
//create or replace json relational duality view department_dv as
//select json {'_id' : d.deptno,
//        'OID' : SYS_MAKE_OID_FROM_PK(d.deptno),
//             'departmentName'   : d.dname,
//        'location'         : d.loc,
//        'employees' :
//        [ select json {'oid' : SYS_MAKE_OID_FROM_PK(e.empno),
//                              'employeeNumber' : e.empno,
//        'employeeName'   : e.ename,
//        'job'            : e.job,
//        'salary'         : e.sal}
//from   emp e with insert update delete
//where  d.deptno = e.deptno ]}
//from dept d with insert update delete;
//
//commit;
//
//DECLARE
//req varchar2(100) := '{"dv_name":"department_dv","OID":"FB03C11800"}';
//data varchar2(1000) := '{ "_id" : 23,"departmentName":"mohit","location" : "ww", "employees" : [{"employeeNumber" : 12,"employeeName" : "MOHIT","job":"CLERK","salary" : 22}]}';
//doc_data blob;
//jobj JSON_OBJECT_T;
//sql_txt varchar2(1000);
//response_result NUMBER;
//auth json_object_t := NULL;
//BEGIN
//doc_data := utl_raw.cast_to_raw(data);
//  fusabase.fusabase_DV_UPDDOC('3C292A543E4AD6A8E0631F965E64EEE9', doc_data,req,auth,jobj  ,response_result);
//  dbms_output.put_line('Status : ' || response_result);
//  dbms_output.put_line(jobj.stringify);
//END;
///
