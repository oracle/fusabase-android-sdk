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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * Represents a transaction that can perform multiple read and write operations
 * atomically. Transactions ensure that all operations either succeed together
 * or fail together, maintaining data consistency.
 */
public class Transaction {

    /**
     * An interface for implementing operations that execute safely within
     * a transaction.
     *
     * @param <TResult> the type of the result returned by the transaction function
     */
    public interface Function<TResult> {
        /**
         * Method that will execute the operations as a transaction.
         *
         * @param transaction an {@code Transaction}
         * @return {@code TResult} Result of executing the transaction.
         * @throws FusabaseOracledbException if the transaction fails
         */
        public abstract @Nullable TResult apply(@NonNull Transaction transaction)
                throws FusabaseOracledbException;
    }

    /** Log tag for debugging. */
    private static final String TAG = "FusabaseOradb";

    /** The Oradb instance this transaction belongs to. */
    private final FusabaseOracledb oradb;

    /** Transaction configuration options. */
    private final TransactionOptions options;

    /** Unique name for this transaction. */
    private String transactionName;

    /** List of write operations to be performed in this transaction. */
    private final List<WriteOperation> operations;

    /** Flag indicating if the first operation has been executed. */
    private TransactionOperation operation;

    /** Flag indicating if this is the last operation in the transaction. */
    private int isLastOperation;

    /** Map of document paths to their current versions for optimistic locking. */
    private final HashMap<String, String> versions;

    /**
     * Private constructor for creating Transaction instances.
     *
     * @param oradb the Oradb instance this transaction belongs to
     * @param options transaction configuration options
     */
    Transaction(@NonNull FusabaseOracledb oradb, @NonNull TransactionOptions options) {
        Objects.requireNonNull(oradb, "FusabaseOracledb cannot be null");
        Objects.requireNonNull(options, "TransactionOptions cannot be null");
        this.oradb = oradb;
        this.options = options;
        this.transactionName = Utils.getUniqueTransactionName();
        this.operations = new ArrayList<>();
        this.operation = new TransactionOperation(1,
            0,
            this.transactionName,
            this.options,
            TransactionOperation.DEFAULT_VERSION
        );
        this.isLastOperation = 0;
        this.versions = new HashMap<String, String>();
    }

    /**
     * Method to delete the document referenced by the provided DocumentReference
     *
     * @param documentRef {@code DocumentReference}
     * @return {@code Transaction}
     */
    @NonNull
    public Transaction delete(@NonNull DocumentReference documentRef) {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");
        if (!this.versions.containsKey(documentRef.getPath())
                ||
                this.versions.get(documentRef.getPath()) == null) {
            throw new IllegalArgumentException("Transaction cannot be performed since there is no " +
                    "get operation performed for this document reference");
        }

        if (Objects.equals(this.versions.get(documentRef.getPath()),
                TransactionOperation.DOES_NOT_EXIST_VERSION))
            throw new IllegalStateException("The document referred by path " +
                    documentRef.getPath() + " doesn't exists.");

        operations.add(new WriteOperation(WriteOperation.OperationType.DELETE,
                documentRef,
                null,
                null,
                this.versions.get(documentRef.getPath()),
                null,
                null));
        return this;
    }

