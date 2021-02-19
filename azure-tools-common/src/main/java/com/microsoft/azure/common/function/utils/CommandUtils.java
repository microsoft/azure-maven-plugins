/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.utils;

import org.apache.commons.lang3.SystemUtils;

import java.util.Arrays;
import java.util.List;

public class CommandUtils {

    public static boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    public static List<Long> getDefaultValidReturnCodes() {
        return Arrays.asList(0L);
    }

    public static List<Long> getValidReturnCodes() {
        return isWindows() ?
                // Windows return code of CTRL-C is 3221225786
                Arrays.asList(0L, 3221225786L) :
                // Linux return code of CTRL-C is 130
                Arrays.asList(0L, 130L);
    }
}
