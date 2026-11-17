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

package com.oracle.mobile.fusabase.oracledb.internal.request.header;

import androidx.annotation.RestrictTo;
import java.util.function.Supplier;
import java.util.Objects;
import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.internal.AuthInterop;
import com.oracle.mobile.fusabase.internal.AuthInteropRegistry;
import com.oracle.mobile.fusabase.internal.AuthSession;

/** Resolves credentials from the optional Auth module for this app, without preference fallback. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AuthInteropAuthorizationHeaderProvider implements AuthorizationHeaderProvider {
    private final AuthSession session;
    private final Supplier<AuthInterop> authProvider;
    public AuthInteropAuthorizationHeaderProvider(FusabaseApp app) {
        this(AuthInteropRegistry.session(app), () -> AuthInteropRegistry.get(app));
    }
    AuthInteropAuthorizationHeaderProvider(AuthSession session, Supplier<AuthInterop> authProvider) {
        this.session = session;
        this.authProvider = authProvider;
    }
    @Override public String getAuthorizationHeader() {
        AuthSession.State state = session.current();
        AuthInterop auth = authProvider.get();
        if (auth == null || state.userId == null
                || !Objects.equals(state.userId, auth.getCurrentUserId())) return "";
        String token = auth.getAccessToken();
        return session.isCurrent(state) && Objects.equals(state.userId, auth.getCurrentUserId())
                && token != null && !token.isEmpty()
                ? "Bearer " + token : "";
    }
}
