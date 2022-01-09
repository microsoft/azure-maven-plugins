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
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class PostgreSqlFirewallRuleDraft extends PostgreSqlFirewallRule implements AzResource.Draft<PostgreSqlFirewallRule, FirewallRule> {
    @Nullable
    private Config config;

    PostgreSqlFirewallRuleDraft(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull PostgreSqlFirewallRuleModule module) {
        super(name, resourceGroup, module);
        this.setStatus(Status.DRAFT);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Override
    public FirewallRule createResourceInAzure() {
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
    public FirewallRule updateResourceInAzure(@NotNull FirewallRule origin) {
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

    @Data
    private static class Config {
        private String startIpAddress;
        private String endIpAddress;
    }
}