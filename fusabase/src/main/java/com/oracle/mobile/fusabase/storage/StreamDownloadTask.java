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

import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a task for downloading data from a storage location as a stream.
 */
public class StreamDownloadTask extends StorageTask<StreamDownloadTask.TaskSnapshot> {

    protected AtomicBoolean keepRunning;
    private OnPausedListener<StreamDownloadTask.TaskSnapshot> pausedListener;
    private OnProgressListener<StreamDownloadTask.TaskSnapshot> progressListener;

    /**
     * Constructs a new instance of StreamDownloadTask.
     *
     * @param reference The storage reference for the data to download.
     */
    protected StreamDownloadTask(@NonNull StorageReference reference) {
        super(reference);
        this.keepRunning = new AtomicBoolean(false);
    }

    /**
     * Called when the task is canceled.
     */
    protected void onCanceled() {
    }

    /**
     * Pauses the task.
     *
     * @return True if the task was successfully paused, false otherwise.
     * @throws UnsupportedOperationException Always thrown as pause operation is not supported for downloads.
     */
    @Override
    public boolean pause() {
        throw new UnsupportedOperationException(
            "Pause operation is not supported for stream downloads. " +
            "The BAAS server does not support resumable downloads.");
    }

    /**
     * Resumes the task.
     *
     * @return True if the task was successfully resumed, false otherwise.
     * @throws UnsupportedOperationException Always thrown as resume operation is not supported for downloads.
     */
    @Override
    public boolean resume() {
        throw new UnsupportedOperationException(
            "Resume operation is not supported for stream downloads. " +
            "The BAAS server does not support resumable downloads.");
    }

    /**
     * Cancels the task.
     *
     * @return True if the task was successfully canceled, false otherwise.
     * @throws UnsupportedOperationException Always thrown as cancel operation is not supported for downloads.
     */
    @Override
    public boolean cancel() {
        throw new UnsupportedOperationException(
            "Cancel operation is not supported for stream downloads. " +
            "The BAAS server does not support resumable downloads.");
    }

    /**
     * Interface for processing the downloaded stream.
     */
    public interface StreamProcessor {
        /**
         * Processes the input stream.
         *
         * @param inputStream The input stream to process.
         * @throws Exception If an error occurs during processing.
         */
        void process(@NonNull InputStream inputStream) throws Exception;
    }

    /**
     * Represents a snapshot of the task's current state.
     */
    public class TaskSnapshot extends StorageTask<StreamDownloadTask.TaskSnapshot>.SnapshotBase {

        protected Exception error;
        protected long bytesTransferred;
        protected long totalByteCount;
        private InputStream stream;

        protected TaskSnapshot(@NonNull StorageReference storageRef,
                               long bytesTransferred,
                               long totalByteCount) {
            super(storageRef);
            this.bytesTransferred = bytesTransferred;
            this.totalByteCount = totalByteCount;
            this.error = null;
        }

        /**
         * Gets the number of bytes transferred so far.
         *
         * @return Number of bytes transferred.
         */
        public long getBytesTransferred() {
            return this.bytesTransferred;
        }

        /**
         * Gets the total number of bytes to transfer.
         *
         * @return Total number of bytes.
         */
        public long getTotalByteCount() {
            return this.totalByteCount;
        }

        /**
         * Sets the total number of bytes to transfer.
         *
         * @param totalByteCount New total byte count.
         */
        protected void setTotalByteCount(long totalByteCount) {
            this.totalByteCount = totalByteCount;
        }

        /**
         * Gets the input stream containing the downloaded data.
         *
         * @return InputStream The input stream.
         */
        public InputStream getStream() {
            return this.stream;
        }

        /**
         * Sets the input stream.
         *
         * @param stream The input stream to set.
         */
        protected void setStream(InputStream stream) {
            this.stream = stream;
        }

    }
}
