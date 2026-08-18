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

package com.oracle.mobile.fusabase.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.FusabaseException;
import com.oracle.mobile.fusabase.FusabaseNetworkException;
import com.oracle.mobile.fusabase.http.HttpRequestHelper;
import com.oracle.mobile.fusabase.http.HttpResponse;
import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

class MfaServiceHelper {
    private static final String TAG = "FusabaseMFAService";

    interface HttpExecutor {
        @NonNull HttpResponse execute(@NonNull String url,
                                      @NonNull String method,
                                      @NonNull Map<String, String> headers,
                                      @NonNull Map<String, String> query,
                                      @Nullable JsonObject payload) throws FusabaseException;
    }

    interface AuthHeaderProvider {
        @NonNull String getAuthorizationHeader() throws FusabaseException;
    }

    private static final HttpExecutor DEFAULT_HTTP_EXECUTOR = (url, method, headers, query, payload) -> {
        HttpRequestHelper helper = new HttpRequestHelper();
        if (payload == null) {
            helper.createHttpRequest(url, method, headers, query);
        } else {
            helper.createHttpRequest(url, method, headers, query, payload.toString());
        }
        return helper.executeRequest();
    };

    private final Config config;
    private final AuthHeaderProvider authHeaderProvider;
    private final HttpExecutor httpExecutor;

    MfaServiceHelper(@NonNull FusabaseAuth auth, @NonNull FusabaseUser user) {
        this(user.config, () -> user.userHelper.getAuthorizationHeader(), DEFAULT_HTTP_EXECUTOR);
    }

    /**
     * Creates a service for resolving a pending sign-in challenge before a user is authenticated.
     */
    static @NonNull MfaServiceHelper forSignInChallenge(@NonNull FusabaseAuth auth) {
        return new MfaServiceHelper(auth.getConfig(), () -> "", DEFAULT_HTTP_EXECUTOR);
    }

    MfaServiceHelper(@NonNull Config config,
                     @NonNull String authorizationHeader,
                     @NonNull HttpExecutor httpExecutor) {
        this(config, () -> authorizationHeader, httpExecutor);
    }

    MfaServiceHelper(@NonNull Config config,
                     @NonNull AuthHeaderProvider authHeaderProvider,
                     @NonNull HttpExecutor httpExecutor) {
        this.config = config;
        this.authHeaderProvider = authHeaderProvider;
        this.httpExecutor = httpExecutor;
    }

    static @NonNull MfaServiceHelper forSession(@NonNull MultiFactorSession session) {
        if (session.getService() != null) {
            return session.getService();
        }
        throw new IllegalStateException("MFA session is not associated with an MFA service.");
    }

    @NonNull MultiFactorSession createSession() throws FusabaseException {
        JsonObject response = request("Creating MFA session", "mfa/createSession", "GET", authHeaders(), null);
        return new MultiFactorSession(response.getString("challengeId"), response.getString("mfaSessionToken"), this);
    }

    @NonNull TotpSecret startTotpEnrollment(@NonNull MultiFactorSession session) throws FusabaseException {
        return startTotpEnrollment(session, null);
    }

