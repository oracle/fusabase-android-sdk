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

package com.oracle.database.osbdt;

import android.content.Context;

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.core.OsbdtAppCore;
import com.oracle.database.osbdt.logger.OsbdtLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an entry point to the BAAS SDKs.
 * <p>
 * In most cases, you don't have to worry about setting up Osbdt manually. Thanks to OsbdtInitProvider,
 * Osbdt is automatically set up for your app based on the settings in your osbdt_config.json file.
 * This setup happens behind the scenes when your app launches, thanks to Gradle merging everything
 * into the manifest. No extra code required — it just works.
 * </p> <p>
 * However, if your app needs to communicate with multiple Osbdt projects, you can use the
 * {@link #initializeApp(Context)} method to establish a connection to an additional project.
 * You must provide a unique name for the project, which can later be used to retrieve the
 * corresponding OsbdtApp instance via {@link #getInstance(String)}.
 * </p> <p>
 * Note that the default {@link #getInstance()} method always returns the OsbdtApp instance
 * associated with your primary project.
 * </p> <p>
 * Important: All Osbdt initialization must occur in the main app process. Initializing Osbdt
 * in background or secondary processes is not supported and may cause resource conflicts or
 * unexpected behavior.
 */
public class OsbdtApp {

    /**
     * A map to hold all initialized OsbdtApp instances, keyed by their names.
     */
    private static final Map<String, OsbdtApp> APP_INSTANCES = new HashMap<>();

    /**
     * Tag used for logging purposes.
     */
    private static final String TAG = "OsbdtApp";

    /**
     * The default name for the primary OsbdtApp instance.
     */
    public static final String DEFAULT_APP_NAME = "[DEFAULT]";

    /**
     * Global flag indicating whether self-signed certificates are allowed.
     * This is set during app initialization and used by HttpRequestHelper.
     */
    private static volatile boolean globalAllowsSelfSignedCertificates = false;

    /**
     * The configuration options for this OsbdtApp instance.
     */
    public final OsbdtOptions osbdtOptions;

    /**
     * The unique name of this OsbdtApp instance.
     */
    public final String name;

    /**
     * The Android context associated with this OsbdtApp instance.
     */
    public final Context context;

    /**
     * Private constructor to create a new OsbdtApp instance.
     * This constructor is used internally by the factory methods to initialize an OsbdtApp with
     * the provided context, options, and name.
     *
     * @param context The Android context for the application.
     * @param osbdtOptions The configuration options for the app.
     * @param name The unique name for this app instance.
     */
    private OsbdtApp(@NonNull Context context,
                     @NonNull OsbdtOptions osbdtOptions,
                     @NonNull String name) {
        this.osbdtOptions = osbdtOptions;
        this.name = name;
        this.context = context;
    }


    /**
     * Method to get the context with which this instance of OsbdtApp was initialized
     *
     * @return {@code Context}
     */
    public Context getApplicationContext() {
        return this.context;
    }

    /**
     * Initializes the default OsbdtApp instance using configuration values retrieved from the app's string resources,
     * which are automatically generated from the osbdt_config.json file.
     * <p>
     * Typically, this setup occurs automatically at app launch via OsbdtInitProvider. However, if you're developing
     * a component that runs outside the main process, you must manually invoke this method before utilizing any Osbdt features.
     * <p>
     * The configuration details (OsbdtOptions) for the default app are derived from the auto-generated string resources.
     *
     * @param context The Android context for the OsbdtApp instance.
     * @return The initialized OsbdtApp instance.
     */
    public static OsbdtApp initializeApp(@NonNull Context context) {
        OsbdtOptions options = OsbdtAppCore.readConfigResources(context);
        OsbdtLogger.d(TAG, "Osbdt Options created from config file " + options);
        return initializeApp(context, options);
    }

    /**
     * Initializes the default OsbdtApp instance using the provided OsbdtOptions, equivalent to calling 
     * {@link #initializeApp(Context, OsbdtOptions, String)} with the {@link #DEFAULT_APP_NAME}.
     * 
     * Typically, manual invocation of this method is unnecessary, as OsbdtInitProvider handles initialization 
     * automatically during app startup. However, in certain exceptional cases where automated setup is not feasible, 
     * you may need to call this method explicitly.
     * 
     * @param context The Android context for the OsbdtApp instance.
     * @param options The OsbdtOptions used to configure the default app.
     * @return The initialized {@code OsbdtApp} instance.
     */
    public static @NonNull OsbdtApp initializeApp(@NonNull Context context,
                                                  @NonNull OsbdtOptions options) {
        // Set logging enabled based on options
        OsbdtLogger.ENABLED = options.isEnableLogging();

        return initializeApp(context, options, DEFAULT_APP_NAME);
    }

    /**
     * Method to initialize the osbdt app with the provider context, options and appName. This
     * is a factory method to initialize Osbdt App.
     *
     * @param context {@code Context} for the osbdtApp
     * @param options {@code OsbdtOptions} for initializing the app
     * @param name    {@code String} Name of the osbdt app
     * @return {@code OsbdtApp} Initialized osbdtApp
     */
    public static @NonNull OsbdtApp initializeApp(
            @NonNull Context context,
            @NonNull OsbdtOptions options,
            @NonNull String name
    ) {
        if (!APP_INSTANCES.containsKey(name)) {
            // Set global SSL configuration for self-signed certificates
            globalAllowsSelfSignedCertificates = options.allowsSelfSignedCertificates();

            APP_INSTANCES.put(name, new OsbdtApp(context, options, name));
            OsbdtLogger.i(TAG, "Initialized OsbdtApp instance with name " + name);
            if (options.allowsSelfSignedCertificates()) {
                OsbdtLogger.w(TAG, "WARNING: Self-signed certificates are allowed for testing. " +
                        "This reduces security and should never be used in production.");
            }
            return Objects.requireNonNull(APP_INSTANCES.get(name));
        }
        OsbdtLogger.e("An app with the same name is already initialized.");
        throw new IllegalStateException("An app with the same name is already initialized." +
                "Please make sure that the all the Osbdt instance has unique name.");

    }

    /**
     * Method to get all the initialized osbdt app
     *
     * @param context {@code Context} of the application
     * @return {@code List<OsbdtApp>}
     */
    @NonNull
    public static List<OsbdtApp> getApps(@NonNull Context context) {
        List<OsbdtApp> apps = new ArrayList<>();
        APP_INSTANCES.forEach((name, app) -> apps.add(app));
        return apps;
    }

    /**
     * Method to get the default instance of the osbdt app.
     *
     * @return {@code OsbdtApp}
     * @throws {@code java.lang.IllegalStateException} If default osbdt app is not initialized
     */
    @NonNull
    public static OsbdtApp getInstance() {
        OsbdtApp defaultApp = APP_INSTANCES.get(DEFAULT_APP_NAME);
        if (defaultApp == null) {
            {
                OsbdtLogger.e("Default app hasn't been initialized yet.");
                throw new java.lang.IllegalStateException("Default app has not been initialized " +
                        "yet. Initialize it first with initializeApp().");
            }
        }
        OsbdtLogger.v(TAG, "Default app instance returned.");
        return defaultApp;
    }

    /**
     * Method to get the unique named instance of the osbdt app.
     *
     * @param name The name of the OsbdtApp instance to retrieve.
     * @return {@code OsbdtApp} The requested OsbdtApp instance.
     * @throws {@code java.lang.IllegalStateException} If named osbdt app is not initialized
     */
    @NonNull
    public static OsbdtApp getInstance(@NonNull String name) {
        OsbdtApp namedApp = APP_INSTANCES.get(name);
        if (namedApp == null) {
            OsbdtLogger.e(name + " app hasn't been initialized yet.");
            throw new java.lang.IllegalStateException(name + " app has not been initialized" +
                    "yet. Initialize it first with initializeApp(Context, OsbdtOptions, String).");
        }
        OsbdtLogger.v(TAG, name + " app instance returned.");
        return namedApp;
    }

    /**
     * Method to get the OsbdtOptions for this app
     *
     * @return {@code OsbdtOptions}
     */
    @NonNull
    public OsbdtOptions getOptions() {
        return osbdtOptions;
    }

    /**
     * Method to get the name of the app
     *
     * @return {@code String} Name of the app
     */
    @NonNull
    public String getName() {
        return this.name;
    }

    /**
     * Gets whether self-signed certificates are globally allowed for testing purposes.
     * <p>
     * <strong>WARNING:</strong> This should only return true for testing and development.
     * Allowing self-signed certificates reduces security and should never be used in production.
     * </p>
     *
     * @return true if self-signed certificates are allowed, false for secure production use
     */
    public static boolean allowsSelfSignedCertificates() {
        return globalAllowsSelfSignedCertificates;
    }

    /**
     * Returns the hash code of this instance.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(osbdtOptions, context, name);
    }

    /**
     * Checks if the provided object is equal to this OsbdtApp instance.
     * Two OsbdtApp instances are considered equal if they have the same context, name, and options.
     *
     * @param obj The object to compare with this instance.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OsbdtApp)) {
            return false;
        }
        OsbdtApp that = (OsbdtApp) obj;
        return Objects.equals(this.context, that.context) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.osbdtOptions, that.osbdtOptions);
    }
}
