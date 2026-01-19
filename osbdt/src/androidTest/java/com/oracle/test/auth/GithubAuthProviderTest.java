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
//import com.oracle.database.osbdt.auth.GithubAuthProvider;
//import com.oracle.database.osbdt.auth.OsbdtAuth;
//import com.oracle.database.osbdt.auth.OsbdtUser;
//
//import org.junit.*;
//import org.junit.runner.RunWith;
//
//@RunWith(AndroidJUnit4.class)
//public class GithubAuthProviderTest {
//
//    private OsbdtAuth auth;
//
//    // ⚠️ Replace with a valid GitHub OAuth access token for real integration tests
//    private static final String VALID_ACCESS_TOKEN = "gho_valid_test_token";
//    private static final String INVALID_ACCESS_TOKEN = "invalid_token";
//
//    @Before
//    public void setUp() {
//        auth = OsbdtAuth.getInstance();
//        auth.signOut();
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
//        AuthCredential credential = GithubAuthProvider.getCredential(VALID_ACCESS_TOKEN);
//        assertNotNull(credential);
//        assertEquals(GithubAuthProvider.PROVIDER_ID, credential.getProvider());
//        assertEquals(GithubAuthProvider.GITHUB_SIGN_IN_METHOD, credential.getSignInMethod());
//    }
//
//    @Test
//    public void testConstants() {
//        assertEquals("github.com", GithubAuthProvider.PROVIDER_ID);
//        assertEquals("github.com", GithubAuthProvider.GITHUB_SIGN_IN_METHOD);
//    }
//
//    @Test
//    public void testEqualityAndHashCode() {
//        AuthCredential c1 = GithubAuthProvider.getCredential(VALID_ACCESS_TOKEN);
//        AuthCredential c2 = GithubAuthProvider.getCredential(VALID_ACCESS_TOKEN);
//        assertEquals(c1, c2);
//        assertEquals(c1.hashCode(), c2.hashCode());
//    }
//
//    // ---------- Firebase Auth Integration ----------
//
//    @Test
//    public void testSignInWithGithubCredential() throws Exception {
//        // Works only with a real valid GitHub OAuth token
//        try {
//            OsbdtUser user = auth.signInWithCredential(GithubAuthProvider.getCredential(VALID_ACCESS_TOKEN))
//                    .getResult().getUser();
//
//            assertNotNull(user);
//            assertNotNull(user.getUid());
//            assertTrue(user.getProviderData().stream()
//                    .anyMatch(info -> GithubAuthProvider.PROVIDER_ID.equals(info.getProviderId())));
//        } catch (Exception e) {
//            // In CI or without a valid token, we expect failure
//            assertTrue(e.getMessage().contains("INVALID_CREDENTIAL") ||
//                    e.getMessage().contains("auth/invalid-credential"));
//        }
//    }
//
//    @Test
//    public void testReauthenticateWithGithubCredential() throws Exception {
//        try {
//            OsbdtUser user = auth.signInWithCredential(GithubAuthProvider.getCredential(VALID_ACCESS_TOKEN))
//                    .getResult().getUser();
//            assertNotNull(user);
//
//            user.reauthenticate(GithubAuthProvider.getCredential(VALID_ACCESS_TOKEN)).getResult();
//            assertNotNull(user.getUid());
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("INVALID_CREDENTIAL") ||
//                    e.getMessage().contains("auth/invalid-credential"));
//        }
//    }
//
//    @Test
//    public void testLinkAndUnlinkGithubAccount() throws Exception {
//        try {
//            OsbdtUser user = auth.signInWithCredential(GithubAuthProvider.getCredential(VALID_ACCESS_TOKEN))
//                    .getResult().getUser();
//            assertNotNull(user);
//
//            // Unlink
//            user.unlink(GithubAuthProvider.PROVIDER_ID).getResult();
//            assertTrue(user.getProviderData().stream()
//                    .noneMatch(info -> GithubAuthProvider.PROVIDER_ID.equals(info.getProviderId())));
//
//            // Link again
//            user.linkWithCredential(GithubAuthProvider.getCredential(VALID_ACCESS_TOKEN)).getResult();
//            assertTrue(user.getProviderData().stream()
//                    .anyMatch(info -> GithubAuthProvider.PROVIDER_ID.equals(info.getProviderId())));
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("INVALID_CREDENTIAL") ||
//                    e.getMessage().contains("auth/invalid-credential"));
//        }
//    }
//
//    // ---------- Error Cases ----------
//
//    @Test
//    public void testInvalidGithubTokenFails() {
//        try {
//            auth.signInWithCredential(GithubAuthProvider.getCredential(INVALID_ACCESS_TOKEN)).getResult();
//            fail("Expected failure with invalid token");
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("INVALID_CREDENTIAL") ||
//                    e.getMessage().contains("auth/invalid-credential"));
//        }
//    }
//}
