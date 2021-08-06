/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KindEnum {

    Storage("Storage", "General Purpose v1"),
    StorageV2("StorageV2", "General Purpose v2"),
    BlobStorage("BlobStorage", "Blob Storage"),
    FileStorage("FileStorage", "File Storage"),
    BlockBlobStorage("BlockBlobStorage", "Block Blobs Storage");

    private String code;
    private String desc;
}
