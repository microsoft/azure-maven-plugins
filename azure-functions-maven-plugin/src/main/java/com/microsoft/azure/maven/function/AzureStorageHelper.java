/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.io.FileInputStream;

public class AzureStorageHelper {
    public static String uploadFileAsBlob(final File fileToUpload, final CloudStorageAccount storageAccount,
                                          final String containerName, final String blobName) throws Exception {
        final CloudBlobContainer blobContainer = getBlobContainer(storageAccount, containerName);
        blobContainer.createIfNotExists(BlobContainerPublicAccessType.BLOB, null, null);

        final CloudBlockBlob blob = blobContainer.getBlockBlobReference(blobName);
        blob.upload(new FileInputStream(fileToUpload), fileToUpload.length());
        return blob.getUri().toString();
    }

    public static void deleteBlob(final CloudStorageAccount storageAccount, final String containerName,
                                  final String blobName) throws Exception {
        final CloudBlobContainer blobContainer = getBlobContainer(storageAccount, containerName);
        if (blobContainer.exists()) {
            final CloudBlockBlob blob = blobContainer.getBlockBlobReference(blobName);
            blob.deleteIfExists();
        }
    }

    protected static CloudBlobContainer getBlobContainer(final CloudStorageAccount storageAccount,
                                                         final String containerName) throws Exception {
        final CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        return blobClient.getContainerReference(containerName);
    }
}
