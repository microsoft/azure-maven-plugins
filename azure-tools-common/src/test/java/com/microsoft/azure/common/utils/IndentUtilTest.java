/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class IndentUtilTest {

    @Test
    public void testXmlIndent() {
        final String[] lines = TextUtils.splitLines("<foo> \n <bar> \n        \t<test></test></bar> \r\n </foo>");
        assertEquals(" ", IndentUtil.calcXmlIndent(lines, 1, 2));
        assertEquals("        \t", IndentUtil.calcXmlIndent(lines, 2, 12));
    }

    @Test
    public void testXmlIndentBadArgument() {
        final String[] lines = TextUtils.splitLines("<foo> \n <bar> \n        \t<test></test></bar> \r\n </foo>");
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
