/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import org.apache.maven.plugin.MojoExecutionException;

public class DeploymentSlotHandlerImpl implements DeploymentSlotHandler {
    private static final String INVALID_SLOT_SETTINGS = "<deploymentSlotSetting> is NULL." +
            "Need configure it in pom.xml for deploying it to deployment slot.";
    private static final String NULL_CONFIGURATION_SOURCE =
            "Empty configuration source, create a brand new deployment slot without any configuration.";
    private static final String DEFAULT_CONFIGURATION_SOURCE =
            "Null or unrecognized configuration source, create deployment slot and copy configuration from parent.";
    private static final String CREATE_DEPLOYMENT_SLOT = "Start create deployment slot...";
    private static final String CREATE_DEPLOYMENT_SLOT_DONE = "Successfully created deployment slot.";

    private AbstractWebAppMojo mojo;

    public DeploymentSlotHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public void handleDeploymentSlot() throws MojoExecutionException, AzureAuthFailureException {
        final DeploymentSlotSetting slotSetting = this.mojo.getDeploymentSlotSetting();
        assureValidSlotSetting(slotSetting);

        final WebApp app = this.mojo.getWebApp();
        final String slotName = slotSetting.getSlotName();
        if (isDeploymentSlotExists(app, slotName)) {
            return;
        }

        this.mojo.getLog().info(CREATE_DEPLOYMENT_SLOT);

        createDeploymentSlotWithConfigurationSource(app, slotName, slotSetting.getConfigurationSource());

        this.mojo.getLog().info(CREATE_DEPLOYMENT_SLOT_DONE);
    }

    protected void createDeploymentSlotWithConfigurationSource (final WebApp app, final String slotName,
                                                              final String configurationSource) {
        final DeploymentSlot cSlot = this.mojo.getDeploymentSlot(app, configurationSource);

        if (cSlot != null) {
            app.deploymentSlots().define(slotName).withConfigurationFromDeploymentSlot(cSlot).create();
        } else if (isCreateSlotWithoutConfiguration(configurationSource)) {
            this.mojo.getLog().info(NULL_CONFIGURATION_SOURCE);

            app.deploymentSlots().define(slotName).withBrandNewConfiguration().create();
        } else {
            this.mojo.getLog().info(DEFAULT_CONFIGURATION_SOURCE);

            app.deploymentSlots().define(slotName).withConfigurationFromParent().create();
        }
    }

    protected boolean isCreateSlotWithoutConfiguration(final String configurationSource) {
        return configurationSource != null && configurationSource.equalsIgnoreCase("");
    }

    private boolean isDeploymentSlotExists(final WebApp app, final String slotName) {
        return this.mojo.getDeploymentSlot(app, slotName) != null;
    }

    protected void assureValidSlotSetting(DeploymentSlotSetting slotSetting) throws MojoExecutionException {
        if (slotSetting == null) {
            throw new MojoExecutionException(INVALID_SLOT_SETTINGS);
        }
    }
}
