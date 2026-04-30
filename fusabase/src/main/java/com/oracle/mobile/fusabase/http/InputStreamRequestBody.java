// Copyright (c) 2015, 2025, Oracle and/or its affiliates.
//
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

package com.oracle.mobile.fusabase.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * OkHttp {@link RequestBody} that streams bytes directly from an {@link InputStream}.
 * This avoids buffering the entire payload in memory.
 */
public final class InputStreamRequestBody extends RequestBody {

    private final MediaType contentType;
    private final InputStream inputStream;
    private final long contentLength;

    /**
     * @param contentType media type
     * @param inputStream stream to read from (will be closed by this RequestBody)
     * @param contentLength length in bytes if known, else -1
     */
    public InputStreamRequestBody(@NonNull MediaType contentType,
                                  @NonNull InputStream inputStream,
                                  long contentLength) {
        this.contentType = contentType;
        this.inputStream = inputStream;
        this.contentLength = contentLength;
    }

    @Override
    public @Nullable MediaType contentType() {
        return contentType;
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        try (InputStream in = inputStream) {
            while ((read = in.read(buffer)) != -1) {
                sink.write(buffer, 0, read);
            }
        }
    }
}
