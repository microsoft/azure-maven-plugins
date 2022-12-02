/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosPatchItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDocumentModule.ID;

public class SqlDocumentDraft extends SqlDocument implements
    AzResource.Draft<SqlDocument, ObjectNode> {

    @Getter
    private SqlDocument origin;

    @Getter
    @Setter
    private ObjectNode draftDocument;

    protected SqlDocumentDraft(@Nonnull String name, @Nullable String resourceGroup, @Nonnull SqlDocumentModule module) {
        super(name, resourceGroup, module);
    }

    protected SqlDocumentDraft(@Nonnull SqlDocument origin) {
        super(origin);
        this.origin = origin;
    }

    @Nullable
    @Override
    public ObjectNode getDocument() {
        return Optional.ofNullable(draftDocument).orElseGet(super::getDocument);
    }

    @Override
    public String getDocumentId() {
        return Optional.ofNullable(draftDocument)
                .map(draftDocument -> draftDocument.get(ID).asText())
                .orElseGet(super::getDocumentId);
    }

    @Nullable
    @Override
    public String getDocumentPartitionKey() {
        final String partitionKey = getParent().getPartitionKey();
        return Optional.ofNullable(draftDocument)
                .map(doc -> SqlDocumentModule.getSqlDocumentPartitionValue(doc, partitionKey))
                .orElseGet(super::getDocumentPartitionKey);
    }

    @Override
    public void reset() {
        this.draftDocument = null;
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.create_sql_document.document", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public ObjectNode createResourceInAzure() {
        final String documentPartitionKey = getParent().getPartitionKey();
        final String documentPartitionValue = SqlDocumentModule.getSqlDocumentPartitionValue(draftDocument, documentPartitionKey);
        final PartitionKey partitionKey = Objects.isNull(documentPartitionValue) ?
                PartitionKey.NONE : new PartitionKey(documentPartitionValue);
        final String documentId = draftDocument.get(ID).asText();
        final CosmosContainer client = ((SqlDocumentModule) getModule()).getClient();
        Objects.requireNonNull(client).createItem(draftDocument).getItem();
        return Objects.requireNonNull(client).readItem(documentId, partitionKey, ObjectNode.class).getItem();
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.update_sql_document.document", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public ObjectNode updateResourceInAzure(@Nonnull ObjectNode origin) {
        final CosmosContainer client = ((SqlDocumentModule) getModule()).getClient();
        final String documentPartitionKey = getDocumentPartitionKey();
        final PartitionKey partitionKey = Objects.isNull(getDocumentPartitionKey()) ? PartitionKey.NONE : new PartitionKey(documentPartitionKey);
        final ObjectNode node = draftDocument.deepCopy();
        for (String field : HIDE_FIELDS) {
            node.set(field, origin.get(field));
        }
        Objects.requireNonNull(client).replaceItem(node, getDocumentId(), partitionKey, new CosmosPatchItemRequestOptions()).getItem();
        return Objects.requireNonNull(client).readItem(node.get(ID).asText(), partitionKey, ObjectNode.class).getItem();
    }

    @Override
    public boolean isModified() {
        return draftDocument != null;
    }
}
