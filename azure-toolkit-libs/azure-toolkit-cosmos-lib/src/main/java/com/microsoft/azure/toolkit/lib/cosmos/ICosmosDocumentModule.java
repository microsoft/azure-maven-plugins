/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.cosmos;

import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;

public interface ICosmosDocumentModule <T extends ICosmosDocument> extends AzResourceModule<T> {
    boolean hasMoreDocuments();

    void loadMoreDocuments();
}
