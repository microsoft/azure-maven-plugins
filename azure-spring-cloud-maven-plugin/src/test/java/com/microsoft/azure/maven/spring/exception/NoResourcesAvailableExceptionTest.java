/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NoResourcesAvailableExceptionTest {
    @Test(expected = NoResourcesAvailableException.class)
    public void testCtor() throws NoResourcesAvailableException {
        throw new NoResourcesAvailableException("error");
    }

    @Test
    public void testMessage() {
        final NoResourcesAvailableException ex = new NoResourcesAvailableException("error");
        assertEquals("error", ex.getMessage());
    }
}
