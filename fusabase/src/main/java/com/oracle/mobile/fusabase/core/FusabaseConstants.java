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

package com.oracle.mobile.fusabase.core;
public final class FusabaseConstants {

    // Configuration keys
    public static final String AUTH_TYPE_KEY = "fusabase_auth_type";
    public static final String ORDS_HOST_KEY = "fusabase_ords_host";
    public static final String STORAGE_BUCKET_KEY = "fusabase_storage_bucket";
    public static final String OBJS_TYPE_KEY = "fusabase_objs_type";
    public static final String SCHEMA_KEY = "fusabase_schema";
    public static final String APP_NAME_KEY = "fusabase_app_name";
    public static final String API_VERSION_KEY = "fusabase_api_version";
    public static final String USE_SOCKET_KEY = "fusabase_useSocket";
    public static final String PROJECT_ID_KEY = "fusabase_project_id";
    public static final String AUTH_ID_KEY = "fusabase_auth_id";
    public static final String APP_ID_KEY = "fusabase_app_id";
    public static final String UPLOAD_CHUNK_SIZE_KEY = "fusabase_upload_chunk_size";
    public static final String LONG_POLLING_INTERVAL_KEY = "fusabase_long_polling_interval";
    public static final String ENABLE_LOGGING_KEY = "fusabase_enableLogging";

    // Default values
    public static final int DEFAULT_UPLOAD_CHUNK_SIZE = 1 * 1024 * 1024; // 1MB
    public static final long DEFAULT_LONG_POLLING_INTERVAL = 29; // 29 seconds
    public static final boolean DEFAULT_USE_SOCKET = false;
    public static final boolean DEFAULT_ENABLE_LOGGING = false;
    public static final String DEFAULT_API_VERSION = "2.0";

    // IDCS configuration keys.
    public static final String IDCS_DOMAIN_URL = "fusabase_idcs_domain_url";

    // Authentication TYPES
    public static final String AUTH_TYPE_BASE = "base";
    public static final String AUTH_TYPE_IDCS = "idcs";
    public static final String AUTH_TYPE_LDAP = "ldap";

}
