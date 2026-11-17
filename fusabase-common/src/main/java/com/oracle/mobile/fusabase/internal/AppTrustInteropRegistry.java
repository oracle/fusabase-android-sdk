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


package com.oracle.mobile.fusabase.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.oracle.mobile.fusabase.FusabaseApp;

import java.util.concurrent.ConcurrentHashMap;

/** Internal registry for optional, app-scoped integrations contributed by SDK artifacts. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppTrustInteropRegistry {
    private static final ConcurrentHashMap<String, AppTrustInterop> APP_TRUST_BY_APP_NAME =
            new ConcurrentHashMap<>();

    private AppTrustInteropRegistry() {
    }

    public static void register(@NonNull FusabaseApp app, @NonNull AppTrustInterop appTrust) {
        APP_TRUST_BY_APP_NAME.put(app.getName(), appTrust);
    }

    @Nullable
    public static AppTrustInterop get(@NonNull FusabaseApp app) {
        return APP_TRUST_BY_APP_NAME.get(app.getName());
    }
}
