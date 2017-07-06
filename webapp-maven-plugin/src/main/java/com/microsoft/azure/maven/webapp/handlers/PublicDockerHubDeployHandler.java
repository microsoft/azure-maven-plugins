/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.DeployMojo;

/**
 * Deployment handler for public DockerHub image
 */
public class PublicDockerHubDeployHandler extends ContainerDeployHandler {
    /**
     * Constructor
     *
     * @param mojo
     */
    public PublicDockerHubDeployHandler(final DeployMojo mojo) {
        super(mojo);
    }

    /**
     * Create or update web app
     *
     * @param app
     * @throws Exception
     */
    @Override
    public void deploy(final WebApp app) throws Exception {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        if (app == null) {
            defineApp()
                    .withPublicDockerHubImage(containerSetting.getImageName())
                    .withStartUpCommand(containerSetting.getStartUpFile())
                    .withAppSettings(mojo.getAppSettings())
                    .create();
        } else {
            updateApp(app)
                    .withPublicDockerHubImage(containerSetting.getImageName())
                    .withStartUpCommand(containerSetting.getStartUpFile())
                    .withAppSettings(mojo.getAppSettings())
                    .apply();
        }
    }
}
