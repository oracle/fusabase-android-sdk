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
//import com.oracle.database.osbdt.auth.OAuthCredential;
//import com.oracle.database.osbdt.auth.OAuthProvider;
//import com.oracle.database.osbdt.auth.OsbdtAuth;
//import com.oracle.database.osbdt.auth.OsbdtUser;
//
//import org.junit.*;
//import org.junit.runner.RunWith;
//
//import java.util.*;
//
//@RunWith(AndroidJUnit4.class)
//public class OAuthProviderTest {
//
//    private OsbdtAuth auth;
//
//    // ⚠️ Replace with a real provider ID ("microsoft.com", "twitter.com", "yahoo.com", etc.)
//    private static final String PROVIDER_ID = "microsoft.com";
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
//    // ---------- Provider Builder & Constants ----------
//
//    @Test
//    public void testBuildOAuthProviderRequest() {
//        OAuthProvider.Builder builder = OAuthProvider.newBuilder(PROVIDER_ID);
//        builder.addCustomParameter("prompt", "login");
//        builder.setScopes(Arrays.asList("mail.read", "calendars.read"));
//
//        OAuthProvider provider = builder.build();
//
//        assertEquals(PROVIDER_ID, provider.getProviderId());
//        assertTrue(provider.getScopes().contains("mail.read"));
//        assertEquals("login", provider.getCustomParameters().get("prompt"));
//    }
//
//    @Test
//    public void testConstants() {
//        // Each OAuth provider has its own ID
//        assertEquals("microsoft.com", PROVIDER_ID);
//    }
//
//
//    @Test
//    public void testSignInWithOAuthProvider() {
//        try {
//            OsbdtUser user = auth
//                    .startActivityForSignInWithProvider(/* activity= */ null,
//                            OAuthProvider.newBuilder(PROVIDER_ID).build())
//                    .getResult()
//                    .getUser();
//
//            assertNotNull(user);
//            assertTrue(user.getProviderData().stream()
//                    .anyMatch(info -> PROVIDER_ID.equals(info.getProviderId())));
//        } catch (Exception e) {
//            // Expected with no real provider activity/token
//            assertTrue(e.getMessage().contains("NO_SUCH_PROVIDER") ||
//                    e.getMessage().contains("auth/operation-not-allowed") ||
//                    e.getMessage().contains("DEVELOPER_ERROR"));
//        }
//    }
//
//    @Test
//    public void testLinkOAuthProvider() {
//        try {
//            // Pretend user is signed in anonymously first
//            OsbdtUser user = auth.signInAnonymously().getResult().getUser();
//            assertNotNull(user);
//
//            user.linkWithProvider(OAuthProvider.newBuilder(PROVIDER_ID).build()).getResult();
//
//            assertTrue(user.getProviderData().stream()
//                    .anyMatch(info -> PROVIDER_ID.equals(info.getProviderId())));
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("auth/operation-not-allowed") ||
//                    e.getMessage().contains("DEVELOPER_ERROR"));
//        }
//    }
//
//    @Test
//    public void testReauthenticateWithOAuthProvider() {
//        try {
//            OsbdtUser user = auth.signInAnonymously().getResult().getUser();
//            assertNotNull(user);
//
//            user.startActivityForReauthenticateWithProvider(
//                    /* activity= */ null, OAuthProvider.newBuilder(PROVIDER_ID).build()
//            ).getResult();
//
//            assertNotNull(user.getUid());
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("auth/operation-not-allowed") ||
//                    e.getMessage().contains("DEVELOPER_ERROR"));
//        }
//    }
//
//    // ---------- Credential Handling ----------
//
//    @Test
//    public void testOAuthCredentialEquality() {
//        AuthCredential c1 = new OAuthCredential(PROVIDER_ID, "token123", null, null, null, null);
//        AuthCredential c2 = new OAuthCredential(PROVIDER_ID, "token123", null, null, null, null);
//
//        assertEquals(c1, c2);
//        assertEquals(c1.hashCode(), c2.hashCode());
//    }
//
//    @Test
//    public void testInvalidCredentialFails() {
//        try {
//            AuthCredential badCred = new OAuthCredential(PROVIDER_ID, "invalid", null, null, null, null);
//            auth.signInWithCredential(badCred).getResult();
//            fail("Expected failure with invalid OAuth credential");
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("INVALID_CREDENTIAL") ||
//                    e.getMessage().contains("auth/invalid-credential") ||
//                    e.getMessage().contains("operation-not-allowed"));
//        }
//    }
//}
