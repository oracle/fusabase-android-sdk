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

import android.net.Uri;

import androidx.annotation.Nullable;

/**
 * This class encapsulates the data required to update a user's profile.
 * It provides methods to access the display name and photo URI.
 */
public class UserProfileChangeRequest {

    private UserProfileChangeRequest (Builder builder) {
        this.displayName = builder.getDisplayName();
        this.photoUri = builder.getPhotoUri();
        this.phoneNumber = builder.getPhoneNumber();
    }

    /**
     * The display name of the user.
     */
    public @Nullable String displayName;
     /**
     * The URI of the user's profile photo.
     */
    public @Nullable String photoUri;

    /**
     * The phoneNumber of the user.
     */
    public @Nullable String phoneNumber;

    /**
     * Returns the display name of the user.
     *
     * @return The display name, or null if not set.
     */
    public @Nullable String getDisplayName() {
        return this.displayName;
    }

    /**
     * Returns the display name of the user.
     *
     * @return The display name, or null if not set.
     */
    public @Nullable String getPhoneNumber() {
        return this.phoneNumber;
    }

    /**
     * Returns the URI of the user's profile photo.
     *
     * @return The photo URI, or null if not set.
     */
    public @Nullable String getPhotoUri() {
        return this.photoUri;
    }

    /**
     * A builder class to construct a {@link UserProfileChangeRequest} object.
     */
    public static class Builder {
        private String displayName;
        private String photoUri;
        private String phoneNumber;

        /**
         * Sets the display name of the user.
         *
         * @param displayName The display name to set.
         * @return This builder instance.
         */
        public Builder setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the URI of the user's profile photo.
         *
         * @param photoUri The photo URI to set.
         * @return This builder instance.
         */
        public Builder setPhotoUri(String photoUri) {
            // Not Supported for Base/LDAP/SRP
            this.photoUri = photoUri;
            return this;
        }

        /**
         * Sets the user's phone Number.
         *
         * @param phoneNumber The phoneNumber to set.
         * @return This builder instance.
         */
        public Builder setPhoneNumber(String phoneNumber) {
            // Not Supported for Base/LDAP/SRP
            this.phoneNumber = phoneNumber;
            return this;
        }

        /**
         * Returns the display name set in this builder.
         *
         * @return The display name, or null if not set.
         */
        private String getDisplayName () {
            return this.displayName;
        }

        /**
         * Returns the photo URI set in this builder.
         *
         * @return The photo URI, or null if not set.
         */
        private String getPhotoUri () {
            return this.photoUri;
        }

        /**
         * Returns the photo URI set in this builder.
         *
         * @return The photo URI, or null if not set.
         */
        private String getPhoneNumber () {return this.phoneNumber;}

        /**
         * Builds a {@link UserProfileChangeRequest} object using the settings in this builder.
         *
         * @return A new {@link UserProfileChangeRequest} object.
         */
        public UserProfileChangeRequest build() {
            return new UserProfileChangeRequest(this); // Pass the builder itself to the User constructor
        }
    }
}
