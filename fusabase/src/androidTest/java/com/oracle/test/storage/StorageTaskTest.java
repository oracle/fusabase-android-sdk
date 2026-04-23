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

//package com.oracle.test.storage;
//
//import static org.junit.Assert.*;
//
//import android.net.Uri;
//
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//import androidx.test.platform.app.InstrumentationRegistry;
//
//import com.oracle.mobile.fusabase.storage.FileDownloadTask;
//import com.oracle.mobile.fusabase.storage.Storage;
//import com.oracle.mobile.fusabase.storage.StorageReference;
//import com.oracle.mobile.fusabase.storage.UploadTask;
//import com.oracle.mobile.fusabase.storage.StorageTask;
//
//import org.junit.*;
//import org.junit.runner.RunWith;
//
//import java.io.File;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//@RunWith(AndroidJUnit4.class)
//public class StorageTaskTest {
//
//    private static Storage storage;
//    private static StorageReference rootRef;
//
//    @BeforeClass
//    public static void setUpClass() throws Exception {
//        storage = Storage.getInstance();
//        rootRef = storage.getReference().child("taskTests");
//        // Clean up
//        for (StorageReference ref : rootRef.listAll().getResult(10, TimeUnit.SECONDS).getItems()) {
//            ref.delete().getResult(10, TimeUnit.SECONDS);
//        }
//    }
//
//    @AfterClass
//    public static void tearDownClass() throws Exception {
//        for (StorageReference ref : rootRef.listAll().getResult(10, TimeUnit.SECONDS).getItems()) {
//            ref.delete().getResult(10, TimeUnit.SECONDS);
//        }
//    }
//
//    @Test
//    public void testUploadTaskSuccessListeners() throws Exception {
//        StorageReference fileRef = rootRef.child("uploadSuccess.txt");
//        byte[] data = "Hello StorageTask".getBytes();
//
//        CountDownLatch latch = new CountDownLatch(1);
//
//        UploadTask task = fileRef.putBytes(data);
//        task.addOnSuccessListener(snapshot -> latch.countDown());
//        task.addOnFailureListener(e -> fail("Upload should succeed"));
//
//        assertTrue(latch.await(10, TimeUnit.SECONDS));
//        assertTrue(task.isComplete());
//        assertFalse(task.isCanceled());
//        assertEquals(StorageTask.State.SUCCESS, task.getSnapshot().getTask().getResult().getTask().getSnapshot().getTask().getSnapshot().getState());
//    }
//
//    @Test
//    public void testUploadTaskPauseResumeCancel() throws Exception {
//        StorageReference fileRef = rootRef.child("uploadPause.txt");
//        byte[] data = new byte[500 * 1024]; // 500 KB (big enough to allow pause)
//
//        UploadTask task = fileRef.putBytes(data);
//
//        assertNotNull(task.pause());
//        assertTrue(task.isPaused());
//
//        assertNotNull(task.resume());
//        assertFalse(task.isPaused());
//
//        assertNotNull(task.cancel());
//        assertTrue(task.isCanceled());
//    }
//
//    @Test
//    public void testProgressListener() throws Exception {
//        StorageReference fileRef = rootRef.child("uploadProgress.txt");
//        byte[] data = new byte[200 * 1024]; // 200 KB
//
//        CountDownLatch latch = new CountDownLatch(1);
//
//        UploadTask task = fileRef.putBytes(data);
//        task.addOnProgressListener(snapshot -> {
//            long bytesTransferred = snapshot.getBytesTransferred();
//            long totalBytes = snapshot.getTotalByteCount();
//            assertTrue(bytesTransferred >= 0 && totalBytes > 0);
//            if (bytesTransferred == totalBytes) {
//                latch.countDown();
//            }
//        });
//
//        assertTrue(latch.await(10, TimeUnit.SECONDS));
//        assertTrue(task.isComplete());
//    }
//
//    @Test
//    public void testFailureListener() throws Exception {
//        StorageReference invalidRef = storage.getReference().child("///badPath/file.txt");
//        byte[] data = "bad".getBytes();
//
//        CountDownLatch latch = new CountDownLatch(1);
//
//        UploadTask task = invalidRef.putBytes(data);
//        task.addOnFailureListener(e -> latch.countDown());
//
//        assertTrue(latch.await(10, TimeUnit.SECONDS));
//        assertTrue(task.isComplete());
//        assertEquals(StorageTask.State.FAILURE, task.getSnapshot().getTask().getSnapshot().getState());
//    }
//
//    @Test
//    public void testDownloadTask() throws Exception {
//        // First upload a file
//        StorageReference fileRef = rootRef.child("downloadFile.txt");
//        byte[] data = "DownloadMe".getBytes();
//        fileRef.putBytes(data).getResult(10, TimeUnit.SECONDS);
//
//        File localFile = File.createTempFile("download", ".txt",
//                InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
//
//        CountDownLatch latch = new CountDownLatch(1);
//
//        FileDownloadTask downloadTask = fileRef.getFile(localFile);
//        downloadTask
//                .addOnSuccessListener(snapshot -> {
//                    assertTrue(localFile.exists());
//                    assertTrue(localFile.length() > 0);
//                    latch.countDown();
//                })
//                .addOnFailureListener(e -> fail("Download should succeed"));
//
//        assertTrue(latch.await(10, TimeUnit.SECONDS));
//        assertTrue(downloadTask.isComplete());
//    }
//}
