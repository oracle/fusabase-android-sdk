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

package com.oracle.test.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.database.osbdt.auth.OsbdtAuth;
import com.oracle.database.osbdt.auth.OsbdtUser;
import com.oracle.database.osbdt.auth.UserProfileChangeRequest;
import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.Tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class OsbdtAuthTestBase {

    private OsbdtAuth auth;

    @Before
    public void setup() {
        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
        optionsBuilder.setAuthType("base");
//        IDCSOptions.Builder idcsOptionsBuilder = new IDCSOptions.Builder();
//        idcsOptionsBuilder.setClientId("cdabbf63124a4fcca4acebbfb456b501");
//        idcsOptionsBuilder.setClientSecret("idcscs-dba52c76-db11-4503-b4e1-a223994b1465");
//        idcsOptionsBuilder.setDomainURL("https://idcs-360c1204d11f41d3836e7fb2f1769ca7.identity.oraclecloud.com");
//        idcsOptionsBuilder.setSelfRegistrationProfile("3a192c21a94e43a9adcbab0ffcaae893");
//        optionsBuilder.setIDCSOptions(idcsOptionsBuilder.build());
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("43C68B185E9732FEE0631F965E641907");
        optionsBuilder.setAppId("43C6C814E61A4D68E0631F965E642FFE");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_CZXMDZFGRFGEITN");
        optionsBuilder.setApiVersion("1.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("43C6C6DC03224D43E0631F965E6456DC");

        OsbdtApp app;

        try{
            app = OsbdtApp.getInstance();
        } catch (Exception e)
        {
            Log.w("OsbdtTest", "Cannot find default instance. Creating the default instance.");
            app = OsbdtApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    , optionsBuilder.build());
        }
        auth = OsbdtAuth.getInstance(app);
    }

    // Utility
    private <T> T await(Task<T> task) throws Exception {
        return Tasks.await(task, 15, TimeUnit.SECONDS);
    }

    @Test
    public void testCreateAndSignInUser() throws Exception {
        String email = "user_create@example.com";
        String password = "password123";

//        AuthResult result = await(auth.createUserWithEmailAndPassword(email, password));
//        assertNotNull(result.getUser());

        OsbdtUser signInUser = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        assertNotNull(signInUser);
        assertEquals(email, signInUser.getEmail());
    }
    @Test
    public void testUpdateUserProfileDisplayName() {
        String email = "abzeghwretterk@oracle.com";
        String password = "MOjfisd5";

        try {
            await(auth.createUserWithEmailAndPassword(email, password));
        } catch (Exception e) {
            Log.d("AuthBaseTests", e.getMessage());
        }
        assertEquals("- -", auth.getCurrentUser().getDisplayName());

        OsbdtUser user;
        try {
            user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        } catch (Exception e) {
            Log.d("AuthBaseTests", e.getMessage());
            return;
        }
        assertEquals("- -", auth.getCurrentUser().getDisplayName());
        String newDisplayName = "SID";
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
            .setDisplayName(newDisplayName)
            .build();
        OsbdtUser newRes;
        try {
            await(user.updateProfile(profileUpdates));
            newRes = auth.getCurrentUser();
            assertEquals(newDisplayName, newRes.getDisplayName());
            assertEquals("SID", auth.getCurrentUser().getDisplayName());
        } catch (Exception e) {
            Log.d("AuthBaseTests", e.getMessage());
        }
    }

    @Test
    public void testUpdateUserPhoneNumber() throws Exception {
        Log.d("AuthBaseTests", "Running Update User Phone Number test");

        String email = "abcd@oracle.com";
        String password = "MOjfisd5";
        Log.d("AuthBaseTests", "Generated user credentials email: " + email + " USER_CREDENTIALS");

        try {
            await(auth.signInWithEmailAndPassword(email, password));
            Log.d("AuthBaseTests", "User account created successfully email: " + email + " USER_CREATION");
        } catch (Exception e) {
            fail("User creation failed: " + e.getMessage());
        }

        OsbdtUser res = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        OsbdtUser user = res;
        String newPhoneNumber = "012345689";

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
            .setPhoneNumber(newPhoneNumber)
            .build();

        assert user != null;
        await(user.updateProfile(profileUpdates));
        await(user.reload());
        await(auth.signOut());

        OsbdtUser updatedUser = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        assertEquals(newPhoneNumber, updatedUser.getPhoneNumber());

        Log.d("AuthBaseTests", "Test completed: Update User Phone Number");
    }

    @Test
    public void testSignOutAndCurrentUser() throws Exception {
        String email = "user_signout1@example.com";
        String password = "mypassword";
        await(auth.createUserWithEmailAndPassword(email, password));
//        await(auth.signInWithEmailAndPassword(email, password));

        assertNotNull(auth.getCurrentUser());

        await(auth.signOut());
        assertNull(auth.getCurrentUser());
    }

    @Test
    public void testPasswordResetEmail() throws Exception {
        String email = "mohit.mv.verma@oracle.com";
        String password = "Samosa@20";
        await(auth.createUserWithEmailAndPassword(email, password));

        await(auth.sendPasswordResetEmail(email));
        assertTrue(true); // success if no exception
    }

    @Test
    public void testUpdateEmailAndPassword() throws Exception {
        String email = "update6@example.com";
        String password = "oldpass123";
//        OsbdtUser user = await(auth.createUserWithEmailAndPassword(email, password)).getUser();
        OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        assertNotNull(user);

        await(user.updatePassword("newpass123"));

        OsbdtUser refreshed = auth.getCurrentUser();
        assertEquals("update6@example.com", refreshed.getEmail());
    }

    @Test
    public void testUpdateProfile() throws Exception {
        String email = "profile1@example.com";
        String password = "profilepass";
        OsbdtUser user = await(auth.createUserWithEmailAndPassword(email, password)).getUser();
//        OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName("Test1 User")
                .build();

        await(user.updateProfile(profileUpdates));

        OsbdtUser updatedUser = auth.getCurrentUser();
        assertEquals("Test1 User", updatedUser.getDisplayName());
    }

    @Test
    public void testGetIdToken() throws Exception {
        String email = "token@example.com";
        String password = "tokenpass";
//        OsbdtUser user = await(auth.createUserWithEmailAndPassword(email, password)).getUser();
        OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();

        String token = await(user.getIdToken(false)).getToken();
        assertNotNull(token);

        String freshToken = await(user.getIdToken(true)).getToken();
        assertNotNull(freshToken);
        assertNotEquals(token, freshToken);
    }
}
