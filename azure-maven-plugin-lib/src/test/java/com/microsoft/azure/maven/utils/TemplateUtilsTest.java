/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.utils;

import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;

import org.jtwig.parser.ParseException;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.maven.utils.TemplateUtils.evalBoolean;
import static com.microsoft.azure.maven.utils.TemplateUtils.evalPlainText;
import static com.microsoft.azure.maven.utils.TemplateUtils.evalText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TemplateUtilsTest {

    @Test
    public void testEvalBoolean() {
        assertEquals(Boolean.TRUE, evalBoolean("foo", Collections.singletonMap("foo", "true")));
        assertEquals(Boolean.TRUE, evalBoolean("foo", Collections.singletonMap("foo", true)));
        assertEquals(Boolean.FALSE, evalBoolean("foo", Collections.singletonMap("foo", null)));
    }

    @Test
    public void testEvalBooleanVariable() {
        final Map<String, Object> map = new HashMap<>();
        map.put("foo", "{{bar}}");
        map.put("bar", "true");
        assertEquals(Boolean.TRUE, evalBoolean("foo", map));
    }

    @Test
    public void testEvalBooleanRecursive() {
        final Map<String, Object> map = new HashMap<>();
        map.put("foo", Collections.singletonMap("bar", "true"));
        assertEquals(Boolean.TRUE, evalBoolean("foo.bar", map));
    }

    @Test
    public void testEvalPlainText() {
        final Map<String, Object> map = new HashMap<>();
        map.put("foo", Collections.singletonMap("bar", "true"));
        assertEquals("true", evalPlainText("foo.bar", map));

        map.put("foo", Collections.singletonMap("bar", true));
        assertEquals("true", evalPlainText("foo.bar", map));
    }

    @Test
    public void testEvalPlainTextVariable() {
        final Map<String, Object> map = new HashMap<>();
        map.put("foo", "{{bar}}");
        map.put("bar", "hello world");
        assertEquals("hello world", evalPlainText("foo", map));
    }

    @Test
    public void testEvalText() {
        final Map<String, Object> map = new HashMap<>();
        map.put("foo", "hello ***{{name}}***");
        map.put("name", "Jack");
        assertEquals(String.format("hello %s", TextUtils.blue("Jack")), evalText("foo", map));
    }

    @Test
    public void testEndlessEval() {
        final Map<String, Object> map = new HashMap<>();
        map.put("foo", "hello {{name}}");
        map.put("name", "{{foo}}");
        assertEquals("hello hello hello hello {{name}}", evalPlainText("foo", map));
    }

    @Test
    public void testBadExpression() {
        final Map<String, Object> map = new HashMap<>();
        map.put("foo", "{{bar}");
        map.put("bar", "hello world");
        try {
            evalPlainText("foo", map);
            fail("Should report error when evaluate text is not valid.");
        } catch (ParseException ex) {
            // expected
        }

    }
}
