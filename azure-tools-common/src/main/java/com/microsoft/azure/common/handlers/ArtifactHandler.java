/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.handlers;

import com.microsoft.azure.common.appservice.DeployTarget;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;

public interface ArtifactHandler<T extends DeployTarget> {
    void publish(T deployTarget) throws AzureExecutionException;
}
