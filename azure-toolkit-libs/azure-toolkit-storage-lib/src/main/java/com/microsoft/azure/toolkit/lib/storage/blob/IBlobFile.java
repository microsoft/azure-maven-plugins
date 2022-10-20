/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.storage.blob.BlobContainerClient;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;

public interface IBlobFile extends StorageFile {
    BlobContainer getContainer();

    BlobContainerClient getClient();

    String getPath();
}
