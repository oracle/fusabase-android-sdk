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

/**
 * Abstract base class for federated authentication providers.
 *
 * This class defines the common interface that all federated authentication
 * providers must implement, such as OAuth, SAML, and social login providers.
 * Subclasses provide specific implementations for different authentication
 * mechanisms and identity providers.
 */
public abstract class FederatedAuthProvider {

    /**
     * Gets the authentication URL for initiating the federated login flow.
     *
     * @return The authentication URL as a non-null string.
     */
    @NonNull abstract String getAuthUrl();

    /**
     * Gets the unique identifier for this authentication provider.
     *
     * @return The provider ID as a non-null string.
     */
    @NonNull abstract String getProviderId();

    /**
     * Default constructor for FederatedAuthProvider.
     */
    public FederatedAuthProvider() {

    }
}
