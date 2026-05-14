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
import com.oracle.mobile.fusabase.utils.Utils;
import com.oracle.mobile.fusabase.task.Task;
import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Represents a batch of write operations that can be executed atomically.
 * WriteBatch allows multiple write operations (set, update, delete) to be
 * grouped together and committed as a single batch operation.
 */
public class WriteBatch {

    /**
     * An interface for implementing operations that execute safely within
     * a WriteBatch.
     */
    public interface Function {
        /**
         * Method that will execute the operations as a batch.
         *
         * @param batch a WriteBatch instance
         */
        abstract void apply(@NonNull WriteBatch batch);
    }

    /** List of write operations to be performed in this batch. */
    private final List<WriteOperation> operations;

    /** Unique name for this batch transaction. */
    private String transactionName;

    /** Maximum number of retry attempts for failed operations. */
    private final static int RETRIES = 5;

    /**
     * Private constructor for creating WriteBatch instances.
     */
    WriteBatch()
    {
        this.transactionName = Utils.getUniqueTransactionName();
        this.operations = new ArrayList<>();
    }

    /**
     * Commits all the operations in this batch atomically.
     *
     * @return a Task that completes when the batch commit is finished
     */
    @NonNull
    public Task<Void> commit()
    {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        Task<Void> task = taskCompletionSource.getTask();

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {

            int operationCount = 0;
            TransactionOperation operation = new TransactionOperation(1,
                    0,
                    this.transactionName,
                    new TransactionOptions.Builder().build(),
                    TransactionOperation.DOES_NOT_EXIST_VERSION);

            for(WriteOperation writeOperation : operations) {
                DocumentReference docRef = writeOperation.getDocRef();
                BulkUpdate bulkUpdate = writeOperation.getBulkUpdate();
                Objects.requireNonNull(docRef, "DocumentReference cannot be null");

                operationCount++;

                if(operationCount == operations.size())
                    operation.setEndTrans();

                // Versions are set to DOES_NOT_EXIST since we don't have the current version as
                // write batch doesn't involve fetching of docs
                switch(writeOperation.getOperation()){
                    case SET:
                        try {
                            docRef.getQueryHelper().setDocs(docRef.getPathSegments(),
                                    writeOperation.getData(),
                                    writeOperation.getOptions(),
                                    docRef,
                                    operation);

                        } catch (FusabaseException e) {
                            Exception exception = e;
                            if(!(exception instanceof FusabaseOracledbException))
                                exception = new FusabaseOracledbException(exception.getMessage() == null ?
                                        "Internal Error Occurred" : exception.getMessage(), FusabaseOracledbException.Code.INTERNAL);
                            throw new CompletionException(exception);
                        }
                        break;
                    case UPDATE:
                        try {
                            docRef.getQueryHelper().updateDocs(docRef.getPathSegments(),
                                    (Map<String, Object>) writeOperation.getData(),
                                    docRef,
                                    operation,
                                    null);
                        } catch (FusabaseException e) {
                            Exception exception = e;
                            if(!(exception instanceof FusabaseOracledbException))
                                exception = new FusabaseOracledbException(exception.getMessage() == null ?
                                        "Internal Error Occurred" : exception.getMessage(), FusabaseOracledbException.Code.INTERNAL);
                            throw new CompletionException(exception);
                        }
                        break;
                    case DELETE:
                        try {
                            docRef.getQueryHelper().removeDocs(docRef.getPathSegments(),
                                    docRef,
                                    operation);
                        } catch (Exception e) {
                            Exception exception = e;
                            if(!(exception instanceof FusabaseOracledbException))
                                exception = new FusabaseOracledbException(exception.getMessage() == null ?
                                        "Internal Error Occurred" : exception.getMessage(), FusabaseOracledbException.Code.INTERNAL);
                            throw new CompletionException(exception);
                        }
                        break;
                    case BULK_UPDATE:
                        try {
                            bulkUpdate.queryHelper.updateDocs(docRef.getPathSegments(),
                                (Map<String, Object>) writeOperation.getData(),
                                docRef,
                                operation,
                                bulkUpdate.getConditions());
                        } catch (Exception e) {
                            Exception exception = e;
                            if(!(exception instanceof FusabaseOracledbException))
                                exception = new FusabaseOracledbException(exception.getMessage() == null ?
                                    "Internal Error Occurred" : exception.getMessage(), FusabaseOracledbException.Code.INTERNAL);
                            throw new CompletionException(exception);
                        }
                        break;
                }
            }
            taskCompletionSource.setResult(null);
            return "";
        });

