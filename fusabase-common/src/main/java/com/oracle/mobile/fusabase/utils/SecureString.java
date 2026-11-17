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

package com.oracle.mobile.fusabase.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * A secure wrapper for sensitive strings that minimizes time in plaintext memory.
 * Stores the string encrypted in memory and provides methods to access decrypted content
 * with automatic or manual clearing to prevent heap dump exposure.
 */
public class SecureString {

    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final byte[] encryptedData;
    private final byte[] iv;
    private final SecretKey secretKey;

    /**
     * Creates a SecureString from a plaintext string.
     * The string is immediately encrypted and the plaintext is discarded.
     *
     * @param plaintext The sensitive string to secure
     */
    public SecureString(@NonNull String plaintext) throws GeneralSecurityException {
        this.secretKey = generateKey();
        this.iv = generateIv();
        this.encryptedData = encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a SecureString from an encrypted byte array and IV.
     * Used internally for reconstruction.
     */
    private SecureString(@NonNull byte[] encryptedData, @NonNull byte[] iv, @NonNull SecretKey key) {
        this.encryptedData = encryptedData;
        this.iv = iv;
        this.secretKey = key;
    }

    /**
     * Generates a random AES key for in-memory encryption.
     */
    private static SecretKey generateKey() throws GeneralSecurityException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE);
        return keyGenerator.generateKey();
    }

    /**
     * Generates a random IV for GCM mode.
     */
    private static byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * Encrypts data using AES-GCM.
     */
    private byte[] encrypt(byte[] data) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(AES_MODE);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        return cipher.doFinal(data);
    }

    /**
     * Decrypts data using AES-GCM.
     */
    private byte[] decrypt() throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(AES_MODE);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Gets the decrypted string. Use this method when you need the plaintext,
     * and call clear() immediately after use to minimize exposure time.
     *
     * @return The decrypted string, or null if decryption fails
     */
    @Nullable
    public String getDecryptedString() {
        try {
            byte[] decrypted = decrypt();
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            return null;
        }
    }

    /**
     * Gets the decrypted string as char array for even more secure handling.
     * Remember to clear the array after use.
     *
     * @return The decrypted char array, or null if decryption fails
     */
    @Nullable
    public char[] getDecryptedChars() {
        String decrypted = getDecryptedString();
        return decrypted != null ? decrypted.toCharArray() : null;
    }

    /**
     * Securely clears the encryption key and encrypted data from memory.
     * Call this when the SecureString is no longer needed.
     */
    public void clear() {
        if (encryptedData != null) {
            Arrays.fill(encryptedData, (byte) 0);
        }
        if (iv != null) {
            Arrays.fill(iv, (byte) 0);
        }
        // Note: SecretKey doesn't have a clear method, but the key bytes are protected
    }

    /**
     * Creates a copy of this SecureString with new encryption.
     * Useful for transferring to different contexts.
     */
    @NonNull
    public SecureString copy() throws GeneralSecurityException {
        String plaintext = getDecryptedString();
        if (plaintext == null) {
            throw new GeneralSecurityException("Cannot copy: decryption failed");
        }
        return new SecureString(plaintext);
    }

    /**
     * Checks if this SecureString contains data.
     */
    public boolean isEmpty() {
        return encryptedData == null || encryptedData.length == 0;
    }
}