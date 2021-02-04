/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.common.appservice.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;

@Deprecated
public class V2NoValidationConfigurationParser extends V2ConfigurationParser {

    public V2NoValidationConfigurationParser(AbstractWebAppMojo mojo, AbstractConfigurationValidator validator) {
        super(mojo, validator);
    }

    @Override
    protected String getAppName() throws AzureExecutionException {
        return validateConfiguration(validator.validateAppName()) ? super.getAppName() : mojo.getAppName();
    }

    @Override
    protected String getResourceGroup() throws AzureExecutionException {
        return validateConfiguration(validator.validateResourceGroup()) ?
                super.getResourceGroup() : mojo.getResourceGroup();
    }

    @Override
    protected PricingTier getPricingTier() throws AzureExecutionException {
        return validateConfiguration(validator.validatePricingTier()) ?
                super.getPricingTier() : null;
    }

    @Override
    protected DeploymentSlotSetting getDeploymentSlotSetting() throws AzureExecutionException {
        return validateConfiguration(validator.validateDeploymentSlot()) ? super.getDeploymentSlotSetting()
                : mojo.getDeploymentSlotSetting();
    }

    @Override
    protected OperatingSystemEnum getOs() throws AzureExecutionException {
        return validateConfiguration(validator.validateOs()) ? super.getOs() : null;
    }

    @Override
    protected Region getRegion() throws AzureExecutionException {
        return validateConfiguration(validator.validateRegion()) ? super.getRegion() : null;
    }

    @Override
    protected RuntimeStack getRuntimeStack() throws AzureExecutionException {
        return validateConfiguration(validator.validateRuntimeStack()) ? super.getRuntimeStack() : null;
    }

    @Override
    protected String getImage() throws AzureExecutionException {
        return validateConfiguration(validator.validateImage()) ? super.getImage() : null;
    }

    @Override
    protected JavaVersion getJavaVersion() throws AzureExecutionException {
        return validateConfiguration(validator.validateJavaVersion()) ? super.getJavaVersion() : null;
    }

    @Override
    protected WebContainer getWebContainer() throws AzureExecutionException {
        return validateConfiguration(validator.validateWebContainer()) ? super.getWebContainer() : null;
    }

    protected boolean validateConfiguration(String errorMessage) {
        if (errorMessage != null) {
            Log.warn(errorMessage);
        }
        return errorMessage == null;
    }
}
