/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.cosmos.mongo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDocumentModule.MONGO_ID_KEY;

public class MongoDocumentDraft extends MongoDocument implements
    AzResource.Draft<MongoDocument, Document> {

    @Getter
    private MongoDocument origin;

    @Getter
    @Setter
    private Document draftDocument;

    protected MongoDocumentDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<MongoDocument, MongoCollection, Document> module) {
        super(name, resourceGroupName, module);
    }

    protected MongoDocumentDraft(@Nonnull MongoDocument origin) {
        super(origin);
        this.origin = origin;
    }

    public void setDocument(ObjectNode document) {
        this.draftDocument = Document.parse(document.toPrettyString());
    }

    @Override
    public Object getDocumentId() {
        return Optional.ofNullable(draftDocument)
                .map(draftDocument -> draftDocument.get(MONGO_ID_KEY))
                .orElseGet(super::getDocumentId);
    }

    @Nullable
    @Override
    public ObjectNode getDocument() {
        return Optional.ofNullable(draftDocument)
                .map(Document::toJson)
                .map(json -> JsonUtils.fromJson(json, ObjectNode.class))
                .orElseGet(() -> super.getDocument());
    }

    @Nullable
    @Override
    public String getSharedKey() {
        final String sharedKey = getParent().getSharedKey();
        return Optional.ofNullable(draftDocument)
                .map(node -> node.get(sharedKey))
                .map(Object::toString)
                .orElseGet(super::getSharedKey);
    }

    @Override
    public void reset() {
        this.draftDocument = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.create_mongo_document.document", params = {"this.getName()"})
    public Document createResourceInAzure() {
        final com.mongodb.client.MongoCollection<Document> client = Objects.requireNonNull(((MongoDocumentModule) getModule()).getClient());
        final Object id = Objects.requireNonNull(draftDocument).get(MONGO_ID_KEY);
        client.insertOne(draftDocument);
        return Objects.requireNonNull(client.find(new BasicDBObject(MONGO_ID_KEY, id)).first());
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.update_mongo_document.document", params = {"this.getName()"})
    public Document updateResourceInAzure(@Nonnull Document origin) {
        final com.mongodb.client.MongoCollection<Document> client = Objects.requireNonNull(((MongoDocumentModule) getModule()).getClient());
        final Object id = getDocumentId();
        final UpdateResult updateResult = client.replaceOne(new Document(MONGO_ID_KEY, id), draftDocument);
        if (updateResult.getModifiedCount() > 0) {
            return Objects.requireNonNull(loadRemoteFromAzure());
        } else {
            throw new AzureToolkitRuntimeException("Failed to update document.");
        }
    }

    @Override
    public boolean isModified() {
        return this.draftDocument != null;
    }

}
