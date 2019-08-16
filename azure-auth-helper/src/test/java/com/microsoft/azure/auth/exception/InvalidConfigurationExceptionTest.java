/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InvalidConfigurationExceptionTest {
    @Test(expected = InvalidConfigurationException.class)
    public void testCtor() throws InvalidConfigurationException {
        throw new InvalidConfigurationException("error");
    }

    @Test
    public void testMessage() {
        final InvalidConfigurationException ex = new InvalidConfigurationException("error");
        assertEquals("error", ex.getMessage());
    }
}
