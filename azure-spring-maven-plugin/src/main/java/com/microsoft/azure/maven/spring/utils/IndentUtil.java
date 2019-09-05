/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import org.parboiled.common.Preconditions;

public class IndentUtil {
    public static String[] splitLines(String text) {
        Preconditions.checkArgNotNull(text, "text");
        return text.split("\\r?\\n");
    }

    public static String calcXmlIndent(String[] lines, int row, int column) {
        Preconditions.checkArgNotNull(lines, "lines");
        Preconditions.checkArgument(lines.length > row && row >= 0, "The parameter 'row' overflows.");
        final String line = lines[row];
        Preconditions.checkArgument(line != null, "Encounter null on row: " + row);
        Preconditions.checkArgument(line.length() >= column && column >= 0, "The parameter 'column' overflows");

        final StringBuilder buffer = new StringBuilder();
        final int pos = line.lastIndexOf('<', column) - 1; // skip the current tag like : <tag>
        for (int i = 0; i <= pos; i++) {
            if (line.charAt(i) == '\t') {
                buffer.append('\t');
            } else {
                buffer.append(' ');
            }
        }
        return buffer.toString();
    }

    private IndentUtil() {

    }
}
