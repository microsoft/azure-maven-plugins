/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.spring.prompt;

import com.microsoft.azure.maven.spring.exception.NoResourcesAvailableException;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class PromptWrapperTest {
    private PromptWrapper wrapper;
    private BufferedReader reader;
    private Log mockLog;
    private ExpressionEvaluator mockEval;

    @Before
    public void setup() throws Exception {
        mockLog = mock(Log.class);
        mockEval = mock(ExpressionEvaluator.class);
        wrapper = new PromptWrapper(mockEval, mockLog);

        wrapper.initialize();
        final Object prompt = FieldUtils.readField(wrapper, "prompt", true);

        reader = mock(BufferedReader.class);
        FieldUtils.writeField(prompt, "reader", reader, true);
    }

    @Test
    public void testCtor() {
        wrapper = new PromptWrapper(mockEval, mockLog);
        assertNotNull(wrapper);
    }

    @Test
    public void testHandle() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the {{global_property1}} value(***{{default}}***):"),
                    new DefaultMapEntry<>("resource", "App"), new DefaultMapEntry<>("default", "defaultValueForUnitTest"),
                    new DefaultMapEntry<>("property", "appName"), new DefaultMapEntry<>("required", true), });
        templates.put("testId1", map);
        wrapper.putCommonVariable("global_property1", "value1");

        // test for default value;
        when(reader.readLine()).thenReturn("");
        String result = wrapper.handle("testId1", false);
        assertEquals("defaultValueForUnitTest", result);

        when(reader.readLine()).thenReturn("abc");
        result = wrapper.handle("testId1", false);
        assertEquals("abc", result);

        when(reader.readLine()).thenReturn("^^&$").thenReturn("xyz");
        result = wrapper.handle("testId1", false, null);
        assertEquals("xyz", result);

        when(reader.readLine()).thenReturn("${expr}");
        when(mockEval.evaluate("${expr}")).thenReturn("evalutedname");
        result = wrapper.handle("testId1", false);
        assertEquals("${expr}", result);

        when(reader.readLine()).thenReturn("${expr1}").thenReturn("${expr").thenReturn("${expr}");
        when(mockEval.evaluate("${expr}")).thenReturn("evalutedname");
        when(mockEval.evaluate("${expr1}")).thenReturn("badappname~!!");
        when(mockEval.evaluate("${expr")).thenThrow(new ExpressionEvaluationException("bad expr"));

        result = wrapper.handle("testId1", false);
        assertEquals("${expr}", result);
    }

    @Test
    public void testEvaluteDefault() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the {{global_property1}} value(***{{evaluatedDefault}}***):"),
                    new DefaultMapEntry<>("resource", "App"), new DefaultMapEntry<>("default", "${public}"),
                    new DefaultMapEntry<>("property", "isPublic"), new DefaultMapEntry<>("required", true), });
        templates.put("testId1", map);
        when(mockEval.evaluate("${public}")).thenReturn("true");
        // test for default value;
        when(reader.readLine()).thenReturn("");
        assertEquals("${public}", wrapper.handle("testId1", false));
    }

    @Test
    public void testHandleBoolean() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the {{global_property1}} value(***{{default}}***):"),
                    new DefaultMapEntry<>("resource", "App"), new DefaultMapEntry<>("default", "true"),
                    new DefaultMapEntry<>("property", "isPublic"), new DefaultMapEntry<>("required", true), });
        templates.put("testId1", map);
        wrapper.putCommonVariable("global_property1", "value1");

        // test for default value;
        when(reader.readLine()).thenReturn("y").thenReturn("n").thenReturn("Y");
        String result = wrapper.handle("testId1", false);
        assertEquals("true", result);

        result = wrapper.handle("testId1", false);
        assertEquals("false", result);

        result = wrapper.handle("testId1", false);
        assertEquals("true", result);

    }

    @Test
    public void testHandleAutoApplyDefault() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"), new DefaultMapEntry<>("resource", "Deployment"),
                    new DefaultMapEntry<>("property", "cpu"), new DefaultMapEntry<>("required", true), });
        templates.put("testId1", map);
        wrapper.putCommonVariable("global_property1", "value1");

        // test for default value;
        String result = wrapper.handle("testId1", true);
        assertEquals("1", result);

        result = wrapper.handle("testId1", true, "2");
        assertEquals("2", result);

        result = wrapper.handle("testId1", true, "abc");
        assertEquals("1", result);

    }

    @Test
    public void testBadTemplate() throws Exception {
        try {
            wrapper.handle("", true);
            fail("Should report IAE when template cannot be found.");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            wrapper.handle("badId", true);
            fail("Should report IAE when template cannot be found.");
        } catch (IllegalArgumentException e) {
            // expected
        }

        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"), new DefaultMapEntry<>("resource", "ResourceNotFound"),
                    new DefaultMapEntry<>("property", "cpu"), new DefaultMapEntry<>("required", true), });
        templates.put("testId1", map);
        try {
            wrapper.handle("testId1", true);
            fail("Should report IAE when resource cannot be found.");
        } catch (IllegalArgumentException e) {
            // expected
        }

        map = MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
            new DefaultMapEntry<>("resource", "App"), new DefaultMapEntry<>("property", "cpu2"), new DefaultMapEntry<>("required", true), });
        templates.put("testId1", map);
        try {
            wrapper.handle("testId1", true);
            fail("Should report IAE when property cannot be found.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testHandleSelectOne() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the {{global_property1}} value(***{{default}}***):"),
                    new DefaultMapEntry<>("resource", "App"), new DefaultMapEntry<>("default", "false"),
                    new DefaultMapEntry<>("property", "isPublic"), new DefaultMapEntry<>("required", true), });
        templates.put("testId1", map);
        // test for default value;
        when(reader.readLine()).thenReturn("");
        String result = wrapper.handleSelectOne("testId1", Arrays.asList("foo", "bar"), "foo", String::toString);
        assertEquals("foo", result);

        map.put("required", false);
        result = wrapper.handleSelectOne("testId1", Arrays.asList("foo", "bar"), null, String::toString);
        assertNull(result);

        map.put("required", true);
        when(reader.readLine()).thenReturn("").thenReturn("1").thenReturn("2").thenReturn("3").thenReturn("2");
        result = wrapper.handleSelectOne("testId1", Arrays.asList("foo", "bar"), null, String::toString);
        assertEquals("foo", result);
        result = wrapper.handleSelectOne("testId1", Arrays.asList("foo", "bar"), null, String::toString);
        assertEquals("bar", result);

        result = wrapper.handleSelectOne("testId1", Arrays.asList("foo", "bar"), null, String::toString);
        assertEquals("bar", result);

        map.put("required", true);
        map.put("message", Collections.singletonMap("empty_options", "Option is empty"));
        try {
            wrapper.handleSelectOne("testId1", Collections.emptyList(), null, String::toString);
            fail("Should report error when requried resources are not available.");
        } catch (NoResourcesAvailableException ex) {
            // expected
        }
        map.put("required", false);
        assertNull(wrapper.handleSelectOne("testId1", Collections.emptyList(), null, String::toString));

        map.put("auto_select", true);
        result = wrapper.handleSelectOne("testId1", Collections.singletonList("foo"), null, String::toString);
        assertEquals("foo", result);
    }

    @Test
    public void testHandleSelectMany() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the {{global_property1}} value(***{{default}}***):"),
                    new DefaultMapEntry<>("resource", "App"), new DefaultMapEntry<>("default", "false"),
                    new DefaultMapEntry<>("property", "isPublic"), new DefaultMapEntry<>("required", true), });
        templates.put("testId1", map);
        when(reader.readLine()).thenReturn("1");
        List<String> result = wrapper.handleMultipleCase("testId1", Arrays.asList("foo", "bar"), String::toString);
        assertEquals("foo", StringUtils.join(result.toArray(), ','));

        when(reader.readLine()).thenReturn("").thenReturn("100").thenReturn("1-2");
        result = wrapper.handleMultipleCase("testId1", Arrays.asList("bar", "foo"), String::toString);
        assertEquals("bar,foo", StringUtils.join(result.toArray(), ','));

        map.put("allow_empty", false);
        map.put("message", Collections.singletonMap("empty_options", "Option is empty"));
        try {
            wrapper.handleMultipleCase("testId1", Collections.emptyList(), String::toString);
            fail("Should report error when requried resources are not available.");
        } catch (NoResourcesAvailableException ex) {
            // expected
        }

        map.put("allow_empty", true);
        assertTrue(wrapper.handleMultipleCase("testId1", Collections.emptyList(), String::toString).isEmpty());
    }
}
