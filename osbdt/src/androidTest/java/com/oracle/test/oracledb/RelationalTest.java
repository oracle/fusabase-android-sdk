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

package com.oracle.test.oracledb;

import static com.oracle.database.osbdt.task.Tasks.await;
import static org.junit.Assert.*;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oracle.database.osbdt.OsbdtApp;
import com.oracle.database.osbdt.OsbdtOptions;
import com.oracle.database.osbdt.oracledb.AggregateField;
import com.oracle.database.osbdt.oracledb.AggregateQuerySnapshot;
import com.oracle.database.osbdt.oracledb.Query;
import com.oracle.database.osbdt.oracledb.AggregateSource;
import com.oracle.database.osbdt.oracledb.DocumentReference;
import com.oracle.database.osbdt.oracledb.DocumentSnapshot;
import com.oracle.database.osbdt.oracledb.FieldValue;
import com.oracle.database.osbdt.oracledb.OsbdtOracledb;
import com.oracle.database.osbdt.oracledb.QuerySnapshot;
import com.oracle.database.osbdt.oracledb.SetOptions;

import org.junit.*;
import org.junit.runner.RunWith;

import java.util.*;

@RunWith(AndroidJUnit4.class)
public class RelationalTest {

    private static OsbdtOracledb db;
    private static DocumentReference album1;

    @BeforeClass
    public static void setUpClass() throws Exception {
        OsbdtOptions.Builder optionsBuilder = new OsbdtOptions.Builder();
        optionsBuilder.setAuthType("base");
        optionsBuilder.setOrdsHost("http://phoenix555379.dev3sub4phx.databasede3phx.oraclevcn.com:8080/ords/USER1/");
        optionsBuilder.setProjectId("3C292A543E4AD6A8E0631F965E64EEE9");
        optionsBuilder.setAppId("3C292ADD4CCBD6C7E0631F965E64552C");
        optionsBuilder.setObjectType("dbfs");
        optionsBuilder.setStorageBucket("dbfs_CZXMDZFGRFGEITN");
        optionsBuilder.setApiVersion("1.0");
        optionsBuilder.setAppName("web-app1");
        optionsBuilder.setAuthId("3AA63EF01E17D2E1E0631F965E6455B5");

        OsbdtApp app;

        try{
            app = OsbdtApp.getInstance();
        } catch (Exception e)
        {
            Log.w("OsbdtTest", "Cannot find default instance. Creating the default instance.");
            app = OsbdtApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    , optionsBuilder.build());
        }

        db = OsbdtOracledb.getInstance(app);
    }

    @Test
    public void testUserPratikExists() throws Exception {
        DocumentSnapshot snap = await(db.collection("Users").document("FB03C10200").get());
        assertTrue(snap.exists());
        assertEquals("pratik", snap.getString("name"));
        assertEquals(29L, (long) snap.getLong("age"));
        assertEquals("hi pratik", snap.getString("about"));
    }

    @Test
    public void testAlbumOracleHasLikes() throws Exception {
        QuerySnapshot qs = await(db.collection("ALBUM").document("FB03C20200")
                .collection("album_likes").get());
        assertEquals(2, qs.size()); // like1, like3
        List<Long> counts = new ArrayList<>();
        for (DocumentSnapshot doc : qs) counts.add(doc.getLong("count"));
        assertTrue(counts.contains(1L));
        assertTrue(counts.contains(3L));
    }

    @Test
    public void testPhoto1000HasComments() throws Exception {
        QuerySnapshot qs = await(db.collection("Users").document("FB03C10200")
                .collection("photo").document("FB03C20B00").collection("Comments").get());
        assertEquals(2, qs.size());
        List<String> texts = new ArrayList<>();
        for (DocumentSnapshot doc : qs) texts.add(doc.getString("data"));
        assertTrue(texts.contains("nicephoto1"));
        assertTrue(texts.contains("nicephoto2"));
    }

    @Test
    public void testPhotoLikeCount() throws Exception {
        DocumentSnapshot snap = await(db.collection("Users").document("FB03C10200")
                .collection("photo").document("FB03C20B00").collection("PHOTO_likes")
                .document("FB03C20B0003C10200").get());
        assertEquals(1L, ((HashMap<String, Long>)snap.get("_id")).get("count").longValue());
    }

    @Test
    public void testFilterPhotosByAlbum100() throws Exception {
        QuerySnapshot qs = await(db.collection("Users")
                .whereEqualTo("age", 29).get());
        assertTrue(qs.size() == 2); // at least 1000–1003 belong to album 100
    }

    @Test
    public void testAggregateCountPhotosInAlbum100() throws Exception {
        AggregateQuerySnapshot agg = await(db.collection("Users")
                .whereEqualTo("age", 29)
                .count("UsersAgeCount").get(AggregateSource.SERVER));
        assertEquals(2, agg.getCount("UsersAgeCount")); // from insert data: pids 1000–1004
    }

    @Test
    public void testUpdateAlbumName() throws Exception {
        DocumentReference albumRef = db.collection("ALBUM").document("FB03C20200");
        await(albumRef.update("aname", "oracle_updated"));
        DocumentSnapshot snap = await(albumRef.get());
        assertEquals("oracle_updated", snap.getString("aname"));
    }

    @Test
    public void testDeleteComment() throws Exception {
        DocumentReference cRef = db.collection("Users")
                .document("FB03C10200")
                .collection("photo")
                .document("FB03C20B00")
                .collection("Comments")
                .document("FB03C302010200");
        await(cRef.delete());
        DocumentSnapshot snap = await(cRef.get());
        assertFalse(snap.exists());
    }

