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

package com.oracle.mobile.fusabase.http;

import java.io.InputStream;

import okhttp3.ResponseBody;

public class HttpResponse {

    private final boolean status;
    private String response;
    private final byte[] responseBytes;
    private final String error;
    private final int code;
    private final ResponseBody responseBody;

    public HttpResponse(boolean status, ResponseBody responseBody, byte[] responseBytes, String error, int code) {
        this.status = status;
        this.responseBody = responseBody;
        this.responseBytes = responseBytes;
        this.error = error;
        this.code = code;
        this.response = null; // Lazy loaded
    }

    public boolean getStatus() {
        return status;
    }

    public byte[] getResponseBytes ()
    {
        return this.responseBytes;
    }

    public String getResponse() {
        if (response == null && responseBody != null) {
            try {
                response = responseBody.string();
            } catch (Exception e) {
                // If string conversion fails, return null or empty
                response = "";
            }
        }
        return response;
    }

    public InputStream getInputStream() {
        return responseBody != null ? responseBody.byteStream() : null;
    }

    public String getError() {
        return error;
    }

    public int getCode() {return code;}
}
