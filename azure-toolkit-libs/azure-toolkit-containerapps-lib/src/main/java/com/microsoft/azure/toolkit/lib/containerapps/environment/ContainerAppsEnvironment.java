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
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import org.apache.http.client.utils.URIBuilder;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
        try {
            final URIBuilder uriBuilder = new URIBuilder(endPoint);
            uriBuilder.addParameter("follow", String.valueOf(follow));
            if (tailLines > 0) {
                uriBuilder.addParameter("tailLines", String.valueOf(tailLines));
            }
            final HttpURLConnection connection = (HttpURLConnection) uriBuilder.build().toURL().openConnection();
            connection.setRequestProperty("Authorization", basicAuth);
            connection.setRequestMethod("GET");
            connection.setReadTimeout(600000);
            connection.setConnectTimeout(3000);
            connection.connect();
            return Flux.create((fluxSink) -> {
                try {
                    final InputStream is = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        fluxSink.next(line);
                    }
                    rd.close();
                } catch (final Exception e) {
                    throw new AzureToolkitRuntimeException(e);
                }
            });
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(e);
        }
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
