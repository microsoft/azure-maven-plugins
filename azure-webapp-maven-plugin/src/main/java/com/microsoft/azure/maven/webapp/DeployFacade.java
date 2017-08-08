/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import org.apache.maven.plugin.MojoExecutionException;

public interface DeployFacade {
    DeployFacade setupRuntime() throws MojoExecutionException;

    DeployFacade applySettings() throws MojoExecutionException;

    DeployFacade commitChanges() throws MojoExecutionException;

    DeployFacade deployArtifacts() throws Exception;
}
