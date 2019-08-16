/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AzureLoginFailureExceptionTest {
    @Test(expected = AzureLoginFailureException.class)
    public void testCtor() throws AzureLoginFailureException {
        throw new AzureLoginFailureException("error");
    }

    @Test
    public void testMessage() {
        final AzureLoginFailureException ex = new AzureLoginFailureException("error");
        assertEquals("error", ex.getMessage());
    }
}
