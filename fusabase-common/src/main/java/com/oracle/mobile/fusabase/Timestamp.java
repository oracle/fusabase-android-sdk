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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.oracle.mobile.fusabase.core.TimestampCore;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Represents a point in time with nanosecond precision.
 * <p>
 * This class provides an immutable representation of a timestamp that can be created from various
 * sources including the current time, Date objects, Instant objects, or string representations.
 * Timestamps are RFC3339 compliant and support conversion to and from various time representations.
 * </p>
 * <p>
 * Instances of this class are immutable and thread-safe.
 * </p>
 */
public class Timestamp implements Comparable<Timestamp>, Parcelable {

    private final TimestampCore core;

    /**
     * Constructs a new Timestamp representing the current time.
     * <p>
     * This creates an RFC3339 compliant timestamp of the current system time.
     * </p>
     */
    protected Timestamp() {
        this.core = new TimestampCore();
    }

    /**
     * Constructs a new Timestamp from a string representation.
     *
     * @param timestamp The string representation of the timestamp.
     */
    protected Timestamp(@NonNull String timestamp) {
        this.core = new TimestampCore(timestamp);
    }

    /**
     * Creator for parceling/unparceling Timestamp instances.
     */
    public static final @NonNull Parcelable.Creator<Timestamp> CREATOR = new Creator<Timestamp>() {
        @Override
        public Timestamp createFromParcel(Parcel parcel) {
            String tempTimestamp = parcel.readString();
            if (tempTimestamp == null) {
                throw new IllegalArgumentException("Timestamp string cannot be null");
            }
            // Validate the timestamp format
            try {
                Instant.parse(tempTimestamp);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid timestamp format: " + tempTimestamp, e);
            }
            return new Timestamp(tempTimestamp);
        }

        @Override
        public Timestamp[] newArray(int i) {
            return new Timestamp[i];
        }
    };

    /**
     * Method to get Oradb timestamp from the provided date
     * @param date {@code Date} Date that needs to be converted into timestamp
     */
    public Timestamp(@NonNull Date date) {
        this.core = new TimestampCore(date);
    }

    /**
     * Constructs a new Timestamp object from the given Instant.
     *
     * @param time A non-null Instant object representing the time.
     * @since Android Oreo (API level 26)
     */
    public Timestamp(@NonNull Instant time) {
        this.core = new TimestampCore(time);
    }

    /**
     * Constructs a new Timestamp with the specified seconds and nanoseconds since epoch.
     *
     * @param seconds     The seconds since Unix epoch (January 1, 1970 UTC).
     * @param nanoseconds The nanoseconds within the second (0-999999999).
     */
    public Timestamp(long seconds, int nanoseconds) {
        this.core = new TimestampCore(seconds, nanoseconds);
    }

    /**
     * Returns a new Timestamp object representing the current time.
     *
     * @return A non-null Timestamp object representing the current time.
     */
    @NonNull
    public static Timestamp now() {
        return new Timestamp();
    }

    /**
     * Constructs a new Timestamp from a TimestampCore instance.
     * <p>
     * This constructor is primarily used internally for creating Timestamp objects
     * from existing TimestampCore instances.
     * </p>
     *
     * @param core The TimestampCore instance to wrap.
     */
    protected Timestamp(TimestampCore core) {
        this.core = core;
    }

    /**
     * Converts this timestamp to a Date object.
     *
     * @return A non-null Date object representing this timestamp.
     * @throws DateTimeParseException If the timestamp cannot be parsed.
     */
    @NonNull
    public final Date toDate() {
        return core.toDate();
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
        return core.toInstant();
    }

    /**
     * Returns the string representation of this timestamp.
     *
     * @return The RFC3339 compliant string representation of this timestamp.
     */
    public String getString() {
        return core.getString();
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable instance's marshaled representation.
     * <p>
     * This implementation returns 0 as this class contains no special objects.
     * </p>
     *
     * @return A bitmask indicating the set of special object types marshaled by this Parcelable.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes this Timestamp instance to the given Parcel.
     *
     * @param parcel The Parcel to write to.
     * @param flags  Additional flags about how the object should be written.
     */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeString(core.getString());
    }

    /**
     * Compares this timestamp with another timestamp.
     *
     * @param other The timestamp to compare with.
     * @return A negative integer, zero, or a positive integer as this timestamp
     *         is before, equal to, or after the specified timestamp.
     */
    @Override
    public int compareTo(Timestamp other) {
        return core.compareTo(other.core);
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
        if (!(obj instanceof Timestamp)) {
            return false;
        }
        Timestamp other = (Timestamp) obj;
        return core.equals(other.core);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return core.hashCode();
    }

    /**
     * Returns a string representation of this timestamp.
     *
     * @return A string representation of this timestamp.
     */
    @Override
    public String toString() {
        return core.toString();
    }

    /**
     * Returns the nanoseconds component of this timestamp.
     * <p>
     * This represents the nanoseconds within the second (0-999999999).
     * </p>
     *
     * @return The nanoseconds component.
     */
    public int getNanoseconds() {
        return core.getNanoseconds();
    }

    /**
     * Returns the seconds component of this timestamp.
     * <p>
     * This represents the seconds since Unix epoch (January 1, 1970 UTC).
     * </p>
     *
     * @return The seconds component.
     */
    public long getSeconds() {
        return core.getSeconds();
    }
}
