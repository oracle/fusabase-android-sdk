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

package com.oracle.mobile.fusabase.oracledb;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.utils.Utils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * Helper class for performing HTTP requests and data operations for Oracle
 * Database queries and documents.
 *
 * <p>
 * This class handles all HTTP communication with the Oracle REST Data Services
 * (ORDS) backend,
 * including query execution, document operations (CRUD), and transaction
 * management. It supports
 * both document model and duality view operations, with automatic handling of
 * authentication,
 * error responses, and data transformation.
 * </p>
 *
 * <p>
 * The class provides methods for:
 * </p>
 * <ul>
 * <li>Executing database queries with result processing</li>
 * <li>Performing document CRUD operations (Create, Read, Update, Delete)</li>
 * <li>Managing transactions with proper header handling</li>
 * <li>Handling FieldValue operations for document updates</li>
 * <li>Supporting both API v1.0 and v2.0 formats</li>
 * <li>Managing authentication tokens and request headers</li>
 * </ul>
 */
class QueryHelper {

    /**
     * Base API service path.
     */
    protected static final String BAAS_SERVICE = "/_/baas-services";

    /**
     * Database service path.
     */
    protected static final String DATABASE = "/database";

    /**
     * IDM service path.
     */
    protected static final String IDM = "/idm";

    /**
     * ONPREM service path.
     */
    protected static final String ONPREM = "/onprem";

    /**
     * IDCS service path.
     */
    protected static final String IDCS = "/idcs";

    /**
     * Authorize snapshot path.
     */
    protected static final String AUTHORIZE_SNAPSHOT = "/authorizeSnapshot";

    /**
     * API version 1 path.
     */
    protected static final String V1 = "/v1";

    /**
     * API version 2 path.
     */
    protected static final String V2 = "/v2";

    /**
     * Run query endpoint.
     */
    protected static final String RUN_QUERY = "/runQuery";

    /**
     * Delete document endpoint.
     */
    protected static final String DELETE_DOCUMENT = "/deleteDocument";

    /**
     * Set document endpoint.
     */
    protected static final String SET_DOCUMENT = "/setDocument";

    /**
     * Update document endpoint.
     */
    protected static final String UPDATE_DOCUMENT = "/updateDocument";

    /**
     * API version string.
     */
    private final String API_VERSION;

    /**
     * Settings instance.
     */
    private FusabaseOracledbSettings settings;

    /**
     * Options instance.
     */
    private FusabaseOptions options;

    /**
     * Oracledb instance.
     */
    private FusabaseOracledb oradb;

    /**
     * Application context.
     */
    private Context context;

    /**
     * Flag indicating if OID is created.
     */
    private boolean oidCreated;

    /**
     * Constructs a QueryHelper instance.
     *
     * @param settings the Oracledb settings
     * @param options  the Oracledb options
     * @param oradb    the Oracledb instance
     */
    QueryHelper(@NonNull FusabaseOracledbSettings settings,
            @NonNull FusabaseOptions options,
            @NonNull FusabaseOracledb oradb) {
        this.settings = settings;
        this.options = options;
        this.oradb = oradb;
        this.context = oradb.getApp().getApplicationContext();
        this.oidCreated = false;
        this.API_VERSION = options.getApiVersion();
    }

    /**
     * Creates a path JSON object for standard paths.
     *
     * @param path the path segments
     * @return the JSON object with path
     */
    @NonNull
    private JsonObject getPathObject(@NonNull List<String> path) {
        return Json.createObjectBuilder()
                .add("path", Json.createArrayBuilder(path))
                .build();
    }

    /**
     * Creates a path JSON object for duality views.
     *
     * @param path the path segments
     * @return the JSON object with duality view path
     */
    @NonNull
    JsonObject getDualityViewPathObject(@NonNull List<String> path) {
        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
        payloadBuilder.add("dv_name", path.get(0));
        if (path.size() > 1) {
            payloadBuilder.add("oid", path.get(1));
        }
        return payloadBuilder.build();
    }

