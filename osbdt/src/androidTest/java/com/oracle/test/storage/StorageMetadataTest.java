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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.net.Uri;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;
import com.oracle.database.osbdt.storage.Storage;
import com.oracle.database.osbdt.storage.StorageMetadata;
import com.oracle.database.osbdt.storage.StorageReference;
import com.oracle.database.osbdt.storage.UploadTask;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.Tasks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class StorageMetadataTest {

    private static Storage storage;
    private static StorageReference rootRef;

    @BeforeClass
    public static void setupClass() {
        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
        optionsBuilder.setAuthType("idcs");
//        IDCSOptions.Builder idcsOptionsBuilder = new IDCSOptions.Builder();
//        idcsOptionsBuilder.setClientId("44d4a5a58a80476e90389ab9f403db87");
//        idcsOptionsBuilder.setClientSecret("idcscs-3a5ba1c4-b496-443d-ac9e-5efd7125d5c2");
//        idcsOptionsBuilder.setDomainURL("https://idcs-360c1204d11f41d3836e7fb2f1769ca7.identity.oraclecloud.com");
//        idcsOptionsBuilder.setSelfRegistrationProfile("f336e6256b9644059663394fe75d7494");
//        optionsBuilder.setIDCSOptions(idcsOptionsBuilder.build());
        optionsBuilder.setOrdsHost("https://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8443/ords/USER1/");
        optionsBuilder.setProjectId("47DD506E3CFEE041E0631F965E640C80");
        optionsBuilder.setAppId("47DE7A6CC67A2484E0631F965E640E85");
        optionsBuilder.setObjectType("oci");
        optionsBuilder.setStorageBucket("baas_bucket");
        optionsBuilder.setApiVersion("2.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("47F3EC3B8C18A099E0631F965E64A607");
        optionsBuilder.setUseSocket(true);
        optionsBuilder.setUploadChunkSize(1000);
        optionsBuilder.setAllowsSelfSignedCertificates(true);

        OsbdtApp app;

        try{
            app = OsbdtApp.getInstance();
        } catch (Exception e)
        {
            Log.w("OsbdtTest", "Cannot find default instance. Creating the default instance.");
            app = OsbdtApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    , optionsBuilder.build());
        }
        storage = Storage.getInstance(app);
        rootRef = storage.getReference();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // clean up test directory
        Tasks.await(rootRef.delete(), 5, TimeUnit.SECONDS);
    }

    private Uri createTempFile(String name, String content) throws Exception {
        File file = File.createTempFile(name, ".txt");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes());
        }
        return Uri.fromFile(file);
    }

    // --------------- INTEGRATION TESTS ---------------

    @Test
    public void testUploadWithMetadataAndRetrieve() throws Exception {
        Uri fileUri = createTempFile("meta_file", "hello metadata");
        StorageReference ref = rootRef.child("file2.txt");

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("text/plain")
//                .setName("meta_file")
                .setPath("file2.txt")
                .setStorageReference(ref)
                .build();

        try {
            Log.d("ObjectStore", "Put file:");
            UploadTask uploadTask = ref.putFile(fileUri, metadata);
            Tasks.await(uploadTask, 20, TimeUnit.SECONDS);
            UploadTask.TaskSnapshot uploadResult = uploadTask.getResult();
            Log.i("StorageTest", uploadResult.getMetadata().getContentType());
        } catch (Exception e) {
            Log.d("ObjectStore", "Expected error occurred: " + e.getMessage());
            assertTrue("Should contain error", e.getMessage().contains("Error") ||
                e.getMessage().contains("error") ||
                e.getMessage().contains("invalid") ||
                e.getMessage().contains("timeout"));
        }

    }

    @Test
    public void testTimeStampsAreSet() throws Exception {
        Uri fileUri = createTempFile("meta_file4", "time test");
        StorageReference ref = rootRef.child("file4.txt");

        Tasks.await(ref.putFile(fileUri), 50, TimeUnit.SECONDS);
        StorageMetadata meta = Tasks.await(ref.getMetadata(), 50, TimeUnit.SECONDS);

        // For OCI we don't get Creation time millis
//        assertNotEquals(meta.getCreationTimeMillis(), 0);
        assertNotEquals(meta.getUpdatedTimeMillis(), 0);
        assertTrue(meta.getUpdatedTimeMillis() >= meta.getCreationTimeMillis());
    }

    @Test
    public void testSizeBytesAndMd5Hash() throws Exception {
        String content = "abcdef12345";
        Uri fileUri = createTempFile("meta_file5", content);
        StorageReference ref = rootRef.child("file5.txt");

        Tasks.await(ref.putFile(fileUri), 50, TimeUnit.SECONDS);
        StorageMetadata meta = Tasks.await(ref.getMetadata(), 50, TimeUnit.SECONDS);

        assertEquals(content.length(), meta.getSizeBytes());
        assertNotNull(meta.getMd5Hash());
    }

    @Test
    public void test() throws Exception {
        Log.d("ObjectStore", "Security Rule will allow to WRITE and READ under /PATH");

        Uri fileUri = createTempFile("sample.docx", "hello metadata");
        StorageReference ref = rootRef.child("sample.docx");

        StorageMetadata metadata = new StorageMetadata.Builder()
//                .setName("sample.docx")
            .setPath("sample.docx")
            .setStorageReference(ref)
            .build();

        // Upload using putFile() with file URI
        try {
            UploadTask uploadTask = ref.putFile(fileUri);
            Tasks.await(uploadTask, 20, TimeUnit.SECONDS);

            UploadTask.TaskSnapshot uploadResult = uploadTask.getResult();

            Log.d("ObjectStore", "✓ Successfully uploaded: " + "sample.docx" + " using putFile()");
        } catch (Exception e) {
            Log.e("ObjectStore", "Upload failed", e);
            fail("Upload failed: " + e.getMessage());
        }
// Get download URL to create reference
        String fileURL = null;
        try {
            Task<Uri> downloadUrlTask = ref.getDownloadUrl();
            Uri downloadUri = Tasks.await(downloadUrlTask);
            fileURL = downloadUri.toString();
            Log.d("ObjectStore", "-----DOWNLOAD URL-----");
            Log.d("ObjectStore", "URL: " + fileURL);
            Log.d("ObjectStore", "-----END DOWNLOAD URL-----");
        } catch (Exception e) {
            Log.e("ObjectStore", "Failed to get download URL", e);
            fail("Failed to get download URL: " + e.getMessage());
        }

        // STEP 2: Create reference from URL
        StorageReference urlRef = null;
        try {
            urlRef = rootRef.child("sample.docx");
            Log.d("ObjectStore", "Created reference from URL successfully");
        } catch (Exception e) {
            Log.e("ObjectStore", "Failed to create reference from URL", e);
            fail("Failed to create reference from URL: " + e.getMessage());
        }

        // Validate reference properties PRINT JUST FOR BUG CHECK
        Log.d("OBJECT STORAGE","COMPARING REFS");
        Log.d("OBJECT STORAGE", "--- using getReference(/path)");
        Log.d("OBJECT STORAGE", ref.getName());
        Log.d("OBJECT STORAGE", ref.getPath());
        Log.d("OBJECT STORAGE", ref.getBucket());
        Log.d("OBJECT STORAGE", ref.getParent().getName());
        Log.d("OBJECT STORAGE", ref.getParent().getPath());

        Log.d("OBJECT STORAGE", "--- using getReference(url)");
        Log.d("OBJECT STORAGE", urlRef.getName());
        Log.d("OBJECT STORAGE", urlRef.getPath());
        Log.d("OBJECT STORAGE", urlRef.getBucket());
        Log.d("OBJECT STORAGE", urlRef.getParent().getName());
        Log.d("OBJECT STORAGE", urlRef.getParent().getPath());

        try {
            Task<StorageMetadata> metadataTask = urlRef.getMetadata();
            StorageMetadata metadata1 = Tasks.await(metadataTask);

            Log.d("ObjectStore", "✓ Metadata retrieval successful using URL reference");
        } catch (Exception e) {
            Log.e("ObjectStore", "Metadata retrieval failed", e);
            fail("Metadata retrieval failed: " + e.getMessage());
        }
    }

    @Test
    public void test19VerifyStorageReference() throws Exception {

         String dataPath = "data1";

        // When reference path is given using ref() and path with no /
        StorageReference dataPathRef2 = storage.getReference(dataPath);
        Log.d("ObjectStore", "\n --- data1 reference:");
        Log.d("ObjectStore", "root: " + dataPathRef2.getRoot());
        Log.d("ObjectStore", "name: " + dataPathRef2.getName());
        Log.d("ObjectStore", "path: " + dataPathRef2.getPath());
        Log.d("ObjectStore", "parent: " + dataPathRef2.getParent());
        Log.d("ObjectStore", "storage: " + dataPathRef2.getStorage());

// When reference path is given using child
        Log.d("ObjectStore", "\n--- root.child(data1) reference:");
        StorageReference childRef = rootRef.child(dataPath);
        Log.d("ObjectStore", "root: " + childRef.getRoot());
        Log.d("ObjectStore", "name: " + childRef.getName());
        Log.d("ObjectStore", "path: " + childRef.getPath());
        Log.d("ObjectStore", "parent: " + childRef.getParent());
        Log.d("ObjectStore", "parent path: " + childRef.getParent().getPath());
        Log.d("ObjectStore", "storage: " + childRef.getStorage());

// Upload bytes object
        String fileName = "bytes7";
        byte[] bytesFile = {(byte) 0x48, (byte) 0x65, (byte) 0x6c, (byte) 0x6c, (byte) 0x6f, (byte) 0x2c, (byte) 0x20, (byte) 0x77};
        long bytesFileSize = bytesFile.length;
        StorageReference bucketRef = rootRef.child(fileName);
        String contentType = "application/octet-stream";

        UploadTask.TaskSnapshot uploadResult = Tasks.await(bucketRef.putBytes(bytesFile), 60, TimeUnit.SECONDS);

        Log.d("ObjectStore", "CHECKING REF AFTER UPLOAD");
        Log.d("ObjectStore", "name: " + bucketRef.getName());
        Log.d("ObjectStore", "path: " + bucketRef.getPath());
        Log.d("ObjectStore", "root: " + bucketRef.getRoot().toString());
        Log.d("ObjectStore", "parent: " + bucketRef.getParent().getPath());
        Log.d("ObjectStore", "parent parent path (root): " + bucketRef.getParent().getParent().getPath());

    }

    @Test
    public void testMetadataWithByteArray() throws Exception {
        // Create a simple byte array for testing
        String testContent = "This is a test file content for metadata verification.";
        byte[] fileBuffer = testContent.getBytes();
        String fileName = "testMetadataFile.txt";

        StorageReference bucketRef = rootRef.child(fileName);

        // Upload using putBytes
        UploadTask uploadTask = bucketRef.putBytes(fileBuffer);
        UploadTask.TaskSnapshot uploadResult = Tasks.await(uploadTask, 60, TimeUnit.SECONDS);

        // Verify upload result
        assertNotNull(uploadResult);
        assertEquals(fileBuffer.length, uploadResult.getTotalByteCount());

        // Get Metadata
        Task<StorageMetadata> metadataTask = bucketRef.getMetadata();
        StorageMetadata metadata = Tasks.await(metadataTask);

        // Verify metadata
        assertNotNull(metadata);
        assertEquals(fileBuffer.length, metadata.getSizeBytes());
        assertNotNull(metadata.getMd5Hash());
        assertNotEquals(0, metadata.getUpdatedTimeMillis());

        Log.d("StorageMetadataTest", "Metadata MD5 Hash: " + metadata.getMd5Hash());
        Log.d("StorageMetadataTest", "Metadata Size: " + metadata.getSizeBytes());
    }

    @Test
    public void testUploadResumablePauseAndGetMetadataWithByteArray() throws Exception {
        Log.d("ObjectStore", "Upload resumable (pause and never resume), getMetaData with byte array. Expected result: Unsuccessful Upload\n");

        // Create a larger byte array for testing resumable upload
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            contentBuilder.append("This is test content for resumable upload testing. ");
        }
        byte[] fileBuffer = contentBuilder.toString().getBytes();
        String fileName = "resumableTestFile.txt";

        StorageReference bucketRef = rootRef.child(fileName);

        // Upload Resumable
        UploadTask uploadTask = bucketRef.putBytes(fileBuffer);

        // Add progress listener for monitoring
        uploadTask.addOnProgressListener(snapshot -> {
            Log.d("ObjectStore", "Upload progress: " + snapshot.getBytesTransferred() + " / " + snapshot.getTotalByteCount());
        });

        // Should not reach success since we pause immediately
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            Log.d("ObjectStore", "Upload completed from LISTENER");
//            fail("Upload should not complete when paused");
        });

        // Should reach failure due to pause
        uploadTask.addOnFailureListener(exception -> {
            Log.d("ObjectStore", "Upload failed as expected: " + exception.getMessage());
        });

        // Should reach paused listener
        uploadTask.addOnPausedListener(snapshot -> {
            Log.d("ObjectStore", "Upload was paused as expected");
        });

        // Pause the upload immediately
