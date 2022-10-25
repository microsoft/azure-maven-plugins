/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.core.util.Context;
import com.azure.resourcemanager.cosmos.fluent.SqlResourcesClient;
import com.azure.resourcemanager.cosmos.fluent.models.SqlContainerGetResultsInner;
import com.azure.resourcemanager.cosmos.models.ContainerPartitionKey;
import com.azure.resourcemanager.cosmos.models.SqlContainerCreateUpdateParameters;
import com.azure.resourcemanager.cosmos.models.SqlContainerResource;
import com.azure.resourcemanager.cosmos.models.UniqueKey;
import com.azure.resourcemanager.cosmos.models.UniqueKeyPolicy;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.cosmos.model.ThroughputConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SqlContainerDraft extends SqlContainer implements
        AzResource.Draft<SqlContainer, SqlContainerGetResultsInner> {

    @Setter
    private SqlContainerConfig config;

    protected SqlContainerDraft(@NotNull String name, @NotNull String resourceGroupName, @NotNull SqlContainerModule module) {
        super(name, resourceGroupName, module);
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @NotNull
    @Override
    public SqlContainerGetResultsInner createResourceInAzure() {
        final SqlResourcesClient sqlResourcesClient = Objects.requireNonNull(((SqlContainerModule) Objects.requireNonNull(getModule())).getClient());
        final SqlContainerResource sqlContainerResource = new SqlContainerResource()
                .withId(ensureConfig().getContainerId())
                .withPartitionKey(new ContainerPartitionKey().withPaths(Collections.singletonList(ensureConfig().getPartitionKey())))
                .withUniqueKeyPolicy(getUniqueKeyPolicyFromConfig());
        final SqlContainerCreateUpdateParameters parameters = new SqlContainerCreateUpdateParameters()
                .withLocation(Objects.requireNonNull(this.getParent().getRegion()).getName())
                .withResource(sqlContainerResource);
        parameters.withOptions(ensureConfig().toCreateUpdateOptions());
        AzureMessager.getMessager().info(AzureString.format("Start creating SQL container({0})...", this.getName()));
        final SqlContainerGetResultsInner result = sqlResourcesClient.createUpdateSqlContainer(this.getResourceGroupName(), this.getParent().getParent().getName(),
                this.getParent().getName(), this.getName(), parameters, Context.NONE);
        AzureMessager.getMessager().success(AzureString.format("SQL container({0}) is successfully created.", this.getName()));
        return result;
    }

    private UniqueKeyPolicy getUniqueKeyPolicyFromConfig() {
        final List<String> uniqueKeys = ensureConfig().getUniqueKeys();
        if (CollectionUtils.isEmpty(uniqueKeys)) {
            return null;
        }
        final List<UniqueKey> uniqueKeyList = uniqueKeys.stream().map(path -> Arrays.asList(path.split(",")))
                .map(paths -> new UniqueKey().withPaths(paths))
                .collect(Collectors.toList());
        return new UniqueKeyPolicy().withUniqueKeys(uniqueKeyList);
    }

    @NotNull
    @Override
    public SqlContainerGetResultsInner updateResourceInAzure(@NotNull SqlContainerGetResultsInner origin) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public boolean isModified() {
        return config != null && ObjectUtils.anyNotNull(config.getContainerId(), config.getPartitionKey(), config.getUniqueKeys(),
                config.getThroughput(), config.getMaxThroughput());
    }

    @Nullable
    @Override
    public SqlContainer getOrigin() {
        return null;
    }

    private SqlContainerConfig ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(SqlContainerConfig::new);
        return this.config;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SqlContainerConfig extends ThroughputConfig {
        private String containerId;
        private String partitionKey;
        private List<String> uniqueKeys;
    }
}
