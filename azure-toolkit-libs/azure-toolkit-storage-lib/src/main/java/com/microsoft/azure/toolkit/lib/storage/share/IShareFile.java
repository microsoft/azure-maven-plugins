/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.storage.share;

import com.azure.storage.common.sas.SasProtocol;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClient;
import com.azure.storage.file.share.sas.ShareFileSasPermission;
import com.azure.storage.file.share.sas.ShareSasPermission;
import com.azure.storage.file.share.sas.ShareServiceSasSignatureValues;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;

import java.time.OffsetDateTime;

public interface IShareFile extends StorageFile {
    Share getShare();

    default String getSasUrl() {
        final String shareUrl = this.getShare().getUrl();
        final OffsetDateTime expiration = OffsetDateTime.now().plusDays(1);
        final String token;
        if (this instanceof Share) {
            final ShareSasPermission sharePermission = new ShareSasPermission().setReadPermission(true).setListPermission(true);
            final ShareServiceSasSignatureValues builder = new ShareServiceSasSignatureValues(expiration, sharePermission).setProtocol(SasProtocol.HTTPS_ONLY);
            token = this.getShare().getShareClient().generateSas(builder);
        } else {
            final ShareFileSasPermission filePermission = new ShareFileSasPermission().setReadPermission(true);
            final ShareServiceSasSignatureValues builder = new ShareServiceSasSignatureValues(expiration, filePermission).setProtocol(SasProtocol.HTTPS_ONLY);
            if (this.isDirectory()) {
                token = ((ShareDirectoryClient) this.getClient()).generateSas(builder);
            } else {
                token = ((ShareFileClient) this.getClient()).generateSas(builder);
            }
        }
        return String.format("%s/%s?%s", shareUrl, this.getPath(), token);
    }
}
