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
//import android.net.Uri;
//
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//
//import com.oracle.mobile.fusabase.auth.UserProfileChangeRequest;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//@RunWith(AndroidJUnit4.class)
//public class UserProfileChangeRequestTest {
//
//    @Test
//    public void testSetDisplayNameOnly() {
//        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
//                .setDisplayName("Test User")
//                .build();
//
//        assertEquals("Test User", req.getDisplayName());
//        assertNull(req.getPhotoUri());
//    }
//
//    @Test
//    public void testSetPhotoUriOnly() {
//        Uri uri = Uri.parse("http://example.com/photo.png");
//
//        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
//                .setPhotoUri(uri)
//                .build();
//
//        assertEquals(uri, req.getPhotoUri());
//        assertNull(req.getDisplayName());
//    }
//
//    @Test
//    public void testSetBothDisplayNameAndPhotoUri() {
//        Uri uri = Uri.parse("http://example.com/photo.png");
//
//        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
//                .setDisplayName("Combined User")
//                .setPhotoUri(uri)
//                .build();
//
//        assertEquals("Combined User", req.getDisplayName());
//        assertEquals(uri, req.getPhotoUri());
//    }
//
//    @Test
//    public void testClearDisplayNameAndPhotoUri() {
//        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
//                .setDisplayName(null)
//                .setPhotoUri(null)
//                .build();
//
//        assertNull(req.getDisplayName());
//        assertNull(req.getPhotoUri());
//    }
//
//    @Test
//    public void testEqualsAndHashCode() {
//        Uri uri = Uri.parse("http://example.com/photo.png");
//
//        UserProfileChangeRequest req1 = new UserProfileChangeRequest.Builder()
//                .setDisplayName("User A")
//                .setPhotoUri(uri)
//                .build();
//
//        UserProfileChangeRequest req2 = new UserProfileChangeRequest.Builder()
//                .setDisplayName("User A")
//                .setPhotoUri(uri)
//                .build();
//
//        assertEquals(req1, req2);
//        assertEquals(req1.hashCode(), req2.hashCode());
//    }
//
//    @Test
//    public void testToStringContainsFields() {
//        Uri uri = Uri.parse("http://example.com/photo.png");
//
//        UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
//                .setDisplayName("Visible User")
//                .setPhotoUri(uri)
//                .build();
//
//        String str = req.toString();
//        assertTrue(str.contains("Visible User"));
//        assertTrue(str.contains("photo.png"));
//    }
//}
//
