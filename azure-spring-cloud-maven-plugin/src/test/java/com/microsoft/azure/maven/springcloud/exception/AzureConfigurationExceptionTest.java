/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.exception;

import com.microsoft.azure.toolkit.lib.common.exception.InvalidConfigurationException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class AzureConfigurationExceptionTest {
    @Test(expected = InvalidConfigurationException.class)
    public void testCtor() throws InvalidConfigurationException {
        throw new InvalidConfigurationException("error");
    }

    @Test
    public void testMessage() {
        final InvalidConfigurationException ex = new InvalidConfigurationException("error");
        assertEquals("error", ex.getMessage());
    }

    @Test
    public void testMessageCause() {
        final RuntimeException cause = new RuntimeException("cause");
        final InvalidConfigurationException ex = new InvalidConfigurationException("error", cause);
        assertEquals("error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
