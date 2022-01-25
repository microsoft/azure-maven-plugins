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
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.database.DatabaseServerConfig;
import lombok.Data;
import lombok.Getter;
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
    @Getter
    @Nullable
    private final MySqlServer origin;
    @Nullable
    private Config config;

    MySqlServerDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull MySqlServerModule module) {
        super(name, resourceGroupName, module);
        this.origin = null;
    }

    MySqlServerDraft(@Nonnull MySqlServer origin) {
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
    @AzureOperation(
        name = "resource.create_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public Server createResourceInAzure() {
        assert this.config != null;
        AzureTelemetry.getContext().setProperty("resourceType", this.getFullResourceType());
        AzureTelemetry.getContext().setProperty("subscriptionId", this.getSubscriptionId());
        final MySqlManager manager = Objects.requireNonNull(this.getParent().getRemote());

        final ServerPropertiesForDefaultCreate parameters = new ServerPropertiesForDefaultCreate()
            .withAdministratorLogin(this.getAdminName())
            .withAdministratorLoginPassword(this.getAdminPassword())
            .withVersion(validateServerVersion(this.getVersion()));
        final List<PerformanceTierProperties> tiers = manager.locationBasedPerformanceTiers()
            .list(this.getRegion().getName()).stream().collect(Collectors.toList());
        final PerformanceTierProperties tier = tiers.stream().filter(e -> CollectionUtils.isNotEmpty(e.serviceLevelObjectives()))
            .min(Comparator.comparingInt(this::getTierPriority))
            .orElseThrow(() -> new AzureToolkitRuntimeException("MySQL is not available in this location for your subscription."));
        final Sku sku = new Sku().withName(tier.serviceLevelObjectives().get(0).id());
        // create server
        final Server.DefinitionStages.WithCreate create = manager.servers().define(this.getName())
            .withRegion(this.getRegion().getName())
            .withExistingResourceGroup(this.getResourceGroupName())
            .withProperties(parameters)
            .withSku(sku);
        final IAzureMessager messager = AzureMessager.getMessager();
        messager.info(AzureString.format("Start creating MySQL server ({0})...", this.getName()));
        final Server remote = this.doModify(() -> create.create(), Status.CREATING);
        messager.success(AzureString.format("MySQL server({0}) is successfully created.", this.getName()));
        return this.updateResourceInAzure(remote);
    }

    @Override
    @AzureOperation(
        name = "resource.update_resource.resource|type",
        params = {"this.getName()", "this.getResourceTypeName()"},
        type = AzureOperation.Type.SERVICE
    )
    public Server updateResourceInAzure(@Nonnull Server origin) {
        // TODO: update other properties
        if (this.isAzureServiceAccessAllowed() != super.isAzureServiceAccessAllowed() ||
            this.isLocalMachineAccessAllowed() != super.isLocalMachineAccessAllowed()) {
            final IAzureMessager messager = AzureMessager.getMessager();
            messager.info(AzureString.format("Start updating firewall rules of MySQL server ({0})...", this.getName()));
            this.firewallRules().toggleAzureServiceAccess(this.isAzureServiceAccessAllowed());
            this.firewallRules().toggleLocalMachineAccess(this.isLocalMachineAccessAllowed());
            messager.success(AzureString.format("Firewall rules of MySQL server({0}) is successfully updated.", this.getName()));
        }
        return origin;
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
        private String adminName;
        private String adminPassword;
        private Region region;
        private String version;
        private String fullyQualifiedDomainName;
        private boolean azureServiceAccessAllowed;
        private boolean localMachineAccessAllowed;
    }
}