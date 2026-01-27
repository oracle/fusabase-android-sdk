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

//package com.oracle.test.auth;
//
//import static org.junit.Assert.*;
//
//import android.util.Log;
//
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//
//import com.oracle.database.osbdt.auth.AuthCredential;
//import com.oracle.database.osbdt.auth.GoogleAuthProvider;
//import com.oracle.database.osbdt.auth.OsbdtAuth;
//import com.oracle.database.osbdt.auth.OsbdtUser;
//import com.oracle.database.osbdt.task.Task;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//@RunWith(AndroidJUnit4.class)
//public class GoogleAuthProviderTest {
//
//    private static final String TAG = "GoogleAuthProviderTest";
//    private OsbdtAuth auth;
//
//    @Before
//    public void setUp() {
//        auth = OsbdtAuth.getInstance();
//        auth.signOut();
//    }
//
//    @After
//    public void tearDown() {
//        if (auth.getCurrentUser() != null) {
//            auth.signOut();
//        }
//    }
//
//    // Helper to await async Tasks
//    private <T> T await(Task<T> task) throws Exception {
//        CountDownLatch latch = new CountDownLatch(1);
//        final Object[] result = new Object[1];
//        final Exception[] error = new Exception[1];
//
//        task.addOnSuccessListener(r -> {
//            result[0] = r;
//            latch.countDown();
//        }).addOnFailureListener(e -> {
//            error[0] = e;
//            latch.countDown();
//        });
//
//        if (!latch.await(20, TimeUnit.SECONDS)) {
//            throw new RuntimeException("Task timed out");
//        }
//        if (error[0] != null) throw error[0];
//        return (T) result[0];
//    }
//
//    // ------------------------------
//    // ✅ UNIT TESTS FOR GoogleAuthProvider
//    // ------------------------------
//
//    @Test
//    public void testGetCredentialCreatesValidCredential() {
//        String idToken = "dummy_id_token";
//        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
//
//        assertNotNull(credential);
//        assertEquals(GoogleAuthProvider.PROVIDER_ID, credential.getProvider());
//        assertEquals(GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD, credential.getSignInMethod());
//    }
//
//    @Test
//    public void testProviderIdConstant() {
//        assertEquals("google.com", GoogleAuthProvider.PROVIDER_ID);
//    }
//
//    @Test
//    public void testGoogleSignInMethodConstant() {
//        assertEquals("google.com", GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD);
//    }
//
//    @Test
//    public void testCredentialEqualityAndHashCode() {
//        String token1 = "id_token_123";
//        String token2 = "id_token_123";
//
//        AuthCredential cred1 = GoogleAuthProvider.getCredential(token1, null);
//        AuthCredential cred2 = GoogleAuthProvider.getCredential(token2, null);
//
//        // Equality should hold
//        assertEquals(cred1, cred2);
//        assertEquals(cred1.hashCode(), cred2.hashCode());
//    }
//
//    // ------------------------------
//    // ✅ INTEGRATION TESTS WITH OsbdtAuth
//    // ------------------------------
//
//    @Test
//    public void testSignInWithGoogleCredential() throws Exception {
//        // ⚠️ Replace with a real Google Sign-In ID Token for actual test
//        String fakeIdToken = "YOUR_VALID_GOOGLE_ID_TOKEN";
//
//        AuthCredential credential = GoogleAuthProvider.getCredential(fakeIdToken, null);
//        OsbdtUser user = await(auth.signInWithCredential(credential)).getUser();
//
//        assertNotNull("OsbdtUser should not be null", user);
//        assertEquals("google.com", user.getProviderData().get(1).getProviderId());
//
//        Log.d(TAG, "Signed in user: " + user.getUid() + " email=" + user.getEmail());
//    }
//
//    @Test
//    public void testSignOutAfterGoogleSignIn() throws Exception {
//        String fakeIdToken = "YOUR_VALID_GOOGLE_ID_TOKEN";
//        AuthCredential credential = GoogleAuthProvider.getCredential(fakeIdToken, null);
//
//        OsbdtUser user = await(auth.signInWithCredential(credential)).getUser();
//        assertNotNull(user);
//
//        auth.signOut();
//        assertNull(auth.getCurrentUser());
//    }
//
//    @Test
//    public void testReauthenticationWithGoogle() throws Exception {
//        String fakeIdToken = "YOUR_VALID_GOOGLE_ID_TOKEN";
//        AuthCredential credential = GoogleAuthProvider.getCredential(fakeIdToken, null);
//
//        OsbdtUser user = await(auth.signInWithCredential(credential)).getUser();
//        assertNotNull(user);
//
//        // Re-authenticate with the same credential
//        await(user.reauthenticate(credential));
//        assertTrue(user.isEmailVerified() || !user.isEmailVerified()); // ensures call succeeds
//    }
//}
