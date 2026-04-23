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

package com.oracle.test.storage;

import static org.junit.Assert.*;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.storage.Storage;
import com.oracle.mobile.fusabase.storage.StorageReference;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StorageTest {

    private static Storage storage;

    @BeforeClass
    public static void setupClass() {
        FusabaseOptions.Builder optionsBuilder = new FusabaseOptions.Builder();
        optionsBuilder.setAuthType("base");
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("3C292A543E4AD6A8E0631F965E64EEE9");
        optionsBuilder.setAppId("3C292ADD4CCBD6C7E0631F965E64552C");
//        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setObjectType("oci");
//        optionsBuilder.setStorageBucket("dbfs_DVQANXJSRTZOFKK");
        optionsBuilder.setStorageBucket("baas_bucket");
        optionsBuilder.setApiVersion("1.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("3AA63EF01E17D2E1E0631F965E6455B5");

        FusabaseApp app;

        try{
            app = FusabaseApp.getInstance();
        } catch (Exception e)
        {
            Log.w("FusabaseTest", "Cannot find default instance. Creating the default instance.");
            app = FusabaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    , optionsBuilder.build());
        }
        storage = Storage.getInstance(app);
    }

    // ----------- BASIC BUCKET & INSTANCE TESTS -----------

    @Test
    public void testGetInstanceDefaultBucket() {
        assertNotNull(storage);
        assertNotNull(storage.getReference());
    }

    // ----------- STORAGE REFERENCE CREATION TESTS -----------

    @Test
    public void testGetReference() {
        StorageReference rootRef = storage.getReference();
        assertNotNull(rootRef);
        assertEquals("", rootRef.getPath());
    }

    @Test
    public void testGetReferenceWithPath() {
        StorageReference childRef = storage.getReference("test_dir/test_file.txt");
        assertNotNull(childRef);
        assertTrue(childRef.getPath().contains("test_dir/test_file.txt"));
    }

}
