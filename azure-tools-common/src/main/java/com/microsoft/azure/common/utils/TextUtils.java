/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.utils;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

public class TextUtils {
    public static String applyColorToText(String text, Color colorCode) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        return Ansi.ansi().fg(colorCode).a(text).reset().toString();
    }

    public static String yellow(String message) {
        return applyColorToText(message, Color.YELLOW);
    }

    public static String green(String message) {
        return applyColorToText(message, Color.GREEN);
    }

    public static String blue(String message) {
        return applyColorToText(message, Color.BLUE);
    }

    public static String cyan(String message) {
        return applyColorToText(message, Color.CYAN);
    }

    public static String red(String message) {
        return applyColorToText(message, Color.RED);
    }

    public static String[] splitLines(String text) {
        Preconditions.checkNotNull(text, "The parameter 'text' cannot be null");
        return text.split("\\r?\\n");
    }

    private TextUtils() {

    }
}
