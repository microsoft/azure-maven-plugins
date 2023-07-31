/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.resourcemanager.cosmos.fluent.models.MongoDBCollectionGetResultsInner;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosCollection;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocumentContainer;
import lombok.Getter;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDocumentModule.MONGO_ID_KEY;

public class MongoCollection extends AbstractAzResource<MongoCollection, MongoDatabase, MongoDBCollectionGetResultsInner>
        implements Deletable, ICosmosCollection, ICosmosDocumentContainer<MongoDocument> {

    @Getter
    private com.mongodb.client.MongoCollection<Document> collection;
    @Getter
    private final MongoDocumentModule documentModule;

    protected MongoCollection(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull MongoCollectionModule module) {
        super(name, resourceGroupName, module);
        this.documentModule = new MongoDocumentModule(this);
    }

    protected MongoCollection(@Nonnull MongoDBCollectionGetResultsInner remote, @Nonnull MongoCollectionModule module) {
        super(remote.name(), module);
        this.documentModule = new MongoDocumentModule(this);
    }

    protected MongoCollection(@Nonnull MongoCollection collection) {
        super(collection);
        this.collection = collection.collection;
        this.documentModule = collection.documentModule;
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(documentModule);
    }

    public MongoDocumentModule documents() {
        return this.documentModule;
    }

    @Override
    public MongoDocument importDocument(@Nonnull final ObjectNode node) {
        final Document document = Document.parse(node.toPrettyString());
        if (document.get(MONGO_ID_KEY) == null) {
            document.put(MONGO_ID_KEY, new ObjectId());
        }
        final String id = document.get(MONGO_ID_KEY).toString();
        final String sharedKey = this.getSharedKey();
        if (StringUtils.isNotEmpty(sharedKey) && !IteratorUtils.contains(node.fieldNames(), sharedKey)) {
            throw new AzureToolkitRuntimeException(String.format("Document does not contain shard key at '%s'", sharedKey));
        }
        final MongoDocumentDraft documentDraft = this.documentModule.create(id, getResourceGroupName());
        documentDraft.setDraftDocument(document);
        final boolean existing = this.getDocumentModule().exists(documentDraft.getName(), documentDraft.getResourceGroupName());
        final MongoDocument result = documentDraft.commit();
        final AzureString importMessage = AzureString.format("Import document to Mongo collection %s successfully.", this.getName());
        final AzureString updateMessage = AzureString.format("Update document %s in Mongo collection %s successfully.", id, this.getName());
        AzureMessager.getMessager().info(existing ? updateMessage : importMessage);
        return result;
    }

    @Nullable
    public String getSharedKey() {
        return Optional.ofNullable(getRemote())
                .map(remote -> remote.resource().shardKey())
                .filter(MapUtils::isNotEmpty)
                .map(map -> map.keySet().iterator().next())
                .orElse(null);
    }

    public synchronized com.mongodb.client.MongoCollection<Document> getClient() {
        if (Objects.isNull(this.collection)) {
            this.collection = getDocumentClient();
        }
        return this.collection;
    }

    @Override
    protected void updateAdditionalProperties(@Nullable MongoDBCollectionGetResultsInner newRemote, @Nullable MongoDBCollectionGetResultsInner oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        if (Objects.nonNull(newRemote)) {
            this.collection = getDocumentClient();
        } else {
            this.collection = null;
        }
    }

    private com.mongodb.client.MongoCollection<Document> getDocumentClient() {
        try {
            final MongoDatabase database = this.getParent();
            final MongoCosmosDBAccount account = (MongoCosmosDBAccount) database.getParent();
            return account.getClient().getDatabase(database.getName()).getCollection(this.getName());
        } catch (Throwable e) {
            // swallow exception to load data client
            return null;
        }
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull MongoDBCollectionGetResultsInner remote) {
        return Status.RUNNING;
    }
}
