/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;

public class AzureStorageHelper {
    private static final int SAS_START_RESERVE_MINUTE = 5;
    private static final String FAIL_TO_DELETE_BLOB = "Fail to delete blob";
    private static final String FAIL_TO_UPLOAD_BLOB = "Fail to upload file as blob";
    private static final String FAIL_TO_GENERATE_BLOB_SAS_TOKEN = "Fail to generate blob sas token";

    public static BlobClient uploadFileAsBlob(final File fileToUpload, final BlobServiceClient blobServiceClient,
            final String containerName, final String blobName) {
        try {
            final BlobContainerClient blobContainer = blobServiceClient.getBlobContainerClient(containerName);
            blobContainer.createIfNotExists();

            final BlobClient blob = blobContainer.getBlobClient(blobName);
            blob.upload(new FileInputStream(fileToUpload), fileToUpload.length());
            return blob;
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException(FAIL_TO_UPLOAD_BLOB, e);
        }
    }

    public static void deleteBlob(final BlobServiceClient blobServiceClient, final String containerName, final String blobName) {
        final BlobContainerClient blobContainer = blobServiceClient.getBlobContainerClient(containerName);
        if (blobContainer.exists()) {
            final BlobClient blob = blobContainer.getBlobClient(blobName);
            blob.deleteIfExists();
        }
    }

    public static String getSASToken(final BlobClient blob, Period period) {
        final BlobServiceSasSignatureValues policy = new BlobServiceSasSignatureValues();
        policy.setPermissions(new BlobSasPermission().setReadPermission(true));

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime sasStartTime = now.minusMinutes(SAS_START_RESERVE_MINUTE);
        final LocalDateTime sasExpireTime = now.plus(period);
        policy.setStartTime(OffsetDateTime.from(sasStartTime));
        policy.setExpiryTime(OffsetDateTime.from(sasExpireTime));
        final String sas = blob.generateSas(policy);
        final String url = blob.getBlobUrl().toString();
        return String.format("%s?%s", url, sas);
    }
}
