/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosPatchItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

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
    @AzureOperation(name = "azure/cosmos.create_sql_document.document", params = {"this.getName()"})
    public ObjectNode createResourceInAzure() {
        final String documentPartitionKey = getParent().getPartitionKey();
        final String documentPartitionValue = SqlDocumentModule.getSqlDocumentPartitionValue(draftDocument, documentPartitionKey);
        final PartitionKey partitionKey = Objects.isNull(documentPartitionValue) ?
                PartitionKey.NONE : new PartitionKey(documentPartitionValue);
        final String documentId = Objects.requireNonNull(draftDocument.get(ID).asText(), "'id' is required for sql document");
        final CosmosContainer client = ((SqlDocumentModule) getModule()).getClient();
        Objects.requireNonNull(client).createItem(draftDocument).getItem();
        return Objects.requireNonNull(client).readItem(documentId, partitionKey, ObjectNode.class).getItem();
    }

    @Nonnull
    @Override
    @AzureOperation(name = "azure/cosmos.update_sql_document.document", params = {"this.getName()"})
    public ObjectNode updateResourceInAzure(@Nonnull ObjectNode origin) {
        final ObjectNode document = getDocument();
        final ObjectNode originDocument = Objects.requireNonNull(super.getDocument());
        if (Objects.isNull(document) || Objects.equals(document, originDocument)) {
            return originDocument;
        }
        if (!Objects.equals(document.get(ID), originDocument.get(ID))) {
            throw new AzureToolkitRuntimeException("Could not modify id for sql document");
        }
        final String partitionKey = Objects.requireNonNull(getParent().getPartitionKey());
        final String newPartitionValue = SqlDocumentModule.getSqlDocumentPartitionValue(document, partitionKey);
        final String originPartitionValue = SqlDocumentModule.getSqlDocumentPartitionValue(originDocument, partitionKey);
        if (!StringUtils.equals(newPartitionValue, originPartitionValue)) {
            throw new AzureToolkitRuntimeException(String.format("Could not modify partition key '%s' for sql document", partitionKey));
        }
        final CosmosContainer client = ((SqlDocumentModule) getModule()).getClient();
        final PartitionKey key = Objects.isNull(newPartitionValue) ? PartitionKey.NONE : new PartitionKey(newPartitionValue);
        final ObjectNode node = draftDocument.deepCopy();
        for (String field : HIDE_FIELDS) {
            node.set(field, origin.get(field));
        }
        Objects.requireNonNull(client).replaceItem(node, getDocumentId(), key, new CosmosPatchItemRequestOptions()).getItem();
        return Objects.requireNonNull(client).readItem(node.get(ID).asText(), key, ObjectNode.class).getItem();
    }

    @Override
    public boolean isModified() {
        return draftDocument != null;
    }
}
