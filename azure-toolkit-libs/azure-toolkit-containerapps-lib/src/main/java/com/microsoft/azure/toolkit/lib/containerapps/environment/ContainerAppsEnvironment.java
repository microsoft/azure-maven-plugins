/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.environment;

import com.azure.resourcemanager.appcontainers.models.CheckNameAvailabilityReason;
import com.azure.resourcemanager.appcontainers.models.CheckNameAvailabilityRequest;
import com.azure.resourcemanager.appcontainers.models.CheckNameAvailabilityResponse;
import com.azure.resourcemanager.appcontainers.models.ManagedEnvironment;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ContainerAppsEnvironment extends AbstractAzResource<ContainerAppsEnvironment, AzureContainerAppsServiceSubscription, ManagedEnvironment> implements Deletable {
    protected ContainerAppsEnvironment(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ContainerAppsEnvironmentModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void refresh() {
        Azure.az(AzureContainerApps.class).containerApps(this.getSubscriptionId()).refresh();
        super.refresh();
    }

    protected ContainerAppsEnvironment(@Nonnull ContainerAppsEnvironment insight) {
        super(insight);
    }

    protected ContainerAppsEnvironment(@Nonnull com.azure.resourcemanager.appcontainers.models.ManagedEnvironment remote, @Nonnull ContainerAppsEnvironmentModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
    }

    public List<ContainerApp> listContainerApps() {
        return Azure.az(AzureContainerApps.class).containerApps(this.getSubscriptionId()).listContainerAppsByEnvironment(this);
    }

    @AzureOperation(name = "azure/containerapps.check_name.name", params = "name")
    public Availability checkContainerAppNameAvailability(String name) {
        final CheckNameAvailabilityRequest request = new CheckNameAvailabilityRequest().withName(name).withType("Microsoft.App/containerApps");
        final CheckNameAvailabilityResponse result = Objects.requireNonNull(this.getModule().getParent().getRemote())
                .namespaces().checkNameAvailability(this.getResourceGroupName(), this.getName(), request);
        return new Availability(result.nameAvailable(), Optional.ofNullable(result.reason()).map(CheckNameAvailabilityReason::toString).orElse(null), result.message());
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull ManagedEnvironment remote) {
        return remote.provisioningState().toString();
    }
}
