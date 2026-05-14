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
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.FusabaseNetworkException;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.core.FusabaseConstants;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.http.InputStreamRequestBody;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.OnCompleteListener;
import com.oracle.mobile.fusabase.task.Tasks;
import com.oracle.mobile.fusabase.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import okhttp3.MediaType;
import okhttp3.RequestBody;

class StorageHelper {

    private static final String TAG = "Storage";

    private int CHUNK_SIZE;

    private static final String METADATA_PATH = "/getmetadata/b";
    private static final String OPERATION_PATH = "/o/operation";
    private static final String LISTOBJECT_PATH = "/listallobj/b";
    private static final String GENPREAUTH_PATH = "/genpreauth";
    private static final String BAAS_SERVICE_PATH = "/_/baas-services";
    private static final String PAR_PATH = "/par";
    private static final String DBFS_OBJECT_TYPE = "dbfs";
    private static final String OCI_OBJECT_TYPE = "oci-objs";

    private final Storage storage;
    private final FusabaseOptions options;
    private final Context context;

    protected StorageHelper(@NonNull Storage instance) {
        this.storage = instance;
        this.options = instance.getApp().getOptions();
        this.CHUNK_SIZE = this.options.getUploadChunkSize();
        this.context = instance.getApp().getApplicationContext();
    }

    /**
     * Builds the Authorization header with access token.
     * Decrypts token inline to minimize memory exposure.
     *
     * @return Complete Authorization header string or empty string if unavailable
     * @throws StorageException if token retrieval fails
     */
    private String buildAuthorizationHeader() throws StorageException {
        try {
            String key = Utils.getPreferenceData(this.storage.getApp().getApplicationContext(),
                    "LOGGED_IN_USER",
                    "FusabasePreferences");
            if (key != null && !key.isEmpty()) {
                // Load the user details JSON object and extract the access token
                JsonObject userDetails = Utils.loadJsonObjectFromPreferences(
                        this.storage.getApp().getApplicationContext(),
                        key,
                        "FusabasePreferences");
                if (userDetails.containsKey("accessToken")) {
                    return "Bearer " + userDetails.getString("accessToken");
                }
            }
        } catch (Exception e) {
            FusabaseLogger.w("StorageHelper", "Cannot get access token from current user: " + e.getMessage());
        }

        return "";
    }

