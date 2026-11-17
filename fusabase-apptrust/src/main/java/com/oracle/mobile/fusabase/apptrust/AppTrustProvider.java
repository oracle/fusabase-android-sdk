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


package com.oracle.mobile.fusabase.apptrust;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.task.Task;

/** A provider that generates App Trust tokens for a Fusabase application. */
public interface AppTrustProvider {
    /**
     * Obtains a valid App Trust token.
     *
     * @return a task that completes with a valid App Trust token or fails when one cannot be
     *         obtained
     */
    @NonNull
    Task<AppTrustToken> getToken();

    /**
     * Obtains a token for a limited-use endpoint.
     *
     * <p>Providers that do not support backend replay protection use a regular token by default.
     * A provider that supports limited-use tokens should override this method.</p>
     *
     * @return a task that completes with a limited-use App Trust token or fails when one cannot
     *         be obtained
     */
    @NonNull
    default Task<AppTrustToken> getLimitedUseToken() {
        return getToken();
    }
}
