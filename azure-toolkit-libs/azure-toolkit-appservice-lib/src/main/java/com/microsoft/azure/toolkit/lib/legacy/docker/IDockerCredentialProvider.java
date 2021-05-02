/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.docker;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;

public interface IDockerCredentialProvider {
    String getUsername() throws AzureExecutionException;

    String getPassword() throws AzureExecutionException;

    void validate() throws AzureExecutionException;
}
