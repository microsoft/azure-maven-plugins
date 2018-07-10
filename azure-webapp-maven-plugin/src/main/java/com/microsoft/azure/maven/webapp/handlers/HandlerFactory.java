/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;

public abstract class HandlerFactory {
    private static HandlerFactory instance = new HandlerFactoryImpl();

    public static HandlerFactory getInstance() {
        return instance;
    }

    public abstract RuntimeHandler getRuntimeHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException;

    public abstract SettingsHandler getSettingsHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException;

    public abstract ArtifactHandler getArtifactHandler(final AbstractWebAppMojo mojo) throws MojoExecutionException;

    public abstract DeploymentSlotHandler getDeploymentSlotHandler(final AbstractWebAppMojo mojo)
            throws MojoExecutionException;
}
