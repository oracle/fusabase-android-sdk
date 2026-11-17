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

import androidx.annotation.*;
/**
 * Provides access to a user's basic profile information.
 *
 * <p>Example:</p>
 * <pre>{@code
 * FusabaseAuth auth = FusabaseAuth.getInstance();
 * FusabaseUser user = auth.getCurrentUser();
 * if (user != null) {
 *     UserInfo info = user;
 *     String uid = info.getUid();
 * }
 * }</pre>
 */
public interface UserInfo {

    /**
     * Returns the display name of the user.
     *
     * <p>Example: Given a UserInfo instance named userInfo.</p>
     * <pre>{@code
     * String displayName = userInfo.getDisplayName();
     * }</pre>
     *
     * @return the display name, or null if not available
     */
    public abstract @Nullable String getDisplayName();

    /**
     * Returns the email address associated with the user.
     *
     * <p>Example: Given a UserInfo instance named userInfo.</p>
     * <pre>{@code
     * String email = userInfo.getEmail();
     * }</pre>
     *
     * @return the email address, or null if not available
     */
    public abstract @Nullable String getEmail();

    /**
     * Returns the URL of the user's photo.
     *
     * <p>Example: Given a UserInfo instance named userInfo.</p>
     * <pre>{@code
     * String photoUrl = userInfo.getPhotoUrl();
     * }</pre>
     *
     * @return the photo URL, or null if not available
     */
    public abstract @Nullable String getPhotoUrl();

    /**
     * Returns the provider ID of the user.
     *
     * <p>Example: Given a UserInfo instance named userInfo.</p>
     * <pre>{@code
     * String providerId = userInfo.getProviderId();
     * }</pre>
     *
     * @return the provider ID, never null
     */
    public abstract @NonNull String getProviderId();

    /**
     * Returns the unique ID of the user.
     *
     * <p>Example: Given a UserInfo instance named userInfo.</p>
     * <pre>{@code
     * String uid = userInfo.getUid();
     * }</pre>
     *
     * @return the user ID, never null
     */
    public abstract @NonNull String getUid();

    /**
     * Checks if the user's email address has been verified.
     *
     * <p>Example: Given a UserInfo instance named userInfo.</p>
     * <pre>{@code
     * boolean isEmailVerified = userInfo.isEmailVerified();
     * }</pre>
     *
     * @return true if the email address is verified, false otherwise
     */
    public abstract boolean isEmailVerified();
}