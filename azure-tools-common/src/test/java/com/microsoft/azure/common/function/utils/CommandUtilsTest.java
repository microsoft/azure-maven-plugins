/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.utils;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class CommandUtilsTest {

    @Test
    public void isWindows() {
        assertEquals(SystemUtils.IS_OS_WINDOWS, CommandUtils.isWindows());
    }

    @Test
    public void getDefaultValidReturnCodes() throws Exception {
        assertEquals(2, CommandUtils.getValidReturnCodes().size());
        assertEquals(true, CommandUtils.getValidReturnCodes().contains(0L));
    }

    @Test
    public void getValidReturnCodes() {
        assertEquals(Arrays.asList(0L), CommandUtils.getDefaultValidReturnCodes());
    }
}
