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

package com.oracle.mobile.fusabase.core;

import static com.oracle.mobile.fusabase.core.FusabaseConstants.AUTH_TYPE_BASE;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.AUTH_TYPE_IDCS;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.AUTH_TYPE_LDAP;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.CLIENT_ID;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.CLIENT_SECRET;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.ALLOW_SELF_SIGNED_CERTIFICATES_KEY;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.DEFAULT_ALLOWS_SELF_SIGNED_CERTIFICATES;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.DEFAULT_API_VERSION;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.DEFAULT_ENABLE_LOGGING;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.DEFAULT_LONG_POLLING_INTERVAL;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.DEFAULT_UPLOAD_CHUNK_SIZE;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.DEFAULT_USE_SOCKET;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.DOMAIN_URL;
import static com.oracle.mobile.fusabase.core.FusabaseConstants.SELF_REGISTRATION_PROFILE;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;
import com.oracle.mobile.fusabase.models.IDCSOptions;
import com.oracle.mobile.fusabase.models.Options;
import com.oracle.mobile.fusabase.utils.Utils;

/**
 * Internal class for FusabaseApp core logic
 */
public class FusabaseAppCore {

    private static final String TAG = "FusabaseApp";

    @NonNull
    public static FusabaseOptions readConfigResources(@NonNull Context context) {

        Options.Builder optionsBuilder = new Options.Builder();
        try {
            optionsBuilder.setAuthType(Utils.getConfigValue(context, FusabaseConstants.AUTH_TYPE_KEY))
                    .setOrdsHost(Utils.getConfigValue(context, FusabaseConstants.ORDS_HOST_KEY))
                    .setStorageBucket(Utils.getConfigValue(context, FusabaseConstants.STORAGE_BUCKET_KEY))
                    .setObjectType(Utils.getConfigValue(context, FusabaseConstants.OBJS_TYPE_KEY))
                    .setSchema(Utils.getConfigValue(context, FusabaseConstants.SCHEMA_KEY))
                    .setAppName(Utils.getConfigValue(context, FusabaseConstants.APP_NAME_KEY));

            try {
                optionsBuilder.setApiVersion(Utils.getConfigValue(context, FusabaseConstants.API_VERSION_KEY));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setApiVersion(DEFAULT_API_VERSION);
            }

            try {
                optionsBuilder.setUseSocket(Boolean.parseBoolean(Utils.getConfigValue(context, FusabaseConstants.USE_SOCKET_KEY)));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setUseSocket(DEFAULT_USE_SOCKET);
            }

            try {
                optionsBuilder.setUploadChunkSize(Integer.parseInt(Utils.getConfigValue(context, FusabaseConstants.UPLOAD_CHUNK_SIZE_KEY)));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setUploadChunkSize(DEFAULT_UPLOAD_CHUNK_SIZE);
            }

            try {
                optionsBuilder.setLongPollingInterval(Integer.parseInt(Utils.getConfigValue(context, FusabaseConstants.LONG_POLLING_INTERVAL_KEY)));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setLongPollingInterval(DEFAULT_LONG_POLLING_INTERVAL);
            }

            try {
                optionsBuilder.setEnableLogging(Boolean.parseBoolean(Utils.getConfigValue(context, FusabaseConstants.ENABLE_LOGGING_KEY)));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setEnableLogging(DEFAULT_ENABLE_LOGGING);
            }

            try {
                optionsBuilder.setAllowsSelfSignedCertificates(Boolean.parseBoolean(Utils.getConfigValue(context, ALLOW_SELF_SIGNED_CERTIFICATES_KEY)));
            } catch (Resources.NotFoundException e) {
                optionsBuilder.setAllowsSelfSignedCertificates(DEFAULT_ALLOWS_SELF_SIGNED_CERTIFICATES);
            }

            optionsBuilder.setProjectId(Utils.getConfigValue(context, FusabaseConstants.PROJECT_ID_KEY))
                .setAuthId(Utils.getConfigValue(context, FusabaseConstants.AUTH_ID_KEY))
                .setAppId(Utils.getConfigValue(context, FusabaseConstants.APP_ID_KEY));

            switch (Utils.getConfigValue(context, FusabaseConstants.AUTH_TYPE_KEY)) {
                case AUTH_TYPE_BASE:
                case AUTH_TYPE_LDAP: {
                    break;
                }
                case AUTH_TYPE_IDCS: {
                    IDCSOptions idcsOptions = new IDCSOptions.Builder()
                        .setClientId(Utils.getConfigValue(context, CLIENT_ID))
                        .setClientSecret(Utils.getConfigValue(context,CLIENT_SECRET))
                        .setDomainURL(Utils.getConfigValue(context,DOMAIN_URL))
                                .build();
                        optionsBuilder.setIDCSOptions(idcsOptions);

                    FusabaseLogger.d("Auth_type idcs read from resources.");
                    break;
                }
                default:
                    FusabaseLogger.e("Invalid auth_type " + Utils.getConfigValue(context, FusabaseConstants.AUTH_TYPE_KEY) +
                            "encountered.");
                    throw new FusabaseException("Invalid authentication type configured. Please check your fusabase-config.json file.");
            }

        } catch (Resources.NotFoundException e) {
            FusabaseLogger.e(TAG, "Required configuration resource not found: " + e.getMessage());
            throw new IllegalStateException("Missing required configuration. Please check your fusabase_config.json file.", e);
        } catch (Exception e) {
            FusabaseLogger.e(TAG, "Error reading configuration: " + e.getMessage());
            throw new IllegalStateException("Failed to initialize FusabaseApp due to configuration error.", e);
        }

        return new FusabaseOptions(optionsBuilder.build());
    }
}
