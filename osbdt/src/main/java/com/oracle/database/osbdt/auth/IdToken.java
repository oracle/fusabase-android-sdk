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

import androidx.annotation.NonNull;

import com.oracle.database.osbdt.utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import jakarta.json.Json;
import jakarta.json.JsonObject;

class IdToken {

    private String token;
    protected long issuedAtTime;
    protected long expirationTime;
    protected String signInProvider;
    protected long authTime;
    protected JsonObject parsedJWT;
    protected JsonObject claims;

    protected IdToken(@NonNull String token, String provider) {

        provider = provider == null ? "password" : provider;
        this.token = token;
        this.parsedJWT = (JsonObject) Utils.parseJWT(token);
        this.issuedAtTime = new BigDecimal(this.parsedJWT.getJsonNumber("iat").toString()).setScale(0, RoundingMode.HALF_UP).longValue();
        this.expirationTime = new BigDecimal(this.parsedJWT.getJsonNumber("exp").toString()).setScale(0, RoundingMode.HALF_UP).longValue();
        this.signInProvider = provider;
        this.authTime = new BigDecimal(this.parsedJWT.getJsonNumber("iat").toString()).setScale(0, RoundingMode.HALF_UP).longValue();
        claims = Json.createObjectBuilder()
                .add("auth_time", new BigDecimal(this.parsedJWT.getJsonNumber("iat").toString()).setScale(0, RoundingMode.HALF_UP).longValue())
                .add("exp", new BigDecimal(this.parsedJWT.getJsonNumber("exp").toString()).setScale(0, RoundingMode.HALF_UP).longValue())
                .add("iat", new BigDecimal(this.parsedJWT.getJsonNumber("iat").toString()).setScale(0, RoundingMode.HALF_UP).longValue())
                .add("sub", this.parsedJWT.getString("sub"))
//                .add("user_id", this.parsedJWT.getString("user_id"))
                .build();
    }

    protected boolean validateAccessToken() {
        return this.expirationTime >= ( Instant.now().toEpochMilli() / 1000) + 5*60;
    }

    protected String getToken() {
        this.token = (this.validateAccessToken() ? token : "");
        return this.token;
    }
}
