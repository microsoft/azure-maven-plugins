/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccountModule;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountConnectionStrings;
import com.mongodb.ConnectionString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MongoCosmosDBAccount extends CosmosDBAccount {

    private final MongoDatabaseModule mongoDatabaseModule;

    public MongoCosmosDBAccount(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull CosmosDBAccountModule module) {
        super(name, resourceGroupName, module);
        this.mongoDatabaseModule = new MongoDatabaseModule(this);
    }

    public MongoCosmosDBAccount(@Nonnull CosmosDBAccount account) {
        super(account);
        this.mongoDatabaseModule = new MongoDatabaseModule(this);
    }

    public MongoCosmosDBAccount(@Nonnull com.azure.resourcemanager.cosmos.models.CosmosDBAccount remote, @Nonnull CosmosDBAccountModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.mongoDatabaseModule = new MongoDatabaseModule(this);
    }

    public ConnectionString getPrimaryConnectionString() {
        return Optional.ofNullable(listConnectionStrings())
                .map(DatabaseAccountConnectionStrings::getPrimaryConnectionString).map(ConnectionString::new).orElse(null);
    }

    public List<String> getHosts() {
        return Optional.ofNullable(getPrimaryConnectionString()).map(ConnectionString::getHosts).orElse(null);
    }

    public String getContactPoint() {
        final List<String> hosts = getHosts();
        if (CollectionUtils.isEmpty(hosts)) {
            return null;
        }
        return StringUtils.substringAfterLast(hosts.get(0), ":");
    }

    public Integer getPort() {
        final List<String> hosts = getHosts();
        if (CollectionUtils.isEmpty(hosts)) {
            return null;
        }
        return Integer.valueOf(StringUtils.substringAfterLast(hosts.get(0), ":"));
    }

    public String getUserName() {
        return getPrimaryConnectionString().getUsername();
    }

    public boolean isSslEnabled() {
        return getPrimaryConnectionString().getSslEnabled();
    }

    public MongoDatabaseModule mongoDatabases() {
        return this.mongoDatabaseModule;
    }

    @Override
    public @NotNull List<AbstractAzResourceModule<?, CosmosDBAccount, ?>> getSubModules() {
        return Collections.singletonList(mongoDatabaseModule);
    }
}
