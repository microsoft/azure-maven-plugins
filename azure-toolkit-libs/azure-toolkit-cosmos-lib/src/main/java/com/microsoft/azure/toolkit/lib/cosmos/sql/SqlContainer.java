/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.resourcemanager.cosmos.fluent.models.SqlContainerGetResultsInner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosCollection;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocumentModule;
import com.microsoft.azure.toolkit.lib.cosmos.model.SqlDatabaseAccountConnectionString;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class SqlContainer extends AbstractAzResource<SqlContainer, SqlDatabase, SqlContainerGetResultsInner>
        implements Deletable, ICosmosCollection, ICosmosDocumentModule<SqlDocument> {
    private CosmosContainer container;
    private CosmosContainerResponse containerResponse;
    @Getter
    private final SqlDocumentModule documentModule;

    protected SqlContainer(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull SqlContainerModule module) {
        super(name, resourceGroupName, module);
        this.documentModule = new SqlDocumentModule(this);
    }

    protected SqlContainer(@Nonnull SqlContainerGetResultsInner remote, @Nonnull SqlContainerModule module) {
        super(remote.name(), module);
        this.documentModule = new SqlDocumentModule(this);
    }

    protected SqlContainer(@Nonnull SqlContainer collection) {
        super(collection);
        this.container = collection.container;
        this.containerResponse = collection.containerResponse;
        this.documentModule = new SqlDocumentModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(documentModule);
    }

    @Override
    protected void updateAdditionalProperties(@Nullable SqlContainerGetResultsInner newRemote, @Nullable SqlContainerGetResultsInner oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        doGetClient();
    }

    @Override
    public SqlDocument importDocument(@Nonnull final ObjectNode node) {
        if (node.get("id") == null) {
            node.put("id", UUID.randomUUID().toString());
        }
        final String id = node.get("id").asText();
        final String partitionKey = Optional.ofNullable(node.get(getPartitionKey())).map(JsonNode::asText).orElse(StringUtils.EMPTY);
        final SqlDocumentDraft documentDraft = this.documentModule.create(String.format("%s#%s", id, partitionKey), getResourceGroupName());
        documentDraft.setDraftDocument(node);
        return documentDraft.commit();
    }

    @Override
    public List<SqlDocument> listDocuments() {
        return getDocumentModule().list();
    }

    public String getPartitionKey() {
        return Optional.ofNullable(this.containerResponse)
                .map(CosmosContainerResponse::getProperties)
                .map(p -> p.getPartitionKeyDefinition().getPaths().get(0))
                .map(key -> key.startsWith("/") ? key.substring(1) : key)
                .orElse(null);
    }

    public synchronized CosmosContainer getClient() {
        if (Objects.isNull(this.container)) {
            doGetClient();
        }
        return this.container;
    }

    private void doGetClient() {
        try {
            final SqlDatabase sqlDatabase = this.getParent();
            final SqlCosmosDBAccount account = (SqlCosmosDBAccount) sqlDatabase.getParent();
            final SqlDatabaseAccountConnectionString connectionString = account.getSqlAccountConnectionString();
            final CosmosClient cosmosClient = new CosmosClientBuilder()
                    .endpoint(account.getDocumentEndpoint())
                    .key(connectionString.getKey())
                    .preferredRegions(Collections.singletonList(Objects.requireNonNull(account.getRegion()).getName()))
                    .consistencyLevel(ConsistencyLevel.EVENTUAL)
                    .buildClient();
            this.container = cosmosClient.getDatabase(sqlDatabase.getName()).getContainer(this.getName());
            this.containerResponse = container.read();
        } catch (Throwable e) {
            // swallow exception to load data client
        }
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull SqlContainerGetResultsInner remote) {
        return Status.RUNNING;
    }
}
