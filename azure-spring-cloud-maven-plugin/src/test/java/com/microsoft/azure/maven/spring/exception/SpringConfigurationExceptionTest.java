/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class SpringConfigurationExceptionTest {
    @Test(expected = SpringConfigurationException.class)
    public void testCtor() throws SpringConfigurationException {
        throw new SpringConfigurationException("error");
    }

    @Test
    public void testMessage() {
        final SpringConfigurationException ex = new SpringConfigurationException("error");
        assertEquals("error", ex.getMessage());
    }

    @Test
    public void testMessageCause() {
        final RuntimeException cause = new RuntimeException("cause");
        final SpringConfigurationException ex = new SpringConfigurationException("error", cause);
        assertEquals("error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
