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

class Config {

    protected static String AUTHENTICATE_REST_EP = "authenticate";
    protected static String SELF_REGISTER_EP = "useradd";
    protected static String UPDATE_PASSWORD_HELPER = "changePassword";
    protected static String UPDATE_PROFILE_HELPER = "updateProfile";
    protected static String REVOKE_REFRESH_TOKEN = "rf/revoke";
    protected static String SEND_EMAIL_VERIFICATION = "sendemail";
    protected static String SEND_PASSWORD_RESET_EMAIL = "sendemail";
    protected static String CONFIRM_PASSWORD_RESET = "resetpwd";
    protected static String VERIFY_PASSWORD_RESET_CODE = "verifycode";
    protected static String SIGN_IN_WITH_CREDENTIAL = "getcredential";
    protected static String GET_REDIRECT_RESULT = "redirectResult";
    protected static String IDM_PATH = "idm";
    protected static String ON_PREM_PATH = "onprem";
    protected static String BAAS_SERVICES_PATH = "baas-services";
    protected static String UNDERSCORE_PATH = "_";
    protected static String IDCS_AUTH_TYPE = "idcs";
    protected static String ONPREM_AUTH_TYPE = "base";
    protected static String LDAP_AUTH_TYPE = "ldap";

    @NonNull
    protected final String authType;
    @NonNull
    protected final String appId;
    @NonNull
    protected final String projectId;
    @NonNull
    protected final String domainURL;

    protected Config(@NonNull String authType,
                     @NonNull String appId,
                     @NonNull String projectId,
                     @NonNull String domainURL) {
        this.authType = authType;
        this.projectId = projectId;
        this.appId = appId;
        this.domainURL = domainURL;
    }

    @NonNull
    protected String getAuthType() {
        return authType;
    }

    @NonNull
    protected String getAppId() {
        return appId;
    }

    @NonNull
    protected String getProjectId() {
        return projectId;
    }

    @NonNull
    protected String getDomainURL() {
        return domainURL;
    }
}
