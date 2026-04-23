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

package com.oracle.test.storage;

import static com.oracle.mobile.fusabase.task.Tasks.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.storage.FileDownloadTask;
import com.oracle.mobile.fusabase.storage.ListResult;
import com.oracle.mobile.fusabase.storage.Storage;
import com.oracle.mobile.fusabase.storage.StorageMetadata;
import com.oracle.mobile.fusabase.storage.StorageReference;
import com.oracle.mobile.fusabase.storage.UploadTask;
import com.oracle.mobile.fusabase.task.Task;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class StorageReferenceTest {

    private static Storage storage;
    private static StorageReference rootRef;
    private static Context context;

    @BeforeClass
    public static void setupClass() {
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
        optionsBuilder.setStorageBucket("dbfs_MOUMJWMRDIYGRCF");
        optionsBuilder.setApiVersion("2.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("43C6C6DC03224D43E0631F965E6456DC");
        optionsBuilder.setUseSocket(true);
        optionsBuilder.setEnableLogging(true);
        optionsBuilder.setUploadChunkSize(1024*1024*5);

        FusabaseApp app;

        try{
            app = FusabaseApp.getInstance();
        } catch (Exception e)
        {
            Log.w("FusabaseTest", "Cannot find default instance. Creating the default instance.");
            app = FusabaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    , optionsBuilder.build());
        }
        storage = Storage.getInstance(app);
        rootRef = storage.getReference().child("metadataTests");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // Clean up after tests
        ListResult listResult = await(rootRef.listAll());
        for (StorageReference ref : listResult.getItems()) {
            await(ref.delete());
        }
        for (StorageReference dir : listResult.getPrefixes()) {
            ListResult innerList = await(dir.listAll());
            for (StorageReference item : innerList.getItems()) {
                await(item.delete());
            }
        }
    }

    private static <T> T awaitResult(Task<T> task) throws Exception {
        final Object[] resultHolder = new Object[1];
        final Exception[] errorHolder = new Exception[1];
        CountDownLatch latch = new CountDownLatch(1);

        task.addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                resultHolder[0] = t.getResult();
            } else {
                errorHolder[0] = t.getException();
            }
            latch.countDown();
        });

        if (!latch.await(400, TimeUnit.SECONDS)) {
            throw new AssertionError("Task timed out");
        }
        if (errorHolder[0] != null) throw errorHolder[0];
        return (T) resultHolder[0];
    }



    // ----------- NAVIGATION & INFO API -----------

    @Test
    public void testBasicReferenceProperties() {
        assertNotNull(rootRef.getBucket());
        assertEquals("test_storage_reference", rootRef.getName());
        assertTrue(rootRef.getPath().contains("test_storage_reference"));
        assertNotNull(rootRef.getRoot());

        StorageReference childRef = rootRef.child("child.txt");
        assertEquals("child.txt", childRef.getName());
        assertEquals(rootRef, childRef.getParent());
    }

    // ----------- UPLOAD TESTS -----------

    @Test
    public void testPutBytes() throws Exception {
        byte[] data = "hello world".getBytes();
        StorageReference ref = rootRef.child("bytes.txt");

        UploadTask.TaskSnapshot snapshot = awaitResult(ref.putBytes(data));
        assertNotNull(snapshot.getMetadata());
//        assertEquals("bytes.txt", snapshot.getMetadata().getName());
        assertEquals("test_storage_reference/bytes.txt", snapshot.getMetadata().getPath());
    }

    @Test
    public void testPutFile() throws Exception {
        File localFile = File.createTempFile("upload", ".txt");
        try (FileOutputStream fos = new FileOutputStream(localFile)) {
            fos.write("file content".getBytes());
        }

        Uri fileUri = Uri.fromFile(localFile);
        StorageReference ref = rootRef.child("file.txt");

        UploadTask.TaskSnapshot snapshot = awaitResult(ref.putFile(fileUri));
        assertNotNull(snapshot.getMetadata());
//        assertEquals("file.txt", snapshot.getMetadata().getName());
        assertEquals("test_storage_reference/file.txt", snapshot.getMetadata().getPath());

    }

    // ----------- METADATA TESTS -----------

    @Test
    public void testGetMetadata() throws Exception {
        StorageReference ref = rootRef.child("meta.txt");
        awaitResult(ref.putBytes("metadata".getBytes()));

        StorageMetadata metadata = awaitResult(ref.getMetadata());
        assertNotNull(metadata);
        assertNotNull(metadata.getSizeBytes());
        assertNotNull(metadata.getReference());
        assertNotNull(metadata.getCreationTimeMillis());
        assertNotNull(metadata.getBucket());
        assertNotNull(metadata.getUpdatedTimeMillis());
        assertNotNull(metadata.getName());
        assertNotNull(metadata.getPath());
    }

    // ----------- DOWNLOAD TESTS -----------

    @Test
    public void testGetDownloadUrlAndFile() throws Exception {
        StorageReference ref = rootRef.child("download.txt");
        awaitResult(ref.putBytes("downloadable content".getBytes()));

        Uri downloadUrl = awaitResult(ref.getDownloadUrl());
        assertNotNull(downloadUrl.toString());

        File localFile = File.createTempFile("downloaded", ".txt");
        FileDownloadTask.TaskSnapshot snapshot = awaitResult(ref.getFile(localFile));
        assertTrue(localFile.exists());
        assertTrue(localFile.length() > 0);
    }

    @Test
    public void testStreamingDownloadToFile() throws Exception {
        // Test streaming download functionality with larger content
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is test content for streaming download. Line ").append(i).append("\n");
        }
        byte[] data = largeContent.toString().getBytes();

        StorageReference ref = rootRef.child("streaming_download.txt");
        awaitResult(ref.putBytes(data));

        // Download using streaming
        File localFile = File.createTempFile("streaming_downloaded", ".txt");
        FileDownloadTask.TaskSnapshot snapshot = awaitResult(ref.getFile(localFile));

        assertTrue("Downloaded file should exist", localFile.exists());
        assertTrue("Downloaded file should not be empty", localFile.length() > 0);
        assertEquals("Downloaded file size should match original", data.length, localFile.length());

        // Verify content matches by comparing byte arrays to avoid OOM with large strings
        java.io.FileInputStream fis = new java.io.FileInputStream(localFile);
        byte[] downloadedData = new byte[(int) localFile.length()];
        fis.read(downloadedData);
        fis.close();

        assertEquals("Downloaded content length should match", data.length, downloadedData.length);
        // Compare first 1000 bytes and last 1000 bytes to verify content integrity
        for (int i = 0; i < Math.min(1000, data.length); i++) {
            assertEquals("Content mismatch at byte " + i, data[i], downloadedData[i]);
        }
        for (int i = Math.max(0, data.length - 1000); i < data.length; i++) {
            assertEquals("Content mismatch at byte " + i, data[i], downloadedData[i]);
        }
    }

    @Test
    public void testGetBytesWithLargerFile() throws Exception {
        // Test getBytes() functionality with larger content to verify JSON/base64 handling
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 700000; i++) {
            content.append("This is test content for getBytes download. Line ").append(i).append("\n");
        }
        byte[] data = content.toString().getBytes();

        StorageReference ref = rootRef.child("getbytes_download.txt");
        awaitResult(ref.putBytes(data));

        // Download using getBytes with sufficient max size
        long maxDownloadSize = data.length + 1024; // Add buffer
        Task<byte[]> downloadTask = ref.getBytes(maxDownloadSize);
        byte[] downloadedBytes = awaitResult(downloadTask);

        assertNotNull("Downloaded bytes should not be null", downloadedBytes);
        assertEquals("Downloaded size should match original", data.length, downloadedBytes.length);

        // Verify content matches by comparing byte arrays
        assertEquals("Downloaded content length should match", data.length, downloadedBytes.length);
        // Compare first 1000 bytes and last 1000 bytes to verify content integrity
        for (int i = 0; i < Math.min(1000, data.length); i++) {
            assertEquals("Content mismatch at byte " + i, data[i], downloadedBytes[i]);
        }
        for (int i = Math.max(0, data.length - 1000); i < data.length; i++) {
            assertEquals("Content mismatch at byte " + i, data[i], downloadedBytes[i]);
        }
    }

    // ----------- LIST & DELETE TESTS -----------

    @Test
    public void testListAndDelete() throws Exception {
        StorageReference ref1 = rootRef.child("list1.txt");
        StorageReference ref2 = rootRef.child("list2.txt");
        awaitResult(ref1.putBytes("file1".getBytes()));
        awaitResult(ref2.putBytes("file2".getBytes()));

        ListResult result = awaitResult(rootRef.listAll());
        assertTrue(result.getItems().size() >= 2);

        awaitResult(ref1.delete());
        awaitResult(ref2.delete());

        ListResult afterDelete = awaitResult(rootRef.listAll());
        assertFalse(afterDelete.getItems().contains(ref1));
        assertFalse(afterDelete.getItems().contains(ref2));
    }
}
