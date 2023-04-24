/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.eventhubs;

import com.azure.resourcemanager.eventhubs.EventHubsManager;
import com.azure.resourcemanager.eventhubs.models.AccessRights;
import com.azure.resourcemanager.eventhubs.models.EventHubNamespace;
import com.azure.resourcemanager.eventhubs.models.EventHubNamespaceAuthorizationRule;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class EventHubsNamespace extends AbstractAzResource<EventHubsNamespace, EventHubsNamespaceSubscription, EventHubNamespace> implements Deletable {
    @Nonnull
    private final EventHubsInstanceModule instanceModule;
    protected EventHubsNamespace(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull EventHubsNamespaceModule module) {
        super(name, resourceGroupName, module);
        this.instanceModule = new EventHubsInstanceModule(this);
    }
    
    protected EventHubsNamespace(@Nonnull EventHubsNamespace origin) {
        super(origin);
        this.instanceModule = origin.instanceModule;
    }

    protected EventHubsNamespace(@Nonnull EventHubNamespace remote, @Nonnull EventHubsNamespaceModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.instanceModule = new EventHubsInstanceModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(instanceModule);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull EventHubNamespace remote) {
        return remote.innerModel().status();
    }

    public List<EventHubsInstance> getInstances() {
        return this.instanceModule.list();
    }

    public String getOrCreateListenConnectionString() {
        final List<AccessRights> accessRights = Collections.singletonList(AccessRights.LISTEN);
        final List<EventHubNamespaceAuthorizationRule> connectionStrings = Optional.ofNullable(getRemote())
                .map(eventHubInstance -> eventHubInstance.listAuthorizationRules().stream()
                        .filter(rule -> new HashSet<>(rule.rights()).containsAll(accessRights))
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
        if (connectionStrings.size() > 0) {
            return connectionStrings.get(0).getKeys().primaryConnectionString();
        }
        final EventHubsManager manager = getParent().getRemote();
        if (Objects.isNull(manager)) {
            throw new AzureToolkitRuntimeException(AzureString.format("resource ({0}) not found", getName()).toString());
        }
        final String accessRightsStr = StringUtils.join(accessRights, "-");
        final EventHubNamespaceAuthorizationRule.DefinitionStages.WithAccessPolicy policy = manager.namespaceAuthorizationRules()
                .define(String.format("policy-%s-%s", accessRightsStr, Utils.getTimestamp()))
                .withExistingNamespace(getResourceGroupName(), getName());
        EventHubNamespaceAuthorizationRule.DefinitionStages.WithCreate withCreate = policy.withListenAccess();
        if (accessRights.contains(AccessRights.MANAGE)) {
            withCreate = policy.withManageAccess();
        } else if (accessRights.contains(AccessRights.SEND) && accessRights.contains(AccessRights.LISTEN)) {
            withCreate = policy.withSendAndListenAccess();
        } else if (accessRights.contains(AccessRights.SEND)) {
            withCreate = policy.withSendAccess();
        }
        return withCreate.create().getKeys().primaryConnectionString();
    }
}
