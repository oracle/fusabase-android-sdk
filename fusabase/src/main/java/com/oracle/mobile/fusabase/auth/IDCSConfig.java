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

class IDCSConfig extends Config {

    protected static String SSO_PATH = "sso";
    protected static String V1_PATH = "v1";
    protected static String SDK_PATH = "sdk";
    protected static String AUTHENTICATE_PATH = "authenticate";
    protected static String OAUTH2_PATH = "oauth2";
    protected static String TOKEN_PATH = "token";
    protected static String ME_PATH = "Me";
    protected static String ADMIN_PATH = "admin";
    protected static String USER_LOGOUT_PATH = "userlogout";
    protected static String REVOKE_PATH = "revoke";
    protected static String ME_PASSWORD_RESET_REQUESTOR_PATH = "MePasswordResetRequestor";
    protected static String ME_PASSWORD_RESETTER_PATH = "MePasswordResetter";
    protected static String USER_TOKEN_VALIDATOR_PATH = "UserTokenValidator";
    protected static String ME_PASSWORD_CHANGER_PATH = "MePasswordChanger";
    protected static String ME_EMAIL_VERIFIER = "MeEmailVerifier";
    protected static String TOKEN_EXCHANGE_PATH = "tokenExchange";
    @NonNull
    public final String clientId;
    @NonNull
    public final String clientSecret;
    @NonNull
    public final String idcsDomainURL;
    IDCSConfig(@NonNull String appId,
               @NonNull String projectId,
               @NonNull String authType,
               @NonNull String clientSecret,
               @NonNull String clientId,
               @NonNull String domainURL,
               @NonNull String idcsDomainURL,
               @NonNull String unusedSelfRegistrationProfile) {
        super(authType, appId, projectId, domainURL);
        this.idcsDomainURL = idcsDomainURL;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @NonNull
    public String getIdcsDomainURL() {
        return this.idcsDomainURL;
    }
}
