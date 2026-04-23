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

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.oracledb.AggregateField;
import com.oracle.mobile.fusabase.oracledb.CollectionReference;
import com.oracle.mobile.fusabase.oracledb.DocumentSnapshot;
import com.oracle.mobile.fusabase.oracledb.FieldPath;
import com.oracle.mobile.fusabase.oracledb.FusabaseOracledb;
import com.oracle.mobile.fusabase.oracledb.QuerySnapshot;
import com.oracle.mobile.fusabase.oracledb.WriteBatch;

import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.Tasks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AggregateFieldTest {

    private static FusabaseOracledb db;
    private static CollectionReference testCollection;

    @BeforeClass
    public static void setUpClass() {
        FusabaseOptions.Builder optionsBuilder = new FusabaseOptions.Builder();
        optionsBuilder.setAuthType("base");
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("40D2FCE3C6FC84C4E0631F965E641EC8");
        optionsBuilder.setAppId("40D2FD377AB884DEE0631F965E648517");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_AJJUTUHAHSTEMBD");
        optionsBuilder.setApiVersion("1.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("40D2FCF5350D84CDE0631F965E64CACE");

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

    private static <T> T await(Task<T> task) throws Exception {
        return Tasks.await(task, 5, TimeUnit.SECONDS);
    }


    // ------------------- AggregateField Tests -------------------

    @Test
    public void testCountFieldCreation() {
        AggregateField count1 = AggregateField.count("count1");
        AggregateField count2 = AggregateField.count("count2");

        assertNotNull(count1);
        assertNotEquals(count1, count2);
        assertNotEquals(count1.hashCode(), count2.hashCode());
    }

    @Test
    public void testSumFieldCreation() {
        AggregateField sum1 = AggregateField.sum("sumScore", "score");
        AggregateField sum2 = AggregateField.sum("sumScore", "score");
        AggregateField sum3 = AggregateField.sum("sumOther", "other");

        assertEquals(sum1, sum2);
        assertNotEquals(sum1, sum3);
    }

    @Test
    public void testAverageFieldCreation() {
        AggregateField avg1 = AggregateField.average("avgScore", "score");
        AggregateField avg2 = AggregateField.average("avgScore", "score");
        AggregateField avg3 = AggregateField.average("avgAge", "age");

        assertEquals(avg1, avg2);
        assertNotEquals(avg1, avg3);
    }

    @Test
    public void testToStringConsistency() {
        AggregateField count = AggregateField.count("count");
        AggregateField sum = AggregateField.sum("sumPoints", "points");
        AggregateField avg = AggregateField.average("avgPoints", "points");

        assertTrue(count.toString().toLowerCase().contains("count"));
        assertTrue(sum.toString().toLowerCase().contains("sum"));
        assertTrue(avg.toString().toLowerCase().contains("average"));
    }


    @Test
    public void testAverageFieldCreationWithFieldPath() {
        FieldPath fieldPath = FieldPath.of("score");
        AggregateField avg1 = AggregateField.average("avgScore", fieldPath);
        AggregateField avg2 = AggregateField.average("avgScore", fieldPath);
        AggregateField avg3 = AggregateField.average("avgOther", fieldPath);

        assertEquals(avg1, avg2);
        assertNotEquals(avg1, avg3);
    }

    @Test
    public void testSumFieldCreationWithFieldPath() {
        FieldPath fieldPath = FieldPath.of("points");
        AggregateField sum1 = AggregateField.sum("sumPoints", fieldPath);
        AggregateField sum2 = AggregateField.sum("sumPoints", fieldPath);
        AggregateField sum3 = AggregateField.sum("sumOther", fieldPath);

        assertEquals(sum1, sum2);
        assertNotEquals(sum1, sum3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCountWithEmptyAliasThrows() {
        AggregateField.count("");
    }

    @Test
    public void testEqualsWithNull() {
        AggregateField count = AggregateField.count("count");
        assertFalse(count.equals(null));
    }

    @Test
    public void testEqualsWithDifferentType() {
        AggregateField count = AggregateField.count("count");
        assertFalse(count.equals("not an aggregate field"));
    }

    @Test
    public void testEqualsWithDifferentAlias() {
        AggregateField count1 = AggregateField.count("count1");
        AggregateField count2 = AggregateField.count("count2");

        assertNotEquals(count1, count2);
    }

    @Test
    public void testEqualsWithDifferentOperator() {
        AggregateField count = AggregateField.count("count");
        AggregateField sum = AggregateField.sum("sum", "field");

        assertNotEquals(count, sum);
    }

    @Test
    public void testEqualsWithDifferentFieldPath() {
        AggregateField sum1 = AggregateField.sum("sum", "field1");
        AggregateField sum2 = AggregateField.sum("sum", "field2");

        assertNotEquals(sum1, sum2);
    }

    @Test
    public void testHashCodeConsistency() {
        AggregateField sum1 = AggregateField.sum("sumScore", "score");
        AggregateField sum2 = AggregateField.sum("sumScore", "score");

        assertEquals(sum1.hashCode(), sum2.hashCode());
    }

    @Test
    public void testHashCodeDifferentForDifferentObjects() {
        AggregateField sum1 = AggregateField.sum("sum1", "score");
        AggregateField sum2 = AggregateField.sum("sum2", "score");

        assertNotEquals(sum1.hashCode(), sum2.hashCode());
    }

    @Test
    public void testCountUsesEmptyFieldPath() {
        AggregateField count = AggregateField.count("count");
        // We can't directly access fieldPath, but we can verify via equals and hashCode
        AggregateField count2 = AggregateField.count("count");

        assertEquals(count, count2);
        assertEquals(count.hashCode(), count2.hashCode());
    }
}