//    @Test
//    public void testCreateNewUser() throws Exception {
//        DocumentReference ref = await(db.collection("users")
//                .add(Map.of("uuid", 6, "name", "newuser", "age", 21, "about", "hi newuser")));
//        DocumentSnapshot snap = await(ref.get());
//        assertTrue(snap.exists());
//        assertEquals("newuser", snap.getString("name"));
//    }

    @Test
    public void testReadUserRyan() throws Exception {
        DocumentSnapshot snap = await(db.collection("Users").document("FB03C10600").get());
        assertTrue(snap.exists());
        assertEquals("ryan", snap.getString("name"));
        assertEquals(29L, (long) snap.getLong("age"));
    }

    @Test
    public void testUpdateUserAge() throws Exception {
        DocumentReference ref = db.collection("Users").document("FB03C10300");
        await(ref.update("age", 24));
        DocumentSnapshot snap = await(ref.get());
        assertEquals(24L, (long) snap.getLong("age"));
    }

//    @Test
//    public void testSetWithMergeFalse() throws Exception {
//        DocumentReference ref = db.collection("users").document("FB03C10400");
//        await(ref.set(Map.of("uuid", 3, "name", "himanshu_new")));
//        DocumentSnapshot snap = await(ref.get());
//        assertNull(snap.getLong("age")); // overwritten
//        assertEquals("himanshu_new", snap.getString("name"));
//    }

    @Test
    public void testSetWithMergeTrue() throws Exception {
        DocumentReference ref = db.collection("Users").document("FB03C10400");
        await(ref.set(Map.of("about", "updated about"), SetOptions.merge()));
        DocumentSnapshot snap = await(ref.get());
        assertEquals("updated about", snap.getString("about"));
//        assertEquals("himanshu_new", snap.getString("name")); // preserved
    }