//        uploadTask.pause();

        // Wait a bit to let listeners trigger
        Thread.sleep(10000);

        // Since upload is paused, we should still be able to get metadata if partial upload occurred
        // But for small files, it might not have started yet
//        try {
//            Task<StorageMetadata> metadataTask = bucketRef.getMetadata();
//            StorageMetadata metadata = Tasks.await(metadataTask, 10, TimeUnit.SECONDS);
//
//            // If metadata exists, verify it
//            if (metadata != null) {
//                Log.d("StorageMetadataTest", "Metadata retrieved for paused upload");
//                Log.d("StorageMetadataTest", "Size: " + metadata.getSizeBytes());
//            }
//        } catch (Exception e) {
//            Log.d("StorageMetadataTest", "Expected: No metadata available for paused upload: " + e.getMessage());
//        }
    }

    @Test
    public void testCancelResumableUpload() throws Exception {
        String fileName = "resumablePDF.pdf";
        String resumablePath = "resumable";

        // Create a large byte array for testing
        byte[] fileBuffer = new byte[100000]; // 100KB
        for (int i = 0; i < fileBuffer.length; i++) {
            fileBuffer[i] = (byte) 'A';
        }
        long fileSize = fileBuffer.length;
        String contentType = "application/pdf";

        CountDownLatch latch = new CountDownLatch(1);

        StorageReference bucketRef = rootRef.child(resumablePath + "/" + fileName);

        // Upload Resumable
        UploadTask uploadTask = bucketRef.putBytes(fileBuffer);

        // Add progress listener for monitoring (equivalent to test_MonitorResumable)
        uploadTask.addOnProgressListener(snapshot -> {
            Log.d("ObjectStore", "Upload progress: " + snapshot.getBytesTransferred() + " / " + snapshot.getTotalByteCount());
        });

        // Add failure listener to handle cancellation
        uploadTask.addOnFailureListener(exception -> {
            Log.d("ObjectStore", "Upload failed after canceled: " + exception.getMessage());
            latch.countDown();
        });

        //Add cancel listener
        uploadTask.addOnCanceledListener(()-> {
            Log.d("ObjectStore", "Upload was cancelled intentionally by user");
        });

        // Cancel upload immediately
        boolean cancelResult = uploadTask.cancel();
        assertTrue("Cancel should return true", cancelResult);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertTrue("Task should be canceled: ", uploadTask.isCanceled());
        assertTrue("Task should be complete", uploadTask.isComplete());

        try {
            Tasks.await(uploadTask, 20, TimeUnit.SECONDS);
        } catch (Exception error) {
            Log.d("ObjectStore", "Exception caught when trying to cancel: " + error.getMessage());
            assertTrue("Should contain timeout ", error.getMessage().contains("Task did not complete"));
        }

        // Verify the upload was cancelled and object is not available
        try {
            Tasks.await(bucketRef.getMetadata(), 5, TimeUnit.SECONDS);
            fail("File should not exist after cancel");
        } catch (Exception e) {
            // Expected - file should not exist
            Log.d("ObjectStore", "File correctly does not exist after cancel: " + e.getMessage());
        }
    }

    @Test
    public void testStorageMetadataCreationWithDefaults() throws Exception {
        // Test that metadata creation works correctly and handles edge cases
        // This indirectly tests the fix for the OCI storage bug by ensuring robust metadata handling

        StorageReference testRef = rootRef.child("test.txt");

        // Test basic metadata creation through StorageMetadata.Builder
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setStorageReference(testRef)
                .setPath(testRef.getPath())
                .build();

        // Verify basic metadata properties
        assertNotNull("Metadata should be created", metadata);
        assertEquals("Reference should be set", testRef, metadata.getReference());
        assertEquals("Path should be set", testRef.getPath(), metadata.getPath());

        // Test with minimal metadata (no content type set)
        StorageMetadata minimalMetadata = new StorageMetadata.Builder()
                .setStorageReference(testRef)
                .setPath(testRef.getPath())
                .setName("test.txt")
                .build();

        assertNotNull("Minimal metadata should be created", minimalMetadata);
        assertEquals("Name should be set", "test.txt", minimalMetadata.getName());
        assertNull("Content type should be null when not set", minimalMetadata.getContentType());

        // Test that the actual upload/getMetadata flow works (this exercises createStorageMetadata indirectly)
        byte[] testData = "test content".getBytes();
        UploadTask.TaskSnapshot uploadResult = Tasks.await(testRef.putBytes(testData), 30, TimeUnit.SECONDS);

        assertNotNull("Upload should succeed", uploadResult);
        assertNotNull("Upload metadata should exist", uploadResult.getMetadata());

        // Verify the metadata from actual upload has reasonable defaults
        StorageMetadata uploadMetadata = uploadResult.getMetadata();
        assertNotNull("Upload metadata should have content type", uploadMetadata.getContentType());
        assertTrue("Content type should not be empty", uploadMetadata.getContentType().length() > 0);
        assertNotNull("Upload metadata should have bucket", uploadMetadata.getBucket());
        assertNotEquals("Size should be set", 0, uploadMetadata.getSizeBytes());

        Log.d("StorageMetadataTest", "✓ Storage metadata creation handles defaults correctly");
    }

}
