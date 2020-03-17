/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.utils;
import org.apache.commons.codec.binary.Hex;
import org.fusesource.jansi.Ansi.Color;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class TextUtilsTest {
    @Before
    public void init() {
        org.fusesource.jansi.AnsiConsole.systemInstall();
    }

    @Test
    public void testBlue() {
        System.out.println("This is a " + TextUtils.blue("blue") + " text.");
        assertEquals("1b5b33346d611b5b6d", Hex.encodeHexString(TextUtils.blue("a").getBytes()));
    }

    @Test
    public void testRed() {
        System.out.println("This is a " + TextUtils.red("red") + " text.");
        assertEquals("1b5b33316d611b5b6d", Hex.encodeHexString(TextUtils.red("a").getBytes()));
    }

    @Test
    public void testYellow() {
        System.out.println("This is a " + TextUtils.yellow("yellow") + " text.");
        assertEquals("1b5b33336d611b5b6d", Hex.encodeHexString(TextUtils.yellow("a").getBytes()));
    }

    @Test
    public void testGreen() {
        System.out.println("This is a " + TextUtils.green("green") + " text.");
        assertEquals("1b5b33326d611b5b6d", Hex.encodeHexString(TextUtils.green("a").getBytes()));
    }

    @Test
    public void testApplyColorToText() {
        System.out.println("This is a " + TextUtils.applyColorToText("magenta", Color.MAGENTA) + " text.");
        assertEquals("1b5b33356d611b5b6d", Hex.encodeHexString(TextUtils.applyColorToText("a", Color.MAGENTA).getBytes()));
        assertEquals("  ", TextUtils.applyColorToText("  ", Color.MAGENTA));
        assertNull(TextUtils.applyColorToText(null, Color.MAGENTA));
    }

    @Test
    public void testSplitLines() {
        final String[] lines = TextUtils.splitLines("foo \n bar \n baz");
        assertEquals(3, lines.length);
        assertEquals("foo ", lines[0]);
        assertEquals(" bar ", lines[1]);
        assertEquals(" baz", lines[2]);
    }

    @Test
    public void testSplitLinesCRLF() {
        final String[] lines = TextUtils.splitLines("foo \r\n bar \r\n baz");
        assertEquals(3, lines.length);
        assertEquals("foo ", lines[0]);
        assertEquals(" bar ", lines[1]);
        assertEquals(" baz", lines[2]);
    }

    @Test
    public void testSplitLinesMixtureCRLF() {
        final String[] lines = TextUtils.splitLines("foo \n bar \r\n baz");
        assertEquals(3, lines.length);
        assertEquals("foo ", lines[0]);
        assertEquals(" bar ", lines[1]);
        assertEquals(" baz", lines[2]);
    }

    @Test
    public void testSplitLinesNull() {
        try {
            TextUtils.splitLines(null);
            fail("Should throw NPE");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    @Test
    public void testSplitLinesEmpty() {
        assertEquals(1, TextUtils.splitLines("").length);
        assertEquals(1, TextUtils.splitLines("   ").length);
    }

}