    /**
     * Builds the Authorization header with access token.
     * Decrypts token inline to minimize memory exposure.
     *
     * @return Complete Authorization header string or empty string if unavailable
     */
    @NonNull
    private String buildAuthorizationHeader() {

        try {
            String key = Utils.getPreferenceData(this.oradb.getApp().getApplicationContext(),
                    "LOGGED_IN_USER",
                    "FusabasePreferences");
            if (key != null && !key.isEmpty()) {
                // Load the user details JSON object and extract the access token
                JsonObject userDetails = Utils.loadJsonObjectFromPreferences(
                        this.oradb.getApp().getApplicationContext(),
                        key,
                        "FusabasePreferences");
                if (userDetails.containsKey("accessToken")) {
                    return "Bearer " + userDetails.getString("accessToken");
                }
            }
        } catch (Exception e) {
            FusabaseLogger.w("QueryHelper", "Cannot get access token from current user: " + e.getMessage());
        }

        FusabaseLogger.w("QueryHelper", "No access token available");
        return "";
    }

    /**
     * Updates a document map with the provided updates, handling FieldValue
     * operations.
     *
     * @param doc     the document map to update
     * @param updates the updates to apply
     * @throws FusabaseException if an error occurs during update
     */
    @NonNull
    private void updateDoc(@NonNull List<String> servertimestamp,
            @NonNull Map<String, Object> doc,
            @NonNull Map<String, Object> updates) throws FusabaseException {
        if (doc == null) {
            throw new FusabaseException("Document map cannot be null");
        }
        if (updates == null) {
            throw new FusabaseException("Updates map cannot be null");
        }

        updates.forEach((key, value) -> {
            if (key == null) {
                FusabaseLogger.w("QueryHelper", "Skipping update with null key");
                return;
            }

            // Handle FieldValue operations for V1 API
            FieldPath fieldPath = new FieldPath(key);
            Map<String, Object> fieldMap = doc;
            List<String> pathSegments = fieldPath.getSegments();

            if (pathSegments == null || pathSegments.isEmpty()) {
                FusabaseLogger.w("QueryHelper", "Skipping update with empty field path");
                return;
            }

            for (int i = 0; i < pathSegments.size() - 1; i++) {
                String segment = pathSegments.get(i);
                if (segment == null) {
                    FusabaseLogger.w("QueryHelper", "Skipping update with null path segment");
                    return;
                }

                if (!fieldMap.containsKey(segment) ||
                        !(fieldMap.get(segment) instanceof Map)) {
                    fieldMap.put(segment, new HashMap<>());
                }
                Object nextMap = fieldMap.get(segment);
                if (nextMap instanceof Map) {
                    fieldMap = (Map<String, Object>) nextMap;
                } else {
                    FusabaseLogger.w("QueryHelper", "Cannot navigate path segment: " + segment);
                    return;
                }
            }

            String finalKey = pathSegments.get(pathSegments.size() - 1);
            if (finalKey == null) {
                FusabaseLogger.w("QueryHelper", "Skipping update with null final key");
                return;
            }

            if (value instanceof FieldValue) {
                applyFieldValueOperation(fieldMap, finalKey, (FieldValue) value, servertimestamp);
            } else if (value instanceof Blob) {
                fieldMap.put(finalKey, ((Blob) value).toBytes());
            } else {
                fieldMap.put(finalKey, value);
            }
        });
    }

    /**
     * Applies a FieldValue operation to the field map.
     *
     * @param fieldMap   the field map
     * @param fieldName  the field name
     * @param fieldValue the FieldValue operation
     */
    private void applyFieldValueOperation(@NonNull Map<String, Object> fieldMap,
            @NonNull String fieldName,
            @NonNull FieldValue fieldValue,
            @NonNull List<String> servertimestamp) {
        switch (fieldValue.getOperation()) {
            case DELETE:
                fieldMap.remove(fieldName);
                break;
            case DELETE_VECTOR:
                fieldMap.remove(fieldName);
                break;
            case INCREMENT:
                handleIncrementOperation(fieldMap, fieldName, fieldValue);
                break;
            case ARRAY_UNION:
                handleArrayUnionOperation(fieldMap, fieldName, fieldValue);
                break;
            case ARRAY_REMOVE:
                handleArrayRemoveOperation(fieldMap, fieldName, fieldValue);
                break;
            case SERVER_TIMESTAMP:
                handleServerTimestampOperation(fieldMap, fieldName);
                servertimestamp.add(fieldName);
                break;
            default:
                fieldMap.put(fieldName, fieldValue.getValues());
                break;
        }
    }

