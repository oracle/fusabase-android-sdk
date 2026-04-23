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

package com.oracle.mobile.fusabase;

import androidx.annotation.NonNull;

/**
 * Represents an exception thrown when the application exceeds the rate limit for requests to Fusabase services.
 * <p>
 * This exception is thrown when too many requests are sent to Fusabase within a short time period,
 * typically due to rate limiting imposed by the server to prevent abuse and ensure fair usage.
 * </p>
 */
public class FusabaseTooManyRequestsException extends FusabaseException{

    /**
     * Constructs a new FusabaseTooManyRequestsException with the specified detail message.
     *
     * @param detailMessage The detail message explaining the rate limit violation.
     */
    public FusabaseTooManyRequestsException(@NonNull String detailMessage) {
        super(detailMessage);
    }
}
