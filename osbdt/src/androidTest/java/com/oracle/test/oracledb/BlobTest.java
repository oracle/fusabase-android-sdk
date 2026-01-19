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
import com.oracle.database.osbdt.oracledb.Blob;
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class BlobTest {

    private static OsbdtOracledb db;
    private static CollectionReference testCollection;

    @BeforeClass
    public static void setUpClass() {
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
        return Tasks.await(task, 5, TimeUnit.SECONDS);
    }


    // ------------------- Blob Tests -------------------

    @Test
    public void testCreateAndRetrieveBytes() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        Blob blob = Blob.fromBytes(data);

        assertArrayEquals(data, blob.toBytes());
    }

    @Test
    public void testEmptyBlob() {
        byte[] data = new byte[]{};
        Blob blob = Blob.fromBytes(data);

        assertEquals(0, blob.toBytes().length);
    }

    @Test
    public void testEqualityAndHashCode() {
        byte[] data1 = new byte[]{10, 20, 30};
        byte[] data2 = new byte[]{10, 20, 30};
        byte[] data3 = new byte[]{40, 50};

        Blob blob1 = Blob.fromBytes(data1);
        Blob blob2 = Blob.fromBytes(data2);
        Blob blob3 = Blob.fromBytes(data3);

        assertEquals(blob1, blob2);
        assertEquals(blob1.hashCode(), blob2.hashCode());
        assertNotEquals(blob1, blob3);
    }

    @Test
    public void testComparison() {
        Blob blob1 = Blob.fromBytes(new byte[]{1, 2, 3});
        Blob blob2 = Blob.fromBytes(new byte[]{1, 2, 3});
        Blob blob3 = Blob.fromBytes(new byte[]{1, 2, 4});
        Blob blob4 = Blob.fromBytes(new byte[]{1, 2});

        assertEquals(0, blob1.compareTo(blob2));
        assertTrue(blob1.compareTo(blob3) < 0);
        assertTrue(blob3.compareTo(blob1) > 0);
        assertTrue(blob4.compareTo(blob1) < 0);
    }

    @Test
    public void testLargeBlob() {
        byte[] data = new byte[1024 * 1024]; // 1 MB
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        Blob blob = Blob.fromBytes(data);

        assertEquals(1024 * 1024, blob.toBytes().length);
        assertEquals((byte) 255, blob.toBytes()[255]);
    }

    @Test
    public void testImmutability() {
        byte[] original = new byte[]{1, 2, 3, 4, 5};
        Blob blob = Blob.fromBytes(original);

        // Modify the original array
        original[0] = 99;

        // Blob should remain unchanged
        byte[] blobBytes = blob.toBytes();
        assertEquals((byte) 1, blobBytes[0]);
        assertEquals((byte) 2, blobBytes[1]);
    }

    @Test
    public void testToBytesReturnsCopy() {
        byte[] original = new byte[]{10, 20, 30};
        Blob blob = Blob.fromBytes(original);

        byte[] returned = blob.toBytes();
        // Modify the returned array
        returned[0] = 99;

        // Original blob should remain unchanged
        byte[] blobBytes = blob.toBytes();
        assertEquals((byte) 10, blobBytes[0]);
        assertEquals((byte) 20, blobBytes[1]);
    }

    @Test(expected = NullPointerException.class)
    public void testFromBytesWithNull() {
        Blob.fromBytes(null);
    }

    @Test
    public void testEqualsWithNull() {
        Blob blob = Blob.fromBytes(new byte[]{1, 2, 3});
        assertFalse(blob.equals(null));
    }

    @Test
    public void testEqualsWithDifferentType() {
        Blob blob = Blob.fromBytes(new byte[]{1, 2, 3});
        assertFalse(blob.equals("not a blob"));
        assertFalse(blob.equals(new byte[]{1, 2, 3}));
    }

    @Test
    public void testHashCodeConsistency() {
        Blob blob = Blob.fromBytes(new byte[]{5, 10, 15});
        int hash1 = blob.hashCode();
        int hash2 = blob.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCodeDifferentForDifferentBlobs() {
        Blob blob1 = Blob.fromBytes(new byte[]{1, 2, 3});
        Blob blob2 = Blob.fromBytes(new byte[]{4, 5, 6});

        assertNotEquals(blob1.hashCode(), blob2.hashCode());
    }

    @Test
    public void testCompareToWithEqualBlobs() {
        Blob blob1 = Blob.fromBytes(new byte[]{1, 2, 3});
        Blob blob2 = Blob.fromBytes(new byte[]{1, 2, 3});

        assertEquals(0, blob1.compareTo(blob2));
    }

    @Test
    public void testCompareToWithDifferentLengths() {
        Blob shorter = Blob.fromBytes(new byte[]{1, 2});
        Blob longer = Blob.fromBytes(new byte[]{1, 2, 3});

        assertTrue(shorter.compareTo(longer) < 0);
        assertTrue(longer.compareTo(shorter) > 0);
    }

    @Test
    public void testCompareToWithEmptyBlobs() {
        Blob empty1 = Blob.fromBytes(new byte[]{});
        Blob empty2 = Blob.fromBytes(new byte[]{});
        Blob nonEmpty = Blob.fromBytes(new byte[]{1});

        assertEquals(0, empty1.compareTo(empty2));
        assertTrue(empty1.compareTo(nonEmpty) < 0);
        assertTrue(nonEmpty.compareTo(empty1) > 0);
    }

    @Test
    public void testCompareToWithSpecialValues() {
        Blob blob1 = Blob.fromBytes(new byte[]{(byte) 0xFF}); // -1 in signed byte
        Blob blob2 = Blob.fromBytes(new byte[]{(byte) 0x00}); // 0 in signed byte

        // In signed byte comparison, -1 < 0, so blob1 < blob2
        assertTrue(blob1.compareTo(blob2) < 0);
        assertTrue(blob2.compareTo(blob1) > 0);
    }

    @Test
    public void testToString() {
        Blob blob = Blob.fromBytes(new byte[]{1, 2, 3});
        String str = blob.toString();

        assertNotNull(str);
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
        assertTrue(str.contains("3"));
    }

    @Test
    public void testToStringEmptyBlob() {
        Blob blob = Blob.fromBytes(new byte[]{});
        String str = blob.toString();

        assertEquals("[]", str);
    }

    @Test
    public void testSingleByteBlob() {
        byte[] data = new byte[]{42};
        Blob blob = Blob.fromBytes(data);

        assertArrayEquals(data, blob.toBytes());
        assertEquals(1, blob.toBytes().length);
    }

    @Test
    public void testBlobWithNegativeBytes() {
        byte[] data = new byte[]{(byte) -1, (byte) -128, (byte) 127};
        Blob blob = Blob.fromBytes(data);

        assertArrayEquals(data, blob.toBytes());
    }
}