    /**
     * Handles the increment operation.
     *
     * @param fieldMap   the field map
     * @param fieldName  the field name
     * @param fieldValue the FieldValue
     */
    private void handleIncrementOperation(@NonNull Map<String, Object> fieldMap,
            @NonNull String fieldName,
            @NonNull FieldValue fieldValue) {
        Object originalValue = fieldMap.get(fieldName);

        if (originalValue == null) {
            fieldMap.put(fieldName, (long) fieldValue.getValues()[0]);
        } else if (originalValue instanceof Number) {
            fieldMap.put(fieldName, ((Number) originalValue).doubleValue() + (long) fieldValue.getValues()[0]);
        } else {
            FusabaseLogger.e("QueryHelper", "Expected a number but existing value is non-numerical. Cannot increment.");
        }
    }

    private void handleServerTimestampOperation(@NonNull Map<String, Object> fieldMap,
            @NonNull String fieldName) {
        fieldMap.put(fieldName, "#servertimestamp");
    }

    /**
     * Handles the array union operation.
     *
     * @param fieldMap   the field map
     * @param fieldName  the field name
     * @param fieldValue the FieldValue
     */
    private void handleArrayUnionOperation(@NonNull Map<String, Object> fieldMap,
            @NonNull String fieldName,
            @NonNull FieldValue fieldValue) {
        Object originalValue = fieldMap.get(fieldName);
        if (originalValue instanceof List<?>) {
            List<?> originalValueRawList = (List<?>) originalValue;
            List<Object> updatedList = new ArrayList<>(originalValueRawList);
            Set<Object> originalValueHashSet = new LinkedHashSet<>(originalValueRawList);
            for (Object x : fieldValue.getValues()) {
                if (!originalValueHashSet.contains(x)) {
                    updatedList.add(x);
                }
            }
            fieldMap.put(fieldName, updatedList);
        } else if (originalValue instanceof Arrays) {
            // Handle array case if needed
            fieldMap.put(fieldName, originalValue); // Placeholder
        } else {
            FusabaseLogger.e("QueryHelper",
                    "Expected an Array or List but existing value is not an Array or List. Cannot perform array union.");
        }
    }

    /**
     * Handles the array remove operation.
     *
     * @param fieldMap   the field map
     * @param fieldName  the field name
     * @param fieldValue the FieldValue
     */
    private void handleArrayRemoveOperation(@NonNull Map<String, Object> fieldMap,
            @NonNull String fieldName,
            @NonNull FieldValue fieldValue) {
        Object originalValue = fieldMap.get(fieldName);
        if (originalValue instanceof List) {
            List<?> originalValueRawList = (List<?>) originalValue;
            List<Object> updatedList = new ArrayList<>(originalValueRawList);
            Set<Object> originalValueHashSet = new LinkedHashSet<>(originalValueRawList);
            for (Object x : fieldValue.getValues()) {
                if (originalValueHashSet.contains(x)) {
                    updatedList.remove(x);
                }
            }
            fieldMap.put(fieldName, updatedList);
        } else if (originalValue instanceof Arrays) {
            // Handle array case
            fieldMap.put(fieldName, originalValue);
        } else {
            FusabaseLogger.e("QueryHelper",
                    "Expected an Array or List but existing value is not an Array or List. Cannot perform array remove.");
        }
    }

