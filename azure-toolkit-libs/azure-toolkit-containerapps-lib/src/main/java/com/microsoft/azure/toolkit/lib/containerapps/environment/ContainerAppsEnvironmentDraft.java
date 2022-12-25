/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.environment;

import com.azure.resourcemanager.appcontainers.models.AppLogsConfiguration;
import com.azure.resourcemanager.appcontainers.models.LogAnalyticsConfiguration;
import com.azure.resourcemanager.appcontainers.models.ManagedEnvironment;
import com.azure.resourcemanager.appcontainers.models.ManagedEnvironments;
import com.microsoft.azure.toolkit.lib.applicationinsights.workspace.LogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class ContainerAppsEnvironmentDraft extends ContainerAppsEnvironment implements AzResource.Draft<ContainerAppsEnvironment, ManagedEnvironment> {
    @Getter
    private final ContainerAppsEnvironment origin;

    @Setter
    @Getter
    private Config config;

    protected ContainerAppsEnvironmentDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ContainerAppsEnvironmentModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    public ContainerAppsEnvironmentDraft(@Nonnull ContainerAppsEnvironment origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/containerapps.create_environment.env", params = {"this.getName()"})
    public ManagedEnvironment createResourceInAzure() {
        final IAzureMessager messager = AzureMessager.getMessager();
        final ManagedEnvironments client = Objects.requireNonNull(((ContainerAppsEnvironmentModule) getModule()).getClient());
        final Config config = ensureConfig();
        final AppLogsConfiguration appLogsConfiguration = new AppLogsConfiguration();
        final LogAnalyticsWorkspace logAnalyticsWorkspace = config.getLogAnalyticsWorkspace();
        if (Objects.nonNull(logAnalyticsWorkspace)) {
            final LogAnalyticsConfiguration analyticsConfiguration = new LogAnalyticsConfiguration()
                    .withCustomerId(logAnalyticsWorkspace.getCustomerId())
                    .withSharedKey(logAnalyticsWorkspace.getPrimarySharedKeys());
            appLogsConfiguration.withDestination("log-analytics").withLogAnalyticsConfiguration(analyticsConfiguration);
        }
        messager.info(AzureString.format("Start updating image in Container App({0})...", getName()));
        final ManagedEnvironment managedEnvironment = client.define(config.getName())
                .withRegion(com.azure.core.management.Region.fromName(config.getRegion().getName()))
                .withExistingResourceGroup(config.getResourceGroupName())
                .withAppLogsConfiguration(appLogsConfiguration).create();
        messager.info(AzureString.format("Image in Container App({0}) is successfully updated.", getName()));
        return managedEnvironment;
    }

    @Nonnull
    @Override
    public ManagedEnvironment updateResourceInAzure(@Nonnull ManagedEnvironment origin) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isModified() {
        return this.config != null && !Objects.equals(config, new Config());
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(ContainerAppsEnvironmentDraft.Config::new);
        return this.config;
    }

    @Data
    public static class Config {
        private String name;
        private String resourceGroupName;
        private Region region;
        private LogAnalyticsWorkspace logAnalyticsWorkspace;
    }
}
