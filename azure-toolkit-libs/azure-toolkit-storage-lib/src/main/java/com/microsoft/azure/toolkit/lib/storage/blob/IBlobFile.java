/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.blob;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;

import java.time.OffsetDateTime;

public interface IBlobFile extends StorageFile {

    BlobContainer getContainer();

    BlobContainerClient getClient();

    default String getSasUrl() {
        final OffsetDateTime expiration = OffsetDateTime.now().plusDays(1);
        final String containerUrl = this.getContainer().getUrl();
        final String token;
        if (this instanceof BlobContainer) {
            final BlobContainerSasPermission containerPermission = new BlobContainerSasPermission().setReadPermission(true).setListPermission(true);
            final BlobServiceSasSignatureValues builder = new BlobServiceSasSignatureValues(expiration, containerPermission).setProtocol(SasProtocol.HTTPS_ONLY);
            token = this.getClient().generateSas(builder);
        } else {
            final BlobSasPermission blobPermission = new BlobSasPermission().setReadPermission(true).setListPermission(true);
            final BlobServiceSasSignatureValues builder = new BlobServiceSasSignatureValues(expiration, blobPermission).setProtocol(SasProtocol.HTTPS_ONLY);
            token = this.getClient().generateSas(builder);
        }
        return String.format("%s/%s?%s", containerUrl, this.getPath(), token);
    }
}
