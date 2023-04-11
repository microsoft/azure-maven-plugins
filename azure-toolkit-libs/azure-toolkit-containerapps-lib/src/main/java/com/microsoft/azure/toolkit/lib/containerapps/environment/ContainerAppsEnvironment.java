/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.environment;

import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.azure.resourcemanager.appcontainers.models.CheckNameAvailabilityReason;
import com.azure.resourcemanager.appcontainers.models.CheckNameAvailabilityRequest;
import com.azure.resourcemanager.appcontainers.models.CheckNameAvailabilityResponse;
import com.azure.resourcemanager.appcontainers.models.ManagedEnvironment;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.*;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.StreamingLogUtils;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

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

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(getRemote()).map(remote -> Region.fromName(remote.region().name())).orElse(null);
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

    public Flux<String> streamingLogs(boolean follow, int tailLines) {
        final String endPoint = getLogStreamingEndpoint();
        final String basicAuth = "Bearer " + getAuthToken();
        final Map<String, String> params = ImmutableMap.of("follow", String.valueOf(follow), "tailLines", String.valueOf(tailLines));
        return StreamingLogUtils.streamingLogs(endPoint, params, ImmutableMap.of("Authorization", basicAuth));
    }

    private String getLogStreamingEndpoint() {
        final ManagedEnvironment remoteEnv = this.getRemote();
        if (Objects.isNull(remoteEnv)) {
            throw new AzureToolkitRuntimeException(AzureString.format("resource ({0}) not found", getName()).toString());
        }
        final String baseUrl = String.format("https://%s.azurecontainerapps.dev", remoteEnv.location());
        return String.format("%s/subscriptions/%s/resourceGroups/%s/managedEnvironments/%s/eventstream",
                baseUrl, getSubscriptionId(), getResourceGroupName(), getName());
    }

    @Nullable
    private String getAuthToken() {
        final ContainerAppsApiManager manager = getParent().getRemote();
        return Optional.ofNullable(manager).map(m -> m.managedEnvironments().getAuthToken(getResourceGroupName(), getName()).token()).orElse(null);
    }
}
