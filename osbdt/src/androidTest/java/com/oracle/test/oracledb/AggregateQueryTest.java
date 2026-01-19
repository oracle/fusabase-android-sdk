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
import com.oracle.database.osbdt.oracledb.AggregateField;
import com.oracle.database.osbdt.oracledb.AggregateQuery;
import com.oracle.database.osbdt.oracledb.AggregateQuerySnapshot;
import com.oracle.database.osbdt.oracledb.AggregateSource;
import com.oracle.database.osbdt.oracledb.CollectionReference;
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

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AggregateQueryTest {

    private static OsbdtOracledb db;
    private static CollectionReference testCollection;

    @BeforeClass
    public static void setUpClass() throws ExecutionException, InterruptedException, TimeoutException {
        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
        optionsBuilder.setAuthType("base");
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("40D2FCE3C6FC84C4E0631F965E641EC8");
        optionsBuilder.setAppId("40D2FD377AB884DEE0631F965E648517");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_AJJUTUHAHSTEMBD");
        optionsBuilder.setApiVersion("1.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("40D2FCF5350D84CDE0631F965E64CACE");

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

        await(testCollection.document("agg1").set(Collections.singletonMap("value", 10)));
        await(testCollection.document("agg2").set(Collections.singletonMap("value", 20)));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testCollection = db.collection("collectionTest");
        QuerySnapshot allDocs = Tasks.await(testCollection.get(), 10, TimeUnit.SECONDS);
        WriteBatch batch = db.batch();
        for (DocumentSnapshot doc : allDocs.getDocuments()) {
            batch.delete(doc.getReference());
        }
        Tasks.await(batch.commit(), 10, TimeUnit.SECONDS);
    }

    private static <T> T await(Task<T> task) throws ExecutionException, InterruptedException, TimeoutException {
        return Tasks.await(task, 40, TimeUnit.SECONDS);
    }


    // ------------------- AggregateQuery Tests -------------------

    @Test
    public void testCountAggregate() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery countQuery =  testCollection.count("COUNT");
        AggregateQuerySnapshot snapshot = await(countQuery.get(AggregateSource.SERVER));

        assertEquals(2L, snapshot.getCount("COUNT"));
    }

    @Test
    public void testSumAggregate() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery sumQuery =  testCollection.aggregate(AggregateField.sum("sumValue", "value"));
        AggregateQuerySnapshot snapshot = await(sumQuery.get());

        assertEquals(30L, snapshot.get(AggregateField.sum("sumValue", "value")));
    }

    @Test
    public void testAverageAggregate() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery avgQuery =  testCollection.aggregate(AggregateField.average("avgValue", "value"));
        AggregateQuerySnapshot snapshot = await(avgQuery.get(AggregateSource.SERVER));

        assertEquals(15.0, snapshot.get(AggregateField.average("avgValue", "value")), 0.0001);
    }

    @Test
    public void testMultipleAggregates() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery multiQuery =  testCollection.aggregate(
                AggregateField.count("count"),
                AggregateField.sum("sumValue", "value"),
                AggregateField.average("avgValue","value")
        );

        AggregateQuerySnapshot snapshot = await(multiQuery.get(AggregateSource.SERVER));
        assertEquals(2L, snapshot.getCount("count"));
        assertEquals(30L, snapshot.get(AggregateField.sum("sumValue", "value")));
        assertEquals(15.0, snapshot.get(AggregateField.average("avgValue", "value")), 0.0001);
    }

    @Test
    public void testAggregateOnMissingField() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery sumQuery = testCollection.aggregate(AggregateField.sum("sumNonexistent","nonexistent"));
        AggregateQuerySnapshot snapshot = await(sumQuery.get(AggregateSource.SERVER));

        assertEquals(0L, snapshot.get(AggregateField.sum("sumNonexistent", "nonexistent")));
    }

    @Test
    public void testGetWithServerSource() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery countQuery = testCollection.count("COUNT_SERVER");
        AggregateQuerySnapshot snapshot = await(countQuery.get(AggregateSource.SERVER));

        assertEquals(2L, snapshot.getCount("COUNT_SERVER"));
    }

    @Test
    public void testSingleAggregateField() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery singleQuery = testCollection.aggregate(AggregateField.sum("singleSum", "value"));
        AggregateQuerySnapshot snapshot = await(singleQuery.get());

        assertEquals(30L, snapshot.get(AggregateField.sum("singleSum", "value")));
    }

    @Test
    public void testGetQueryMethod() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery aggQuery = testCollection.count("testCount");

        assertNotNull(aggQuery.getQuery());
    }

    @Test
    public void testEqualsMethod() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");

        AggregateQuery query1 = testCollection.aggregate(AggregateField.count("count1"));
        AggregateQuery query2 = testCollection.aggregate(AggregateField.count("count1"));  // Same aggregate field
        AggregateQuery query3 = testCollection.aggregate(AggregateField.count("count2"));  // Different alias

        // Same queries should be equal
        assertTrue(query1.equals(query2));
        assertTrue(query2.equals(query1));

        // Different queries should not be equal
        assertFalse(query1.equals(query3));

        // Null should return false
        assertFalse(query1.equals(null));

        // Different type should return false
        assertFalse(query1.equals("not an aggregate query"));
    }

    @Test
    public void testHashCodeConsistency() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery query1 = testCollection.aggregate(
                AggregateField.count("count"),
                AggregateField.sum("sum", "value")
        );
        AggregateQuery query2 = testCollection.aggregate(
                AggregateField.count("count"),
                AggregateField.sum("sum", "value")
        );

        assertEquals(query1.hashCode(), query2.hashCode());
    }

    @Test
    public void testHashCodeDifferentForDifferentQueries() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery query1 = testCollection.aggregate(AggregateField.count("count1"));
        AggregateQuery query2 = testCollection.aggregate(AggregateField.count("count2"));

        assertNotEquals(query1.hashCode(), query2.hashCode());
    }

    @Test
    public void testAggregateQueryToString() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery aggQuery = testCollection.count("testCount");

        assertNotNull(aggQuery.toString());
    }

//    //  PLSQL Bug . Acknowledged will be fixed later.
////    @Test
////    public void testAggregateOnNonNumericField() throws Exception {
////
////        CollectionReference testCollection = db.collection("collectionTest");
////        await(testCollection.document("agg10").set(Collections.singletonMap("textField", "abc")));
////
////        AggregateQuery sumQuery =  testCollection.aggregate(AggregateField.sum("sumTextField", "textField"));
////        AggregateQuerySnapshot snapshot = await(sumQuery.get());
////
////        // Non-numeric values should be ignored, sum should be 0
////        assertEquals(0.0, snapshot.get(AggregateField.sum("sumTextField","textField")));
////    }
}
