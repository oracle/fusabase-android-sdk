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
public class AggregateQuerySnapshotTest {

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


    // ------------------- AggregateQuerySnapshot Tests -------------------

    @Test
    public void testAggregateQuerySnapshotGetQuery() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery countQuery = testCollection.count("COUNT");
        AggregateQuerySnapshot snapshot = await(countQuery.get(AggregateSource.SERVER));

        assertNotNull(snapshot.getQuery());
        assertEquals(countQuery, snapshot.getQuery());
    }

    @Test
    public void testAggregateQuerySnapshotGetCountField() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery countQuery = testCollection.count("COUNT");
        AggregateQuerySnapshot snapshot = await(countQuery.get(AggregateSource.SERVER));

        AggregateField.CountAggregateField countField = AggregateField.count("COUNT");
        long result = snapshot.get(countField);
        assertEquals(2L, result);
    }

    @Test
    public void testAggregateQuerySnapshotGetCountFieldMissingData() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery countQuery = testCollection.count("COUNT_EXISTING");
        AggregateQuerySnapshot snapshot = await(countQuery.get(AggregateSource.SERVER));

        // Try to get a count field with an alias that doesn't exist in the results
        AggregateField.CountAggregateField countField = AggregateField.count("COUNT_MISSING");
        try {
            snapshot.get(countField);
            fail("Expected RuntimeException for missing count data");
        } catch (RuntimeException e) {
            assertEquals("No aggregate field exists with the provided alias", e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotGetSumField() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery sumQuery = testCollection.aggregate(AggregateField.sum("sumValue", "value"));
        AggregateQuerySnapshot snapshot = await(sumQuery.get());

        AggregateField sumField = AggregateField.sum("sumValue", "value");
        Object result = snapshot.get(sumField);
        assertEquals(30L, result);
    }

    @Test
    public void testAggregateQuerySnapshotGetSumFieldMissingData() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery sumQuery = testCollection.aggregate(AggregateField.sum("missingSum", "missingField"));
        AggregateQuerySnapshot snapshot = await(sumQuery.get(AggregateSource.SERVER));

        AggregateField sumField = AggregateField.sum("missingSum", "missingField");
        Object result = snapshot.get(sumField);
        assertEquals(0L, result); // Sum fields return 0L for missing data
    }

    @Test
    public void testAggregateQuerySnapshotGetAverageField() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery avgQuery = testCollection.aggregate(AggregateField.average("avgValue", "value"));
        AggregateQuerySnapshot snapshot = await(avgQuery.get(AggregateSource.SERVER));

        AggregateField.AverageAggregateField avgField = AggregateField.average("avgValue", "value");
        Double result = snapshot.get(avgField);
        assertEquals(15.0, result, 0.0001);
    }

    @Test
    public void testAggregateQuerySnapshotGetAverageFieldMissingData() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery avgQuery = testCollection.aggregate(AggregateField.average("missingAvg", "missingField"));
        AggregateQuerySnapshot snapshot = await(avgQuery.get(AggregateSource.SERVER));

        AggregateField.AverageAggregateField avgField = AggregateField.average("missingAvg", "missingField");
        Double result = snapshot.get(avgField);
        assertNull(result); // Average fields return null for missing data
    }

    @Test
    public void testAggregateQuerySnapshotGetCountByAlias() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery countQuery = testCollection.count("COUNT_ALIAS");
        AggregateQuerySnapshot snapshot = await(countQuery.get(AggregateSource.SERVER));

        long result = snapshot.getCount("COUNT_ALIAS");
        assertEquals(2L, result);
    }

    @Test
    public void testAggregateQuerySnapshotGetCountByAliasMissingData() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery countQuery = testCollection.count("COUNT_ALIAS");
        AggregateQuerySnapshot snapshot = await(countQuery.get(AggregateSource.SERVER));

        try {
            snapshot.getCount("MISSING_COUNT_ALIAS");
            fail("Expected RuntimeException for missing count alias");
        } catch (RuntimeException e) {
            assertEquals("No aggregate count field exists with the provided alias", e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotGetDouble() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery avgQuery = testCollection.aggregate(AggregateField.average("avgValue", "value"));
        AggregateQuerySnapshot snapshot = await(avgQuery.get(AggregateSource.SERVER));

        AggregateField avgField = AggregateField.average("avgValue", "value");
        Double result = snapshot.getDouble(avgField);
        assertEquals(15.0, result, 0.0001);
    }

    @Test
    public void testAggregateQuerySnapshotGetDoubleMissingData() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery avgQuery = testCollection.aggregate(AggregateField.average("missingAvg", "missingField"));
        AggregateQuerySnapshot snapshot = await(avgQuery.get(AggregateSource.SERVER));

        AggregateField avgField = AggregateField.average("missingAvg", "missingField");
        Double result = snapshot.getDouble(avgField);
        assertNull(result); // Non-sum fields return null for missing data
    }

    @Test
    public void testAggregateQuerySnapshotGetDoubleSumFieldMissingData() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery sumQuery = testCollection.aggregate(AggregateField.sum("missingSum", "missingField"));
        AggregateQuerySnapshot snapshot = await(sumQuery.get(AggregateSource.SERVER));

        AggregateField sumField = AggregateField.sum("missingSum", "missingField");
        Double result = snapshot.getDouble(sumField);
        assertEquals(0.0, result, 0.0001); // Sum fields return 0.0 for missing data
    }

    @Test
    public void testAggregateQuerySnapshotGetLong() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery sumQuery = testCollection.aggregate(AggregateField.sum("sumValue", "value"));
        AggregateQuerySnapshot snapshot = await(sumQuery.get());

        AggregateField sumField = AggregateField.sum("sumValue", "value");
        Long result = snapshot.getLong(sumField);
        assertEquals(30L, result.longValue());
    }

    @Test
    public void testAggregateQuerySnapshotGetLongMissingData() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery avgQuery = testCollection.aggregate(AggregateField.average("missingAvg", "missingField"));
        AggregateQuerySnapshot snapshot = await(avgQuery.get(AggregateSource.SERVER));

        AggregateField avgField = AggregateField.average("missingAvg", "missingField");
        Long result = snapshot.getLong(avgField);
        assertNull(result); // Non-sum fields return null for missing data
    }

    @Test
    public void testAggregateQuerySnapshotGetLongSumFieldMissingData() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery sumQuery = testCollection.aggregate(AggregateField.sum("missingSum", "missingField"));
        AggregateQuerySnapshot snapshot = await(sumQuery.get(AggregateSource.SERVER));

        AggregateField sumField = AggregateField.sum("missingSum", "missingField");
        Long result = snapshot.getLong(sumField);
        assertEquals(0L, result.longValue()); // Sum fields return 0L for missing data
    }

    @Test
    public void testAggregateQuerySnapshotEqualsAndHashCode() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery countQuery1 = testCollection.count("COUNT1");
        AggregateQuery countQuery2 = testCollection.count("COUNT2");

        AggregateQuerySnapshot snapshot1 = await(countQuery1.get(AggregateSource.SERVER));
        AggregateQuerySnapshot snapshot2 = await(countQuery1.get(AggregateSource.SERVER)); // Same query
        AggregateQuerySnapshot snapshot3 = await(countQuery2.get(AggregateSource.SERVER)); // Different query

        // Same snapshots should be equal
        assertTrue(snapshot1.equals(snapshot2));
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode());

        // Different snapshots should not be equal
        assertFalse(snapshot1.equals(snapshot3));

        // Test null and different types
        assertFalse(snapshot1.equals(null));
        assertFalse(snapshot1.equals("not a snapshot"));
    }

    @Test
    public void testAggregateQuerySnapshotToString() throws Exception {
        CollectionReference testCollection = db.collection("collectionTest");
        AggregateQuery countQuery = testCollection.count("COUNT");
        AggregateQuerySnapshot snapshot = await(countQuery.get(AggregateSource.SERVER));

        assertNotNull(snapshot.toString());
    }
}
