/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.maven.function.utils.DateUtils;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.security.InvalidKeyException;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.EnumSet;

public class AzureStorageHelper {
    private static final int SAS_START_RESERVE_MINUTE = 5;
    private static final String FAIL_TO_GENERATE_BLOB_SAS_TOKEN = "Fail to generate blob sas token";

    public static CloudBlockBlob uploadFileAsBlob(final File fileToUpload, final CloudStorageAccount storageAccount,
                                                  final String containerName, final String blobName) throws Exception {
        final CloudBlobContainer blobContainer = getBlobContainer(storageAccount, containerName);
        blobContainer.createIfNotExists(BlobContainerPublicAccessType.BLOB, null, null);

        final CloudBlockBlob blob = blobContainer.getBlockBlobReference(blobName);
        blob.upload(new FileInputStream(fileToUpload), fileToUpload.length());
        return blob;
    }

    public static void deleteBlob(final CloudStorageAccount storageAccount, final String containerName,
                                  final String blobName) throws Exception {
        final CloudBlobContainer blobContainer = getBlobContainer(storageAccount, containerName);
        if (blobContainer.exists()) {
            final CloudBlockBlob blob = blobContainer.getBlockBlobReference(blobName);
            blob.deleteIfExists();
        }
    }

    public static String getSASToken(final CloudBlob blob, Period period) throws MojoExecutionException {
        final SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
        policy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sasStartTime = now.minusMinutes(SAS_START_RESERVE_MINUTE);
        LocalDateTime sasExpireTime = now.plus(period);
        policy.setSharedAccessStartTime(DateUtils.convertLocalDateTimeToDate(sasStartTime));
        policy.setSharedAccessExpiryTime(DateUtils.convertLocalDateTimeToDate(sasExpireTime));

        try {
            final String sas = blob.generateSharedAccessSignature(policy, null);
            final String url = blob.getUri().toString();
            return String.format("%s?%s", url, sas);
        } catch (InvalidKeyException | StorageException e) {
            throw new MojoExecutionException(FAIL_TO_GENERATE_BLOB_SAS_TOKEN, e);
        }
    }

    protected static CloudBlobContainer getBlobContainer(final CloudStorageAccount storageAccount,
                                                         final String containerName) throws Exception {
        final CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        return blobClient.getContainerReference(containerName);
    }
}