    protected JsonObject deleteObject(@NonNull StorageReference storageRef) throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to delete object from fusabase");

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject objectPath = Json.createObjectBuilder()
                .add("path", storageRef.getPath())
                .add("bucket", this.options.getStorageBucket())
                .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("x-object-path", objectPath.toString());

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        requestHelper.createHttpRequest(Utils.urlBuilder(this.options.getOrdsHost(),
                        BAAS_SERVICE_PATH,
                        this.options.getObjectsType().equals("dbfs") ? DBFS_OBJECT_TYPE : OCI_OBJECT_TYPE,
                        this.options.getProjectId(),
                        OPERATION_PATH),
                "DELETE",
                headers,
                queryParameters);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "deleteObjectSuccessful");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        FusabaseLogger.w(TAG, "deleteObject failed with response message " + response.getError());
        throw new StorageException(new Exception("deleteObject failed with response message " + response.getError()),
                StorageErrorCode.fromCode(response.getCode()));
    }

    protected JsonObject getMetadata(@NonNull StorageReference storageRef) throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to fetch metadata from fusabase");

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject objectPath = Json.createObjectBuilder()
                .add("path", storageRef.getPath())
                .add("bucket", this.options.getStorageBucket())
                .build();

        // Populate header
        Map<String, String> headers = new HashMap<>();
        headers.put("x-object-path", objectPath.toString());

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());


        // Create the request
        requestHelper.createHttpRequest(Utils.urlBuilder(this.options.getOrdsHost(),
                        BAAS_SERVICE_PATH,
                        this.options.getObjectsType().equals("dbfs") ? DBFS_OBJECT_TYPE : OCI_OBJECT_TYPE,
                        this.options.getProjectId(),
                        METADATA_PATH,
                        this.options.getStorageBucket()),
                "GET",
                headers,
                queryParameters);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Object metadata fetched Successful");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();

            reader.close();
            return result;
        }
        FusabaseLogger.e(TAG, "Object metadata failed with response message "
                + response.getError());
        throw new StorageException(new Exception("Object metadata failed with response message "
                + response.getError()),
                StorageErrorCode.fromCode(response.getCode()));
    }

    /**
     * Parses an RFC 3339/ RFC 2616 / ISO-8601 timestamp (with or without zone info)
     * into epoch milliseconds.
     */
    private long parseToEpochMillis(String timestamp) {

        // 1. Try ISO-8601 (Instant.parse)
        try {
            return Instant.parse(timestamp).toEpochMilli();
        } catch (Exception ignored) {}

        // 2. Try ISO_LOCAL_DATE_TIME (assume UTC)
        try {
            LocalDateTime ldt = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception ignored) {}

        // 3. Oracle TIMESTAMP without AM/PM, e.g. "28-NOV-25 18.02.00.535618"
        try {
            DateTimeFormatter oracle24 = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("dd-MMM-yy HH.mm.ss.SSSSSS")
                .toFormatter(Locale.ENGLISH);

            LocalDateTime ldt = LocalDateTime.parse(timestamp, oracle24);
            return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception ignored) {}

        // 4. Oracle TIMESTAMP with AM/PM, e.g. "28-NOV-25 06.17.17.041536 PM"
        try {
            DateTimeFormatter oracle12 = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("dd-MMM-yy hh.mm.ss.SSSSSS a")
                .toFormatter(Locale.ENGLISH);

            LocalDateTime ldt = LocalDateTime.parse(timestamp, oracle12);
            return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception ignored) {}

        // 5. Human format, e.g. "Fri Nov 21 18:29:03 GMT+05:30 2025"
        try {
            DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("EEE MMM dd HH:mm:ss zzz yyyy")
                .toFormatter(Locale.ENGLISH);

            ZonedDateTime zdt = ZonedDateTime.parse(timestamp, fmt);
            return zdt.toInstant().toEpochMilli();
        } catch (Exception ignored) {
        }

        // 6. RFC 1123 format, e.g. "Sat, 17 Aug 2024 12:49:07 GMT"
        try {
            DateTimeFormatter rfc1123 = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("EEE, dd MMM yyyy HH:mm:ss zzz")
                .toFormatter(Locale.ENGLISH);

            ZonedDateTime zdt = ZonedDateTime.parse(timestamp, rfc1123);
            return zdt.toInstant().toEpochMilli();
        } catch (Exception error) {
            FusabaseLogger.e("Exception encountered while parsing timestamp string." + error.getMessage());
        }

        return 0;
    }


    protected StorageMetadata createStorageMetadata (@NonNull StorageReference storageRef,
                                                    @NonNull JsonObject result) {
        String timeCreated = result.containsKey("timeCreated") ?
            result.getString("timeCreated", "") :
            result.getString("time-created", "");
        String updated = result.containsKey("updated") ?
            result.getString("updated", "") :
            result.getString("last-modified", "");
        long creationTimestampMillis = !timeCreated.isEmpty() ? parseToEpochMillis(timeCreated) : 0;
        long updatedTimestampMillis = !updated.isEmpty() ? parseToEpochMillis(updated) : 0;

        // Bring the default metadata here in case of multipart upload and fix it here
        return new StorageMetadata.Builder()
            .setContentType(result.getString("contentType", "application/octet-stream"))
            .setStorageReference(storageRef)
            .setPath(storageRef.getPath())
            .setBucket(result.getString("bucket", storageRef.getBucket()))
            .setCreationTimeMillis(creationTimestampMillis)
            .setUpdatedTimeMillis(updatedTimestampMillis)
            .setName(result.getString("name", ""))
            .setSize(result.getInt("size", 0))
            .setMd5Hash(result.containsKey("md5Hash") ?
                result.getString("md5Hash", "") :
                result.containsKey("md5sum") ?
                    result.getString("md5sum", "") :
                    result.getString("opc-multipart-md5", "")
            )
            .build();

    }
    protected JsonObject listObjs(@NonNull StorageReference storageRef,
                                  int maxResults,
                                  String pageToken,
                                  boolean recurse) throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to list objs from fusabase");

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject objectPath = Json.createObjectBuilder()
                .add("path", storageRef.getPath())
                .add("recurse", recurse)
                .add("pageToken", pageToken)
                .add("maxResults", maxResults)
                .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("x-dbfs-list-opts", objectPath.toString());

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        requestHelper.createHttpRequest(Utils.urlBuilder(this.options.getOrdsHost(),
                        BAAS_SERVICE_PATH,
                        this.options.getObjectsType().equals("dbfs") ? DBFS_OBJECT_TYPE : OCI_OBJECT_TYPE,
                        this.options.getProjectId(),
                        LISTOBJECT_PATH,
                        this.options.getStorageBucket()),
                "GET",
                headers,
                queryParameters);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "ListObjectSuccessful");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();

            reader.close();
            return result;
        }
        FusabaseLogger.e(TAG, "ListObject failed with response message " + response.getError());
        throw new StorageException(new Exception("ListObject failed with response message " + response.getError()),
                StorageErrorCode.fromCode(response.getCode()));
    }

    protected JsonObject getPARUrl(@NonNull StorageReference storageRef,
                                   @Nullable StorageMetadata metadata,
                                   @NonNull String accessType) throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to fetch PAR URL from fusabase");
        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject payload = Json.createObjectBuilder()
                .add("path", storageRef.getPath())
                .add("bucket", storageRef.getBucket())
                .add("access_type", accessType)
                .add("metadata", metadata == null ?
                        (new StorageMetadata
                                .Builder().build()).getJsonObject()
                        :
                        metadata.getJsonObject())
                .build();

        // Populate header
        Map<String, String> headers = new HashMap<>();

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        // Create the request
        requestHelper.createHttpRequest(Utils.urlBuilder(Utils.urlBuilder(this.options.getOrdsHost(),
                        BAAS_SERVICE_PATH,
                        this.options.getObjectsType().equals("dbfs") ? DBFS_OBJECT_TYPE : OCI_OBJECT_TYPE,
                        this.options.getProjectId(),
                        GENPREAUTH_PATH)),
                "POST",
                headers,
                queryParameters,
                payload.toString());

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Successfully fetched PAR url from thread "
                    + Thread.currentThread().getName());
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        }

        FusabaseLogger.e(TAG, "Failed to get par url with response message "
                + response.getError());
        throw new StorageException(new Exception("Failed to get par url with response message "
                + response.getError()),
                StorageErrorCode.fromCode(response.getCode()));
    }

    protected Uri getDownloadURLHelper(@NonNull StorageReference storageReference)
            throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to get download URL from fusabase");

        JsonObject result = this.getPARUrl(storageReference,
                new StorageMetadata.Builder().build(),
                "ObjectRead");

        HashMap<String, Object> responseData = DataReader.getJsonObjectDataInMap(result);

        if (Objects.equals(this.options.getObjectsType(), OCI_OBJECT_TYPE)) {
            return Uri.parse((String) responseData.get("URL"));
        }

        return Uri.parse(Utils.urlBuilder(this.storage.getApp().getOptions().getOrdsHost(),
                BAAS_SERVICE_PATH,
                this.options.getObjectsType().equals("dbfs") ? DBFS_OBJECT_TYPE : OCI_OBJECT_TYPE,
                PAR_PATH,
                this.options.getProjectId(),
                (String) responseData.get("URL")));
    }

    /**
     * Helper method to create and send request to get an object at a specified path.
     *
     * @param storageRef            an {@code StorageReference}
     * @param maxResultDownloadSize an {@code long}
     * @return byte[]
     */
    public byte[] getObject(@NonNull StorageReference storageRef,
                            long maxResultDownloadSize) throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to fetch object from fusabase");
        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject objectPath = Json.createObjectBuilder()
                .add("path", storageRef.getPath().charAt(0) == '/' ? storageRef.getPath().substring(1) : storageRef.getPath())
                .add("bucket", storageRef.getBucket())
                .build();

        // Populate header
        Map<String, String> headers = new HashMap<>();
        headers.put("x-object-path", objectPath.toString());

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        // Create the request
        requestHelper.createHttpRequest(Utils.urlBuilder(Utils.urlBuilder(this.options.getOrdsHost(),
                        BAAS_SERVICE_PATH,
                        this.options.getObjectsType().equals("dbfs") ? DBFS_OBJECT_TYPE : OCI_OBJECT_TYPE,
                        this.options.getProjectId(),
                        OPERATION_PATH)),
                "GET",
                headers,
                queryParameters);

        HttpResponse response = null;

        try {
            // Stream the response body and enforce maxResultDownloadSize while reading.
            response = requestHelper.executeRequestStreaming();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Fetched Object Successfully.");

            try (InputStream in = response.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (in == null) {
                    throw new StorageException(new Exception("Empty response body"), StorageErrorCode.UNKNOWN_ERROR);
                }

                byte[] buffer = new byte[8192];
                long totalRead = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    totalRead += read;
                    if (totalRead > maxResultDownloadSize) {
                        throw new StorageException(
                                new Exception("Download exceeds maxResultDownloadSize of " + maxResultDownloadSize + " bytes"),
                                StorageErrorCode.INVALID_ARGUMENT);
                    }
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            } catch (IOException ioe) {
                throw new StorageException(ioe, StorageErrorCode.INVALID_ARGUMENT);
            }
        }
        FusabaseLogger.e(TAG, "Failed to fetch object with response message " + response.getError());
        throw new StorageException(
                new Exception("Failed to fetch object with response message " + response.getError()),
                StorageErrorCode.fromCode(response.getCode()));
    }

    /**
     * Helper method to get an InputStream for downloading an object at a specified path.
     *
     * @param storageRef an {@code StorageReference}
     * @return InputStream
     */
    public InputStream getObjectStream(@NonNull StorageReference storageRef) throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending request to fetch object stream from fusabase");
        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject objectPath = Json.createObjectBuilder()
                .add("path", storageRef.getPath().charAt(0) == '/' ? storageRef.getPath().substring(1) : storageRef.getPath())
                .add("bucket", storageRef.getBucket())
                .build();

        // Populate header
        Map<String, String> headers = new HashMap<>();
        headers.put("x-object-path", objectPath.toString());

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        // Create the request
        requestHelper.createHttpRequest(Utils.urlBuilder(this.options.getOrdsHost(),
                        BAAS_SERVICE_PATH,
                        this.options.getObjectsType().equals("dbfs") ? DBFS_OBJECT_TYPE : OCI_OBJECT_TYPE,
                        this.options.getProjectId(),
                        OPERATION_PATH),
                "GET",
                headers,
                queryParameters);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequestStreaming();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Fetched Object Stream Successfully.");
            return response.getInputStream();
        }
        FusabaseLogger.e(TAG, "Failed to fetch object stream with response message " + response.getError());
        throw new StorageException(
                new Exception("Failed to fetch object stream with response message " + response.getError()),
                StorageErrorCode.fromCode(response.getCode()));
    }

    private void checkIfTaskIsPaused(@NonNull UploadTask uploadTask) {
        synchronized (uploadTask.getLock()) {
            while (!uploadTask.getKeepRunning().get() && uploadTask.isPaused()) {
                try {
                    uploadTask.getLock().wait(); // **Thread stops consuming CPU here**
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private boolean checkIfTaskIsCancelled(@NonNull UploadTask task) {
        return task.isCanceled();
    }

    private int getUploadBufferSize() {
        return this.CHUNK_SIZE > 0 ? this.CHUNK_SIZE : FusabaseConstants.DEFAULT_UPLOAD_CHUNK_SIZE;
    }

    private void checkIfStreamingUploadShouldContinue(@NonNull UploadTask uploadTask) throws IOException {
        if (checkIfTaskIsCancelled(uploadTask)) {
            throw new IOException("Upload task canceled");
        }

        synchronized (uploadTask.getLock()) {
            while (!uploadTask.getKeepRunning().get() && uploadTask.isPaused()) {
                try {
                    uploadTask.getLock().wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException("Upload task interrupted while paused");
                }

                if (checkIfTaskIsCancelled(uploadTask)) {
                    throw new IOException("Upload task canceled");
                }
            }
        }
    }

    private void updateStreamingUploadProgress(@NonNull StorageReference storageReference,
                                               @NonNull UploadTask uploadTask,
                                               long bytesTransferred,
                                               long totalBytes) {
        StorageMetadata metadata = uploadTask.getResult() != null ?
                uploadTask.getResult().getMetadata() :
                new StorageMetadata.Builder().build();
        uploadTask.setResult(uploadTask.new TaskSnapshot(
                storageReference,
                metadata,
                bytesTransferred,
                totalBytes));

        if (uploadTask.getProgressListener() != null) {
            uploadTask.getListenerExecutor().post(uploadTask::onProgress);
        }
    }

    /**
     * Helper method to distinguish whether the required upload needs to be uploaded
     * in multiple chunk or as a whole.
     *
     * @param storageRef an {@code StorageReference}
     * @param bytes      an {@code bytes} Data
     * @param metadata   an {@code} StorageMetadata
     * @return JsonObject
     */
    public JsonObject putObject(@NonNull StorageReference storageRef,
                                @NonNull String mimeType,
                                @NonNull byte[] bytes,
                                @Nullable StorageMetadata metadata,
                                @NonNull UploadTask uploadTask) throws FusabaseException {

        // Check for pause/cancel at the very beginning of the upload operation
        checkIfTaskIsPaused(uploadTask);
        if (checkIfTaskIsCancelled(uploadTask)) {
            uploadTask.setCanceled();
            return Json.createObjectBuilder().build();
        }

        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (bytes.length > 2 * this.CHUNK_SIZE) {
            FusabaseLogger.d(TAG, "Performing multipart request to put object at fusabase");
            // Create a PAR to upload the bytes
            JsonObject parResult = this.getPARUrl(storageRef, metadata, "ObjectWrite");

            checkIfTaskIsPaused(uploadTask);

            // Indexed upload of data
            MultipartRequest multipartRequest = new MultipartRequest(
                    Utils.urlBuilder(
                            this.options.getOrdsHost(),
                            BAAS_SERVICE_PATH,
                            this.options.getObjectsType().equals("dbfs") ? DBFS_OBJECT_TYPE : OCI_OBJECT_TYPE,
                            PAR_PATH,
                            this.options.getProjectId(),
                            parResult.getString("accessUri")),
                    mimeType,
                    bytes,
                    this.CHUNK_SIZE);

            boolean success = true;

            for (int i = 0; i < multipartRequest.getTotalNumberOfChunks(); i++) {
                if (checkIfTaskIsCancelled(uploadTask)) {
                    FusabaseLogger.d(TAG, "Put bytes task cancellation received.");
                    success = this.sendAbortRequest(multipartRequest, storageRef);
                    FusabaseLogger.d(TAG, "Put bytes task canceled.");
                    throw new StorageException(new Exception("Put file task canceled."),
                            StorageErrorCode.CANCELLED);
                }

                checkIfTaskIsPaused(uploadTask);

                success = success & this.putChunk(multipartRequest, storageRef);
                uploadTask.setResult(uploadTask.new TaskSnapshot(storageRef,
                        metadata,
                        multipartRequest.getBytesTransferred(),
                        multipartRequest.getTotalBytes()));

                if (uploadTask.getProgressListener() != null) {
                    uploadTask.getListenerExecutor().post(uploadTask::onProgress);
                }
            }

            if (!success)
            {
                FusabaseLogger.e(TAG, "Cannot upload the Objects successfully.");
                throw new FusabaseException("Cannot upload the Objects successfully.");
            }

            success = this.sendCommitRequest(multipartRequest, storageRef);

            if (!success)
            {
                FusabaseLogger.e(TAG, "Cannot commit the Objects successfully.");
                throw new FusabaseException("Cannot commit the Objects successfully.");
            }
            return this.getMetadata(storageRef);
        }

        FusabaseLogger.d(TAG, "Performing single request to put object at fusabase");
        return this.putData(storageRef, mimeType, bytes, uploadTask);
    }

    private JsonObject putData(@NonNull StorageReference storageReference,
                               @NonNull String mimeType,
                               @NonNull byte[] bytes,
                               @NonNull UploadTask uploadTask) throws FusabaseException {

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject objectPath = Json.createObjectBuilder()
                .add("path", storageReference.getPath())
                .add("bucket", storageReference.getBucket())
                .build();

        // Populate header
        Map<String, String> headers = new HashMap<>();
        headers.put("x-object-path", objectPath.toString());

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        headers.put("Content-Type", mimeType);

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        RequestBody body = RequestBody.create(bytes, MediaType.parse(mimeType));

        // Create the request
        requestHelper.createHttpRequest(Utils.urlBuilder(
                        this.options.getOrdsHost(),
                        BAAS_SERVICE_PATH,
                        this.options.getObjectsType().equals("dbfs") ? DBFS_OBJECT_TYPE : OCI_OBJECT_TYPE,
                        this.options.getProjectId(),
                        OPERATION_PATH),
                "POST",
                headers,
                queryParameters,
                body);

        checkIfTaskIsPaused(uploadTask);

        if (checkIfTaskIsCancelled(uploadTask)) {
            uploadTask.setCanceled();
            return Json.createObjectBuilder().build();
        }

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Put bytes successfully uploaded data.");

            JsonObject result = this.getMetadata(storageReference);
            StorageMetadata metadata = this.createStorageMetadata(storageReference, result);

            uploadTask.setResult(uploadTask.new TaskSnapshot(
                    storageReference,
                    metadata,
                    bytes.length,
                    bytes.length));
            if (uploadTask.getProgressListener() != null) {
                uploadTask.getListenerExecutor().post(uploadTask::onProgress);
            }

            return result;
        }

        FusabaseLogger.e(TAG, "Put bytes failed with response message " + response.getError());
        throw new StorageException(new Exception("Put data failed with response message " + response.getError()), StorageErrorCode.fromCode(response.getCode()));
    }

    protected JsonObject putStreamData(@NonNull StorageReference storageReference,
                                       @NonNull String mimeType,
                                       @NonNull InputStream stream,
                                       long contentLength,
                                       @NonNull UploadTask uploadTask) throws FusabaseException {

        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject objectPath = Json.createObjectBuilder()
                .add("path", storageReference.getPath())
                .add("bucket", storageReference.getBucket())
                .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("x-object-path", objectPath.toString());

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        headers.put("Content-Type", mimeType);

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        MediaType mediaType = MediaType.parse(mimeType);
        if (mediaType == null) {
            mediaType = MediaType.parse("application/octet-stream");
        }

        RequestBody body = new InputStreamRequestBody(
                mediaType,
                stream,
                contentLength,
                getUploadBufferSize(),
                (bytesWritten, totalBytes) -> updateStreamingUploadProgress(
                        storageReference,
                        uploadTask,
                        bytesWritten,
                        totalBytes),
                () -> checkIfStreamingUploadShouldContinue(uploadTask));

        requestHelper.createHttpRequest(Utils.urlBuilder(
                        this.options.getOrdsHost(),
                        BAAS_SERVICE_PATH,
                        this.options.getObjectsType().equals("dbfs") ? DBFS_OBJECT_TYPE : OCI_OBJECT_TYPE,
                        this.options.getProjectId(),
                        OPERATION_PATH),
                "POST",
                headers,
                queryParameters,
                body);

        checkIfTaskIsPaused(uploadTask);

        if (checkIfTaskIsCancelled(uploadTask)) {
            uploadTask.setCanceled();
            return Json.createObjectBuilder().build();
        }

        HttpResponse response = null;
        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Put stream successfully uploaded data.");

            // Do not buffer the upload response body. Fetch compact metadata separately.
            JsonObject result = this.getMetadata(storageReference);
            StorageMetadata metadata = this.createStorageMetadata(storageReference, result);

            // If contentLength unknown, use size from metadata.
            long totalBytes = contentLength >= 0 ? contentLength : metadata.getSizeBytes();
            uploadTask.setResult(uploadTask.new TaskSnapshot(
                    storageReference,
                    metadata,
                    totalBytes,
                    totalBytes));

            if (uploadTask.getProgressListener() != null) {
                uploadTask.getListenerExecutor().post(uploadTask::onProgress);
            }
            return result;
        }

        if (checkIfTaskIsCancelled(uploadTask)) {
            uploadTask.setCanceled();
            throw new StorageException(new Exception("Put stream task canceled."),
                    StorageErrorCode.CANCELLED);
        }

        FusabaseLogger.e(TAG, "Put stream failed with response message " + response.getError());
        throw new StorageException(new Exception("Put stream failed with response message " + response.getError()),
                StorageErrorCode.fromCode(response.getCode()));
    }
//    protected boolean putStreamData (@NonNull InputStream stream, @NonNull StorageMetadata metadata) {
//
//        MultipartRequest multipartRequest = new MultipartRequest();
//
//
//    }

    private boolean putChunk(@NonNull MultipartRequest multipartRequest, @NonNull StorageReference storageReference) throws FusabaseException {
        FusabaseLogger.d(TAG, "Performing multipart request to put a chunk at fusabase");
        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject objectPath = Json.createObjectBuilder()
                .add("path", storageReference.getPath())
                .add("bucket", storageReference.getBucket())
                .build();

        // Populate header
        Map<String, String> headers = new HashMap<>();
        headers.put("x-object-path", objectPath.toString());
        
        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        RequestBody body = RequestBody.create(multipartRequest.getNextChunk(), MediaType.parse(multipartRequest.getMimeType()));

        // Create the request
        requestHelper.createHttpRequest(
                Utils.urlBuilder(multipartRequest.getAccessURL(),
                        String.valueOf(multipartRequest.getCurrIndex())),
                "PUT",
                headers,
                queryParameters,
                body);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Put byte part" + multipartRequest.getCurrIndex() + "successful");
            multipartRequest.updateRequestForNextChunk();
            return response.getStatus();
        }

        FusabaseLogger.e(TAG, "Put bytes part" + multipartRequest.getCurrIndex()
                + "failed with response message " + response.getError());
        throw new StorageException(
                new Exception("Put bytes part" + multipartRequest.getCurrIndex()
                        + "failed with response message " + response.getError()),
                StorageErrorCode.fromCode(response.getCode()));


    }

    private boolean sendCommitRequest(@NonNull MultipartRequest multipartRequest,
                                      @NonNull StorageReference storageReference)
            throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending commit request for object at fusabase");
        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject objectPath = Json.createObjectBuilder()
                .add("path", storageReference.getPath())
                .add("bucket", storageReference.getBucket())
                .build();

        // Populate header
        Map<String, String> headers = new HashMap<>();
        headers.put("x-object-path", objectPath.toString());

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }


        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        // Create the request
        requestHelper.createHttpRequest(Utils.urlBuilder(multipartRequest.getAccessURL()) + '/',
                "POST",
                headers,
                queryParameters);

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Put bytes parts commited successfully");
            return response.getStatus();
        }

        FusabaseLogger.e(TAG, "Commit failed with response message " + response.getError());
        throw new StorageException(new Exception("Commit failed with response message " + response.getError()),
                StorageErrorCode.fromCode(response.getCode()));
    }

    private boolean sendAbortRequest(@NonNull MultipartRequest multipartRequest,
                                     @NonNull StorageReference storageReference) throws FusabaseException {

        FusabaseLogger.d(TAG, "Sending abort request for object at fusabase");
        HttpRequestHelper requestHelper = new HttpRequestHelper();

        JsonObject objectPath = Json.createObjectBuilder()
                .add("path", storageReference.getPath())
                .add("bucket", storageReference.getBucket())
                .build();

        // Populate header
        Map<String, String> headers = new HashMap<>();
        headers.put("x-object-path", objectPath.toString());

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }


        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        // Create the request
        // Here there is a special case that the url needs a back slash after entire url.
        requestHelper.createHttpRequest(Utils.urlBuilder(multipartRequest.getAccessURL()) + '/',
                "DELETE",
                headers,
                queryParameters,
                "");

        HttpResponse response = null;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing the operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }


        if (response.getStatus()) {
            FusabaseLogger.d(TAG, "Put bytes aborted successfully");
            return response.getStatus();
        }

        FusabaseLogger.e(TAG, "Abort request failed with response message " + response.getError());
        throw new StorageException(new Exception("Abort request failed with response message " + response.getError()),
                StorageErrorCode.INTERNAL);
    }
}
