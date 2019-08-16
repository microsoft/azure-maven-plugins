/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DesktopNotSupportedExceptionTest {
    @Test(expected = DesktopNotSupportedException.class)
    public void testCtor() throws DesktopNotSupportedException {
        throw new DesktopNotSupportedException("error");
    }

    @Test
    public void testMessage() {
        final DesktopNotSupportedException ex = new DesktopNotSupportedException("error");
        assertEquals("error", ex.getMessage());
    }
}
