/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.postgre;

import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.FirewallRule;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class PostgreSqlFirewallRuleDraft extends PostgreSqlFirewallRule implements AzResource.Draft<PostgreSqlFirewallRule, FirewallRule> {
    @Getter
    @Nullable
    private final PostgreSqlFirewallRule origin;
    @Nullable
    private Config config;

    PostgreSqlFirewallRuleDraft(@Nonnull String name, @Nonnull PostgreSqlFirewallRuleModule module) {
        super(name, module);
        this.origin = null;
    }

    PostgreSqlFirewallRuleDraft(@Nonnull PostgreSqlFirewallRule origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Override
    @AzureOperation(
        name = "resource.create_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public FirewallRule createResourceInAzure() {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        final PostgreSqlServer server = this.getParent();
        final PostgreSqlManager manager = Objects.requireNonNull(server.getParent().getRemote());
        final FirewallRule.DefinitionStages.WithCreate withCreate = manager.firewallRules().define(this.getName())
            .withExistingServer(this.getResourceGroupName(), server.getName())
            .withStartIpAddress(this.getStartIpAddress())
            .withEndIpAddress(this.getEndIpAddress());
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating firewall rule \"{0}\"...", this.getName()));
        final FirewallRule rule = withCreate.create();
        messager.success(AzureString.format("Firewall rule \"{0}\" is successfully created.", this.getName()));
        return rule;
    }

    @Override
    @AzureOperation(
        name = "resource.update_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public FirewallRule updateResourceInAzure(@NotNull FirewallRule origin) {
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        final Optional<String> modifiedStartIp = Optional.ofNullable(this.getStartIpAddress()).filter(n -> !Objects.equals(n, super.getStartIpAddress()));
        final Optional<String> modifiedEndIp = Optional.ofNullable(this.getEndIpAddress()).filter(n -> !Objects.equals(n, super.getEndIpAddress()));
        if (modifiedStartIp.isPresent() || modifiedEndIp.isPresent()) {
            final PostgreSqlServer server = this.getParent();
            final PostgreSqlManager manager = Objects.requireNonNull(server.getParent().getRemote());
            final FirewallRule.Update update = manager.firewallRules().get(this.getResourceGroupName(), server.getName(), this.getName()).update();
            modifiedStartIp.ifPresent(update::withStartIpAddress);
            modifiedEndIp.ifPresent(update::withEndIpAddress);
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating firewall rule \"{0}\"...", this.getName()));
            final FirewallRule rule = update.apply();
            messager.success(AzureString.format("Firewall rule \"{0}\" is successfully updated.", this.getName()));
            return rule;
        }
        return origin;
    }

    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    @Nullable
    @Override
    public String getStartIpAddress() {
        return Optional.ofNullable(this.config).map(Config::getStartIpAddress).orElseGet(super::getStartIpAddress);
    }

    public void setStartIpAddress(String startIpAddress) {
        this.ensureConfig().setStartIpAddress(startIpAddress);
    }

    @Nullable
    @Override
    public String getEndIpAddress() {
        return Optional.ofNullable(this.config).map(Config::getEndIpAddress).orElseGet(super::getEndIpAddress);
    }

    public void setEndIpAddress(String endIpAddress) {
        this.ensureConfig().setEndIpAddress(endIpAddress);
    }

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) ||
            Objects.isNull(this.config.getStartIpAddress()) || Objects.equals(this.config.getStartIpAddress(), super.getStartIpAddress()) ||
            Objects.isNull(this.config.getEndIpAddress()) || Objects.equals(this.config.getEndIpAddress(), super.getEndIpAddress());
        return !notModified;
    }

    @Data
    private static class Config {
        private String startIpAddress;
        private String endIpAddress;
    }
}