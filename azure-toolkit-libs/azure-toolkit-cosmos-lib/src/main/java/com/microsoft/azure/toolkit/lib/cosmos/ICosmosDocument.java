/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.cosmos;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface ICosmosDocument extends AzResource {
    @Nullable
    ObjectNode getDocument();

    @Nullable
    Object getDocumentId();

    void updateDocument(ObjectNode document);

    default String getDocumentDisplayName() {
        final List<String> documentsLabelFields = Azure.az().config().getDocumentsLabelFields();
        final ObjectNode document = getDocument();
        for (final String label : documentsLabelFields) {
            if (document != null && document.has(label)) {
                return document.get(label).asText();
            }
        }
        return Optional.ofNullable(getDocumentId()).map(Object::toString).orElse("Unknown");
    }
}
