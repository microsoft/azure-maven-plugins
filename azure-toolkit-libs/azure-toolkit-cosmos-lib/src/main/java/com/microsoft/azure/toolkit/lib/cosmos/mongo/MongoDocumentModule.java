/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.azure.core.http.rest.Page;
import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.mongodb.client.MongoCursor;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class MongoDocumentModule extends AbstractAzResourceModule<MongoDocument, MongoCollection, Document> {

    public static final String MONGO_ID_KEY = "_id";

    public MongoDocumentModule(@Nonnull MongoCollection parent) {
        super("documents", parent);
    }

    @Nonnull
    @Override
    protected MongoDocument newResource(@Nonnull Document document) {
        final String id = Objects.requireNonNull(document.get(MONGO_ID_KEY)).toString();
        final MongoDocument mongoDocument = new MongoDocument(id, parent.getResourceGroupName(), this);
        mongoDocument.setRemote(document);
        return mongoDocument;
    }

    @Nonnull
    @Override
    protected MongoDocument newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new MongoDocument(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, Document>> loadResourcePagesFromAzure() {
        final com.mongodb.client.MongoCollection<Document> client = getClient();
        if (client == null) {
            return Collections.emptyIterator();
        }
        final MongoCursor<Document> itemsIterator = client.find().batchSize(getPageSize()).iterator();
        return new Iterator<Page<Document>>() {
            @Override
            public boolean hasNext() {
                return itemsIterator.hasNext();
            }

            @Override
            public Page<Document> next() {
                return new ItemPage<>(readDocuments(itemsIterator));
            }
        };
    }

    @Nonnull
    @Override
    protected Stream<Document> loadResourcesFromAzure() {
        final com.mongodb.client.MongoCollection<Document> client = getClient();
        if (client == null) {
            return Stream.empty();
        }
        final MongoCursor<Document> iterator = client.find().batchSize(getPageSize()).iterator();
        return readDocuments(iterator);
    }

    private Stream<Document> readDocuments(final MongoCursor<Document> iterator) {
        if (iterator == null || !iterator.hasNext()) {
            if (Objects.nonNull(iterator)) {
                iterator.close();
            }
            return Stream.empty();
        }
        final List<Document> result = new ArrayList<>();
        for (int i = 0; i < getPageSize() && iterator.hasNext(); i++) {
            result.add(iterator.next());
        }
        return result.stream();
    }

    @Nullable
    @Override
    protected Document loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        final Object documentId = getDocumentIdFromName(name);
        return Optional.ofNullable(getClient())
            .map(client -> client.find(new Document(MONGO_ID_KEY, documentId)).first()).orElse(null);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<MongoDocument, Document> newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        return new MongoDocumentDraft(name, Objects.requireNonNull(rgName), this);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<MongoDocument, Document> newDraftForUpdate(@Nonnull MongoDocument mongoDocument) {
        return new MongoDocumentDraft(mongoDocument);
    }

    @Override
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final Object documentId = getDocumentIdFromName(id.name());
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteOne(new Document(MONGO_ID_KEY, documentId)));
    }

    private Object getDocumentIdFromName(final String name) {
        return this.list().stream()
            .filter(document -> document.getDocumentId() != null && StringUtils.equals(name, document.getDocumentId().toString()))
            .findFirst()
            .map(MongoDocument::getDocumentId)
            .orElseGet(() -> new ObjectId(name));
    }

    @Nullable
    @Override
    protected com.mongodb.client.MongoCollection<Document> getClient() {
        return getParent().getClient();
    }
}
