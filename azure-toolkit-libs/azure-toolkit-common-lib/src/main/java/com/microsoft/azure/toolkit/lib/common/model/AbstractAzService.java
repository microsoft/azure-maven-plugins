/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractAzService<T extends AbstractAzServiceSubscription<T, R>, R> extends AbstractAzResourceModule<T, AzResource.None, R>
    implements AzService {

    public AbstractAzService(@Nonnull String name) {
        super(name, AzResource.NONE);
        AzureEventBus.on("account.logged_out.account", new AzureEventBus.EventListener((e) -> this.clear()));
        AzureEventBus.on("account.subscription_changed.account", new AzureEventBus.EventListener((e) -> refreshOnSubscriptionChanged()));
    }

    @Nullable
    public T get(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        return this.get(id.subscriptionId(), id.resourceGroupName());
    }

    @Nonnull
    @Override
    public List<T> list() {
        return super.list().stream().filter(s -> s.getSubscription().isSelected()).collect(Collectors.toList());
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
            .flatMap(m -> ((AbstractAzServiceSubscription) m).getSubModules().stream())
            .forEach(m -> preload((AzResourceModule) m));
    }

    @AzureOperation(name = "auto/resource.refresh_on_subscription_changed.type", params = {"this.getResourceTypeName()"})
    private void refreshOnSubscriptionChanged() {
        this.clear();
        this.refresh();
    }

    @AzureOperation(name = "auto/resource.preload.type", params = {"module.getResourceTypeName()"})
    private static void preload(AzResourceModule<?> module) {
        OperationContext.action().setTelemetryProperty("preloading", String.valueOf(true));
        module.list();
    }

    @Nonnull
    public T forSubscription(@Nonnull String subscriptionId) {
        return Objects.requireNonNull(this.get(subscriptionId, null));
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/resource.load_resources_by_page.type", params = {"this.getResourceTypeName()"})
    protected Iterator<? extends ContinuablePage<String, R>> loadResourcePagesFromAzure() {
        final Stream<R> resources = Azure.az(IAzureAccount.class).account().getSelectedSubscriptions().stream().parallel()
            .map(Subscription::getId).map(i -> loadResourceFromAzure(i, null));
        return Collections.singletonList(new ItemPage<>(resources)).iterator();
    }

    @Nonnull
    @Override
    public String toResourceId(@Nonnull String resourceName, String resourceGroup) {
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/%s", resourceName, AzResource.RESOURCE_GROUP_PLACEHOLDER, this.getName());
    }

    @Nullable
    public <E> E getById(@Nonnull String id) { // move to upper class
        return this.doGetById(id);
    }

    @Nullable
    @SneakyThrows(UnsupportedEncodingException.class)
    protected <E> E doGetById(@Nonnull String id) {
        ResourceId resourceId = ResourceId.fromString(id);
        final String resourceGroup = resourceId.resourceGroupName();
        AbstractAzResource<?, ?, ?> resource = this.get(resourceId.subscriptionId(), resourceGroup);
        if (resource == null) {
            final IAccount account = Azure.az(IAzureAccount.class).account();
            final String message = String.format("the signed-in account (%s) has no access to resources in subscription (%s)", account.getUsername(), resourceId.subscriptionId());
            throw new AzureToolkitRuntimeException(message, Action.AUTHENTICATE);
        }
        final LinkedList<Pair<String, String>> resourceTypeNames = new LinkedList<>();
        while (resourceId != null) {
            resourceTypeNames.push(Pair.of(resourceId.resourceType(), URLDecoder.decode(resourceId.name(), "UTF-8")));
            resourceId = resourceId.parent();
        }
        for (final Pair<String, String> resourceTypeName : resourceTypeNames) {
            resource = Optional.ofNullable(resource)
                .map(r -> r.getSubModule(resourceTypeName.getLeft()))
                .map(m -> m.getOrTemp(resourceTypeName.getRight(), resourceGroup)).orElse(null);
        }
        //noinspection unchecked
        return (E) resource;
    }

    @Nullable
    public <E> E getOrInitById(@Nonnull String id) { // move to upper class
        return this.doGetOrInitById(id);
    }

    @Nullable
    @SneakyThrows(UnsupportedEncodingException.class)
    protected <E> E doGetOrInitById(@Nonnull String id) {
        ResourceId resourceId = ResourceId.fromString(id);
        final String resourceGroup = resourceId.resourceGroupName();
        AbstractAzResource<?, ?, ?> resource = Objects.requireNonNull(this.get(resourceId.subscriptionId(), resourceGroup));
        final LinkedList<Pair<String, String>> resourceTypeNames = new LinkedList<>();
        while (resourceId != null) {
            resourceTypeNames.push(Pair.of(resourceId.resourceType(), URLDecoder.decode(resourceId.name(), "UTF-8")));
            resourceId = resourceId.parent();
        }
        for (final Pair<String, String> resourceTypeName : resourceTypeNames) {
            resource = Optional.ofNullable(resource)
                .map(r -> r.getSubModule(resourceTypeName.getLeft()))
                .map(m -> m.getOrInit(resourceTypeName.getRight(), resourceGroup)).orElse(null);
        }
        //noinspection unchecked
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

    @Nonnull
    @Override
    protected T newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        final R r = this.loadResourceFromAzure(name, resourceGroupName);
        return this.newResource(Objects.requireNonNull(r));
    }
}
