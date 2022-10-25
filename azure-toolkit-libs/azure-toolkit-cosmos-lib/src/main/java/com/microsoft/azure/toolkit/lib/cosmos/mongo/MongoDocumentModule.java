/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class MongoDocumentModule extends AbstractAzResourceModule<MongoDocument, MongoCollection, Document> {

    public static final String MONGO_ID_KEY = "_id";

    public MongoDocumentModule(@Nonnull MongoCollection parent) {
        super("documents", parent);
    }

    @Nonnull
    @Override
    protected MongoDocument newResource(@Nonnull Document document) {
        final String id = Objects.requireNonNull(document.get(MONGO_ID_KEY)).toString();
        return new MongoDocument(id, parent.getResourceGroupName(), this);
    }

    @Nonnull
    @Override
    protected MongoDocument newResource(@Nonnull String name, @Nullable String resourceGroupName) {
        return new MongoDocument(name, Objects.requireNonNull(resourceGroupName), this);
    }

    @Nonnull
    @Override
    protected Stream<Document> loadResourcesFromAzure() {
        final int cosmosBatchSize = Azure.az().config().getCosmosBatchSize();
        return Optional.ofNullable(getClient()).map(client -> client.find().limit(cosmosBatchSize))
                .map(it -> StreamSupport.stream(it.spliterator(), false)).orElse(Stream.empty());
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
    public void delete(@Nonnull String name, @Nullable String rgName) {
        final Object documentId = getDocumentIdFromName(name);
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteOne(new Document(MONGO_ID_KEY, documentId)));
    }

    private Object getDocumentIdFromName(final String name){
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
