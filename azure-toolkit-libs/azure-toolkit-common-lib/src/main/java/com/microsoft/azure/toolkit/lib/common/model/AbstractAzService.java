/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class AbstractAzService<T extends AbstractAzResourceManager<T, R>, R> extends AbstractAzResourceModule<T, AzResource.None, R>
    implements AzService {

    public AbstractAzService(@Nonnull String name) {
        super(name, AzResource.NONE);
        AzureEventBus.on("account.logout.account", new AzureEventBus.EventListener((e) -> this.clear()));
        AzureEventBus.on("account.subscription_changed.account", new AzureEventBus.EventListener((e) -> this.refresh()));
    }

    @Nullable
    public T get(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        return this.get(id.subscriptionId(), id.resourceGroupName());
    }

    @Nonnull
    @Override
    public String getFullResourceType() {
        return this.getName();
    }

    @Preload
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void preload() {
        Azure.getServices(AbstractAzService.class).stream()
            .flatMap(s -> s.list().stream())
            .flatMap(m -> ((AbstractAzResourceManager) m).getSubModules().stream())
            .forEach(m -> preload((AzResourceModule) m));
    }

    @AzureOperation(name = "resource.preload", type = AzureOperation.Type.ACTION)
    private static void preload(AzResourceModule<?, ?, ?> m) {
        OperationContext.action().setTelemetryProperty("preloading", String.valueOf(true));
        m.list();
    }

    @Nonnull
    public T forSubscription(@Nonnull String subscriptionId) {
        return Objects.requireNonNull(this.get(subscriptionId, null));
    }

    @Override
    public void refresh() {
        super.refresh();
        this.list().forEach(AbstractAzResource::refresh);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "resource.list_resources.type", params = {"this.getResourceTypeName()"}, type = AzureOperation.Type.SERVICE)
    protected Stream<R> loadResourcesFromAzure() {
        return Azure.az(IAzureAccount.class).account().getSelectedSubscriptions().stream().parallel()
            .map(Subscription::getId).map(i -> loadResourceFromAzure(i, null));
    }

    @Nonnull
    @Override
    public String toResourceId(@Nonnull String resourceName, String resourceGroup) {
        final String rg = StringUtils.firstNonBlank(resourceGroup, AzResource.RESOURCE_GROUP_PLACEHOLDER);
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/%s", resourceName, rg, this.getName());
    }

    @Nullable
    public <E> E getById(@Nonnull String id) { // move to upper class
        return this.doGetById(id);
    }

    @Nullable
    protected <E> E doGetById(@Nonnull String id) { // move to upper class
        ResourceId resourceId = ResourceId.fromString(id);
        final String resourceGroup = resourceId.resourceGroupName();
        AbstractAzResource<?, ?, ?> resource = Objects.requireNonNull(this.get(resourceId.subscriptionId(), resourceGroup));
        final LinkedList<Pair<String, String>> resourceTypeNames = new LinkedList<>();
        while (resourceId != null) {
            resourceTypeNames.push(Pair.of(resourceId.resourceType(), resourceId.name()));
            resourceId = resourceId.parent();
        }
        for (Pair<String, String> resourceTypeName : resourceTypeNames) {
            resource = (AbstractAzResource<?, ?, ?>) resource.getSubModule(resourceTypeName.getLeft()).getOrDraft(resourceTypeName.getRight(), resourceGroup);
        }
        return (E) resource;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