//    @Test
//    public void testDeleteUser() throws Exception {
//        DocumentReference ref = db.collection("users").document("4");
//        await(ref.delete());
//        DocumentSnapshot snap = await(ref.get());
//        assertFalse(snap.exists());
//    }

    // ---------------- QUERY TESTS ----------------

    @Test
    public void testFilterUsersByAgeGreaterThan25() throws Exception {
        QuerySnapshot qs = await(db.collection("Users")
                .whereGreaterThan("age", 25).get());
        List<String> names = new ArrayList<>();
        for (DocumentSnapshot d : qs) names.add(d.getString("name"));
        assertTrue(names.contains("pratik"));
        assertTrue(names.contains("ryan"));
    }

    @Test
    public void testOrderUsersByAgeAsc() throws Exception {
        QuerySnapshot qs = await(db.collection("Users")
                .orderBy("age", Query.Direction.ASCENDING).get());
        List<String> ordered = new ArrayList<>();
        for (DocumentSnapshot d : qs) ordered.add(d.getString("name"));
        assertTrue(ordered.indexOf("hrishabh") < ordered.indexOf("pratik"));
        assertTrue(ordered.indexOf("hrishabh") < ordered.indexOf("ryan"));
    }

    @Test
    public void testOrderPhotosByCreatedDesc() throws Exception {
        QuerySnapshot qs = await(db.collection("Users").document("FB03C10200").collection("photo")
                .orderBy("created", Query.Direction.DESCENDING).limit(1).get());
        assertEquals(1, qs.size());
        assertNotNull(qs.getDocuments().get(0).get("created"));
    }

    // ---------------- AGGREGATE TESTS ----------------

    @Test
    public void testAggregateCountAllUsers() throws Exception {
        AggregateQuerySnapshot agg = await(db.collection("Users").count("usersCount").get(AggregateSource.SERVER));
        assertEquals(5, agg.getCount("usersCount")); // from seed data (before new inserts/deletes)
    }

    @Test
    public void testAggregateCountPhotosByAlbum101() throws Exception {
        AggregateQuerySnapshot agg = await(db.collection("Users").document("FB03C10200").collection("photo")
                .whereEqualTo("aid", 100).count("aidCount").get(AggregateSource.SERVER));
        assertEquals(2L, agg.getCount("aidCount"));
    }

    @Test
    public void testAggregateSumPhotoLikes() throws Exception {
        // sum all likes counts for photos
        QuerySnapshot qs = await(db.collectionGroup("likes").get());
        long sum = 0;
        for (DocumentSnapshot d : qs) {
            Long c = d.getLong("count");
            if (c != null) sum += c;
        }
        assertTrue(sum >= 10); // since each photo had at least 1 like
    }

    @Test
    public void testAggregateAverageUserAge() throws Exception {
        QuerySnapshot qs = await(db.collection("users").get());
        long total = 0;
        int count = 0;
        for (DocumentSnapshot d : qs) {
            Long age = d.getLong("age");
            if (age != null) {
                total += age;
                count++;
            }
        }
        double avg = (double) total / count;
        assertTrue(avg > 20 && avg < 30); // expected avg ~26.4
    }

    // ---------------- COMPOUND QUERY TESTS ----------------

    @Test
    public void testFilterUsersByNameAndAge() throws Exception {
        QuerySnapshot qs = await(db.collection("users")
                .whereEqualTo("name", "pratik")
                .whereEqualTo("age", 29).get());
        assertEquals(1, qs.size());
    }

    @Test
    public void testRangeFilterPhotosByPid() throws Exception {
        QuerySnapshot qs = await(db.collection("photos")
                .whereGreaterThanOrEqualTo("pid", 1002)
                .whereLessThanOrEqualTo("pid", 1004).get());
        List<Long> pids = new ArrayList<>();
        for (DocumentSnapshot d : qs) pids.add(d.getLong("pid"));
        assertTrue(pids.contains(1002L));
        assertTrue(pids.contains(1003L));
        assertTrue(pids.contains(1004L));
    }

    @Test
    public void testInQueryUsers() throws Exception {
        QuerySnapshot qs = await(db.collection("users")
                .whereIn("name", Arrays.asList("pratik", "ryan")).get());
        assertEquals(2, qs.size());
    }

    // ---------------- TRANSACTION TESTS ----------------

