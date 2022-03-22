/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.sqlserver;

import com.azure.resourcemanager.sql.SqlServerManager;
import com.azure.resourcemanager.sql.models.SqlServer;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.database.DatabaseServerConfig;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class MicrosoftSqlServerDraft extends MicrosoftSqlServer implements AzResource.Draft<MicrosoftSqlServer, SqlServer> {
    @Getter
    @Setter
    private boolean committed;
    @Getter
    @Nullable
    private final MicrosoftSqlServer origin;
    @Nullable
    private Config config;

    MicrosoftSqlServerDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull MicrosoftSqlServerModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    MicrosoftSqlServerDraft(@Nonnull MicrosoftSqlServer origin) {
        super(origin);
        this.origin = origin;
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

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.create_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public SqlServer createResourceInAzure() {
        assert this.config != null;
        final SqlServerManager manager = Objects.requireNonNull(this.getParent().getRemote());
        final SqlServer.DefinitionStages.WithCreate create = manager.sqlServers()
            .define(this.getName())
            .withRegion(Objects.requireNonNull(this.getRegion(), "'region' is required to create Sql server").getName())
            .withExistingResourceGroup(this.getResourceGroupName())
            .withAdministratorLogin(this.getAdminName())
            .withAdministratorPassword(this.getAdminPassword());
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating SQL server ({0})...", this.getName()));
        final SqlServer remote = this.doModify(() -> create.create(), Status.CREATING);
        messager.success(AzureString.format("SQL server({0}) is successfully created.", this.getName()));
        return this.updateResourceInAzure(Objects.requireNonNull(remote));
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "resource.update_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public SqlServer updateResourceInAzure(@Nonnull SqlServer origin) {
        if (this.isAzureServiceAccessAllowed() != super.isAzureServiceAccessAllowed() ||
            this.isLocalMachineAccessAllowed() != super.isLocalMachineAccessAllowed()) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating firewall rules of SQL server ({0})...", this.getName()));
            this.firewallRules().toggleAzureServiceAccess(this.isAzureServiceAccessAllowed());
            this.firewallRules().toggleLocalMachineAccess(this.isLocalMachineAccessAllowed());
            messager.success(AzureString.format("Firewall rules of SQL server({0}) is successfully updated.", this.getName()));
        }
        return origin;
    }

    @Nonnull
    private synchronized Config ensureConfig() {
        this.config = Optional.ofNullable(this.config).orElseGet(Config::new);
        return this.config;
    }

    @Nullable
    @Override
    public String getAdminName() {
        return Optional.ofNullable(this.config).map(Config::getAdminName)
            .orElseGet(() -> Optional.ofNullable(origin).map(MicrosoftSqlServer::getAdminName).orElse(null));
    }

    @Nullable
    public String getAdminPassword() {
        return Optional.ofNullable(this.config).map(Config::getAdminPassword).orElse(null);
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(config).map(Config::getRegion)
            .orElseGet(() -> Optional.ofNullable(origin).map(MicrosoftSqlServer::getRegion).orElse(null));
    }

    @Nullable
    @Override
    public String getVersion() {
        return Optional.ofNullable(this.config).map(Config::getVersion)
            .orElseGet(() -> Optional.ofNullable(origin).map(MicrosoftSqlServer::getVersion).orElse(null));
    }

    @Nullable
    @Override
    public String getFullyQualifiedDomainName() {
        return Optional.ofNullable(this.config).map(Config::getFullyQualifiedDomainName)
            .orElseGet(() -> Optional.ofNullable(origin).map(MicrosoftSqlServer::getFullyQualifiedDomainName).orElse(null));
    }

    @Override
    public boolean isLocalMachineAccessAllowed() {
        return Optional.ofNullable(this.config).map(Config::isLocalMachineAccessAllowed)
            .orElseGet(() -> Optional.ofNullable(origin).map(MicrosoftSqlServer::isLocalMachineAccessAllowed).orElse(false));
    }

    @Override
    public boolean isAzureServiceAccessAllowed() {
        return Optional.ofNullable(this.config).map(Config::isAzureServiceAccessAllowed)
            .orElseGet(() -> Optional.ofNullable(origin).map(MicrosoftSqlServer::isAzureServiceAccessAllowed).orElse(false));
    }

    public void setAdminName(@Nullable String name) {
        this.ensureConfig().setAdminName(name);
    }

    public void setAdminPassword(@Nullable String password) {
        this.ensureConfig().setAdminPassword(password);
    }

    public void setRegion(@Nullable Region region) {
        this.ensureConfig().setRegion(region);
    }

    public void setVersion(@Nullable String version) {
        this.ensureConfig().setVersion(version);
    }

    public void setFullyQualifiedDomainName(@Nullable String name) {
        this.ensureConfig().setFullyQualifiedDomainName(name);
    }

    public void setLocalMachineAccessAllowed(boolean allowed) {
        this.ensureConfig().setLocalMachineAccessAllowed(allowed);
    }

    public void setAzureServiceAccessAllowed(boolean allowed) {
        this.ensureConfig().setAzureServiceAccessAllowed(allowed);
    }

    @Override
    public boolean isModified() {
        final boolean notModified = Objects.isNull(this.config) ||
            Objects.equals(this.config.isLocalMachineAccessAllowed(), super.isLocalMachineAccessAllowed()) ||
            Objects.equals(this.config.isAzureServiceAccessAllowed(), super.isAzureServiceAccessAllowed()) ||
            Objects.isNull(this.config.getAdminPassword()) ||
            Objects.isNull(this.config.getAdminName()) || Objects.equals(this.config.getAdminName(), super.getAdminName()) ||
            Objects.isNull(this.config.getRegion()) || Objects.equals(this.config.getRegion(), super.getRegion()) ||
            Objects.isNull(this.config.getVersion()) || Objects.equals(this.config.getVersion(), super.getVersion()) ||
            Objects.isNull(this.config.getFullyQualifiedDomainName()) ||
            Objects.equals(this.config.getFullyQualifiedDomainName(), super.getFullyQualifiedDomainName());
        return !notModified;
    }

    @Data
    private static class Config {
        @Nullable
        private String adminName;
        @Nullable
        private String adminPassword;
        @Nullable
        private Region region;
        @Nullable
        private String version;
        @Nullable
        private String fullyQualifiedDomainName;
        private boolean azureServiceAccessAllowed;
        private boolean localMachineAccessAllowed;
    }
}