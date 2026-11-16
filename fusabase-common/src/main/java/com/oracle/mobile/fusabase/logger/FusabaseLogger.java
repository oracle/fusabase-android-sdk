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

package com.oracle.mobile.fusabase.logger;

import android.util.Log;

public class FusabaseLogger {

    public static boolean ENABLED = false;

    private static final String DEFAULT_TAG = "fusabase";

    public enum Level {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    public static void v(String tag, String message) {
        log(Level.VERBOSE, tag, message, null);
    }

    public static void d(String tag, String message) {
        log(Level.DEBUG, tag, message, null);
    }

    public static void i(String tag, String message) {
        log(Level.INFO, tag, message, null);
    }

    public static void w(String tag, String message) {
        log(Level.WARN, tag, message, null);
    }

    public static void w(String tag, String message, Throwable throwable) {
        log(Level.WARN, tag, message, throwable);
    }

    public static void e(String tag, String message) {
        log(Level.ERROR, tag, message, null);
    }

    public static void e(String tag, String message, Throwable throwable) {
        log(Level.ERROR, tag, message, throwable);
    }

    public static void log(Level level, String tag, String message, Throwable throwable) {
        if (!ENABLED) return;
        if (tag == null) tag = DEFAULT_TAG;
        if (message == null) message = "null";

        switch (level) {
            case VERBOSE:
                Log.v(tag, message, throwable);
                break;
            case DEBUG:
                Log.d(tag, message, throwable);
                break;
            case INFO:
                Log.i(tag, message, throwable);
                break;
            case WARN:
                Log.w(tag, message, throwable);
                break;
            case ERROR:
                Log.e(tag, message, throwable);
                break;
        }
    }

    public static void d(String message) {
        d(DEFAULT_TAG, message);
    }

    public static void i(String message) {
        i(DEFAULT_TAG, message);
    }

    public static void w(String message) {
        w(DEFAULT_TAG, message);
    }

    public static void e(String message) {
        e(DEFAULT_TAG, message);
    }

    public static void e(String message, Throwable throwable) {
        e(DEFAULT_TAG, message, throwable);
    }
}
