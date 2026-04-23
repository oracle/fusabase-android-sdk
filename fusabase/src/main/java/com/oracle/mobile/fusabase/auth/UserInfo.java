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
 */
public interface UserInfo {

    /**
     * Returns the display name of the user.
     *
     * @return the display name, or null if not available
     */
    public abstract @Nullable String getDisplayName();

    /**
     * Returns the email address associated with the user.
     *
     * @return the email address, or null if not available
     */
    public abstract @Nullable String getEmail();

    /**
     * Returns the URL of the user's photo.
     *
     * @return the photo URL, or null if not available
     */
    public abstract @Nullable String getPhotoUrl();

    /**
     * Returns the provider ID of the user.
     *
     * @return the provider ID, never null
     */
    public abstract @NonNull String getProviderId();

    /**
     * Returns the unique ID of the user.
     *
     * @return the user ID, never null
     */
    public abstract @NonNull String getUid();

    /**
     * Checks if the user's email address has been verified.
     *
     * @return true if the email address is verified, false otherwise
     */
    public abstract boolean isEmailVerified();
}