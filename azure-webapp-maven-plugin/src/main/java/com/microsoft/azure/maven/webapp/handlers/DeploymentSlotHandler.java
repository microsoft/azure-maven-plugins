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
import org.codehaus.plexus.util.StringUtils;

public class DeploymentSlotHandler {
    private static final String INVALID_SLOT_SETTINGS = "<deploymentSlotSetting> is NULL." +
            "Need configure it in pom.xml for deploying it to deployment slot.";
    private static final String EMPTY_CONFIGURATION_SOURCE =
            "Empty configuration source, create a brand new deployment slot without any configuration.";
    private static final String DEFAULT_CONFIGURATION_SOURCE =
            "Null or unrecognized configuration source, create deployment slot and copy configuration from parent.";
    private static final String CREATE_DEPLOYMENT_SLOT = "Start create deployment slot...";
    private static final String CREATE_DEPLOYMENT_SLOT_DONE = "Successfully created deployment slot.";
    private static final String INVALID_SLOT_NAME = "<slotName> is invalid for creating deployment slot.";

    private AbstractWebAppMojo mojo;

    public DeploymentSlotHandler(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    public void createDeploymentSlotIfNotExist() throws MojoExecutionException, AzureAuthFailureException {
        final DeploymentSlotSetting slotSetting = this.mojo.getDeploymentSlotSetting();
        assureValidSlotSetting(slotSetting);

        final WebApp app = this.mojo.getWebApp();
        final String slotName = slotSetting.getSlotName();
        final DeploymentSlot slot = this.mojo.getDeploymentSlot(app, slotName);
        if (slot == null) {
            createDeploymentSlot(app, slotName, slotSetting.getConfigurationSource());
        }
    }

    protected void createDeploymentSlot(final WebApp app, final String slotName,
                                        final String configurationSource) throws MojoExecutionException {
        assureValidSlotName(slotName);
        final DeploymentSlot configurationSourceSlot = this.mojo.getDeploymentSlot(app, configurationSource);
        final DeploymentSlot.DefinitionStages.Blank definedSlot = app.deploymentSlots().define(slotName);

        this.mojo.getLog().info(CREATE_DEPLOYMENT_SLOT);

        if (configurationSourceSlot != null) {
            definedSlot.withConfigurationFromDeploymentSlot(configurationSourceSlot).create();
        } else if ("".equalsIgnoreCase(configurationSource)) {
            this.mojo.getLog().info(EMPTY_CONFIGURATION_SOURCE);
            definedSlot.withBrandNewConfiguration().create();
        } else {
            this.mojo.getLog().info(DEFAULT_CONFIGURATION_SOURCE);
            definedSlot.withConfigurationFromParent().create();
        }

        this.mojo.getLog().info(CREATE_DEPLOYMENT_SLOT_DONE);
    }

    protected void assureValidSlotSetting(DeploymentSlotSetting slotSetting) throws MojoExecutionException {
        if (slotSetting == null) {
            throw new MojoExecutionException(INVALID_SLOT_SETTINGS);
        }
    }

    protected void assureValidSlotName(final String slotName) throws MojoExecutionException {
        if (StringUtils.isEmpty(slotName)) {
            throw new MojoExecutionException(INVALID_SLOT_NAME);
        }
    }
}
