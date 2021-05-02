/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.handlers.artifact;

import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTarget;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Map;

import static com.microsoft.azure.toolkit.lib.legacy.function.Constants.INTERNAL_STORAGE_KEY;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class MSDeployArtifactHandlerImplTest {
    private MSDeployArtifactHandlerImpl handler;

    private MSDeployArtifactHandlerImpl handlerSpy;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    public void buildHandler() {
        handler = new MSDeployArtifactHandlerImpl.Builder()
                .stagingDirectoryPath("target/classes")
                .build();
        handlerSpy = Mockito.spy(handler);
    }

    @Test
    public void publish() throws Exception {
        final DeployTarget deployTarget = Mockito.mock(DeployTarget.class);
        final Map mapSettings = Mockito.mock(Map.class);
        final File file = Mockito.mock(File.class);
        final AppSetting storageSetting = Mockito.mock(AppSetting.class);

        mapSettings.put(INTERNAL_STORAGE_KEY, storageSetting);
        buildHandler();

        Mockito.doReturn("").when(handlerSpy).uploadPackageToAzureStorage(file, null, "");
        Mockito.doReturn("").when(handlerSpy).getBlobName();
        Mockito.doNothing().when(handlerSpy).deployWithPackageUri(ArgumentMatchers.eq(deployTarget), ArgumentMatchers.eq(""), ArgumentMatchers.any(Runnable.class));
        Mockito.doReturn(file).when(handlerSpy).createZipPackage();

        try (MockedStatic<FunctionArtifactHelper> mockArtifactHelper = Mockito.mockStatic(FunctionArtifactHelper.class)) {
            mockArtifactHelper.when(() -> FunctionArtifactHelper.getCloudStorageAccount(deployTarget)).thenReturn(null);
            handlerSpy.publish(deployTarget);
        }

        Mockito.verify(handlerSpy, Mockito.times(1)).publish(deployTarget);
        Mockito.verify(handlerSpy, Mockito.times(1)).createZipPackage();
        Mockito.verify(handlerSpy, Mockito.times(1)).getBlobName();
        Mockito.verify(handlerSpy, Mockito.times(1)).uploadPackageToAzureStorage(file, null, "");
        Mockito.verify(handlerSpy, Mockito.times(1)).deployWithPackageUri(ArgumentMatchers.eq(deployTarget), ArgumentMatchers.eq(""), ArgumentMatchers.any(Runnable.class));
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void createZipPackage() throws Exception {
        buildHandler();
        final File zipPackage = handlerSpy.createZipPackage();

        Assert.assertTrue(zipPackage.exists());
    }

    @Test
    public void uploadPackageToAzureStorage() throws Exception {
        final CloudStorageAccount storageAccount = Mockito.mock(CloudStorageAccount.class);
        final CloudBlobClient blobClient = Mockito.mock(CloudBlobClient.class);
        Mockito.doReturn(blobClient).when(storageAccount).createCloudBlobClient();
        final CloudBlobContainer blobContainer = Mockito.mock(CloudBlobContainer.class);
        Mockito.doReturn(blobContainer).when(blobClient).getContainerReference(ArgumentMatchers.anyString());
        Mockito.doReturn(true).when(blobContainer)
                .createIfNotExists(ArgumentMatchers.any(BlobContainerPublicAccessType.class), ArgumentMatchers.isNull(), ArgumentMatchers.isNull());
        final CloudBlockBlob blob = Mockito.mock(CloudBlockBlob.class);
        Mockito.doReturn(blob).when(blobContainer).getBlockBlobReference(ArgumentMatchers.anyString());
        Mockito.doNothing().when(blob).upload(ArgumentMatchers.any(FileInputStream.class), ArgumentMatchers.anyLong());
        Mockito.doReturn(new URI("http://blob")).when(blob).getUri();
        Mockito.doReturn("token").when(blob).generateSharedAccessSignature(ArgumentMatchers.any(), ArgumentMatchers.isNull());
        final File file = new File("pom.xml");

        buildHandler();
        final String packageUri = handler.uploadPackageToAzureStorage(file, storageAccount, "blob");

        Assert.assertEquals("http://blob?token", packageUri);
    }

    @Test
    public void deployWithPackageUri() throws Exception {
        final DeployTarget deployTarget = Mockito.mock(DeployTarget.class);
        final Runnable runnable = Mockito.mock(Runnable.class);
        Mockito.doNothing().when(deployTarget).msDeploy("uri", false);
        buildHandler();
        handlerSpy.deployWithPackageUri(deployTarget, "uri", runnable);

        Mockito.verify(handlerSpy, Mockito.times(1)).deployWithPackageUri(deployTarget, "uri", runnable);
        Mockito.verify(runnable, Mockito.times(1)).run();
        Mockito.verify(deployTarget, Mockito.times(1)).msDeploy("uri", false);
    }

    @Test
    public void deletePackageFromAzureStorage() throws Exception {
        final CloudStorageAccount storageAccount = Mockito.mock(CloudStorageAccount.class);
        final CloudBlobClient blobClient = Mockito.mock(CloudBlobClient.class);
        Mockito.doReturn(blobClient).when(storageAccount).createCloudBlobClient();
        final CloudBlobContainer blobContainer = Mockito.mock(CloudBlobContainer.class);
        Mockito.doReturn(blobContainer).when(blobClient).getContainerReference(ArgumentMatchers.anyString());
        Mockito.doReturn(true).when(blobContainer).exists();
        final CloudBlockBlob blob = Mockito.mock(CloudBlockBlob.class);
        Mockito.doReturn(blob).when(blobContainer).getBlockBlobReference(ArgumentMatchers.anyString());
        Mockito.doReturn(true).when(blob).deleteIfExists();
        buildHandler();
        handler.deletePackageFromAzureStorage(storageAccount, "blob");
    }
}
