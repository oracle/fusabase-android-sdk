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

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oracle.mobile.fusabase.logger.FusabaseLogger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;

public class Utils {

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public static Uri prepareUriForDownload(@NonNull Context context,
                                            @NonNull Uri uri,
                                            @NonNull String fileName,
                                            @NonNull String mimeType) {
        ContentResolver resolver = context.getContentResolver();

        if (uriExists(context, uri)) {
            return uri;
        }

        if ("file".equals(uri.getScheme())) {
            File file = new File(Objects.requireNonNull(uri.getPath()));
            if (!file.exists()) {
                try {
                    boolean created = file.createNewFile();
                    if (created) {
                        return Uri.fromFile(file);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return Uri.fromFile(file);
        }

        if ("content".equals(uri.getScheme())) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

            return resolver.insert(MediaStore.Files.getContentUri("external"), values); // Return the new Uri
        }

        return null;
    }

    private static boolean uriExists(@NonNull Context context, @NonNull Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            return inputStream != null;
        } catch (IOException e) {
            return false;
        }
    }

    public static Date convertStringToDate (@NonNull String timestampString){

        if (timestampString.contains(".")) {
            int dotIndex = timestampString.indexOf(".");
            timestampString = timestampString.substring(0, dotIndex + 4);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        LocalDateTime ldt = LocalDateTime.parse(timestampString, formatter);

        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static String addQueryParameterToURL(@NonNull String URL, @NonNull String... PARAMS) {

        StringBuilder updatedURL = new StringBuilder(new String(URL));
        updatedURL = new StringBuilder(updatedURL.charAt(updatedURL.length() - 1) == '/' ?
                updatedURL.substring(0, updatedURL.length() - 2) : updatedURL.toString());

        if(!URL.contains("?"))
            updatedURL.append('?');

        for (int i = 0; i < PARAMS.length; i += 2) {
            if (updatedURL.charAt(updatedURL.length() - 1) != '?')
                updatedURL.append('&');
            updatedURL.append(PARAMS[i])
                    .append('=')
                    .append(PARAMS[i + 1]);
        }

        return updatedURL.toString();
    }

    public static String urlBuilder(@NonNull String... PATHS) {

        String URL = "";

        for (String path : PATHS) {
            path = URL.isEmpty() ? path : path.charAt(0) == '/' ? path : "/" + path;
            URL = URL.concat(path);
            URL = URL.charAt(URL.length() - 1) == '/' ? URL.substring(0, URL.length() - 1) : URL;
        }

        return URL;
    }

    /**
     * Get or create an AES secret key in Android KeyStore for the given alias
     */
    private static SecretKey getOrCreateSecretKey(@NonNull String alias) throws GeneralSecurityException, IOException {
        try {
            // Check if Android KeyStore is available
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            // Check if key already exists
            if (keyStore.containsAlias(alias)) {
                return (SecretKey) keyStore.getKey(alias, null);
            }

            // Generate new AES key using Android KeyStore
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEY_STORE);

            KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(AES_KEY_SIZE)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build();

            keyGenerator.init(keySpec);
            return keyGenerator.generateKey();
        } catch (KeyStoreException e) {
            throw new GeneralSecurityException("Android KeyStore not available: " + e.getMessage(), e);
        } catch (Exception e) {
            FusabaseLogger.e("Utils", "Failed to create or retrieve Android KeyStore key for alias '" + alias + "': " + e.getMessage());
            throw new GeneralSecurityException("Android KeyStore operation failed for alias '" + alias + "': " + e.getMessage(), e);
        }
    }

    /**
     * Encrypt data using AES-GCM with Android KeyStore
     */
    private static String encryptData(@NonNull String data, @NonNull String keyAlias) throws GeneralSecurityException, IOException {
        if (data.isEmpty()) {
            return data; // Don't encrypt empty strings
        }

        SecretKey secretKey = getOrCreateSecretKey(keyAlias);
        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();

        // Combine IV and encrypted data
        byte[] combined = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, combined, GCM_IV_LENGTH, encryptedData.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt data using AES-GCM with Android KeyStore
     */
    private static String decryptData(@NonNull String encryptedData, @NonNull String keyAlias) throws GeneralSecurityException, IOException {
        if (encryptedData.isEmpty()) {
            return encryptedData; // Don't decrypt empty strings
        }

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedData);
            if (combined.length < GCM_IV_LENGTH) {
                // Not encrypted data, return as-is (backward compatibility)
                return encryptedData;
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKey secretKey = getOrCreateSecretKey(keyAlias);
            Cipher cipher = Cipher.getInstance(AES_MODE);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decryptedData = cipher.doFinal(encrypted);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // If decryption fails, assume it's unencrypted data (backward compatibility)
            FusabaseLogger.w("Failed to decrypt data, assuming unencrypted: " + e.getMessage());
            return encryptedData;
        }
    }

    /**
     * Generate a unique key alias for each preference file
     */
    private static String getKeyAlias(@NonNull String prefsName) {
        return "fusabase_prefs_" + prefsName.hashCode();
    }

    /**
     * Compress data using GZIP
     */
    private static byte[] compressData(@NonNull byte[] data) throws IOException {
        if (data.length < 2048) { // Don't compress small data
            return data;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            gzipOutputStream.write(data);
        }
        return outputStream.toByteArray();
    }

    /**
     * Decompress data using GZIP
     */
    private static byte[] decompressData(@NonNull byte[] compressedData) throws IOException {
        // Check if data is compressed (starts with GZIP magic bytes)
        if (compressedData.length < 2 || compressedData[0] != 31 || compressedData[1] != -117) {
            return compressedData; // Not compressed
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return outputStream.toByteArray();
    }

    public static void savePreferenceData(@NonNull Context context,
                                          @NonNull String token,
                                          @NonNull String tokenKey,
                                          @NonNull String prefsName) throws GeneralSecurityException, IOException {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsName, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Encrypt the token before storing
        String encryptedToken = encryptData(token, getKeyAlias(prefsName));
        editor.putString(tokenKey, encryptedToken);
        editor.apply();
    }

    public static String getPreferenceData(@NonNull Context context,
                                           @NonNull String tokenKey,
                                           @NonNull String prefsName) throws GeneralSecurityException, IOException {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsName, MODE_PRIVATE);
        String encryptedToken = sharedPreferences.getString(tokenKey, "");

        // Decrypt the token before returning
        return decryptData(encryptedToken, getKeyAlias(prefsName));
    }

    public static void deletePreferenceData(@NonNull Context context,
                                            @NonNull String tokenKey,
                                            @NonNull String prefsName) throws GeneralSecurityException, IOException {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsName, MODE_PRIVATE);
        sharedPreferences.edit().remove(tokenKey).commit();
        sharedPreferences.edit().apply();
    }

    public static void clearDataWithBaseKey ( @NonNull Context context,
                                              @NonNull String baseKey,
                                              @NonNull String prefsName)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsName, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Map<String, ?> allPrefs = sharedPreferences.getAll();

        for (String key : allPrefs.keySet()) {
            if (key.startsWith(baseKey + "$") || key.equals(baseKey)) {
                editor.remove(key);
            }
        }

        editor.apply();
    }
    public static void saveJsonObjectToPrefs(@NonNull Context context,
                                             @NonNull String baseKey,
                                             @NonNull JsonObject jsonObject,
                                             @NonNull String prefsName) throws GeneralSecurityException, IOException {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsName, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Serialize the entire JSON object to string
        String jsonString = jsonObject.toString();

        // Compress if large enough
        byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);
        byte[] compressedBytes = compressData(jsonBytes);
        String dataToEncrypt = Base64.getEncoder().encodeToString(compressedBytes);

