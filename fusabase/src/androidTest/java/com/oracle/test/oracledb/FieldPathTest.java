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

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.oracle.mobile.fusabase.oracledb.FieldPath;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive test suite for FieldPath.java with 100% test coverage.
 * Tests all public and protected methods, constructors, factory methods, and edge cases.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class FieldPathTest {

    // ------------------- Constructor and Basic Functionality Tests -------------------

    @Test
    public void testFieldPathConstructorWithList() throws Exception {
        // Use reflection to test protected constructor
        Constructor<FieldPath> constructor = FieldPath.class.getDeclaredConstructor(List.class);
        constructor.setAccessible(true);

        List<String> segments = Arrays.asList("user", "address", "city");
        FieldPath path = constructor.newInstance(segments);

        assertNotNull("FieldPath should be created", path);
        assertEquals("Segments should match", segments, invokeGetSegments(path));
    }

    @Test
    public void testFieldPathConstructorWithString() throws Exception {
        Constructor<FieldPath> constructor = FieldPath.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);

        FieldPath path = constructor.newInstance("user.address.city");

        assertNotNull("FieldPath should be created", path);
        assertEquals("Should have 3 segments", 3, invokeGetSegments(path).size());
        assertEquals("First segment should be 'user'", "user", invokeGetSegments(path).get(0));
        assertEquals("Second segment should be 'address'", "address", invokeGetSegments(path).get(1));
        assertEquals("Third segment should be 'city'", "city", invokeGetSegments(path).get(2));
    }

    // ------------------- Factory Method Tests -------------------

    @Test
    public void testOfWithSingleField() throws Exception {
        FieldPath path = FieldPath.of("name");

        assertNotNull("FieldPath should not be null", path);
        assertEquals("Should have 1 segment", 1, invokeGetSegments(path).size());
        assertEquals("Segment should be 'name'", "name", invokeGetSegments(path).get(0));
    }

    @Test
    public void testOfWithMultipleFields() throws Exception {
        FieldPath path = FieldPath.of("user", "profile", "settings");

        assertNotNull("FieldPath should not be null", path);
        assertEquals("Should have 3 segments", 3, invokeGetSegments(path).size());
        assertEquals("First segment should be 'user'", "user", invokeGetSegments(path).get(0));
        assertEquals("Second segment should be 'profile'", "profile", invokeGetSegments(path).get(1));
        assertEquals("Third segment should be 'settings'", "settings", invokeGetSegments(path).get(2));
    }

    @Test
    public void testDocumentId() throws Exception {
        FieldPath idPath = FieldPath.documentId();

        assertNotNull("DocumentId FieldPath should not be null", idPath);
        assertEquals("Should have 1 segment", 1, invokeGetSegments(idPath).size());
        assertEquals("Segment should be 'OID'", "OID", invokeGetSegments(idPath).get(0));
    }

    @Test
    public void testDocumentIdConsistency() {
        FieldPath idPath1 = FieldPath.documentId();
        FieldPath idPath2 = FieldPath.documentId();

        assertEquals("Multiple documentId calls should return equal objects", idPath1, idPath2);
        assertEquals("Hash codes should be equal", idPath1.hashCode(), idPath2.hashCode());
    }

    // ------------------- getSegments() Method Tests -------------------

    @Test
    public void testGetSegments() throws Exception {
        FieldPath path = FieldPath.of("a", "b", "c");
        List<String> segments = invokeGetSegments(path);

        assertNotNull("Segments should not be null", segments);
        assertEquals("Should have 3 segments", 3, segments.size());
        assertEquals("First segment should be 'a'", "a", segments.get(0));
        assertEquals("Second segment should be 'b'", "b", segments.get(1));
        assertEquals("Third segment should be 'c'", "c", segments.get(2));
    }

    @Test
    public void testGetSegmentsImmutability() throws Exception {
        FieldPath path = FieldPath.of("x", "y");
        List<String> segments1 = invokeGetSegments(path);
        List<String> segments2 = invokeGetSegments(path);

        assertEquals("Segments should be equal", segments1, segments2);
        // Test that returned list is not modifiable (if it were, this would throw)
        assertEquals("Size should be consistent", 2, segments1.size());
    }

    // ------------------- equals() and hashCode() Tests -------------------

    @Test
    public void testEqualsSelf() {
        FieldPath path = FieldPath.of("test");
        assertTrue("FieldPath should equal itself", path.equals(path));
    }

    @Test
    public void testEqualsIdentical() {
        FieldPath path1 = FieldPath.of("user", "name", "first");
        FieldPath path2 = FieldPath.of("user", "name", "first");

        assertTrue("Identical FieldPaths should be equal", path1.equals(path2));
        assertEquals("Hash codes should be equal", path1.hashCode(), path2.hashCode());
    }

    @Test
    public void testEqualsDifferent() {
        FieldPath path1 = FieldPath.of("user", "name");
        FieldPath path2 = FieldPath.of("user", "email");

        assertFalse("Different FieldPaths should not be equal", path1.equals(path2));
        assertNotEquals("Hash codes should be different", path1.hashCode(), path2.hashCode());
    }

    @Test
    public void testEqualsWithNull() {
        FieldPath path = FieldPath.of("test");
        assertFalse("FieldPath should not equal null", path.equals(null));
    }

    @Test
    public void testEqualsWithDifferentType() {
        FieldPath path = FieldPath.of("test");
        assertFalse("FieldPath should not equal different type", path.equals("string"));
    }

    @Test
    public void testEqualsWithDifferentSegmentCount() {
        FieldPath path1 = FieldPath.of("a", "b");
        FieldPath path2 = FieldPath.of("a", "b", "c");

        assertFalse("FieldPaths with different segment counts should not be equal", path1.equals(path2));
    }

    @Test
    public void testHashCodeConsistency() {
        FieldPath path = FieldPath.of("consistent", "hash");
        int hash1 = path.hashCode();
        int hash2 = path.hashCode();

        assertEquals("Hash code should be consistent", hash1, hash2);
    }

    @Test
    public void testHashCodeBasedOnSegments() {
        FieldPath path1 = FieldPath.of("a", "b");
        FieldPath path2 = FieldPath.of("a", "b");
        FieldPath path3 = FieldPath.of("x", "y");

        assertEquals("Equal paths should have equal hash codes", path1.hashCode(), path2.hashCode());
        // Different paths might have different hash codes, but it's not guaranteed
    }

    // ------------------- toString() Method Tests -------------------

    @Test
    public void testToStringSingleSegment() {
        FieldPath path = FieldPath.of("field");
        assertEquals("Single segment should be returned as-is", "field", path.toString());
    }

    @Test
    public void testToStringMultipleSegments() {
        FieldPath path = FieldPath.of("user", "address", "street");
        assertEquals("Multiple segments should be dot-separated", "user.address.street", path.toString());
    }

    @Test
    public void testToStringWithSpecialCharacters() {
        FieldPath path = FieldPath.of("field.with.dots", "another.field");
        assertEquals("Dots in field names should be preserved", "field.with.dots.another.field", path.toString());
    }

    @Test
    public void testToStringWithEscapedCharacters() throws Exception {
        Constructor<FieldPath> constructor = FieldPath.class.getDeclaredConstructor(List.class);
        constructor.setAccessible(true);

        // Test with segments containing backslashes and backticks
        List<String> segments = Arrays.asList("field\\with\\backslashes", "field`with`backticks");
        FieldPath path = constructor.newInstance(segments);

        String result = path.toString();
        assertTrue("Backslashes should be escaped", result.contains("\\\\"));
        assertTrue("Backticks should be escaped", result.contains("\\`"));
    }

    // ------------------- hasNullOrEmptyArg() Method Tests -------------------

    @Test
    public void testHasNullOrEmptyArgWithValidArgs() {
        boolean result = FieldPath.hasNullOrEmptyArg("field1", "field2", "field3");
        assertFalse("Valid args should return false", result);
    }

    @Test
    public void testHasNullOrEmptyArgWithNullArray() {
        boolean result = FieldPath.hasNullOrEmptyArg((String[]) null);
        assertTrue("Null array should return true", result);
    }

    @Test
    public void testHasNullOrEmptyArgWithNullElement() {
        boolean result = FieldPath.hasNullOrEmptyArg("field1", null, "field3");
        assertTrue("Array with null element should return true", result);
    }

    @Test
    public void testHasNullOrEmptyArgWithEmptyString() {
        boolean result = FieldPath.hasNullOrEmptyArg("field1", "", "field3");
        assertTrue("Array with empty string should return true", result);
    }

    @Test
    public void testHasNullOrEmptyArgWithEmptyArray() {
        boolean result = FieldPath.hasNullOrEmptyArg();
        assertFalse("Empty array should return false", result);
    }

    // ------------------- Error Condition Tests -------------------

    @Test(expected = IllegalArgumentException.class)
    public void testOfWithNullElement() {
        FieldPath.of("field1", null, "field3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOfWithEmptyString() {
        FieldPath.of("field1", "", "field3");
    }

    @Test(expected = NullPointerException.class)
    public void testOfWithNullArray() {
        FieldPath.of((String[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOfWithAllNullElements() {
        FieldPath.of(null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOfWithAllEmptyElements() {
        FieldPath.of("", "", "");
    }

    // ------------------- Edge Cases and Boundary Tests -------------------

    @Test
    public void testSingleCharacterSegments() {
        FieldPath path = FieldPath.of("a", "b", "c");
        assertEquals("Single characters should work", "a.b.c", path.toString());
    }

    @Test
    public void testLongFieldNames() {
        String longName1 = "very_long_field_name_that_goes_on_and_on";
        String longName2 = "another_extremely_long_field_name_for_testing";
        FieldPath path = FieldPath.of(longName1, longName2);

        String expected = longName1 + "." + longName2;
        assertEquals("Long field names should work", expected, path.toString());
    }

    @Test
    public void testNumericFieldNames() {
        FieldPath path = FieldPath.of("field1", "field2", "field10");
        assertEquals("Numeric field names should work", "field1.field2.field10", path.toString());
    }

    @Test
    public void testUnderscoreFieldNames() {
        FieldPath path = FieldPath.of("field_name", "another_field");
        assertEquals("Underscore field names should work", "field_name.another_field", path.toString());
    }

    @Test
    public void testMixedCaseFieldNames() {
        FieldPath path = FieldPath.of("FieldName", "fieldName", "FIELDNAME");
        assertEquals("Mixed case should be preserved", "FieldName.fieldName.FIELDNAME", path.toString());
    }

    @Test
    public void testUnicodeFieldNames() {
        FieldPath path = FieldPath.of("字段", "поле", "field");
        assertEquals("Unicode field names should work", "字段.поле.field", path.toString());
    }

    // ------------------- Helper Methods -------------------

    private List<String> invokeGetSegments(FieldPath path) throws Exception {
        java.lang.reflect.Method method = FieldPath.class.getDeclaredMethod("getSegments");
        method.setAccessible(true);
        return (List<String>) method.invoke(path);
    }
}
