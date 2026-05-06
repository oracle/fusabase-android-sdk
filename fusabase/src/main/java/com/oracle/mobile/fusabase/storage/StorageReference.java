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

package com.oracle.mobile.fusabase.storage;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.utils.Utils;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.json.JsonObject;

/**
 * Class StorageReference
 * This class represents a path in the BAAS Storage and provides an reference to it.
 */
public class StorageReference {

    private static final String TAG = "StorageReference";
    private final int DEFAULT_MAX_DOWNLOAD_SIZE = -1;
    private final int MAX_RESULTS = 1000;
    public final String DEFAULT_NEXT_PAGE_TOKEN = "0";
    private final String DEFAULT_ROOT_PATH = "";

    private final Storage storage;
    private final String objectId;
    private final Context context;
    private String name;
    private final List<String> path;
    private final List<FileDownloadTask> fileDownloadTasks;
    private final List<UploadTask> fileUploadTasks;
    private final List<StreamDownloadTask> streamDownloadTasks;
    @Nullable private StorageReference parent;
    @NonNull private final StorageHelper storageHelper;

    /**
     * Constructor
     * @param storageInstance an {@code Storage} A storageInstance
     * @param path an {@code String}  Path of the Object
     */
    protected StorageReference(@NonNull Storage storageInstance, @NonNull String path) {
        this.storage = storageInstance;
        this.context = this.storage.getApp().context;
        this.path = new ArrayList<>(Arrays.asList(path.split("/")));

        if(this.path.isEmpty())
            this.path.add("");

        this.objectId = this.path.remove(this.path.size() -1);

        if(!this.path.isEmpty())
        {
            List<String> parentPath = new ArrayList<>(this.path);
            this.parent = new StorageReference(storageInstance, String.join("/", parentPath));
        }
        this.path.add(objectId);
        this.fileDownloadTasks = new ArrayList<>();
        this.fileUploadTasks = new ArrayList<>();
        this.streamDownloadTasks = new ArrayList<>();
        this.storageHelper = new StorageHelper(storageInstance);
        this.name = objectId;
    }

    /**
     * Method to get the reference to the child of this storage reference.
     * @param pathString an {@code String} Path to the child.
     * @return StorageReference
     */
    @NonNull public StorageReference child(@NonNull String pathString) {
        return new StorageReference(this.storage, String.join("/", this.path) + "/" + pathString);
    }

    /**
     *
     * @param other
     * @return
     */
    public int compareTo (@NonNull StorageReference other) {
        return (this.path == other.path) ? 1:0;
    }

