/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.environment;

import com.azure.resourcemanager.appcontainers.models.ManagedEnvironment;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class ContainerAppsEnvironment extends AbstractAzResource<ContainerAppsEnvironment, AzureContainerAppsServiceSubscription, ManagedEnvironment> implements Deletable {
    protected ContainerAppsEnvironment(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ContainerAppsEnvironmentModule module) {
        super(name, resourceGroupName, module);
    }

    protected ContainerAppsEnvironment(@Nonnull ContainerAppsEnvironment insight) {
        super(insight);
    }

    protected ContainerAppsEnvironment(@Nonnull com.azure.resourcemanager.appcontainers.models.ManagedEnvironment remote, @Nonnull ContainerAppsEnvironmentModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
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
