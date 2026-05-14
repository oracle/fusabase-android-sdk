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

package com.oracle.mobile.fusabase.models;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Model class for IDCS-specific options.
 */
public class IDCSOptions {
    private final String clientId;
    private final String clientSecret;
    private final String domainURL;

    public IDCSOptions(Builder builder) {
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.domainURL = builder.domainURL;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, clientSecret, domainURL);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IDCSOptions)) {
            return false;
        }
        IDCSOptions that = (IDCSOptions) obj;
        return Objects.equals(clientId, that.clientId) &&
                Objects.equals(clientSecret, that.clientSecret) &&
                Objects.equals(domainURL, that.domainURL);
    }

    /**
     * Gets the client ID.
     *
     * @return The client ID.
     */
    @NonNull
    public String getClientId() {
        return this.clientId;
    }

    /**
     * Gets the client secret.
     *
     * @return The client secret.
     */
    @NonNull
    public String getClientSecret() {
        return this.clientSecret;
    }

    /**
     * Gets the IDCS Domain URL.
     *
     * @return The IDCS domain URL.
     */
    @NonNull
    public String getDomainURL() {
        return this.domainURL;
    }

    /**
     * Builder for creating new instances of IDCSOptions.
     */
    public static class Builder {
        private String clientId;
        private String clientSecret;
        private String domainURL;

        /**
         * Sets the client ID.
         *
         * @param clientId The client ID.
         * @return This builder instance.
         */
        public Builder setClientId(@NonNull String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * Sets the client secret.
         *
         * @param clientSecret The client secret.
         * @return This builder instance.
         */
        public Builder setClientSecret(@NonNull String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        /**
         * Sets the IDCS domain URL.
         *
         * @param domainURL The IDCS domain url.
         * @return This builder instance.
         */
        public Builder setDomainURL(@NonNull String domainURL) {
            this.domainURL = domainURL;
            return this;
        }

        /**
         * Builds the IDCSOptions object based on the properties set in this builder.
         *
         * @return The constructed IDCSOptions object.
         */
        public IDCSOptions build() {
            return new IDCSOptions(this);
        }
    }
}
