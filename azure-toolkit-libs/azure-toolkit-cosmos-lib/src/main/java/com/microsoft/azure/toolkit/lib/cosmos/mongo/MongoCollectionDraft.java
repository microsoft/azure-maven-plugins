/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.core.util.Context;
import com.azure.resourcemanager.cosmos.fluent.MongoDBResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.MongoDBCollectionGetResultsInner;
import com.azure.resourcemanager.cosmos.models.MongoDBCollectionCreateUpdateParameters;
import com.azure.resourcemanager.cosmos.models.MongoDBCollectionResource;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.cosmos.model.ThroughputConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MongoCollectionDraft extends MongoCollection implements
        AzResource.Draft<MongoCollection, MongoDBCollectionGetResultsInner> {

    @Setter
    private MongoCollectionConfig config;

    protected MongoCollectionDraft(@NotNull String name, @NotNull String resourceGroupName, @NotNull MongoCollectionModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @NotNull
    @Override
    public MongoDBCollectionGetResultsInner createResourceInAzure() {
        final MongoDBResourcesClient mongoDBResourcesClient = Objects.requireNonNull(((MongoCollectionModule) Objects.requireNonNull(getModule())).getClient());
        final Map<String, String> shardKey = StringUtils.isEmpty(ensureConfig().getShardKey()) ? null : Collections.singletonMap(ensureConfig().getShardKey(), "Hash");
        final MongoDBCollectionResource sqlContainerResource = new MongoDBCollectionResource()
                .withId(ensureConfig().getCollectionId())
                .withShardKey(shardKey);
        final MongoDBCollectionCreateUpdateParameters parameters = new MongoDBCollectionCreateUpdateParameters()
                .withLocation(Objects.requireNonNull(this.getParent().getRegion()).getName())
                .withResource(sqlContainerResource);
        parameters.withOptions(ensureConfig().toCreateUpdateOptions());
        AzureMessager.getMessager().info(AzureString.format("Start creating MongoDB collection({0})...", this.getName()));
        final MongoDBCollectionGetResultsInner result = mongoDBResourcesClient.createUpdateMongoDBCollection(this.getResourceGroupName(), this.getParent().getParent().getName(),
                this.getParent().getName(), this.getName(), parameters, Context.NONE);
        AzureMessager.getMessager().success(AzureString.format("MongoDB collection({0}) is successfully created.", this.getName()));
        return result;
    }

    @NotNull
    @Override
    public MongoDBCollectionGetResultsInner updateResourceInAzure(@NotNull MongoDBCollectionGetResultsInner origin) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isModified() {
        return config != null && ObjectUtils.anyNotNull(config.getCollectionId(), config.getShardKey(), config.getThroughput(), config.getMaxThroughput());
    }

    @Nullable
    @Override
    public MongoCollection getOrigin() {
        return null;
    }

    private MongoCollectionConfig ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(MongoCollectionConfig::new);
        return this.config;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MongoCollectionConfig extends ThroughputConfig {
        private String collectionId;
        private String shardKey;

        public static MongoCollectionConfig getDefaultConfig() {
            final MongoCollectionConfig result = new MongoCollectionConfig();
            result.setCollectionId(String.format("collection-%s", Utils.getTimestamp()));
            return result;
        }
    }
}