        // Encrypt the serialized data
        String encryptedData = encryptData(dataToEncrypt, getKeyAlias(prefsName));

        // Store as single encrypted blob
        editor.putString(baseKey, encryptedData);
        editor.apply();
    }

    public static JsonObject loadJsonObjectFromPreferences(@NonNull Context context,
                                                           @NonNull String baseKey,
                                                           @NonNull String prefsName) throws GeneralSecurityException, IOException {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsName, MODE_PRIVATE);

        // Get the encrypted blob
        String encryptedData = sharedPreferences.getString(baseKey, "");
        if (encryptedData.isEmpty()) {
            // Return empty JSON object if no data exists
            return Json.createObjectBuilder().build();
        }

        // Decrypt the data
        String decryptedData = decryptData(encryptedData, getKeyAlias(prefsName));

        // Decode from Base64 and decompress if needed
        byte[] compressedBytes = Base64.getDecoder().decode(decryptedData);
        byte[] jsonBytes = decompressData(compressedBytes);
        String jsonString = new String(jsonBytes, StandardCharsets.UTF_8);

        // Parse JSON
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }



    public static String getValueType(Object value) {
        if (value == null) {
            return "nullValue";
        } else if (value instanceof Integer || value instanceof Long || value instanceof Short) {
            return "integerValue";
        } else if (value instanceof Double || value instanceof Float) {
            return "doubleValue";
        } else if (value instanceof String || value instanceof Character) {
            return ((String) value).
                    matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}$")
                    ? "timestampValue" : "stringValue";
        } else if (value instanceof Boolean) {
            return "booleanValue";
        } else if (value instanceof Byte) {
            return "byteValue";
        } else if (value instanceof List) {
            return "arrayValue";
        } else if (value instanceof Map) {
            return "mapValue";
        } else {
            return "unknownValue";
        }
    }

    public static String getUniqueId () {
        return UUID.randomUUID().toString();
    }

    @NonNull
    public static String getUniqueTransactionName() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static boolean isTokenValid (@NonNull String token) {
        JsonObject tokenStructure = (JsonObject) parseJWT(token);
        long expirationTime = new BigDecimal(tokenStructure.getJsonNumber("exp").toString()).setScale(0, RoundingMode.HALF_UP).longValue();
        return expirationTime >= Instant.now().toEpochMilli() / 1000 + 60 * 5;
    }

    public static JsonStructure parseJWT(@NonNull String token) {
        try {
            // Split the JWT into parts
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            String base64Url = parts[1];

            // Replace URL-safe characters
            String base64 = base64Url.replace('-', '+').replace('_', '/');

            // Decode Base64
            byte[] decodedBytes = Base64.getDecoder().decode(base64);

            // Convert to UTF-8 String
            String jsonPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            JsonReader jsonReader = Json.createReader(new StringReader(jsonPayload));

            return jsonReader.readObject();
        } catch (Exception e) {
            FusabaseLogger.e("Unable to parse the JWT token, the following exception occured " +
                    e.getMessage());
            throw new RuntimeException("Failed to parse JWT", e);
        }
    }

    /**
     * Securely clears a char array by overwriting its contents.
     * Use this for sensitive data like passwords to prevent heap dump exposure.
     *
     * @param array The char array to clear
     */
    public static void clearCharArray(@Nullable char[] array) {
        if (array != null) {
            Arrays.fill(array, '\0');
        }
    }

    /**
     * Securely clears a StringBuilder by overwriting and clearing its contents.
     *
     * @param builder The StringBuilder to clear
     */
    public static void clearStringBuilder(@Nullable StringBuilder builder) {
        if (builder != null) {
            builder.setLength(0);
        }
    }

    public static String getConfigValue ( @NonNull Context context, @NonNull String propertyName) {
        int resId = context.getResources().getIdentifier(propertyName,
                "string",
                context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        } else {
            throw new Resources.NotFoundException("Cannot find the resource : " + propertyName);
        }
    }
}
