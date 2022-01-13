/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.sql.SqlServerManager;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.DatabaseServerConfig;
import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class MicrosoftSqlServerDraft extends MicrosoftSqlServer implements AzResource.Draft<MicrosoftSqlServer, SqlServer> {
    @Nullable
    private Config config;

    MicrosoftSqlServerDraft(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull MicrosoftSqlServerModule module) {
        super(name, resourceGroup, module);
        this.setStatus(Status.DRAFT);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    public void setConfig(@Nonnull DatabaseServerConfig config) {
        this.setAdminName(config.getAdminName());
        this.setAdminPassword(config.getAdminPassword());
        this.setRegion(config.getRegion());
        this.setVersion(config.getVersion());
        this.setFullyQualifiedDomainName(config.getFullyQualifiedDomainName());
        this.setAzureServiceAccessAllowed(config.isAzureServiceAccessAllowed());
        this.setLocalMachineAccessAllowed(config.isLocalMachineAccessAllowed());
    }

    @Override
    public SqlServer createResourceInAzure() {
        assert this.config != null;
        final SqlServerManager manager = Objects.requireNonNull(this.getParent().getRemote());
        final SqlServer.DefinitionStages.WithCreate create = manager.sqlServers()
            .define(this.getName())
            .withRegion(this.getRegion().getName())
            .withExistingResourceGroup(this.getResourceGroupName())
            .withAdministratorLogin(this.getAdminName())
            .withAdministratorPassword(this.getAdminPassword());
        final SqlServer remote = this.doModify(() -> create.create(), Status.CREATING);
        if (this.isAzureServiceAccessAllowed() != super.isAzureServiceAccessAllowed() ||
            this.isLocalMachineAccessAllowed() != super.isLocalMachineAccessAllowed()) {
            final AzureString title = AzureOperationBundle.title("sqlserver.add_special_firewall_rule.server", this.getName());
            AzureTaskManager.getInstance().runInBackground(title, () -> {
                this.firewallRules().toggleAzureServiceAccess(this.isAzureServiceAccessAllowed());
                this.firewallRules().toggleLocalMachineAccess(this.isLocalMachineAccessAllowed());
            });
        }
        return remote;
    }

    @Override
    public SqlServer updateResourceInAzure(@Nonnull SqlServer origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    @Override
    public String getAdminName() {
        return Optional.ofNullable(this.config).map(Config::getAdminName).orElseGet(super::getAdminName);
    }

    public String getAdminPassword() {
        return Optional.ofNullable(this.config).map(Config::getAdminPassword).orElse(null);
    }

    @Nonnull
    public Region getRegion() {
        return Objects.requireNonNull(Optional.ofNullable(config).map(Config::getRegion).orElseGet(super::getRegion));
    }

    @Override
    public String getVersion() {
        return Optional.ofNullable(this.config).map(Config::getVersion).orElseGet(super::getVersion);
    }

    @Override
    public String getFullyQualifiedDomainName() {
        return Optional.ofNullable(this.config).map(Config::getFullyQualifiedDomainName).orElseGet(super::getFullyQualifiedDomainName);
    }

    @Override
    public boolean isLocalMachineAccessAllowed() {
        return Optional.ofNullable(this.config).map(Config::isLocalMachineAccessAllowed).orElseGet(super::isLocalMachineAccessAllowed);
    }

    @Override
    public boolean isAzureServiceAccessAllowed() {
        return Optional.ofNullable(this.config).map(Config::isAzureServiceAccessAllowed).orElseGet(super::isAzureServiceAccessAllowed);
    }

    public void setAdminName(String name) {
        this.ensureConfig().setAdminName(name);
    }

    public void setAdminPassword(String password) {
        this.ensureConfig().setAdminPassword(password);
    }

    public void setRegion(Region region) {
        this.ensureConfig().setRegion(region);
    }

    public void setVersion(String version) {
        this.ensureConfig().setVersion(version);
    }

    public void setFullyQualifiedDomainName(String name) {
        this.ensureConfig().setFullyQualifiedDomainName(name);
    }

    public void setLocalMachineAccessAllowed(boolean allowed) {
        this.ensureConfig().setLocalMachineAccessAllowed(allowed);
    }

    public void setAzureServiceAccessAllowed(boolean allowed) {
        this.ensureConfig().setAzureServiceAccessAllowed(allowed);
    }

    @Data
    private static class Config {
        private String adminName;
        private String adminPassword;
        private Region region;
        private String version;
        private String fullyQualifiedDomainName;
        private boolean azureServiceAccessAllowed;
        private boolean localMachineAccessAllowed;
    }
}