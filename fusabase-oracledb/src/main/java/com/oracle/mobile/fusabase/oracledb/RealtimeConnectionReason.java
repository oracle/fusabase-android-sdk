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
 *  Sanitized reason associated with a realtime connection state transition. 
 *
 * <p>Example:</p>
 * <pre>{@code
 * RealtimeConnectionReason reason = RealtimeConnectionReason.NONE;
 * }</pre>
 */
public enum RealtimeConnectionReason {
    /** The authenticated session ended or changed; register new listeners after sign-in. */
    AUTH_CHANGED,

    /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * RealtimeConnectionReason reason = RealtimeConnectionReason.NONE;
     * }</pre>
     */NETWORK_UNAVAILABLE, /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * RealtimeConnectionReason reason = RealtimeConnectionReason.NONE;
     * }</pre>
     */TOKEN_FAILURE, /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * RealtimeConnectionReason reason = RealtimeConnectionReason.NONE;
     * }</pre>
     */TRANSPORT_FAILURE, /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * RealtimeConnectionReason reason = RealtimeConnectionReason.NONE;
     * }</pre>
     */CLOSED, /**
     * Uses this Oracle Database API.
     *
     * <p>Example:</p>
     * <pre>{@code
     * RealtimeConnectionReason reason = RealtimeConnectionReason.NONE;
     * }</pre>
     */NONE
}
