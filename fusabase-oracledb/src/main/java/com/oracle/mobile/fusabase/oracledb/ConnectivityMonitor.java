// Copyright (c) 2015, 2026, Oracle and/or its affiliates.

package com.oracle.mobile.fusabase.oracledb;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

/** Reports transitions of the default network's validated Internet capability. */
final class ConnectivityMonitor {
    interface Listener {
        void onValidatedConnectivityChanged(boolean validated);
    }

    private final ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback callback;
    private final Listener listener;
    private boolean registered;

    ConnectivityMonitor(@NonNull Context context, @NonNull Listener listener) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        this.listener = listener;
        this.callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                publishCurrentState();
            }

            @Override
            public void onLost(@NonNull Network network) {
                publishCurrentState();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                               @NonNull NetworkCapabilities capabilities) {
                publish(capabilities);
            }
        };
    }

    /** Creates a no-op monitor for JVM tests that inject connectivity events separately. */
    ConnectivityMonitor(@NonNull Listener listener) {
        this.connectivityManager = null;
        this.listener = listener;
        this.callback = null;
    }

    synchronized void start() {
        if (registered || connectivityManager == null) {
            return;
        }
        connectivityManager.registerDefaultNetworkCallback(callback);
        registered = true;
        publishCurrentState();
    }

    synchronized void stop() {
        if (!registered || connectivityManager == null) {
            return;
        }
        connectivityManager.unregisterNetworkCallback(callback);
        registered = false;
    }

    private void publishCurrentState() {
        if (connectivityManager == null) {
            listener.onValidatedConnectivityChanged(false);
            return;
        }
        Network network = connectivityManager.getActiveNetwork();
        publish(network == null ? null : connectivityManager.getNetworkCapabilities(network));
    }

    private void publish(NetworkCapabilities capabilities) {
        listener.onValidatedConnectivityChanged(capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
    }
}
