/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfig;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkits.appservice.model.PricingTier;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.tools.common.model.Region;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

@AllArgsConstructor
public abstract class AbstractConfigParser {

    protected AbstractWebAppMojo mojo;
    protected AbstractConfigurationValidator validator;

    public String getAppName() throws AzureExecutionException {
        validate(validator::validateAppName);
        return mojo.getAppName();
    }

    public String getResourceGroup() throws AzureExecutionException {
        validate(validator::validateResourceGroup);
        return mojo.getResourceGroup();
    }

    public String getDeploymentSlotName() {
        return mojo.getDeploymentSlotSetting() == null ? null : mojo.getDeploymentSlotSetting().getName();
    }

    public String getDeploymentSlotConfigurationSource() {
        return mojo.getDeploymentSlotSetting() == null ? null : mojo.getDeploymentSlotSetting().getConfigurationSource();
    }

    public PricingTier getPricingTier() throws AzureExecutionException {
        validate(validator::validatePricingTier);
        return PricingTier.fromString(mojo.getPricingTier());
    }

    public String getAppServicePlanName() throws AzureExecutionException {
        validate(validator::validateAppServicePlan);
        return mojo.getAppServicePlanName();
    }

    public String getAppServicePlanResourceGroup() throws AzureExecutionException {
        validate(validator::validateResourceGroup);
        return mojo.getAppServicePlanResourceGroup();
    }

    public abstract Region getRegion();

    public abstract String getSchemaVersion();

    public abstract DockerConfiguration getDockerConfiguration();

    public abstract List<Pair<File, DeployType>> getResources();

    public abstract Runtime getRuntime();

    public WebAppConfig getWebAppConfig() throws AzureExecutionException {
        return WebAppConfig.builder()
                .appName(getAppName())
                .resourceGroup(getResourceGroup())
                .servicePlanName(getAppServicePlanName())
                .servicePlanResourceGroup(getAppServicePlanResourceGroup())
                .pricingTier(getPricingTier())
                .region(getRegion())
                .runtime(getRuntime())
                .dockerConfiguration(getDockerConfiguration())
                .deploymentSlotName(getDeploymentSlotName())
                .deploymentSlotConfigurationSource(getDeploymentSlotConfigurationSource())
                .resources(getResources())
                .build();
    }

    protected void validate(Supplier<String> validator) throws AzureExecutionException {
        final String message = validator.get();
        if (StringUtils.isNotEmpty(message)) {
            throw new AzureExecutionException(message);
        }
    }
}
