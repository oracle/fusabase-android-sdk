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
//import com.oracle.mobile.fusabase.auth.AuthCredential;
//import com.oracle.mobile.fusabase.auth.FacebookAuthProvider;
//import com.oracle.mobile.fusabase.auth.FusabaseAuth;
//
//import org.junit.*;
//import org.junit.runner.RunWith;
//
//@RunWith(AndroidJUnit4.class)
//public class FacebookAuthProviderTest {
//
//    private FusabaseAuth auth;
//
//    private static final String VALID_ACCESS_TOKEN = "fb_valid_test_token";
//    private static final String INVALID_ACCESS_TOKEN = "invalid_token";
//
//    @Before
//    public void setUp() {
//        auth = FusabaseAuth.getInstance();
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
//        AuthCredential credential = FacebookAuthProvider.getCredential(VALID_ACCESS_TOKEN);
//        assertNotNull(credential);
//        assertEquals(FacebookAuthProvider.PROVIDER_ID, credential.getProvider());
//        assertEquals(FacebookAuthProvider.FACEBOOK_SIGN_IN_METHOD, credential.getSignInMethod());
//    }
//
//    @Test
//    public void testConstants() {
//        assertEquals("facebook.com", FacebookAuthProvider.PROVIDER_ID);
//        assertEquals("facebook.com", FacebookAuthProvider.FACEBOOK_SIGN_IN_METHOD);
//    }
//
//    @Test
//    public void testEqualityAndHashCode() {
//        AuthCredential c1 = FacebookAuthProvider.getCredential(VALID_ACCESS_TOKEN);
//        AuthCredential c2 = FacebookAuthProvider.getCredential(VALID_ACCESS_TOKEN);
//        assertEquals(c1, c2);
//        assertEquals(c1.hashCode(), c2.hashCode());
//    }
//
//
//    @Test
//    public void testSignInWithFacebookCredential() {
//        try {
//            FusabaseUser user = auth.signInWithCredential(
//                    FacebookAuthProvider.getCredential(VALID_ACCESS_TOKEN)
//            ).getResult().getUser();
//
//            assertNotNull(user);
//            assertTrue(user.getProviderData().stream()
//                    .anyMatch(info -> FacebookAuthProvider.PROVIDER_ID.equals(info.getProviderId())));
//        } catch (Exception e) {
//            // With invalid/expired token we expect failure
//            assertTrue(e.getMessage().contains("INVALID_CREDENTIAL") ||
//                    e.getMessage().contains("auth/invalid-credential"));
//        }
//    }
//
//    @Test
//    public void testReauthenticateWithFacebookCredential() {
//        try {
//            FusabaseUser user = auth.signInWithCredential(
//                    FacebookAuthProvider.getCredential(VALID_ACCESS_TOKEN)
//            ).getResult().getUser();
//            assertNotNull(user);
//
//            user.reauthenticate(FacebookAuthProvider.getCredential(VALID_ACCESS_TOKEN)).getResult();
//            assertNotNull(user.getUid());
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("INVALID_CREDENTIAL") ||
//                    e.getMessage().contains("auth/invalid-credential"));
//        }
//    }
//
//    @Test
//    public void testLinkAndUnlinkFacebookAccount() {
//        try {
//            FusabaseUser user = auth.signInWithCredential(
//                    FacebookAuthProvider.getCredential(VALID_ACCESS_TOKEN)
//            ).getResult().getUser();
//            assertNotNull(user);
//
//            // unlink
//            user.unlink(FacebookAuthProvider.PROVIDER_ID).getResult();
//            assertTrue(user.getProviderData().stream()
//                    .noneMatch(info -> FacebookAuthProvider.PROVIDER_ID.equals(info.getProviderId())));
//
//            // link again
//            user.linkWithCredential(FacebookAuthProvider.getCredential(VALID_ACCESS_TOKEN)).getResult();
//            assertTrue(user.getProviderData().stream()
//                    .anyMatch(info -> FacebookAuthProvider.PROVIDER_ID.equals(info.getProviderId())));
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("INVALID_CREDENTIAL") ||
//                    e.getMessage().contains("auth/invalid-credential"));
//        }
//    }
//
//    // ---------- Error Case ----------
//
//    @Test
//    public void testInvalidFacebookTokenFails() {
//        try {
//            auth.signInWithCredential(
//                    FacebookAuthProvider.getCredential(INVALID_ACCESS_TOKEN)
//            ).getResult();
//            fail("Expected sign-in failure with invalid token");
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("INVALID_CREDENTIAL") ||
//                    e.getMessage().contains("auth/invalid-credential"));
//        }
//    }
//}
