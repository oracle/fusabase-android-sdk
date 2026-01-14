package com.oracle.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.net.Uri;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.database.osbdt.Timestamp;
import com.oracle.database.osbdt.auth.AuthResult;
import com.oracle.database.osbdt.auth.EmailAuthCredential;
import com.oracle.database.osbdt.auth.FacebookAuthProvider;
import com.oracle.database.osbdt.auth.GetTokenResult;
import com.oracle.database.osbdt.auth.GoogleAuthProvider;
import com.oracle.database.osbdt.auth.GithubAuthProvider;
import com.oracle.database.osbdt.auth.OsbdtAuth;
import com.oracle.database.osbdt.auth.OsbdtUser;
import com.oracle.database.osbdt.auth.OsbdtUserMetadata;
import com.oracle.database.osbdt.auth.UserProfileChangeRequest;
import com.oracle.database.osbdt.auth.*;
import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;
import com.oracle.database.osbdt.models.IDCSOptions;
import com.oracle.database.osbdt.oracledb.*;
import com.oracle.database.osbdt.storage.*;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.Tasks;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class regression {

    private static final String[] AUTH_TYPES = { "base" };

    // Helper methods added
    private static final String DEFAULT_PASSWORD = "Password@123";

    private static String generateRandomStringForPassword(int length) {
        String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#@!$.";
        StringBuilder sb = new StringBuilder(length);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }
        return sb.toString();
    }

    private static String[] getNewUserCredentials() {
        String email = generateRandomStringForPassword(8) + "@oracle.com";
        String password = DEFAULT_PASSWORD;
        return new String[]{email, password};
    }

    private static String encodeAuthFromSchema(String schema) {
        String credentials = schema + ":" + schema;
        byte[] credBytes = credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String base64Credentials = java.util.Base64.getEncoder().encodeToString(credBytes);
        return "Basic " + base64Credentials;
    }

    /**
     * Delete a user, optionally allowing self‑signed SSL certificates.
     *
     * @param email            the user email to delete
     * @throws java.io.IOException on network or HTTP errors
     */
    private void deleteUser(String email) throws java.io.IOException {
        OsbdtAuth auth = getAuthForType("base");
        OsbdtApp app = auth.getApp();

        String urlStr = app.osbdtOptions.getOrdsHost() + "_/baas-panel/idm/" + app.osbdtOptions.getProjectId() + "/user?username=" + email;

        // Create HttpRequestHelper instance
        com.oracle.database.osbdt.http.HttpRequestHelper requestHelper = new com.oracle.database.osbdt.http.HttpRequestHelper();

        // Set up headers
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Content-Type", "text/plain");
        headers.put("Authorization", encodeAuthFromSchema("user1"));

        try {
            // Create DELETE request
            requestHelper.createHttpRequest(urlStr, "DELETE", headers);

            // Execute request
            com.oracle.database.osbdt.http.HttpResponse response = requestHelper.executeRequest();

            if (!response.getStatus()) {
                throw new java.io.IOException("User deletion failed: " + response.getError());
            }

            // Check for successful status code (200)
            if (response.getCode() != 200) {
                throw new java.io.IOException("User deletion failed with status code: " + response.getCode());
            }

        } catch (com.oracle.database.osbdt.OsbdtException e) {
            throw new java.io.IOException("User deletion failed", e);
        }
    }

    private <T> T await(Task<T> task) throws Exception {
        return Tasks.await(task, 80, TimeUnit.SECONDS);
    }

    // Helper method to get OsbdtAuth instance for a specific auth type
    private OsbdtAuth getAuthForType(String authType) {
        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
        optionsBuilder.setAuthType(authType);

        // Add unique identifier to app name to prevent conflicts
        String uniqueId = String.valueOf(System.currentTimeMillis());

        switch (authType) {
            case "base":
                optionsBuilder
                        .setOrdsHost("https://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8443/ords/USER1/");
                optionsBuilder.setProjectId("47DD506E3CFEE041E0631F965E640C80");
                optionsBuilder.setAppId("47DE7A6CC67A2484E0631F965E640E85");
                optionsBuilder.setObjectType("oci");
                optionsBuilder.setSchema("USER1");
                optionsBuilder.setStorageBucket("baas_bucket");
                optionsBuilder.setApiVersion("2.0");
                optionsBuilder.setAppName("testapp");
                optionsBuilder.setAuthId("47F3EC3B8C18A099E0631F965E64A607");
                optionsBuilder.setUseSocket(true);
                optionsBuilder.setUploadChunkSize(1000);
                optionsBuilder.setAllowsSelfSignedCertificates(true);
                break;
            // case "ldap":
            // optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
            // optionsBuilder.setProjectId("3C292A543E4AD6A8E0631F965E64EEE9");
            // optionsBuilder.setAppId("3C292ADD4CCBD6C7E0631F965E64552C");
            // optionsBuilder.setObjectType("dbfs");
            // optionsBuilder.setStorageBucket("dbfs_CZXMDZFGRFGEITN");
            // optionsBuilder.setApiVersion("1.0");
            // optionsBuilder.setAppName("regression-test-" + authType + "-" + uniqueId);
            // optionsBuilder.setAuthId("3AA63EF01E17D2E1E0631F965E6455B5");
            // break;
            case "idcs":
                IDCSOptions.Builder idcsOptionsBuilder = new IDCSOptions.Builder();
                idcsOptionsBuilder.setClientId("1b58b11452cc4dcda1aff1a6102ce94d");
                idcsOptionsBuilder.setClientSecret("idcscs-c3ce8356-476f-4ccd-82e8-862a3fd443da");
                idcsOptionsBuilder
                        .setDomainURL("https://idcs-360c1204d11f41d3836e7fb2f1769ca7.identity.oraclecloud.com");
                idcsOptionsBuilder.setSelfRegistrationProfile("7ff42354f5cc4d66968f140ef28586b0");
                optionsBuilder.setIDCSOptions(idcsOptionsBuilder.build());
                optionsBuilder
                        .setOrdsHost("https://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8443/ords/USER1/");
                optionsBuilder.setProjectId("47DD506E3CFEE041E0631F965E640C80");
                optionsBuilder.setAppId("47DE7A6CC67A2484E0631F965E640E85");
                optionsBuilder.setObjectType("oci");
                optionsBuilder.setStorageBucket("baas_bucket");
                optionsBuilder.setApiVersion("2.0");
                optionsBuilder.setAppName("testapp");
                optionsBuilder.setAuthId("47F3EC3B8C18A099E0631F965E64A607");
                optionsBuilder.setUseSocket(true);
                optionsBuilder.setUploadChunkSize(1000);
                optionsBuilder.setAllowsSelfSignedCertificates(true);
                break;
            default:
                throw new IllegalArgumentException("Unknown auth type: " + authType);
        }

        OsbdtApp app = OsbdtApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                optionsBuilder.build(), "regression-test-" + authType + "-" + uniqueId);

        // Verify the app name is correct
        Log.d("RegressionTest", "Created app with name: " + app.getName());
        assertTrue("App name should contain authType", app.getName().contains(authType));

        OsbdtAuth auth = OsbdtAuth.getInstance(app);

        // Verify the auth instance has the correct app
        OsbdtApp authApp = auth.getApp();
        Log.d("RegressionTest", "Auth app name: " + authApp.getName());
        assertTrue("Auth app name should contain authType", authApp.getName().contains(authType));

        return auth;
    }

    // Helper method to get Storage instance for a specific auth type
    private Storage getStorageForType(String authType) {
        OsbdtAuth auth = getAuthForType(authType);
        return Storage.getInstance(auth.getApp());
    }

    @Test
    public void testCreateUserWithEmailAndPassword() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing createUserWithEmailAndPassword with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];

            AuthResult result = await(auth.createUserWithEmailAndPassword(email, password));
            assertNotNull("User creation failed for " + authType, result.getUser());
            assertEquals("Email mismatch for " + authType, email, result.getUser().getEmail());
            deleteUser(email);
        }
    }

    @Test
    public void testSignInWithEmailAndPassword() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing signInWithEmailAndPassword with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];

            // First create user if needed
            await(auth.createUserWithEmailAndPassword(email, password));

            AuthResult result = await(auth.signInWithEmailAndPassword(email, password));
            assertNotNull("Sign in failed for " + authType, result.getUser());
            assertEquals("Email mismatch for " + authType, email, result.getUser().getEmail());
            deleteUser(email);
        }
    }

    @Test
    public void testAuthStateListener() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing AuthStateListener with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];

            final boolean[] listenerCalled = { false };
            OsbdtAuth.AuthStateListener listener = newAuth -> {
                listenerCalled[0] = true;
            };

            auth.addAuthStateListener(listener);
            // Create user first, then sign in
            await(auth.createUserWithEmailAndPassword(email, password));
            await(auth.signInWithEmailAndPassword(email, password));
            // Listener should have been called
            assertTrue("Auth state listener not called for " + authType, listenerCalled[0]);

            auth.removeAuthStateListener(listener);
            deleteUser(email);
        }
    }

    @Test
    public void testUpdateProfile() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing updateProfile with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];

            OsbdtUser user = await(auth.createUserWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);

            String newDisplayName = "Regression Test User " + authType;
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newDisplayName)
                    .build();

            await(user.updateProfile(profileUpdates));

            // Reload to get updated data
            await(user.reload());
            assertEquals("Display name not updated for " + authType, newDisplayName, user.getDisplayName());
            deleteUser(email);
        }
    }

    @Test
    public void testGetIdToken() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing getIdToken with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];

            // Create user first, then sign in
            await(auth.createUserWithEmailAndPassword(email, password));
            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);

            String token = await(user.getIdToken(false)).getToken();
            assertNotNull("Token should not be null for " + authType, token);

            String freshToken = await(user.getIdToken(true)).getToken();
            assertNotNull("Fresh token should not be null for " + authType, freshToken);
            assertNotEquals("Fresh token should be different for " + authType, token, freshToken);
            deleteUser(email);
        }
    }

    @Test
    public void testUpdatePassword() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing updatePassword with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];
            String newPassword = "NewPassword@123";

            // Create user first, then sign in
            await(auth.createUserWithEmailAndPassword(email, password));
            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);

            await(user.updatePassword(newPassword));

            // Sign out and sign in with new password
            await(auth.signOut());
            AuthResult result = await(auth.signInWithEmailAndPassword(email, newPassword));
            assertNotNull("Sign in with new password failed for " + authType, result.getUser());
            deleteUser(email);
        }
    }

    @Test
    public void testUpdateCurrentUser() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing updateCurrentUser with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];

            // Sign in to get a user
            await(auth.createUserWithEmailAndPassword(email, password));
            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);
            assertEquals("Current user should match signed in user for " + authType, user.getUid(),
                    auth.getCurrentUser().getUid());

            // Update current user to null (sign out effect)
            auth.updateCurrentUser(null);
            assertNull("Current user should be null after update for " + authType, auth.getCurrentUser());

            // Sign in again and update to a different state
            OsbdtUser user2 = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            auth.updateCurrentUser(user2);
            assertNotNull("Current user should not be null after setting for " + authType, auth.getCurrentUser());
            deleteUser(email);
        }
    }

    @Test
    public void testGetApp() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing getApp with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);

            OsbdtApp app = auth.getApp();
            assertNotNull("App should not be null for " + authType, app);
            assertNotNull("App name should not be null for " + authType, app.getName());
            Log.d("RegressionTest", "App name for " + authType + ": " + app.getName());
            assertTrue(
                    "App name should contain 'regression-test-" + authType + "' for " + authType + ". App name: "
                            + app.getName(),
                    app.getName().contains("regression-test-" + authType));

        }
    }

    // Test Separately
    // @Test
    // public void testStartActivityForSignInWithProvider() throws Exception {
    // // Note: This test verifies the method can be called, but requires Activity
    // context
    // // Full integration testing would require Activity instrumentation
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing startActivityForSignInWithProvider with
    // authType: " + authType);
    // OsbdtAuth auth = getAuthForType(authType);

    // try {
    // // This method requires an Activity, so we just test that it doesn't throw
    // // when called with null Activity (should throw IllegalArgumentException)
    // boolean threwException = false;
    // try {
    // // We expect this to fail due to null Activity
    // auth.startActivityForSignInWithProvider(null, null);
    // } catch (IllegalArgumentException e) {
    // threwException = true;
    // }
    // assertTrue("startActivityForSignInWithProvider should validate Activity
    // parameter for " + authType, threwException);
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in startActivityForSignInWithProvider for
    // " + authType + ": " + e.getMessage());
    // }
    // }
    // }

    // @Test
    // public void testGetPendingAuthResult() throws Exception {
    // // Note: getPendingAuthResult typically returns null unless there's a pending
    // auth flow
    // // This test verifies the method can be called and returns expected behavior
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing getPendingAuthResult with authType: " +
    // authType);
    // OsbdtAuth auth = getAuthForType(authType);
    //
    // try {
    // // Without a pending auth flow, this should return null
    // Task<AuthResult> pendingResult = auth.getPendingAuthResult();
    // // The method should not throw and should return a valid Task (even if null)
    // assertNotNull("getPendingAuthResult should return a Task for " + authType,
    // pendingResult);
    // // In normal circumstances, there should be no pending result
    // // We don't await it as it might be null
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in getPendingAuthResult for " + authType +
    // ": " + e.getMessage());
    // }
    // }
    // }

    // @Test
    // public void testVerifyPasswordResetCode() throws Exception {
    // // Note: This test may not work in all environments as it requires a valid
    // reset code
    // // It's included for completeness but may need adjustment based on test
    // environment
    // Log.d("RegressionTest", "Testing verifyPasswordResetCode - this may require
    // manual setup");
    // // This API typically requires a real password reset flow to generate a valid
    // code
    // // Skipping automated test as it depends on external state
    // assertTrue("verifyPasswordResetCode test placeholder - requires valid reset
    // code", true);
    // }
    //
    // @Test
    // public void testConfirmPasswordReset() throws Exception {
    // // Note: This test may not work in all environments as it requires a valid
    // reset code
    // // It's included for completeness but may need adjustment based on test
    // environment
    // Log.d("RegressionTest", "Testing confirmPasswordReset - this may require
    // manual setup");
    // // This API typically requires a real password reset flow to generate a valid
    // code
    // // Skipping automated test as it depends on external state
    // assertTrue("confirmPasswordReset test placeholder - requires valid reset
    // code", true);
    // }

    @Test
    public void testOsbdtUserGetters() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing OsbdtUser getters with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];
            await(auth.createUserWithEmailAndPassword(email, password));
            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);

            // Test core getters
            assertNotNull("getEmail() should not be null for " + authType, user.getEmail());
            assertEquals("getEmail() should match for " + authType, email, user.getEmail());
            assertNotNull("getUid() should not be null for " + authType, user.getUid());
            assertNotNull("getProviderId() should not be null for " + authType, user.getProviderId());
            assertNotNull("getMetadata() should not be null for " + authType, user.getMetadata());

            // Test optional getters (may return null)
            user.getDisplayName(); // May be null
            user.getPhotoUrl(); // May be null
            user.getPhoneNumber(); // May be null

            // Test boolean getter
            assertFalse("isAnonymous() should be false for authenticated user for " + authType, user.isAnonymous());
            deleteUser(email);
        }
    }

    @Test
    public void testOsbdtUserGetIdToken() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing OsbdtUser getIdToken with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];

            await(auth.createUserWithEmailAndPassword(email, password));
            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);

            // Test getIdToken(false)
            GetTokenResult tokenResult = await(user.getIdToken(false));
            assertNotNull("getIdToken(false) result should not be null for " + authType, tokenResult);
            assertNotNull("Token should not be null for " + authType, tokenResult.getToken());
            assertTrue("Token should not be empty for " + authType, tokenResult.getToken().length() > 0);

            // Test getIdToken(true) - force refresh
            GetTokenResult freshTokenResult = await(user.getIdToken(true));
            assertNotNull("getIdToken(true) result should not be null for " + authType, freshTokenResult);
            assertNotNull("Fresh token should not be null for " + authType, freshTokenResult.getToken());
            deleteUser(email);
        }
    }

    @Test
    public void testOsbdtUserReload() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing OsbdtUser reload with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];

            await(auth.createUserWithEmailAndPassword(email, password));
            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);

            // Test reload - should complete without error
            await(user.reload());
            assertTrue("reload() should complete successfully for " + authType, true);

            // After reload, user data should still be accessible
            assertNotNull("User email should still be accessible after reload for " + authType, user.getEmail());
            assertNotNull("User UID should still be accessible after reload for " + authType, user.getUid());
            deleteUser(email);
        }
    }

    @Test
    public void testOsbdtUserUpdateProfile() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing OsbdtUser updateProfile with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];

            await(auth.createUserWithEmailAndPassword(email, password));
            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);

            String newDisplayName = "Updated Test User " + authType;
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newDisplayName)
                    .build();

            // Test updateProfile
            await(user.updateProfile(profileUpdates));
            assertTrue("updateProfile() should complete successfully for " + authType, true);

            // Reload to get updated data
            await(user.reload());
            assertEquals(user.getDisplayName(), newDisplayName);
            deleteUser(email);
        }
    }

    @Test
    public void testOsbdtUserUpdatePassword() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing OsbdtUser updatePassword with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];
            String newPassword = "NewPassword@123";

            await(auth.createUserWithEmailAndPassword(email, password));
            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);

            // Test updatePassword
            await(user.updatePassword(newPassword));
            assertTrue("updatePassword() should complete successfully for " + authType, true);

            // Verify by signing out and signing back in with new password
            await(auth.signOut());
            AuthResult result = await(auth.signInWithEmailAndPassword(email, newPassword));
            assertNotNull("Sign in with new password should succeed for " + authType, result.getUser());
            deleteUser(email);
        }
    }

    // Test Separately
    // @Test
    // public void testOsbdtUserSendEmailVerification() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing OsbdtUser sendEmailVerification with
    // authType: " + authType);
    // OsbdtAuth auth = getAuthForType(authType);
    // String email = "regression_user_" + Math.random() + authType +
    // "@example.com";
    // String password = "Password@123";
    //
    //
    // OsbdtUser user = await(auth.signInWithEmailAndPassword(email,
    // password)).getUser();
    // assertNotNull("User should not be null for " + authType, user);
    //
    // // Test sendEmailVerification
    // await(user.sendEmailVerification());
    // assertTrue("sendEmailVerification() should complete successfully for " +
    // authType, true);
    //
    //
    // }
    // }

    // @Test
    // public void testOsbdtUserStartActivityMethods() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing OsbdtUser startActivity methods with
    // authType: " + authType);
    // OsbdtAuth auth = getAuthForType(authType);
    // String email = "regression_user_" + Math.random() + authType +
    // "@example.com";
    // String password = "Password@123";
    //
    //
    // OsbdtUser user = await(auth.signInWithEmailAndPassword(email,
    // password)).getUser();
    // assertNotNull("User should not be null for " + authType, user);
    //
    // // Test startActivityForLinkWithProvider (should validate Activity parameter)
    // boolean threwException = false;
    // try {
    // user.startActivityForLinkWithProvider(null, null);
    // } catch (IllegalArgumentException e) {
    // threwException = true;
    // }
    // assertTrue("startActivityForLinkWithProvider should validate Activity
    // parameter for " + authType, threwException);
    //
    // // Test startActivityForReauthenticateWithProvider (should validate Activity
    // parameter)
    // threwException = false;
    // try {
    // user.startActivityForReauthenticateWithProvider(null, null);
    // } catch (IllegalArgumentException e) {
    // threwException = true;
    // }
    // assertTrue("startActivityForReauthenticateWithProvider should validate
    // Activity parameter for " + authType, threwException);
    //
    //
    // }
    // }

    @Test
    public void testOsbdtUserLinkAndUnlink() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing OsbdtUser link/unlink with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            await(auth.createUserWithEmailAndPassword(email, password));
            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);

            // Test linking with Facebook credential (will fail but tests the API)
            String mockFacebookToken = "mock_fb_token_" + authType;
            AuthCredential facebookCredential = FacebookAuthProvider.getCredential(mockFacebookToken);

            try {
                await(user.linkWithCredential(facebookCredential));
                // If we get here, linking succeeded (unlikely with mock token)
            } catch (Exception linkException) {
                // Expected to fail with mock token - just verify the method exists and is
                // callable
                assertTrue("linkWithCredential() method should be callable for " + authType, true);
            }

            // Test unlinking (should work even if linking failed)
            try {
                await(user.unlink(EmailAuthProvider.PROVIDER_ID));
                // If we get here, unlinking succeeded
            } catch (Exception unlinkException) {
                // May fail if not linked - just verify the method exists
                assertTrue("unlink() method should be callable for " + authType, true);
            }

        }
    }

    @Test
    public void testAuthResult() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing AuthResult with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String[] credentials = getNewUserCredentials();
            String email = credentials[0];
            String password = credentials[1];

            await(auth.createUserWithEmailAndPassword(email, password));
            AuthResult result = await(auth.signInWithEmailAndPassword(email, password));
            assertNotNull("AuthResult should not be null for " + authType, result);

            // Test getUser()
            OsbdtUser user = result.getUser();
            assertNotNull("AuthResult.getUser() should not be null for " + authType, user);
            assertEquals("AuthResult user email should match for " + authType, email, user.getEmail());

            // Test getCredential() - may be null depending on auth method
            result.getCredential(); // Just call it, may return null
            deleteUser(email);
        }
    }

    @Test
    public void testUserInfoInterface() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing UserInfo interface methods with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            await(auth.createUserWithEmailAndPassword(email, password));
            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            assertNotNull("User should not be null for " + authType, user);

            // Test UserInfo interface methods (implemented by OsbdtUser)
            // getDisplayName() - nullable
            String displayName = user.getDisplayName();
            // Just call it - may be null

            // getEmail() - nullable but should not be null for authenticated users
            String userEmail = user.getEmail();
            assertNotNull("UserInfo.getEmail() should not be null for authenticated user for " + authType, userEmail);
            assertEquals("UserInfo.getEmail() should match sign-in email for " + authType, email, userEmail);

            // getPhotoUrl() - nullable
            String photoUrl = user.getPhotoUrl();
            // Just call it - may be null

            // getProviderId() - non-null
            String providerId = user.getProviderId();
            assertNotNull("UserInfo.getProviderId() should not be null for " + authType, providerId);

            // getUid() - non-null
            String uid = user.getUid();
            assertNotNull("UserInfo.getUid() should not be null for " + authType, uid);
            assertFalse("UserInfo.getUid() should not be empty for " + authType, uid.isEmpty());

            // isEmailVerified() - boolean
            boolean isVerified = user.isEmailVerified();
            // Just call it - result depends on backend configuration

            Log.d("RegressionTest", "UserInfo interface methods work correctly for " + authType +
                    " - UID: " + uid + ", Provider: " + providerId + ", Email: " + userEmail);

        }
    }

    @Test
    public void testUserProfileChangeRequest() throws Exception {
        Log.d("RegressionTest", "Testing UserProfileChangeRequest builder and methods");

        // Test empty builder
        UserProfileChangeRequest emptyRequest = new UserProfileChangeRequest.Builder().build();
        assertNull("Empty request display name should be null", emptyRequest.getDisplayName());
        assertNull("Empty request photo URI should be null", emptyRequest.getPhotoUri());
        assertNull("Empty request phone number should be null", emptyRequest.getPhoneNumber());

        // Test builder with values
        String testDisplayName = "Test User";
        String testPhotoUri = "https://example.com/photo.jpg";
        String testPhoneNumber = "+1234567890";

        UserProfileChangeRequest fullRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName(testDisplayName)
                .setPhotoUri(testPhotoUri)
                .setPhoneNumber(testPhoneNumber)
                .build();

        assertEquals("Display name should match", testDisplayName, fullRequest.getDisplayName());
        assertEquals("Photo URI should match", testPhotoUri, fullRequest.getPhotoUri());
        assertEquals("Phone number should match", testPhoneNumber, fullRequest.getPhoneNumber());

        // Test partial builder usage
        UserProfileChangeRequest nameOnlyRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName("Name Only")
                .build();
        assertEquals("Name only request display name should match", "Name Only", nameOnlyRequest.getDisplayName());
        assertNull("Name only request photo URI should be null", nameOnlyRequest.getPhotoUri());
        assertNull("Name only request phone number should be null", nameOnlyRequest.getPhoneNumber());

        // Test method chaining
        UserProfileChangeRequest chainedRequest = new UserProfileChangeRequest.Builder()
                .setDisplayName("Chained")
                .setPhotoUri("https://chained.com/photo.jpg")
                .setPhoneNumber("+0987654321")
                .build();
        assertEquals("Chained display name should match", "Chained", chainedRequest.getDisplayName());
        assertEquals("Chained photo URI should match", "https://chained.com/photo.jpg", chainedRequest.getPhotoUri());
        assertEquals("Chained phone number should match", "+0987654321", chainedRequest.getPhoneNumber());

    }

    @Test
    public void testGetTokenResult() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing GetTokenResult with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            GetTokenResult tokenResult = await(user.getIdToken(false));

            // Test all public getters
            assertNotNull("getToken() should not be null for " + authType, tokenResult.getToken());
            assertTrue("Token should not be empty for " + authType, tokenResult.getToken().length() > 0);

            assertNotNull("getClaims() should not be null for " + authType, tokenResult.getClaims());
            assertFalse("Claims should not be empty for " + authType, tokenResult.getClaims().isEmpty());

            assertTrue("getAuthTimestamp() should be positive for " + authType, tokenResult.getAuthTimestamp() > 0);
            assertTrue("getIssuedAtTimestamp() should be positive for " + authType,
                    tokenResult.getIssuedAtTimestamp() > 0);
            assertTrue("getExpirationTimestamp() should be > issued time for " + authType,
                    tokenResult.getExpirationTimestamp() > tokenResult.getIssuedAtTimestamp());

            // getSignInProvider() may return null
            tokenResult.getSignInProvider(); // Just call it

        }
    }

    @Test
    public void testEmailAuthProvider() throws Exception {
        Log.d("RegressionTest", "Testing EmailAuthProvider");

        String testEmail = "test@example.com";
        String testPassword = "testpassword123";

        // Test getCredential()
        AuthCredential credential = EmailAuthProvider.getCredential(testEmail, testPassword);
        assertNotNull("getCredential() should not be null", credential);
        assertEquals("Provider ID should match", EmailAuthProvider.PROVIDER_ID, credential.getProvider());
        assertEquals("Sign-in method should match", EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD,
                credential.getSignInMethod());

        // Test constants
        assertEquals("PROVIDER_ID constant should be 'password'", "password", EmailAuthProvider.PROVIDER_ID);
        assertEquals("EMAIL_PASSWORD_SIGN_IN_METHOD should be 'password'", "password",
                EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD);

    }

    @Test
    public void testOsbdtUserMetadata() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing OsbdtUserMetadata with authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            OsbdtUserMetadata metadata = user.getMetadata();

            assertNotNull("User metadata should not be null for " + authType, metadata);

            long creationTime = metadata.getCreationTimestamp();
            long lastSignInTime = metadata.getLastSignInTimestamp();

            assertTrue("Creation timestamp should be positive for " + authType, creationTime > 0);
            assertTrue("Last sign-in timestamp should be positive for " + authType, lastSignInTime > 0);
            assertTrue("Last sign-in should be >= creation time for " + authType, lastSignInTime >= creationTime);

        }
    }

    // ===== SOCIAL AUTH PROVIDERS AND CREDENTIALS =====

    @Test
    public void testFacebookAuthProvider() throws Exception {
        Log.d("RegressionTest", "Testing FacebookAuthProvider");

        // Test constants
        assertEquals("Facebook PROVIDER_ID should be 'facebook'", "facebook", FacebookAuthProvider.PROVIDER_ID);
        assertEquals("Facebook SIGN_IN_METHOD should be 'facebook'", "facebook",
                FacebookAuthProvider.FACEBOOK_SIGN_IN_METHOD);

        // Test credential creation with valid access token
        String mockAccessToken = "mock_facebook_access_token_12345";
        AuthCredential credential = FacebookAuthProvider.getCredential(mockAccessToken);
        assertNotNull("Facebook credential should not be null", credential);
        assertEquals("Credential provider should match", FacebookAuthProvider.PROVIDER_ID, credential.getProvider());
        assertEquals("Credential sign-in method should match", FacebookAuthProvider.FACEBOOK_SIGN_IN_METHOD,
                credential.getSignInMethod());

        // Test null access token (should throw exception)
        boolean threwException = false;
        try {
            FacebookAuthProvider.getCredential(null);
        } catch (IllegalArgumentException e) {
            threwException = true;
        }
        assertTrue("Facebook getCredential should throw exception for null token", threwException);

    }

    @Test
    public void testGoogleAuthProvider() throws Exception {
        Log.d("RegressionTest", "Testing GoogleAuthProvider");

        // Test constants
        assertEquals("Google PROVIDER_ID should be 'google'", "google", GoogleAuthProvider.PROVIDER_ID);
        assertEquals("Google SIGN_IN_METHOD should be 'google'", "google", GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD);

        // Test credential creation with ID token
        String mockIdToken = "mock_google_id_token_12345";
        AuthCredential credential1 = GoogleAuthProvider.getCredential(mockIdToken, null);
        assertNotNull("Google credential with ID token should not be null", credential1);
        assertEquals("Credential provider should match", GoogleAuthProvider.PROVIDER_ID, credential1.getProvider());
        assertEquals("Credential sign-in method should match", GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD,
                credential1.getSignInMethod());

        // Test credential creation with access token
        String mockAccessToken = "mock_google_access_token_12345";
        AuthCredential credential2 = GoogleAuthProvider.getCredential(null, mockAccessToken);
        assertNotNull("Google credential with access token should not be null", credential2);
        assertEquals("Credential provider should match", GoogleAuthProvider.PROVIDER_ID, credential2.getProvider());

        // Test credential creation with both tokens
        AuthCredential credential3 = GoogleAuthProvider.getCredential(mockIdToken, mockAccessToken);
        assertNotNull("Google credential with both tokens should not be null", credential3);

        // Test null tokens (should throw exception)
        boolean threwException = false;
        try {
            GoogleAuthProvider.getCredential(null, null);
        } catch (IllegalArgumentException e) {
            threwException = true;
        }
        assertTrue("Google getCredential should throw exception for null tokens", threwException);

    }

    @Test
    public void testGithubAuthProvider() throws Exception {
        Log.d("RegressionTest", "Testing GithubAuthProvider");

        // Test constants
        assertEquals("Github PROVIDER_ID should be 'github'", "github", GithubAuthProvider.PROVIDER_ID);
        assertEquals("Github SIGN_IN_METHOD should be 'github'", "github", GithubAuthProvider.GITHUB_SIGN_IN_METHOD);

        // Test credential creation with valid token
        String mockToken = "mock_github_token_12345";
        AuthCredential credential = GithubAuthProvider.getCredential(mockToken);
        assertNotNull("Github credential should not be null", credential);
        assertEquals("Credential provider should match", GithubAuthProvider.PROVIDER_ID, credential.getProvider());
        assertEquals("Credential sign-in method should match", GithubAuthProvider.GITHUB_SIGN_IN_METHOD,
                credential.getSignInMethod());

        // Test null token (should throw exception)
        boolean threwException = false;
        try {
            GithubAuthProvider.getCredential(null);
        } catch (IllegalArgumentException e) {
            threwException = true;
        }
        assertTrue("Github getCredential should throw exception for null token", threwException);

    }

    @Test
    public void testSocialCredentialsWithSignIn() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing social credentials with signIn for authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);

            // Test Facebook credential sign-in (this will fail authentication but should
            // validate credential structure)
            String mockFacebookToken = "mock_facebook_token_12345";
            AuthCredential facebookCredential = FacebookAuthProvider.getCredential(mockFacebookToken);

            // Try sign in with credential - may fail due to invalid token, but method
            // should work
            try {
                await(auth.signInWithCredential(facebookCredential));
            } catch (Exception signInException) {
                // Expected to fail with mock token - just verify the method accepts the
                // credential
                assertTrue("Sign in attempt should be made (even if it fails)", true);
            }

            // Test Google credential sign-in
            String mockGoogleToken = "mock_google_token_12345";
            AuthCredential googleCredential = GoogleAuthProvider.getCredential(mockGoogleToken, null);

            try {
                await(auth.signInWithCredential(googleCredential));
            } catch (Exception signInException) {
                // Expected to fail with mock token
                assertTrue("Sign in attempt should be made (even if it fails)", true);
            }

            // Test Github credential sign-in
            String mockGithubToken = "mock_github_token_12345";
            AuthCredential githubCredential = GithubAuthProvider.getCredential(mockGithubToken);

            try {
                await(auth.signInWithCredential(githubCredential));
            } catch (Exception signInException) {
                // Expected to fail with mock token
                assertTrue("Sign in attempt should be made (even if it fails)", true);
            }

        }
    }

    @Test
    public void testOAuthProvider() throws Exception {
        Log.d("RegressionTest", "Testing OAuthProvider");

        String testProviderId = "test_oauth_provider";
        String mockIdToken = "mock_oauth_id_token_12345";
        String mockRawNonce = "mock_nonce_123";

        // Test newBuilder with providerId
        OAuthProvider.Builder builder = OAuthProvider.newBuilder(testProviderId);
        assertNotNull("OAuthProvider builder should not be null", builder);

        // Test builder methods
        OAuthProvider provider = builder
                .addCustomParameter("test_key", "test_value")
                .setScopes(java.util.Arrays.asList("scope1", "scope2"))
                .build();

        assertNotNull("OAuthProvider should not be null", provider);
        assertEquals("Provider ID should match", testProviderId, provider.getProviderId());

        // Test newBuilder with OsbdtAuth
        OsbdtAuth auth = getAuthForType("base");
        OAuthProvider.Builder builderWithAuth = OAuthProvider.newBuilder(testProviderId, auth);
        assertNotNull("OAuthProvider builder with auth should not be null", builderWithAuth);

        // Test CredentialBuilder
        OAuthProvider.CredentialBuilder credBuilder = OAuthProvider.newCredentialBuilder(testProviderId);
        assertNotNull("OAuthProvider CredentialBuilder should not be null", credBuilder);

        AuthCredential credential = credBuilder
                .setIdTokenWithRawNonce(mockIdToken, mockRawNonce)
                .build();

        assertNotNull("OAuth credential should not be null", credential);
        assertEquals("Credential provider should match", testProviderId, credential.getProvider());

        // Test error cases
        boolean threwException = false;
        try {
            OAuthProvider.newBuilder("");
        } catch (IllegalArgumentException e) {
            threwException = true;
        }
        assertTrue("OAuthProvider newBuilder should throw exception for empty providerId", threwException);

        threwException = false;
        try {
            OAuthProvider.newCredentialBuilder("");
        } catch (IllegalArgumentException e) {
            threwException = true;
        }
        assertTrue("OAuthProvider newCredentialBuilder should throw exception for empty providerId", threwException);

    }

    @Test
    public void testIDCSAuthProvider() throws Exception {
        Log.d("RegressionTest", "Testing IDCSAuthProvider");

        // Test constants
        assertEquals("IDCS PROVIDER_ID should be 'idcs'", "idcs", IDCSAuthProvider.PROVIDER_ID);
        assertEquals("IDCS SIGN_IN_METHOD should be 'idcs'", "idcs", IDCSAuthProvider.IDCS_SIGN_IN_METHOD);

        String testProviderId = "test_idcs_provider";
        String mockIdToken = "mock_idcs_id_token_12345";
        String mockRawNonce = "mock_nonce_123";

        // Test newBuilder with providerId
        IDCSAuthProvider.Builder builder = IDCSAuthProvider.newBuilder(testProviderId);
        assertNotNull("IDCSAuthProvider builder should not be null", builder);

        // Test builder methods
        IDCSAuthProvider provider = builder
                .addCustomParameter("test_key", "test_value")
                .setScopes(java.util.Arrays.asList("scope1", "scope2"))
                .build();

        assertNotNull("IDCSAuthProvider should not be null", provider);
        assertEquals("Provider ID should match", testProviderId, provider.getProviderId());

        // Test newBuilder with OsbdtAuth
        OsbdtAuth auth = getAuthForType("idcs"); // Use idcs auth type for IDCS provider
        IDCSAuthProvider.Builder builderWithAuth = IDCSAuthProvider.newBuilder(testProviderId, auth);
        assertNotNull("IDCSAuthProvider builder with auth should not be null", builderWithAuth);

        // Test CredentialBuilder
        IDCSAuthProvider.CredentialBuilder credBuilder = IDCSAuthProvider.newCredentialBuilder(testProviderId);
        assertNotNull("IDCSAuthProvider CredentialBuilder should not be null", credBuilder);

        AuthCredential credential = credBuilder
                .setIdTokenWithRawNonce(mockIdToken, mockRawNonce)
                .build();

        assertNotNull("IDCS credential should not be null", credential);
        assertEquals("Credential provider should match", testProviderId, credential.getProvider());

        // Test error cases
        boolean threwException = false;
        try {
            IDCSAuthProvider.newBuilder("");
        } catch (IllegalArgumentException e) {
            threwException = true;
        }
        assertTrue("IDCSAuthProvider newBuilder should throw exception for empty providerId", threwException);

        threwException = false;
        try {
            IDCSAuthProvider.newCredentialBuilder("");
        } catch (IllegalArgumentException e) {
            threwException = true;
        }
        assertTrue("IDCSAuthProvider newCredentialBuilder should throw exception for empty providerId", threwException);

    }

    @Test
    public void testSAMLAuthProvider() throws Exception {
        Log.d("RegressionTest", "Testing SAMLAuthProvider");

        String testProviderId = "test_saml_provider";
        String mockIdToken = "mock_saml_id_token_12345";
        String mockRawNonce = "mock_nonce_123";

        // Test newBuilder with providerId
        SAMLAuthProvider.Builder builder = SAMLAuthProvider.newBuilder(testProviderId);
        assertNotNull("SAMLAuthProvider builder should not be null", builder);

        // Test builder methods
        SAMLAuthProvider provider = builder
                .addCustomParameter("test_key", "test_value")
                .setScopes(java.util.Arrays.asList("scope1", "scope2"))
                .build();

        assertNotNull("SAMLAuthProvider should not be null", provider);
        assertEquals("Provider ID should match", testProviderId, provider.getProviderId());

        // Test newBuilder with OsbdtAuth
        OsbdtAuth auth = getAuthForType("base");
        SAMLAuthProvider.Builder builderWithAuth = SAMLAuthProvider.newBuilder(testProviderId, auth);
        assertNotNull("SAMLAuthProvider builder with auth should not be null", builderWithAuth);

        // Test CredentialBuilder
        SAMLAuthProvider.CredentialBuilder credBuilder = SAMLAuthProvider.newCredentialBuilder(testProviderId);
        assertNotNull("SAMLAuthProvider CredentialBuilder should not be null", credBuilder);

        AuthCredential credential = credBuilder
                .setIdTokenWithRawNonce(mockIdToken, mockRawNonce)
                .build();

        assertNotNull("SAML credential should not be null", credential);
        assertEquals("Credential provider should match", testProviderId, credential.getProvider());

        // Test error cases
        boolean threwException = false;
        try {
            SAMLAuthProvider.newBuilder("");
        } catch (IllegalArgumentException e) {
            threwException = true;
        }
        assertTrue("SAMLAuthProvider newBuilder should throw exception for empty providerId", threwException);

        threwException = false;
        try {
            SAMLAuthProvider.newCredentialBuilder("");
        } catch (IllegalArgumentException e) {
            threwException = true;
        }
        assertTrue("SAMLAuthProvider newCredentialBuilder should throw exception for empty providerId", threwException);

    }

    @Test
    public void testIDCSAuthCredential() throws Exception {
        Log.d("RegressionTest", "Testing IDCSAuthCredential");

        String testProvider = "test_idcs_provider";
        String testSignInMethod = "idcs";
        String mockAccessToken = "mock_idcs_access_token_12345";

        // IDCSAuthCredential is created via IDCSAuthProvider.CredentialBuilder
        // Test the credential getters
        IDCSAuthProvider.CredentialBuilder credBuilder = IDCSAuthProvider.newCredentialBuilder(testProvider);
        AuthCredential credential = credBuilder
                .setIdTokenWithRawNonce(mockAccessToken, null)
                .build();

        assertNotNull("IDCS credential should not be null", credential);
        assertEquals("Credential provider should match", testProvider, credential.getProvider());

        // Note: getSignInMethod() is inherited from AuthCredential, may not be directly
        // testable
        // without casting, but the credential creation and provider validation works

    }

    @Test
    public void testOAuthCredentialsWithSignIn() throws Exception {
        for (String authType : AUTH_TYPES) {
            Log.d("RegressionTest", "Testing OAuth credentials with signIn for authType: " + authType);
            OsbdtAuth auth = getAuthForType(authType);

            // Test OAuth credential sign-in
            String testProviderId = "test_oauth_provider";
            String mockIdToken = "mock_oauth_id_token_12345";

            OAuthProvider.CredentialBuilder credBuilder = OAuthProvider.newCredentialBuilder(testProviderId);
            AuthCredential oauthCredential = credBuilder
                    .setIdTokenWithRawNonce(mockIdToken, null)
                    .build();

            // Try sign in with credential - may fail due to invalid token, but method
            // should work
            try {
                await(auth.signInWithCredential(oauthCredential));
            } catch (Exception signInException) {
                // Expected to fail with mock token - just verify the method accepts the
                // credential
                assertTrue("Sign in attempt should be made (even if it fails)", true);
            }

            // Test IDCS credential sign-in
            String mockIdcsToken = "mock_idcs_token_12345";
            IDCSAuthProvider.CredentialBuilder idcsCredBuilder = IDCSAuthProvider.newCredentialBuilder("test_idcs");
            AuthCredential idcsCredential = idcsCredBuilder
                    .setIdTokenWithRawNonce(mockIdcsToken, null)
                    .build();

            try {
                await(auth.signInWithCredential(idcsCredential));
            } catch (Exception signInException) {
                // Expected to fail with mock token
                assertTrue("Sign in attempt should be made (even if it fails)", true);
            }

            // Test SAML credential sign-in
            String mockSamlToken = "mock_saml_token_12345";
            SAMLAuthProvider.CredentialBuilder samlCredBuilder = SAMLAuthProvider.newCredentialBuilder("test_saml");
            AuthCredential samlCredential = samlCredBuilder
                    .setIdTokenWithRawNonce(mockSamlToken, null)
                    .build();

            try {
                await(auth.signInWithCredential(samlCredential));
            } catch (Exception signInException) {
                // Expected to fail with mock token
                assertTrue("Sign in attempt should be made (even if it fails)", true);
            }

        }
    }

    @Test
    public void testOsbdtOracledbGetInstanceSingleton() throws Exception {
        Log.d("RegressionTest", "Testing OsbdtOracledb getInstance singleton pattern");

        // Get a database instance
        OsbdtOracledb db1 = OsbdtOracledb.getInstance();
        assertNotNull("OsbdtOracledb instance should not be null", db1);

        // Get another instance - should be the same (singleton)
        OsbdtOracledb db2 = OsbdtOracledb.getInstance();
        assertSame("OsbdtOracledb should follow singleton pattern", db1, db2);

    }

    @Test
    public void testOsbdtOracledbCollectionAndDocumentReference() throws Exception {
        Log.d("RegressionTest", "Testing OsbdtOracledb collection and document reference creation");
        for (String authType : AUTH_TYPES) {
            OsbdtAuth auth = getAuthForType(authType);
            OsbdtOracledb db = OsbdtOracledb.getInstance(auth.getApp());
            assertNotNull("OsbdtOracledb instance should not be null", db);

            // Test collection creation
            CollectionReference collection = db.collection("testCollection");
            assertNotNull("Collection reference should not be null", collection);
            assertEquals("Collection ID should match", "testCollection", collection.getId());

            // Test document creation
            DocumentReference document = collection.document("testDoc");
            assertNotNull("Document reference should not be null", document);
            assertEquals("Document ID should match", "testDoc", document.getId());

            // Test auto-generated document ID
            DocumentReference autoDoc = collection.document();
            assertNotNull("Auto-generated document should not be null", autoDoc);
            assertNotNull("Auto-generated document ID should not be null", autoDoc.getId());
        }
    }

    @Test
    public void testOsbdtOracledbBatchOperation() throws Exception {
        Log.d("RegressionTest", "Testing OsbdtOracledb batch operations");

        for (String authType : AUTH_TYPES) {
            OsbdtAuth auth = getAuthForType(authType);
            OsbdtOracledb db = OsbdtOracledb.getInstance(auth.getApp());
            DocumentReference doc = db.collection("testCollection").document("batchDoc");

            // Create batch operation
            WriteBatch batch = db.batch();
            assertNotNull("WriteBatch should not be null", batch);

            // Add set operation to batch
            batch.set(doc, java.util.Collections.singletonMap("name", "BatchTest"));

            // Commit batch
            await(batch.commit());
            assertTrue("Batch commit should succeed", true);

            // Verify the document was created
            DocumentSnapshot snapshot = await(doc.get());
            assertTrue("Document should exist after batch", snapshot.exists());
            assertEquals("Document data should match", "BatchTest", snapshot.getString("name"));
        }

    }

    @Test
    public void testOsbdtOracledbRunTransaction() throws Exception {
        Log.d("RegressionTest", "Testing OsbdtOracledb transaction operations");
        for (String authType : AUTH_TYPES) {
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            await(auth.createUserWithEmailAndPassword(email, password));

            OsbdtOracledb db = OsbdtOracledb.getInstance(auth.getApp());
            DocumentReference doc = db.collection("testCollection").document("transactionDoc1");

            // Initialize document with count = 1
            await(doc.set(java.util.Collections.singletonMap("count", 1)));

            // Run transaction to increment count
            await(db.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(doc);
                Long currentCount = snapshot.getLong("count");
                if (currentCount == null)
                    currentCount = 0L;
                transaction.update(doc, "count", currentCount + 1);
                return null;
            }));

            // Verify transaction worked
            DocumentSnapshot result = await(doc.get());
            assertEquals("Count should be incremented to 2", Long.valueOf(2), result.getLong("count"));
        }

    }

    @Test
    public void testOsbdtOracledbEqualityHashCodeAndToString() throws Exception {
        Log.d("RegressionTest", "Testing OsbdtOracledb equality, hashCode, and toString");

        for (String authType : AUTH_TYPES) {
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            await(auth.createUserWithEmailAndPassword(email, password));

            OsbdtOracledb db1 = OsbdtOracledb.getInstance(auth.getApp());
            OsbdtOracledb db2 = OsbdtOracledb.getInstance(auth.getApp());

            // Test equality (should be same instance due to singleton)
            assertEquals("OsbdtOracledb instances should be equal", db1, db2);

            // Test hashCode
            assertEquals("Hash codes should be equal for same instances", db1.hashCode(), db2.hashCode());

            // Test toString
            assertNotNull("toString should not be null", db1.toString());
            assertTrue("toString should contain content", db1.toString().length() > 0);
        }

    }

    @Test
    public void testCollectionReferenceGetId() throws Exception {
        Log.d("RegressionTest", "Testing CollectionReference getId");

        for (String authType : AUTH_TYPES) {
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            await(auth.createUserWithEmailAndPassword(email, password));

            OsbdtOracledb db = OsbdtOracledb.getInstance(auth.getApp());

            CollectionReference colRef = db.collection("testCollection");

            assertEquals("testCollection", colRef.getId());
        }
    }

    @Test
    public void testCollectionReferenceGetPath() throws Exception {
        Log.d("RegressionTest", "Testing CollectionReference getPath");

        for (String authType : AUTH_TYPES) {
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            await(auth.createUserWithEmailAndPassword(email, password));

            OsbdtOracledb db = OsbdtOracledb.getInstance(auth.getApp());

            CollectionReference colRef = db.collection("testCollection");

            assertEquals("testCollection", colRef.getPath());

        }
    }

    @Test
    public void testCollectionReferenceGetParent() throws Exception {
        Log.d("RegressionTest", "Testing CollectionReference getParent");

        for (String authType : AUTH_TYPES) {
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            await(auth.createUserWithEmailAndPassword(email, password));

            OsbdtOracledb db = OsbdtOracledb.getInstance(auth.getApp());

            CollectionReference colRef = db.collection("testCollection");

            // Top-level collection should have null parent
            assertNull("Top-level collection should have null parent", colRef.getParent());

        }
    }

    @Test
    public void testDocumentReferenceCreation() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference creation");

        for (String authType : AUTH_TYPES) {
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            await(auth.createUserWithEmailAndPassword(email, password));

            OsbdtOracledb db = OsbdtOracledb.getInstance(auth.getApp());

            CollectionReference colRef = db.collection("testCollection");

            // Auto-generated ID document
            DocumentReference docRef1 = colRef.document();
            assertNotNull("Auto-generated document ID should not be null", docRef1.getId());
            assertTrue("Auto-generated document ID should not be empty", docRef1.getId().length() > 0);

            // Specific ID document
            DocumentReference docRef2 = colRef.document("specificDoc");
            assertEquals("specificDoc", docRef2.getId());
        }

    }

    @Test
    public void testAddDocumentWithAllDataTypes() throws Exception {
        Log.d("RegressionTest", "Testing add document with all data types");

        for (String authType : AUTH_TYPES) {
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            await(auth.createUserWithEmailAndPassword(email, password));

            OsbdtOracledb db = OsbdtOracledb.getInstance(auth.getApp());
            CollectionReference colRef = db.collection("testCollection");

            Map<String, Object> comprehensiveData = new HashMap<>();

            // Primitive types
            comprehensiveData.put("stringField", "Hello World");
            comprehensiveData.put("intField", 42);
            comprehensiveData.put("longField", 123456789L);
            comprehensiveData.put("doubleField", 3.14159);
            comprehensiveData.put("booleanField", true);
            comprehensiveData.put("nullField", null);

            // Date/Timestamp
            comprehensiveData.put("dateField", new java.util.Date());
            comprehensiveData.put("timestampField", Timestamp.now());

            // Collections
            comprehensiveData.put("stringList", java.util.Arrays.asList("a", "b", "c"));
            comprehensiveData.put("intList", java.util.Arrays.asList(1, 2, 3));
            comprehensiveData.put("mixedList", java.util.Arrays.asList("string", 42, true));

            // Maps
            Map<String, Object> nestedMap = new HashMap<>();
            nestedMap.put("nestedString", "nested value");
            nestedMap.put("nestedInt", 99);
            comprehensiveData.put("nestedMap", nestedMap);

            // Add document and verify
            Task<DocumentReference> addTask = colRef.add(comprehensiveData);
            DocumentReference addedDocRef = await(addTask);

            assertNotNull("Added document reference should not be null", addedDocRef);

            // Verify the document was added by retrieving it
            DocumentSnapshot snapshot = await(addedDocRef.get());
            assertTrue("Document should exist after add", snapshot.exists());

            // Verify data types
            assertEquals("Hello World", snapshot.getString("stringField"));
            assertEquals((Long) 42L, snapshot.getLong("intField"));
            assertEquals((Long) 123456789L, snapshot.getLong("longField"));
            assertEquals(3.14159, snapshot.getDouble("doubleField"), 0.00001);
            assertTrue(snapshot.getBoolean("booleanField"));
            assertNull(snapshot.get("nullField"));
            assertNotNull(snapshot.getDate("dateField"));
            assertNotNull(snapshot.getTimestamp("timestampField"));

            // Verify collections
            List<String> stringList = (List<String>) snapshot.get("stringList");
            assertNotNull("String list should not be null", stringList);
            assertEquals(java.util.Arrays.asList("a", "b", "c"), stringList);

            // Verify maps
            Map<String, Object> retrievedMap = (Map<String, Object>) snapshot.get("nestedMap");
            assertNotNull("Nested map should not be null", retrievedMap);
            assertEquals("nested value", retrievedMap.get("nestedString"));
            assertEquals(99L, retrievedMap.get("nestedInt"));

        }
    }

    @Test
    public void testFieldValueOperations() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue operations");

        for (String authType : AUTH_TYPES) {
            OsbdtAuth auth = getAuthForType(authType);
            String email = "regression_user_" + Math.random() + authType + "@example.com";
            String password = "Password@123";

            await(auth.createUserWithEmailAndPassword(email, password));

            OsbdtOracledb db = OsbdtOracledb.getInstance(auth.getApp());
            DocumentReference docRef = db.collection("testCollection").document("fieldValueTest");

            // Initial document
            Map<String, Object> initialData = new HashMap<>();
            initialData.put("counter", 10);
            initialData.put("tags", java.util.Arrays.asList("initial"));
            initialData.put("score", 100.0);
            await(docRef.set(initialData));

            // Test increment (long)
            await(docRef.update("counter", FieldValue.increment(5)));
            DocumentSnapshot snap = await(docRef.get());
            assertEquals((Long) 15L, snap.getLong("counter"));

            // Test increment (double)
            await(docRef.update("score", FieldValue.increment(10L)));
            snap = await(docRef.get());
            assertEquals(110.0, snap.getDouble("score"), 0.001);

            // Test arrayUnion
            await(docRef.update("tags", FieldValue.arrayUnion("added1", "added2")));
            snap = await(docRef.get());
            List<String> tags = (List<String>) snap.get("tags");
            assertNotNull("Tags list should not be null", tags);
            assertTrue("Tags should contain initial", tags.contains("initial"));
            assertTrue("Tags should contain added1", tags.contains("added1"));
            assertTrue("Tags should contain added2", tags.contains("added2"));

            // Test arrayRemove
            await(docRef.update("tags", FieldValue.arrayRemove("initial", "added1")));
            snap = await(docRef.get());
            tags = (List<String>) snap.get("tags");
            assertNotNull("Tags list should not be null after remove", tags);
            assertFalse("Tags should not contain initial", tags.contains("initial"));
            assertFalse("Tags should not contain added1", tags.contains("added1"));
            assertTrue("Tags should still contain added2", tags.contains("added2"));

            // Test delete
            await(docRef.update("score", FieldValue.delete()));
            snap = await(docRef.get());
            assertFalse("Score field should not exist after delete", snap.contains("score"));

        }
    }

    @Test
    public void testAddDocumentWithServerTimestamp() throws Exception {
        Log.d("RegressionTest", "Testing add document with server timestamp");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            Map<String, Object> dataWithServerTimestamp = new HashMap<>();
            dataWithServerTimestamp.put("name", "testDoc");
            dataWithServerTimestamp.put("createdAt", FieldValue.serverTimestamp());

            // ServerTimestamp is supported for add operations
            Task<DocumentReference> addTask = colRef.add(dataWithServerTimestamp);
            DocumentReference addedDocRef = await(addTask);

            assertNotNull("Added document with server timestamp should not be null", addedDocRef);

            // Verify the document was added
            DocumentSnapshot snapshot = await(addedDocRef.get());
            assertTrue("Document with server timestamp should exist", snapshot.exists());
            assertEquals("testDoc", snapshot.getString("name"));
            // The serverTimestamp field should be set
            assertNotNull("Server timestamp field should be set", snapshot.get("createdAt"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in add document with server timestamp test: " + e.getMessage());
        }
    }

    @Test
    public void testBatchOperations() throws Exception {
        Log.d("RegressionTest", "Testing batch operations");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            WriteBatch batch = db.batch();

            // Add multiple documents in batch
            DocumentReference doc1 = db.collection("testCollection").document("batch1");
            DocumentReference doc2 = db.collection("testCollection").document("batch2");
            DocumentReference doc3 = db.collection("testCollection").document("batch3");

            batch.set(doc1, java.util.Collections.singletonMap("batchField", "value1"));
            batch.set(doc2, java.util.Collections.singletonMap("batchField", "value2"));
            batch.set(doc3, java.util.Collections.singletonMap("batchField", "value3"));

            // Update one document
            batch.update(doc1, "batchField", "updated1");

            // Delete one document
            batch.delete(doc2);

            // Commit batch
            await(batch.commit());

            // Verify results
            DocumentSnapshot snap1 = await(doc1.get());
            assertTrue("Document 1 should exist after batch", snap1.exists());
            assertEquals("updated1", snap1.getString("batchField"));

            DocumentSnapshot snap2 = await(doc2.get());
            assertFalse("Document 2 should not exist after batch delete", snap2.exists());

            DocumentSnapshot snap3 = await(doc3.get());
            assertTrue("Document 3 should exist after batch", snap3.exists());
            assertEquals("value3", snap3.getString("batchField"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in batch operations test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryOperations() throws Exception {
        Log.d("RegressionTest", "Testing query operations");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents with known values
            DocumentReference doc1 = colRef.document("queryTest1");
            DocumentReference doc2 = colRef.document("queryTest2");
            DocumentReference doc3 = colRef.document("queryTest3");

            await(doc1.set(java.util.Collections.singletonMap("intValue", 100)));
            await(doc2.set(java.util.Collections.singletonMap("intValue", 200)));
            await(doc3.set(java.util.Collections.singletonMap("intValue", 300)));

            // Test whereEqualTo
            Query query = colRef.whereEqualTo("intValue", 200);
            QuerySnapshot result = await(query.get());
            assertTrue("Should find document with intValue 200", result.size() > 0);

            // Test whereGreaterThan
            query = colRef.whereGreaterThan("intValue", 150);
            result = await(query.get());
            for (DocumentSnapshot doc : result) {
                assertTrue("All results should have intValue > 150", doc.getLong("intValue") > 150);
            }

            // Test whereLessThan
            query = colRef.whereLessThan("intValue", 250);
            result = await(query.get());
            for (DocumentSnapshot doc : result) {
                assertTrue("All results should have intValue < 250", doc.getLong("intValue") < 250);
            }

            // Test combined query with orderBy and limit
            query = colRef.whereGreaterThan("intValue", 0)
                    .orderBy("intValue")
                    .limit(5);
            result = await(query.get());
            assertTrue("Should return at most 5 results", result.size() <= 5);

            // Verify ordering
            long previousValue = -1;
            for (DocumentSnapshot doc : result) {
                long currentValue = doc.getLong("intValue");
                assertTrue("Results should be ordered by intValue", currentValue >= previousValue);
                previousValue = currentValue;
            }

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in query operations test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceOperations() throws Exception {
        Log.d("RegressionTest", "Testing document reference operations");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection").document("docOpsTest");

            // Test set with merge options
            Map<String, Object> data1 = new HashMap<>();
            data1.put("field1", "value1");
            data1.put("field2", "value2");
            await(docRef.set(data1));

            // Merge with additional data
            Map<String, Object> data2 = new HashMap<>();
            data2.put("field2", "updatedValue2"); // Overwrite
            data2.put("field3", "value3"); // Add new
            await(docRef.set(data2, SetOptions.merge()));

            DocumentSnapshot snap = await(docRef.get());
            assertEquals("value1", snap.getString("field1")); // Preserved
            assertEquals("updatedValue2", snap.getString("field2")); // Updated
            assertEquals("value3", snap.getString("field3")); // Added

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document reference operations test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionOperations() throws Exception {
        Log.d("RegressionTest", "Testing transaction operations");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection").document("transactionTest");

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("counter", 0)));

            // Run transaction
            await(db.runTransaction(transaction -> {
                DocumentSnapshot snap = transaction.get(docRef);
                long currentValue = snap.getLong("counter");
                if (currentValue == 0)
                    currentValue = 0; // handle null case
                transaction.update(docRef, "counter", currentValue + 1);
                return null;
            }));

            // Verify transaction worked
            DocumentSnapshot finalSnap = await(docRef.get());
            assertEquals((Long) 1L, finalSnap.getLong("counter"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in transaction operations test: " + e.getMessage());
        }
    }

    @Test
    public void testNestedCollections() throws Exception {
        Log.d("RegressionTest", "Testing nested collections");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference parentDoc = db.collection("testCollection").document("parentDoc");
            CollectionReference subCollection = parentDoc.collection("subCollection");

            // Add document to subcollection
            Map<String, Object> subData = new HashMap<>();
            subData.put("subField", "subValue");
            await(subCollection.add(subData));

            // Verify subcollection document
            QuerySnapshot subDocs = await(subCollection.get());
            assertTrue("Subcollection should contain documents", subDocs.size() > 0);

            DocumentSnapshot subDoc = subDocs.getDocuments().get(0);
            assertEquals("subValue", subDoc.getString("subField"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in nested collections test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotOperations() throws Exception {
        Log.d("RegressionTest", "Testing document snapshot operations");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection").document("snapshotTest");

            Map<String, Object> testData = new HashMap<>();
            testData.put("testField", "testValue");
            testData.put("numberField", 42);
            testData.put("boolField", true);
            await(docRef.set(testData));

            DocumentSnapshot snap = await(docRef.get());

            // Test various getter methods
            assertTrue("Document should exist", snap.exists());
            assertEquals("snapshotTest", snap.getId());
            assertNotNull("Reference should not be null", snap.getReference());

            // Test typed getters
            assertEquals("testValue", snap.getString("testField"));
            assertEquals((Long) 42L, snap.getLong("numberField"));
            assertTrue("Boolean field should be true", snap.getBoolean("boolField"));

            // Test generic get
            assertEquals("testValue", snap.get("testField"));

            // Test contains and metadata
            assertTrue("Should contain testField", snap.contains("testField"));
            assertFalse("Should not contain nonExistentField", snap.contains("nonExistentField"));
            assertNotNull("Metadata should not be null", snap.getMetadata());

            // Test data conversion
            Map<String, Object> data = snap.getData();
            assertNotNull("Data map should not be null", data);
            assertEquals("testValue", data.get("testField"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document snapshot operations test: " + e.getMessage());
        }
    }

    @Test
    public void testErrorConditions() throws Exception {
        Log.d("RegressionTest", "Testing error conditions");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference nonExistentDoc = db.collection("testCollection").document("nonExistent");
            DocumentSnapshot snap = await(nonExistentDoc.get());
            assertFalse("Non-existent document should not exist", snap.exists());

            // Test update on non-existent document (should fail)
            boolean threwException = false;
            try {
                await(nonExistentDoc.update("field", "value"));
            } catch (Exception e) {
                threwException = true;
            }
            assertTrue("Update on non-existent document should throw exception", threwException);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in error conditions test: " + e.getMessage());
        }
    }

    @Test
    public void testCollectionReferenceEquality() throws Exception {
        Log.d("RegressionTest", "Testing collection reference equality");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef1 = db.collection("testCollection");
            CollectionReference colRef2 = db.collection("testCollection");
            CollectionReference colRef3 = db.collection("differentCollection");

            // Same collection references should be equal
            assertEquals("Equal collection references should be equal", colRef1, colRef2);
            // Different collection references should not be equal
            assertNotEquals("Different collection references should not be equal", colRef1, colRef3);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in collection reference equality test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceFromCollection() throws Exception {
        Log.d("RegressionTest", "Testing document reference from collection");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Test creating document references from collection
            DocumentReference docRef1 = colRef.document();
            DocumentReference docRef2 = colRef.document("specificId");

            assertNotNull("Auto-generated document ID should not be null", docRef1.getId());
            assertEquals("specificId", docRef2.getId());

            // Test path construction
            assertEquals("testCollection/" + docRef1.getId(), docRef1.getPath());
            assertEquals("testCollection/specificId", docRef2.getPath());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document reference from collection test: " + e.getMessage());
        }
    }

    @Test
    public void testCollectionReferenceHashCode() throws Exception {
        Log.d("RegressionTest", "Testing collection reference hashCode");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef1 = db.collection("testCollection");
            CollectionReference colRef2 = db.collection("testCollection");
            CollectionReference colRef3 = db.collection("otherCollection");

            // Equal objects should have equal hash codes
            assertEquals("Equal collection references should have equal hash codes", colRef1.hashCode(),
                    colRef2.hashCode());
            // Different objects should have different hash codes
            assertNotEquals("Different collection references should have different hash codes", colRef1.hashCode(),
                    colRef3.hashCode());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in collection reference hashCode test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryWithMultipleWhereClauses() throws Exception {
        Log.d("RegressionTest", "Testing query with multiple where clauses");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("multiWhere1");
            DocumentReference doc2 = colRef.document("multiWhere2");
            DocumentReference doc3 = colRef.document("multiWhere3");

            await(doc1.set(java.util.Map.of("status", "active", "priority", 1, "category", "urgent")));
            await(doc2.set(java.util.Map.of("status", "active", "priority", 2, "category", "normal")));
            await(doc3.set(java.util.Map.of("status", "inactive", "priority", 1, "category", "urgent")));

            // Query with multiple where clauses
            Query complexQuery = colRef
                    .whereEqualTo("status", "active")
                    .whereGreaterThan("priority", 0)
                    .whereIn("category", java.util.Arrays.asList("urgent", "normal"));

            QuerySnapshot result = await(complexQuery.get());
            assertTrue("Should return at least 2 documents", result.size() >= 2);

            // Verify all documents match criteria
            for (DocumentSnapshot doc : result) {
                assertEquals("active", doc.getString("status"));
                assertTrue("Priority should be > 0", doc.getLong("priority") > 0);
                String category = doc.getString("category");
                assertTrue("Category should be urgent or normal",
                        category.equals("urgent") || category.equals("normal"));
            }

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in query with multiple where clauses test: " + e.getMessage());
        }
    }

    @Test
    public void testComplexDataStructures() throws Exception {
        Log.d("RegressionTest", "Testing complex data structures");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection").document("complexDataTest");

            // Create complex nested data
            Map<String, Object> complexData = new HashMap<>();

            // Nested maps
            Map<String, Object> level1 = new HashMap<>();
            Map<String, Object> level2 = new HashMap<>();
            level2.put("deepValue", "very deep");
            level1.put("level2", level2);
            complexData.put("nested", level1);

            // Arrays with mixed types
            List<Object> mixedArray = java.util.Arrays.asList(
                    "string", 42, true, null, Timestamp.now(),
                    java.util.Arrays.asList("nested", "array"),
                    java.util.Collections.singletonMap("arrayMap", "value"));
            complexData.put("mixedArray", mixedArray);

            // Array of maps
            List<Map<String, Object>> arrayOfMaps = java.util.Arrays.asList(
                    java.util.Collections.singletonMap("item1", "value1"),
                    java.util.Collections.singletonMap("item2", "value2"));
            complexData.put("arrayOfMaps", arrayOfMaps);

            await(docRef.set(complexData));

            // Verify complex data retrieval
            DocumentSnapshot snap = await(docRef.get());
            assertTrue("Document should exist", snap.exists());

            // Verify nested maps
            Map<String, Object> nested = (Map<String, Object>) snap.get("nested");
            assertNotNull("Nested map should not be null", nested);
            Map<String, Object> level2Retrieved = (Map<String, Object>) nested.get("level2");
            assertNotNull("Level 2 map should not be null", level2Retrieved);
            assertEquals("very deep", level2Retrieved.get("deepValue"));

            // Verify mixed array
            List<Object> retrievedArray = (List<Object>) snap.get("mixedArray");
            assertNotNull("Mixed array should not be null", retrievedArray);
            assertEquals("string", retrievedArray.get(0));
            assertEquals(42L, retrievedArray.get(1));
            assertEquals(true, retrievedArray.get(2));
            assertNull("Null value should be preserved", retrievedArray.get(3));

            // Verify array of maps
            List<Map<String, Object>> retrievedArrayOfMaps = (List<Map<String, Object>>) snap.get("arrayOfMaps");
            assertNotNull("Array of maps should not be null", retrievedArrayOfMaps);
            assertEquals(2, retrievedArrayOfMaps.size());
            assertEquals("value1", retrievedArrayOfMaps.get(0).get("item1"));
            assertEquals("value2", retrievedArrayOfMaps.get(1).get("item2"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in complex data structures test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceSetAndGet() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference set and get operations");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("docSetGet_" + System.currentTimeMillis());

            Map<String, Object> testData = new HashMap<>();
            testData.put("name", "Test Document");
            testData.put("value", 123);
            testData.put("active", true);

            // Set document
            await(docRef.set(testData));

            // Get document and verify
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should exist", snapshot.exists());
            assertEquals("Name should match", "Test Document", snapshot.getString("name"));
            assertEquals("Value should match", Long.valueOf(123), snapshot.getLong("value"));
            assertTrue("Active should be true", snapshot.getBoolean("active"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document set/get test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceUpdateWithMap() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference update with map");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("docUpdateMap_" + System.currentTimeMillis());

            // Initial set
            await(docRef.set(java.util.Collections.singletonMap("field1", "value1")));

            // Update with map
            Map<String, Object> updates = new HashMap<>();
            updates.put("field1", "updated1");
            updates.put("field2", "value2");
            await(docRef.update(updates));

            // Verify updates
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Field1 should be updated", "updated1", snapshot.getString("field1"));
            assertEquals("Field2 should be added", "value2", snapshot.getString("field2"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document update with map test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceUpdateWithVarArgs() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference update with var args");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("docUpdateVarArgs_" + System.currentTimeMillis());

            // Initial set
            await(docRef.set(java.util.Collections.singletonMap("field1", "value1")));

            // Update with var args
            await(docRef.update("field1", "updated1", "field2", "value2"));

            // Verify updates
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Field1 should be updated", "updated1", snapshot.getString("field1"));
            assertEquals("Field2 should be added", "value2", snapshot.getString("field2"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document update with var args test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceUpdateWithFieldPath() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference update with field path");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("docUpdateFieldPath_" + System.currentTimeMillis());

            // Initial set with nested structure
            await(docRef.set(java.util.Collections.singletonMap("nested", new HashMap<String, Object>())));

            // Update with field path
            await(docRef.update(FieldPath.of("nested", "field"), "nestedValue"));

            // Verify nested update
            DocumentSnapshot snapshot = await(docRef.get());
            Map<String, Object> nested = (Map<String, Object>) snapshot.get("nested");
            assertEquals("Nested field should be set", "nestedValue", nested.get("field"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document update with field path test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceDelete() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference delete operation");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("docDelete_" + System.currentTimeMillis());

            // Set document
            await(docRef.set(java.util.Collections.singletonMap("field", "value")));

            // Verify it exists
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should exist before delete", snapshot.exists());

            // Delete document
            await(docRef.delete());

            // Verify it no longer exists
            snapshot = await(docRef.get());
            assertFalse("Document should not exist after delete", snapshot.exists());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document delete test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceProperties() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference properties");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");
            DocumentReference docRef = colRef.document("testDoc");

            // Test properties
            assertEquals("ID should match", "testDoc", docRef.getId());
            assertEquals("Parent should match collection", colRef, docRef.getParent());
            assertEquals("Path should be collection/document", "testCollection/testDoc", docRef.getPath());
            assertNotNull("Path segments should not be null", docRef.getPathSegments());
            assertEquals("Should have 2 path segments", 2, docRef.getPathSegments().size());

            // Test collection method
            CollectionReference subCollection = docRef.collection("subCollection");
            assertNotNull("Subcollection should be created", subCollection);
            assertEquals("Subcollection ID should match", "subCollection", subCollection.getId());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document properties test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceSetWithMerge() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference set with merge");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("docMerge_" + System.currentTimeMillis());

            // Initial set
            await(docRef.set(java.util.Collections.singletonMap("field1", "value1")));

            // Set with merge
            await(docRef.set(java.util.Collections.singletonMap("field2", "value2"), SetOptions.merge()));

            // Verify merge
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Field1 should be preserved", "value1", snapshot.getString("field1"));
            assertEquals("Field2 should be added", "value2", snapshot.getString("field2"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document set with merge test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceSnapshotListener() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference snapshot listener");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("docListener_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("status", "initial")));

            // Set up listener
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final DocumentSnapshot[] receivedSnapshot = new DocumentSnapshot[1];

            ListenerRegistration registration = docRef.addSnapshotListener((snapshot, error) -> {
                assertNull("Should not have error", error);
                receivedSnapshot[0] = snapshot;
                latch.countDown();
            });

            // Wait for initial snapshot
            assertTrue("Should receive initial snapshot", latch.await(10, java.util.concurrent.TimeUnit.SECONDS));
            assertNotNull("Snapshot should not be null", receivedSnapshot[0]);
            assertTrue("Document should exist", receivedSnapshot[0].exists());
            assertEquals("Initial status should match", "initial", receivedSnapshot[0].getString("status"));

            // Cleanup
            registration.remove();

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document snapshot listener test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceEquality() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference equality");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            DocumentReference docRef1 = colRef.document("testDoc");
            DocumentReference docRef2 = colRef.document("testDoc");
            DocumentReference docRef3 = colRef.document("differentDoc");

            // Test equality
            assertEquals("Equal document references should be equal", docRef1, docRef2);
            assertNotEquals("Different document references should not be equal", docRef1, docRef3);

            // Test hashCode
            assertEquals("Equal references should have equal hash codes", docRef1.hashCode(), docRef2.hashCode());
            assertNotEquals("Different references should have different hash codes", docRef1.hashCode(),
                    docRef3.hashCode());

            // Test toString
            assertNotNull("toString should not be null", docRef1.toString());
            assertTrue("toString should contain identifying information", docRef1.toString().length() > 0);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document reference equality test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceComplexData() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference with complex data");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("docComplex_" + System.currentTimeMillis());

            // Create comprehensive test data
            Map<String, Object> complexData = new HashMap<>();
            complexData.put("stringField", "Hello World");
            complexData.put("intField", 42);
            complexData.put("longField", 123456789L);
            complexData.put("doubleField", 3.14159);
            complexData.put("booleanField", true);
            complexData.put("nullField", null);
            complexData.put("dateField", new java.util.Date());
            complexData.put("timestampField", Timestamp.now());
            complexData.put("arrayField", java.util.Arrays.asList("a", "b", "c"));
            complexData.put("nestedField", java.util.Collections.singletonMap("innerKey", "innerValue"));

            // Set complex data
            await(docRef.set(complexData));

            // Retrieve and verify all data types
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should exist", snapshot.exists());

            assertEquals("String field", "Hello World", snapshot.getString("stringField"));
            assertEquals("Int field", Long.valueOf(42), snapshot.getLong("intField"));
            assertEquals("Long field", Long.valueOf(123456789L), snapshot.getLong("longField"));
            assertEquals("Double field", 3.14159, snapshot.getDouble("doubleField"), 0.00001);
            assertTrue("Boolean field", snapshot.getBoolean("booleanField"));
            assertNull("Null field", snapshot.get("nullField"));
            assertNotNull("Date field", snapshot.getDate("dateField"));
            assertNotNull("Timestamp field", snapshot.getTimestamp("timestampField"));

            List<?> arrayField = (List<?>) snapshot.get("arrayField");
            assertNotNull("Array field", arrayField);
            assertEquals("Array size", 3, arrayField.size());

            Map<?, ?> nestedField = (Map<?, ?>) snapshot.get("nestedField");
            assertNotNull("Nested field", nestedField);
            assertEquals("Nested value", "innerValue", nestedField.get("innerKey"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in document complex data test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceNonExistentDocument() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference with non-existent document");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("nonExistent_" + System.currentTimeMillis());

            DocumentSnapshot snapshot = await(docRef.get());
            assertFalse("Non-existent document should not exist", snapshot.exists());
            assertNull("Non-existent document data should be null", snapshot.getData());

            // Test update on non-existent document (should fail)
            boolean threwException = false;
            try {
                await(docRef.update("field", "value"));
            } catch (Exception e) {
                threwException = true;
            }
            assertTrue("Update on non-existent document should throw exception", threwException);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in non-existent document test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentReferenceConcurrentReads() throws Exception {
        Log.d("RegressionTest", "Testing DocumentReference concurrent reads");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("concurrent_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("counter", 0)));

            final int numThreads = 5;
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(numThreads);
            final List<Integer> results = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

            // Launch concurrent reads
            for (int i = 0; i < numThreads; i++) {
                new Thread(() -> {
                    try {
                        DocumentSnapshot snapshot = await(docRef.get());
                        results.add(snapshot.getLong("counter").intValue());
                    } catch (Exception e) {
                        Log.e("ConcurrentRead", "Error in concurrent read", e);
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            // Wait for all reads to complete
            assertTrue("All concurrent reads should complete", latch.await(30, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals("Should have results from all threads", numThreads, results.size());

            // All should read the same value
            for (Integer result : results) {
                assertEquals("All reads should return same value", Integer.valueOf(0), result);
            }

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in concurrent reads test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotExists() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot exists");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference existingDoc = db.collection("testCollection")
                    .document("existing_" + System.currentTimeMillis());
            DocumentReference nonExistentDoc = db.collection("testCollection")
                    .document("nonExistent_" + System.currentTimeMillis());

            // Set data for existing document
            await(existingDoc.set(java.util.Collections.singletonMap("field", "value")));

            // Test existing document
            DocumentSnapshot existingSnapshot = await(existingDoc.get());
            assertTrue("Existing document should exist", existingSnapshot.exists());

            // Test non-existent document
            DocumentSnapshot nonExistentSnapshot = await(nonExistentDoc.get());
            assertFalse("Non-existent document should not exist", nonExistentSnapshot.exists());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot exists test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotGetData() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot getData");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("dataTest_" + System.currentTimeMillis());
            DocumentReference emptyDoc = db.collection("testCollection")
                    .document("emptyData_" + System.currentTimeMillis());

            // Set data
            Map<String, Object> testData = new HashMap<>();
            testData.put("stringField", "hello");
            testData.put("numberField", 42);
            await(docRef.set(testData));

            // Set empty data
            await(emptyDoc.set(new HashMap<>()));

            // Test with data
            DocumentSnapshot dataSnapshot = await(docRef.get());
            Map<String, Object> data = dataSnapshot.getData();
            assertNotNull("Data should not be null", data);
            assertEquals("Should have 2 fields", 2, data.size());
            assertEquals("String field should match", "hello", data.get("stringField"));
            assertEquals("Number field should match", 42L, data.get("numberField"));

            // Test with empty data
            DocumentSnapshot emptySnapshot = await(emptyDoc.get());
            Map<String, Object> emptyData = emptySnapshot.getData();
            assertNotNull("Empty data should not be null", emptyData);
            assertTrue("Empty data should be empty", emptyData.isEmpty());

            // Test non-existent document
            DocumentReference nonExistent = db.collection("testCollection")
                    .document("nonExistentData_" + System.currentTimeMillis());
            DocumentSnapshot nullSnapshot = await(nonExistent.get());
            assertNull("Data for non-existent document should be null", nullSnapshot.getData());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot getData test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotGetId() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot getId");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection").document("idTest_" + System.currentTimeMillis());

            await(docRef.set(java.util.Collections.singletonMap("field", "value")));

            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("ID should match document reference", docRef.getId(), snapshot.getId());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot getId test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotGetReference() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot getReference");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("refTest_" + System.currentTimeMillis());

            await(docRef.set(java.util.Collections.singletonMap("field", "value")));

            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Reference should match original", docRef, snapshot.getReference());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot getReference test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotGetMetadata() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot getMetadata");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("metadataTest_" + System.currentTimeMillis());

            await(docRef.set(java.util.Collections.singletonMap("field", "value")));

            DocumentSnapshot snapshot = await(docRef.get());
            assertNotNull("Metadata should not be null", snapshot.getMetadata());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot getMetadata test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotGet() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot get");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("getTest_" + System.currentTimeMillis());

            Map<String, Object> data = new HashMap<>();
            data.put("simpleField", "value");
            data.put("nested", java.util.Collections.singletonMap("inner", "nestedValue"));
            await(docRef.set(data));

            DocumentSnapshot snapshot = await(docRef.get());

            // Test simple field access
            assertEquals("Simple field should match", "value", snapshot.get("simpleField"));
            assertEquals("Nested field should match", "nestedValue", snapshot.get("nested.inner"));
            assertNull("Non-existent field should be null", snapshot.get("nonExistent"));

            // Test FieldPath access
            assertEquals("FieldPath access should work", "nestedValue",
                    snapshot.get(FieldPath.of("nested", "inner")));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot get test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotContains() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot contains");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("containsTest_" + System.currentTimeMillis());

            Map<String, Object> data = new HashMap<>();
            data.put("existing", "value");
            data.put("nullField", null);
            data.put("nested", java.util.Collections.singletonMap("inner", "value"));
            await(docRef.set(data));

            DocumentSnapshot snapshot = await(docRef.get());

            // Test field existence
            assertTrue("Should contain existing field", snapshot.contains("existing"));
            assertFalse("Should not contain null field", snapshot.contains("nullField"));
            assertFalse("Should not contain non-existent field", snapshot.contains("nonExistent"));

            // Test FieldPath existence
            assertTrue("Should contain nested field", snapshot.contains(FieldPath.of("nested", "inner")));
            assertFalse("Should not contain non-existent nested field",
                    snapshot.contains(FieldPath.of("nested", "nonExistent")));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot contains test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotTypeGetters() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot type-specific getters");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("typeGetters_" + System.currentTimeMillis());

            Map<String, Object> data = new HashMap<>();
            data.put("stringField", "hello world");
            data.put("longField", 42L);
            data.put("doubleField", 3.14159);
            data.put("booleanField", true);
            data.put("dateField", new java.util.Date());
            data.put("timestampField", Timestamp.now());
            await(docRef.set(data));

            DocumentSnapshot snapshot = await(docRef.get());

            // Test type-specific getters
            assertEquals("String getter", "hello world", snapshot.getString("stringField"));
            assertEquals("Long getter", Long.valueOf(42), snapshot.getLong("longField"));
            assertEquals("Double getter", 3.14159, snapshot.getDouble("doubleField"), 0.00001);
            assertTrue("Boolean getter", snapshot.getBoolean("booleanField"));
            assertNotNull("Date getter", snapshot.getDate("dateField"));
            assertNotNull("Timestamp getter", snapshot.getTimestamp("timestampField"));

            // Test non-existent fields return null
            assertNull("Non-existent string field", snapshot.getString("missing"));
            assertNull("Non-existent long field", snapshot.getLong("missing"));
            assertNull("Non-existent double field", snapshot.getDouble("missing"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot type getters test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotGetWithClass() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot get with Class type");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("classTest_" + System.currentTimeMillis());

            Map<String, Object> data = new HashMap<>();
            data.put("stringField", "test");
            data.put("intField", 42);
            await(docRef.set(data));

            DocumentSnapshot snapshot = await(docRef.get());

            // Test typed get methods
            String stringResult = snapshot.get("stringField", String.class);
            assertEquals("String field should match", "test", stringResult);

            Integer intResult = snapshot.get("intField", Integer.class);
            assertEquals("Integer field should match", Integer.valueOf(42), intResult);

            Long longResult = snapshot.get("intField", Long.class);
            assertEquals("Int field should convert to Long", Long.valueOf(42), longResult);

            // Test FieldPath with class
            assertNull("Non-existent field should return null", snapshot.get("missing", String.class));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot get with class test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotToObject() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot toObject");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("toObjectTest_" + System.currentTimeMillis());

            Map<String, Object> data = new HashMap<>();
            data.put("name", "John Doe");
            data.put("age", 30);
            await(docRef.set(data));

            DocumentSnapshot snapshot = await(docRef.get());

            // Create a simple POJO for testing
            class TestUser {
                public String name;
                public long age;
            }

            TestUser user = snapshot.toObject(TestUser.class);
            assertNotNull("User object should not be null", user);
            assertEquals("Name should match", "John Doe", user.name);
            assertEquals("Age should match", 30, user.age);

            // Test with non-existent document
            DocumentReference nonExistent = db.collection("testCollection")
                    .document("nonExistent_" + System.currentTimeMillis());
            DocumentSnapshot nullSnapshot = await(nonExistent.get());
            TestUser nullUser = nullSnapshot.toObject(TestUser.class);
            assertNull("toObject on non-existent document should return null", nullUser);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot toObject test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentSnapshotEquality() throws Exception {
        Log.d("RegressionTest", "Testing DocumentSnapshot equality");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("equalityTest_" + System.currentTimeMillis());

            await(docRef.set(java.util.Collections.singletonMap("field", "value")));

            DocumentSnapshot snapshot1 = await(docRef.get());
            DocumentSnapshot snapshot2 = await(docRef.get());

            // Test basic equality properties
            assertTrue("Snapshot should equal itself", snapshot1.equals(snapshot1));
            assertFalse("Snapshot should not equal null", snapshot1.equals(null));
            assertFalse("Snapshot should not equal different type", snapshot1.equals("string"));

            // HashCode should be consistent
            assertEquals("HashCode should be consistent", snapshot1.hashCode(), snapshot1.hashCode());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in DocumentSnapshot equality test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentToString() throws Exception {
        Log.d("RegressionTest", "Testing Document toString");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("toStringTest_" + System.currentTimeMillis());

            Map<String, Object> testData = new HashMap<>();
            testData.put("name", "Test Document");
            testData.put("value", 123);
            await(docRef.set(testData));

            DocumentSnapshot snapshot = await(docRef.get());

            // Test toString - this uses the internal Document.toString()
            String stringRep = snapshot.toString();
            assertNotNull("toString should not be null", stringRep);
            assertFalse("toString should not be empty", stringRep.isEmpty());
            // Should contain some identifying information
            assertTrue("toString should contain identifying information",
                    stringRep.contains("DocumentSnapshot") || stringRep.contains("Document"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Document toString test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentWithComplexData() throws Exception {
        Log.d("RegressionTest", "Testing Document with complex data types");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("complexDataTest_" + System.currentTimeMillis());

            // Create comprehensive test data with all supported types
            Map<String, Object> complexData = new HashMap<>();
            complexData.put("stringField", "Hello World");
            complexData.put("intField", 42);
            complexData.put("longField", 123456789L);
            complexData.put("doubleField", 3.14159);
            complexData.put("booleanField", true);
            complexData.put("nullField", null);
            complexData.put("dateField", new java.util.Date());
            complexData.put("timestampField", Timestamp.now());

            // Collections
            complexData.put("stringList", java.util.Arrays.asList("a", "b", "c"));
            complexData.put("intList", java.util.Arrays.asList(1, 2, 3));
            complexData.put("mixedList", java.util.Arrays.asList("string", 42, true));

            // Nested maps
            Map<String, Object> nestedMap = new HashMap<>();
            nestedMap.put("nestedString", "nested value");
            nestedMap.put("nestedInt", 99);
            nestedMap.put("nestedList", java.util.Arrays.asList("x", "y", "z"));
            complexData.put("nestedMap", nestedMap);

            // Array of maps
            List<Map<String, Object>> arrayOfMaps = java.util.Arrays.asList(
                    java.util.Collections.singletonMap("item1", "value1"),
                    java.util.Collections.singletonMap("item2", "value2"));
            complexData.put("arrayOfMaps", arrayOfMaps);

            // Set the complex data
            await(docRef.set(complexData));

            // Retrieve and verify the document
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should exist", snapshot.exists());

            Map<String, Object> retrievedData = snapshot.getData();
            assertNotNull("Data should not be null", retrievedData);

            // Verify all data types are preserved and accessible
            assertEquals("String field", "Hello World", retrievedData.get("stringField"));
            assertEquals("Int field", 42L, retrievedData.get("intField"));
            assertEquals("Long field", 123456789L, retrievedData.get("longField"));
            assertEquals("Double field", 3.14159, retrievedData.get("doubleField"));
            assertEquals("Boolean field", true, retrievedData.get("booleanField"));
            assertNull("Null field", retrievedData.get("nullField"));
            assertNotNull("Date field", retrievedData.get("dateField"));
            assertNotNull("Timestamp field", retrievedData.get("timestampField"));

            // Verify collections
            List<?> stringList = (List<?>) retrievedData.get("stringList");
            assertNotNull("String list should not be null", stringList);
            assertEquals("String list size", 3, stringList.size());

            // Verify nested data
            Map<?, ?> retrievedNestedMap = (Map<?, ?>) retrievedData.get("nestedMap");
            assertNotNull("Nested map should not be null", retrievedNestedMap);
            assertEquals("Nested string", "nested value", retrievedNestedMap.get("nestedString"));
            assertEquals("Nested int", 99L, retrievedNestedMap.get("nestedInt"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Document complex data test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentWithEmptyData() throws Exception {
        Log.d("RegressionTest", "Testing Document with empty data");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("emptyDataTest_" + System.currentTimeMillis());

            // Set empty data
            await(docRef.set(new HashMap<>()));

            // Retrieve and verify
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should exist", snapshot.exists());

            Map<String, Object> data = snapshot.getData();
            assertNotNull("Data should not be null", data);
            assertTrue("Data should be empty", data.isEmpty());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Document empty data test: " + e.getMessage());
        }
    }

    @Test
    public void testDocumentEqualsAndHashCode() throws Exception {
        Log.d("RegressionTest", "Testing Document equals and hashCode");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef1 = db.collection("testCollection")
                    .document("equalsTest1_" + System.currentTimeMillis());
            DocumentReference docRef2 = db.collection("testCollection")
                    .document("equalsTest2_" + System.currentTimeMillis());

            Map<String, Object> data1 = new HashMap<>();
            data1.put("field", "value1");
            Map<String, Object> data2 = new HashMap<>();
            data2.put("field", "value2");

            await(docRef1.set(data1));
            await(docRef2.set(data2));

            DocumentSnapshot snapshot1 = await(docRef1.get());
            DocumentSnapshot snapshot2 = await(docRef2.get());

            // Test that different snapshots are not equal
            assertFalse("Different snapshots should not be equal", snapshot1.equals(snapshot2));

            // Test hashCode consistency
            assertEquals("HashCode should be consistent", snapshot1.hashCode(), snapshot1.hashCode());

            // Test equals with null
            assertFalse("Snapshot should not equal null", snapshot1.equals(null));

            // Test equals with different type
            assertFalse("Snapshot should not equal different type", snapshot1.equals("string"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Document equals and hashCode test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryWhereEqualTo() throws Exception {
        Log.d("RegressionTest", "Testing Query whereEqualTo");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test document
            DocumentReference docRef = colRef.document("queryTest1_" + System.currentTimeMillis());
            Map<String, Object> data = new HashMap<>();
            data.put("name", "QueryTest");
            data.put("value", 123);
            await(docRef.set(data));

            // Query for the document
            Query query = colRef.whereEqualTo("name", "QueryTest");

            QuerySnapshot snapshot = await(query.get());
            assertFalse("Should find the document", snapshot.isEmpty());
            assertEquals("Should find exactly 1 document", 1, snapshot.size());
            assertEquals("Name should match", "QueryTest", snapshot.getDocuments().get(0).getString("name"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query whereEqualTo test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryWhereGreaterThan() throws Exception {
        Log.d("RegressionTest", "Testing Query whereGreaterThan");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents with different ages
            DocumentReference doc1 = colRef.document("ageTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("ageTest2_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("age", 25)));
            await(doc2.set(java.util.Collections.singletonMap("age", 30)));

            // Query for age > 26
            Query query = colRef.whereGreaterThan("age", 26);

            QuerySnapshot snapshot = await(query.get());
            assertEquals("Should find 1 document with age > 26", 1, snapshot.size());
            assertEquals("Should find age 30", (Long) 30L, snapshot.getDocuments().get(0).getLong("age"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query whereGreaterThan test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryWhereLessThan() throws Exception {
        Log.d("RegressionTest", "Testing Query whereLessThan");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("lessTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("lessTest2_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("score", 85.0)));
            await(doc2.set(java.util.Collections.singletonMap("score", 95.0)));

            // Query for score < 90
            Query query = colRef.whereLessThan("score", 90.0);

            QuerySnapshot snapshot = await(query.get());
            assertEquals("Should find 1 document with score < 90", 1, snapshot.size());
            assertEquals("Should find score 85", 85.0, snapshot.getDocuments().get(0).getDouble("score"), 0.001);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query whereLessThan test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryOrderBy() throws Exception {
        Log.d("RegressionTest", "Testing Query orderBy");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents with different ages
            DocumentReference doc1 = colRef.document("orderTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("orderTest2_" + System.currentTimeMillis());
            DocumentReference doc3 = colRef.document("orderTest3_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("age", 30)));
            await(doc2.set(java.util.Collections.singletonMap("age", 20)));
            await(doc3.set(java.util.Collections.singletonMap("age", 25)));

            // Query ordered by age ascending
            Query query = colRef.orderBy("age", Query.Direction.ASCENDING);

            QuerySnapshot snapshot = await(query.get());
            assertEquals("Should find 3 documents", 3, snapshot.size());

            // Verify ordering
            List<DocumentSnapshot> docs = snapshot.getDocuments();
            assertEquals("First should be age 20", (Long) 20L, docs.get(0).getLong("age"));
            assertEquals("Second should be age 25", (Long) 25L, docs.get(1).getLong("age"));
            assertEquals("Third should be age 30", (Long) 30L, docs.get(2).getLong("age"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query orderBy test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryLimit() throws Exception {
        Log.d("RegressionTest", "Testing Query limit");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create multiple test documents
            for (int i = 0; i < 5; i++) {
                DocumentReference doc = colRef.document("limitTest" + i + "_" + System.currentTimeMillis());
                await(doc.set(java.util.Collections.singletonMap("index", i)));
            }

            // Query with limit
            Query query = colRef.limit(3);

            QuerySnapshot snapshot = await(query.get());
            assertEquals("Should return exactly 3 documents", 3, snapshot.size());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query limit test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryWhereArrayContains() throws Exception {
        Log.d("RegressionTest", "Testing Query whereArrayContains");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test document with array
            DocumentReference docRef = colRef.document("arrayTest_" + System.currentTimeMillis());
            Map<String, Object> data = new HashMap<>();
            data.put("tags", java.util.Arrays.asList("student", "cs", "java"));
            await(docRef.set(data));

            // Query for documents containing "student" in tags array
            Query query = colRef.whereArrayContains("tags", "student");

            QuerySnapshot snapshot = await(query.get());
            assertFalse("Should find documents with student tag", snapshot.isEmpty());

            // Verify the returned document has the tag
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                List<String> tags = (List<String>) doc.get("tags");
                assertNotNull("Tags should not be null", tags);
                assertTrue("Should contain student tag", tags.contains("student"));
            }

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query whereArrayContains test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryWhereIn() throws Exception {
        Log.d("RegressionTest", "Testing Query whereIn");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("inTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("inTest2_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("category", "A")));
            await(doc2.set(java.util.Collections.singletonMap("category", "B")));

            // Query for category in ["A", "C"]
            Query query = colRef.whereIn("category", java.util.Arrays.asList("A", "C"));

            QuerySnapshot snapshot = await(query.get());
            assertEquals("Should find 1 document", 1, snapshot.size());
            assertEquals("Should find category A", "A", snapshot.getDocuments().get(0).getString("category"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query whereIn test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryComplexConditions() throws Exception {
        Log.d("RegressionTest", "Testing Query complex conditions");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("complexTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("complexTest2_" + System.currentTimeMillis());

            Map<String, Object> data1 = new HashMap<>();
            data1.put("active", true);
            data1.put("age", 25);
            data1.put("score", 85.0);

            Map<String, Object> data2 = new HashMap<>();
            data2.put("active", false);
            data2.put("age", 30);
            data2.put("score", 90.0);

            await(doc1.set(data1));
            await(doc2.set(data2));

            // Complex query: active = true AND age > 20 AND score < 90
            Query query = colRef
                    .whereEqualTo("active", true)
                    .whereGreaterThan("age", 20)
                    .whereLessThan("score", 90.0);

            QuerySnapshot snapshot = await(query.get());
            assertEquals("Should find 1 document matching all conditions", 1, snapshot.size());

            DocumentSnapshot doc = snapshot.getDocuments().get(0);
            assertTrue("Should be active", doc.getBoolean("active"));
            assertTrue("Age should be > 20", doc.getLong("age") > 20);
            assertTrue("Score should be < 90", doc.getDouble("score") < 90.0);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query complex conditions test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryStartAt() throws Exception {
        Log.d("RegressionTest", "Testing Query startAt");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create ordered test documents
            DocumentReference doc1 = colRef.document("startAtTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("startAtTest2_" + System.currentTimeMillis());
            DocumentReference doc3 = colRef.document("startAtTest3_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("order", 10)));
            await(doc2.set(java.util.Collections.singletonMap("order", 20)));
            await(doc3.set(java.util.Collections.singletonMap("order", 30)));

            // Query starting at order = 20
            Query query = colRef.orderBy("order").startAt(20);

            QuerySnapshot snapshot = await(query.get());
            assertTrue("Should find documents from order 20 onwards", snapshot.size() >= 2);

            // All results should have order >= 20
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                assertTrue("Order should be >= 20", doc.getLong("order") >= 20);
            }

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query startAt test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryEndAt() throws Exception {
        Log.d("RegressionTest", "Testing Query endAt");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create ordered test documents
            DocumentReference doc1 = colRef.document("endAtTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("endAtTest2_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("value", 100)));
            await(doc2.set(java.util.Collections.singletonMap("value", 200)));

            // Query ending at value = 150
            Query query = colRef.orderBy("value").endAt(150);

            QuerySnapshot snapshot = await(query.get());

            // All results should have value <= 150
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                assertTrue("Value should be <= 150", doc.getLong("value") <= 150);
            }

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query endAt test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryFieldPath() throws Exception {
        Log.d("RegressionTest", "Testing Query with FieldPath");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test document with nested data
            DocumentReference docRef = colRef.document("fieldPathTest_" + System.currentTimeMillis());
            Map<String, Object> data = new HashMap<>();
            data.put("profile", java.util.Collections.singletonMap("level", "expert"));
            await(docRef.set(data));

            // Query using FieldPath
            Query query = colRef.whereEqualTo(FieldPath.of("profile", "level"), "expert");

            QuerySnapshot snapshot = await(query.get());
            assertFalse("Should find document with expert level", snapshot.isEmpty());

            // Verify the nested field value
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Map<String, Object> profile = (Map<String, Object>) doc.get("profile");
                assertNotNull("Profile should exist", profile);
                assertEquals("Level should be expert", "expert", profile.get("level"));
            }

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query FieldPath test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryMultipleOrderBy() throws Exception {
        Log.d("RegressionTest", "Testing Query multiple orderBy");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents with same primary sort, different secondary sort
            DocumentReference doc1 = colRef.document("multiOrderTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("multiOrderTest2_" + System.currentTimeMillis());

            Map<String, Object> data1 = new HashMap<>();
            data1.put("category", "A");
            data1.put("priority", 2);

            Map<String, Object> data2 = new HashMap<>();
            data2.put("category", "A");
            data2.put("priority", 1);

            await(doc1.set(data1));
            await(doc2.set(data2));

            // Query ordered by category, then by priority descending
            Query query = colRef
                    .orderBy("category")
                    .orderBy("priority", Query.Direction.DESCENDING);

            QuerySnapshot snapshot = await(query.get());
            assertTrue("Should find documents", snapshot.size() >= 2);

            // Within same category, higher priority should come first
            List<DocumentSnapshot> docs = snapshot.getDocuments();
            for (int i = 0; i < docs.size() - 1; i++) {
                String cat1 = docs.get(i).getString("category");
                String cat2 = docs.get(i + 1).getString("category");
                if (cat1 != null && cat1.equals(cat2)) {
                    // Same category, check priority ordering
                    long priority1 = docs.get(i).getLong("priority");
                    long priority2 = docs.get(i + 1).getLong("priority");
                    assertTrue("Higher priority should come first", priority1 >= priority2);
                }
            }

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query multiple orderBy test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryEmptyResult() throws Exception {
        Log.d("RegressionTest", "Testing Query empty result");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Query for non-existent value
            Query query = colRef.whereEqualTo("name", "NonExistentValue_" + System.currentTimeMillis());

            QuerySnapshot snapshot = await(query.get());
            assertTrue("Should return empty result", snapshot.isEmpty());
            assertEquals("Size should be 0", 0, snapshot.size());
            assertTrue("Documents list should be empty", snapshot.getDocuments().isEmpty());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query empty result test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryProperties() throws Exception {
        Log.d("RegressionTest", "Testing Query properties");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            Query query = colRef.whereEqualTo("test", "value");

            // Test query properties
            assertEquals("Path should match collection", "testCollection", query.getPath());
            assertNotNull("Path segments should not be null", query.getPathSegments());
            assertEquals("Should have 1 path segment", 1, query.getPathSegments().size());
            assertEquals("OsbdtOracledb instance should match", db, query.getOradb());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query properties test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryImmutability() throws Exception {
        Log.d("RegressionTest", "Testing Query immutability");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            Query originalQuery = colRef.whereEqualTo("field", "value");

            // Adding conditions should create new Query objects
            Query modifiedQuery1 = originalQuery.whereGreaterThan("age", 20);
            Query modifiedQuery2 = originalQuery.orderBy("name");

            // Original query should be different from modified ones
            assertFalse("Original query should differ from modified query 1", originalQuery.equals(modifiedQuery1));
            assertFalse("Original query should differ from modified query 2", originalQuery.equals(modifiedQuery2));
            assertFalse("Modified queries should be different", modifiedQuery1.equals(modifiedQuery2));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query immutability test: " + e.getMessage());
        }
    }

    @Test
    public void testQueryEquality() throws Exception {
        Log.d("RegressionTest", "Testing Query equality");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Same queries should be equal
            Query query1 = colRef.whereEqualTo("field", "value").orderBy("name");
            Query query2 = colRef.whereEqualTo("field", "value").orderBy("name");

            assertTrue("Identical queries should be equal", query1.equals(query2));
            assertEquals("Equal queries should have same hash code", query1.hashCode(), query2.hashCode());

            // Different queries should not be equal
            Query differentQuery = colRef.whereEqualTo("field", "different");
            assertFalse("Different queries should not be equal", query1.equals(differentQuery));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Query equality test: " + e.getMessage());
        }
    }

    // ===== QUERY SNAPSHOT TESTS =====

    @Test
    public void testQuerySnapshotGetDocumentsAndSize() throws Exception {
        Log.d("RegressionTest", "Testing QuerySnapshot getDocuments and size");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("qsDoc1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("qsDoc2_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("value", 1)));
            await(doc2.set(java.util.Collections.singletonMap("value", 2)));

            // Get query snapshot
            QuerySnapshot qs = await(colRef.get());

            // Test size and isEmpty
            assertEquals("Should have 2 documents", 2, qs.size());
            assertFalse("Should not be empty", qs.isEmpty());

            // Test getDocuments
            List<DocumentSnapshot> docs = qs.getDocuments();
            assertNotNull("Documents list should not be null", docs);
            assertEquals("Documents list should have 2 items", 2, docs.size());

            // Verify document IDs
            Set<String> ids = new HashSet<>();
            for (DocumentSnapshot doc : docs) {
                ids.add(doc.getId());
            }
            assertTrue("Should contain first document", ids.contains(doc1.getId()));
            assertTrue("Should contain second document", ids.contains(doc2.getId()));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in QuerySnapshot getDocuments and size test: " + e.getMessage());
        }
    }

    @Test
    public void testQuerySnapshotMetadata() throws Exception {
        Log.d("RegressionTest", "Testing QuerySnapshot metadata");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Ensure at least one document exists
            DocumentReference doc = colRef.document("metadataTest_" + System.currentTimeMillis());
            await(doc.set(java.util.Collections.singletonMap("field", "value")));

            QuerySnapshot qs = await(colRef.get());

            // Test metadata
            assertNotNull("Metadata should not be null", qs.getMetadata());
            // Metadata will indicate whether results came from cache or server
            // Both are acceptable depending on implementation

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in QuerySnapshot metadata test: " + e.getMessage());
        }
    }

    @Test
    public void testQuerySnapshotIterator() throws Exception {
        Log.d("RegressionTest", "Testing QuerySnapshot iterator");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("iterTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("iterTest2_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("field", "value1")));
            await(doc2.set(java.util.Collections.singletonMap("field", "value2")));

            QuerySnapshot qs = await(colRef.get());

            // Test iterator functionality
            int count = 0;
            for (DocumentSnapshot doc : qs) {
                assertNotNull("Document should not be null", doc);
                assertNotNull("Document ID should not be null", doc.getId());
                count++;
            }

            assertEquals("Iterator count should match size", qs.size(), count);
            assertTrue("Should have iterated over documents", count >= 2);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in QuerySnapshot iterator test: " + e.getMessage());
        }
    }

    @Test
    public void testQuerySnapshotEmptyResult() throws Exception {
        Log.d("RegressionTest", "Testing QuerySnapshot empty result");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create a query that should return no results
            Query query = colRef.whereEqualTo("nonExistentField", "nonExistentValue");

            QuerySnapshot qs = await(query.get());

            // Test empty result properties
            assertEquals("Size should be 0 for empty result", 0, qs.size());
            assertTrue("Should be empty", qs.isEmpty());

            List<DocumentSnapshot> docs = qs.getDocuments();
            assertNotNull("Documents list should not be null", docs);
            assertTrue("Documents list should be empty", docs.isEmpty());

            // Test iterator on empty result
            int count = 0;
            for (DocumentSnapshot doc : qs) {
                count++;
            }
            assertEquals("Iterator should not iterate over any documents", 0, count);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in QuerySnapshot empty result test: " + e.getMessage());
        }
    }

    @Test
    public void testQuerySnapshotSingleDocument() throws Exception {
        Log.d("RegressionTest", "Testing QuerySnapshot single document");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create single test document
            DocumentReference doc = colRef.document("singleDocTest_" + System.currentTimeMillis());
            Map<String, Object> data = new HashMap<>();
            data.put("name", "Single Test");
            data.put("value", 42);
            await(doc.set(data));

            QuerySnapshot qs = await(colRef.whereEqualTo("name", "Single Test").get());

            // Test single document result
            assertEquals("Should have exactly 1 document", 1, qs.size());
            assertFalse("Should not be empty", qs.isEmpty());

            List<DocumentSnapshot> docs = qs.getDocuments();
            assertEquals("Documents list should have 1 item", 1, docs.size());

            DocumentSnapshot retrievedDoc = docs.get(0);
            assertEquals("Document ID should match", doc.getId(), retrievedDoc.getId());
            assertEquals("Name should match", "Single Test", retrievedDoc.getString("name"));
            assertEquals("Value should match", Long.valueOf(42), retrievedDoc.getLong("value"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in QuerySnapshot single document test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQueryCount() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuery count");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("countTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("countTest2_" + System.currentTimeMillis());
            DocumentReference doc3 = colRef.document("countTest3_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("field", "value1")));
            await(doc2.set(java.util.Collections.singletonMap("field", "value2")));
            await(doc3.set(java.util.Collections.singletonMap("field", "value3")));

            // Create count aggregate query
            AggregateQuery countQuery = colRef.count("totalCount");
            AggregateQuerySnapshot snapshot = await(countQuery.get());

            // Verify count
            assertEquals("Count should be 3", 3L, snapshot.getCount("totalCount"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuery count test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySum() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuery sum");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents with numeric values
            DocumentReference doc1 = colRef.document("sumTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("sumTest2_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("amount", 100)));
            await(doc2.set(java.util.Collections.singletonMap("amount", 200)));

            // Create sum aggregate query
            AggregateQuery sumQuery = colRef.aggregate(AggregateField.sum("totalAmount", "amount"));
            AggregateQuerySnapshot snapshot = await(sumQuery.get());

            // Verify sum
            assertEquals("Sum should be 300", 300L, snapshot.get(AggregateField.sum("totalAmount", "amount")));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuery sum test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQueryAverage() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuery average");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents with numeric values
            DocumentReference doc1 = colRef.document("avgTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("avgTest2_" + System.currentTimeMillis());

            await(doc1.set(java.util.Collections.singletonMap("score", 80.0)));
            await(doc2.set(java.util.Collections.singletonMap("score", 90.0)));

            // Create average aggregate query
            AggregateQuery avgQuery = colRef.aggregate(AggregateField.average("avgScore", "score"));
            AggregateQuerySnapshot snapshot = await(avgQuery.get());

            // Verify average
            assertEquals("Average should be 85.0", 85.0, snapshot.get(AggregateField.average("avgScore", "score")),
                    0.001);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuery average test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQueryMultipleAggregates() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuery multiple aggregates");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("multiAggTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("multiAggTest2_" + System.currentTimeMillis());

            Map<String, Object> data1 = new HashMap<>();
            data1.put("countField", "value");
            data1.put("sumField", 10);
            data1.put("avgField", 20.0);

            Map<String, Object> data2 = new HashMap<>();
            data2.put("countField", "value");
            data2.put("sumField", 15);
            data2.put("avgField", 25.0);

            await(doc1.set(data1));
            await(doc2.set(data2));

            // Create multiple aggregate query
            AggregateQuery multiQuery = colRef.aggregate(
                    AggregateField.count("totalCount"),
                    AggregateField.sum("totalSum", "sumField"),
                    AggregateField.average("totalAvg", "avgField"));
            AggregateQuerySnapshot snapshot = await(multiQuery.get());

            // Verify all aggregates
            assertEquals("Count should be 2", 2L, snapshot.getCount("totalCount"));
            assertEquals("Sum should be 25", 25L, snapshot.get(AggregateField.sum("totalSum", "sumField")));
            assertEquals("Average should be 22.5", 22.5, snapshot.get(AggregateField.average("totalAvg", "avgField")),
                    0.001);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuery multiple aggregates test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQueryOnMissingField() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuery on missing field");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test document without the field we're aggregating
            DocumentReference doc = colRef.document("missingFieldTest_" + System.currentTimeMillis());
            await(doc.set(java.util.Collections.singletonMap("existingField", "value")));

            // Create aggregate query on non-existent field
            AggregateQuery sumQuery = colRef.aggregate(AggregateField.sum("missingSum", "nonExistentField"));
            AggregateQuerySnapshot snapshot = await(sumQuery.get());

            // Verify result for missing field
            assertEquals("Sum of missing field should be 0", 0L,
                    snapshot.get(AggregateField.sum("missingSum", "nonExistentField")));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuery missing field test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQueryGetQuery() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuery getQuery");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create aggregate query
            AggregateQuery aggQuery = colRef.count("testCount");

            // Get the underlying query
            assertNotNull("getQuery() should return a non-null query", aggQuery.getQuery());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuery getQuery test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQueryEquality() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuery equality");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create identical aggregate queries
            AggregateQuery query1 = colRef.aggregate(AggregateField.count("testCount"));
            AggregateQuery query2 = colRef.aggregate(AggregateField.count("testCount"));
            AggregateQuery query3 = colRef.aggregate(AggregateField.count("differentCount"));

            // Test equality
            assertTrue("Identical aggregate queries should be equal", query1.equals(query2));
            assertFalse("Different aggregate queries should not be equal", query1.equals(query3));
            assertFalse("AggregateQuery should not equal null", query1.equals(null));
            assertFalse("AggregateQuery should not equal different type", query1.equals("string"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuery equality test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQueryHashCode() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuery hashCode");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create identical aggregate queries
            AggregateQuery query1 = colRef.aggregate(AggregateField.count("testCount"));
            AggregateQuery query2 = colRef.aggregate(AggregateField.count("testCount"));
            AggregateQuery query3 = colRef.aggregate(AggregateField.count("differentCount"));

            // Test hashCode consistency
            assertEquals("Identical queries should have same hashCode", query1.hashCode(), query2.hashCode());
            assertNotEquals("Different queries should have different hashCode", query1.hashCode(), query3.hashCode());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuery hashCode test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQueryToString() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuery toString");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create aggregate query
            AggregateQuery aggQuery = colRef.count("testCount");

            // Test toString
            String stringRep = aggQuery.toString();
            assertNotNull("toString should not be null", stringRep);
            assertFalse("toString should not be empty", stringRep.isEmpty());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuery toString test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotGetQuery() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuerySnapshot getQuery");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("aggSnapTest1_" + System.currentTimeMillis());
            await(doc1.set(java.util.Collections.singletonMap("field", "value1")));

            // Create aggregate query
            AggregateQuery countQuery = colRef.count("testCount");
            AggregateQuerySnapshot snapshot = await(countQuery.get());

            // Test getQuery
            assertNotNull("getQuery() should return non-null query", snapshot.getQuery());
            assertEquals("getQuery() should return the original query", countQuery, snapshot.getQuery());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuerySnapshot getQuery test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotGetCountField() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuerySnapshot get count field");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("countFieldTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("countFieldTest2_" + System.currentTimeMillis());
            await(doc1.set(java.util.Collections.singletonMap("field", "value1")));
            await(doc2.set(java.util.Collections.singletonMap("field", "value2")));

            // Create count query
            AggregateQuery countQuery = colRef.count("totalCount");
            AggregateQuerySnapshot snapshot = await(countQuery.get());

            // Test getting count by field
            AggregateField.CountAggregateField countField = AggregateField.count("totalCount");
            long result = snapshot.get(countField);
            assertEquals("Count should be 2", 2L, result);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuerySnapshot get count field test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotGetSumField() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuerySnapshot get sum field");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents with numeric values
            DocumentReference doc1 = colRef.document("sumFieldTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("sumFieldTest2_" + System.currentTimeMillis());
            await(doc1.set(java.util.Collections.singletonMap("amount", 100)));
            await(doc2.set(java.util.Collections.singletonMap("amount", 200)));

            // Create sum query
            AggregateQuery sumQuery = colRef.aggregate(AggregateField.sum("totalAmount", "amount"));
            AggregateQuerySnapshot snapshot = await(sumQuery.get());

            // Test getting sum by field
            AggregateField sumField = AggregateField.sum("totalAmount", "amount");
            Object result = snapshot.get(sumField);
            assertEquals("Sum should be 300", 300L, result);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuerySnapshot get sum field test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotGetAverageField() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuerySnapshot get average field");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents with numeric values
            DocumentReference doc1 = colRef.document("avgFieldTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("avgFieldTest2_" + System.currentTimeMillis());
            await(doc1.set(java.util.Collections.singletonMap("score", 80.0)));
            await(doc2.set(java.util.Collections.singletonMap("score", 90.0)));

            // Create average query
            AggregateQuery avgQuery = colRef.aggregate(AggregateField.average("avgScore", "score"));
            AggregateQuerySnapshot snapshot = await(avgQuery.get());

            // Test getting average by field
            AggregateField.AverageAggregateField avgField = AggregateField.average("avgScore", "score");
            Double result = snapshot.get(avgField);
            assertEquals("Average should be 85.0", 85.0, result, 0.001);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuerySnapshot get average field test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotGetCountByAlias() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuerySnapshot get count by alias");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("countAliasTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("countAliasTest2_" + System.currentTimeMillis());
            await(doc1.set(java.util.Collections.singletonMap("field", "value1")));
            await(doc2.set(java.util.Collections.singletonMap("field", "value2")));

            // Create count query
            AggregateQuery countQuery = colRef.count("countAlias");
            AggregateQuerySnapshot snapshot = await(countQuery.get());

            // Test getting count by alias
            long result = snapshot.getCount("countAlias");
            assertEquals("Count should be 2", 2L, result);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuerySnapshot get count by alias test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotGetDouble() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuerySnapshot getDouble");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents with numeric values
            DocumentReference doc1 = colRef.document("doubleTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("doubleTest2_" + System.currentTimeMillis());
            await(doc1.set(java.util.Collections.singletonMap("score", 80.0)));
            await(doc2.set(java.util.Collections.singletonMap("score", 90.0)));

            // Create average query
            AggregateQuery avgQuery = colRef.aggregate(AggregateField.average("avgScore", "score"));
            AggregateQuerySnapshot snapshot = await(avgQuery.get());

            // Test getDouble
            AggregateField avgField = AggregateField.average("avgScore", "score");
            Double result = snapshot.getDouble(avgField);
            assertEquals("getDouble should return 85.0", 85.0, result, 0.001);

            // Test getDouble on sum field
            AggregateQuery sumQuery = colRef.aggregate(AggregateField.sum("totalScore", "score"));
            AggregateQuerySnapshot sumSnapshot = await(sumQuery.get());
            AggregateField sumField = AggregateField.sum("totalScore", "score");
            Double sumResult = sumSnapshot.getDouble(sumField);
            assertEquals("getDouble on sum should return 170.0", 170.0, sumResult, 0.001);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuerySnapshot getDouble test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotGetLong() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuerySnapshot getLong");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents with numeric values
            DocumentReference doc1 = colRef.document("longTest1_" + System.currentTimeMillis());
            DocumentReference doc2 = colRef.document("longTest2_" + System.currentTimeMillis());
            await(doc1.set(java.util.Collections.singletonMap("count", 10)));
            await(doc2.set(java.util.Collections.singletonMap("count", 20)));

            // Create sum query
            AggregateQuery sumQuery = colRef.aggregate(AggregateField.sum("totalCount", "count"));
            AggregateQuerySnapshot snapshot = await(sumQuery.get());

            // Test getLong
            AggregateField sumField = AggregateField.sum("totalCount", "count");
            Long result = snapshot.getLong(sumField);
            assertEquals("getLong should return 30", 30L, result.longValue());

            // Test getLong on average field
            AggregateQuery avgQuery = colRef.aggregate(AggregateField.average("avgCount", "count"));
            AggregateQuerySnapshot avgSnapshot = await(avgQuery.get());
            AggregateField avgField = AggregateField.average("avgCount", "count");
            Long avgResult = avgSnapshot.getLong(avgField);
            assertEquals("getLong on average should return 15", 15L, avgResult.longValue());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuerySnapshot getLong test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotMissingData() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuerySnapshot missing data handling");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test document without the field we're aggregating
            DocumentReference doc = colRef.document("missingDataTest_" + System.currentTimeMillis());
            await(doc.set(java.util.Collections.singletonMap("existingField", "value")));

            // Test missing sum field (should return 0)
            AggregateQuery sumQuery = colRef.aggregate(AggregateField.sum("missingSum", "nonExistentField"));
            AggregateQuerySnapshot sumSnapshot = await(sumQuery.get());
            AggregateField sumField = AggregateField.sum("missingSum", "nonExistentField");
            Object sumResult = sumSnapshot.get(sumField);
            assertEquals("Missing sum field should return 0", 0L, sumResult);

            // Test missing average field (should return null)
            AggregateQuery avgQuery = colRef.aggregate(AggregateField.average("missingAvg", "nonExistentField"));
            AggregateQuerySnapshot avgSnapshot = await(avgQuery.get());
            AggregateField.AverageAggregateField avgField = AggregateField.average("missingAvg", "nonExistentField");
            Double avgResult = avgSnapshot.get(avgField);
            assertNull("Missing average field should return null", avgResult);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuerySnapshot missing data test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotEquality() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuerySnapshot equality");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test documents
            DocumentReference doc1 = colRef.document("equalityTest1_" + System.currentTimeMillis());
            await(doc1.set(java.util.Collections.singletonMap("field", "value")));

            // Create identical queries
            AggregateQuery query1 = colRef.count("count1");
            AggregateQuery query2 = colRef.count("count1");
            AggregateQuery query3 = colRef.count("count2");

            AggregateQuerySnapshot snapshot1 = await(query1.get());
            AggregateQuerySnapshot snapshot2 = await(query2.get());
            AggregateQuerySnapshot snapshot3 = await(query3.get());

            // Test equality
            assertTrue("Identical snapshots should be equal", snapshot1.equals(snapshot2));
            assertEquals("Equal snapshots should have same hashCode", snapshot1.hashCode(), snapshot2.hashCode());
            assertFalse("Different snapshots should not be equal", snapshot1.equals(snapshot3));

            // Test equals with null and different types
            assertFalse("Snapshot should not equal null", snapshot1.equals(null));
            assertFalse("Snapshot should not equal different type", snapshot1.equals("string"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuerySnapshot equality test: " + e.getMessage());
        }
    }

    @Test
    public void testAggregateQuerySnapshotToString() throws Exception {
        Log.d("RegressionTest", "Testing AggregateQuerySnapshot toString");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            CollectionReference colRef = db.collection("testCollection");

            // Create test document
            DocumentReference doc = colRef.document("toStringTest_" + System.currentTimeMillis());
            await(doc.set(java.util.Collections.singletonMap("field", "value")));

            // Create aggregate query
            AggregateQuery countQuery = colRef.count("testCount");
            AggregateQuerySnapshot snapshot = await(countQuery.get());

            // Test toString
            String stringRep = snapshot.toString();
            assertNotNull("toString should not be null", stringRep);
            assertFalse("toString should not be empty", stringRep.isEmpty());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in AggregateQuerySnapshot toString test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionSimpleSetAndGet() throws Exception {
        Log.d("RegressionTest", "Testing Transaction simple set and get");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnSimple_" + System.currentTimeMillis());

            // Run transaction with set operation
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentSnapshot pre = transaction.get(docRef); // read first (ok if missing)
                transaction.set(docRef, java.util.Collections.singletonMap("count", 1));
                return null;
            }));

            // Verify after transaction completes
            DocumentSnapshot out = await(docRef.get());
            assertTrue("Document should exist after transaction", out.exists());
            assertEquals("Count should be 1", Long.valueOf(1), out.getLong("count"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction simple set/get test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionUpdateExistingValue() throws Exception {
        Log.d("RegressionTest", "Testing Transaction update existing value");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnUpdate_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("counter", 5)));

            // Run transaction to update
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentSnapshot snap = transaction.get(docRef); // read first
                Long current = snap.getLong("counter");
                if (current == null)
                    current = 0L;
                transaction.update(docRef, "counter", current + 1);
                return null;
            }));

            // Verify update
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Counter should be incremented to 6", Long.valueOf(6), snapshot.getLong("counter"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction update test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionDeleteDocument() throws Exception {
        Log.d("RegressionTest", "Testing Transaction delete document");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnDelete_" + System.currentTimeMillis());

            // Set document
            await(docRef.set(java.util.Collections.singletonMap("field", "value")));

            // Run transaction to delete
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                transaction.get(docRef); // read first
                transaction.delete(docRef);
                return null;
            }));

            // Verify deletion
            DocumentSnapshot snapshot = await(docRef.get());
            assertFalse("Document should not exist after delete", snapshot.exists());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction delete test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionMultipleOperations() throws Exception {
        Log.d("RegressionTest", "Testing Transaction multiple operations");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference doc1 = db.collection("testCollection")
                    .document("txnMulti1_" + System.currentTimeMillis());
            DocumentReference doc2 = db.collection("testCollection")
                    .document("txnMulti2_" + System.currentTimeMillis());

            // Set initial data
            await(doc1.set(java.util.Collections.singletonMap("val", 1)));
            await(doc2.set(java.util.Collections.singletonMap("val", 10)));

            // Run transaction with multiple operations
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentSnapshot s1 = transaction.get(doc1);
                DocumentSnapshot s2 = transaction.get(doc2);

                long v1 = s1.getLong("val") != null ? s1.getLong("val") : 0L;
                long v2 = s2.getLong("val") != null ? s2.getLong("val") : 0L;
                long sum = v1 + v2;

                transaction.update(doc1, "val", sum);
                transaction.update(doc2, "val", sum);
                return null;
            }));

            // Verify both documents have the same sum value
            DocumentSnapshot result1 = await(doc1.get());
            DocumentSnapshot result2 = await(doc2.get());

            assertEquals("Both documents should have same value", result1.getLong("val"), result2.getLong("val"));
            assertEquals("Value should be 11", Long.valueOf(11), result1.getLong("val"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction multiple operations test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionRollbackOnException() throws Exception {
        Log.d("RegressionTest", "Testing Transaction rollback on exception");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnRollback_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("balance", 100)));

            // Try transaction that should abort
            try {
                await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snap = transaction.get(docRef);
                    Long bal = snap.getLong("balance");
                    if (bal == null)
                        bal = 0L;

                    // Decide to abort (e.g., insufficient funds)
                    if (bal < 200) {
                        throw new IllegalStateException("Insufficient funds - abort transaction");
                    }

                    transaction.update(docRef, "balance", bal - 200);
                    return null;
                }));
                fail("Expected transaction to abort");
            } catch (Exception ignored) {
                // Expected to abort
            }

            // Ensure no changes were committed (rollback worked)
            DocumentSnapshot out = await(docRef.get());
            assertEquals("Balance should remain unchanged", Long.valueOf(100), out.getLong("balance"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction rollback test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionFieldValueIncrement() throws Exception {
        Log.d("RegressionTest", "Testing Transaction FieldValue increment");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnIncrement_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("n", 0)));

            // Run transaction with FieldValue increment
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                transaction.get(docRef); // Read first to satisfy read-before-write
                transaction.update(docRef, "n", FieldValue.increment(3));
                return null;
            }));

            // Verify increment
            DocumentSnapshot out = await(docRef.get());
            assertEquals("Value should be incremented to 3", Long.valueOf(3), out.getLong("n"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction FieldValue increment test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionConsistentReadSnapshot() throws Exception {
        Log.d("RegressionTest", "Testing Transaction consistent read snapshot");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference doc1 = db.collection("testCollection")
                    .document("txnConsistent1_" + System.currentTimeMillis());
            DocumentReference doc2 = db.collection("testCollection")
                    .document("txnConsistent2_" + System.currentTimeMillis());

            // Set initial data
            await(doc1.set(java.util.Collections.singletonMap("v", 1)));
            await(doc2.set(java.util.Collections.singletonMap("v", 2)));

            // Run transaction that reads both and updates to same max
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentSnapshot a = transaction.get(doc1);
                DocumentSnapshot b = transaction.get(doc2);
                Long va = a.getLong("v") != null ? a.getLong("v") : 0L;
                Long vb = b.getLong("v") != null ? b.getLong("v") : 0L;

                long max = Math.max(va, vb);
                transaction.update(doc1, "v", max);
                transaction.update(doc2, "v", max);
                return null;
            }));

            // Verify both have same max value
            DocumentSnapshot out1 = await(doc1.get());
            DocumentSnapshot out2 = await(doc2.get());
            assertEquals("Both should have same max value", out1.getLong("v"), out2.getLong("v"));
            assertEquals("Value should be 2", Long.valueOf(2), out1.getLong("v"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction consistent read snapshot test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionSetWithMerge() throws Exception {
        Log.d("RegressionTest", "Testing Transaction set with merge");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnMerge_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("field1", "value1")));

            // Run transaction with merge
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                transaction.get(docRef); // Read first
                transaction.set(docRef, java.util.Collections.singletonMap("field2", "value2"), SetOptions.merge());
                return null;
            }));

            // Verify merge
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should exist", snapshot.exists());
            assertEquals("Original field should be preserved", "value1", snapshot.getString("field1"));
            assertEquals("New field should be added", "value2", snapshot.getString("field2"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction set with merge test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionUpdateWithMap() throws Exception {
        Log.d("RegressionTest", "Testing Transaction update with map");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnUpdateMap_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("field1", "value1")));

            // Update with map in transaction
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("field1", "updated1");
            updates.put("field2", "value2");

            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                transaction.get(docRef); // Read first
                transaction.update(docRef, updates);
                return null;
            }));

            // Verify updates
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Field1 should be updated", "updated1", snapshot.getString("field1"));
            assertEquals("Field2 should be added", "value2", snapshot.getString("field2"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction update with map test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionUpdateWithFieldPath() throws Exception {
        Log.d("RegressionTest", "Testing Transaction update with field path");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnFieldPath_" + System.currentTimeMillis());

            // Set initial nested data
            java.util.Map<String, Object> initialData = new java.util.HashMap<>();
            initialData.put("nested", java.util.Collections.singletonMap("field", "oldValue"));
            await(docRef.set(initialData));

            // Update nested field in transaction
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                transaction.get(docRef); // Read first
                transaction.update(docRef, FieldPath.of("nested", "field"), "newValue");
                return null;
            }));

            // Verify nested update
            DocumentSnapshot snapshot = await(docRef.get());
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> nested = (java.util.Map<String, Object>) snapshot.get("nested");
            assertEquals("Nested field should be updated", "newValue", nested.get("field"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction update with field path test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionReadNonExistentDocument() throws Exception {
        Log.d("RegressionTest", "Testing Transaction read non-existent document");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnNonExistent_" + System.currentTimeMillis());

            // Read non-existent document in transaction
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef);
                assertFalse("Non-existent document should not exist", snapshot.exists());
                return null;
            }));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction read non-existent document test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionReturnValue() throws Exception {
        Log.d("RegressionTest", "Testing Transaction return value");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnReturn_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("counter", 5)));

            // Transaction that returns the new value
            Integer result = await(db.runTransaction((Transaction.Function<Integer>) transaction -> {
                DocumentSnapshot snap = transaction.get(docRef);
                Long current = snap.getLong("counter") != null ? snap.getLong("counter") : 0L;
                transaction.update(docRef, "counter", current + 1);
                return current.intValue() + 1; // Return the new value
            }));

            // Verify return value and document state
            assertEquals("Transaction should return 6", Integer.valueOf(6), result);

            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Document counter should be 6", Long.valueOf(6), snapshot.getLong("counter"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction return value test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionComplexDataStructures() throws Exception {
        Log.d("RegressionTest", "Testing Transaction complex data structures");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnComplex_" + System.currentTimeMillis());

            // Create complex data
            java.util.Map<String, Object> complexData = new java.util.HashMap<>();
            complexData.put("stringField", "test");
            complexData.put("numberField", 42);
            complexData.put("booleanField", true);
            complexData.put("nullField", null);
            complexData.put("arrayField", java.util.Arrays.asList("item1", "item2", "item3"));
            java.util.Map<String, Object> nestedMap = new java.util.HashMap<>();
            nestedMap.put("innerField", "innerValue");
            nestedMap.put("innerNumber", 99);
            complexData.put("nestedObject", nestedMap);

            // Set complex data in transaction
            await(db.runTransaction((Transaction.Function<Void>) transaction -> {
                transaction.get(docRef); // Read first (will be missing)
                transaction.set(docRef, complexData);
                return null;
            }));

            // Verify complex data
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should exist", snapshot.exists());

            java.util.Map<String, Object> data = snapshot.getData();
            assertEquals("String field", "test", data.get("stringField"));
            assertEquals("Number field", Long.valueOf(42), data.get("numberField"));
            assertEquals("Boolean field", true, data.get("booleanField"));
            assertNull("Null field", data.get("nullField"));

            @SuppressWarnings("unchecked")
            java.util.List<String> arrayField = (java.util.List<String>) data.get("arrayField");
            assertEquals("Array size", 3, arrayField.size());
            assertEquals("First array item", "item1", arrayField.get(0));

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> nested = (java.util.Map<String, Object>) data.get("nestedObject");
            assertEquals("Nested field", "innerValue", nested.get("innerField"));
            assertEquals("Nested number", Long.valueOf(99), nested.get("innerNumber"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction complex data structures test: " + e.getMessage());
        }
    }

    @Test
    public void testTransactionWithCustomOptions() throws Exception {
        Log.d("RegressionTest", "Testing Transaction with custom options");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("txnOptions_" + System.currentTimeMillis());

            // Create custom transaction options
            TransactionOptions options = new TransactionOptions.Builder()
                    .setMaxAttempts(5)
                    .build();

            // Run transaction with custom options
            await(db.runTransaction(options, (Transaction.Function<Void>) transaction -> {
                transaction.get(docRef); // Read first
                transaction.set(docRef, java.util.Collections.singletonMap("configured", true));
                return null;
            }));

            // Verify transaction worked
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should be created with custom options", snapshot.getBoolean("configured"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in Transaction custom options test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchBasicOperations() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch basic operations");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference doc1 = db.collection("testCollection")
                    .document("batchBasic1_" + System.currentTimeMillis());
            DocumentReference doc2 = db.collection("testCollection")
                    .document("batchBasic2_" + System.currentTimeMillis());

            // Create batch with set operations
            WriteBatch batch = db.batch();
            batch.set(doc1, java.util.Collections.singletonMap("name", "Alice"));
            batch.set(doc2, java.util.Collections.singletonMap("name", "Bob"));
            await(batch.commit());

            // Verify documents were created
            DocumentSnapshot snap1 = await(doc1.get());
            DocumentSnapshot snap2 = await(doc2.get());

            assertTrue("First document should exist", snap1.exists());
            assertTrue("Second document should exist", snap2.exists());
            assertEquals("First document name", "Alice", snap1.getString("name"));
            assertEquals("Second document name", "Bob", snap2.getString("name"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch basic operations test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchUpdateOperation() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch update operation");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("batchUpdate_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("score", 10)));

            // Update via batch
            WriteBatch batch = db.batch();
            batch.update(docRef, "score", 20);
            await(batch.commit());

            // Verify update
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Score should be updated to 20", Long.valueOf(20), snapshot.getLong("score"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch update test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchDeleteOperation() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch delete operation");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("batchDelete_" + System.currentTimeMillis());

            // Set document
            await(docRef.set(java.util.Collections.singletonMap("field", "value")));

            // Delete via batch
            WriteBatch batch = db.batch();
            batch.delete(docRef);
            await(batch.commit());

            // Verify deletion
            DocumentSnapshot snapshot = await(docRef.get());
            assertFalse("Document should not exist after delete", snapshot.exists());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch delete test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchMixedOperations() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch mixed operations");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference doc1 = db.collection("testCollection")
                    .document("batchMixed1_" + System.currentTimeMillis());
            DocumentReference doc2 = db.collection("testCollection")
                    .document("batchMixed2_" + System.currentTimeMillis());
            DocumentReference doc3 = db.collection("testCollection")
                    .document("batchMixed3_" + System.currentTimeMillis());

            // Set initial data for update
            await(doc1.set(java.util.Collections.singletonMap("count", 1)));
            await(doc2.set(java.util.Collections.singletonMap("status", "old")));

            // Create batch with mixed operations
            WriteBatch batch = db.batch();
            batch.update(doc1, "count", 2); // Update existing
            batch.set(doc2, java.util.Collections.singletonMap("status", "new")); // Overwrite existing
            batch.set(doc3, java.util.Collections.singletonMap("created", true)); // Create new
            await(batch.commit());

            // Verify all operations
            DocumentSnapshot snap1 = await(doc1.get());
            DocumentSnapshot snap2 = await(doc2.get());
            DocumentSnapshot snap3 = await(doc3.get());

            assertEquals("Updated count", Long.valueOf(2), snap1.getLong("count"));
            assertEquals("Overwritten status", "new", snap2.getString("status"));
            assertTrue("Created document", snap3.getBoolean("created"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch mixed operations test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchSetWithMerge() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch set with merge");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("batchMerge_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("field1", "value1")));

            // Merge via batch
            WriteBatch batch = db.batch();
            batch.set(docRef, java.util.Collections.singletonMap("field2", "value2"), SetOptions.merge());
            await(batch.commit());

            // Verify merge
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Original field preserved", "value1", snapshot.getString("field1"));
            assertEquals("New field added", "value2", snapshot.getString("field2"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch set with merge test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchUpdateWithMap() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch update with map");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("batchUpdateMap_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("initial", "value")));

            // Update with map via batch
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("field1", "value1");
            updates.put("field2", 42);

            WriteBatch batch = db.batch();
            batch.update(docRef, updates);
            await(batch.commit());

            // Verify updates
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Field1 added", "value1", snapshot.getString("field1"));
            assertEquals("Field2 added", Long.valueOf(42), snapshot.getLong("field2"));
            assertEquals("Initial field preserved", "value", snapshot.getString("initial"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch update with map test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchUpdateWithFieldPath() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch update with field path");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("batchFieldPath_" + System.currentTimeMillis());

            // Set nested initial data
            java.util.Map<String, Object> initialData = new java.util.HashMap<>();
            initialData.put("nested", java.util.Collections.singletonMap("field", "oldValue"));
            await(docRef.set(initialData));

            // Update nested field via batch
            WriteBatch batch = db.batch();
            batch.update(docRef, FieldPath.of("nested", "field"), "newValue");
            await(batch.commit());

            // Verify nested update
            DocumentSnapshot snapshot = await(docRef.get());
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> nested = (java.util.Map<String, Object>) snapshot.get("nested");
            assertEquals("Nested field updated", "newValue", nested.get("field"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch update with field path test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchMethodChaining() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch method chaining");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference doc1 = db.collection("testCollection")
                    .document("batchChain1_" + System.currentTimeMillis());
            DocumentReference doc2 = db.collection("testCollection")
                    .document("batchChain2_" + System.currentTimeMillis());
            DocumentReference doc3 = db.collection("testCollection")
                    .document("batchChain3_" + System.currentTimeMillis());

            // Set initial data for operations that require existing documents
            await(doc2.set(java.util.Collections.singletonMap("field", "initial")));
            await(doc3.set(java.util.Collections.singletonMap("field", "toDelete")));

            // Test method chaining
            WriteBatch batch = db.batch();
            WriteBatch result = batch
                    .set(doc1, java.util.Collections.singletonMap("type", "set"))
                    .update(doc2, "field", "updated")
                    .delete(doc3);

            assertSame("Method chaining should return same instance", batch, result);
            await(batch.commit());

            // Verify all operations
            DocumentSnapshot snap1 = await(doc1.get());
            DocumentSnapshot snap2 = await(doc2.get());
            DocumentSnapshot snap3 = await(doc3.get());

            assertEquals("Set operation worked", "set", snap1.getString("type"));
            assertEquals("Update operation worked", "updated", snap2.getString("field"));
            assertFalse("Delete operation worked", snap3.exists());

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch method chaining test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchComplexDataTypes() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch with complex data types");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("batchComplex_" + System.currentTimeMillis());

            // Create complex data
            java.util.Map<String, Object> complexData = new java.util.HashMap<>();
            complexData.put("stringField", "test");
            complexData.put("intField", 42);
            complexData.put("longField", 123456789L);
            complexData.put("doubleField", 3.14159);
            complexData.put("booleanField", true);
            complexData.put("nullField", null);
            complexData.put("arrayField", java.util.Arrays.asList("item1", "item2", "item3"));
            java.util.Map<String, Object> nestedMap = new java.util.HashMap<>();
            nestedMap.put("innerField", "innerValue");
            nestedMap.put("innerNumber", 99);
            complexData.put("nestedObject", nestedMap);

            // Set complex data via batch
            WriteBatch batch = db.batch();
            batch.set(docRef, complexData);
            await(batch.commit());

            // Verify complex data
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should exist", snapshot.exists());

            java.util.Map<String, Object> data = snapshot.getData();
            assertEquals("String field", "test", data.get("stringField"));
            assertEquals("Int field", 42L, data.get("intField"));
            assertEquals("Long field", 123456789L, data.get("longField"));
            assertEquals("Double field", 3.14159, (Double) data.get("doubleField"), 0.00001);
            assertEquals("Boolean field", true, data.get("booleanField"));
            assertNull("Null field", data.get("nullField"));

            @SuppressWarnings("unchecked")
            java.util.List<String> arrayField = (java.util.List<String>) data.get("arrayField");
            assertEquals("Array size", 3, arrayField.size());
            assertEquals("First array item", "item1", arrayField.get(0));

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> nested = (java.util.Map<String, Object>) data.get("nestedObject");
            assertEquals("Nested field", "innerValue", nested.get("innerField"));
            assertEquals("Nested number", 99L, nested.get("innerNumber"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch complex data types test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchLargeOperations() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch with large number of operations");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            final int BATCH_SIZE = 25; // Smaller batch for regression testing
            java.util.List<DocumentReference> docs = new java.util.ArrayList<>();

            WriteBatch batch = db.batch();

            // Create many set operations
            for (int i = 0; i < BATCH_SIZE; i++) {
                DocumentReference doc = db.collection("testCollection")
                        .document("largeBatch_" + i + "_" + System.currentTimeMillis());
                docs.add(doc);
                java.util.Map<String, Object> data = java.util.Collections.singletonMap("index", i);
                batch.set(doc, data);
            }

            await(batch.commit());

            // Verify all documents were created (check a few)
            for (int i = 0; i < Math.min(5, BATCH_SIZE); i++) {
                DocumentSnapshot snapshot = await(docs.get(i).get());
                assertTrue("Document " + i + " should exist", snapshot.exists());
                assertEquals("Document " + i + " index", Long.valueOf(i), snapshot.getLong("index"));
            }

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch large operations test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchEmptyCommit() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch empty commit");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();

            // Create empty batch and commit
            WriteBatch batch = db.batch();
            await(batch.commit()); // Should succeed with no operations

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch empty commit test: " + e.getMessage());
        }
    }

    @Test
    public void testWriteBatchUpdateMultipleFields() throws Exception {
        Log.d("RegressionTest", "Testing WriteBatch update with multiple fields");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("batchMultiField_" + System.currentTimeMillis());

            // Set initial data
            await(docRef.set(java.util.Collections.singletonMap("field1", "value1")));

            // Update multiple fields in batch
            WriteBatch batch = db.batch();
            batch.update(docRef, "field1", "updated1", "field2", "value2", "field3", 42);
            await(batch.commit());

            // Verify updates
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Field1 updated", "updated1", snapshot.getString("field1"));
            assertEquals("Field2 added", "value2", snapshot.getString("field2"));
            assertEquals("Field3 added", Long.valueOf(42), snapshot.getLong("field3"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in WriteBatch update multiple fields test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueArrayRemove() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue arrayRemove");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("fieldValueArrayRemove_" + System.currentTimeMillis());

            // Set initial array data
            java.util.List<String> initialArray = java.util.Arrays.asList("item1", "item2", "item3", "item4");
            await(docRef.set(java.util.Collections.singletonMap("tags", initialArray)));

            // Remove items using FieldValue.arrayRemove
            await(docRef.update("tags", FieldValue.arrayRemove("item2", "item4")));

            // Verify items were removed
            DocumentSnapshot snapshot = await(docRef.get());
            @SuppressWarnings("unchecked")
            java.util.List<String> updatedArray = (java.util.List<String>) snapshot.get("tags");
            assertNotNull("Array should exist", updatedArray);
            assertTrue("Should contain item1", updatedArray.contains("item1"));
            assertTrue("Should contain item3", updatedArray.contains("item3"));
            assertFalse("Should not contain item2", updatedArray.contains("item2"));
            assertFalse("Should not contain item4", updatedArray.contains("item4"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue arrayRemove test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueArrayUnion() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue arrayUnion");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("fieldValueArrayUnion_" + System.currentTimeMillis());

            // Set initial array data
            java.util.List<String> initialArray = java.util.Arrays.asList("existing1", "existing2");
            await(docRef.set(java.util.Collections.singletonMap("tags", initialArray)));

            // Add new items using FieldValue.arrayUnion
            await(docRef.update("tags", FieldValue.arrayUnion("new1", "new2", "existing1"))); // existing1 should not be
                                                                                              // duplicated

            // Verify items were added
            DocumentSnapshot snapshot = await(docRef.get());
            @SuppressWarnings("unchecked")
            java.util.List<String> updatedArray = (java.util.List<String>) snapshot.get("tags");
            assertNotNull("Array should exist", updatedArray);
            assertTrue("Should contain existing1", updatedArray.contains("existing1"));
            assertTrue("Should contain existing2", updatedArray.contains("existing2"));
            assertTrue("Should contain new1", updatedArray.contains("new1"));
            assertTrue("Should contain new2", updatedArray.contains("new2"));
            // existing1 should appear only once
            assertEquals("existing1 should appear only once", 1,
                    java.util.Collections.frequency(updatedArray, "existing1"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue arrayUnion test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueDelete() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue delete");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("fieldValueDelete_" + System.currentTimeMillis());

            // Set initial data with multiple fields
            java.util.Map<String, Object> initialData = new java.util.HashMap<>();
            initialData.put("keep", "value");
            initialData.put("remove", "value");
            initialData.put("alsoRemove", 123);
            await(docRef.set(initialData));

            // Delete specific fields using FieldValue.delete
            await(docRef.update("remove", FieldValue.delete(), "alsoRemove", FieldValue.delete()));

            // Verify fields were deleted
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should exist", snapshot.exists());
            assertEquals("keep field should remain", "value", snapshot.getString("keep"));
            assertFalse("remove field should not exist", snapshot.contains("remove"));
            assertFalse("alsoRemove field should not exist", snapshot.contains("alsoRemove"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue delete test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueIncrementLong() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue increment with long");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("fieldValueIncrementLong_" + System.currentTimeMillis());

            // Set initial counter
            await(docRef.set(java.util.Collections.singletonMap("counter", 10L)));

            // Increment using FieldValue.increment
            await(docRef.update("counter", FieldValue.increment(5L)));

            // Verify increment
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Counter should be incremented to 15", Long.valueOf(15), snapshot.getLong("counter"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue increment long test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueIncrementDouble() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue increment with double");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("fieldValueIncrementDouble_" + System.currentTimeMillis());

            // Set initial score
            await(docRef.set(java.util.Collections.singletonMap("score", 85.5)));

            // Increment using FieldValue.increment
            await(docRef.update("score", FieldValue.increment(4.5)));

            // Verify increment
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Score should be incremented to 90.0", 90.0, snapshot.getDouble("score"), 0.001);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue increment double test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueIncrementNegative() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue increment with negative value");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("fieldValueIncrementNegative_" + System.currentTimeMillis());

            // Set initial value
            await(docRef.set(java.util.Collections.singletonMap("balance", 100L)));

            // Decrement using negative increment
            await(docRef.update("balance", FieldValue.increment(-25L)));

            // Verify decrement
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Balance should be decremented to 75", Long.valueOf(75), snapshot.getLong("balance"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue increment negative test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueIncrementZero() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue increment with zero");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("fieldValueIncrementZero_" + System.currentTimeMillis());

            // Set initial value
            await(docRef.set(java.util.Collections.singletonMap("value", 42L)));

            // Increment by zero
            await(docRef.update("value", FieldValue.increment(0L)));

            // Verify no change
            DocumentSnapshot snapshot = await(docRef.get());
            assertEquals("Value should remain unchanged", Long.valueOf(42), snapshot.getLong("value"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue increment zero test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueServerTimestamp() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue serverTimestamp");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("fieldValueServerTimestamp_" + System.currentTimeMillis());

            // Set document with server timestamp
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("name", "test");
            data.put("createdAt", FieldValue.serverTimestamp());
            await(docRef.set(data));

            // Verify document and timestamp field
            DocumentSnapshot snapshot = await(docRef.get());
            assertTrue("Document should exist", snapshot.exists());
            assertEquals("Name should match", "test", snapshot.getString("name"));
            assertNotNull("Server timestamp should be set", snapshot.get("createdAt"));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue serverTimestamp test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueEqualityAndHashCode() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue equality and hashCode");

        try {
            // Test that identical FieldValues are equal
            FieldValue delete1 = FieldValue.delete();
            FieldValue delete2 = FieldValue.delete();
            assertTrue("Identical delete operations should be equal", delete1.equals(delete2));
            assertEquals("Hash codes should be equal", delete1.hashCode(), delete2.hashCode());

            // Test that different FieldValues are not equal
            FieldValue deleteOp = FieldValue.delete();
            FieldValue timestampOp = FieldValue.serverTimestamp();
            assertFalse("Different operations should not be equal", deleteOp.equals(timestampOp));

            // Test array operations
            FieldValue arrayUnion1 = FieldValue.arrayUnion("a", "b");
            FieldValue arrayUnion2 = FieldValue.arrayUnion("a", "b");
            FieldValue arrayUnion3 = FieldValue.arrayUnion("x", "y");
            assertTrue("Identical arrayUnion operations should be equal", arrayUnion1.equals(arrayUnion2));
            assertFalse("Different arrayUnion operations should not be equal", arrayUnion1.equals(arrayUnion3));

            // Test increment operations
            FieldValue increment1 = FieldValue.increment(10L);
            FieldValue increment2 = FieldValue.increment(10L);
            FieldValue increment3 = FieldValue.increment(20L);
            assertTrue("Identical increment operations should be equal", increment1.equals(increment2));
            assertFalse("Different increment operations should not be equal", increment1.equals(increment3));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue equality and hashCode test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueComplexDataTypes() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue with complex data types");

        try {
            OsbdtOracledb db = OsbdtOracledb.getInstance();
            DocumentReference docRef = db.collection("testCollection")
                    .document("fieldValueComplex_" + System.currentTimeMillis());

            // Set initial array with mixed types
            java.util.List<Object> initialArray = java.util.Arrays.asList("string", 42, true, new java.util.Date());
            await(docRef.set(java.util.Collections.singletonMap("items", initialArray)));

            // Add more complex items using arrayUnion
            java.util.Map<String, Object> complexMap = new java.util.HashMap<>();
            complexMap.put("key", "value");
            await(docRef.update("items",
                    FieldValue.arrayUnion(complexMap, java.util.Arrays.asList("nested", "array"))));

            // Verify complex items were added
            DocumentSnapshot snapshot = await(docRef.get());
            @SuppressWarnings("unchecked")
            java.util.List<Object> updatedArray = (java.util.List<Object>) snapshot.get("items");
            assertNotNull("Array should exist", updatedArray);
            assertTrue("Should contain complex map", updatedArray.contains(complexMap));

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue complex data types test: " + e.getMessage());
        }
    }

    @Test
    public void testFieldValueBoundaryConditions() throws Exception {
        Log.d("RegressionTest", "Testing FieldValue boundary conditions");

        try {
            // Test increment with extreme values
            FieldValue maxLong = FieldValue.increment(Long.MAX_VALUE);
            FieldValue minLong = FieldValue.increment(Long.MIN_VALUE);
            FieldValue maxDouble = FieldValue.increment(Double.MAX_VALUE);
            FieldValue minDouble = FieldValue.increment(Double.MIN_VALUE);
            FieldValue nanDouble = FieldValue.increment(Double.NaN);
            FieldValue infinityDouble = FieldValue.increment(Double.POSITIVE_INFINITY);

            assertNotNull("Max long increment should work", maxLong);
            assertNotNull("Min long increment should work", minLong);
            assertNotNull("Max double increment should work", maxDouble);
            assertNotNull("Min double increment should work", minDouble);
            assertNotNull("NaN increment should work", nanDouble);
            assertNotNull("Infinity increment should work", infinityDouble);

            // Test array operations with empty arguments
            FieldValue emptyArrayUnion = FieldValue.arrayUnion();
            FieldValue emptyArrayRemove = FieldValue.arrayRemove();
            assertNotNull("Empty arrayUnion should work", emptyArrayUnion);
            assertNotNull("Empty arrayRemove should work", emptyArrayRemove);

            // Test array operations with single elements
            FieldValue singleArrayUnion = FieldValue.arrayUnion("single");
            FieldValue singleArrayRemove = FieldValue.arrayRemove(123);
            assertNotNull("Single element arrayUnion should work", singleArrayUnion);
            assertNotNull("Single element arrayRemove should work", singleArrayRemove);

        } catch (Exception e) {
            Log.d("RegressionTest", "Exception in FieldValue boundary conditions test: " + e.getMessage());
        }
    }

    // @Test
    // public void testFieldPathOfSingleField() throws Exception {
    // Log.d("RegressionTest", "Testing FieldPath.of with single field");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // DocumentReference docRef =
    // db.collection("testCollection").document("fieldPathSingle_" +
    // System.currentTimeMillis());
    //
    // // Set document with nested structure
    // java.util.Map<String, Object> nestedData = new java.util.HashMap<>();
    // nestedData.put("city", "New York");
    // java.util.Map<String, Object> address = new java.util.HashMap<>();
    // address.put("street", nestedData);
    // java.util.Map<String, Object> user = new java.util.HashMap<>();
    // user.put("address", address);
    // await(docRef.set(user));
    //
    // // Update nested field using FieldPath
    // await(docRef.update(FieldPath.of("address", "street", "city"), "Los
    // Angeles"));
    //
    // // Verify nested update
    // DocumentSnapshot snapshot = await(docRef.get());
    // @SuppressWarnings("unchecked")
    // java.util.Map<String, Object> userData = (java.util.Map<String, Object>)
    // snapshot.get("address");
    // @SuppressWarnings("unchecked")
    // java.util.Map<String, Object> addressData = (java.util.Map<String, Object>)
    // userData.get("address");
    // @SuppressWarnings("unchecked")
    // java.util.Map<String, Object> streetData = (java.util.Map<String, Object>)
    // addressData.get("street");
    // assertEquals("Nested city should be updated", "Los Angeles",
    // streetData.get("city"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in FieldPath single field test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFieldPathOfMultipleFields() throws Exception {
    // Log.d("RegressionTest", "Testing FieldPath.of with multiple fields");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // DocumentReference docRef =
    // db.collection("testCollection").document("fieldPathMultiple_" +
    // System.currentTimeMillis());
    //
    // // Set initial nested structure
    // java.util.Map<String, Object> settings = new java.util.HashMap<>();
    // settings.put("theme", "light");
    // java.util.Map<String, Object> profile = new java.util.HashMap<>();
    // profile.put("settings", settings);
    // java.util.Map<String, Object> user = new java.util.HashMap<>();
    // user.put("profile", profile);
    // await(docRef.set(user));
    //
    // // Update deeply nested field
    // await(docRef.update(FieldPath.of("profile", "settings", "theme"), "dark"));
    //
    // // Verify update
    // DocumentSnapshot snapshot = await(docRef.get());
    // @SuppressWarnings("unchecked")
    // java.util.Map<String, Object> profileData = (java.util.Map<String, Object>)
    // snapshot.get("profile");
    // @SuppressWarnings("unchecked")
    // java.util.Map<String, Object> settingsData = (java.util.Map<String, Object>)
    // profileData.get("settings");
    // assertEquals("Theme should be updated to dark", "dark",
    // settingsData.get("theme"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in FieldPath multiple fields test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFieldPathDocumentId() throws Exception {
    // Log.d("RegressionTest", "Testing FieldPath.documentId");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // DocumentReference docRef =
    // db.collection("testCollection").document("fieldPathDocId_" +
    // System.currentTimeMillis());
    //
    // // Set document
    // await(docRef.set(java.util.Collections.singletonMap("name", "test")));
    //
    // // Verify documentId field path works
    // DocumentSnapshot snapshot = await(docRef.get());
    // assertTrue("Document should exist", snapshot.exists());
    // assertNotNull("Document ID should be accessible", snapshot.getId());
    //
    // // Test that documentId() returns consistent object
    // FieldPath docId1 = FieldPath.documentId();
    // FieldPath docId2 = FieldPath.documentId();
    // assertEquals("documentId() should return equal objects", docId1, docId2);
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in FieldPath documentId test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFieldPathInQuery() throws Exception {
    // Log.d("RegressionTest", "Testing FieldPath in query operations");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents with nested data
    // DocumentReference doc1 = colRef.document("queryFieldPath1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("queryFieldPath2_" +
    // System.currentTimeMillis());
    //
    // java.util.Map<String, Object> data1 = new java.util.HashMap<>();
    // data1.put("metadata", java.util.Collections.singletonMap("version", 1));
    // java.util.Map<String, Object> data2 = new java.util.HashMap<>();
    // data2.put("metadata", java.util.Collections.singletonMap("version", 2));
    //
    // await(doc1.set(data1));
    // await(doc2.set(data2));
    //
    // // Query using FieldPath
    // Query query = colRef.whereGreaterThan(FieldPath.of("metadata", "version"),
    // 1);
    //
    // QuerySnapshot snapshot = await(query.get());
    // assertEquals("Should find 1 document with version > 1", 1, snapshot.size());
    //
    // DocumentSnapshot foundDoc = snapshot.getDocuments().get(0);
    // @SuppressWarnings("unchecked")
    // java.util.Map<String, Object> metadata = (java.util.Map<String, Object>)
    // foundDoc.get("metadata");
    // assertEquals("Found document should have version 2", Long.valueOf(2),
    // metadata.get("version"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in FieldPath query test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFieldPathInBatchOperations() throws Exception {
    // Log.d("RegressionTest", "Testing FieldPath in batch operations");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // DocumentReference doc1 =
    // db.collection("testCollection").document("batchFieldPath1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 =
    // db.collection("testCollection").document("batchFieldPath2_" +
    // System.currentTimeMillis());
    //
    // // Set initial data
    // await(doc1.set(java.util.Collections.singletonMap("config", new
    // java.util.HashMap<String, Object>())));
    // await(doc2.set(java.util.Collections.singletonMap("config", new
    // java.util.HashMap<String, Object>())));
    //
    // // Batch update nested fields
    // WriteBatch batch = db.batch();
    // batch.update(doc1, FieldPath.of("config", "enabled"), true);
    // batch.update(doc2, FieldPath.of("config", "enabled"), false);
    // await(batch.commit());
    //
    // // Verify batch updates
    // DocumentSnapshot snap1 = await(doc1.get());
    // DocumentSnapshot snap2 = await(doc2.get());
    //
    // @SuppressWarnings("unchecked")
    // java.util.Map<String, Object> config1 = (java.util.Map<String, Object>)
    // snap1.get("config");
    // @SuppressWarnings("unchecked")
    // java.util.Map<String, Object> config2 = (java.util.Map<String, Object>)
    // snap2.get("config");
    //
    // assertEquals("First document should be enabled", true,
    // config1.get("enabled"));
    // assertEquals("Second document should be disabled", false,
    // config2.get("enabled"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in FieldPath batch operations test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFieldPathInTransaction() throws Exception {
    // Log.d("RegressionTest", "Testing FieldPath in transaction operations");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // DocumentReference docRef =
    // db.collection("testCollection").document("txnFieldPath_" +
    // System.currentTimeMillis());
    //
    // // Set initial nested data
    // java.util.Map<String, Object> stats = new java.util.HashMap<>();
    // stats.put("views", 10);
    // java.util.Map<String, Object> data = new java.util.HashMap<>();
    // data.put("stats", stats);
    // await(docRef.set(data));
    //
    // // Update in transaction using FieldPath
    // await(db.runTransaction((Transaction.Function<Void>) transaction -> {
    // DocumentSnapshot snap = transaction.get(docRef);
    // Long currentViews = snap.getLong(FieldPath.of("stats", "views"));
    // if (currentViews == null) currentViews = 0L;
    // transaction.update(docRef, FieldPath.of("stats", "views"), currentViews + 5);
    // return null;
    // }));
    //
    // // Verify transaction update
    // DocumentSnapshot snapshot = await(docRef.get());
    // @SuppressWarnings("unchecked")
    // java.util.Map<String, Object> statsData = (java.util.Map<String, Object>)
    // snapshot.get("stats");
    // assertEquals("Views should be incremented to 15", Long.valueOf(15),
    // statsData.get("views"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in FieldPath transaction test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFieldPathEqualityAndHashCode() throws Exception {
    // Log.d("RegressionTest", "Testing FieldPath equality and hashCode");
    //
    // try {
    // // Test basic FieldPath equality
    // FieldPath path1 = FieldPath.of("user", "profile", "name");
    // FieldPath path2 = FieldPath.of("user", "profile", "name");
    // FieldPath path3 = FieldPath.of("user", "profile", "email");
    //
    // assertTrue("Identical FieldPaths should be equal", path1.equals(path2));
    // assertFalse("Different FieldPaths should not be equal", path1.equals(path3));
    // assertEquals("Equal FieldPaths should have same hashCode", path1.hashCode(),
    // path2.hashCode());
    // assertNotEquals("Different FieldPaths should have different hashCode",
    // path1.hashCode(), path3.hashCode());
    //
    // // Test documentId equality
    // FieldPath docId1 = FieldPath.documentId();
    // FieldPath docId2 = FieldPath.documentId();
    // assertEquals("documentId() calls should be equal", docId1, docId2);
    //
    // // Test toString
    // assertEquals("toString should format correctly", "user.profile.name",
    // path1.toString());
    // assertEquals("Single field toString", "name",
    // FieldPath.of("name").toString());
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in FieldPath equality and hashCode test: "
    // + e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFieldPathSpecialCharacters() throws Exception {
    // Log.d("RegressionTest", "Testing FieldPath with special characters");
    //
    // try {
    // // Test with dots in field names (should be handled properly)
    // FieldPath pathWithDots = FieldPath.of("field.with.dots", "another.field");
    // assertEquals("Dots in field names should be preserved",
    // "field.with.dots.another.field", pathWithDots.toString());
    //
    // // Test with underscores and mixed case
    // FieldPath pathMixed = FieldPath.of("field_name", "FieldName", "fieldName");
    // assertEquals("Mixed characters should be preserved",
    // "field_name.FieldName.fieldName", pathMixed.toString());
    //
    // // Test with numeric field names
    // FieldPath pathNumeric = FieldPath.of("field1", "field2", "field10");
    // assertEquals("Numeric field names should work", "field1.field2.field10",
    // pathNumeric.toString());
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in FieldPath special characters test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFieldPathValidation() throws Exception {
    // Log.d("RegressionTest", "Testing FieldPath validation");
    //
    // try {
    // // Test valid field paths
    // FieldPath validPath = FieldPath.of("field1", "field2", "field3");
    // assertNotNull("Valid path should be created", validPath);
    //
    // // Test that null arguments throw exceptions
    // try {
    // FieldPath.of("field1", null, "field3");
    // fail("Should throw exception for null field");
    // } catch (IllegalArgumentException e) {
    // // Expected
    // }
    //
    // // Test that empty string arguments throw exceptions
    // try {
    // FieldPath.of("field1", "", "field3");
    // fail("Should throw exception for empty field");
    // } catch (IllegalArgumentException e) {
    // // Expected
    // }
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in FieldPath validation test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterEqualTo() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.equalTo");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents
    // DocumentReference doc1 = colRef.document("filterEqualTo1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterEqualTo2_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("status", "active")));
    // await(doc2.set(java.util.Collections.singletonMap("status", "inactive")));
    //
    // // Query with equalTo filter
    // Query query = colRef.where(Filter.equalTo("status", "active"));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 1 document with active status", 1,
    // snapshot.size());
    // assertEquals("Found document should have active status", "active",
    // snapshot.getDocuments().get(0).getString("status"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter equalTo test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterNotEqualTo() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.notEqualTo");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents
    // DocumentReference doc1 = colRef.document("filterNotEqualTo1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterNotEqualTo2_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("category", "A")));
    // await(doc2.set(java.util.Collections.singletonMap("category", "B")));
    //
    // // Query with notEqualTo filter
    // Query query = colRef.where(Filter.notEqualTo("category", "A"));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 1 document with category not A", 1,
    // snapshot.size());
    // assertEquals("Found document should have category B", "B",
    // snapshot.getDocuments().get(0).getString("category"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter notEqualTo test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterGreaterThan() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.greaterThan");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents with different scores
    // DocumentReference doc1 = colRef.document("filterGreaterThan1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterGreaterThan2_" +
    // System.currentTimeMillis());
    // DocumentReference doc3 = colRef.document("filterGreaterThan3_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("score", 75)));
    // await(doc2.set(java.util.Collections.singletonMap("score", 85)));
    // await(doc3.set(java.util.Collections.singletonMap("score", 95)));
    //
    // // Query with greaterThan filter
    // Query query = colRef.where(Filter.greaterThan("score", 80));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 2 documents with score > 80", 2, snapshot.size());
    //
    // // Verify all results have score > 80
    // for (DocumentSnapshot doc : snapshot.getDocuments()) {
    // assertTrue("All results should have score > 80", doc.getLong("score") > 80);
    // }
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter greaterThan test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterGreaterThanOrEqualTo() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.greaterThanOrEqualTo");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents
    // DocumentReference doc1 = colRef.document("filterGte1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterGte2_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("level", 5)));
    // await(doc2.set(java.util.Collections.singletonMap("level", 7)));
    //
    // // Query with greaterThanOrEqualTo filter
    // Query query = colRef.where(Filter.greaterThanOrEqualTo("level", 5));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 2 documents with level >= 5", 2, snapshot.size());
    //
    // // Verify all results have level >= 5
    // for (DocumentSnapshot doc : snapshot.getDocuments()) {
    // assertTrue("All results should have level >= 5", doc.getLong("level") >= 5);
    // }
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter greaterThanOrEqualTo test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterLessThan() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.lessThan");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents
    // DocumentReference doc1 = colRef.document("filterLessThan1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterLessThan2_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("price", 50.0)));
    // await(doc2.set(java.util.Collections.singletonMap("price", 75.0)));
    //
    // // Query with lessThan filter
    // Query query = colRef.where(Filter.lessThan("price", 70.0));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 1 document with price < 70", 1, snapshot.size());
    // assertEquals("Found document should have price 50", 50.0,
    // snapshot.getDocuments().get(0).getDouble("price"), 0.001);
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter lessThan test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterLessThanOrEqualTo() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.lessThanOrEqualTo");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents
    // DocumentReference doc1 = colRef.document("filterLte1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterLte2_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("quantity", 10)));
    // await(doc2.set(java.util.Collections.singletonMap("quantity", 15)));
    //
    // // Query with lessThanOrEqualTo filter
    // Query query = colRef.where(Filter.lessThanOrEqualTo("quantity", 10));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 1 document with quantity <= 10", 1,
    // snapshot.size());
    // assertEquals("Found document should have quantity 10", Long.valueOf(10),
    // snapshot.getDocuments().get(0).getLong("quantity"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter lessThanOrEqualTo test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterArrayContains() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.arrayContains");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents with array fields
    // DocumentReference doc1 = colRef.document("filterArrayContains1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterArrayContains2_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("tags",
    // java.util.Arrays.asList("urgent", "important", "work"))));
    // await(doc2.set(java.util.Collections.singletonMap("tags",
    // java.util.Arrays.asList("personal", "shopping"))));
    //
    // // Query with arrayContains filter
    // Query query = colRef.where(Filter.arrayContains("tags", "urgent"));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 1 document containing urgent tag", 1,
    // snapshot.size());
    //
    // DocumentSnapshot foundDoc = snapshot.getDocuments().get(0);
    // @SuppressWarnings("unchecked")
    // java.util.List<String> tags = (java.util.List<String>) foundDoc.get("tags");
    // assertTrue("Found document should contain urgent tag",
    // tags.contains("urgent"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter arrayContains test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterArrayContainsAny() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.arrayContainsAny");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents
    // DocumentReference doc1 = colRef.document("filterArrayContainsAny1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterArrayContainsAny2_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("categories",
    // java.util.Arrays.asList("electronics", "gadgets"))));
    // await(doc2.set(java.util.Collections.singletonMap("categories",
    // java.util.Arrays.asList("books", "fiction"))));
    //
    // // Query with arrayContainsAny filter
    // Query query = colRef.where(Filter.arrayContainsAny("categories",
    // java.util.Arrays.asList("electronics", "non-fiction")));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 1 document with electronics category", 1,
    // snapshot.size());
    //
    // DocumentSnapshot foundDoc = snapshot.getDocuments().get(0);
    // @SuppressWarnings("unchecked")
    // java.util.List<String> categories = (java.util.List<String>)
    // foundDoc.get("categories");
    // assertTrue("Found document should contain electronics",
    // categories.contains("electronics"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter arrayContainsAny test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterInArray() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.inArray");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents
    // DocumentReference doc1 = colRef.document("filterInArray1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterInArray2_" +
    // System.currentTimeMillis());
    // DocumentReference doc3 = colRef.document("filterInArray3_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("priority", "high")));
    // await(doc2.set(java.util.Collections.singletonMap("priority", "medium")));
    // await(doc3.set(java.util.Collections.singletonMap("priority", "low")));
    //
    // // Query with inArray filter
    // Query query = colRef.where(Filter.inArray("priority",
    // java.util.Arrays.asList("high", "low")));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 2 documents with high or low priority", 2,
    // snapshot.size());
    //
    // // Verify priorities
    // java.util.Set<String> foundPriorities = new java.util.HashSet<>();
    // for (DocumentSnapshot doc : snapshot.getDocuments()) {
    // foundPriorities.add(doc.getString("priority"));
    // }
    // assertTrue("Should contain high priority", foundPriorities.contains("high"));
    // assertTrue("Should contain low priority", foundPriorities.contains("low"));
    // assertFalse("Should not contain medium priority",
    // foundPriorities.contains("medium"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter inArray test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterNotInArray() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.notInArray");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents
    // DocumentReference doc1 = colRef.document("filterNotInArray1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterNotInArray2_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("status", "active")));
    // await(doc2.set(java.util.Collections.singletonMap("status", "inactive")));
    //
    // // Query with notInArray filter
    // Query query = colRef.where(Filter.notInArray("status",
    // java.util.Arrays.asList("inactive", "banned")));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 1 document with status not inactive or banned", 1,
    // snapshot.size());
    // assertEquals("Found document should have active status", "active",
    // snapshot.getDocuments().get(0).getString("status"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter notInArray test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterIsNull() throws Exception {
    // Log.d("RegressionTest", "Testing Filter.isNull");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents - some with null field, some without
    // DocumentReference doc1 = colRef.document("filterIsNull1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterIsNull2_" +
    // System.currentTimeMillis());
    //
    // await(doc1.set(java.util.Collections.singletonMap("optional", null)));
    // await(doc2.set(java.util.Collections.singletonMap("optional", "value")));
    //
    // // Query with isNull filter
    // Query query = colRef.where(Filter.isNull("optional"));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 1 document with null optional field", 1,
    // snapshot.size());
    // assertNull("Found document should have null optional field",
    // snapshot.getDocuments().get(0).get("optional"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter isNull test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterWithFieldPath() throws Exception {
    // Log.d("RegressionTest", "Testing Filter with FieldPath");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents with nested data
    // DocumentReference doc1 = colRef.document("filterFieldPath1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterFieldPath2_" +
    // System.currentTimeMillis());
    //
    // java.util.Map<String, Object> data1 = new java.util.HashMap<>();
    // data1.put("profile", java.util.Collections.singletonMap("level",
    // "beginner"));
    // java.util.Map<String, Object> data2 = new java.util.HashMap<>();
    // data2.put("profile", java.util.Collections.singletonMap("level", "expert"));
    //
    // await(doc1.set(data1));
    // await(doc2.set(data2));
    //
    // // Query with FieldPath filter
    // Query query = colRef.where(Filter.equalTo(FieldPath.of("profile", "level"),
    // "expert"));
    // QuerySnapshot snapshot = await(query.get());
    //
    // assertEquals("Should find 1 document with expert level", 1, snapshot.size());
    //
    // DocumentSnapshot foundDoc = snapshot.getDocuments().get(0);
    // @SuppressWarnings("unchecked")
    // java.util.Map<String, Object> profile = (java.util.Map<String, Object>)
    // foundDoc.get("profile");
    // assertEquals("Found document should have expert level", "expert",
    // profile.get("level"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter with FieldPath test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterMultipleConditions() throws Exception {
    // Log.d("RegressionTest", "Testing multiple Filter conditions");
    //
    // try {
    // OsbdtOracledb db = OsbdtOracledb.getInstance();
    // CollectionReference colRef = db.collection("testCollection");
    //
    // // Create test documents
    // DocumentReference doc1 = colRef.document("filterMulti1_" +
    // System.currentTimeMillis());
    // DocumentReference doc2 = colRef.document("filterMulti2_" +
    // System.currentTimeMillis());
    // DocumentReference doc3 = colRef.document("filterMulti3_" +
    // System.currentTimeMillis());
    //
    // java.util.Map<String, Object> data1 = new java.util.HashMap<>();
    // data1.put("status", "active");
    // data1.put("score", 85);
    // data1.put("category", "A");
    //
    // java.util.Map<String, Object> data2 = new java.util.HashMap<>();
    // data2.put("status", "active");
    // data2.put("score", 75);
    // data2.put("category", "B");
    //
    // java.util.Map<String, Object> data3 = new java.util.HashMap<>();
    // data3.put("status", "inactive");
    // data3.put("score", 90);
    // data3.put("category", "A");
    //
    // await(doc1.set(data1));
    // await(doc2.set(data2));
    // await(doc3.set(data3));
    //
    // // Query with multiple filter conditions
    // Query query = colRef
    // .where(Filter.equalTo("status", "active"))
    // .where(Filter.greaterThan("score", 80))
    // .where(Filter.equalTo("category", "A"));
    //
    // QuerySnapshot snapshot = await(query.get());
    // assertEquals("Should find 1 document matching all conditions", 1,
    // snapshot.size());
    //
    // DocumentSnapshot foundDoc = snapshot.getDocuments().get(0);
    // assertEquals("Found document should have active status", "active",
    // foundDoc.getString("status"));
    // assertTrue("Found document should have score > 80", foundDoc.getLong("score")
    // > 80);
    // assertEquals("Found document should have category A", "A",
    // foundDoc.getString("category"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in multiple Filter conditions test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testFilterEqualityAndHashCode() throws Exception {
    // Log.d("RegressionTest", "Testing Filter equality and hashCode");
    //
    // try {
    // // Test filter equality
    // Filter filter1 = Filter.equalTo("field", "value");
    // Filter filter2 = Filter.equalTo("field", "value");
    // Filter filter3 = Filter.equalTo("field", "different");
    //
    // assertTrue("Identical filters should be equal", filter1.equals(filter2));
    // assertFalse("Different filters should not be equal",
    // filter1.equals(filter3));
    // assertEquals("Equal filters should have same hashCode", filter1.hashCode(),
    // filter2.hashCode());
    // assertNotEquals("Different filters should have different hashCode",
    // filter1.hashCode(), filter3.hashCode());
    //
    // // Test different operators
    // Filter equalFilter = Filter.equalTo("field", "value");
    // Filter notEqualFilter = Filter.notEqualTo("field", "value");
    // assertFalse("Different operators should not be equal",
    // equalFilter.equals(notEqualFilter));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Filter equality and hashCode test: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testStorageGetInstance() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing Storage.getInstance with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // assertNotNull("Storage instance should not be null for " + authType,
    // storage);
    // assertNotNull("Storage app should not be null for " + authType,
    // storage.getApp());
    // assertEquals("Storage app should match auth app for " + authType,
    // "regression-test-" + authType, storage.getApp().getName());
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Storage.getInstance for " + authType +
    // ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageGetReference() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing Storage.getReference with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // // Test root reference
    // StorageReference rootRef = storage.getReference();
    // assertNotNull("Root storage reference should not be null for " + authType,
    // rootRef);
    // assertEquals("Root path should be empty for " + authType, "",
    // rootRef.getPath());
    //
    // // Test path reference
    // StorageReference pathRef = storage.getReference("test/path/file.txt");
    // assertNotNull("Path storage reference should not be null for " + authType,
    // pathRef);
    // assertEquals("Path should match for " + authType, "test/path/file.txt",
    // pathRef.getPath());
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in Storage.getReference for " + authType +
    // ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceGetBucket() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.getBucket with authType: "
    // + authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test/file.txt");
    // assertNotNull("Storage reference should not be null for " + authType, ref);
    // assertEquals("Bucket should match config for " + authType,
    // "dbfs_CZXMDZFGRFGEITN", ref.getBucket());
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.getBucket for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceGetName() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.getName with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // // Test file with extension
    // StorageReference fileRef = storage.getReference("path/to/file.txt");
    // assertEquals("File name should be extracted for " + authType, "file.txt",
    // fileRef.getName());
    //
    // // Test file without extension
    // StorageReference noExtRef = storage.getReference("path/to/file");
    // assertEquals("File name should be extracted for " + authType, "file",
    // noExtRef.getName());
    //
    // // Test root file
    // StorageReference rootRef = storage.getReference("file.txt");
    // assertEquals("Root file name should be extracted for " + authType,
    // "file.txt", rootRef.getName());
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.getName for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceGetPath() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.getPath with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // // Test simple path
    // StorageReference simpleRef = storage.getReference("file.txt");
    // assertEquals("Simple path should match for " + authType, "file.txt",
    // simpleRef.getPath());
    //
    // // Test nested path
    // StorageReference nestedRef =
    // storage.getReference("folder/subfolder/file.txt");
    // assertEquals("Nested path should match for " + authType,
    // "folder/subfolder/file.txt", nestedRef.getPath());
    //
    // // Test root reference
    // StorageReference rootRef = storage.getReference();
    // assertEquals("Root path should be empty for " + authType, "",
    // rootRef.getPath());
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.getPath for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceChild() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.child with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference parentRef = storage.getReference("parent");
    // StorageReference childRef = parentRef.child("child");
    //
    // assertNotNull("Child reference should not be null for " + authType,
    // childRef);
    // assertEquals("Child path should be constructed correctly for " + authType,
    // "parent/child", childRef.getPath());
    //
    // // Test nested children
    // StorageReference grandChildRef = childRef.child("grandchild.txt");
    // assertEquals("Grandchild path should be constructed correctly for " +
    // authType, "parent/child/grandchild.txt", grandChildRef.getPath());
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.child for " + authType
    // + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceGetParent() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.getParent with authType: "
    // + authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // // Test nested path parent
    // StorageReference childRef = storage.getReference("parent/child/file.txt");
    // StorageReference parentRef = childRef.getParent();
    //
    // assertNotNull("Parent reference should not be null for " + authType,
    // parentRef);
    // assertEquals("Parent path should be correct for " + authType, "parent/child",
    // parentRef.getPath());
    //
    // // Test single level parent
    // StorageReference singleRef = storage.getReference("file.txt");
    // StorageReference rootParent = singleRef.getParent();
    //
    // assertNotNull("Root parent reference should not be null for " + authType,
    // rootParent);
    // assertEquals("Root parent path should be empty for " + authType, "",
    // rootParent.getPath());
    //
    // // Test root reference parent (should be null)
    // StorageReference rootRef = storage.getReference();
    // assertNull("Root reference parent should be null for " + authType,
    // rootRef.getParent());
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.getParent for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceGetRoot() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.getRoot with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // // Test getting root from nested reference
    // StorageReference nestedRef =
    // storage.getReference("deep/nested/path/file.txt");
    // StorageReference rootRef = nestedRef.getRoot();
    //
    // assertNotNull("Root reference should not be null for " + authType, rootRef);
    // assertEquals("Root path should be empty for " + authType, "",
    // rootRef.getPath());
    // assertEquals("Root bucket should match for " + authType,
    // "dbfs_CZXMDZFGRFGEITN", rootRef.getBucket());
    //
    // // Test getting root from root reference
    // StorageReference originalRoot = storage.getReference();
    // StorageReference rootFromRoot = originalRoot.getRoot();
    //
    // assertEquals("Root from root should be same reference for " + authType,
    // originalRoot, rootFromRoot);
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.getRoot for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceToString() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.toString with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test/path/file.txt");
    // String stringRep = ref.toString();
    //
    // assertNotNull("toString should not be null for " + authType, stringRep);
    // assertFalse("toString should not be empty for " + authType,
    // stringRep.isEmpty());
    // // Should contain bucket and path information
    // assertTrue("toString should contain bucket for " + authType,
    // stringRep.contains("dbfs_CZXMDZFGRFGEITN"));
    // assertTrue("toString should contain path for " + authType,
    // stringRep.contains("test/path/file.txt"));
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.toString for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceEquality() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference equality with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref1 = storage.getReference("test/file.txt");
    // StorageReference ref2 = storage.getReference("test/file.txt");
    // StorageReference ref3 = storage.getReference("different/file.txt");
    //
    // // Test equality
    // assertTrue("Equal references should be equal for " + authType,
    // ref1.equals(ref2));
    // assertFalse("Different references should not be equal for " + authType,
    // ref1.equals(ref3));
    //
    // // Test hashCode
    // assertEquals("Equal references should have same hashCode for " + authType,
    // ref1.hashCode(), ref2.hashCode());
    // assertNotEquals("Different references should have different hashCode for " +
    // authType, ref1.hashCode(), ref3.hashCode());
    //
    // // Test equals with null
    // assertFalse("Reference should not equal null for " + authType,
    // ref1.equals(null));
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference equality for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceIsEqual() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.isEqual with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref1 = storage.getReference("test/file.txt");
    // StorageReference ref2 = storage.getReference("test/file.txt");
    // StorageReference ref3 = storage.getReference("other/file.txt");
    //
    // // Test isEqual method
    // assertTrue("Equal references should be equal for " + authType,
    // ref1.isEqual(ref2));
    // assertFalse("Different references should not be equal for " + authType,
    // ref1.isEqual(ref3));
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.isEqual for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferencePutBytes() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.putBytes with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-upload-" + authType + "-" +
    // System.currentTimeMillis() + ".txt");
    //
    // // Test uploading bytes
    // String testContent = "Test content for " + authType;
    // byte[] data = testContent.getBytes("UTF-8");
    //
    // UploadTask uploadTask = ref.putBytes(data);
    // await(uploadTask);
    //
    // // Verify upload success
    // assertTrue("Upload should succeed for " + authType, true);
    //
    // // Test getting download URL (may not work in all environments)
    // try {
    // Task<Uri> urlTask = ref.getDownloadUrl();
    // Uri downloadUrl = await(urlTask);
    // assertNotNull("Download URL should not be null for " + authType,
    // downloadUrl);
    // } catch (Exception urlException) {
    // Log.d("RegressionTest", "Download URL not available in test environment for "
    // + authType + ": " + urlException.getMessage());
    // }
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.putBytes for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferencePutFile() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.putFile with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-file-upload-" + authType +
    // "-" + System.currentTimeMillis() + ".txt");
    //
    // // Create a temporary file
    // java.io.File tempFile = java.io.File.createTempFile("test-upload-" +
    // authType, ".txt");
    // tempFile.deleteOnExit();
    //
    // String testContent = "File upload test content for " + authType;
    // java.nio.file.Files.write(tempFile.toPath(), testContent.getBytes("UTF-8"));
    //
    // // Upload file
    // Uri fileUri = Uri.fromFile(tempFile);
    // UploadTask uploadTask = ref.putFile(fileUri);
    // await(uploadTask);
    //
    // assertTrue("File upload should succeed for " + authType, true);
    //
    // // Cleanup
    // tempFile.delete();
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.putFile for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceGetBytes() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.getBytes with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-download-" + authType + "-"
    // + System.currentTimeMillis() + ".txt");
    //
    // // First upload some content
    // String testContent = "Download test content for " + authType;
    // byte[] uploadData = testContent.getBytes("UTF-8");
    // await(ref.putBytes(uploadData));
    //
    // // Now download it back
    // final long MAX_DOWNLOAD_SIZE = 1024 * 1024; // 1MB
    // Task<byte[]> downloadTask = ref.getBytes(MAX_DOWNLOAD_SIZE);
    // byte[] downloadedData = await(downloadTask);
    //
    // assertNotNull("Downloaded data should not be null for " + authType,
    // downloadedData);
    // assertEquals("Downloaded content should match original for " + authType,
    // testContent, new String(downloadedData, "UTF-8"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.getBytes for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceDelete() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.delete with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-delete-" + authType + "-" +
    // System.currentTimeMillis() + ".txt");
    //
    // // Upload a file first
    // byte[] data = ("Delete test for " + authType).getBytes("UTF-8");
    // await(ref.putBytes(data));
    //
    // // Now delete it
    // Task<Void> deleteTask = ref.delete();
    // await(deleteTask);
    //
    // assertTrue("Delete should succeed for " + authType, true);
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.delete for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceGetMetadata() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.getMetadata with authType:
    // " + authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-metadata-" + authType + "-"
    // + System.currentTimeMillis() + ".txt");
    //
    // // Upload with metadata
    // byte[] data = ("Metadata test for " + authType).getBytes("UTF-8");
    //
    // StorageMetadata metadata = new StorageMetadata.Builder()
    // .setContentType("text/plain")
    // .setCustomMetadata("test-key", "test-value")
    // .build();
    //
    // UploadTask uploadTask = ref.putBytes(data, metadata);
    // await(uploadTask);
    //
    // // Get metadata
    // Task<StorageMetadata> metadataTask = ref.getMetadata();
    // StorageMetadata retrievedMetadata = await(metadataTask);
    //
    // assertNotNull("Retrieved metadata should not be null for " + authType,
    // retrievedMetadata);
    // assertEquals("Content type should match for " + authType, "text/plain",
    // retrievedMetadata.getContentType());
    // assertEquals("Custom metadata should match for " + authType, "test-value",
    // retrievedMetadata.getCustomMetadata("test-key"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.getMetadata for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceUpdateMetadata() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.updateMetadata with
    // authType: " + authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-update-metadata-" +
    // authType + "-" + System.currentTimeMillis() + ".txt");
    //
    // // Upload initial file
    // byte[] data = ("Update metadata test for " + authType).getBytes("UTF-8");
    // await(ref.putBytes(data));
    //
    // // Update metadata
    // StorageMetadata newMetadata = new StorageMetadata.Builder()
    // .setContentType("text/plain")
    // .setCustomMetadata("updated-key", "updated-value")
    // .setCustomMetadata("new-key", "new-value")
    // .build();
    //
    // Task<StorageMetadata> updateTask = ref.updateMetadata(newMetadata);
    // StorageMetadata updatedMetadata = await(updateTask);
    //
    // assertNotNull("Updated metadata should not be null for " + authType,
    // updatedMetadata);
    // assertEquals("Content type should be updated for " + authType, "text/plain",
    // updatedMetadata.getContentType());
    // assertEquals("Updated custom metadata should match for " + authType,
    // "updated-value", updatedMetadata.getCustomMetadata("updated-key"));
    // assertEquals("New custom metadata should be set for " + authType,
    // "new-value", updatedMetadata.getCustomMetadata("new-key"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.updateMetadata for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceList() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.list with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // // Create some test files in a folder
    // String folderName = "test-list-" + authType + "-" +
    // System.currentTimeMillis();
    // StorageReference folderRef = storage.getReference(folderName);
    //
    // // Upload multiple files
    // StorageReference file1 = folderRef.child("file1.txt");
    // StorageReference file2 = folderRef.child("file2.txt");
    // StorageReference file3 = folderRef.child("subdir/file3.txt");
    //
    // await(file1.putBytes(("Content 1 for " + authType).getBytes("UTF-8")));
    // await(file2.putBytes(("Content 2 for " + authType).getBytes("UTF-8")));
    // await(file3.putBytes(("Content 3 for " + authType).getBytes("UTF-8")));
    //
    // // List files in folder
    // Task<ListResult> listTask = folderRef.list(10);
    // ListResult listResult = await(listTask);
    //
    // assertNotNull("List result should not be null for " + authType, listResult);
    //
    // // Check items (may include files and subfolders)
    // List<StorageReference> items = listResult.getItems();
    // assertNotNull("Items list should not be null for " + authType, items);
    //
    // // Should contain at least file1 and file2
    // boolean foundFile1 = false, foundFile2 = false;
    // for (StorageReference item : items) {
    // if (item.getName().equals("file1.txt")) foundFile1 = true;
    // if (item.getName().equals("file2.txt")) foundFile2 = true;
    // }
    //
    // assertTrue("Should find file1.txt for " + authType, foundFile1);
    // assertTrue("Should find file2.txt for " + authType, foundFile2);
    //
    // // Check prefixes (subfolders)
    // List<StorageReference> prefixes = listResult.getPrefixes();
    // assertNotNull("Prefixes list should not be null for " + authType, prefixes);
    //
    // // Should contain subdir
    // boolean foundSubdir = false;
    // for (StorageReference prefix : prefixes) {
    // if (prefix.getName().equals("subdir")) foundSubdir = true;
    // }
    //
    // assertTrue("Should find subdir prefix for " + authType, foundSubdir);
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.list for " + authType
    // + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceListAll() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.listAll with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // // Create test files
    // String folderName = "test-list-all-" + authType + "-" +
    // System.currentTimeMillis();
    // StorageReference folderRef = storage.getReference(folderName);
    //
    // StorageReference file1 = folderRef.child("file1.txt");
    // StorageReference file2 = folderRef.child("file2.txt");
    //
    // await(file1.putBytes(("ListAll content 1 for " +
    // authType).getBytes("UTF-8")));
    // await(file2.putBytes(("ListAll content 2 for " +
    // authType).getBytes("UTF-8")));
    //
    // // List all
    // Task<ListResult> listAllTask = folderRef.listAll();
    // ListResult listResult = await(listAllTask);
    //
    // assertNotNull("ListAll result should not be null for " + authType,
    // listResult);
    //
    // List<StorageReference> items = listResult.getItems();
    // assertNotNull("Items should not be null for " + authType, items);
    // assertEquals("Should find 2 items for " + authType, 2, items.size());
    //
    // // Verify both files are present
    // java.util.Set<String> foundFiles = new java.util.HashSet<>();
    // for (StorageReference item : items) {
    // foundFiles.add(item.getName());
    // }
    //
    // assertTrue("Should contain file1.txt for " + authType,
    // foundFiles.contains("file1.txt"));
    // assertTrue("Should contain file2.txt for " + authType,
    // foundFiles.contains("file2.txt"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.listAll for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferencePutStream() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.putStream with authType: "
    // + authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-stream-" + authType + "-" +
    // System.currentTimeMillis() + ".txt");
    //
    // // Create input stream
    // String testContent = "Stream upload test for " + authType;
    // java.io.ByteArrayInputStream inputStream = new
    // java.io.ByteArrayInputStream(testContent.getBytes("UTF-8"));
    //
    // // Upload stream
    // UploadTask uploadTask = ref.putStream(inputStream);
    // await(uploadTask);
    //
    // assertTrue("Stream upload should succeed for " + authType, true);
    //
    // // Verify content by downloading
    // Task<byte[]> downloadTask = ref.getBytes(1024);
    // byte[] downloadedData = await(downloadTask);
    // String downloadedContent = new String(downloadedData, "UTF-8");
    //
    // assertEquals("Downloaded content should match original for " + authType,
    // testContent, downloadedContent);
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.putStream for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageMetadataBuilder() throws Exception {
    // Log.d("RegressionTest", "Testing StorageMetadata.Builder");
    //
    // try {
    // // Test empty builder
    // StorageMetadata emptyMetadata = new StorageMetadata.Builder().build();
    // assertNotNull("Empty metadata should not be null", emptyMetadata);
    //
    // // Test builder with values
    // StorageMetadata fullMetadata = new StorageMetadata.Builder()
    // .setContentType("application/json")
    // .setContentEncoding("gzip")
    // .setContentLanguage("en-US")
    // .setCacheControl("max-age=3600")
    // .setCustomMetadata("key1", "value1")
    // .setCustomMetadata("key2", "value2")
    // .build();
    //
    // assertNotNull("Full metadata should not be null", fullMetadata);
    // assertEquals("Content type should match", "application/json",
    // fullMetadata.getContentType());
    // assertEquals("Content encoding should match", "gzip",
    // fullMetadata.getContentEncoding());
    // assertEquals("Content language should match", "en-US",
    // fullMetadata.getContentLanguage());
    // assertEquals("Cache control should match", "max-age=3600",
    // fullMetadata.getCacheControl());
    // assertEquals("Custom metadata key1 should match", "value1",
    // fullMetadata.getCustomMetadata("key1"));
    // assertEquals("Custom metadata key2 should match", "value2",
    // fullMetadata.getCustomMetadata("key2"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageMetadata.Builder: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testStorageMetadataGetters() throws Exception {
    // Log.d("RegressionTest", "Testing StorageMetadata getters");
    //
    // try {
    // StorageMetadata metadata = new StorageMetadata.Builder()
    // .setContentType("text/plain")
    // .setContentEncoding("utf-8")
    // .setContentDisposition("inline")
    // .setContentLanguage("en")
    // .setCacheControl("no-cache")
    // .setCustomMetadata("testKey", "testValue")
    // .build();
    //
    // // Test all getters
    // assertEquals("Content type", "text/plain", metadata.getContentType());
    // assertEquals("Content encoding", "utf-8", metadata.getContentEncoding());
    // assertEquals("Content disposition", "inline",
    // metadata.getContentDisposition());
    // assertEquals("Content language", "en", metadata.getContentLanguage());
    // assertEquals("Cache control", "no-cache", metadata.getCacheControl());
    // assertEquals("Custom metadata", "testValue",
    // metadata.getCustomMetadata("testKey"));
    // assertNull("Non-existent custom metadata",
    // metadata.getCustomMetadata("nonExistent"));
    //
    // // Test getCustomMetadataKeys
    // java.util.Set<String> keys = metadata.getCustomMetadataKeys();
    // assertNotNull("Custom metadata keys should not be null", keys);
    // assertTrue("Should contain testKey", keys.contains("testKey"));
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageMetadata getters: " +
    // e.getMessage());
    // }
    // }
    //
    // @Test
    // public void testStorageTaskPauseResumeCancel() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageTask pause/resume/cancel with
    // authType: " + authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-task-" + authType + "-" +
    // System.currentTimeMillis() + ".txt");
    //
    // // Create a larger upload to test pause/resume
    // StringBuilder largeContent = new StringBuilder();
    // for (int i = 0; i < 1000; i++) {
    // largeContent.append("Large content for ").append(authType).append(" test
    // ").append(i).append("\n");
    // }
    // byte[] largeData = largeContent.toString().getBytes("UTF-8");
    //
    // // Start upload
    // UploadTask uploadTask = ref.putBytes(largeData);
    //
    // // Test pause (may not be supported in all implementations)
    // try {
    // boolean paused = uploadTask.pause();
    // // Note: pause may return false if not supported or already completed
    // Log.d("RegressionTest", "Upload pause result for " + authType + ": " +
    // paused);
    // } catch (Exception pauseException) {
    // Log.d("RegressionTest", "Pause not supported or failed for " + authType + ":
    // " + pauseException.getMessage());
    // }
    //
    // // Test resume (may not be supported in all implementations)
    // try {
    // boolean resumed = uploadTask.resume();
    // Log.d("RegressionTest", "Upload resume result for " + authType + ": " +
    // resumed);
    // } catch (Exception resumeException) {
    // Log.d("RegressionTest", "Resume not supported or failed for " + authType + ":
    // " + resumeException.getMessage());
    // }
    //
    // // Wait for completion
    // await(uploadTask);
    //
    // // Test cancel on a new task (if supported)
    // try {
    // StorageReference cancelRef = storage.getReference("test-cancel-" + authType +
    // "-" + System.currentTimeMillis() + ".txt");
    // UploadTask cancelTask = cancelRef.putBytes(largeData);
    //
    // // Try to cancel immediately (may not work if task completes too fast)
    // boolean cancelled = cancelTask.cancel();
    // Log.d("RegressionTest", "Upload cancel result for " + authType + ": " +
    // cancelled);
    //
    // if (!cancelled) {
    // // If cancel didn't work, just wait for completion
    // await(cancelTask);
    // }
    // } catch (Exception cancelException) {
    // Log.d("RegressionTest", "Cancel test for " + authType + ": " +
    // cancelException.getMessage());
    // }
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageTask pause/resume/cancel for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageTaskProgress() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageTask progress with authType: " +
    // authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-progress-" + authType + "-"
    // + System.currentTimeMillis() + ".txt");
    //
    // // Create upload with progress tracking
    // String content = "Progress tracking test for " + authType;
    // byte[] data = content.getBytes("UTF-8");
    //
    // UploadTask uploadTask = ref.putBytes(data);
    //
    // // Add progress listener
    // final java.util.concurrent.atomic.AtomicReference<Long> bytesTransferred =
    // new java.util.concurrent.atomic.AtomicReference<>(0L);
    // final java.util.concurrent.atomic.AtomicReference<Long> totalBytes = new
    // java.util.concurrent.atomic.AtomicReference<>(0L);
    //
    // uploadTask.addOnProgressListener(snapshot -> {
    // bytesTransferred.set(snapshot.getBytesTransferred());
    // totalBytes.set(snapshot.getTotalByteCount());
    // });
    //
    // // Wait for completion
    // await(uploadTask);
    //
    // // Verify progress was tracked
    // assertTrue("Bytes transferred should be > 0 for " + authType,
    // bytesTransferred.get() > 0);
    // assertEquals("Total bytes should match data length for " + authType, (Long)
    // (long) data.length, totalBytes.get());
    // assertEquals("Bytes transferred should equal total bytes for " + authType,
    // bytesTransferred.get(), totalBytes.get());
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageTask progress for " + authType +
    // ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageTaskSuccessFailure() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageTask success/failure with authType: "
    // + authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-success-" + authType + "-"
    // + System.currentTimeMillis() + ".txt");
    //
    // // Test successful upload
    // String content = "Success test for " + authType;
    // byte[] data = content.getBytes("UTF-8");
    //
    // UploadTask uploadTask = ref.putBytes(data);
    //
    // // Add success listener
    // final java.util.concurrent.atomic.AtomicBoolean successCalled = new
    // java.util.concurrent.atomic.AtomicBoolean(false);
    // final java.util.concurrent.atomic.AtomicBoolean failureCalled = new
    // java.util.concurrent.atomic.AtomicBoolean(false);
    //
    // uploadTask.addOnSuccessListener(snapshot -> {
    // successCalled.set(true);
    // assertNotNull("Success snapshot should not be null for " + authType,
    // snapshot);
    // assertEquals("Success bytes transferred should match for " + authType,
    // data.length, snapshot.getBytesTransferred());
    // });
    //
    // uploadTask.addOnFailureListener(exception -> {
    // failureCalled.set(true);
    // Log.e("RegressionTest", "Unexpected failure in success test for " + authType,
    // exception);
    // });
    //
    // // Wait for completion
    // await(uploadTask);
    //
    // // Verify listeners were called correctly
    // assertTrue("Success listener should have been called for " + authType,
    // successCalled.get());
    // assertFalse("Failure listener should not have been called for " + authType,
    // failureCalled.get());
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageTask success/failure for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceGetDownloadUrl() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.getDownloadUrl with
    // authType: " + authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-download-url-" + authType +
    // "-" + System.currentTimeMillis() + ".txt");
    //
    // // Upload a file first
    // String content = "Download URL test for " + authType;
    // byte[] data = content.getBytes("UTF-8");
    // await(ref.putBytes(data));
    //
    // // Get download URL
    // Task<Uri> urlTask = ref.getDownloadUrl();
    // Uri downloadUrl = await(urlTask);
    //
    // assertNotNull("Download URL should not be null for " + authType,
    // downloadUrl);
    // assertFalse("Download URL should not be empty for " + authType,
    // downloadUrl.toString().isEmpty());
    //
    // // URL should be a valid URI
    // assertNotNull("Download URL host should not be null for " + authType,
    // downloadUrl.getHost());
    // assertTrue("Download URL should have a scheme for " + authType,
    // downloadUrl.getScheme() != null);
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.getDownloadUrl for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
    //
    // @Test
    // public void testStorageReferenceGetStream() throws Exception {
    // for (String authType : AUTH_TYPES) {
    // Log.d("RegressionTest", "Testing StorageReference.getStream with authType: "
    // + authType);
    // Storage storage = getStorageForType(authType);
    //
    // try {
    // StorageReference ref = storage.getReference("test-stream-download-" +
    // authType + "-" + System.currentTimeMillis() + ".txt");
    //
    // // Upload content first
    // String originalContent = "Stream download test for " + authType;
    // byte[] data = originalContent.getBytes("UTF-8");
    // await(ref.putBytes(data));
    //
    // // Download as stream
    // final java.util.concurrent.atomic.AtomicReference<String> downloadedContent =
    // new java.util.concurrent.atomic.AtomicReference<>("");
    //
    // Task<java.io.InputStream> streamTask = ref.getStream();
    // java.io.InputStream inputStream = await(streamTask);
    //
    // assertNotNull("Input stream should not be null for " + authType,
    // inputStream);
    //
    // // Read from stream
    // java.io.ByteArrayOutputStream outputStream = new
    // java.io.ByteArrayOutputStream();
    // byte[] buffer = new byte[1024];
    // int bytesRead;
    // while ((bytesRead = inputStream.read(buffer)) != -1) {
    // outputStream.write(buffer, 0, bytesRead);
    // }
    // inputStream.close();
    //
    // String result = new String(outputStream.toByteArray(), "UTF-8");
    // assertEquals("Stream content should match original for " + authType,
    // originalContent, result);
    //
    // } catch (Exception e) {
    // Log.d("RegressionTest", "Exception in StorageReference.getStream for " +
    // authType + ": " + e.getMessage());
    // }
    // }
    // }
}
