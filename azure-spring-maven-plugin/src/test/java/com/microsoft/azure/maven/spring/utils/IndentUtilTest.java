/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class IndentUtilTest {
    @Test
    public void testSplitLines() {
        final String[] lines = IndentUtil.splitLines("foo \n bar \n baz");
        assertEquals(3, lines.length);
        assertEquals("foo ", lines[0]);
        assertEquals(" bar ", lines[1]);
        assertEquals(" baz", lines[2]);
    }

    @Test
    public void testSplitLinesCRLF() {
        final String[] lines = IndentUtil.splitLines("foo \r\n bar \r\n baz");
        assertEquals(3, lines.length);
        assertEquals("foo ", lines[0]);
        assertEquals(" bar ", lines[1]);
        assertEquals(" baz", lines[2]);
    }

    @Test
    public void testSplitLinesMixtureCRLF() {
        final String[] lines = IndentUtil.splitLines("foo \n bar \r\n baz");
        assertEquals(3, lines.length);
        assertEquals("foo ", lines[0]);
        assertEquals(" bar ", lines[1]);
        assertEquals(" baz", lines[2]);
    }

    @Test
    public void testSplitLinesNull() {
        try {
            IndentUtil.splitLines(null);
            fail("Should throw NPE");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    @Test
    public void testSplitLinesEmpty() {
        assertEquals(1, IndentUtil.splitLines("").length);
        assertEquals(1, IndentUtil.splitLines("   ").length);
    }

    @Test
    public void testXmlIndent() {
        final String[] lines = IndentUtil.splitLines("<foo> \n <bar> \n        \t<test></test></bar> \r\n </foo>");
        assertEquals(" ", IndentUtil.calcXmlIndent(lines, 1, 2));
        assertEquals("        \t", IndentUtil.calcXmlIndent(lines, 2, 12));
    }

    @Test
    public void testXmlIndentBadArgument() {
        final String[] lines = IndentUtil.splitLines("<foo> \n <bar> \n        \t<test></test></bar> \r\n </foo>");
        try {
            IndentUtil.calcXmlIndent(lines, 100, 1);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            IndentUtil.calcXmlIndent(lines, -1, 1);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            IndentUtil.calcXmlIndent(lines, 0, -1);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        try {
            IndentUtil.calcXmlIndent(lines, 0, 1000);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            IndentUtil.calcXmlIndent(null, 1, 1000);
            fail("Should throw NPE");
        } catch (NullPointerException ex) {
            // expected
        }

        try {
            IndentUtil.calcXmlIndent(new String[] { null }, 0, 1000);
            fail("Should throw IAE");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }
}