    @NonNull TotpSecret startTotpEnrollment(@NonNull MultiFactorSession session,
                                             @Nullable String displayName) throws FusabaseException {
        JsonObject response = startEnrollment(session, "TOTP", displayName, null, null);
        String factorId = response.getString("factorId", response.getString("factor_id", ""));
        String sharedSecret = response.getString("sharedSecret", response.getString("secretCode", ""));
        if (factorId.trim().isEmpty() || sharedSecret.trim().isEmpty()) {
            throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "TOTP enrollment response is missing the factor ID or shared secret.");
        }
        return new TotpSecret(factorId, sharedSecret, response.getInt("digits", 6),
                response.getInt("periodSeconds", 30), response.getInt("enrollmentTtlSeconds", 0), session);
    }

    @NonNull MultiFactorInfo verifyTotpEnrollment(@NonNull TotpMultiFactorAssertion assertion,
                                                   @Nullable String displayName) throws FusabaseException {
        JsonObject response = verifyEnrollment(assertion.getFactorUid(), assertion.getCode(), assertion.getSession());
        return factorInfoFromResponse(response, assertion.getFactorUid(), TotpMultiFactorGenerator.FACTOR_ID, displayName);
    }

    @NonNull SmsEnrollment startSmsEnrollment(@NonNull MultiFactorSession session,
                                              @NonNull String phoneNumber,
                                              @Nullable String countryCode) throws FusabaseException {
        if (countryCode == null || countryCode.trim().isEmpty()) {
            throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "countryCode is required for SMS MFA enrollment.");
        }
        JsonObject response = startEnrollment(session, "SMS", null, countryCode, phoneNumber);
        return new SmsEnrollment(response.getString("factorId"), session);
    }

    @NonNull String startSmsSignIn(@NonNull MultiFactorSession session,
                                   @NonNull String factorUid) throws FusabaseException {
        JsonObjectBuilder payload = Json.createObjectBuilder()
                .add("challengeId", session.getChallengeId())
                .add("mfaSessionToken", session.getMfaSessionToken());
        payload.add(isIdcs() ? "factor" : "factorId", factorUid);
        request("Starting SMS MFA sign-in",
                isIdcs() ? "authenticate/mfa/challenge/sms/send" : "mfa/challenge/sms/send",
                "POST", optionalAuthHeaders(), payload.build());
        return factorUid;
    }

    @NonNull MultiFactorInfo verifySmsEnrollment(@NonNull PhoneMultiFactorAssertion assertion,
                                                  @Nullable String displayName) throws FusabaseException {
        JsonObject response = verifyEnrollment(assertion.getCredential().getVerificationId(),
                assertion.getCredential().getSmsCode(), assertion.getCredential().getSession());
        return factorInfoFromResponse(response, assertion.getCredential().getVerificationId(), PhoneMultiFactorGenerator.FACTOR_ID, displayName);
    }

    @NonNull JsonObject verifyMfaChallenge(@NonNull MultiFactorAssertion assertion) throws FusabaseException {
        return request("Verifying MFA challenge", "authenticate/mfa/verify", "POST",
                optionalAuthHeaders(), mfaChallengePayload(assertion));
    }

    void deleteFactor(@NonNull String factorUid) throws FusabaseException {
        String endpoint = (isIdcs() ? "authenticate/mfa/factors/" : "mfa/factors/")
                + encodePathSegment(factorUid);
        request("Deleting MFA factor", endpoint, "DELETE", authHeaders(), null);
    }

    @NonNull List<MultiFactorInfo> listFactors() throws FusabaseException {
        JsonObject response = request("Listing MFA factors", enrollmentOptionsEndpoint(), "GET", authHeaders(), null);
        List<MultiFactorInfo> result = new ArrayList<>();
        for (JsonValue value : response.getJsonArray("factors")) {
            if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                result.add(MultiFactorInfoFactory.fromJson(value.asJsonObject()));
            }
        }
        return result;
    }

    @NonNull private String encodePathSegment(@NonNull String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    @NonNull private MultiFactorInfo factorInfoFromResponse(@NonNull JsonObject response,
                                                            @NonNull String fallbackUid,
                                                            @NonNull String factorId,
                                                            @Nullable String displayName) throws FusabaseAuthException {
        JsonObjectBuilder builder = Json.createObjectBuilder(response);
        if (!response.containsKey("factor_id") && !response.containsKey("factorId")) {
            builder.add("factorId", fallbackUid);
        }
        if (!response.containsKey("method")) {
            builder.add("method", factorId);
        }
        if (displayName != null && !response.containsKey("displayName")) {
            builder.add("displayName", displayName);
        }
        return MultiFactorInfoFactory.fromJson(builder.build());
    }

    @NonNull private JsonObject request(@NonNull String logAction,
                                        @NonNull String endpoint,
                                        @NonNull String method,
                                        @NonNull Map<String, String> headers,
                                        @Nullable JsonObject payload) throws FusabaseException {
        String url = config.getAuthBaseEndpoint(endpoint);
        Map<String, String> query = new HashMap<>();
        query.put("apiKey", config.getAppId());

        HttpResponse response;
        try {
            response = httpExecutor.execute(url, method, headers, query, payload);
        } catch (FusabaseException e) {
            FusabaseLogger.e(TAG, "Network error encountered while performing MFA operation");
            throw new FusabaseNetworkException("Network error encountered due to " + e.getCause());
        }

        if (response.getStatus()) {
            String body = response.getResponse();
            if (body == null || body.isEmpty()) return Json.createObjectBuilder().add("success", 1).build();
            try (JsonReader reader = Json.createReader(new StringReader(body))) {
                return reader.readObject();
            }
        }

        throw new FusabaseAuthException(FusabaseAuthException.Code.fromCode(response.getCode()).toString(),
                response.getError() == null || response.getError().isEmpty()
                        ? "MFA request failed."
                        : response.getError());
    }

    @NonNull private Map<String, String> authHeaders() throws FusabaseException {
        Map<String, String> headers = optionalAuthHeaders();
        String authHeaderName = isIdcs() ? "X-AUTHZ" : "Authorization";
        if (!headers.containsKey(authHeaderName)) {
            throw new FusabaseAuthException(FusabaseAuthException.Code.UNAUTHENTICATED.toString(),
                    "User must be signed in for MFA operations.");
        }
        return headers;
    }

    @NonNull private Map<String, String> optionalAuthHeaders() throws FusabaseException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String authHeader = authHeaderProvider.getAuthorizationHeader();
        if (!authHeader.isEmpty()) {
            if (isIdcs()) {
                String accessToken = rawAccessToken(authHeader);
                if (!accessToken.isEmpty()) {
                    headers.put("X-AUTHZ", accessToken);
                }
            } else {
                headers.put("Authorization", authHeader);
            }
        }
        return headers;
    }

    @NonNull private static String rawAccessToken(@NonNull String authorizationHeader) {
        String value = authorizationHeader.trim();
        String bearerPrefix = "Bearer ";
        return value.regionMatches(true, 0, bearerPrefix, 0, bearerPrefix.length())
                ? value.substring(bearerPrefix.length()).trim()
                : value;
    }

    private boolean isIdcs() {
        return Config.IDCS_AUTH_TYPE.equals(config.getAuthType());
    }

    @NonNull private String enrollmentOptionsEndpoint() {
        return isIdcs() ? "authenticate/mfa/factors" : "mfa/enrollment-options";
    }

    @NonNull private JsonObject startEnrollment(@NonNull MultiFactorSession session,
                                                @NonNull String factor,
                                                @Nullable String displayName,
                                                @Nullable String countryCode,
                                                @Nullable String mobileNumber) throws FusabaseException {
        JsonObjectBuilder payload = Json.createObjectBuilder()
                .add("challengeId", session.getChallengeId())
                .add("mfaSessionToken", session.getMfaSessionToken())
                .add("factor", factor);
        if (displayName != null) {
            payload.add("displayName", displayName);
        }
        if (countryCode != null && mobileNumber != null) {
            payload.add("countryCode", countryCode.trim())
                    .add("mobileNumber", mobileNumber);
        }
        return request("Starting MFA enrollment", "authenticate/mfa/enroll/start", "POST",
                authHeaders(), payload.build());
    }

    @NonNull private JsonObject verifyEnrollment(@NonNull String factorId,
                                                 @NonNull String otpCode,
                                                 @Nullable MultiFactorSession session)
            throws FusabaseException {
        if (session == null) {
            throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "MFA session is required for enrollment verification.");
        }
        JsonObject payload = Json.createObjectBuilder().add("factorId", factorId).add("otpCode", otpCode)
                .add("challengeId", session.getChallengeId())
                .add("mfaSessionToken", session.getMfaSessionToken()).build();
        return request("Verifying MFA enrollment", "authenticate/mfa/enroll/verify", "POST",
                authHeaders(), payload);
    }

    @NonNull private JsonObject mfaChallengePayload(@NonNull MultiFactorAssertion assertion)
            throws FusabaseAuthException {
        String factor;
        String otpCode;
        MultiFactorSession session;
        if (assertion instanceof TotpMultiFactorAssertion) {
            TotpMultiFactorAssertion totp = (TotpMultiFactorAssertion) assertion;
            factor = totp.getFactorUid();
            otpCode = totp.getCode();
            session = totp.getSession();
        } else if (assertion instanceof PhoneMultiFactorAssertion) {
            PhoneAuthCredential credential = ((PhoneMultiFactorAssertion) assertion).getCredential();
            factor = credential.getVerificationId();
            otpCode = credential.getSmsCode();
            session = credential.getSession();
        } else {
            throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "Unsupported MFA assertion.");
        }
        if (session == null) {
            throw new FusabaseAuthException(FusabaseAuthException.Code.INVALID_ARGUMENT.toString(),
                    "MFA session is required for challenge verification.");
        }
        return Json.createObjectBuilder().add("factor", factor).add("otpCode", otpCode)
                .add("challengeId", session.getChallengeId())
                .add("mfaSessionToken", session.getMfaSessionToken()).build();
    }

    static final class SmsEnrollment {
        final String verificationId;
        final MultiFactorSession session;

        SmsEnrollment(@NonNull String verificationId, @NonNull MultiFactorSession session) {
            this.verificationId = verificationId;
            this.session = session;
        }
    }
}
