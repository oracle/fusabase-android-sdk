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

import com.oracle.database.osbdt.auth.AuthResult;
import com.oracle.database.osbdt.logger.OsbdtLogger;
import com.oracle.database.osbdt.auth.OsbdtAuth;
import com.oracle.database.osbdt.auth.OsbdtUser;
import com.oracle.database.osbdt.auth.UserProfileChangeRequest;
import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;
import com.oracle.database.osbdt.models.IDCSOptions;
import com.oracle.database.osbdt.task.Task;
import com.oracle.database.osbdt.task.Tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class OsbdtAuthTestIDCS {

    private OsbdtAuth auth;

    @Before
    public void setup() {
        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
        optionsBuilder.setAuthType("idcs");
        IDCSOptions.Builder idcsOptionsBuilder = new IDCSOptions.Builder();
        idcsOptionsBuilder.setClientId("44d4a5a58a80476e90389ab9f403db87");
        idcsOptionsBuilder.setClientSecret("idcscs-3a5ba1c4-b496-443d-ac9e-5efd7125d5c2");
        idcsOptionsBuilder.setDomainURL("https://idcs-360c1204d11f41d3836e7fb2f1769ca7.identity.oraclecloud.com");
        idcsOptionsBuilder.setSelfRegistrationProfile("f336e6256b9644059663394fe75d7494");
        optionsBuilder.setIDCSOptions(idcsOptionsBuilder.build());
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("43C68B185E9732FEE0631F965E641907");
        optionsBuilder.setAppId("43C6C814E61A4D68E0631F965E642FFE");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_CZXMDZFGRFGEITN");
        optionsBuilder.setApiVersion("2.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("43C6C6DC03224D43E0631F965E6456DC");
        optionsBuilder.setUseSocket(true);

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
        return Tasks.await(task, 80, TimeUnit.SECONDS);
    }

    @Test
    public void testCreateAndSignInUser() throws Exception {
        String email = "user_create18@example.com";
        String password = "Password@123";

        AuthResult result = await(auth.createUserWithEmailAndPassword(email, password));
        assertNotNull(result.getUser());

        OsbdtUser signInUser = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        assertNotNull(signInUser);
        assertEquals(email, signInUser.getEmail());
    }

    @Test
    public void testPasswordChangeAndSigninWithLength8() throws Exception {
        String email = "user_create37@example.com";
        String password = "Password@123";
        String newPassword = "Password@456";

        OsbdtUser user = await(auth.createUserWithEmailAndPassword(email, password)).getUser();
        await(auth.signInWithEmailAndPassword(email, password));
        Log.d("AuthBaseTests", "User signed in successfully with password=" + password);
        user.updatePassword(newPassword);
        Log.d("AuthBaseTests", "User password=" + password + " changed to password=" + newPassword);
        await(auth.signInWithEmailAndPassword(email, newPassword));
        Log.d("AuthBaseTests", "User signed in successfully with password=" + newPassword);
        await(auth.signOut());
        Boolean isFailed = false;
        try {
            await(auth.signInWithEmailAndPassword(email, password));
            fail("Expected Sign in Error after password change for [" + email + "] from " + password + "->" + newPassword);
        } catch (Exception e) {
            isFailed = true;
        }
        assertEquals(true, isFailed);

    }

    @Test
    public void testSignInUpdatesLastSignInTime() throws Exception {
        Log.d("AuthBaseTests", "Running signInUpdatesLastSignInTime test");

        String email = "user_create38@example.com";
        String password = "Password@123";
        Log.d("AuthBaseTests", "Generated user credentials email: " + email + " USER_CREDENTIALS");

//        try {
//            await(auth.createUserWithEmailAndPassword(email, password));
//            Log.d("AuthBaseTests", "User account created successfully email: " + email + " USER_CREATION");
//        } catch (Exception e) {
//            fail("User creation failed: " + e.getMessage());
//        }

        long firstSignInTime = 0;
        OsbdtUser signInRes1 = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        firstSignInTime = signInRes1.getMetadata().getLastSignInTimestamp();

        try {
            await(auth.signOut());
            Log.d("AuthBaseTests", "User signed out successfully email: " + email);
        } catch (Exception e) {
            fail("Sign out failed: " + e.getMessage());
        }
        Thread.sleep(6000);

        long secondSignInTime;
        try {
            OsbdtUser signInRes2 = await(auth.signInWithEmailAndPassword(email, password)).getUser();
            secondSignInTime = signInRes2.getMetadata().getLastSignInTimestamp();
            Log.d("AuthBaseTests", "Second sign-in successful email: " + email + " secondSignInTime: " + secondSignInTime);

            assertTrue("Expected second lastSignInTime [" + secondSignInTime + "] to be greater than first [" + firstSignInTime + "]", secondSignInTime > firstSignInTime);
        } catch (Exception e) {
            fail("Unable to sign in after sign-out email:" + email + " : " + e.getMessage());
        }
    }

    @Test
    public void testUpdateUserPhoneNumber() throws Exception {
        Log.d("AuthBaseTests", "Running Update User Phone Number test");

        String email = "user_create38@example.com";
        String password = "Password@123";
        Log.d("AuthBaseTests", "Generated user credentials email: " + email + " USER_CREDENTIALS");

//        try {
//            await(auth.createUserWithEmailAndPassword(email, password));
//            Log.d("AuthBaseTests", "User account created successfully email: " + email + " USER_CREATION");
//        } catch (Exception e) {
//            fail("User creation failed: " + e.getMessage());
//        }

        OsbdtUser res = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        OsbdtUser user = res;
        String newPhoneNumber = "012345689";
        String newPhotoUrl = "www.google.com";
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
            .setPhoneNumber(newPhoneNumber)
//            .setPhotoUri(newPhotoUrl)
            .build();

        assert user != null;
        try {
            await(user.updateProfile(profileUpdates));
        } catch (Exception e) {
            Log.d("AuthBaseTests", e.getMessage());
        }
        await(auth.signOut());

        OsbdtUser updatedUser = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        assertEquals(newPhoneNumber, updatedUser.getPhoneNumber());
//        assertEquals(newPhotoUrl, updatedUser.getPhotoUrl());

        Log.d("AuthBaseTests", "Test completed: Update User Phone Number");
    }

    @Test
    public void testDuplicateAccountCreation() throws Exception {
        String email = "user_create17@example.com";
        String password = "Password@123";
        OsbdtLogger.d(email);
        OsbdtLogger.d(password);
        OsbdtUser user = await(auth.createUserWithEmailAndPassword(email, password)).getUser();
        Boolean isError = false;
        try {
            await(auth.createUserWithEmailAndPassword(email, password));
        } catch (Exception e) {
            isError = true;
        }
        assertTrue("User Registration Successful", isError);

    }

    @Test
    public void testSignOutAndCurrentUser() throws Exception {
        String email = "user_signout1@example.com";
        String password = "Mypassword@123";
        await(auth.createUserWithEmailAndPassword(email, password));

        assertNotNull(auth.getCurrentUser());

        await(auth.signOut());
        assertNull(auth.getCurrentUser());
    }

    @Test
    public void testPasswordResetEmail() throws Exception {
        String email = "mohit.mv.verma@oracle.com";

        await(auth.sendPasswordResetEmail(email));
        assertTrue(true); // success if no exception
    }

    @Test
    public void testUpdateEmailAndPassword() throws Exception {
        String email = "update6@example.com";
        String password = "Oldpass@123";
        OsbdtUser user = await(auth.createUserWithEmailAndPassword(email, password)).getUser();
//        OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        assertNotNull(user);

        await(user.updatePassword("Newpass@123"));

        OsbdtUser refreshed = auth.getCurrentUser();
        assertEquals("update6@example.com", refreshed.getEmail());
    }

    @Test
    public void testUpdateProfile() throws Exception {
        String email = "profile1@example.com";
        String password = "Profilepass@123";
//        OsbdtUser user = await(auth.createUserWithEmailAndPassword(email, password)).getUser();
        OsbdtUser user = await(auth.signInWithEmailAndPassword(email, password)).getUser();
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName("Test1 User")
                .setPhotoUri("http://abc.com")
                .setPhoneNumber("919958465715")
                .build();

        await(user.updateProfile(profileUpdates));

        OsbdtUser updatedUser = auth.getCurrentUser();
        assertEquals("Test1 User", updatedUser.getDisplayName());
        assertEquals("http://abc.com", updatedUser.getPhotoUrl());
        assertEquals("919958465715", updatedUser.getPhoneNumber());
    }

    @Test
    public void testGetIdToken() throws Exception {
        String email = "token@example.com";
        String password = "Tokenpass@123";
        OsbdtUser user = await(auth.createUserWithEmailAndPassword(email, password)).getUser();

        String token = await(user.getIdToken(false)).getToken();
        assertNotNull(token);

        String freshToken = await(user.getIdToken(true)).getToken();
        assertNotNull(freshToken);
        assertNotEquals(token, freshToken);
    }
}
