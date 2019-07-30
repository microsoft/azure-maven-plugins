/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import org.junit.Test;

public class AzureLoginFailureExceptionTest {

    @Test(expected = AzureLoginFailureException.class)
    public void testAzureLoginFailureException() throws AzureLoginFailureException {
        throw new AzureLoginFailureException("Message to display while throwing an error");
    }
}
