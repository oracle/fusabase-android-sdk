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

/**
 * Represents the source of data for a query operation.
 * Specifies whether to use cached data or force server retrieval.
 */
public enum Source {
    /**
     * Causes Cloud Oradb to try to retrieve an up-to-date (server-retrieved) snapshot, but fall
     * back to returning cached data if the server can't be reached.
     */
    DEFAULT,

    /**
     * Causes Cloud Oradb to avoid the cache, generating an error if the server cannot be reached.
     * Note that the cache will still be updated if the server request succeeds. Also note that
     * latency-compensation still takes effect, so any pending write operations will be visible in the
     * returned data (merged into the server-provided data).
     */
    SERVER
}
