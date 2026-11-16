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

package com.oracle.mobile.fusabase.internal;

import androidx.annotation.RestrictTo;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Internal app-scoped session fence. A new state invalidates work from an earlier session. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AuthSession {
    public static final class State {
        public final String userId;
        private State(String userId) { this.userId = userId; }
    }
    public interface Subscription { void remove(); }
    private volatile State state = new State(null);
    private final CopyOnWriteArrayList<Consumer<State>> listeners = new CopyOnWriteArrayList<>();

    public State current() { return state; }
    public boolean isCurrent(State expected) { return state == expected; }

    public void update(String userId) { change(userId, false); }
    public void invalidate() { change(null, true); }

    private void change(String userId, boolean force) {
        synchronized (this) {
            if (!force && Objects.equals(state.userId, userId)) return;
            state = new State(userId);
        }
        // Never call consumers while holding the session lock. Consumers read the latest state,
        // so concurrent updates cannot restore an obsolete session.
        for (Consumer<State> listener : listeners) notifyListener(listener);
    }

    public Subscription subscribe(Consumer<State> listener) {
        Objects.requireNonNull(listener);
        listeners.add(listener);
        notifyListener(listener);
        return () -> listeners.remove(listener);
    }

    private void notifyListener(Consumer<State> listener) {
        try { listener.accept(state); }
        catch (RuntimeException failure) {
            // A failing consumer must not prevent the remaining clients from invalidating work.
            Logger.getLogger(AuthSession.class.getName()).warning("Auth session consumer failed");
        }
    }
}
