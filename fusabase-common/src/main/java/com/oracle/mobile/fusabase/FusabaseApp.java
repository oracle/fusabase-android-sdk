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

import android.content.Context;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.core.FusabaseAppCore;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an entry point to the BAAS SDKs.
 * <p>
 * In most cases, you don't have to worry about setting up Fusabase manually. Thanks to FusabaseInitProvider,
 * Fusabase is automatically set up for your app based on the settings in your fusabase_config.json file.
 * This setup happens behind the scenes when your app launches, thanks to Gradle merging everything
 * into the manifest. No extra code required — it just works.
 * </p> <p>
 * However, if your app needs to communicate with multiple Fusabase projects, you can use the
 * {@link #initializeApp(Context)} method to establish a connection to an additional project.
 * You must provide a unique name for the project, which can later be used to retrieve the
 * corresponding FusabaseApp instance via {@link #getInstance(String)}.
 * </p> <p>
 * Note that the default {@link #getInstance()} method always returns the FusabaseApp instance
 * associated with your primary project.
 * </p> <p>
 * Important: All Fusabase initialization must occur in the main app process. Initializing Fusabase
 * in background or secondary processes is not supported and may cause resource conflicts or
 * unexpected behavior.
 */
public class FusabaseApp {

    /**
     * A map to hold all initialized FusabaseApp instances, keyed by their names.
     */
    private static final Map<String, FusabaseApp> APP_INSTANCES = new HashMap<>();

    /**
     * Tag used for logging purposes.
     */
    private static final String TAG = "FusabaseApp";

    /**
     * The default name for the primary FusabaseApp instance.
     */
    public static final String DEFAULT_APP_NAME = "[DEFAULT]";

    /**
     * The configuration options for this FusabaseApp instance.
     */
    public final FusabaseOptions fusabaseOptions;

    /**
     * The unique name of this FusabaseApp instance.
     */
    public final String name;

    /**
     * The Android context associated with this FusabaseApp instance.
     */
    public final Context context;

    /**
     * Private constructor to create a new FusabaseApp instance.
     * This constructor is used internally by the factory methods to initialize an FusabaseApp with
     * the provided context, options, and name.
     *
     * @param context The Android context for the application.
     * @param fusabaseOptions The configuration options for the app.
     * @param name The unique name for this app instance.
     */
    private FusabaseApp(@NonNull Context context,
                     @NonNull FusabaseOptions fusabaseOptions,
                     @NonNull String name) {
        this.fusabaseOptions = fusabaseOptions;
        this.name = name;
        this.context = context;
    }


    /**
     * Method to get the context with which this instance of FusabaseApp was initialized
     *
     * @return {@code Context}
     */
    public Context getApplicationContext() {
        return this.context;
    }

    /**
     * Initializes the default FusabaseApp instance using configuration values retrieved from the app's string resources,
     * which are automatically generated from the fusabase_config.json file.
     * <p>
     * Typically, this setup occurs automatically at app launch via FusabaseInitProvider. However, if you're developing
     * a component that runs outside the main process, you must manually invoke this method before utilizing any Fusabase features.
     * <p>
     * The configuration details (FusabaseOptions) for the default app are derived from the auto-generated string resources.
     *
     * @param context The Android context for the FusabaseApp instance.
     * @return The initialized FusabaseApp instance.
     */
    public static FusabaseApp initializeApp(@NonNull Context context) {
        FusabaseOptions options = FusabaseAppCore.readConfigResources(context);
        FusabaseLogger.d(TAG, "Fusabase Options created from config file " + options);
        return initializeApp(context, options);
    }

    /**
     * Initializes the default FusabaseApp instance using the provided FusabaseOptions, equivalent to calling 
     * {@link #initializeApp(Context, FusabaseOptions, String)} with the {@link #DEFAULT_APP_NAME}.
     * 
     * Typically, manual invocation of this method is unnecessary, as FusabaseInitProvider handles initialization 
     * automatically during app startup. However, in certain exceptional cases where automated setup is not feasible, 
     * you may need to call this method explicitly.
     * 
     * @param context The Android context for the FusabaseApp instance.
     * @param options The FusabaseOptions used to configure the default app.
     * @return The initialized {@code FusabaseApp} instance.
     */
    public static @NonNull FusabaseApp initializeApp(@NonNull Context context,
                                                  @NonNull FusabaseOptions options) {
        // Set logging enabled based on options
        FusabaseLogger.ENABLED = options.isEnableLogging();

        return initializeApp(context, options, DEFAULT_APP_NAME);
    }

    /**
     * Method to initialize the fusabase app with the provider context, options and appName. This
     * is a factory method to initialize Fusabase App.
     *
     * @param context {@code Context} for the fusabaseApp
     * @param options {@code FusabaseOptions} for initializing the app
     * @param name    {@code String} Name of the fusabase app
     * @return {@code FusabaseApp} Initialized fusabaseApp
     */
    public static @NonNull FusabaseApp initializeApp(
            @NonNull Context context,
            @NonNull FusabaseOptions options,
            @NonNull String name
    ) {
        // Only the default instance is supported.
        // Named instances are intentionally not supported.
        if (!DEFAULT_APP_NAME.equals(name)) {
            FusabaseLogger.e(TAG, "Multiple FusabaseApp instances are not supported. Initialize only the default instance ([DEFAULT]).");
            throw new IllegalStateException(
                    "Multiple FusabaseApp instances are not supported. Initialize only the default instance ([DEFAULT])."
            );
        }

        if (!APP_INSTANCES.containsKey(name)) {
            APP_INSTANCES.put(name, new FusabaseApp(context, options, name));
            FusabaseLogger.i(TAG, "Initialized FusabaseApp instance with name " + name);
            return Objects.requireNonNull(APP_INSTANCES.get(name));
        }
        FusabaseLogger.e("An app with the same name is already initialized.");
        throw new IllegalStateException("An app with the same name is already initialized." +
                "Please make sure that the all the Fusabase instance has unique name.");

    }

    /**
     * Method to get all the initialized fusabase app
     *
     * @param context {@code Context} of the application
     * @return {@code List<FusabaseApp>}
     */
    @NonNull
    public static List<FusabaseApp> getApps(@NonNull Context context) {
        List<FusabaseApp> apps = new ArrayList<>();
        APP_INSTANCES.forEach((name, app) -> apps.add(app));
        return apps;
    }

    /**
     * Method to get the default instance of the fusabase app.
     *
     * @return {@code FusabaseApp}
     * @throws {@code java.lang.IllegalStateException} If default fusabase app is not initialized
     */
    @NonNull
    public static FusabaseApp getInstance() {
        FusabaseApp defaultApp = APP_INSTANCES.get(DEFAULT_APP_NAME);
        if (defaultApp == null) {
            {
                FusabaseLogger.e("Default app hasn't been initialized yet.");
                throw new java.lang.IllegalStateException("Default app has not been initialized " +
                        "yet. Initialize it first with initializeApp().");
            }
        }
        FusabaseLogger.v(TAG, "Default app instance returned.");
        return defaultApp;
    }

    /**
     * Method to get the unique named instance of the fusabase app.
     *
     * @param name The name of the FusabaseApp instance to retrieve.
     * @return {@code FusabaseApp} The requested FusabaseApp instance.
     * @throws {@code java.lang.IllegalStateException} If named fusabase app is not initialized
     */
    @NonNull
    public static FusabaseApp getInstance(@NonNull String name) {
        if (!DEFAULT_APP_NAME.equals(name)) {
            FusabaseLogger.e(TAG, "Named FusabaseApp instances are not supported. Use getInstance() for the default instance.");
            throw new IllegalStateException("Named FusabaseApp instances are not supported. Use getInstance() for the default instance.");
        }
        FusabaseApp namedApp = APP_INSTANCES.get(name);
        if (namedApp == null) {
            FusabaseLogger.e(name + " app hasn't been initialized yet.");
            throw new java.lang.IllegalStateException(name + " app has not been initialized" +
                    "yet. Initialize it first with initializeApp(Context, FusabaseOptions, String).");
        }
        FusabaseLogger.v(TAG, name + " app instance returned.");
        return namedApp;
    }

    /**
     * Method to get the FusabaseOptions for this app
     *
     * @return {@code FusabaseOptions}
     */
    @NonNull
    public FusabaseOptions getOptions() {
        return fusabaseOptions;
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
     * Returns the hash code of this instance.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(fusabaseOptions, context, name);
    }

    /**
     * Checks if the provided object is equal to this FusabaseApp instance.
     * Two FusabaseApp instances are considered equal if they have the same context, name, and options.
     *
     * @param obj The object to compare with this instance.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FusabaseApp)) {
            return false;
        }
        FusabaseApp that = (FusabaseApp) obj;
        return Objects.equals(this.context, that.context) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.fusabaseOptions, that.fusabaseOptions);
    }
}
