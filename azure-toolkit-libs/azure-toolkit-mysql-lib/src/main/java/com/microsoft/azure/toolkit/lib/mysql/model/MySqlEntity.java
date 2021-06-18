/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.mysql.model;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.mysql.models.Server;
import com.azure.resourcemanager.mysql.models.Sku;
import com.azure.resourcemanager.mysql.models.SslEnforcementEnum;
import com.azure.resourcemanager.mysql.models.StorageProfile;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.mysql.service.AbstractAzureEntityManager;

import javax.annotation.Nonnull;
import java.util.Optional;

public class MySqlEntity extends AbstractAzureEntityManager.RemoteAwareResourceEntity<Server> {

    public MySqlEntity(Server server) {
        this.resourceId = ResourceId.fromString(server.id());
        this.remote = server;
    }

    private @Nonnull ResourceId resourceId;

    public String getId() {
        return resourceId.id();
    }

    public String getResourceGroup() {
        return resourceId.resourceGroupName();
    }

    @Override
    public String getName() {
        return resourceId.name();
    }

    @Override
    public String getSubscriptionId() {
        return resourceId.subscriptionId();
    }

    private Optional<Server> remoteOptional() {
        return Optional.ofNullable(this.remote);
    }

    public Region getRegion() {
        return remoteOptional().map(remote -> Region.fromName(remote.regionName())).orElse(null);
    }

    public Region getAdministratorLoginName() {
        return remoteOptional().map(remote -> Region.fromName(remote.administratorLogin())).orElse(null);
    }

    public String getVersion() {
        return remoteOptional().map(Server::version).map(ExpandableStringEnum::toString).orElse(null);
    }

    public String getState() {
        return remoteOptional().map(Server::userVisibleState).map(ExpandableStringEnum::toString).orElse(null);
    }

    public String getFullyQualifiedDomainName() {
        return remoteOptional().map(Server::fullyQualifiedDomainName).orElse(null);
    }

    public String getType() {
        return remoteOptional().map(Server::type).orElse(null);
    }

    public String getSkuTier() {
        return remoteOptional().map(Server::sku).map(Sku::tier).map(ExpandableStringEnum::toString).orElse(null);
    }

    public int getVCore() {
        return remoteOptional().map(Server::sku).map(Sku::capacity).orElse(0);
    }

    public int getStorageInMB() {
        return remoteOptional().map(Server::storageProfile).map(StorageProfile::storageMB).orElse(0);
    }

    public String getSslEnforceStatus() {
        return remoteOptional().map(Server::sslEnforcement).map(SslEnforcementEnum::name).orElse(null);
    }
}