    /**
     * Method to delete an object from BAAS storage referred by this reference.
     * @return Task<Void> An asynchronous task that represents the operation of deleting the object.
     */
    @NonNull public Task<Void> delete () {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject result = this.storageHelper.deleteObject(this);
                taskCompletionSource.setResult(null);
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Method to check if the provided Object is same as this object.
     * @param other {@code Object}  Object which is to be compared.
     * @return boolean true if object is same as this reference otherwise false.
     */
    public boolean equals (Object other) {
        if(other instanceof StorageReference)
        {
            StorageReference otherRef = (StorageReference) other;
            return this.path.equals(otherRef.path) &&
                    this.fileUploadTasks.equals(otherRef.fileUploadTasks) &&
                    this.fileDownloadTasks.equals(otherRef.fileDownloadTasks) &&
                    this.parent != null ?
                    this.parent.equals(otherRef.parent) :
                    otherRef.parent == null &&
                    this.objectId.equals(otherRef.objectId);
        }

        return false;
    }

    /**
     * Returns a hash code value for this StorageReference.
     * @return int hash code value.
     */
    public int hashCode() {
        return java.util.Objects.hash(path, fileUploadTasks, fileDownloadTasks, parent, objectId);
    }

    /**
     * Method to return the list of active download tasks.
     * @return List<FileDownloadTask> A List containing all the active download task.
     */
    @NonNull public  List<FileDownloadTask>  getActiveDownloadTasks() {
        return this.fileDownloadTasks;
    }

    /**
     * Method to return the list of active upload tasks.
     * @return List<UploadTask> A List containing all the active upload task.
     */
    @NonNull public  List<UploadTask> getActiveUploadTasks() {
        return this.fileUploadTasks;
    }

    /**
     * Method the get the bucket name of the BAAS storage.
     * @return String Name of the bucket.
     */
    @NonNull public  String getBucket() {
        return this.storage.getBucketName();
    }

    /**
     * Method to fetch the object in a byte array with the maximum download size.
     * @param maxDownloadSizeBytes {@code long} Maximum download size for the object
     * @return Task<byte[]> An asynchronous task which on completion provides the byte array containing the object.
     */
    @NonNull public Task<byte[]> getBytes(long maxDownloadSizeBytes) {

        if(maxDownloadSizeBytes < 0)
        {
            throw new IllegalArgumentException("Invalid maxDownloadSizeBytes provided");
        }

        TaskCompletionSource<byte[]> taskCompletionSource = new TaskCompletionSource<>();
        Task<byte[]> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Fail fast based on verified server metadata size.
                StorageMetadata metadata = this.storageHelper.createStorageMetadata(this, this.storageHelper.getMetadata(this));
                if (metadata.getSizeBytes() > maxDownloadSizeBytes) {
                    throw new StorageException(
                            "Download size " + metadata.getSizeBytes() + " exceeds maxDownloadSizeBytes " + maxDownloadSizeBytes,
                            StorageException.Code.INVALID_ARGUMENT);
                }
                byte[] result = this.storageHelper.getObject(this, maxDownloadSizeBytes);
                taskCompletionSource.setResult(result);
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Method to get the download url for the object at this reference.
     * @return Task<Uri> An asynchronous task which provides the download uri of the object.
     */
    @NonNull public  Task<Uri> getDownloadUrl() {
        TaskCompletionSource<Uri> taskCompletionSource = new TaskCompletionSource<>();
        Task<Uri> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                Uri downloadUri = this.storageHelper.getDownloadURLHelper(this);
                // Download URI path can be sensitive in some environments; do not log it.
                FusabaseLogger.d("Storage", "Fetched download URL");
                taskCompletionSource.setResult(downloadUri);
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Method to download the object at this object reference to a specified file.
     * @param destinationFile an {code File} Destination Path where the file is to be downloaded
     * @return FileDownloadTask
     */
    @NonNull public FileDownloadTask getFile(@NonNull File destinationFile) {

        FileDownloadTask downloadTask = new FileDownloadTask(this);

        this.fileDownloadTasks.add(downloadTask);

        CompletableFuture<StorageTask<FileDownloadTask.TaskSnapshot>.SnapshotBase> future = CompletableFuture.supplyAsync(() -> {

            try {
                // Check if destinationFile exists
                if (!destinationFile.exists()) {
                    // Create a new file if it doesn't exists
                    boolean isSuccess = destinationFile.createNewFile();

                    if (!isSuccess) {
                        throw new StorageException("Unable to create destination file", StorageException.Code.CANNOT_CREATE_FILE);
                    }

                }

                // Stream the downloaded data to the destination file
                long totalBytes = 0;
                try (InputStream in = this.storageHelper.getObjectStream(this);
                     FileOutputStream fos = new FileOutputStream(destinationFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }
                    fos.flush();
                }

                this.fileDownloadTasks.removeIf(task -> this.path == task.getStorageReference().path);

                FileDownloadTask.TaskSnapshot taskSnapshot = downloadTask.new TaskSnapshot(this,
                        totalBytes,
                        totalBytes);
                downloadTask.setResult(taskSnapshot);

                return taskSnapshot;
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }

        });
        downloadTask.handleFuture(future);

        return downloadTask;
    }

    /**
     * Method to download the object at this object reference to a specified file.
     * Only supported scheme is file://
     * @param destinationUri an {code Uri} destination uri where the file is to be downloaded
     * @return FileDownloadTask
     */
    @NonNull public FileDownloadTask getFile(@NonNull Uri destinationUri){

        FileDownloadTask downloadTask = new FileDownloadTask(this);

        this.fileDownloadTasks.add(downloadTask);

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Check if destinationFile exists
                Utils.prepareUriForDownload(this.context, destinationUri, this.getName(),"application/octet-stream");

                // Stream the downloaded data to the destination uri
                long totalBytes = 0;
                try (InputStream in = this.storageHelper.getObjectStream(this);
                     OutputStream outputStream = context.getContentResolver().openOutputStream(destinationUri)) {
                    if (outputStream != null) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytes += bytesRead;
                        }
                        outputStream.flush();
                    }
                }

                this.fileDownloadTasks.removeIf(task ->  this.path == task.getStorageReference().path);

                FileDownloadTask.TaskSnapshot taskSnapshot = downloadTask.new TaskSnapshot(this,
                        totalBytes,
                        totalBytes);

                downloadTask.setResult(taskSnapshot);

                return taskSnapshot;
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }
        });

        downloadTask.handleFuture(future);

        return downloadTask;
    }

    /**
     * Method to get metadata of the object at this Storage Reference.
     * @return Task<StorageMetadata> An asynchronous task to fetch the metadata for the object at this reference.
     */
    @NonNull public Task<StorageMetadata> getMetadata() {

        TaskCompletionSource<StorageMetadata> taskCompletionSource = new TaskCompletionSource<>();
        Task<StorageMetadata> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject result = this.storageHelper.getMetadata(this);
                StorageMetadata storageMetadata = this.storageHelper.createStorageMetadata(this, result);
                taskCompletionSource.setResult(storageMetadata);
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Method to get name of the Object at this storage reference.
     * @return String Name of the Object
     */
    @NonNull public String getName() {
        return name;
    }

    public void setName(@NonNull String name) { this.name = name;}
    /**
     * Method to get the reference to the parent of the Storage Reference.
     * @return StorageReference or null if it is in the root directory.
     */
    @Nullable public StorageReference getParent() {
        return this.parent;
    }

    /**
     * Method to get the path being referenced by this StorageReference.
     * @return String
     */
    @NonNull public String getPath() {
        String path = String.join("/", this.path);

        // return as it is if its parent path
        if(path.isEmpty()) return path;

        return path.charAt(0) == '/' ? path.substring(1) : path;
    }

    /**
     * Method to get the storage reference to the root of the bucket.
     * @return StorageReference
     */
    @NonNull public StorageReference getRoot(){
        return new StorageReference(this.storage, DEFAULT_ROOT_PATH);
    }

    /**
     * Method to get the storage instance which is linked with this reference.
     * @return Storage
     */
    @NonNull public Storage getStorage(){
        return this.storage;
    }

    /**
     * Asynchronously downloads the object at this StorageReference via a InputStream.
     * @return StreamDownloadTask
     */
    @NonNull public StreamDownloadTask getStream(){

        StreamDownloadTask streamTask = new StreamDownloadTask(this);

        this.streamDownloadTasks.add(streamTask);

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {

            try {

                byte[] result = this.storageHelper.getObject(this, DEFAULT_MAX_DOWNLOAD_SIZE);

                java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(result);

                StreamDownloadTask.TaskSnapshot taskSnapshot = streamTask.new TaskSnapshot(this,

                        result.length,

                        result.length);

                taskSnapshot.setStream(inputStream);

                streamTask.setResult(taskSnapshot);

                this.streamDownloadTasks.removeIf(task -> String.join("/", this.path).equals(task.getStorageReference().getPath()));

                return taskSnapshot;

            } catch (Exception e) {

                Exception exception = e;

                if(!(exception instanceof StorageException))

                    exception = new StorageException(exception.getMessage() == null ?

                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);

                throw new CompletionException(exception);

            }

        });

        streamTask.handleFuture(future);

        return streamTask;

    }

    /**
     * Asynchronously downloads the object at this StorageReference via a InputStream.
     * @param processor an {@code StreamDownloadTask.StreamProcessor} processor to process the stream
     * @return StreamDownloadTask
     */
    @NonNull public StreamDownloadTask getStream(@NonNull StreamDownloadTask.StreamProcessor processor){

        StreamDownloadTask streamTask = new StreamDownloadTask(this);

        this.streamDownloadTasks.add(streamTask);

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {

            try {

                byte[] result = this.storageHelper.getObject(this, DEFAULT_MAX_DOWNLOAD_SIZE);

                java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(result);

                processor.process(inputStream);

                java.io.ByteArrayInputStream snapshotStream = new java.io.ByteArrayInputStream(result);

                StreamDownloadTask.TaskSnapshot taskSnapshot = streamTask.new TaskSnapshot(this,

                        result.length,

                        result.length);

                taskSnapshot.setStream(snapshotStream);

                streamTask.setResult(taskSnapshot);

                this.streamDownloadTasks.removeIf(task -> String.join("/", this.path).equals(task.getStorageReference().getPath()));

                return taskSnapshot;

            } catch (Exception e) {

                Exception exception = e;

                if(!(exception instanceof StorageException))

                    exception = new StorageException(exception.getMessage() == null ?

                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);

                throw new CompletionException(exception);

            }

        });

        streamTask.handleFuture(future);

        return streamTask;

    }

    /**
     * Method to get the list of objects under the referenced path. You
     * can also set limit on the maximum number of path of objects fetched under
     * this method.
     * @param maxResults an {@code int} Maximum number of result to be fetched
     * @return Task<ListResult> An asynchronous task which on completion provides
     * the list of objects.
     */
    @NonNull public Task<ListResult> list(int maxResults) {
        TaskCompletionSource<ListResult> taskCompletionSource = new TaskCompletionSource<>();
        Task<ListResult> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            // Send request and receive response
            try {
                JsonObject result = this.storageHelper.listObjs(this, maxResults, DEFAULT_NEXT_PAGE_TOKEN, false);
                ListResult listResult = new ListResult(storage, DataReader.getJsonObjectDataInMap(result));
                taskCompletionSource.setResult(listResult);
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Method to get the list of objects under the referenced path. This method
     * also allows you to recursively list all the objects under this path. You
     * can also set limit on the maximum number of path of objects fetched under
     * this method.
     * @param maxResults an {@code int} Maximum number of result to be fetched
     * @param nextPageToken an {@code int} Paging Token to continue with the list of element
     * @return Task<ListResult> An asynchronous task which on completion provides the list of objects.
     */
    @NonNull public Task<ListResult> list(int maxResults, String nextPageToken) {
        TaskCompletionSource<ListResult> taskCompletionSource = new TaskCompletionSource<>();
        Task<ListResult> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            // Send request and receive response
           try {
               JsonObject result = this.storageHelper.listObjs(this, maxResults, nextPageToken, false);
               ListResult listResult = new ListResult(storage, DataReader.getJsonObjectDataInMap(result));
               taskCompletionSource.setResult(listResult);
           } catch (Exception e){
               Exception exception = e;
               if(!(exception instanceof StorageException))
                   exception = new StorageException(exception.getMessage() == null ?
                       "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
               throw new CompletionException(exception);
           }
            return "";
        });
        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Method to get the list of objects under the referenced path. This method
     * allows you to recursively list all the objects under this path.
     * @return Task<ListResult> An asynchronous task which on completion provides
     * the list of objects.
     */
    @NonNull public Task<ListResult> listAll (){

        TaskCompletionSource<ListResult> taskCompletionSource = new TaskCompletionSource<>();

        Task<ListResult> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {

            try {
                JsonObject result = this.storageHelper.listObjs(this, MAX_RESULTS, DEFAULT_NEXT_PAGE_TOKEN, true);

                ListResult listResult = new ListResult(storage, DataReader.getJsonObjectDataInMap(result));

                taskCompletionSource.setResult(listResult);
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }
            return "";
        });

        taskCompletionSource.handleFuture(future);

        return task;
    }

    /**
     * Method to upload provided byte[] array into the referred storage.
     * @param bytes an {@code byte[]} byte array to be uploaded
     * @return UploadTask An asynchronous task representing the task of
     * uploading the byte array.
     */
    @NonNull public UploadTask putBytes(@NonNull byte[] bytes){

        return this.putBytes(bytes, new StorageMetadata.Builder().build());
    }

    /**
     * Method to upload provided byte[] array along with the provided metadata
     * into the referred storage.
     * @param bytes an {@code byte[]} byte array to be uploaded
     * @param metadata an {@code StorageMetadata} metadata for the object
     * @return UploadTask An asynchronous task representing the task of
     * uploading the object at the uri.
     */
    @NonNull public UploadTask putBytes(@NonNull byte[] bytes, @NonNull StorageMetadata metadata){

        UploadTask uploadTask = new UploadTask(this);

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Send request and receive response
                this.fileUploadTasks.add(uploadTask);
                JsonObject result;

                result = this.storageHelper.putObject(this,
                    metadata.getContentType() == null ?
                        "application/octet-stream" :
                        metadata.getContentType(),
                    bytes, metadata, uploadTask);

                result = result.containsKey("metadata") ? result.getJsonObject("metadata") : result;
                StorageMetadata storageMetadata = this.storageHelper.createStorageMetadata(this, result);

                UploadTask.TaskSnapshot taskSnapshot = uploadTask.new TaskSnapshot(this,
                        storageMetadata,
                        result.getInt("size"),
                        result.getInt("size"));

                this.setName(result.getString("name", ""));

                uploadTask.setResult(taskSnapshot);

                this.fileUploadTasks.removeIf(task -> String.join("/", this.path).equals(task.getStorageReference().getPath()));
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }
            return "";
        });

        uploadTask.handleFuture(future);

        return uploadTask;
    }

    /**
     * Method to upload provided byte[] array along with the provided metadata
     * into the referred storage.
     * @param uri an {@code Uri} Uri of the object
     * @return UploadTask An asynchronous task representing the task of
     * uploading the object at Uri.
     */

    @NonNull public UploadTask putFile(@NonNull Uri uri){

        return this.putFile(uri, new StorageMetadata.Builder().build());
    }


    /**
     * Method to upload provided byte[] array along with the provided metadata
     * into the referred storage.
     * @param uri an {@code Uri} Uri of the object
     * @param metadata an {@code StorageMetadata} metadata for the object
     * @return UploadTask An asynchronous task representing the task of
     * uploading the object at Uri.
     */
    @NonNull public UploadTask putFile(@NonNull Uri uri, @NonNull StorageMetadata metadata) {

        if(uri.getPath() == null)
        {
            throw new IllegalArgumentException("Invalid URI provided");
        }

        UploadTask uploadTask = new UploadTask(this);

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Set the task running
                uploadTask.setRunning();

                JsonObject result;

                String mimeType = metadata.getContentType() == null ?
                    this.context.getContentResolver().getType(uri) :
                    metadata.getContentType();
                if(mimeType == null) {
                    String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                }

                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new FileNotFoundException("Unable to open input stream for uri: " + uri);
                }

                long contentLength = getContentLength(uri);
                result = this.storageHelper.putStreamData(
                        this,
                        mimeType == null ? "application/octet-stream" : mimeType,
                        inputStream,
                        contentLength,
                        uploadTask);

                result = result.containsKey("metadata") ? result.getJsonObject("metadata") : result;
                StorageMetadata storageMetadata = this.storageHelper.createStorageMetadata(this, result);

                UploadTask.TaskSnapshot taskSnapshot = uploadTask.new TaskSnapshot(this,
                        storageMetadata,
                        result.getInt("size"),
                        result.getInt("size"));

                uploadTask.setResult(taskSnapshot);

                return "";
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }
        });

        uploadTask.handleFuture(future);
        return uploadTask;
    }

    private long getContentLength(@NonNull Uri uri) {
        try (AssetFileDescriptor descriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r")) {
            if (descriptor != null && descriptor.getLength() >= 0) {
                return descriptor.getLength();
            }
        } catch (IOException | SecurityException ignored) {
        }

        android.database.Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{OpenableColumns.SIZE},
                    null,
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    long size = cursor.getLong(sizeIndex);
                    if (size >= 0) {
                        return size;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return -1;
    }

    /**
     * Asynchronously uploads a stream of data to this StorageReference.
     * @param stream an {@code InputStream} stream of data to upload
     * @return UploadTask An asynchronous task representing the upload operation.
     */
    @NonNull public UploadTask putStream(@NonNull InputStream stream){
        return putStream(stream, new StorageMetadata.Builder().build());
    }

    /**
     * Asynchronously uploads a stream of data to this StorageReference.
     * @param stream an {@code InputStream} stream of data to upload
     * @param metadata an {@code StorageMetadata} metadata for the object
     * @return UploadTask An asynchronous task representing the upload operation.
     */
    @NonNull public UploadTask putStream(@NonNull InputStream stream, @NonNull StorageMetadata metadata){

        UploadTask uploadTask = new UploadTask(this);

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Set the task running
                uploadTask.setRunning();

                JsonObject result = this.storageHelper.putStreamData(
                        this,
                        metadata.getContentType() == null ?
                                "application/octet-stream" :
                                metadata.getContentType(),
                        stream,
                        -1,
                        uploadTask);

                result = result.containsKey("metadata") ? result.getJsonObject("metadata") : result;
                StorageMetadata storageMetadata = this.storageHelper.createStorageMetadata(this, result);

                UploadTask.TaskSnapshot taskSnapshot = uploadTask.new TaskSnapshot(this,
                        storageMetadata,
                        result.getInt("size"),
                        result.getInt("size"));

                uploadTask.setResult(taskSnapshot);

                return "";
            } catch (Exception e) {
                Exception exception = e;
                if(!(exception instanceof StorageException))
                    exception = new StorageException(exception.getMessage() == null ?
                        "Internal Error Occurred" : exception.getMessage(), StorageException.Code.INTERNAL);
                throw new CompletionException(exception);
            }
        });

        uploadTask.handleFuture(future);
        return uploadTask;
    }

}
