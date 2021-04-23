/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.maven.webapp.utils.DeployUtils;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AbstractConfigParser {

    protected AbstractWebAppMojo mojo;
    protected AbstractConfigurationValidator validator;

    public AbstractConfigParser(AbstractWebAppMojo mojo, AbstractConfigurationValidator validator) {
        this.mojo = mojo;
        this.validator = validator;
    }

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

    public String getSubscriptionId() {
        return mojo.getSubscriptionId();
    }

    public abstract Region getRegion() throws AzureExecutionException;

    public abstract DockerConfiguration getDockerConfiguration() throws AzureExecutionException;

    public abstract List<WebAppArtifact> getMavenArtifacts() throws AzureExecutionException;

    public abstract Runtime getRuntime() throws AzureExecutionException;

    public WebAppConfig parse() throws AzureExecutionException {
        return WebAppConfig.builder()
                .subscriptionId(getSubscriptionId())
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
                .webAppArtifacts(getMavenArtifacts())
                .appSettings(this.mojo.getAppSettings())
                .build();
    }

    protected MavenDockerCredentialProvider getDockerCredential(String serverId) {
        return MavenDockerCredentialProvider.fromMavenSettings(mojo.getSettings(), serverId);
    }

    protected void validate(Supplier<String> validator) throws AzureExecutionException {
        final String message = validator.get();
        if (StringUtils.isNotEmpty(message)) {
            throw new AzureExecutionException(message);
        }
    }

    protected static List<WebAppArtifact> parseArtifactsFromResources(List<? extends Resource> resources) {
        return CollectionUtils.isEmpty(resources) ? Collections.emptyList() :
                resources.stream()
                        // filter out external resources
                        .filter(resource -> !DeployUtils.isExternalResource(resource) && !DeployUtils.isOneDeployResource(resource))
                        .flatMap(resource -> convertResourceToArtifact(resource).stream()).collect(Collectors.toList());
    }

    protected static List<WebAppArtifact> convertResourceToArtifact(Resource resource) {
        return MavenArtifactUtils.getArtifacts(resource).stream()
                .map(file -> WebAppArtifact.builder().file(file).deployType(getDeployTypeFromFile(file)).path(resource.getTargetPath()).build())
                .collect(Collectors.toList());
    }

    protected static DeployType getDeployTypeFromFile(File file) {
        final DeployType type = DeployType.fromString(FilenameUtils.getExtension(file.getName()));
        return type == null ? DeployType.ZIP : type;
    }
}
