package com.oracle.test;

import com.oracle.database.osbdt.oracledb.OsbdtOracledb;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TestImport {


    @Test
    public void testImport () {
        OsbdtOracledb db = OsbdtOracledb.getInstance();

        // 1️⃣ Create/Update user document
        Map<String, Object> user = new HashMap<>();
        user.put("displayName", "Scott");
        user.put("photoURL", null); // or user.getPhotoUrl()

        db.collection("users").document("Scott")
                .set(user);

        // 2️⃣ Create recipe subcollection document
        Map<String, Object> recipe = new HashMap<>();
        recipe.put("uuid", "Scott");
        recipe.put("title", "Chocolate Chip Cookies");
        recipe.put("ingredients", "2 cups flour,1 cup sugar,1/2 cup chocolate chips");
        recipe.put("steps", Arrays.asList("Mix ingredients", "Bake for 15 min"));
        recipe.put("category", "Dessert");
        recipe.put("difficulty", "Easy");
        recipe.put("tags", Arrays.asList("sweet", "baking"));
        recipe.put("avgRating", 0);
        recipe.put("photoRefs", new ArrayList<>());

        db.collection("users").document("Scott")
                .collection("recipes").document("Cookies")
                .set(recipe);
    }

}
