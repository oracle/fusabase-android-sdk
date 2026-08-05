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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseApp;

/**
 * Contains the TOTP secret and configuration used to enroll an authenticator-app factor.
 *
 * <p>Obtain an instance with {@link TotpMultiFactorGenerator#generateSecret(MultiFactorSession)}.
 * Treat the shared secret as sensitive and do not log or persist it.
 */
public class TotpSecret {
    private static final String DEFAULT_ACCOUNT_NAME = "App User";
    private static final String DEFAULT_ISSUER = "Fusabase";
    private static final String DEFAULT_FALLBACK_URL =
            "https://play.google.com/store/search?q=otpauth&c=apps";
    private final String enrollmentId;
    private final String secretKey;
    private final int codeLength;
    private final int codeIntervalSeconds;
    private final int enrollmentTtlSeconds;
    private final MultiFactorSession session;

    TotpSecret(@NonNull String enrollmentId,
               @NonNull String secretKey,
               int codeLength,
               int codeIntervalSeconds,
               int enrollmentTtlSeconds,
               @NonNull MultiFactorSession session) {
        this.enrollmentId = enrollmentId;
        this.secretKey = secretKey;
        this.codeLength = codeLength;
        this.codeIntervalSeconds = codeIntervalSeconds;
        this.enrollmentTtlSeconds = enrollmentTtlSeconds;
        this.session = session;
    }

    /**
     * Returns the identifier for the pending TOTP enrollment.
     *
     * @return the pending factor identifier
     */
    public @NonNull String getEnrollmentId() { return enrollmentId; }

    /**
     * Returns the Base32 secret used to configure an authenticator app.
     *
     * @return the shared TOTP secret
     */
    public @NonNull String getSecretKey() { return secretKey; }

    /**
     * Returns the Base32 secret used to configure an authenticator app.
     *
     * @return the shared TOTP secret
     */
    public @NonNull String getSharedSecretKey() { return secretKey; }

    /**
     * Returns the hash algorithm used by the ORDS TOTP profile.
     *
     * @return {@code SHA1}
     */
    public @NonNull String getHashAlgorithm() { return "SHA1"; }

    /**
     * Returns the number of digits in generated one-time passwords.
     *
     * @return the TOTP code length
     */
    public int getDigits() { return codeLength; }

    /**
     * Returns the TOTP code period in seconds.
     *
     * @return the TOTP code period
     */
    public int getPeriod() { return codeIntervalSeconds; }

    /**
     * Generates an {@code otpauth://} URI using Fusabase defaults.
     *
     * @return a URI that can be displayed as a QR code or opened in an OTP app
     */
    public @NonNull String generateQrCodeUrl() {
        return generateQrCodeUrl(DEFAULT_ACCOUNT_NAME, DEFAULT_ISSUER);
    }

    /**
     * Generates an {@code otpauth://} URI for the supplied account and issuer.
     *
     * @param accountName the account label shown in the OTP app
     * @param issuer the application or organization name shown in the OTP app
     * @return a URI that can be displayed as a QR code or opened in an OTP app
     */
    public @NonNull String getQrCodeUrl(@NonNull String accountName, @NonNull String issuer) {
        return generateQrCodeUrl(accountName, issuer);
    }

    /**
     * Generates an {@code otpauth://} URI for the supplied account and issuer.
     *
     * @param accountName the account label shown in the OTP app
     * @param issuer the application or organization name shown in the OTP app
     * @return a URI that can be displayed as a QR code or opened in an OTP app
     */
    public @NonNull String generateQrCodeUrl(@NonNull String accountName, @NonNull String issuer) {
        return "otpauth://totp/" + encode(issuer + ":" + accountName)
                + "?secret=" + encode(secretKey)
                + "&issuer=" + encode(issuer)
                + "&algorithm=" + getHashAlgorithm()
                + "&digits=" + codeLength
                + "&period=" + codeIntervalSeconds;
    }

    /**
     * Returns the required one-time-password length.
     *
     * @return the TOTP code length
     */
    public int getCodeLength() { return codeLength; }

    /**
     * Returns the TOTP time-step length in seconds.
     *
     * @return the TOTP code period
     */
    public int getCodeIntervalSeconds() { return codeIntervalSeconds; }

    /**
     * Returns the lifetime of the pending enrollment in seconds.
     *
     * @return the enrollment time-to-live, or {@code 0} when the provider does not return one
     */
    public int getEnrollmentTtlSeconds() { return enrollmentTtlSeconds; }
    @NonNull MultiFactorSession getSession() { return session; }

    /**
     * Opens an {@code otpauth://} URI in an installed OTP app.
     *
     * <p>The method creates an {@link Intent#ACTION_VIEW} intent and starts it from the application
     * context with {@link Intent#FLAG_ACTIVITY_NEW_TASK}. If no OTP app handles the URI, it opens
     * the Play Store search page for compatible apps.
     *
     * @param qrCodeUrl the {@code otpauth://} URI to open
     */
    public void openInOtpApp(@NonNull String qrCodeUrl) {
        Intent intent = otpIntent(qrCodeUrl);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            FusabaseApp.getInstance().getApplicationContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_FALLBACK_URL));
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            FusabaseApp.getInstance().getApplicationContext().startActivity(fallbackIntent);
        }
    }

    /**
     * Opens an {@code otpauth://} URI in an installed OTP app.
     *
     * @param qrCodeUrl the {@code otpauth://} URI to open
     * @param fallbackUrl the URI to open when no OTP app handles the QR code URI
     * @param activity the activity used to launch the intent
     */
    public void openInOtpApp(@NonNull String qrCodeUrl, @NonNull String fallbackUrl,
                             @NonNull Activity activity) {
        try {
            activity.startActivity(otpIntent(qrCodeUrl));
        } catch (ActivityNotFoundException e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
        }
    }

    /**
     * Generates and opens an {@code otpauth://} URI for the supplied issuer and account name.
     *
     * @param appName the issuer shown in the OTP app
     * @param accountName the account label shown in the OTP app
     * @param activity the activity used to launch the intent
     */
    public void openInOtpAppForAccount(@NonNull String appName, @NonNull String accountName,
                                       @NonNull Activity activity) {
        openInOtpApp(generateQrCodeUrl(accountName, appName), DEFAULT_FALLBACK_URL, activity);
    }

    @NonNull private static Intent otpIntent(@NonNull String qrCodeUrl) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(qrCodeUrl));
    }

    private static @NonNull String encode(@NonNull String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
}
