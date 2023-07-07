/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.cosmos.sql;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

public class SqlDocumentModule extends AbstractAzResourceModule<SqlDocument, SqlContainer, ObjectNode> {

    public static final String DELIMITER = "#";
    public static final String ID = "id";
    public static final String NONE = "$$$none$$$";

    public SqlDocumentModule(@Nonnull SqlContainer parent) {
        super("documents", parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, ObjectNode>> loadResourcePagesFromAzure() {
        final CosmosContainer client = getClient();
        if (client == null) {
            return Collections.emptyIterator();
        }
        return client.queryItems("select * from c", new CosmosQueryRequestOptions(), ObjectNode.class)
            .iterableByPage(getPageSize()).iterator();
    }

    @Nullable
    @Override
    @SneakyThrows(UnsupportedEncodingException.class)
    protected ObjectNode loadResourceFromAzure(@Nonnull String name, @Nullable String resourceGroup) {
        // workaround to fix the name by be encoded which will make split by DELIMITER failed
        final String decodedName = URLDecoder.decode(name, "UTF-8");
        final String[] split = decodedName.split(DELIMITER);
        if (split.length > 2) {
            return null;
        }
        final String id = split[0];
        final String partitionKeyValue = split.length > 1 ? split[1] : StringUtils.EMPTY;
        final PartitionKey partitionKey = StringUtils.equals(partitionKeyValue, NONE) ? PartitionKey.NONE : new PartitionKey(partitionKeyValue);
        return Optional.ofNullable(getClient())
            .map(client -> doLoadDocument(client, partitionKey, id))
            .orElse(null);
    }

    @Nullable
    private ObjectNode doLoadDocument(@Nonnull CosmosContainer container, @Nonnull PartitionKey partitionKey, @Nonnull String id) {
        try {
            return container.readItem(id, partitionKey, ObjectNode.class).getItem();
        } catch (RuntimeException e2) {
            return null;
        }
    }

    @Nullable
    public SqlDocument get(@Nonnull String id, @Nonnull String partitionKey, @Nullable String resourceGroup) {
        return super.get(getSqlDocumentResourceName(id, partitionKey), resourceGroup);
    }

    @Nonnull
    @Override
    protected SqlDocument newResource(@Nonnull ObjectNode objectNode) {
        final SqlContainer container = getParent();
        final String id = Objects.requireNonNull(objectNode.get(ID)).asText();
        final String partitionKey = container.getPartitionKey();
        final String partitionValue = getSqlDocumentPartitionValue(objectNode, partitionKey);
        return newResource(getSqlDocumentResourceName(id, partitionValue), container.getResourceGroupName());
    }

    @Nonnull
    @Override
    protected SqlDocument newResource(@Nonnull String name, @Nullable String resourceGroup) {
        return new SqlDocument(name, resourceGroup, this);
    }

    @Override
    @AzureOperation(name = "azure/cosmos.delete_sql_document.document", params = {"nameFromResourceId(resourceId)"})
    protected void deleteResourceFromAzure(@Nonnull String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final ObjectNode node = loadResourceFromAzure(id.name(), id.resourceGroupName());
        Optional.ofNullable(getClient()).ifPresent(client -> client.deleteItem(node, new CosmosItemRequestOptions()));
    }

    @Nonnull
    @Override
    protected AzResource.Draft<SqlDocument, ObjectNode> newDraftForCreate(@Nonnull String name, @Nullable String rgName) {
        return new SqlDocumentDraft(name, rgName, this);
    }

    @Nonnull
    @Override
    protected AzResource.Draft<SqlDocument, ObjectNode> newDraftForUpdate(@Nonnull SqlDocument document) {
        return new SqlDocumentDraft(document);
    }

    @Override
    @Nullable
    protected synchronized CosmosContainer getClient() {
        return getParent().getClient();
    }

    @Nonnull
    public static String getSqlDocumentResourceName(@Nonnull final String id, @Nullable final String partitionKey) {
        return String.format("%s#%s", id, Objects.isNull(partitionKey) ? NONE : partitionKey);
    }

    @Nullable
    public static String getSqlDocumentPartitionValue(@Nonnull final ObjectNode node, @Nullable final String partitionKey) {
        return Optional.ofNullable(partitionKey)
            .map(node::at)
            .filter(n -> !n.isMissingNode())
            .map(JsonNode::asText).orElse(null);
    }
}
