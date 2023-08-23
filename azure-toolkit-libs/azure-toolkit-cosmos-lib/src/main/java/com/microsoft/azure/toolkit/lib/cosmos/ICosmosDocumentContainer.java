/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.cosmos;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nonnull;

public interface ICosmosDocumentContainer<T extends ICosmosDocument> extends AzResource {
    public static final Action.Id<ICosmosDocumentContainer<?>> IMPORT_DOCUMENT = Action.Id.of("user/cosmos.import_document.container");
    public static final Action.Id<ICosmosDocumentContainer<?>> CREATE_DOCUMENT = Action.Id.of("user/cosmos.create_document.container");

    T importDocument(@Nonnull final ObjectNode node);
}
