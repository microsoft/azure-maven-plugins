/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerregistry;

import com.azure.containers.containerregistry.ContainerRegistryClient;
import com.azure.containers.containerregistry.ContainerRegistryClientBuilder;
import com.azure.containers.containerregistry.ContainerRepository;
import com.azure.containers.containerregistry.models.ContainerRegistryAudience;
import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

public class RepositoryModule extends AbstractAzResourceModule<Repository, ContainerRegistry, ContainerRepository> {

    public static final String NAME = "repositories";
    private ContainerRegistryClient client;

    public RepositoryModule(@Nonnull ContainerRegistry parent) {
        super(NAME, parent);
    }

    @Override
    protected void invalidateCache() {
        super.invalidateCache();
        this.client = null;
    }

    @Override
    public ContainerRegistryClient getClient() {
        if (Objects.isNull(this.client) && this.parent.exists()) {
            final String endpoint = String.format("https://%s", this.getParent().getLoginServerUrl());
            final Account account = Azure.az(AzureAccount.class).account();
            this.client = new ContainerRegistryClientBuilder()
                .endpoint(endpoint)
                .audience(getAudience())
                .credential(account.getTokenCredential(this.getSubscriptionId()))
                .buildClient();
        }
        return this.client;
    }

    @Nonnull
    @Override
    protected Stream<ContainerRepository> loadResourcesFromAzure() {
        if (!this.parent.exists()) {
            return Stream.empty();
        }
        final ContainerRegistryClient client = this.getClient();
        return Objects.requireNonNull(client).listRepositoryNames().stream().map(client::getRepository);
    }

    @Nullable
    @Override
    protected ContainerRepository loadResourceFromAzure(@Nonnull String name, String resourceGroup) {
        if (!this.parent.exists()) {
            return null;
        }
        final ContainerRegistryClient client = this.getClient();
        return Objects.requireNonNull(client).getRepository(name);
    }

    @Nonnull
    @Override
    protected Repository newResource(@Nonnull ContainerRepository registry) {
        return new Repository(registry.getName(), this);
    }

    @Nonnull
    @Override
    protected Repository newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new Repository(name, this);
    }

    private static ContainerRegistryAudience getAudience() {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureEnvironment env = account.getEnvironment();
        if (env.getPortal().equalsIgnoreCase(AzureEnvironment.AZURE_CHINA.getPortal())) {
            return ContainerRegistryAudience.AZURE_RESOURCE_MANAGER_CHINA;
        } else if (env.getPortal().equalsIgnoreCase(AzureEnvironment.AZURE_GERMANY.getPortal())) {
            return ContainerRegistryAudience.AZURE_RESOURCE_MANAGER_GERMANY;
        } else if (env.getPortal().equalsIgnoreCase(AzureEnvironment.AZURE_US_GOVERNMENT.getPortal())) {
            return ContainerRegistryAudience.AZURE_RESOURCE_MANAGER_GOVERNMENT;
        }
        return ContainerRegistryAudience.AZURE_RESOURCE_MANAGER_PUBLIC_CLOUD;
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Container ContainerRepository";
    }
}