    /**
     * Method to get the document referenced by the provided DocumentReference
     *
     * @param documentRef {@code DocumentReference}
     * @return {@code Transaction}
     */
    @NonNull
    public DocumentSnapshot get(@NonNull DocumentReference documentRef) {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");

        JsonObject result = null;

        try {
            result = documentRef.getQueryHelper().fetchDocs(documentRef.getPathSegments(),
                    documentRef.getParams(false),
                   operation
            );

        } catch (FusabaseOracledbException e) {
            throw new CompletionException(e);
        }


        if (!result.getJsonArray("ret").isEmpty()) {
            JsonObject data = result.getJsonArray("ret")
                    .getJsonObject(0)
                    .getJsonObject("osons");

            JsonObject document = data.getJsonObject("DOCUMENT");
            if (document != null) {
                HashMap<String, Object> dataMap =
                        DataReader.getJsonObjectDataInMap(document);

                DocumentSnapshot snap = new DocumentSnapshot(oradb,
                    new Document(dataMap,
                        data.getString("OID"),
                        data.getString("CREATED"),
                        data.getString("LAST_MODIFIED"),
                        String.valueOf(data.getInt("VERSION")),
                        "",
                        0L,
                        documentRef.getPathSegments(),
                        ""),
                    documentRef,
                    String.join("/", documentRef.getPathSegments()),
                    false,
                    false);

                this.versions.put(documentRef.getPath(), data.get("VERSION").toString());
                return snap;
            }
        }
        // Document doesn't exists, it might be possible that user is checking that
        // a particular document exists or not
        this.versions.put(documentRef.getPath(), TransactionOperation.DOES_NOT_EXIST_VERSION);
        return new DocumentSnapshot(oradb,
                null,
                documentRef,
                String.join("/", documentRef.getPathSegments()),
                false,
                false);

    }

    /**
     * Method to set the document referenced by the provided DocumentReference and provided data.
     * Note that this method inherently overwrites the entire document
     *
     * @param documentRef {@code DocumentReference}
     * @param data        {@code Object} Document Data
     * @return {@code Transaction}
     */
    @NonNull
    public Transaction set(@NonNull DocumentReference documentRef, @NonNull Object data) {
        return set(documentRef, data, SetOptions.OVERWRITE);
    }

    @NonNull
    public Transaction bulkUpdate(@NonNull BulkUpdate bulkUpdate, @NonNull Object data) {
        Objects.requireNonNull(bulkUpdate, "DocumentReference cannot be null");
        Objects.requireNonNull(data, "Data cannot be null");
        operations.add(new WriteOperation(WriteOperation.OperationType.BULK_UPDATE,
            null,
            data,
            null,
            null,
            bulkUpdate,
            bulkUpdate.getConditions()));
        return this;
    }

    /**
     * Method to set the document referenced by the provided DocumentReference,TransactionOptions
     * and provided data
     *
     * @param documentRef {@code DocumentReference}
     * @param data        {@code Object} Document Data
     * @param options     {@code SetOptions} Set option for the set operation
     * @return {@code Transaction}
     */
    @NonNull
    public Transaction set(@NonNull DocumentReference documentRef, @NonNull Object data,
                           @NonNull SetOptions options) {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");

        if (options.isMerge() &&
                Objects.equals(this.versions.get(documentRef.getPath()), TransactionOperation.DOES_NOT_EXIST_VERSION))
            throw new IllegalStateException("The document referred by path " +
                    documentRef.getPath() + " doesn't exists.");

        if (options.isMerge() && !this.versions.containsKey(documentRef.getPath())) {
            throw new IllegalStateException("Please perform a get operation on " +
                    documentRef.getPath() + " before performing any write operation.");
        }

        operations.add(new WriteOperation(WriteOperation.OperationType.SET,
                documentRef,
                data,
                options,
                this.versions.get(documentRef.getPath()),
                null,
                null));
        return this;
    }

    /**
     * Method to update the document referenced by the provided DocumentReference and values
     *
     * @param documentRef {@code DocumentReference}
     * @param data        {@code Map<String, Object>} Update fields and values in form of a Map
     * @return {@code Transaction}
     */
    @NonNull
    public Transaction update(@NonNull DocumentReference documentRef, @NonNull Map<String, Object> data) {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");
        if (!this.versions.containsKey(documentRef.getPath())
                ||
                this.versions.get(documentRef.getPath()) == null) {
            throw new IllegalArgumentException("Transaction cannot be performed since there is no " +
                    "get operation performed for this document reference");
        }

        operations.add(new WriteOperation(WriteOperation.OperationType.UPDATE,
                documentRef,
                data,
                null,
                this.versions.get(documentRef.getPath()),
            null,
            null));
        return this;
    }