        taskCompletionSource.handleFuture(future);
        return task;
    }

    /**
     * Adds a delete operation to the batch for the specified document.
     *
     * @param documentRef the reference to the document to delete
     * @return this WriteBatch instance for chaining
     */
    @NonNull
    public WriteBatch delete(@NonNull DocumentReference documentRef)
    {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");
        operations.add(new WriteOperation(WriteOperation.OperationType.DELETE,
            documentRef,
            null,
            null,
            null,
            null,
            null));
        return this;
    }

    /**
     * Adds a set operation to the batch that overwrites the entire document.
     *
     * @param documentRef the reference to the document to set
     * @param data the document data to set
     * @return this WriteBatch instance for chaining
     */
    @NonNull
    public WriteBatch set(@NonNull DocumentReference documentRef, @NonNull Object data)
    {
        return set(documentRef, data, SetOptions.OVERWRITE);
    }

    /**
     * Adds a set operation to the batch with specified set options.
     *
     * @param documentRef the reference to the document to set
     * @param data the document data to set
     * @param options the set options (merge vs overwrite)
     * @return this WriteBatch instance for chaining
     */
    @NonNull
    public WriteBatch set(@NonNull DocumentReference documentRef, @NonNull Object data,
                          @NonNull SetOptions options)
    {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");
        Objects.requireNonNull(data, "Data cannot be null");
        Objects.requireNonNull(options, "SetOptions cannot be null");
        operations.add(new WriteOperation(WriteOperation.OperationType.SET,
            documentRef,
            data,
            options,
            null,
            null,
            null));
        return this;
    }

    /**
     * Adds an update operation to the batch using a map of field-value pairs.
     *
     * @param documentRef the reference to the document to update
     * @param data map of field names to values to update
     * @return this WriteBatch instance for chaining
     */
    @NonNull
    public WriteBatch update(@NonNull DocumentReference documentRef, @NonNull Map<String, Object> data)
    {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");
        Objects.requireNonNull(data, "Data cannot be null");
        operations.add(new WriteOperation(WriteOperation.OperationType.UPDATE,
            documentRef,
            data,
            null,
            null,
            null,
            null));
        return this;
    }

    @NonNull
    public WriteBatch bulkUpdate(@NonNull BulkUpdate bulkUpdate, @NonNull Object data) {
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
     * Adds an update operation to the batch using field name-value pairs.
     *
     * @param documentRef the reference to the document to update
     * @param field the name of the field to update
     * @param value the new value for the field
     * @param moreFieldsAndValues additional field-value pairs
     * @return this WriteBatch instance for chaining
     */
    @NonNull
    public WriteBatch update(@NonNull DocumentReference documentRef, @NonNull String field,
                             @Nullable Object value, Object... moreFieldsAndValues)
    {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");
        Objects.requireNonNull(field, "Field cannot be null");
        Map<String, Object> data = new HashMap<>();
        data.put(field, value);

        for(int i=0; i<moreFieldsAndValues.length; i+=2) {
            data.put((String) moreFieldsAndValues[i], moreFieldsAndValues[i+1]);
        }

        return update(documentRef, data);
    }

    /**
     * Adds an update operation to the batch using FieldPath-value pairs.
     *
     * @param documentRef the reference to the document to update
     * @param fieldPath the path of the field to update
     * @param value the new value for the field
     * @param moreFieldsAndValues additional field path-value pairs
     * @return this WriteBatch instance for chaining
     */
    @NonNull
    public WriteBatch update(@NonNull DocumentReference documentRef, @NonNull FieldPath fieldPath,
                             @Nullable Object value, Object... moreFieldsAndValues)
    {
        Objects.requireNonNull(documentRef, "DocumentReference cannot be null");
        Objects.requireNonNull(fieldPath, "FieldPath cannot be null");
        Map<String, Object> data = new HashMap<>();
        data.put(fieldPath.toString(), value);

        for(int i=0; i<moreFieldsAndValues.length; i+=2) {
            data.put(moreFieldsAndValues[i].toString(), moreFieldsAndValues[i+1]);
        }

        return update(documentRef, data);
    }
}