    /**
     * Processes update data into the required format for API requests.
     *
     * @param updates the updates map
     * @return the processed map containing update requests and servertimestamp array
     */
    @NonNull
    private Map<String, Object> processUpdateData(@NonNull Map<String, Object> updates) {
        List<Map<String, Object>> updatedFormat = new ArrayList<>();
        List<String> servertimestamp = new ArrayList<>();

        Map<String, Object> embeddingsSet = extractEmbeddingsSetMap(updates);
        List<String> embeddingsDelete = extractEmbeddingsDeleteKeys(updates);
        if (!embeddingsSet.isEmpty()) {
            HashMap<String, Object> request = new HashMap<>();
            request.put("field", "$embeddings");
            request.put("op", "set");
            request.put("value", embeddingsSet);
            request.put("valueType", "mapValue");
            updatedFormat.add(request);
        }
        if (!embeddingsDelete.isEmpty()) {
            HashMap<String, Object> request = new HashMap<>();
            request.put("field", "$embeddings");
            request.put("op", "delete");
            request.put("value", embeddingsDelete);
            request.put("valueType", "arrayValue");
            updatedFormat.add(request);
        }

        Map<String, Object> filteredUpdates = filterOutEmbeddingInputs(updates);

        filteredUpdates.forEach((key, value) -> {
            HashMap<String, Object> request = new HashMap<>();
            request.put("field", key);
            if (value instanceof FieldValue) {
                FieldValue fieldValue = (FieldValue) value;
                request.put("op", fieldValue.getOperation().getOperationInString());
                // Handle different FieldValue operations appropriately

                if (fieldValue.getOperation() == FieldValue.Operation.SERVER_TIMESTAMP) {
                    servertimestamp.add(key);
                }

                if (fieldValue.getOperation() == FieldValue.Operation.INCREMENT) {
                    request.put("value", fieldValue.getValues()[0]);
                } else if (fieldValue.getOperation() == FieldValue.Operation.ARRAY_UNION ||
                    fieldValue.getOperation() == FieldValue.Operation.ARRAY_REMOVE) {
                    request.put("value", fieldValue.getValues());
                } else if (fieldValue.getValues().length == 0) {
                    request.put("value", "");
                } else {
                    request.put("value", fieldValue.getValues()[0]); // for any other single value operations
                }
                // FieldValue operations don't include valueType
            } else if (value instanceof byte[]) {
                request.put("op", "set");
                request.put("value", value);
            } else {
                request.put("op", "set");
                request.put("value", value);
            }
            updatedFormat.add(request);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("dataArray", updatedFormat);
        result.put("servertimestamp", servertimestamp);
        return result;
    }

    /**
     * Creates the update payload based on API version.
     *
     * @param data            the update data
     * @param path            the path segments
     * @param entityReference the document reference
     * @param operation       the transaction operation
     * @return the payload object
     * @throws FusabaseException if an error occurs
     */
    @NonNull
    private Object createUpdatePayload(@NonNull Map<String, Object> data,
            @NonNull List<String> path,
            @NonNull EntityReference entityReference,
            @NonNull TransactionOperation operation,
            boolean isAddDoc,
            @Nullable SetOptions setOptions) {
        Map<String, Object> updatedData = Collections.emptyMap();
        try {
            HashMap<String, Object> doc = new HashMap<>();
            List<String> servertimestamp = new ArrayList<>();
            Map<String, Object> payload = new HashMap<>();

            if (Objects.equals(API_VERSION, "1.0")) {
                if (!path.get(path.size() - 1).contains("BAAS_DOC_TEMP_ID=")
                        && (setOptions == null || setOptions.isMerge())
                        && !(entityReference instanceof DualityViewDocReference)) {

                    boolean isLastTransaction = false;

                    if (operation.getEndTrans() == 1) {
                        isLastTransaction = true;
                        operation.unsetEndTrans();
                    }

                    JsonObject fetchResult = this.fetchDocs(path,
                            entityReference instanceof DualityViewDocReference ? this.getDualityViewPathObject(path)
                                    : this.getPathObject(path),
                            operation);

                    if (isLastTransaction) {
                        operation.setEndTrans();
                    }

                    if (setOptions == null && fetchResult.getJsonArray("ret").isEmpty()) {
                        throw new FusabaseException("Document does not exist");
                    }
                    if (!fetchResult.getJsonArray("ret").isEmpty()) {
                        JsonObject result = fetchResult.getJsonArray("ret")
                                .getJsonObject(0)
                                .getJsonObject("osons");
                        JsonObject document = entityReference instanceof DualityViewDocReference
                                ? result.getJsonObject("DATA")
                                : result.containsKey("DOCUMENT") ? result.getJsonObject("DOCUMENT") : result;
                        doc = DataReader.getJsonObjectDataInMap(document);
                    }

                    // Remove metadata for duality views
                    if (entityReference instanceof DualityViewDocReference) {
                        doc.remove("_metadata");
                        doc.remove("oid");
                    }
                }

                updateDoc(servertimestamp, doc, data);
                payload.put("processedData", doc);
                payload.put("servertimestamp", servertimestamp);

                return payload;
            } else {
                if (isAddDoc) {
                    updateDoc(servertimestamp, doc, data);
                    Map<String, Object> embeddingsSet = extractEmbeddingsSetMap(doc);
                    List<String> embeddingsDelete = extractEmbeddingsDeleteKeys(doc);
                    if (!embeddingsDelete.isEmpty()) {
                        throw new FusabaseException("deleteVector() is not supported in addDoc/set create payload. Use update/set merge contexts.");
                    }
                    if (!embeddingsSet.isEmpty()) {
                        Map<String, Object> filteredDoc = filterOutEmbeddingInputs(doc);
                        filteredDoc.put("$embeddings", embeddingsSet);
                        doc = new HashMap<>(filteredDoc);
                    }
                    payload.put("processedData", doc);
                    payload.put("servertimestamp", servertimestamp);
                }
                if(isAddDoc)
                    return payload;

                return processUpdateData(data);
            }
        } catch (FusabaseException e) {
            FusabaseLogger.d("Error in creating updated payload " + e.getMessage());
        }
        return updatedData;
    }

    private boolean isDenseEmbeddingList(Object value) {
        if (!(value instanceof List<?>)) {
            return false;
        }
        for (Object item : (List<?>) value) {
            if (!(item instanceof Number)) {
                return false;
            }
        }
        return true;
    }

    private boolean isEmbeddingInput(Object value) {
        return value instanceof DenseVector || value instanceof SparseVector;
    }

    private Map<String, Object> normalizeEmbedding(Object value) {
        HashMap<String, Object> embedding = new HashMap<>();
        if (value instanceof DenseVector) {
            DenseVector dense = (DenseVector) value;
            VectorValidators.validateDenseVector(dense.getValues(), "Embedding values must be a numeric array.");
            embedding.put("type", "dense");
            embedding.put("values", dense.getValues());
            return embedding;
        }
        if (value instanceof SparseVector) {
            SparseVector sparse = (SparseVector) value;
            VectorValidators.validateSparseEmbedding(sparse);
            embedding.put("type", "sparse");
            embedding.put("dimension", sparse.getDimension());
            embedding.put("indices", sparse.getIndices());
            embedding.put("values", sparse.getValues());
            return embedding;
        }
        List<Double> denseValues = new ArrayList<>();
        for (Object item : (List<?>) value) {
            denseValues.add(((Number) item).doubleValue());
        }
        VectorValidators.validateDenseVector(denseValues, "Embedding values must be a numeric array.");
        embedding.put("type", "dense");
        embedding.put("values", denseValues);
        return embedding;
    }

    private Map<String, Object> extractEmbeddingsSetMap(@NonNull Map<String, Object> input) {
        HashMap<String, Object> setMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (isEmbeddingInput(entry.getValue())) {
                setMap.put(entry.getKey(), normalizeEmbedding(entry.getValue()));
            }
        }
        return setMap;
    }

    private List<String> extractEmbeddingsDeleteKeys(@NonNull Map<String, Object> input) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof FieldValue &&
                    ((FieldValue) value).getOperation() == FieldValue.Operation.DELETE_VECTOR) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    private Map<String, Object> filterOutEmbeddingInputs(@NonNull Map<String, Object> input) {
        HashMap<String, Object> filtered = new HashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Object value = entry.getValue();
            boolean isDeleteVector = value instanceof FieldValue
                    && ((FieldValue) value).getOperation() == FieldValue.Operation.DELETE_VECTOR;
            if (!isEmbeddingInput(value) && !isDeleteVector) {
                filtered.put(entry.getKey(), value);
            }
        }
        return filtered;
    }

    /**
     * Creates transaction header JSON object.
     *
     * @param beginTrans      begin transaction flag
     * @param endTrans        end transaction flag
     * @param transactionName the transaction name
     * @return the transaction header JSON
     */
    @NonNull
    protected JsonObject createTransactionHeader(int beginTrans,
            int endTrans,
            @NonNull String transactionName) {
        return Json.createObjectBuilder()
                .add("begin_trans", beginTrans)
                .add("end_trans", endTrans)
                .add("trans_name", transactionName)
                .build();
    }

    /**
     * Fetches documents from the database.
     *
     * @param path      the path segments
     * @param payload   the request payload
     * @param operation the transaction operation
     * @return the response JSON object
     * @throws FusabaseOracledbException if the fetch fails
     */
    @NonNull
    protected JsonObject fetchDocs(@NonNull List<String> path,
            @NonNull JsonObject payload,
            @NonNull TransactionOperation operation)
            throws FusabaseOracledbException {

        int maxReattempt = operation.getTransactionName().isEmpty() ? 1 : operation.getOptions().maxAttempts;
        HttpRequestHelper requestHelper = new HttpRequestHelper(maxReattempt);

        // Populate headers
        Map<String, String> headers = new HashMap<>();
        headers.put("x-transaction", createTransactionHeader(operation.getBeginTrans(),
                operation.getEndTrans(),
                operation.getTransactionName()).toString());
        headers.put("Content-Type", "application/json");

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        // Create the request
        try {
            requestHelper.createHttpRequest(Utils.urlBuilder(this.options.getOrdsHost(),
                    BAAS_SERVICE,
                    DATABASE,
                    this.options.getProjectId(),
                    this.API_VERSION.equals("2.0") ? V2 : V1,
                    RUN_QUERY),
                    "POST",
                    headers,
                    queryParameters,
                    payload.toString());
        } catch (Exception e) {
            throw new FusabaseOracledbException(e.getMessage(), FusabaseOracledbException.Code.INTERNAL);
        }

        HttpResponse response;

        try {
            response = requestHelper.executeRequest();
        } catch (FusabaseException e) {
            throw new FusabaseOracledbException(e.getMessage(), FusabaseOracledbException.Code.NETWORK_ERROR, e);
        }

        if (operation.getBeginTrans() == 1)
            operation.unsetBeginTrans();

        JsonObject result;

        if (response.getStatus() && response.getCode() == 200) {
            FusabaseLogger.i("FusabaseOradb", "Document Reference fetch docs executed successfully");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            result = reader.readObject();
            reader.close();
        } else if (response.getCode() == 404 || response.getCode() == 403){
              return Json.createObjectBuilder().add("ret", Json.createArrayBuilder().build()).build();
        } else {
            FusabaseLogger.w("FusabaseOradb", "getDoc failed with response code " + response.getCode());
            throw new FusabaseOracledbException("Query execution failed",
                    FusabaseOracledbException.Code.fromCode(response.getCode()));
        }
        return result;
    }

    /**
     * Removes documents from the database.
     *
     * @param path            the path segments
     * @param entityReference the document reference
     * @param operation       the transaction operation
     * @return the response JSON object
     * @throws FusabaseException if the removal fails
     */
    @NonNull
    protected JsonObject removeDocs(@NonNull List<String> path,
            @NonNull EntityReference entityReference,
            @NonNull TransactionOperation operation) throws FusabaseException {

        int maxReattempt = operation.getTransactionName().isEmpty() ? 1 : operation.getOptions().maxAttempts;

        HttpRequestHelper requestHelper = new HttpRequestHelper(maxReattempt);
        // Populate header
        Map<String, String> headers = new HashMap<>();
        headers.put("x-transaction", createTransactionHeader(operation.getBeginTrans(),
                operation.getEndTrans(),
                operation.getTransactionName()).toString());
        headers.put("Content-Type", "application/json");

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());
//        if (!Objects.equals(operation.getTransactionName(), "")
//                &&
//                operation.getVersion() != TransactionOperation.DOES_NOT_EXIST_VERSION) {
//            queryParameters.put("version", String.valueOf(operation.getVersion()));
//        }

        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();

        if (entityReference instanceof DualityViewDocReference) {
            payloadBuilder.add("dv_name", path.get(0));
            if (path.size() > 1) {
                payloadBuilder.add("oid", path.get(1));
            }
        } else {
            payloadBuilder.add("path", Json.createArrayBuilder(path).build());
        }

        if (!Objects.equals(operation.getTransactionName(), "")
            &&
            !Objects.equals(operation.getVersion(), TransactionOperation.DOES_NOT_EXIST_VERSION)) {
            payloadBuilder.add("version", String.valueOf(operation.getVersion()));
        }

        // Construct request body
        JsonObject payload = payloadBuilder.build();

        // Create the request
        requestHelper.createHttpRequest(Utils.urlBuilder(this.options.getOrdsHost(),
                BAAS_SERVICE,
                DATABASE,
                this.options.getProjectId(),
                DELETE_DOCUMENT),
                "PUT",
                headers,
                queryParameters,
                payload.toString());

        HttpResponse response = requestHelper.executeRequest();

        if (operation.getBeginTrans() == 1)
            operation.unsetBeginTrans();

        if (response.getStatus()) {
            FusabaseLogger.i("FusabaseOradb", "deleteDocSuccessful");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        } else {
            FusabaseLogger.w("FusabaseOradb", "deleteDoc failed with response code " + response.getCode());
            throw new FusabaseOracledbException("Document deletion failed",
                    FusabaseOracledbException.Code.fromCode(response.getCode()));
        }
    }

    /**
     * Sets documents in the database.
     *
     * @param path            the path segments
     * @param data            the document data
     * @param setOptions      the set options
     * @param entityReference the document reference
     * @param operation       the transaction operation
     * @return the response JSON object
     * @throws FusabaseException if the set operation fails
     */
    protected JsonObject setDocs(@NonNull List<String> path,
            @NonNull Object data,
            SetOptions setOptions,
            @NonNull EntityReference entityReference,
            @NonNull TransactionOperation operation) throws FusabaseException {

        entityReference.updatePathIfNecessary();

        int maxReattempt = operation.getTransactionName().isEmpty() ? 1 : operation.getOptions().maxAttempts;

        HttpRequestHelper requestHelper = new HttpRequestHelper(maxReattempt);

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        Map<String, Object> dataMap = DataReader.convertDataInHashMap(data);

        Map<String, Object> updatedDataMap = new HashMap<>();

        updatedDataMap.put("dataBody", createUpdatePayload(dataMap, path, entityReference, operation, setOptions == null, setOptions));
        JsonValue documentData = DataReader.getDataInJsonValue(updatedDataMap);

        JsonValue preparedBody;

        if (API_VERSION.equals("2.0")) {
            if (setOptions == null)
                preparedBody = documentData.asJsonObject().getJsonObject("dataBody");
            else
                preparedBody = documentData.asJsonObject().getJsonObject("dataBody").getJsonArray("dataArray");
        } else {
            preparedBody = documentData.asJsonObject().getJsonObject("dataBody");
        }

        if (entityReference.isOidNotGenerated()) {
            path.remove(path.size() - 1);
        }

        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();

        if (entityReference instanceof DualityViewDocReference) {
            payloadBuilder.add("dv_name", path.get(0));
            if (path.size() > 1) {
                payloadBuilder.add("oid", path.get(1));
            }
        } else {
            payloadBuilder.add("path", Json.createArrayBuilder(path).build());
        }

        if (setOptions != null) {
            payloadBuilder.add("options", Json.createObjectBuilder()
                    .add("merge", setOptions.isMerge())
                    .add("mergeFields",
                            setOptions.getMergeFields() != null
                                    ? Json.createArrayBuilder(setOptions.getMergeFields()).build()
                                    : Json.createArrayBuilder().build())
                    .build());
        } else if (entityReference instanceof DocumentReference && preparedBody != null &&
                preparedBody.asJsonObject().containsKey("servertimestamp")) {
            payloadBuilder.add("servertimestamp", preparedBody.asJsonObject().get("servertimestamp"));
        }

        if (preparedBody instanceof JsonObject) {
            if (preparedBody.asJsonObject().get("processedData").asJsonObject().isEmpty()) {
                throw new FusabaseException("Invalid Data provided to set()");
            }
            payloadBuilder.add("data", preparedBody.asJsonObject().get("processedData"));
        } else {
            if (preparedBody != null && preparedBody.asJsonArray().isEmpty()) {
                throw new FusabaseException("Invalid Data provided to set()");
            }
            payloadBuilder.add("data", preparedBody);
        }

        if (setOptions != null && Objects.equals(API_VERSION, "2.0"))
            payloadBuilder.add("apiversion", 2);
        // Construct request body

        if (!Objects.equals(operation.getTransactionName(), "")
            &&
            !Objects.equals(operation.getVersion(), TransactionOperation.DOES_NOT_EXIST_VERSION)
            &&
            operation.getVersion() != null) {
            payloadBuilder.add("version", String.valueOf(operation.getVersion()));
        }

        JsonObject payload = payloadBuilder.build();

        // Populate headers
        Map<String, String> headers = new HashMap<>();

        headers.put("x-transaction", createTransactionHeader(operation.getBeginTrans(),
                operation.getEndTrans(),
                operation.getTransactionName()).toString());
        headers.put("Content-Type", "application/json");

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        // Create the request
        requestHelper.createHttpRequest(Utils.urlBuilder(this.options.getOrdsHost(),
                BAAS_SERVICE,
                DATABASE,
                this.options.getProjectId(),
                this.API_VERSION.equals("2.0") ? V2 : V1,
                SET_DOCUMENT),
                "POST",
                headers,
                queryParameters,
                payload.toString());

        HttpResponse response = requestHelper.executeRequest();

        if (operation.getBeginTrans() == 1)
            operation.unsetBeginTrans();

        if (response.getStatus()) {
            FusabaseLogger.i("FusabaseOradb", "setDocSuccessful");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();

            if (entityReference.isOidNotGenerated()) {
                String oid = null;
                JsonValue oidValue = result.get("OID");
                if (oidValue != null && oidValue != JsonValue.NULL) {
                    if (oidValue.getValueType() == JsonValue.ValueType.STRING) {
                        oid = ((JsonString) oidValue).getString();
                    } else if (oidValue.getValueType() == JsonValue.ValueType.NUMBER) {
                        oid = String.valueOf(((JsonNumber) oidValue).longValue());
                    } else {
                        oid = oidValue.toString().replace("\"", "");
                    }
                }
                if (oid != null) {
                    entityReference.updateId(oid);
                }
            }
            return result;

        } else {
            FusabaseLogger.w("FusabaseOradb", "setDoc failed with response code " + response.getCode());
            throw new FusabaseOracledbException("Document creation failed",
                    FusabaseOracledbException.Code.fromCode(response.getCode()));
        }
    }

    /**
     * Updates documents in the database.
     *
     * @param path            the path segments
     * @param data            the update data
     * @param entityReference the document reference
     * @param operation       the transaction operation
     * @return the response JSON object
     * @throws FusabaseException if the update fails
     */
    protected JsonObject updateDocs(@NonNull List<String> path,
            @NonNull Map<String, Object> data,
            EntityReference entityReference,
            @NonNull TransactionOperation operation,
            JsonObject params) throws FusabaseException {

        int maxReattempt = operation.getTransactionName().isEmpty() ? 1 : operation.getOptions().maxAttempts;

        HttpRequestHelper requestHelper = new HttpRequestHelper(maxReattempt);

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("apiKey", this.options.getAppId());

        Map<String, Object> dataMap = DataReader.convertDataInHashMap(data);
        Map<String, Object> updatedDataMap = new HashMap<>();
        updatedDataMap.put("dataBody", createUpdatePayload(dataMap, path, entityReference, operation, false, null));
        JsonValue documentData = DataReader.getDataInJsonValue(updatedDataMap);

        JsonValue preparedBody;

        if (API_VERSION.equals("2.0")) {
            preparedBody = documentData.asJsonObject()
                .getJsonObject("dataBody")
                .getJsonArray("dataArray");
        } else {
            preparedBody = documentData.asJsonObject().getJsonObject("dataBody");
        }

        JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();

        if (entityReference instanceof DualityViewDocReference) {
            payloadBuilder.add("dv_name", path.get(0));
            if (path.size() > 1) {
                payloadBuilder.add("oid", path.get(1));
            }
        } else {
            if(params != null)
                payloadBuilder.add("path", Json.createArrayBuilder(path).add("*").build());
            else
                payloadBuilder.add("path", Json.createArrayBuilder(path).build());
        }

        if (preparedBody instanceof JsonObject)
            payloadBuilder.add("data", preparedBody.asJsonObject().get("processedData"));
        else {
            payloadBuilder.add("data", preparedBody);
        }

        if (Objects.equals(API_VERSION, "2.0"))
            payloadBuilder.add("apiversion", 2);

        if (!Objects.equals(operation.getTransactionName(), "")
            &&
            !Objects.equals(operation.getVersion(), TransactionOperation.DOES_NOT_EXIST_VERSION)) {
            payloadBuilder.add("version", String.valueOf(operation.getVersion()));
        }

        if(params != null &&
            params.containsKey("conditions") &&
            !params.getJsonArray("conditions").isEmpty()) {
            payloadBuilder.add("conditions", params.getJsonArray("conditions"));
        }

        // Populate headers
        Map<String, String> headers = new HashMap<>();
        headers.put("x-transaction", createTransactionHeader(operation.getBeginTrans(),
                operation.getEndTrans(),
                operation.getTransactionName()).toString());
        headers.put("Content-Type", "application/json");

        String authHeader = this.buildAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            headers.put("Authorization", authHeader);
        }

        // Construct request body
        JsonObject payload = payloadBuilder.build();

        // Create the request
        requestHelper.createHttpRequest(Utils.urlBuilder(this.options.getOrdsHost(),
                BAAS_SERVICE,
                DATABASE,
                this.options.getProjectId(),
                this.API_VERSION.equals("2.0") ? V2 : V1,
                UPDATE_DOCUMENT),
                "PUT",
                headers,
                queryParameters,
                payload.toString());

        HttpResponse response = requestHelper.executeRequest();

        if (operation.getBeginTrans() == 1)
            operation.unsetBeginTrans();

        if (response.getStatus()) {
            FusabaseLogger.i("FusabaseOradb", "updateDocSuccessful");
            JsonReader reader = Json.createReader(new StringReader(response.getResponse()));
            JsonObject result = reader.readObject();
            reader.close();
            return result;
        } else {
            FusabaseLogger.w("FusabaseOradb", "updateDoc failed with response code " + response.getCode());
            throw new FusabaseOracledbException("Document update failed",
                    FusabaseOracledbException.Code.fromCode(response.getCode()));
        }
    }
}
