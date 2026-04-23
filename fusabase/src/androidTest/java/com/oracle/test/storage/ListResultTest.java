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

import android.net.Uri;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.mobile.fusabase.FusabaseApp;
import com.oracle.mobile.fusabase.FusabaseOptions;
import com.oracle.mobile.fusabase.storage.ListResult;
import com.oracle.mobile.fusabase.storage.Storage;
import com.oracle.mobile.fusabase.storage.StorageReference;
import com.oracle.mobile.fusabase.task.Tasks;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ListResultTest {

    private static Storage storage;
    private static StorageReference rootRef;

    @BeforeClass
    public static void setupClass() {
        FusabaseOptions.Builder optionsBuilder = new FusabaseOptions.Builder();
        optionsBuilder.setAuthType("base");
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("3E61FC42BE088297E0631F965E64B166");
        optionsBuilder.setAppId("3E61FC965B0C82AFE0631F965E646705");
//        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setObjectType("oci");
//        optionsBuilder.setStorageBucket("dbfs_GKFLGYFWQEDSEED");
        optionsBuilder.setStorageBucket("baas_bucket");
        optionsBuilder.setApiVersion("1.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("3E61FC54CE2B829FE0631F965E64E3E5");

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
        rootRef = storage.getReference().child("listResultTests");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // delete entire test folder recursively
        ListResult list = Tasks.await(rootRef.listAll(), 10, TimeUnit.SECONDS);
        for (StorageReference file : list.getItems()) {
            Tasks.await(file.delete(), 5, TimeUnit.SECONDS);
        }
        for (StorageReference folder : list.getPrefixes()) {
            ListResult sub = Tasks.await(folder.listAll(), 5, TimeUnit.SECONDS);
            for (StorageReference subItem : sub.getItems()) {
                Tasks.await(subItem.delete(), 5, TimeUnit.SECONDS);
            }
            Tasks.await(folder.delete(), 5, TimeUnit.SECONDS);
        }
        Tasks.await(rootRef.delete(), 5, TimeUnit.SECONDS);
    }

    private Uri createTempFile(String name, String content) throws Exception {
        File file = File.createTempFile(name, ".txt");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes());
        }
        return Uri.fromFile(file);
    }

    // ---------------- INTEGRATION TESTS ----------------

    @Test
    public void testListAllFilesAndPrefixes() throws Exception {
        // create files
        Uri file1 = createTempFile("file1", "file1-content");
        Uri file2 = createTempFile("file2", "file2-content");
        StorageReference f1 = rootRef.child("file1.txt");
        StorageReference f2 = rootRef.child("file2.txt");

        Tasks.await(f1.putFile(file1), 50, TimeUnit.SECONDS);
        Tasks.await(f2.putFile(file2), 50, TimeUnit.SECONDS);

        // create subfolder and file inside
        StorageReference subFolder = rootRef.child("subfolder");
        StorageReference subFile = subFolder.child("inside.txt");
        Uri inside = createTempFile("inside", "subfolder-content");
        Tasks.await(subFile.putFile(inside), 50, TimeUnit.SECONDS);

        // now list all
        ListResult result = Tasks.await(rootRef.listAll(), 50, TimeUnit.SECONDS);

        assertNotNull(result);
        assertFalse(result.getItems().isEmpty());
        assertTrue(result.getItems().size() >= 3);

        // verify files
        List<StorageReference> items = result.getItems();
        assertTrue(items.stream().anyMatch(r -> r.getName().equals("file1.txt")));
        assertTrue(items.stream().anyMatch(r -> r.getName().equals("file2.txt")));

    }

    @Test
    public void testListWithPagination() throws Exception {
        // create multiple files
        for (int i = 0; i < 5; i++) {
            Uri f = createTempFile("paginated_" + i, "content_" + i);
            Tasks.await(rootRef.child("paginated_" + i + ".txt").putFile(f), 10, TimeUnit.SECONDS);
        }

        // request first page with maxResults = 2
        ListResult firstPage = Tasks.await(rootRef.list(2), 10, TimeUnit.SECONDS);
        assertNotNull(firstPage);
        assertEquals(2, firstPage.getItems().size());
        assertNotNull(firstPage.getPageToken());

        // get next page
        ListResult secondPage = Tasks.await(rootRef.list(2, firstPage.getPageToken()), 10, TimeUnit.SECONDS);
        assertNotNull(secondPage);
        assertTrue(secondPage.getItems().size() >= 1);

        // make sure we don’t repeat firstPage items
        assertFalse(firstPage.getItems().get(0).getName()
                .equals(secondPage.getItems().get(0).getName()));
    }

    @Test
    public void testEmptyListResult() throws Exception {
        StorageReference emptyFolder = rootRef.child("emptyFolder");
        ListResult result = Tasks.await(emptyFolder.listAll(), 10, TimeUnit.SECONDS);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertTrue(result.getPrefixes().isEmpty());
    }
}

