/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.compute.security;

import com.azure.resourcemanager.network.models.NetworkSecurityGroup.DefinitionStages.WithCreate;
import com.azure.resourcemanager.network.models.NetworkSecurityRule;
import com.azure.resourcemanager.network.models.NetworkSecurityRule.DefinitionStages.WithDestinationAddressOrSecurityGroup;
import com.azure.resourcemanager.network.models.NetworkSecurityRule.DefinitionStages.WithSourceAddressOrSecurityGroup;
import com.azure.resourcemanager.network.models.NetworkSecurityRule.DefinitionStages.WithSourcePort;
import com.azure.resourcemanager.network.models.SecurityRuleProtocol;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasId;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.compute.AzureResourceDraft;
import com.microsoft.azure.toolkit.lib.compute.security.model.SecurityRule;
import io.jsonwebtoken.lang.Collections;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DraftNetworkSecurityGroup extends NetworkSecurityGroup implements AzureResourceDraft<NetworkSecurityGroup> {
    private static final int BASE_PRIORITY = 300;
    private static final int PRIORITY_STEP = 20;

    private Region region;
    private List<SecurityRule> securityRuleList;

    public DraftNetworkSecurityGroup(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        super(getResourceId(subscriptionId, resourceGroup, name), null);
    }

    public void setSubscriptionId(final String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setResourceGroup(final String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return Optional.ofNullable(remote).map(HasId::id).orElseGet(() -> getResourceId(subscriptionId, resourceGroup, name));
    }

    NetworkSecurityGroup create(final AzureNetworkSecurityGroup module) {
        this.module = module;
        WithCreate withCreate = module.getSecurityGroupManager(subscriptionId)
                .define(name)
                .withRegion(region.getName())
                .withExistingResourceGroup(resourceGroup);
        if (!Collections.isEmpty(securityRuleList)) {
            applySecurityRule(withCreate, securityRuleList);
        }
        this.remote = withCreate.create();
        refreshStatus();
        module.refresh();
        return this;
    }

    private static void applySecurityRule(WithCreate withCreate, List<SecurityRule> securityRuleList) {
        for (int priority = BASE_PRIORITY, count = 0; count < securityRuleList.size(); count++, priority += PRIORITY_STEP) {
            applySecurityRule(withCreate, securityRuleList.get(count), priority);
        }
    }

    private static void applySecurityRule(final WithCreate withCreate, final SecurityRule securityRule, final int priority) {
        final WithSourceAddressOrSecurityGroup<WithCreate> withSource = withCreate.defineRule(securityRule.getName()).allowInbound();
        final WithSourcePort<WithCreate> withSourcePort = securityRule.getFromAddresses() != null ?
                withSource.fromAddresses(securityRule.getFromAddresses()) : withSource.fromAnyAddress();
        final WithDestinationAddressOrSecurityGroup<WithCreate> withDestination = securityRule.getFromPort() != null ?
                withSourcePort.fromPort(securityRule.getFromPort()) : withSourcePort.fromAnyPort();
        final NetworkSecurityRule.DefinitionStages.WithDestinationPort<WithCreate> withDestPort = securityRule.getToAddresses() != null ?
                withDestination.toAddresses(securityRule.getToAddresses()) : withDestination.toAnyAddress();
        final NetworkSecurityRule.DefinitionStages.WithProtocol<WithCreate> withProtocol = securityRule.getToPort() != null ?
                withDestPort.toPort(securityRule.getToPort()) : withDestPort.toAnyPort();
        final NetworkSecurityRule.DefinitionStages.WithAttach<WithCreate> withAttach = securityRule.getProtocol() == SecurityRule.Protocol.ALL ?
                withProtocol.withAnyProtocol() : withProtocol.withProtocol(SecurityRuleProtocol.fromString(securityRule.getProtocol().name()));
        withAttach.withPriority(priority).attach();
    }

    @Override
    protected String loadStatus() {
        return Optional.ofNullable(remote).map(ignore -> super.loadStatus()).orElse(IAzureBaseResource.Status.DRAFT);
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.network.models.NetworkSecurityGroup loadRemote() {
        return Optional.ofNullable(remote).map(ignore -> super.loadRemote()).orElse(null);
    }

    private static String getResourceId(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        return String.format("/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkSecurityGroups/%s", subscriptionId, resourceGroup, name);
    }
}
