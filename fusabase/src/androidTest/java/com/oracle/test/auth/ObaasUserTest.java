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
//import static org.junit.Assert.*;
//
//import android.net.Uri;
//
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//
//import com.oracle.mobile.fusabase.auth.AuthCredential;
//import com.oracle.mobile.fusabase.auth.EmailAuthProvider;
//import com.oracle.mobile.fusabase.auth.FusabaseAuth;
//import com.oracle.mobile.fusabase.auth.FusabaseUser;
//import com.oracle.mobile.fusabase.auth.UserInfo;
//import com.oracle.mobile.fusabase.auth.UserProfileChangeRequest;
//import com.oracle.mobile.fusabase.task.Task;
//import com.oracle.mobile.fusabase.task.Tasks;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//@RunWith(AndroidJUnit4.class)
//public class FusabaseUserTest {
//
//    private FusabaseAuth auth;
//
//    @Before
//    public void setup() {
//        auth = FusabaseAuth.getInstance();
//        auth.signOut();
//    }
//
//    private <T> T await(Task<T> task) throws Exception {
//        return Tasks.await(task, 5, TimeUnit.SECONDS);
//    }
//
//    private FusabaseUser createTestUser(String email, String password) throws Exception {
//        return await(auth.createUserWithEmailAndPassword(email, password)).getUser();
//    }
//
//    @Test
//    public void testBasicIdentity() throws Exception {
//        FusabaseUser user = createTestUser("identity@example.com", "pass123");
//        assertNotNull(user.getUid());
//        assertEquals("identity@example.com", user.getEmail());
//        assertFalse(user.isAnonymous());
//    }
//
//    @Test
//    public void testProviderData() throws Exception {
//        FusabaseUser user = createTestUser("provider@example.com", "pass456");
//        List<? extends UserInfo> providers = user.getProviderData();
//        assertFalse(providers.isEmpty());
//        assertTrue(
//                providers.stream().anyMatch(info -> "password".equals(info.getProviderId()))
//        );
//    }
//
//    @Test
//    public void testUpdateProfile() throws Exception {
//        FusabaseUser user = createTestUser("profileuser@example.com", "profilepass");
//
//        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
//                .setDisplayName("My Test User")
//                .setPhotoUri(Uri.parse("http://example.com/avatar.png"))
//                .build();
//
//        await(user.updateProfile(request));
//        await(user.reload()); // refresh cached data
//
//        FusabaseUser refreshed = auth.getCurrentUser();
//        assertEquals("My Test User", refreshed.getDisplayName());
//        assertEquals("http://example.com/avatar.png", refreshed.getPhotoUrl().toString());
//    }
//
//    @Test
//    public void testUpdateEmailAndPassword() throws Exception {
//        FusabaseUser user = createTestUser("change@example.com", "oldpass");
//
//        await(user.updateEmail("changed@example.com"));
//        await(user.updatePassword("newpass123"));
//
//        await(user.reload());
//        assertEquals("changed@example.com", auth.getCurrentUser().getEmail());
//    }
//
//    @Test
//    public void testIdToken() throws Exception {
//        FusabaseUser user = createTestUser("tokenuser@example.com", "tokpass");
//
//        String token1 = await(user.getIdToken(false)).getToken();
//        assertNotNull(token1);
//
//        String token2 = await(user.getIdToken(true)).getToken();
//        assertNotNull(token2);
//        assertNotEquals(token1, token2); // forced refresh gives new token
//    }
//
//    @Test
//    public void testReauthenticate() throws Exception {
//        String email = "reauthuser@example.com";
//        String password = "reauthpass";
//        FusabaseUser user = createTestUser(email, password);
//
//        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
//        await(user.reauthenticate(credential));
//        assertTrue(true); // success means no exception thrown
//    }
//
//    @Test
//    public void testLinkAndUnlinkCredential() throws Exception {
//        String email = "linkuser@example.com";
//        String password = "linkpass";
//        FusabaseUser user = createTestUser(email, password);
//
//        // Create secondary credential
//        AuthCredential newCred = EmailAuthProvider.getCredential("altuser@example.com", "altpass");
//
//        FusabaseUser linked = await(user.linkWithCredential(newCred)).getUser();
//        assertNotNull(linked);
//
//        FusabaseUser unlinked = await(user.unlink("password")).getUser(); // unlink email/password provider
//        assertNotNull(unlinked);
//    }
//
//    @Test
//    public void testDeleteUser() throws Exception {
//        FusabaseUser user = createTestUser("deleteuser@example.com", "deletepass");
//
//        await(user.delete());
//
//        auth.signOut();
//        try {
//            await(auth.signInWithEmailAndPassword("deleteuser@example.com", "deletepass"));
//            fail("Expected failure, user should be deleted");
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("no user record"));
//        }
//    }
//}
