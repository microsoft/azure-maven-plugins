/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.maven.springcloud.prompt;

import com.microsoft.azure.common.prompt.IPrompter;

import com.microsoft.azure.maven.springcloud.config.ConfigurationPrompter;
import com.microsoft.azure.tools.exception.InvalidConfigurationException;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

public class ConfigurationPrompterTest {
    private ConfigurationPrompter wrapper;
    private BufferedReader reader;
    private Log mockLog;
    private ExpressionEvaluator mockEval;

    @Before
    public void setup() throws Exception {
        mockLog = mock(Log.class);
        mockEval = mock(ExpressionEvaluator.class);
        wrapper = new ConfigurationPrompter(mockEval, mockLog);

        wrapper.initialize();
        final Object prompt = FieldUtils.readField(wrapper, "prompt", true);

        reader = mock(BufferedReader.class);
        FieldUtils.writeField(prompt, "reader", reader, true);
    }

    @Test
    public void testCtor() {
        wrapper = new ConfigurationPrompter(mockEval, mockLog);
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

        when(reader.readLine()).thenReturn("abcd");
        result = wrapper.handle("testId1", false);
        assertEquals("abcd", result);

        when(reader.readLine()).thenReturn("^^&$").thenReturn("wxyz");
        result = wrapper.handle("testId1", false, null);
        assertEquals("wxyz", result);

        when(reader.readLine()).thenReturn("${expr}");
        when(mockEval.evaluate("${expr}")).thenReturn("evaluated");
        result = wrapper.handle("testId1", false);
        assertEquals("${expr}", result);

        when(reader.readLine()).thenReturn("${expr1}").thenReturn("${expr").thenReturn("${expr}");
        when(mockEval.evaluate("${expr}")).thenReturn("evaluated");
        when(mockEval.evaluate("${expr1}")).thenReturn("evaluated~!!");
        when(mockEval.evaluate("${expr")).thenThrow(new ExpressionEvaluationException("bad expr"));

        result = wrapper.handle("testId1", false);
        assertEquals("${expr}", result);
    }

    @Test
    public void testEvaluateDefault() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the {{global_property1}} value(***{{default}}***):"),
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

