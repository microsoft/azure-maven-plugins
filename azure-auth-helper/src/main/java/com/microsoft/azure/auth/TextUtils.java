/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.nimbusds.oauth2.sdk.util.StringUtils;
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
}
