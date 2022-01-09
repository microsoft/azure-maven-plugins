/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql;

import com.azure.resourcemanager.mysql.MySqlManager;
import com.azure.resourcemanager.mysql.models.PerformanceTierProperties;
import com.azure.resourcemanager.mysql.models.Server;
import com.azure.resourcemanager.mysql.models.ServerPropertiesForDefaultCreate;
import com.azure.resourcemanager.mysql.models.ServerVersion;
import com.azure.resourcemanager.mysql.models.Sku;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MySqlServerDraft extends MySqlServer implements AzResource.Draft<MySqlServer, Server> {
    @Nullable
    private Config config;

    MySqlServerDraft(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull MySqlServerModule module) {
        super(name, resourceGroup, module);
        this.setStatus(Status.DRAFT);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    private int getTierPriority(PerformanceTierProperties tier) {
        return StringUtils.equals("Basic", tier.id()) ? 1 :
            StringUtils.equals("GeneralPurpose", tier.id()) ? 2 : StringUtils.equals("MemoryOptimized", tier.id()) ? 3 : 4;
    }

    private ServerVersion validateServerVersion(String version) {
        if (StringUtils.isNotBlank(version)) {
            final ServerVersion res = ServerVersion.fromString(version);
            if (res == null) {
                throw new AzureToolkitRuntimeException(String.format("Invalid postgre version '%s'.", version));
            }
            return res;
        }
        return null;
    }

    @Override
    public Server createResourceInAzure() {
        assert this.config != null;
        final MySqlManager manager = Objects.requireNonNull(this.getParent().getRemote());

        final ServerPropertiesForDefaultCreate parameters = new ServerPropertiesForDefaultCreate()
            .withAdministratorLogin(this.getAdminName())
            .withAdministratorLoginPassword(this.getAdminPassword())
            .withVersion(validateServerVersion(this.getVersion()));
        final List<PerformanceTierProperties> tiers = manager.locationBasedPerformanceTiers()
            .list(this.getRegion().getName()).stream().collect(Collectors.toList());
        final PerformanceTierProperties tier = tiers.stream().filter(e -> CollectionUtils.isNotEmpty(e.serviceLevelObjectives()))
            .min(Comparator.comparingInt(this::getTierPriority))
            .orElseThrow(() -> new AzureToolkitRuntimeException("PostgreSQL is not available in this location for your subscription."));
        final Sku sku = new Sku().withName(tier.serviceLevelObjectives().get(0).id());
        // create server
        final Server.DefinitionStages.WithCreate create = manager.servers().define(this.getName())
            .withRegion(this.getRegion().getName())
            .withExistingResourceGroup(this.getResourceGroupName())
            .withProperties(parameters)
            .withSku(sku);
        final Server remote = this.doModify(() -> create.create(), Status.CREATING);
        this.firewallRules().toggleAzureServiceAccess(this.isAzureServiceAccessAllowed());
        this.firewallRules().toggleLocalMachineAccess(this.isLocalMachineAccessAllowed());
        return remote;
    }

    @Override
    public Server updateResourceInAzure(@Nonnull Server origin) {
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