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

package com.oracle.database.osbdt.core;

import static com.oracle.database.osbdt.core.OsbdtConstants.AUTH_TYPE_BASE;
import static com.oracle.database.osbdt.core.OsbdtConstants.AUTH_TYPE_IDCS;
import static com.oracle.database.osbdt.core.OsbdtConstants.AUTH_TYPE_LDAP;
import static com.oracle.database.osbdt.core.OsbdtConstants.CLIENT_ID;
import static com.oracle.database.osbdt.core.OsbdtConstants.CLIENT_SECRET;
import static com.oracle.database.osbdt.core.OsbdtConstants.ALLOW_SELF_SIGNED_CERTIFICATES_KEY;
import static com.oracle.database.osbdt.core.OsbdtConstants.DEFAULT_ALLOWS_SELF_SIGNED_CERTIFICATES;
import static com.oracle.database.osbdt.core.OsbdtConstants.DEFAULT_API_VERSION;
import static com.oracle.database.osbdt.core.OsbdtConstants.DEFAULT_ENABLE_LOGGING;
import static com.oracle.database.osbdt.core.OsbdtConstants.DEFAULT_LONG_POLLING_INTERVAL;
import static com.oracle.database.osbdt.core.OsbdtConstants.DEFAULT_UPLOAD_CHUNK_SIZE;
import static com.oracle.database.osbdt.core.OsbdtConstants.DEFAULT_USE_SOCKET;
import static com.oracle.database.osbdt.core.OsbdtConstants.DOMAIN_URL;
import static com.oracle.database.osbdt.core.OsbdtConstants.SELF_REGISTRATION_PROFILE;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.OsbdtOptions;
import com.oracle.database.osbdt.logger.OsbdtLogger;
import com.oracle.database.osbdt.models.IDCSOptions;
import com.oracle.database.osbdt.models.Options;
import com.oracle.database.osbdt.utils.Utils;

/**
 * Internal class for OsbdtApp core logic
 */
public class OsbdtAppCore {

    private static final String TAG = "OsbdtApp";

    @NonNull
    public static OsbdtOptions readConfigResources(@NonNull Context context) {

        Options.Builder optionsBuilder = new Options.Builder();
        try {
            optionsBuilder.setAuthType(Utils.getConfigValue(context, OsbdtConstants.AUTH_TYPE_KEY))
                    .setOrdsHost(Utils.getConfigValue(context, OsbdtConstants.ORDS_HOST_KEY))
                    .setStorageBucket(Utils.getConfigValue(context, OsbdtConstants.STORAGE_BUCKET_KEY))
                    .setObjectType(Utils.getConfigValue(context, OsbdtConstants.OBJS_TYPE_KEY))
                    .setSchema(Utils.getConfigValue(context, OsbdtConstants.SCHEMA_KEY))
                    .setAppName(Utils.getConfigValue(context, OsbdtConstants.APP_NAME_KEY));

            try {
                optionsBuilder.setApiVersion(Utils.getConfigValue(context, OsbdtConstants.API_VERSION_KEY));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setApiVersion(DEFAULT_API_VERSION);
            }

            try {
                optionsBuilder.setUseSocket(Boolean.parseBoolean(Utils.getConfigValue(context, OsbdtConstants.USE_SOCKET_KEY)));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setUseSocket(DEFAULT_USE_SOCKET);
            }

            try {
                optionsBuilder.setUploadChunkSize(Integer.parseInt(Utils.getConfigValue(context, OsbdtConstants.UPLOAD_CHUNK_SIZE_KEY)));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setUploadChunkSize(DEFAULT_UPLOAD_CHUNK_SIZE);
            }

            try {
                optionsBuilder.setLongPollingInterval(Integer.parseInt(Utils.getConfigValue(context, OsbdtConstants.LONG_POLLING_INTERVAL_KEY)));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setLongPollingInterval(DEFAULT_LONG_POLLING_INTERVAL);
            }

            try {
                optionsBuilder.setEnableLogging(Boolean.parseBoolean(Utils.getConfigValue(context, OsbdtConstants.ENABLE_LOGGING_KEY)));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setEnableLogging(DEFAULT_ENABLE_LOGGING);
            }

            try {
                optionsBuilder.setAllowsSelfSignedCertificates(Boolean.parseBoolean(Utils.getConfigValue(context, ALLOW_SELF_SIGNED_CERTIFICATES_KEY)));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setAllowsSelfSignedCertificates(DEFAULT_ALLOWS_SELF_SIGNED_CERTIFICATES);
            }

            optionsBuilder.setProjectId(Utils.getConfigValue(context, OsbdtConstants.PROJECT_ID_KEY))
                .setAuthId(Utils.getConfigValue(context, OsbdtConstants.AUTH_ID_KEY))
                .setAppId(Utils.getConfigValue(context, OsbdtConstants.APP_ID_KEY));

            switch (Utils.getConfigValue(context, OsbdtConstants.AUTH_TYPE_KEY)) {
                case AUTH_TYPE_BASE:
                case AUTH_TYPE_LDAP: {
                    break;
                }
                case AUTH_TYPE_IDCS: {
                    IDCSOptions idcsOptions = new IDCSOptions.Builder()
                        .setClientId(Utils.getConfigValue(context, CLIENT_ID))
                        .setClientSecret(Utils.getConfigValue(context,CLIENT_SECRET))
                        .setSelfRegistrationProfile(Utils.getConfigValue(context,SELF_REGISTRATION_PROFILE))
                        .setDomainURL(Utils.getConfigValue(context,DOMAIN_URL))
                                .build();
                        optionsBuilder.setIDCSOptions(idcsOptions);

                    OsbdtLogger.d("Auth_type idcs read from resources.");
                    break;
                }
                default:
                    OsbdtLogger.e("Invalid auth_type " + Utils.getConfigValue(context, OsbdtConstants.AUTH_TYPE_KEY) +
                            "encountered.");
                    throw new RuntimeException("Invalid auth_type detected." +
                            " Kindly check your osbdt-config.json for any errors.");
            }

        } catch (Resources.NotFoundException e) {
            OsbdtLogger.e(TAG, "Required configuration resource not found: " + e.getMessage());
            throw new IllegalStateException("Missing required configuration. Please check your osbdt_config.json file.", e);
        } catch (Exception e) {
            OsbdtLogger.e(TAG, "Error reading configuration: " + e.getMessage());
            throw new IllegalStateException("Failed to initialize OsbdtApp due to configuration error.", e);
        }

        return new OsbdtOptions(optionsBuilder.build());
    }
}
