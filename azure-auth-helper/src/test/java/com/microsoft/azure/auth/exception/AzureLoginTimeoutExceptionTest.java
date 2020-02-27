/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AzureLoginTimeoutExceptionTest {
    @Test(expected = AzureLoginTimeoutException.class)
    public void testCtor() throws AzureLoginTimeoutException {
        throw new AzureLoginTimeoutException("error");
    }

    @Test
    public void testMessage() {
        final AzureLoginTimeoutException ex = new AzureLoginTimeoutException("error");
        assertEquals("error", ex.getMessage());
    }
}
