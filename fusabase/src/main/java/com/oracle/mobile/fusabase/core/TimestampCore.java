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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Internal class for Timestamp core logic
 */
public class TimestampCore {

    private static final long MAX_SECONDS = 253402300799L;    // 9999-12-31T23:59:59Z
    private static final long MIN_SECONDS = -62135596800L;    // 0001-01-01T00:00:00Z

    private final String timestamp;

    /**
     * Default Constructor
     * Provides a RFC3339 compliant timestamp of the current time
     */
    public TimestampCore() {
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    public TimestampCore(@NonNull String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Method to get Oradb timestamp from the provided date
     * @param date {@code Date} Date that needs to be converted into timestamp
     */
    public TimestampCore(@NonNull Date date) {
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(date.toInstant());
    }

    /**
     * Constructs a new TimestampCore object from the given Instant.
     *
     * @param time A non-null Instant object representing the time.
     * @since Android Oreo (API level 26)
     */
    public TimestampCore(@NonNull Instant time) {
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(time);
    }

    public TimestampCore(long seconds, int nanoseconds) {
        if (nanoseconds < 0 || nanoseconds >= 1_000_000_000) {
            throw new IllegalArgumentException("Nanoseconds must be between 0 and 999,999,999");
        }
        if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) {
            throw new IllegalArgumentException("Seconds value is out of valid range: " + seconds);
        }

        Instant instant = Instant.ofEpochSecond(seconds, nanoseconds);
        this.timestamp = DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    /**
     * Returns a new TimestampCore object representing the current time.
     *
     * @return A non-null TimestampCore object representing the current time.
     */
    @NonNull
    public static TimestampCore now() {
        return new TimestampCore();
    }

    /**
     * Converts this timestamp to a Date object.
     *
     * @return A non-null Date object representing this timestamp.
     * @throws DateTimeParseException If the timestamp cannot be parsed.
     */
    @NonNull
    public final Date toDate() {
        return Date.from(Instant.parse(this.timestamp));
    }

    /**
     * Converts this timestamp to an Instant object.
     *
     * @return An Instant object representing this timestamp.
     * @throws DateTimeParseException If the timestamp cannot be parsed.
     * @since Android Oreo (API level 26)
     */
    @RequiresApi(value = 26)
    public final Instant toInstant() {
        return Instant.parse(this.timestamp);
    }

    /**
     * Method to get the timestamp in string
     * @return {@code String} Timestamp
     */
    public String getString() {
        return this.timestamp;
    }

    /**
     * Compares this timestamp with another timestamp.
     *
     * @param other The timestamp to compare with.
     * @return A negative integer, zero, or a positive integer as this timestamp
     *         is before, equal to, or after the specified timestamp.
     */
    public int compareTo(TimestampCore other) {
        return this.toInstant().compareTo(other.toInstant());
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj The reference object with which to compare.
     * @return True if this object is the same as the obj argument; false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TimestampCore)) {
            return false;
        }
        TimestampCore other = (TimestampCore) obj;
        return this.timestamp.equals(other.timestamp);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return timestamp.hashCode();
    }

    /**
     * Returns a string representation of this timestamp.
     *
     * @return A string representation of this timestamp.
     */
    @Override
    public String toString() {
        return timestamp;
    }

    public int getNanoseconds() {
        return toInstant().getNano();
    }

    public long getSeconds() {
        return toInstant().getEpochSecond();
    }
}
