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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Class EmailAuthCredential
 * Represents an Email and Password credential for auth workflow
 */
public class EmailAuthCredential extends AuthCredential{

    @NonNull
    private final String provider;
    @NonNull
    private final String signInMethod;
    @NonNull
    private final String email;
    @Nullable
    private final String password;

    /**
     * Creator for creating EmailAuthCredential objects from parcels.
     */
    public static final Creator<EmailAuthCredential> CREATOR = new Creator<EmailAuthCredential>() {
        @Override
        public EmailAuthCredential createFromParcel(Parcel in) {
            return new EmailAuthCredential(in);
        }

        @Override
        public EmailAuthCredential[] newArray(int size) {
            return new EmailAuthCredential[size];
        }
    };

     /**
     * Constructs a new EmailAuthCredential object.
     *
     * @param provider The provider associated with this credential.
     * @param signInMethod The sign-in method associated with this credential.
     * @param email The email address associated with this credential.
     * @param password The password associated with this credential (optional).
     */
    protected EmailAuthCredential(@NonNull String provider,
                                  @NonNull String signInMethod,
                                  @NonNull String email,
                                  @Nullable String password) {
            this.provider = provider;
            this.signInMethod = signInMethod;
            this.email = email;
            this.password = password;
    }

    /**
     * Constructs a new EmailAuthCredential object from a parcel.
     *
     * @param in The parcel containing the data.
     */
    protected EmailAuthCredential(Parcel in) {
        this.provider = Objects.requireNonNull(in.readString());
        this.signInMethod = Objects.requireNonNull(in.readString());
        this.email = Objects.requireNonNull(in.readString());
        this.password = null;
    }

    /**
     * Method to get the provider of the EmailAuthCredential
     * @return {@code String}
     */
    @NonNull
    @Override
    public String getProvider() {
        return this.provider;
    }

    /**
     * Method to get the signInMethod of the EmailAuthCredential
     * @return {@code String}
     */
    @NonNull
    @Override
    public String getSignInMethod() {
        return this.signInMethod;
    }

    /**
     * Method to get the email of the EmailAuthCredential
     * @return {@code String}
     */
    @NonNull
    public String getEmail() {
        return this.email;
    }

    /**
     * Method to get the password of the EmailAuthCredential
     * @return {@code String}
     */
    @Nullable
    public String getPassword() {
        return this.password;
    }

    @NonNull
    @Override
    protected String getIdToken() {
        return "";
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
        parcel.writeString(email);
    }
}
