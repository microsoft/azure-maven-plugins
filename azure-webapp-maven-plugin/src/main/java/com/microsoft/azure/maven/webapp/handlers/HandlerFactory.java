/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public abstract class HandlerFactory {
    private static HandlerFactory instance = new HandlerFactoryImpl();

    public static HandlerFactory getInstance() {
        return instance;
    }

    public abstract RuntimeHandler getRuntimeHandler(final WebAppConfiguration config,
                                                     final Azure azureClient, final Log log)
        throws MojoExecutionException;

    public abstract SettingsHandler getSettingsHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException;

    public abstract ArtifactHandler getArtifactHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException;

    public abstract DeploymentSlotHandler getDeploymentSlotHandler(final AbstractWebAppMojo mojo)
        throws MojoExecutionException;
}
