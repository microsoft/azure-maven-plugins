/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import org.junit.Test;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("unchecked")
public class JsonUtilsTest {

    private final String testString = "{\"id\":1,\"title\":\"hello\",\"children\":[{\"id\":2}]}";

    @Test
    public void testFromJson() {
        final String testFailure = "{{{{{{{{{{{";
        Object obj = null;

        try {
            obj = JsonUtils.fromJson(testString, Map.class);
        } catch (final Exception e) {
            fail("Fail to parse a json to java.util.Map.");
        }

        assertTrue(((Map<String, Object>) obj).containsKey("id"));
        assertTrue(((Map<String, Object>) obj).get("id") instanceof Number);
        try {
            obj = JsonUtils.fromJson(testString, ObjectForJson.class);
        } catch (final Exception e) {
            fail("Fail to parse a json to java.util.Map.");
        }
        final ObjectForJson objForJson = (ObjectForJson) obj;
        assertEquals(1, objForJson.id);
        assertEquals("hello", objForJson.title);
        assertEquals(1, objForJson.children.length);
        assertEquals(2, objForJson.children[0].id);
        try {
            obj = JsonUtils.fromJson(testFailure, Map.class);
            fail("Should fail for invalid json.");
        } catch (final Exception e) {
            // ignore
        }
    }

    @Test
    public void testToJson() {
        final ObjectForJson objForJson = JsonUtils.fromJson(testString, ObjectForJson.class);
        final String json = JsonUtils.toJson(objForJson);
        assertEquals(testString, json);
    }

    static class ObjectForJson {
        public int id;
        private String title;
        protected ObjectForJson[] children;
    }
}
