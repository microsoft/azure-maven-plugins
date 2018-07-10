/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import org.apache.maven.plugin.MojoExecutionException;

public interface DeploymentSlotHandler {
    void handleDeploymentSlot() throws AzureAuthFailureException, MojoExecutionException;
}
