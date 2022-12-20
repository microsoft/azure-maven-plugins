/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.network.networksecuritygroup;

import com.azure.resourcemanager.network.NetworkManager;
import com.azure.resourcemanager.network.models.NetworkSecurityGroup.DefinitionStages;
import com.azure.resourcemanager.network.models.NetworkSecurityRule.DefinitionStages.WithAttach;
import com.azure.resourcemanager.network.models.NetworkSecurityRule.DefinitionStages.WithDestinationAddressOrSecurityGroup;
import com.azure.resourcemanager.network.models.NetworkSecurityRule.DefinitionStages.WithDestinationPort;
import com.azure.resourcemanager.network.models.NetworkSecurityRule.DefinitionStages.WithProtocol;
import com.azure.resourcemanager.network.models.NetworkSecurityRule.DefinitionStages.WithSourceAddressOrSecurityGroup;
import com.azure.resourcemanager.network.models.NetworkSecurityRule.DefinitionStages.WithSourcePort;
import com.azure.resourcemanager.network.models.SecurityRuleProtocol;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
public class NetworkSecurityGroupDraft extends NetworkSecurityGroup implements AzResource.Draft<NetworkSecurityGroup, com.azure.resourcemanager.network.models.NetworkSecurityGroup> {
    private static final int BASE_PRIORITY = 300;
    private static final int PRIORITY_STEP = 20;

    @Getter
    @Nullable
    private final NetworkSecurityGroup origin;
    @Nullable
    @Getter(AccessLevel.NONE)
    private Region region;
    @Nonnull
    private List<SecurityRule> securityRules;

    NetworkSecurityGroupDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull NetworkSecurityGroupModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    NetworkSecurityGroupDraft(@Nonnull NetworkSecurityGroup origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.region = null;
        this.securityRules = Collections.emptyList();
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/vm.create_nsg.nsg", params = {"this.getName()"})
    public com.azure.resourcemanager.network.models.NetworkSecurityGroup createResourceInAzure() {
        final String name = this.getName();
        final Region region = Objects.requireNonNull(this.getRegion(), "'region' is required to create a Network security group");
        final List<SecurityRule> securityRules = this.getSecurityRules();

        final NetworkManager manager = Objects.requireNonNull(this.getParent().getRemote());
        final DefinitionStages.WithCreate create = manager.networkSecurityGroups().define(name)
            .withRegion(region.getName())
            .withExistingResourceGroup(this.getResourceGroupName());
        applySecurityRule(create, securityRules);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating Network security group ({0})...", name));
        com.azure.resourcemanager.network.models.NetworkSecurityGroup group = this.doModify(() -> create.create(), Status.CREATING);
        messager.success(AzureString.format("Network security group ({0}) is successfully created", name));
        return Objects.requireNonNull(group);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/vm.update_nsg.nsg", params = {"this.getName()"})
    public com.azure.resourcemanager.network.models.NetworkSecurityGroup updateResourceInAzure(@Nonnull com.azure.resourcemanager.network.models.NetworkSecurityGroup origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    private static void applySecurityRule(DefinitionStages.WithCreate withCreate, List<SecurityRule> securityRuleList) {
        for (int priority = BASE_PRIORITY, count = 0; count < securityRuleList.size(); count++, priority += PRIORITY_STEP) {
            final SecurityRule securityRule = securityRuleList.get(count);
            final WithSourceAddressOrSecurityGroup<DefinitionStages.WithCreate> withSource = withCreate.defineRule(securityRule.getName()).allowInbound();
            final WithSourcePort<DefinitionStages.WithCreate> withSourcePort = securityRule.getFromAddresses() != null ?
                withSource.fromAddresses(securityRule.getFromAddresses()) : withSource.fromAnyAddress();
            final WithDestinationAddressOrSecurityGroup<DefinitionStages.WithCreate> withDestination = securityRule.getFromPort() != null ?
                withSourcePort.fromPort(securityRule.getFromPort()) : withSourcePort.fromAnyPort();
            final WithDestinationPort<DefinitionStages.WithCreate> withDestPort = securityRule.getToAddresses() != null ?
                withDestination.toAddresses(securityRule.getToAddresses()) : withDestination.toAnyAddress();
            final WithProtocol<DefinitionStages.WithCreate> withProtocol = securityRule.getToPort() != null ?
                withDestPort.toPort(securityRule.getToPort()) : withDestPort.toAnyPort();
            final WithAttach<DefinitionStages.WithCreate> withAttach = securityRule.getProtocol() == SecurityRule.Protocol.ALL ?
                withProtocol.withAnyProtocol() : withProtocol.withProtocol(SecurityRuleProtocol.fromString(securityRule.getProtocol().name()));
            withAttach.withPriority(priority).attach();
        }
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(this.region)
            .orElseGet(() -> Optional.ofNullable(origin).map(NetworkSecurityGroup::getRegion).orElse(null));
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(this.region) && !Objects.equals(this.region, this.getRegion()) ||
            CollectionUtils.isNotEmpty(this.securityRules);
    }

    public static String generateDefaultName() {
        return String.format("security-group-%s", Utils.getTimestamp());
    }
}