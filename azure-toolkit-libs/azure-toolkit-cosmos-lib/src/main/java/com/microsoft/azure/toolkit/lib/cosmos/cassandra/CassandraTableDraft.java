/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.cassandra;

import com.azure.core.util.Context;
import com.azure.resourcemanager.cosmos.fluent.CassandraResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.CassandraTableGetResultsInner;
import com.azure.resourcemanager.cosmos.models.CassandraSchema;
import com.azure.resourcemanager.cosmos.models.CassandraTableCreateUpdateParameters;
import com.azure.resourcemanager.cosmos.models.CassandraTableResource;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.cosmos.model.ThroughputConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class CassandraTableDraft extends CassandraTable implements
        AzResource.Draft<CassandraTable, CassandraTableGetResultsInner> {

    private CassandraTableConfig config;

    protected CassandraTableDraft(@NotNull String name, @NotNull String resourceGroupName, @NotNull CassandraTableModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @NotNull
    @Override
    public CassandraTableGetResultsInner createResourceInAzure() {
        final CassandraResourcesClient cassandraResourcesClient = Objects.requireNonNull(((CassandraTableModule) Objects.requireNonNull(getModule())).getClient());

        final CassandraTableResource sqlContainerResource = new CassandraTableResource()
                .withId(ensureConfig().getTableId())
                .withSchema(new CassandraSchema());
        final CassandraTableCreateUpdateParameters parameters = new CassandraTableCreateUpdateParameters()
                .withLocation(Objects.requireNonNull(this.getParent().getRegion()).getName())
                .withResource(sqlContainerResource);
        parameters.withOptions(ensureConfig().toCreateUpdateOptions());
        AzureMessager.getMessager().info(AzureString.format("Start creating Cassandra table({0})...", this.getName()));
        final CassandraTableGetResultsInner result = cassandraResourcesClient.createUpdateCassandraTable(this.getResourceGroupName(), this.getParent().getParent().getName(),
                this.getParent().getName(), this.getName(), parameters, Context.NONE);
        AzureMessager.getMessager().success(AzureString.format("Cassandra table({0}) is successfully created.", this.getName()));
        return result;
    }

    @NotNull
    @Override
    public CassandraTableGetResultsInner updateResourceInAzure(@NotNull CassandraTableGetResultsInner origin) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isModified() {
        return config != null && ObjectUtils.anyNotNull(config.getTableId(), config.getSchema());
    }

    @Nullable
    @Override
    public CassandraTable getOrigin() {
        return null;
    }

    private CassandraTableConfig ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(CassandraTableConfig::new);
        return this.config;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CassandraTableConfig extends ThroughputConfig {
        private String tableId;
        private String schema;
    }
}
