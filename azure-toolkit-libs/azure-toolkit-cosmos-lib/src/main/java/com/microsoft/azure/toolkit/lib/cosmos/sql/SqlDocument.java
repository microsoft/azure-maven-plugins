/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocument;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDocumentModule.ID;

public class SqlDocument extends AbstractAzResource<SqlDocument, SqlContainer, ObjectNode> implements Deletable, ICosmosDocument {
    protected static final String[] HIDE_FIELDS = {"_rid", "_self", "_etag", "_attachments", "_ts"};

    protected SqlDocument(@Nonnull String name, @Nullable String resourceGroup, @Nonnull SqlDocumentModule module) {
        super(name, Objects.requireNonNull(resourceGroup), module);
    }

    public SqlDocument(@Nonnull SqlDocument origin) {
        super(origin);
    }

    @Nullable
    public String getDocumentId() {
        return Optional.ofNullable(this.getRemote()).map(remote -> remote.get(ID)).map(JsonNode::asText).orElse(null);
    }

    @Nullable
    public String getDocumentPartitionKey() {
        final String partitionKey = getParent().getPartitionKey();
        return Optional.ofNullable(this.getRemote())
                .map(remote -> SqlDocumentModule.getSqlDocumentPartitionValue(remote, partitionKey))
                .orElse(null);
    }

    @Override
    public void updateDocument(ObjectNode document) {
        final SqlDocumentDraft sqlDocumentDraft = (SqlDocumentDraft) this.update();
        sqlDocumentDraft.setDraftDocument(document);
        sqlDocumentDraft.updateIfExist();
    }

    @Override
    @Nullable
    public ObjectNode getDocument() {
        return Optional.ofNullable(getRemote()).map(remote -> {
            final ObjectNode node = remote.deepCopy();
            for (String field : HIDE_FIELDS) {
                node.remove(field);
            }
            return node;
        }).orElse(null);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull ObjectNode remote) {
        return Status.RUNNING;
    }

    @Override
    protected void setRemote(@Nullable ObjectNode newRemote) {
        super.setRemote(newRemote);
    }
}
