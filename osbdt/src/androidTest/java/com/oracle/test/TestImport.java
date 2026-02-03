package com.oracle.test;


import android.util.Log;

import com.oracle.database.osbdt.auth.OsbdtAuth;
import com.oracle.database.osbdt.oracledb.DocumentReference;
import com.oracle.database.osbdt.oracledb.DocumentSnapshot;
import com.oracle.database.osbdt.oracledb.FieldValue;
import com.oracle.database.osbdt.oracledb.OsbdtOracledb;
import com.oracle.database.osbdt.oracledb.Query;
import com.oracle.database.osbdt.oracledb.QuerySnapshot;
import com.oracle.database.osbdt.task.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.junit.Test;
public class TestImport {


    @Test
    public void testImport () {


        OsbdtOracledb db = OsbdtOracledb.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("createdAt", FieldValue.serverTimestamp());

// Set document with server timestamp
        db.collection("users").document("Scott").set(data);
    }


}
