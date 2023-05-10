/*
 *
 *  * Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.utils.StreamingLogSupport;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ReplicaContainer extends AbstractAzResource<ReplicaContainer, Replica, com.azure.resourcemanager.appcontainers.models.ReplicaContainer>
    implements StreamingLogSupport {
    protected ReplicaContainer(@Nonnull String name, @Nonnull ReplicaContainerModule module) {
        super(name, module);
    }

    protected ReplicaContainer(@Nonnull ReplicaContainer origin) {
        super(origin);
    }

    protected ReplicaContainer(@Nonnull com.azure.resourcemanager.appcontainers.models.ReplicaContainer remote, @Nonnull ReplicaContainerModule module) {
        super(remote.name(), module);
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull com.azure.resourcemanager.appcontainers.models.ReplicaContainer remote) {
        return BooleanUtils.isTrue(remote.started()) ? Status.RUNNING : Status.UNKNOWN;
    }


    // refer to https://github.com/Azure/azure-cli-extensions/blob/main/src/containerapp/azext_containerapp/custom.py
    public @Nullable String getLogStreamEndpoint() {
        if (!this.exists()) {
            throw new AzureToolkitRuntimeException(AzureString.format("resource ({0}) not found", getName()).toString());
        }
        final Replica replica = this.getParent();
        final Revision revision = replica.getParent();
        final ContainerApp app = revision.getParent();
        final String eventStreamEndpoint = Objects.requireNonNull(app.getRemote()).eventStreamEndpoint();
        final String baseUrl = eventStreamEndpoint.substring(0, eventStreamEndpoint.indexOf("/subscriptions/"));
        return String.format("%s/subscriptions/%s/resourceGroups/%s/containerApps/%s/revisions/%s/replicas/%s/containers/%s/logstream",
            baseUrl, getSubscriptionId(), getResourceGroupName(), app.getName(), revision.getName(), replica.getName(), this.getName());
    }

    @Override
    public String getLogStreamAuthorization() {
        final AzureContainerAppsServiceSubscription subs = this.getParent().getParent().getParent().getParent();
        final ContainerAppsApiManager manager = subs.getRemote();
        final String authToken = Optional.ofNullable(manager).map(m -> m.containerApps().getAuthToken(getResourceGroupName(), getName()).token()).orElse(null);
        return "Bearer " + authToken;
    }
}
