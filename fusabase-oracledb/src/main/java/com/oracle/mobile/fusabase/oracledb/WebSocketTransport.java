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

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.http.WebSocketCallback;
import com.oracle.mobile.fusabase.http.WebSocketClient;

/** Internal WebSocket boundary used by the realtime controller. */
interface WebSocketTransport {
    void establishConnection(@NonNull String url, @NonNull WebSocketCallback callback);

    void sendMessage(@NonNull String message);

    void close();

    /** Immediately aborts an authenticated connection, including an in-flight handshake. */
    default void cancel() { close(); }

    @NonNull WebSocketClient.State getState();
}

/** Creates the internal WebSocket transport. Production code uses the default implementation. */
interface WebSocketTransportFactory {
    @NonNull WebSocketTransport create();
}

/** Bridges the controller's internal transport boundary to the existing HTTP module client. */
final class DefaultWebSocketTransportFactory implements WebSocketTransportFactory {
    @Override
    @NonNull
    public WebSocketTransport create() {
        return new DefaultWebSocketTransport();
    }

    private static final class DefaultWebSocketTransport implements WebSocketTransport {
        private volatile WebSocketClient delegate;

        @Override
        public synchronized void establishConnection(@NonNull String url, @NonNull WebSocketCallback callback) {
            if (delegate != null) delegate.cancel();
            delegate = new WebSocketClient(callback);
            delegate.establishConnection(url);
        }

        @Override
        public void sendMessage(@NonNull String message) {
            if (delegate != null) {
                delegate.sendMessage(message);
            }
        }

        @Override
        public synchronized void close() {
            if (delegate != null) {
                delegate.close();
            }
        }

        @Override
        public synchronized void cancel() {
            if (delegate != null) delegate.cancel();
        }

        @Override
        @NonNull
        public WebSocketClient.State getState() {
            return delegate == null ? WebSocketClient.State.CLOSED : delegate.getState();
        }
    }
}
