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

package com.oracle.mobile.fusabase.auth;

import androidx.annotation.NonNull;

class IDCSConfig extends Config {

    protected static String OAUTH2_PATH = "oauth2";
    protected static String V1_PATH = "v1";
    protected static String USER_LOGOUT_PATH = "userlogout";
    protected static String AUTHORIZE_SNAPSHOT_PATH = "authorizeSnapshot";

    @NonNull
    public final String idcsDomainURL;

    IDCSConfig(@NonNull String appId,
               @NonNull String projectId,
               @NonNull String authType,
               @NonNull String domainURL,
               @NonNull String idcsDomainURL) {
        super(authType, appId, projectId, domainURL);
        this.idcsDomainURL = normalizeIdcsDomainURL(idcsDomainURL);
    }

    @NonNull
    public String getIdcsDomainURL() {
        return this.idcsDomainURL;
    }

    @NonNull
    private static String normalizeIdcsDomainURL(@NonNull String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
