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
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//
//import com.oracle.database.osbdt.auth.AuthCredential;
//import com.oracle.database.osbdt.auth.EmailAuthProvider;
//import com.oracle.database.osbdt.auth.OsbdtAuth;
//import com.oracle.database.osbdt.auth.OsbdtUser;
//
//import org.junit.*;
//import org.junit.runner.RunWith;
//
//@RunWith(AndroidJUnit4.class)
//public class EmailAuthProviderTest {
//
//    private OsbdtAuth auth;
//    private static final String TEST_EMAIL = "user_email_provider@example.com";
//    private static final String TEST_PASSWORD = "password123";
//
//    @Before
//    public void setUp() throws Exception {
//        auth = OsbdtAuth.getInstance();
//        auth.signOut();
//
//        // Ensure test user exists
//        try {
//            auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).getResult();
//        } catch (Exception e) {
//            // user may already exist, ignore
//        }
//    }
//
//    @After
//    public void tearDown() {
//        auth.signOut();
//    }
//
//    // ---------- Credential & Constants ----------
//
//    @Test
//    public void testCredentialCreation() {
//        AuthCredential credential = EmailAuthProvider.getCredential(TEST_EMAIL, TEST_PASSWORD);
//        assertNotNull(credential);
//        assertEquals(EmailAuthProvider.PROVIDER_ID, credential.getProvider());
//        assertEquals(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD, credential.getSignInMethod());
//    }
//
//    @Test
//    public void testConstants() {
//        assertEquals("password", EmailAuthProvider.PROVIDER_ID);
//        assertEquals("password", EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD);
//    }
//
//    @Test
//    public void testEqualityAndHashCode() {
//        AuthCredential c1 = EmailAuthProvider.getCredential(TEST_EMAIL, TEST_PASSWORD);
//        AuthCredential c2 = EmailAuthProvider.getCredential(TEST_EMAIL, TEST_PASSWORD);
//        assertEquals(c1, c2);
//        assertEquals(c1.hashCode(), c2.hashCode());
//    }
//
//
//    @Test
//    public void testSignInWithEmailAndPassword() throws Exception {
//        OsbdtUser user = auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).getResult().getUser();
//        assertNotNull(user);
//        assertEquals(TEST_EMAIL, user.getEmail());
//    }
//
//    @Test
//    public void testReauthenticate() throws Exception {
//        OsbdtUser user = auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).getResult().getUser();
//        assertNotNull(user);
//
//        AuthCredential credential = EmailAuthProvider.getCredential(TEST_EMAIL, TEST_PASSWORD);
//        user.reauthenticate(credential).getResult();
//        assertEquals(TEST_EMAIL, user.getEmail());
//    }
//
//    @Test
//    public void testLinkAndUnlinkAccount() throws Exception {
//        OsbdtUser user = auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).getResult().getUser();
//        assertNotNull(user);
//
//        // Link with another dummy email account
//        String otherEmail = "link_test@example.com";
//        String otherPassword = "linkPass123";
//
//        try {
//            auth.createUserWithEmailAndPassword(otherEmail, otherPassword).getResult();
//        } catch (Exception ignore) {}
//
//        AuthCredential otherCred = EmailAuthProvider.getCredential(otherEmail, otherPassword);
//        user.linkWithCredential(otherCred).getResult();
//        assertTrue(user.getProviderData().size() > 1);
//
//        // Unlink
//        user.unlink(EmailAuthProvider.PROVIDER_ID).getResult();
//        assertTrue(user.getProviderData().size() >= 1);
//    }
//
//    @Test
//    public void testUpdatePassword() throws Exception {
//        OsbdtUser user = auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).getResult().getUser();
//        assertNotNull(user);
//
//        String newPassword = "newPassword456";
//        user.updatePassword(newPassword).getResult();
//
//        auth.signOut();
//        OsbdtUser reSignIn = auth.signInWithEmailAndPassword(TEST_EMAIL, newPassword).getResult().getUser();
//        assertNotNull(reSignIn);
//    }
//
//    @Test
//    public void testDeleteUser() throws Exception {
//        OsbdtUser user = auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).getResult().getUser();
//        assertNotNull(user);
//
//        user.delete().getResult();
//
//        auth.signOut();
//        try {
//            auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD).getResult();
//            fail("Expected sign-in failure after user deletion");
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("ERROR_USER_NOT_FOUND") || e.getMessage().contains("no user record"));
//        }
//    }
//
//    // ---------- Error Cases ----------
//
//    @Test
//    public void testInvalidPassword() {
//        try {
//            auth.signInWithEmailAndPassword(TEST_EMAIL, "wrongPass").getResult();
//            fail("Expected failure with wrong password");
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("ERROR_WRONG_PASSWORD"));
//        }
//    }
//
//    @Test
//    public void testInvalidEmail() {
//        try {
//            auth.signInWithEmailAndPassword("not_an_email", "pw").getResult();
//            fail("Expected failure with bad email format");
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("ERROR_INVALID_EMAIL"));
//        }
//    }
//}
//
