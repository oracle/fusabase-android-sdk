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

package com.oracle.database.osbdt.auth;

import android.os.Parcel;

import androidx.annotation.*;

import java.util.Objects;

/**
 * Represents a Google authentication credential.
 *<p>
 * This class extends the AuthCredential abstract class and provides additional functionality for handling Google authentication.
 *</p>
 */
public class GoogleAuthCredential extends AuthCredential{
    @NonNull
    private final String provider;
    @NonNull
    private final String signInMethod;
    @NonNull
    private final String accessToken;

    /**
     * Creator for creating GoogleAuthCredential objects from parcels.
     */
    public static final Creator<GoogleAuthCredential> CREATOR = new Creator<GoogleAuthCredential>() {
        @Override
        public GoogleAuthCredential createFromParcel(Parcel in) {
            return new GoogleAuthCredential(in);
        }

        @Override
        public GoogleAuthCredential[] newArray(int size) {
            return new GoogleAuthCredential[size];
        }
    };

    /**
     * Constructs a new GoogleAuthCredential object.
     *
     * @param provider The provider associated with this credential.
     * @param signInMethod The sign-in method associated with this credential.
     * @param accessToken The access token associated with this credential.
     */
    protected GoogleAuthCredential(@NonNull String provider,
                                   @NonNull String signInMethod,
                                   @NonNull String accessToken) {
        this.provider = provider;
        this.signInMethod = signInMethod;
        this.accessToken = accessToken;
    }

    /**
     * Constructs a new GoogleAuthCredential object from a parcel.
     *
     * @param in The parcel containing the data.
     */
    protected GoogleAuthCredential(Parcel in) {
        this.provider = Objects.requireNonNull(in.readString());
        this.signInMethod = Objects.requireNonNull(in.readString());
        this.accessToken = null;
    }

    /**
     * Gets the provider associated with this credential.
     *
     * @return The provider.
     */
    public @NonNull String getProvider() {
        return provider;
    }

    /**
     * Gets the sign-in method associated with this credential.
     *
     * @return The sign-in method.
     */
    public @NonNull String getSignInMethod() {
        return signInMethod;
    }

    @NonNull
    @Override
    protected String getIdToken() {
        return this.accessToken;
    }

    /**
     * Describes the contents of this parcelable object.
     *
     * @return A bitmask indicating the set of special object types held by the Parcelable object.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes this object to a parcel.
     *
     * @param parcel The parcel to write to.
     * @param flags Additional flags about how the object should be written.
     */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeString(provider);
        parcel.writeString(signInMethod);
        // Don't write access token into parcel
    }}
