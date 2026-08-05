// Copyright (c) 2015, 2026, Oracle and/or its affiliates.

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
