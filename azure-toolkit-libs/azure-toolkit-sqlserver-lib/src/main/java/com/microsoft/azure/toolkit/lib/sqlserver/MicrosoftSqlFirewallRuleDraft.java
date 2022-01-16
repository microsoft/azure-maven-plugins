/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.sql.models.SqlFirewallRule;
import com.azure.resourcemanager.sql.models.SqlFirewallRuleOperations;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class MicrosoftSqlFirewallRuleDraft extends MicrosoftSqlFirewallRule implements AzResource.Draft<MicrosoftSqlFirewallRule, SqlFirewallRule> {
    @Nullable
    private Config config;

    MicrosoftSqlFirewallRuleDraft(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull MicrosoftSqlFirewallRuleModule module) {
        super(name, resourceGroup, module);
        this.setStatus(Status.DRAFT);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Override
    public SqlFirewallRule createResourceInAzure() {
        final SqlServer server = Objects.requireNonNull(this.getParent().getRemote());
        SqlFirewallRuleOperations.DefinitionStages.WithCreate withCreate = server.firewallRules().define(this.getName())
            .withIpAddressRange(this.getStartIpAddress(), this.getEndIpAddress());
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating firewall rule \"{0}\"...", this.getName()));
        final SqlFirewallRule rule = withCreate.create();
        messager.success(AzureString.format("Firewall rule \"{0}\" is successfully created.", this.getName()));
        return rule;
    }

    @Override
    public SqlFirewallRule updateResourceInAzure(@Nonnull SqlFirewallRule origin) {
        final Optional<String> modifiedStartIp = Optional.ofNullable(this.getStartIpAddress()).filter(n -> !Objects.equals(n, super.getStartIpAddress()));
        final Optional<String> modifiedEndIp = Optional.ofNullable(this.getEndIpAddress()).filter(n -> !Objects.equals(n, super.getEndIpAddress()));
        if (modifiedStartIp.isPresent() || modifiedEndIp.isPresent()) {
            final MicrosoftSqlServer server = this.getParent();
            final SqlServer manager = Objects.requireNonNull(server.getRemote());
            final SqlFirewallRule.Update update = manager.firewallRules().get(this.getName()).update();
            modifiedStartIp.ifPresent(update::withStartIpAddress);
            modifiedEndIp.ifPresent(update::withEndIpAddress);
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating firewall rule \"{0}\"...", this.getName()));
            final SqlFirewallRule rule = update.apply();
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