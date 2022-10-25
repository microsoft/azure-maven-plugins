/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.resourcemanager.cosmos.fluent.models.MongoDBCollectionGetResultsInner;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosCollection;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocumentModule;
import com.microsoft.azure.toolkit.lib.cosmos.model.MongoDatabaseAccountConnectionString;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import lombok.Getter;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDocumentModule.MONGO_ID_KEY;

public class MongoCollection extends AbstractAzResource<MongoCollection, MongoDatabase, MongoDBCollectionGetResultsInner>
        implements Deletable, ICosmosCollection, ICosmosDocumentModule<MongoDocument> {

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

    @NotNull
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
        final MongoDocumentDraft documentDraft = this.documentModule.create(id, getResourceGroupName());
        documentDraft.setDraftDocument(document);
        return documentDraft.commit();
    }

    @Override
    public List<MongoDocument> listDocuments() {
        return documents().list();
    }

    @Override
    public long getDocumentCount() {
        return Optional.ofNullable(getClient()).map(client -> client.countDocuments()).orElse(0L);
    }

    @Override
    protected void updateAdditionalProperties(@Nullable MongoDBCollectionGetResultsInner newRemote, @Nullable MongoDBCollectionGetResultsInner oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        this.collection = getDocumentClient();
    }

    public synchronized com.mongodb.client.MongoCollection<Document> getClient() {
        if (Objects.isNull(this.collection)) {
            this.collection = getDocumentClient();
        }
        return this.collection;
    }

    private com.mongodb.client.MongoCollection<Document> getDocumentClient() {
        try {
            final MongoDatabase database = this.getParent();
            final MongoCosmosDBAccount account = (MongoCosmosDBAccount) database.getParent();
            final MongoDatabaseAccountConnectionString mongoConnectionString = Objects.requireNonNull(account.getMongoConnectionString());
            final MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoConnectionString.getConnectionString()));
            return mongoClient.getDatabase(database.getName()).getCollection(this.getName());
        } catch (Throwable e) {
            // swallow exception to load data client
            return null;
        }
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull MongoDBCollectionGetResultsInner remote) {
        return Status.RUNNING;
    }
}