        result = wrapper.handle("testId1", true, "abcd");
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
    public void testHandleBadConfiguration() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the {{global_property1}} value(***{{default}}***):"),
                    new DefaultMapEntry<>("default", "false"),
                    new DefaultMapEntry<>("required", false), });
        templates.put("testId1", map);

        map.put("property", "");

        try {
            wrapper.handle("testId1", true, "foo");
            fail("Should throw InvalidConfigurationException");
        } catch (InvalidConfigurationException ex) {
            // expected
        }

        map.put("property", "appName");

        try {
            wrapper.handle("testId1", true, "foo");
            fail("Should throw InvalidConfigurationException");
        } catch (InvalidConfigurationException ex) {
            // expected
        }

        map.put("resource", "");
        try {
            wrapper.handle("testId1", true, "foo");
            fail("Should throw InvalidConfigurationException");
        } catch (InvalidConfigurationException ex) {
            // expected
        }

        map.put("resource", "App");

        wrapper.handle("testId1", true, "foo");
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
            fail("Should report error when required resources are not available.");
        } catch (InvalidConfigurationException ex) {
            // expected
        }
        map.put("required", false);
        assertNull(wrapper.handleSelectOne("testId1", Collections.emptyList(), null, String::toString));

        map.put("auto_select", true);
        result = wrapper.handleSelectOne("testId1", Collections.singletonList("foo"), null, String::toString);
        assertEquals("foo", result);
    }

    @Test
    public void testHandleSelectOnePromoteYesNo() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the {{global_property1}} value(***{{default}}***):"),
                    new DefaultMapEntry<>("resource", "App"), new DefaultMapEntry<>("default", "false"),
                    new DefaultMapEntry<>("property", "isPublic"), new DefaultMapEntry<>("required", false), });
        templates.put("testId1", map);
        when(reader.readLine()).thenReturn("Y").thenReturn("N");
        String result = wrapper.handleSelectOne("testId1", Collections.singletonList("foo"), null, String::toString);
        assertEquals("foo", result);
        result = wrapper.handleSelectOne("testId1", Collections.singletonList("foo"), null, String::toString);
        assertNull(result);
        map.put("required", true);

        try {
            wrapper.handleSelectOne("testId1", Collections.singletonList("foo"), null, String::toString);
            fail("Should throw SpringConfigurationException.");
        } catch (InvalidConfigurationException ex) {
            // expected
        }

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
            fail("Should report error when required resources are not available.");
        } catch (InvalidConfigurationException ex) {
            // expected
        }

        map.put("allow_empty", true);
        assertTrue(wrapper.handleMultipleCase("testId1", Collections.emptyList(), String::toString).isEmpty());
    }

    @Test
    public void testPromoteYesNoForOneOption() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the {{global_property1}} value(***{{default}}***):"),
                    new DefaultMapEntry<>("resource", "App"), new DefaultMapEntry<>("default", "false"),
                    new DefaultMapEntry<>("property", "isPublic"), new DefaultMapEntry<>("required", true), });
        templates.put("testId1", map);
        when(reader.readLine()).thenReturn("Y");
        List<String> result = wrapper.handleMultipleCase("testId1", Collections.singletonList("foo"), String::toString);
        assertEquals("foo", StringUtils.join(result.toArray(), ','));

        when(reader.readLine()).thenReturn("N");
        result = wrapper.handleMultipleCase("testId1", Collections.singletonList("foo"), String::toString);
        assertTrue(result.isEmpty());

        when(reader.readLine()).thenReturn("");

        map.put("auto_select", true);
        map.put("default_selected", false);
        result = wrapper.handleMultipleCase("testId1", Collections.singletonList("foo"), String::toString);
        assertEquals("foo", StringUtils.join(result.toArray(), ','));

        map.put("auto_select", false);
        map.put("default_selected", true);
        result = wrapper.handleMultipleCase("testId1", Collections.singletonList("foo"), String::toString);
        assertEquals("foo", StringUtils.join(result.toArray(), ','));

        map.put("default_selected", false);
        map.put("allow_empty", true);
        when(reader.readLine()).thenReturn("10-10000");
        result = wrapper.handleMultipleCase("testId1", Arrays.asList("foo", "bar"), String::toString);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMavenEvaluationError() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the {{global_property1}} value(***{{default}}***):"),
                    new DefaultMapEntry<>("resource", "App"), new DefaultMapEntry<>("default", "defaultValueForUnitTest"),
                    new DefaultMapEntry<>("property", "appName"), new DefaultMapEntry<>("required", true), });
        templates.put("testId1", map);
        wrapper.putCommonVariable("global_property1", "value1");
        when(mockEval.evaluate("${expr}")).thenReturn(null);
        when(reader.readLine()).thenReturn("${expr}").thenReturn("abcd");
        final String result = wrapper.handle("testId1", false);
        assertEquals("abcd", result);
    }

    @Test
    public void testConfirmChanges() throws Exception {
        final Map<String, String> changesToConfirm = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("foo", "bar"),
                    new DefaultMapEntry<>("count", "1"),
                    new DefaultMapEntry<>("blank", ""),
                    new DefaultMapEntry<>("update", "true"), });
        final List<MavenProject> projects = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final MavenProject proj = mock(MavenProject.class);
            Mockito.when(proj.getFile()).thenReturn(new File("test" + (i + 1)));
            projects.add(proj);
        }
        when(reader.readLine()).thenReturn("");
        wrapper.putCommonVariable("projects", projects);
        wrapper.confirmChanges(changesToConfirm, () -> 10);
        wrapper.confirmChanges(changesToConfirm, () -> null);
        wrapper.confirmChanges(changesToConfirm, () -> 1);
        when(reader.readLine()).thenReturn("N");
        wrapper.confirmChanges(changesToConfirm, () -> {
            throw new RuntimeException("This function will never be called.");
        });
    }

    @Test
    public void testValidateDefaultValue() throws Exception {
        final Map<String, Map<String, Object>> templates = (Map<String, Map<String, Object>>) FieldUtils.readField(wrapper, "templates", true);
        final Map<String, Object> map = MapUtils.putAll(new LinkedHashMap<>(),
                new Map.Entry[] { new DefaultMapEntry<>("id", "testId1"),
                    new DefaultMapEntry<>("promote", "Input the value(***{{default}}***):"),
                    new DefaultMapEntry<>("resource", "App"), new DefaultMapEntry<>("default", "!@#*$(*~"),
                    new DefaultMapEntry<>("property", "appName"), new DefaultMapEntry<>("required", true), });
        wrapper.putCommonVariable("global_property1", "Value1");
        templates.put("testId1", map);
        try {
            wrapper.handle("testId1", true);
            fail("Should throw exception when default value cannot pass validation.");
        } catch (InvalidConfigurationException ex) {
            // expected
        }
        map.put("default", "{{global_property1|lower}}");
        assertEquals("value1", wrapper.handle("testId1", true));

        map.put("default", null);
        assertNull(wrapper.handle("testId1", true));
    }

    @Test
    public void testClose() throws Exception {
        final IPrompter prompt = mock(IPrompter.class);
        Mockito.doThrow(IOException.class).when(prompt).close();
        FieldUtils.writeField(wrapper, "prompt", prompt, true);
        try {
            wrapper.close();
            fail("Should throw IOException");
        } catch (IOException ex) {
            // expected
        }
        Mockito.verify(prompt);
        prompt.close();
    }
}
