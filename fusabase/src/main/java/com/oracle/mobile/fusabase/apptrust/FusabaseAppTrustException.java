// Copyright (c) 2015, 2025, Oracle and/or its affiliates.
package com.oracle.mobile.fusabase.apptrust;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseException;

/** Exception raised by Fusabase App Trust operations. */
public class FusabaseAppTrustException extends FusabaseException {
    public FusabaseAppTrustException(@NonNull String message) {
        super(message);
    }

    public FusabaseAppTrustException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }
}