//    @Test
//    public void testTransactionIncrementAlbumLikes() throws Exception {
//        DocumentReference likeRef = db.collection("albums").document("100")
//                .collection("likes").document("1");
//        await(db.runTransaction(trx -> {
//            DocumentSnapshot snap = trx.get(likeRef).get();
//            long oldCount = snap.getLong("count");
//            trx.update(likeRef, "count", oldCount + 1);
//            return null;
//        }));
//        DocumentSnapshot snap = await(likeRef.get());
//        assertEquals(2L, (long) snap.getLong("count"));
//    }

    @Test
    public void testAddUserPhone() throws Exception {
        DocumentReference ref = db.collection("users").document("1")
                .collection("phones").document("999");
        await(ref.set(Map.of("phone", 999L)));
        DocumentSnapshot snap = await(ref.get());
        assertTrue(snap.exists());
        assertEquals(999L, (long) snap.getLong("phone"));
    }

    @Test
    public void testReadUserPhone() throws Exception {
        DocumentSnapshot snap = await(db.collection("users")
                .document("1").collection("phones").document("123").get());
        assertTrue(snap.exists());
        assertEquals(123L, (long) snap.getLong("phone"));
    }

    @Test
    public void testUpdateUserPhone() throws Exception {
        DocumentReference ref = db.collection("users").document("2")
                .collection("phones").document("234");
        await(ref.update("phone", 987L));
        DocumentSnapshot snap = await(ref.get());
        assertEquals(987L, (long) snap.getLong("phone"));
    }

    @Test
    public void testDeleteUserPhone() throws Exception {
        DocumentReference ref = db.collection("users").document("1")
                .collection("phones").document("123");
        await(ref.delete());
        DocumentSnapshot snap = await(ref.get());
        assertFalse(snap.exists());
    }

    // ---------------- COMMENTS_LIKES ----------------

    @Test
    public void testAddCommentLike() throws Exception {
        DocumentReference ref = db.collection("comments").document("10000")
                .collection("likes").document("2");
        await(ref.set(Map.of("cid", 10000L, "count", 2L)));
        DocumentSnapshot snap = await(ref.get());
        assertTrue(snap.exists());
        assertEquals(2L, (long) snap.getLong("count"));
    }

    @Test
    public void testReadCommentLike() throws Exception {
        DocumentSnapshot snap = await(db.collection("comments").document("10001")
                .collection("likes").document("1").get());
        assertTrue(snap.exists());
        assertEquals(1L, (long) snap.getLong("count"));
    }

    @Test
    public void testUpdateCommentLike() throws Exception {
        DocumentReference ref = db.collection("comments").document("10001")
                .collection("likes").document("1");
        await(ref.update("count", 5));
        DocumentSnapshot snap = await(ref.get());
        assertEquals(5L, (long) snap.getLong("count"));
    }

    @Test
    public void testDeleteCommentLike() throws Exception {
        DocumentReference ref = db.collection("comments").document("10002")
                .collection("likes").document("1");
        await(ref.delete());
        DocumentSnapshot snap = await(ref.get());
        assertFalse(snap.exists());
    }

    @Test
    public void testAggregateCommentLikes() throws Exception {
        QuerySnapshot qs = await(db.collectionGroup("likes").get());
        long total = 0;
        for (DocumentSnapshot d : qs) {
            Long c = d.getLong("count");
            if (c != null) total += c;
        }
        assertTrue(total >= 10); // seed data had ~12
    }

    // ---------------- ALBUM_LIKES ----------------

    @Test
    public void testAddAlbumLike() throws Exception {
        DocumentReference ref = db.collection("albums").document("100")
                .collection("likes").document("99");
        await(ref.set(Map.of("aid", 100L, "likesid", 99L, "count", 9L)));
        DocumentSnapshot snap = await(ref.get());
        assertTrue(snap.exists());
        assertEquals(9L, (long) snap.getLong("count"));
    }

    @Test
    public void testReadAlbumLike() throws Exception {
        DocumentSnapshot snap = await(db.collection("albums").document("100")
                .collection("likes").document("1").get());
        assertTrue(snap.exists());
        assertEquals(1L, (long) snap.getLong("count"));
    }

    @Test
    public void testUpdateAlbumLike() throws Exception {
        DocumentReference ref = db.collection("albums").document("101")
                .collection("likes").document("2");
        await(ref.update("count", 7));
        DocumentSnapshot snap = await(ref.get());
        assertEquals(7L, (long) snap.getLong("count"));
    }

    @Test
    public void testDeleteAlbumLike() throws Exception {
        DocumentReference ref = db.collection("albums").document("101")
                .collection("likes").document("4");
        await(ref.delete());
        DocumentSnapshot snap = await(ref.get());
        assertFalse(snap.exists());
    }

    @Test
    public void testAggregateAlbumLikes() throws Exception {
        QuerySnapshot qs = await(db.collectionGroup("likes").get());
        long total = 0;
        for (DocumentSnapshot d : qs) {
            Long c = d.getLong("count");
            if (c != null) total += c;
        }
        assertTrue(total >= 10); // album likes contributed multiple entries
    }

    // ---------------- ORDERING & FILTERING SUBCOLLECTIONS ----------------

    @Test
    public void testOrderAlbumLikesByCountDesc() throws Exception {
        QuerySnapshot qs = await(db.collection("albums").document("101")
                .collection("likes").orderBy("count", Query.Direction.DESCENDING).get());
        assertFalse(qs.isEmpty());
        long first = qs.getDocuments().get(0).getLong("count");
        long last = qs.getDocuments().get(qs.size() - 1).getLong("count");
        assertTrue(first >= last);
    }

    @Test
    public void testFilterCommentsLikesGreaterThan1() throws Exception {
        QuerySnapshot qs = await(db.collection("comments").document("10000")
                .collection("likes").whereGreaterThan("count", 0).get());
        assertFalse(qs.isEmpty());
    }

    @Test
    public void testFieldValueIncrementOnPhotoLikes() throws Exception {
        DocumentReference ref = db.collection("photo_likes").document("1000");
        await(ref.update("count", FieldValue.increment(1)));
        DocumentSnapshot snap = await(ref.get());
        assertTrue(snap.contains("count"));
        assertTrue(snap.getLong("count") >= 2); // seed was 1, increment should bump
    }

    @Test
    public void testFieldValueServerTimestamp() throws Exception {
        DocumentReference ref = db.collection("users").document("1");
        await(ref.update("lastActive", FieldValue.serverTimestamp()));
        DocumentSnapshot snap = await(ref.get());
        assertTrue(snap.contains("lastActive"));
        assertNotNull(snap.getTimestamp("lastActive"));
    }

    @Test
    public void testFieldValueArrayUnionAndRemove() throws Exception {
        DocumentReference ref = db.collection("users").document("1");
        await(ref.set(Map.of("tags", Arrays.asList("a", "b")), SetOptions.merge()));

        await(ref.update("tags", FieldValue.arrayUnion("c", "d")));
        DocumentSnapshot snapAfterUnion = await(ref.get());
        List<String> tags = (List<String>) snapAfterUnion.get("tags");
        assertTrue(tags.containsAll(Arrays.asList("a", "b", "c", "d")));

        await(ref.update("tags", FieldValue.arrayRemove("b", "d")));
        DocumentSnapshot snapAfterRemove = await(ref.get());
        List<String> tagsRemoved = (List<String>) snapAfterRemove.get("tags");
        assertFalse(tagsRemoved.contains("b"));
        assertFalse(tagsRemoved.contains("d"));
    }

    // ---------------- AGGREGATEFIELD TESTS ----------------

