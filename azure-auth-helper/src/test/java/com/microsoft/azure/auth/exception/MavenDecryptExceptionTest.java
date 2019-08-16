/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MavenDecryptExceptionTest {
    @Test(expected = MavenDecryptException.class)
    public void testCtor() throws MavenDecryptException {
        throw new MavenDecryptException("key", "value", "error");
    }

    @Test
    public void testMessage() {
        final MavenDecryptException ex = new MavenDecryptException("key", "value", "error");
        assertEquals("error", ex.getMessage());
    }

    @Test
    public void testProperty() {
        final MavenDecryptException ex = new MavenDecryptException("key", "value", "error");
        assertEquals("key", ex.getPropertyName());
        assertEquals("value", ex.getPropertyValue());
    }
}
