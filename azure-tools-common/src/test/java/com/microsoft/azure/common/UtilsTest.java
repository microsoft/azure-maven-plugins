/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common;

import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.common.exceptions.AzureExecutionException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UtilsTest {
    @Test
    public void testParseOperationSystem() throws Exception {
        assertEquals(OperatingSystemEnum.Windows, Utils.parseOperationSystem("windows"));
        assertEquals(OperatingSystemEnum.Linux, Utils.parseOperationSystem("Linux"));
        assertEquals(OperatingSystemEnum.Docker, Utils.parseOperationSystem("dOcker"));
    }

    @Test
    public void testParseOperationSystemUnknown() throws Exception {
        try {
            Utils.parseOperationSystem("unkown");
            fail("expected AzureExecutionException when os is invalid");
        } catch (AzureExecutionException ex) {
            // expected AzureExecutionException when os is invalid
        }
        try {
            Utils.parseOperationSystem("windows ");
            fail("expected AzureExecutionException when os has spaces");
        } catch (AzureExecutionException ex) {
            // expected AzureExecutionException when os has spaces
        }

        try {
            Utils.parseOperationSystem(" ");
            fail("expected AzureExecutionException when os is empty");
        } catch (AzureExecutionException ex) {
            // expected AzureExecutionException when os is empty
        }

        try {
            Utils.parseOperationSystem(null);
            fail("expected AzureExecutionException when os is null");
        } catch (AzureExecutionException ex) {
            // expected AzureExecutionException when os is null
        }

    }
}
