/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import org.apache.commons.lang3.CharUtils;
import org.parboiled.common.Preconditions;

public class IndentUtil {
    public static String[] splitLines(String text) {
        Preconditions.checkArgNotNull(text, "text");
        return text.split("\\r?\\n");
    }

    public static String calcXmlIndent(String[] lines, int row, int column) {
        Preconditions.checkArgNotNull(lines, "lines");
        Preconditions.checkArgument(lines.length > row, "The parameter 'row' overflows.");
        final String s = lines[row];
        Preconditions.checkArgument(s != null, "Encounter null on row: " + row);
        Preconditions.checkArgument(s.length() >= column, "The parameter 'column' overflows");

        final StringBuilder b = new StringBuilder();
        int pos = column;
        while (pos >= 0 && s.charAt(pos--) != '<') {
            // empty
        }

        for (int i = 0; i <= pos; i++) {
            if (s.charAt(i) == '\t') {
                b.append('\t');
            } else {
                b.append(' ');
            }
        }
        return b.toString();
    }

    public static boolean isCrLf(char ch) {
        return ch == CharUtils.CR || ch == CharUtils.LF;
    }

    private IndentUtil() {

    }
}
