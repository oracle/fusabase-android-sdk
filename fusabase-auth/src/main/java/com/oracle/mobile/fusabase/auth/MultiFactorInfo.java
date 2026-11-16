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
import androidx.annotation.Nullable;

/**
 * Represents a single enrolled multi-factor authentication factor.
 *
 * <p>Use the concrete phone or TOTP subtype for factor-specific information.
 *
 * <p>Example:</p>
 * <pre>{@code
 * // Given a signed-in user:
 * user.getMultiFactor().getEnrolledFactors().addOnSuccessListener(factors -> {
 *     for (MultiFactorInfo factor : factors) {
 *         String factorId = factor.getFactorId();
 *     }
 * });
 * }</pre>
 */
public abstract class MultiFactorInfo {
    private final String uid;
    private final String factorId;
    private final String displayName;

    /**
     *  Creates an empty MFA factor for subclasses.
     *
     * <p>Example: Applications normally obtain an SDK implementation as shown below instead of subclassing this base type.</p>
     * <pre>{@code
     * // Given a signed-in user:
     * user.getMultiFactor().getEnrolledFactors().addOnSuccessListener(factors -> {
     *     for (MultiFactorInfo factor : factors) {
     *         String factorId = factor.getFactorId();
     *     }
     * });
     * }</pre>
     */
    public MultiFactorInfo() {
        this("", "", null);
    }

    protected MultiFactorInfo(@NonNull String uid,
                              @NonNull String factorId,
                              @Nullable String displayName) {
        this.uid = uid;
        this.factorId = factorId;
        this.displayName = displayName;
    }

    /**
     * Returns the unique identifier for this enrolled factor.
     *
     * <p>Example: Given a MultiFactorInfo instance named factor.</p>
     * <pre>{@code
     * String uid = factor.getUid();
     * }</pre>
     *
     * @return the factor UID
     */
    public @NonNull String getUid() {
        return uid;
    }

    /**
     * Returns the type of this factor.
     *
     * <p>Example: Given a MultiFactorInfo instance named factor.</p>
     * <pre>{@code
     * String factorId = factor.getFactorId();
     * }</pre>
     *
     * @return the factor identifier, such as {@code phone} or {@code totp}
     */
    public @NonNull String getFactorId() {
        return factorId;
    }

    /**
     * Returns the user-provided display name for this factor.
     *
     * <p>Example: Given a MultiFactorInfo instance named factor.</p>
     * <pre>{@code
     * String displayName = factor.getDisplayName();
     * }</pre>
     *
     * @return the display name, or {@code null} when one was not provided
     */
    public @Nullable String getDisplayName() {
        return displayName;
    }

}
