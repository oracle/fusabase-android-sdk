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

import com.oracle.mobile.fusabase.task.Task;

import java.util.List;

/**
 * Manages multi-factor authentication state and provides methods for enrolling and unenrolling
 * second factors for an authenticated user.
 *
 * <p>Obtain an instance from {@link FusabaseUser#getMultiFactor()}; applications do not create
 * implementations directly.
 */
public abstract class MultiFactor {
    /**
     * Initializes a multi-factor authentication manager implementation.
     *
     * <p>This constructor is invoked by subclasses. Applications obtain an implementation from
     * {@link FusabaseUser#getMultiFactor()}.
     */
    public MultiFactor() { }

    /**
     * Returns a task that enrolls a second factor for the current user.
     *
     * <p>Create the assertion with the appropriate phone or TOTP factor generator.
     *
     * @param assertion the verified assertion that represents the factor to enroll
     * @param displayName an optional user-visible name for the enrolled factor
     * @return task that enrolls the second factor
     */
    public abstract @NonNull Task<Void> enroll(@NonNull MultiFactorAssertion assertion,
                                               @Nullable String displayName);

    /**
     * Retrieves the multi-factor authentication factors enrolled for the current user.
     *
     * <p>This fetches the factors from the authentication server and updates the user's
     * locally cached factor list. The returned list is empty when no second factors are enrolled.
     *
     * @return a task that completes with the user's enrolled multi-factor authentication factors
     */
    public abstract @NonNull Task<List<MultiFactorInfo>> getEnrolledFactors();

    /**
     * Returns a task that creates a session for enrolling a new second factor.
     *
     * <p>Use the returned session when creating phone enrollment options or generating a TOTP
     * secret.
     *
     * @return task that retrieves the MFA enrollment session
     */
    public abstract @NonNull Task<MultiFactorSession> getSession();

    /**
     * Returns a task that unenrolls the second factor with the supplied unique identifier.
     *
     * <p>Obtain a factor UID from the list returned by {@link #getEnrolledFactors()}.
     *
     * @param factorUid the unique identifier of the factor to remove
     * @return task that unenrolls the second factor
     */
    public abstract @NonNull Task<Void> unenroll(@NonNull String factorUid);

    /**
     * Returns a task that unenrolls the supplied second factor.
     *
     * @param factor the enrolled factor to remove
     * @return task that unenrolls the second factor
     */
    public abstract @NonNull Task<Void> unenroll(@NonNull MultiFactorInfo factor);
}
