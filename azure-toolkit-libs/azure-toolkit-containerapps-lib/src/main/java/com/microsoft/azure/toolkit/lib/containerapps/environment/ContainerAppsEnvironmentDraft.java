/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.environment;

import com.azure.resourcemanager.appcontainers.models.AppLogsConfiguration;
import com.azure.resourcemanager.appcontainers.models.LogAnalyticsConfiguration;
import com.azure.resourcemanager.appcontainers.models.ManagedEnvironment;
import com.azure.resourcemanager.appcontainers.models.ManagedEnvironments;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceDraft;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
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

    @Nullable
    @Override
    public Region getRegion() {
        return Optional.ofNullable(config).map(Config::getRegion).orElseGet(super::getRegion);
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
            if (logAnalyticsWorkspace.isDraftForCreating() && !logAnalyticsWorkspace.exists()) {
                ((LogAnalyticsWorkspaceDraft) logAnalyticsWorkspace).commit();
            }
            final LogAnalyticsConfiguration analyticsConfiguration = new LogAnalyticsConfiguration()
                    .withCustomerId(logAnalyticsWorkspace.getCustomerId())
                    .withSharedKey(logAnalyticsWorkspace.getPrimarySharedKeys());
            appLogsConfiguration.withDestination("log-analytics").withLogAnalyticsConfiguration(analyticsConfiguration);
        }
        messager.info(AzureString.format("Start creating Azure Container Apps Environment({0})...", this.getName()));
        final ManagedEnvironment managedEnvironment = client.define(config.getName())
                .withRegion(com.azure.core.management.Region.fromName(config.getRegion().getName()))
                .withExistingResourceGroup(Objects.requireNonNull(config.getResourceGroup(), "Resource Group is required to create Container app.").getResourceGroupName())
                .withAppLogsConfiguration(appLogsConfiguration).create();
        messager.success(AzureString.format("Azure Container Apps Environment({0}) is successfully created.", this.getName()));
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
        private Subscription subscription;
        private ResourceGroup resourceGroup;
        private Region region;
        private LogAnalyticsWorkspace logAnalyticsWorkspace;
    }
}
