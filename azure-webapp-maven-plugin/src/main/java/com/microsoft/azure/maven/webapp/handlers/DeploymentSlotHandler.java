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
import com.microsoft.azure.maven.webapp.configuration.ConfigurationSourceType;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.regex.Pattern;

public class DeploymentSlotHandler {
    private static final String INVALID_SLOT_SETTINGS = "<deploymentSlotSetting> is NULL." +
            "Need configure it in pom.xml for deploying it to deployment slot.";
    private static final String SLOT_NAME_PATTERN = "[A-Za-z0-9-]{1,60}";
    private static final String INVALID_SLOT_NAME = String.format("<slotName> must match %s", SLOT_NAME_PATTERN);
    private static final String EMPTY_CONFIGURATION_SOURCE =
            "Creating a brand new deployment slot without any configuration.";
    private static final String DEFAULT_CONFIGURATION_SOURCE =
            "Creating deployment slot and copy configuration from parent.";
    private static final String COPY_CONFIGURATION_FROM_SLOT =
            "Creating deployment slot and copy configuration from %s";
    private static final String CREATE_DEPLOYMENT_SLOT = "Start create deployment slot...";
    private static final String CREATE_DEPLOYMENT_SLOT_DONE = "Successfully created deployment slot.";
    private static final String UNKNOWN_CONFIGURATION_SOURCE =
            "Unknown <configurationSource> value. Please use 'NEW', 'PARENT' or other slot name.";
    private static final String TARGET_CONFIGURATION_SOURCE_SLOT_NOT_EXIST =
            "The slot configured in <configurationSource> to copy configuration from does not exist.";


    private AbstractWebAppMojo mojo;

    public DeploymentSlotHandler(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    public void createDeploymentSlotIfNotExist() throws MojoExecutionException, AzureAuthFailureException {
        final DeploymentSlotSetting slotSetting = this.mojo.getDeploymentSlotSetting();
        assureValidSlotSetting(slotSetting);

        final WebApp app = this.mojo.getWebApp();
        final String slotName = slotSetting.getSlotName();

        if (this.mojo.getDeploymentSlot(app, slotName) == null) {
            createDeploymentSlot(app, slotName, slotSetting.getConfigurationSource());
        }
    }

    protected void createDeploymentSlot(final WebApp app, final String slotName,
                                        final String configurationSource) throws MojoExecutionException {
        assureValidSlotName(slotName);
        final DeploymentSlot.DefinitionStages.Blank definedSlot = app.deploymentSlots().define(slotName);
        final ConfigurationSourceType type = ConfigurationSourceType.fromString(configurationSource);

        this.mojo.getLog().info(CREATE_DEPLOYMENT_SLOT);

        switch (type) {
            case NEW:
                this.mojo.getLog().info(EMPTY_CONFIGURATION_SOURCE);
                definedSlot.withBrandNewConfiguration().create();
                break;
            case PARENT:
                this.mojo.getLog().info(DEFAULT_CONFIGURATION_SOURCE);
                definedSlot.withConfigurationFromParent().create();
                break;
            case OTHERS:
                final DeploymentSlot configurationSourceSlot = this.mojo.getDeploymentSlot(app, configurationSource);
                if (configurationSourceSlot == null) {
                    throw new MojoExecutionException(TARGET_CONFIGURATION_SOURCE_SLOT_NOT_EXIST);
                }
                this.mojo.getLog().info(COPY_CONFIGURATION_FROM_SLOT);
                definedSlot.withConfigurationFromDeploymentSlot(configurationSourceSlot).create();
                break;
            default:
                throw new MojoExecutionException(UNKNOWN_CONFIGURATION_SOURCE);
        }

        this.mojo.getLog().info(CREATE_DEPLOYMENT_SLOT_DONE);
    }

    protected void assureValidSlotSetting(final DeploymentSlotSetting slotSetting) throws MojoExecutionException {
        if (slotSetting == null) {
            throw new MojoExecutionException(INVALID_SLOT_SETTINGS);
        }
    }

    protected void assureValidSlotName(final String slotName) throws MojoExecutionException {
        final Pattern pattern = Pattern.compile(SLOT_NAME_PATTERN, Pattern.CASE_INSENSITIVE);

        if (!pattern.matcher(slotName).matches()) {
            throw new MojoExecutionException(INVALID_SLOT_NAME);
        }
    }
}
