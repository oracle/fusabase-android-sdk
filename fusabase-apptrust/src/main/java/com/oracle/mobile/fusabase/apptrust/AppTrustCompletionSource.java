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

package com.oracle.mobile.fusabase.apptrust;

import com.oracle.mobile.fusabase.task.TaskCompletionSource;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Completes callback-driven App Trust tasks through the SDK's future lifecycle. */
final class AppTrustCompletionSource<T> extends TaskCompletionSource<T> {
    private final CompletableFuture<Void> completion = new CompletableFuture<>();

    /** Attaches completion before any result or listener can be supplied. */
    AppTrustCompletionSource() {
        super.handleFuture(completion);
    }

    /** Stores the first result and dispatches success and completion listeners once. */
    @Override
    public synchronized void setResult(T result) {
        if (completion.isDone()) {
            return;
        }
        super.setResult(result);
        completion.complete(null);
    }

    /** Preserves the original failure for the SDK's future exception unwrapping. */
    @Override
    public synchronized void setException(Exception error) {
        Objects.requireNonNull(error, "error");
        if (completion.isDone()) {
            return;
        }
        completion.completeExceptionally(new CompletionException(error));
    }
}
