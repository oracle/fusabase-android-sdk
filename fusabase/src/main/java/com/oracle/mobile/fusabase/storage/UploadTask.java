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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents an upload task, which is a subclass of {@link StorageTask}.
 * Provides additional functionality specific to uploading data to cloud storage.
 */
public class UploadTask extends StorageTask<UploadTask.TaskSnapshot> {

    private OnPausedListener<UploadTask.TaskSnapshot> pausedListener;
    private OnProgressListener<UploadTask.TaskSnapshot> progressListener;

    protected UploadTask (@NonNull StorageReference storageRef) {
        super(storageRef);
        this.setResult(this. new TaskSnapshot(storageRef, new StorageMetadata.Builder().build(), 0,0));
    }

    /**
     * Class TaskSnapshot
     * A class representing the status of the UploadTask progress.
     */
    public class TaskSnapshot extends StorageTask<UploadTask.TaskSnapshot>.SnapshotBase {

        private final long bytesTransferred;
        private final long totalByteCount;
        @Nullable Error error;
        private final StorageMetadata metadata;

        protected TaskSnapshot (@NonNull StorageReference storageRef,
                                @Nullable StorageMetadata metadata,
                                long bytesTransferred,
                                long totalByteCount) {
            super(storageRef);
            this.bytesTransferred = bytesTransferred;
            this.totalByteCount = totalByteCount;
            this.error = null;
            this.metadata = metadata;
        }

        /**
         * Method to get the total no. of bytes to be transferred.
         * @return long
         */
        public long getTotalByteCount() {
            return totalByteCount;
        }

        /**
         * Method to get the total no. of bytes transferred for the upload task.
         * @return long The total bytes transferred.
         */
        public long getBytesTransferred() {
            return bytesTransferred;
        }

        /**
         * Method to get the metadata for the current upload.
         * @return StorageMetadata
         */
        public StorageMetadata getMetadata() {
            return this.metadata;
        }

    }

}
