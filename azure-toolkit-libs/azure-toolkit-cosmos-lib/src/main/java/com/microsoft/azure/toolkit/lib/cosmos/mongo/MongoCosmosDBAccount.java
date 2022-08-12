/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccountModule;
import com.microsoft.azure.toolkit.lib.cosmos.model.CosmosDBAccountConnectionString;
import com.microsoft.azure.toolkit.lib.cosmos.model.MongoDatabaseAccountConnectionString;
import com.mongodb.ConnectionString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    @Nullable
    public ConnectionString getPrimaryConnectionString() {
        return Optional.ofNullable(listConnectionStrings().getPrimaryConnectionString()).map(ConnectionString::new).orElse(null);
    }

    @Nonnull
    public MongoDatabaseAccountConnectionString getMongoConnectionString() {
        return Optional.ofNullable(listConnectionStrings().getPrimaryConnectionString())
                .map(MongoDatabaseAccountConnectionString::fromConnectionString)
                .orElseGet(MongoDatabaseAccountConnectionString::new);
    }

    @Nullable
    public List<String> getHosts() {
        return getMongoConnectionString().getHosts();
    }

    @Nullable
    public String getContactPoint() {
        return getMongoConnectionString().getHost();
    }

    @Nullable
    public Integer getPort() {
        return getMongoConnectionString().getPort();
    }

    @Nullable
    public String getUserName() {
        return getMongoConnectionString().getUsername();
    }

    @Nullable
    public Boolean isSslEnabled() {
        return getMongoConnectionString().getSslEnabled();
    }

    public MongoDatabaseModule mongoDatabases() {
        return this.mongoDatabaseModule;
    }

    @Nonnull
    @Override
    public CosmosDBAccountConnectionString getCosmosDBAccountPrimaryConnectionString() {
        return getMongoConnectionString();
    }

    @Override
    public @NotNull List<AbstractAzResourceModule<?, CosmosDBAccount, ?>> getSubModules() {
        return Collections.singletonList(mongoDatabaseModule);
    }
}
