/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.prompt;

import com.microsoft.azure.common.utils.SneakyThrowUtils;
import com.microsoft.azure.maven.TestHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultPrompterTest {
    private final BufferedReader reader = mock(BufferedReader.class);
    private final DefaultPrompter prompter = new DefaultPrompter();

    @Before
    public void setup() throws IllegalAccessException {
        FieldUtils.writeField(prompter, "reader", reader, true);
    }

    @Test
    public void testPromoteStringRequired() throws Exception {
        when(reader.readLine()).thenReturn("bar");
        String result = prompter.promoteString("Please input a string", "foo", input -> {
            if (StringUtils.equals("bar", input)) {
                // cannot input bar, set 10 to next call;
                try {
                    when(reader.readLine()).thenReturn(" 10 ");
                } catch (IOException e) {
                    SneakyThrowUtils.sneakyThrow(e);
                }
                return InputValidateResult.error("cannot input bar");
            }
            return InputValidateResult.wrap(input.trim());
        }, true);
        assertEquals("10", result);

        when(reader.readLine()).thenReturn("");

        result = prompter.promoteString("Please input a string", "foo", input -> {
            throw new RuntimeException();
        }, true);
        assertEquals("foo", result);

        when(reader.readLine()).thenReturn("").thenReturn("").thenReturn("a");
        result = prompter.promoteString("Please input a string", null, InputValidateResult::wrap, true);
        assertEquals("a", result);
    }

    @Test
    public void testPromoteStringNotRequired() throws Exception {
        when(reader.readLine()).thenReturn("");
        String result = prompter.promoteString("Please input a string", "foo", input -> {
            throw new RuntimeException();
        }, false);
        assertEquals("foo", result);

        result = prompter.promoteString("Please input a string", null, input -> {
            throw new RuntimeException();
        }, false);
        assertNull(result);
    }

    @Test
    public void testPromoteStringEmpty() throws Exception {
        when(reader.readLine()).thenReturn(":");
        final String result = prompter.promoteString("Please input a string", "foo", input -> {
            throw new RuntimeException();
        }, false);
        assertEquals("", result);
    }

    @Test
    public void testPromoteYesNo() throws Exception {
        when(reader.readLine()).thenReturn("Y").thenReturn("y").thenReturn("n");
        Boolean result = prompter.promoteYesNo("Do you want to continue(y/n)", null, true);
        assertNotNull(result);
        assertTrue(result);

        result = prompter.promoteYesNo("Do you want to continue(y/n)", null, true);
        assertNotNull(result);
        assertTrue(result);

        result = prompter.promoteYesNo("Do you want to continue(y/n)", null, true);
        assertNotNull(result);
        assertFalse(result);

        when(reader.readLine()).thenReturn("").thenReturn("");
        result = prompter.promoteYesNo("Do you want to continue(Y/n)", true, false);
        assertNotNull(result);
        assertTrue(result);

        result = prompter.promoteYesNo("Do you want to continue(Y/n)", false, false);
        assertNotNull(result);
        assertFalse(result);
    }

    @Test
    public void testPromoteYesNoBadInput() throws Exception {
        when(reader.readLine()).thenReturn("foo").thenReturn("bar").thenReturn("Y");
        final Boolean result = prompter.promoteYesNo("Do you want to continue(Y/n)", null, true);
        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    public void testPromoteMultipleEntities() throws Exception {
        when(reader.readLine()).thenReturn("1").thenReturn("1-2").thenReturn("1-2,3-5").thenReturn("3-1000000,1-2,3-5");
        final List<Integer> integers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            integers.add(i);
        }
        List<Integer> selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", integers, t -> t.toString(), false,
                "to select none", Collections.emptyList());

        Assert.assertEquals("0", TestHelper.joinIntegers(selected));
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", integers, t -> t.toString(), false,
                "to select none", Collections.emptyList());
        Assert.assertEquals("0,1", TestHelper.joinIntegers(selected));
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", integers, t -> t.toString(), false,
                "to select none", Collections.emptyList());
        Assert.assertEquals("0,1,2,3,4", TestHelper.joinIntegers(selected));

        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", integers, t -> t.toString(), false,
                "to select none", Collections.emptyList());
        Assert.assertEquals("2,3,4,5,6,7,8,9,0,1", TestHelper.joinIntegers(selected));

        when(reader.readLine()).thenReturn("bad value").thenReturn("100").thenReturn("");
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", integers, t -> t.toString(), true,
                "to select none", Collections.emptyList());
        assertTrue(selected.isEmpty());

        when(reader.readLine()).thenReturn("");
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", Collections.singletonList(1), t -> t.toString(), true,
                "to select none", Collections.emptyList());
        assertTrue(selected.isEmpty());

        when(reader.readLine()).thenReturn("");
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
        "You have select no entities", integers, t -> t.toString(), true,
        "to select none", Arrays.asList(4, 5));
        Assert.assertEquals("4,5", TestHelper.joinIntegers(selected));
    }

    @Test
    public void testPromoteMultipleEntitiesAllowEmpty() throws Exception {
        when(reader.readLine()).thenReturn("1").thenReturn("1-2").thenReturn("1-2,3-5").thenReturn("3-1000000,1-2,3-5");
        final List<Integer> integers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            integers.add(i);
        }
        List<Integer> selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", integers, t -> t.toString(), true,
                "to select none", Collections.emptyList());

        Assert.assertEquals("0", TestHelper.joinIntegers(selected));
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", integers, t -> t.toString(), true,
                "to select none", Collections.emptyList());
        Assert.assertEquals("0,1", TestHelper.joinIntegers(selected));
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", integers, t -> t.toString(), true,
                "to select none", Collections.emptyList());
        Assert.assertEquals("0,1,2,3,4", TestHelper.joinIntegers(selected));

        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", integers, t -> t.toString(), true,
                "to select none", Collections.emptyList());
        Assert.assertEquals("2,3,4,5,6,7,8,9,0,1", TestHelper.joinIntegers(selected));

        when(reader.readLine()).thenReturn("bad value").thenReturn("100").thenReturn("");
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", integers, t -> t.toString(), true,
                "to select none", Collections.emptyList());
        assertTrue(selected.isEmpty());

        when(reader.readLine()).thenReturn("");
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", Collections.singletonList(1), t -> t.toString(), true,
                "to select none", Collections.emptyList());
        assertTrue(selected.isEmpty());

        when(reader.readLine()).thenReturn("");
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
        "You have select no entities", integers, t -> t.toString(), true,
        "to select none", Arrays.asList(4, 5));
        Assert.assertEquals("4,5", TestHelper.joinIntegers(selected));
    }

    @Test
    public void testPromoteMultipleEntitiesOnlyOne() throws Exception {
        final List<Integer> selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", Collections.singletonList(100), t -> t.toString(), false,
                "to select none", Collections.emptyList());
        Assert.assertEquals("100", TestHelper.joinIntegers(selected));
    }

    @Test
    public void testPromoteMultipleEntitiesNotAllowEmptyNoDefaultValue() throws Exception {
        when(reader.readLine()).thenReturn("1000-11111").thenReturn("10001111111111111111111111111").thenReturn("2");;
        final List<Integer> selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", Arrays.asList(98, 99, 100), t -> t.toString(), false,
                "to select none", Collections.emptyList());
        Assert.assertEquals("99", TestHelper.joinIntegers(selected));
    }

    @Test
    public void testPromoteMultipleEntitiesSelectNone() throws Exception {
        when(reader.readLine()).thenReturn("");
        List<Integer> selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", Collections.singletonList(100), t -> t.toString(), true,
                "to select none", Collections.emptyList());
        assertEquals(0, selected.size());

        when(reader.readLine()).thenReturn("100000000000000").thenReturn("1");
        selected = prompter.promoteMultipleEntities("This is header", "Please input range",
                "You have select no entities", Collections.singletonList(100), t -> t.toString(), true,
                "to select none", Collections.emptyList());
        Assert.assertEquals("100", TestHelper.joinIntegers(selected));
    }

    @Test
    public void testPromoteSingle() throws Exception {
        when(reader.readLine()).thenReturn("1").thenReturn("10001111111111111111111111111").thenReturn("1000").thenReturn("2").thenReturn("");
        final List<Integer> integers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            integers.add(i);
        }
        Integer value = prompter.promoteSingleEntity("This is header", "Please input index for integer", integers, 2, t -> t.toString(), false);
        assertEquals(Integer.valueOf(1), value);
        value = prompter.promoteSingleEntity("This is header", "Please input index for integer", integers, 2, t -> t.toString(), false);
        assertEquals(Integer.valueOf(2), value);
        value = prompter.promoteSingleEntity("This is header", "Please input index for integer", integers, 2, t -> t.toString(), false);
        assertEquals(Integer.valueOf(2), value);
        when(reader.readLine()).thenReturn("bad number").thenReturn("10").thenReturn("3");
        value = prompter.promoteSingleEntity("This is header", "Please input index for integer", integers, 2, t -> t.toString(), false);
        assertEquals(Integer.valueOf(3), value);
    }

    @Test
    public void testClose() throws Exception {
        doNothing().when(reader).close();
        this.prompter.close();
    }
}
