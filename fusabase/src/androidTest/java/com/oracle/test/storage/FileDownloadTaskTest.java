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
//import android.content.Context;
//import android.net.Uri;
//
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//import androidx.test.platform.app.InstrumentationRegistry;
//
//import com.oracle.mobile.fusabase.storage.FileDownloadTask;
//import com.oracle.mobile.fusabase.storage.Storage;
//import com.oracle.mobile.fusabase.storage.StorageReference;
//
//import org.junit.*;
//import org.junit.runner.RunWith;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//@RunWith(AndroidJUnit4.class)
//public class FileDownloadTaskTest {
//
//    private static Storage storage;
//    private static StorageReference rootRef;
//    private static Context context;
//
//    @BeforeClass
//    public static void setUpClass() throws Exception {
//        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
//        storage = Storage.getInstance();
//        rootRef = storage.getReference().child("fileDownloadTests");
//
//        // Upload a seed file for download tests
//        StorageReference seedRef = rootRef.child("seed.txt");
//        byte[] data = "This is a seed file for download testing.".getBytes();
//        seedRef.putBytes(data).getResult(10, TimeUnit.SECONDS);
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
//    public void testDownloadSuccess() throws Exception {
//        StorageReference fileRef = rootRef.child("seed.txt");
//        File localFile = File.createTempFile("download_success", ".txt", context.getCacheDir());
//
//        CountDownLatch latch = new CountDownLatch(1);
//
//        FileDownloadTask task = fileRef.getFile(localFile);
//        task.addOnSuccessListener(snapshot -> {
//            assertTrue(localFile.exists());
//            assertTrue(localFile.length() > 0);
//            latch.countDown();
//        }).addOnFailureListener(e -> fail("Download should succeed"));
//
//        assertTrue(latch.await(10, TimeUnit.SECONDS));
//        assertTrue(task.isComplete());
//        assertFalse(task.isCanceled());
//    }
//
//    @Test
//    public void testDownloadFailure() throws Exception {
//        StorageReference invalidRef = rootRef.child("non_existing_file.txt");
//        File localFile = File.createTempFile("download_failure", ".txt", context.getCacheDir());
//
//        CountDownLatch latch = new CountDownLatch(1);
//
//        FileDownloadTask task = invalidRef.getFile(localFile);
//        task.addOnFailureListener(e -> latch.countDown());
//
//        assertTrue(latch.await(10, TimeUnit.SECONDS));
//        assertTrue(task.isComplete());
//        assertEquals(FileDownloadTask.TaskSnapshot.class, task.getSnapshot().getClass());
//    }
//
//    @Test
//    public void testDownloadProgressListener() throws Exception {
//        StorageReference fileRef = rootRef.child("seed.txt");
//        File localFile = File.createTempFile("download_progress", ".txt", context.getCacheDir());
//
//        CountDownLatch latch = new CountDownLatch(1);
//
//        FileDownloadTask task = fileRef.getFile(localFile);
//        task.addOnProgressListener(snapshot -> {
//            long bytesTransferred = snapshot.getBytesTransferred();
//            long totalBytes = snapshot.getTotalByteCount();
//            assertTrue(totalBytes > 0);
//            assertTrue(bytesTransferred >= 0);
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
//    public void testPauseResumeCancel() throws Exception {
//        // Create a large file to upload for a bigger download
//        File largeFile = File.createTempFile("large_seed", ".bin", context.getCacheDir());
//        try (FileOutputStream fos = new FileOutputStream(largeFile)) {
//            fos.write(new byte[1024 * 1024]); // 1 MB
//        }
//        StorageReference largeRef = rootRef.child("large_seed.bin");
//        largeRef.putFile(Uri.fromFile(largeFile)).getResult(20, TimeUnit.SECONDS);
//
//        File localFile = File.createTempFile("download_pause_resume", ".bin", context.getCacheDir());
//
//        FileDownloadTask task = largeRef.getFile(localFile);
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
//}
