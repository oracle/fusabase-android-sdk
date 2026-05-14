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
import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * Represents a binary large object (BLOB) that holds an immutable array of bytes.
 */
public class Blob implements Comparable<Blob> {

    /**
     * The byte array containing the BLOB data.
     */
    private final byte[] bytes;

    /**
     * Constructs a Blob with the specified byte array.
     *
     * @param bytes the byte array to store in this Blob
     */
    protected Blob(@NonNull byte[] bytes) {
        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    /**
     * Compares this Blob with the specified Blob for order.
     *
     * @param blob the Blob to be compared
     * @return a negative integer, zero, or a positive integer as this Blob
     *         is less than, equal to, or greater than the specified Blob
     */
    @Override
    public int compareTo(Blob blob) {
        for (int i = 0; i < Math.min(this.bytes.length, blob.bytes.length); i++) {
            int diff = Byte.compare(this.bytes[i], blob.bytes[i]);
            if (diff != 0) {
                return diff;
            }
        }

        return Integer.compare(this.bytes.length, blob.bytes.length);
    }

    /**
     * Compares this Blob to the specified object for equality.
     *
     * @param other the object to compare
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        if (other instanceof Blob) {
            Blob otherBlob = (Blob) other;
            return Arrays.equals(otherBlob.bytes, this.bytes);
        }

        return false;
    }

    /**
     * Creates a new Blob from the specified byte array.
     *
     * @param bytes the byte array
     * @return a new Blob containing the specified bytes
     */
    public static @NonNull Blob fromBytes(@NonNull byte[] bytes) {
        java.util.Objects.requireNonNull(bytes, "Invalid bytes provided");
        return new Blob(bytes);
    }

    /**
     * Returns a hash code value for this Blob.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    /**
     * Returns the bytes of this Blob as a byte array.
     *
     * @return the byte array
     */
    @NonNull
    public byte[] toBytes() {
        return Arrays.copyOf(this.bytes, this.bytes.length);
    }

    /**
     * Returns a string representation of this Blob.
     *
     * @return a string representation
     */
    @NonNull
    @Override
    public String toString() {
        return Arrays.toString(this.bytes);
    }
}