    /**
     * Method to update the document referenced by the provided DocumentReference, fieldName
     * and values
     *
     * @param documentRef         {@code DocumentReference}
     * @param field               {@code String} Name of the field
     * @param value               {@code Object} Value of the field
     * @param moreFieldsAndValues {@code Object[]} More fields and value pairs
     * @return {@code Transaction}
     */
    @NonNull
    public Transaction update(@NonNull DocumentReference documentRef, @NonNull String field,
                              @Nullable Object value, Object... moreFieldsAndValues) {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");
        Map<String, Object> data = new HashMap<>();
        data.put(field, value);

        for (int i = 0; i < moreFieldsAndValues.length; i += 2) {
            data.put((String) moreFieldsAndValues[i], moreFieldsAndValues[i + 1]);
        }

        return update(documentRef, data);
    }

    /**
     * Method to update the document referenced by the provided DocumentReference, fieldPath
     * and values
     *
     * @param documentRef         {@code DocumentReference}
     * @param fieldPath           {@code FieldPath} Path of the field
     * @param value               {@code Object} Value of the field
     * @param moreFieldsAndValues {@code Object[]} More fieldPath and value pairs
     * @return {@code Transaction}
     */
    @NonNull
    public Transaction update(@NonNull DocumentReference documentRef,
                              @NonNull FieldPath fieldPath,
                              @Nullable Object value,
                              Object... moreFieldsAndValues) {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");
        Map<String, Object> data = new HashMap<>();
        data.put(fieldPath.toString(), value);

        for (int i = 0; i < moreFieldsAndValues.length; i += 2) {
            data.put(moreFieldsAndValues[i].toString(), moreFieldsAndValues[i + 1]);
        }
        return update(documentRef, data);
    }

    protected void apply() throws FusabaseException {
        int operationCount = 0;
        for (WriteOperation writeOperation : operations) {
            DocumentReference docRef = writeOperation.getDocRef();
            BulkUpdate bulkUpdate = writeOperation.getBulkUpdate();
            JsonObject params = writeOperation.getParams();

            Objects.requireNonNull(docRef, "DocumentReference cannot be null");

            operationCount++;
            if (operationCount == operations.size())
                operation.setEndTrans();

            JsonObject result = JsonObject.EMPTY_JSON_OBJECT;

            operation.setVersion(versions.get(docRef.getPath()));
            switch (writeOperation.getOperation()) {
                case SET:
                    result = docRef.getQueryHelper().setDocs(docRef.getPathSegments(),
                        writeOperation.getData(),
                        writeOperation.getOptions(),
                        docRef,
                        operation
                    );
                    break;
                case UPDATE:
                    result = docRef.getQueryHelper().updateDocs(docRef.getPathSegments(),
                        (Map<String, Object>) writeOperation.getData(),
                        docRef,
                        operation,
                        null
                    );
                    break;
                case DELETE:
                    result = docRef.getQueryHelper().removeDocs(docRef.getPathSegments(),
                        docRef,
                        operation
                    );
                    break;
                case BULK_UPDATE:
                    if(params == null || bulkUpdate == null)
                        throw new FusabaseOracledbException("Invalid parameter created for Bulk Update in Transaction.",
                            FusabaseOracledbException.Code.INTERNAL);
                    result = bulkUpdate.queryHelper.updateDocs(bulkUpdate.path,
                        (Map<String, Object>) writeOperation.getData(),
                        null,
                        operation,
                        params
                    );
            }

            if (result.get("OID") == JsonValue.NULL) {
                FusabaseLogger.w("Transaction", "Transaction failed. OID is null");
                throw new FusabaseOracledbException("Transaction failed. OID is null", FusabaseOracledbException.Code.INTERNAL);
            } else if (writeOperation.getOperation() != WriteOperation.OperationType.DELETE){
                this.versions.put(docRef.getPath(), result.get("VERSION").toString());
            }
        }
    }
}
