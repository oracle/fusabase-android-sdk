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

package com.oracle.mobile.fusabase.oracledb;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.util.function.Consumer;

/** Tracks default-network availability, not endpoint reachability or Internet validation. */
final class OfflineConnectivityMonitor implements AutoCloseable {
    private static final String TAG = "OracledbConnectivity";
    private final ConnectivityManager manager;
    private final Consumer<Boolean> listener;
    private boolean registered;
    private boolean closed;
    private Network currentNetwork;
    private final ConnectivityManager.NetworkCallback callback =
            new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network network) {
                    synchronized (OfflineConnectivityMonitor.this) {
                        if (closed) return;
                        currentNetwork = network;
                        listener.accept(true);
                    }
                }

                @Override public void onLost(Network network) {
                    synchronized (OfflineConnectivityMonitor.this) {
                        if (closed || !network.equals(currentNetwork)) return;
                        currentNetwork = null;
                        listener.accept(false);
                    }
                }
            };

    OfflineConnectivityMonitor(Context context, Consumer<Boolean> listener) {
        this.manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listener = listener;
    }

    synchronized void start() {
        if (closed || registered) return;
        if (manager == null) {
            FusabaseLogger.w(TAG, "Connectivity service unavailable; retaining explicit network control");
            return;
        }
        try {
            // Register first to avoid missing a transition between the initial read and registration.
            manager.registerDefaultNetworkCallback(callback);
            registered = true;
            currentNetwork = manager.getActiveNetwork();
            listener.accept(currentNetwork != null);
        } catch (RuntimeException failure) {
            FusabaseLogger.w(TAG, "Connectivity monitoring unavailable; retaining explicit network control");
            close();
            listener.accept(true);
        }
    }

    @Override public synchronized void close() {
        closed = true;
        if (registered) {
            registered = false;
            try {
                manager.unregisterNetworkCallback(callback);
            } catch (RuntimeException failure) {
                FusabaseLogger.w(TAG, "Connectivity callback could not be unregistered");
            }
        }
    }
}
