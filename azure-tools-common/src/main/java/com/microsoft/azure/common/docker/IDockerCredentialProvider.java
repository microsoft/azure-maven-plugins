/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.docker;

import com.microsoft.azure.common.exceptions.AzureExecutionException;

public interface IDockerCredentialProvider {
    String getUsername() throws AzureExecutionException;

    String getPassword() throws AzureExecutionException;
}
