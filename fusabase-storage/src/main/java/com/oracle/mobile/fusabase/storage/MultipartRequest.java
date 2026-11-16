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

import static java.lang.Math.floor;

import androidx.annotation.NonNull;

import java.util.Arrays;

class MultipartRequest {

    private final byte[] bytes;
    private int currIndex;
    private final int chunkSize;
    private final int totalBytes;
    private int bytesTransferred;
    private final String accessURL;
    private final String mimeType;

    MultipartRequest(@NonNull String accessURL, @NonNull String mimeType, @NonNull byte[] bytes, int chunkSize) {
        this.bytes = bytes;
        this.currIndex = 1;
        this.chunkSize = chunkSize;
        this.totalBytes = bytes.length;
        this.accessURL = accessURL;
        this.bytesTransferred = 0;
        this.mimeType = mimeType;
    }

    @NonNull
    protected String getMimeType() {
        return this.mimeType;
    }

    protected int getBytesTransferred() {
        return this.bytesTransferred;
    }

    protected int getTotalBytes() {
        return this.totalBytes;
    }

    protected byte[] getNextChunk() {
        int end = Math.min(bytesTransferred + chunkSize, totalBytes);
        return Arrays.copyOfRange(bytes, bytesTransferred, end);
    }

    protected int getTotalNumberOfChunks() {
        return (int) floor((totalBytes + chunkSize - 1.0) / chunkSize);
    }

    protected int getCurrIndex() {
        return this.currIndex;
    }

    protected void updateRequestForNextChunk() {
        int actualChunkSize = Math.min(this.chunkSize, this.totalBytes - this.bytesTransferred);
        this.bytesTransferred += actualChunkSize;
        this.currIndex++;
    }

    protected void updateRequestForRetry() {
        this.bytesTransferred = this.bytesTransferred > this.chunkSize ? this.bytesTransferred - this.chunkSize : 0;
        this.currIndex = 0;
    }

    protected void updateRequestForPreviousChunk() {
        this.bytesTransferred = this.bytesTransferred > this.chunkSize ? this.bytesTransferred - this.chunkSize : 0;
        this.currIndex = this.currIndex == 0 ? this.currIndex : this.currIndex - 1;
    }

    protected String getAccessURL() {
        return this.accessURL;
    }
}
