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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.oracle.mobile.fusabase.FusabaseApp;

/**
 * Contains the TOTP secret and configuration used to enroll an authenticator-app factor.
 *
 * <p>Obtain an instance with {@link TotpMultiFactorGenerator#generateSecret(MultiFactorSession)}.
 * Treat the shared secret as sensitive and do not log or persist it.
 * Opening it in an OTP app displays a chooser. Select only a trusted authenticator:
 * the selected app receives the shared secret and can generate verification codes.
 *
 * <p>Example:</p>
 * <pre>{@code
 * // Given an enrollment session:
 * TotpMultiFactorGenerator.generateSecret(session).addOnSuccessListener(secret -> {
 *     secret.openInOtpApp(secret.generateQrCodeUrl("alex@example.com", "My App"));
 * });
 * }</pre>
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
     * <p>Example: Given a TotpSecret instance named secret.</p>
     * <pre>{@code
     * String enrollmentId = secret.getEnrollmentId();
     * }</pre>
     *
     * @return the pending factor identifier
     */
    public @NonNull String getEnrollmentId() { return enrollmentId; }

    /**
     * Returns the Base32 secret used to configure an authenticator app.
     *
     * <p>Example: Given a TotpSecret instance named secret. Do not log or persist this sensitive value.</p>
     * <pre>{@code
     * String secretKey = secret.getSecretKey();
     * }</pre>
     *
     * @return the shared TOTP secret
     */
    public @NonNull String getSecretKey() { return secretKey; }

    /**
     * Returns the Base32 secret used to configure an authenticator app.
     *
     * <p>Example: Given a TotpSecret instance named secret. Do not log or persist this sensitive value.</p>
     * <pre>{@code
     * String sharedSecretKey = secret.getSharedSecretKey();
     * }</pre>
     *
     * @return the shared TOTP secret
     */
    public @NonNull String getSharedSecretKey() { return secretKey; }

    /**
     * Returns the hash algorithm used by the ORDS TOTP profile.
     *
     * <p>Example: Given a TotpSecret instance named secret.</p>
     * <pre>{@code
     * String hashAlgorithm = secret.getHashAlgorithm();
     * }</pre>
     *
     * @return {@code SHA1}
     */
    public @NonNull String getHashAlgorithm() { return "SHA1"; }

    /**
     * Returns the number of digits in generated one-time passwords.
     *
     * <p>Example: Given a TotpSecret instance named secret.</p>
     * <pre>{@code
     * int digits = secret.getDigits();
     * }</pre>
     *
     * @return the TOTP code length
     */
    public int getDigits() { return codeLength; }

    /**
     * Returns the TOTP code period in seconds.
     *
     * <p>Example: Given a TotpSecret instance named secret.</p>
     * <pre>{@code
     * int period = secret.getPeriod();
     * }</pre>
     *
     * @return the TOTP code period
     */
    public int getPeriod() { return codeIntervalSeconds; }

    /**
     * Generates an {@code otpauth://} URI using Fusabase defaults.
     *
     * <p>Example: Given a secret returned by generateSecret; keep the generated URI private.</p>
     * <pre>{@code
     * String qrCodeUrl = secret.generateQrCodeUrl();
     * }</pre>
     *
     * @return a URI that can be displayed as a QR code or opened in an OTP app
     */
    public @NonNull String generateQrCodeUrl() {
        return generateQrCodeUrl(DEFAULT_ACCOUNT_NAME, DEFAULT_ISSUER);
    }

    /**
     * Generates an {@code otpauth://} URI for the supplied account and issuer.
     *
     * <p>Example: Given a secret returned by generateSecret; keep the generated URI private.</p>
     * <pre>{@code
     * String qrCodeUrl = secret.getQrCodeUrl("alex@example.com", "My App");
     * }</pre>
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
     * <p>Example: Given a secret returned by generateSecret; keep the generated URI private.</p>
     * <pre>{@code
     * String qrCodeUrl = secret.generateQrCodeUrl("alex@example.com", "My App");
     * }</pre>
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
     * <p>Example: Given a TotpSecret instance named secret.</p>
     * <pre>{@code
     * int codeLength = secret.getCodeLength();
     * }</pre>
     *
     * @return the TOTP code length
     */
    public int getCodeLength() { return codeLength; }

    /**
     * Returns the TOTP time-step length in seconds.
     *
     * <p>Example: Given a TotpSecret instance named secret.</p>
     * <pre>{@code
     * int codeIntervalSeconds = secret.getCodeIntervalSeconds();
     * }</pre>
     *
     * @return the TOTP code period
     */
    public int getCodeIntervalSeconds() { return codeIntervalSeconds; }

    /**
     * Returns the lifetime of the pending enrollment in seconds.
     *
     * <p>Example: Given a TotpSecret instance named secret.</p>
     * <pre>{@code
     * int enrollmentTtlSeconds = secret.getEnrollmentTtlSeconds();
     * }</pre>
     *
     * @return the enrollment time-to-live, or {@code 0} when the provider does not return one
     */
    public int getEnrollmentTtlSeconds() { return enrollmentTtlSeconds; }
    @NonNull MultiFactorSession getSession() { return session; }

    /**
     * Opens an {@code otpauth://} URI through an authenticator app chooser.
     *
     * <p>The method displays an Android chooser from the application context with
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK}. Select only a trusted authenticator: the selected
     * app receives the shared secret and can generate verification codes. The chooser does not
     * verify the trustworthiness of the app. If no OTP app handles the URI, it opens
     * the Play Store search page for compatible apps.
     *
     * <p>Example: Given a secret returned by generateSecret and, where needed, the foreground activity.</p>
     * <pre>{@code
     * secret.openInOtpApp(secret.generateQrCodeUrl());
     * }</pre>
     *
     * @param qrCodeUrl the {@code otpauth://} URI to open
     */
    public void openInOtpApp(@NonNull String qrCodeUrl) {
        Context context = FusabaseApp.getInstance().getApplicationContext();
        try {
            Intent intent = otpIntent(qrCodeUrl, context);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_FALLBACK_URL));
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(fallbackIntent);
        }
    }

    /**
     * Opens an {@code otpauth://} URI through an authenticator app chooser.
     *
     * <p>The selected app receives the shared secret and can generate verification codes.
     * Select only a trusted authenticator; the chooser does not verify app trustworthiness.
     *
     * <p>Example: Given a secret returned by generateSecret and, where needed, the foreground activity.</p>
     * <pre>{@code
     * secret.openInOtpApp(secret.generateQrCodeUrl(), "https://example.com/help/authenticator", activity);
     * }</pre>
     *
     * @param qrCodeUrl the {@code otpauth://} URI to open
     * @param fallbackUrl the URI to open when no OTP app handles the QR code URI
     * @param activity the activity used to launch the intent
     */
    public void openInOtpApp(@NonNull String qrCodeUrl, @NonNull String fallbackUrl,
                             @NonNull Activity activity) {
        try {
            activity.startActivity(otpIntent(qrCodeUrl, activity));
        } catch (ActivityNotFoundException e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)));
        }
    }

    /**
     * Generates and opens an {@code otpauth://} URI for the supplied issuer and account name.
     *
     * <p>The chooser shares the setup secret with the selected app. Select only a trusted
     * authenticator; the SDK does not verify the app's trustworthiness.
     *
     * <p>Example: Given a secret returned by generateSecret and, where needed, the foreground activity.</p>
     * <pre>{@code
     * secret.openInOtpAppForAccount("My App", "alex@example.com", activity);
     * }</pre>
     *
     * @param appName the issuer shown in the OTP app
     * @param accountName the account label shown in the OTP app
     * @param activity the activity used to launch the intent
     */
    public void openInOtpAppForAccount(@NonNull String appName, @NonNull String accountName,
                                       @NonNull Activity activity) {
        openInOtpApp(generateQrCodeUrl(accountName, appName), DEFAULT_FALLBACK_URL, activity);
    }

    @NonNull private static Intent otpIntent(@NonNull String qrCodeUrl, @NonNull Context context) {
        Intent target = new Intent(Intent.ACTION_VIEW, Uri.parse(qrCodeUrl));
        // Resolve the target, not the system chooser, so the no-handler fallback still works.
        if (target.resolveActivity(context.getPackageManager()) == null) {
            throw new ActivityNotFoundException("No OTP app is available.");
        }
        return Intent.createChooser(target,
                "Choose a trusted authenticator — it will receive your setup secret");
    }

    private static @NonNull String encode(@NonNull String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
}