//    @Test
//    public void testAggregateCountPhotos() throws Exception {
//        AggregateQuerySnapshot agg = await(db.collection("photo").count().get(AggregateSource.SERVER));
//        assertTrue(agg.getCount() >= 10); // seed data has 10 photos
//    }
//
//    @Test
//    public void testAggregateSumPhotoLikes() throws Exception {
//        AggregateQuerySnapshot agg = await(
//                db.collection("photo_likes").aggregate(AggregateField.sum("count")).get(AggregateSource.SERVER)
//        );
//        Long total = agg.get(AggregateField.sum("count"));
//        assertNotNull(total);
//        assertTrue(total >= 10); // each photo had count=1 in seed
//    }

    @Test
    public void testAggregateAverageAlbumLikes() throws Exception {
        AggregateQuerySnapshot agg = await(
                db.collectionGroup("likes").aggregate(AggregateField.average("countAverage", "count")).get(AggregateSource.SERVER)
        );
        Double avg = agg.get(AggregateField.average("countAverage", "count"));
        assertNotNull(avg);
        assertTrue(avg >= 1.0);
    }

    @Test
    public void testAggregateFilteredCountCommentsOnPhoto1000() throws Exception {
        AggregateQuerySnapshot agg = await(
                db.collection("comments").whereEqualTo("pid", 1000L).count("CommentsPidCount").get(AggregateSource.SERVER)
        );
        assertEquals(2, agg.getCount("CommentsPidCount")); // seed has 2 comments on photo 1000
    }
}
