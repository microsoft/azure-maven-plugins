/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.WebDeployment.DefinitionStages.WithExecute;
import com.microsoft.azure.management.appservice.WebDeployment.DefinitionStages.WithPackageUri;
import com.microsoft.azure.maven.function.AbstractFunctionMojo;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Map;

import static com.microsoft.azure.maven.function.handlers.MSDeployArtifactHandlerImpl.INTERNAL_STORAGE_NOT_FOUND;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.*;

public class MSDeployArtifactHandlerImplTest {
    @Mock
    AbstractFunctionMojo mojo;

    @Mock
    Log log;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mojo.getAppName()).thenReturn("appName");
        when(mojo.getLog()).thenReturn(log);
    }

    @Test
    public void publish() throws Exception {
        final MSDeployArtifactHandlerImpl handler = new MSDeployArtifactHandlerImpl(mojo);
        final MSDeployArtifactHandlerImpl handlerSpy = spy(handler);
        doReturn(null).when(handlerSpy).createZipPackage();
        doReturn(null).when(handlerSpy).getCloudStorageAccount(isNull());
        doReturn(null).when(handlerSpy).uploadPackageToAzureStorage(isNull(), isNull(), anyString());
        doNothing().when(handlerSpy).deployWithPackageUri(isNull(), isNull(), any(Runnable.class));

        handlerSpy.publish();

        verify(handlerSpy, times(1)).publish();
        verify(handlerSpy, times(1)).createZipPackage();
        verify(handlerSpy, times(1)).getCloudStorageAccount(isNull());
        verify(handlerSpy, times(1)).getBlobName();
        verify(handlerSpy, times(1)).uploadPackageToAzureStorage(isNull(), isNull(), anyString());
        verify(handlerSpy, times(1)).deployWithPackageUri(isNull(), isNull(), any(Runnable.class));
        verifyNoMoreInteractions(handlerSpy);
        verify(mojo, times(1)).getFunctionApp();
    }

    @Test
    public void createZipPackage() throws Exception {
        final MSDeployArtifactHandlerImpl handler = new MSDeployArtifactHandlerImpl(mojo);
        final MSDeployArtifactHandlerImpl handlerSpy = spy(handler);
        when(mojo.getDeploymentStageDirectory()).thenReturn("target/classes");

        final File zipPackage = handlerSpy.createZipPackage();

        assertTrue(zipPackage.exists());
    }

    @Test
    public void getCloudStorageAccount() throws Exception {
        final String storageConnection =
                "DefaultEndpointsProtocol=https;AccountName=123456;AccountKey=12345678;EndpointSuffix=core.windows.net";
        final MSDeployArtifactHandlerImpl handler = new MSDeployArtifactHandlerImpl(mojo);
        final FunctionApp app = mock(FunctionApp.class);
        final Map appSettings = mock(Map.class);
        doReturn(appSettings).when(app).getAppSettings();
        final AppSetting storageSetting = mock(AppSetting.class);
        doReturn(storageSetting).when(appSettings).get(anyString());
        doReturn(storageConnection).when(storageSetting).value();

        final CloudStorageAccount storageAccount = handler.getCloudStorageAccount(app);
        assertNotNull(storageAccount);
    }

    @Test
    public void getCloudStorageAccountWithException() throws Exception {
        final MSDeployArtifactHandlerImpl handler = new MSDeployArtifactHandlerImpl(mojo);
        final FunctionApp app = mock(FunctionApp.class);
        final Map appSettings = mock(Map.class);
        doReturn(appSettings).when(app).getAppSettings();
        doReturn(null).when(appSettings).get(anyString());

        String exceptionMessage = null;
        try {
            handler.getCloudStorageAccount(app);
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        } finally {
            assertEquals(INTERNAL_STORAGE_NOT_FOUND, exceptionMessage);
        }
    }

    @Test
    public void uploadPackageToAzureStorage() throws Exception {
        final MSDeployArtifactHandlerImpl handler = new MSDeployArtifactHandlerImpl(mojo);
        final CloudStorageAccount storageAccount = mock(CloudStorageAccount.class);
        final CloudBlobClient blobClient = mock(CloudBlobClient.class);
        doReturn(blobClient).when(storageAccount).createCloudBlobClient();
        final CloudBlobContainer blobContainer = mock(CloudBlobContainer.class);
        doReturn(blobContainer).when(blobClient).getContainerReference(anyString());
        doReturn(true).when(blobContainer)
                .createIfNotExists(any(BlobContainerPublicAccessType.class), isNull(), isNull());
        final CloudBlockBlob blob = mock(CloudBlockBlob.class);
        doReturn(blob).when(blobContainer).getBlockBlobReference(anyString());
        doNothing().when(blob).upload(any(FileInputStream.class), anyLong());
        doReturn(new URI("http://blob")).when(blob).getUri();
        final File file = new File("pom.xml");

        final String packageUri = handler.uploadPackageToAzureStorage(file, storageAccount, "blob");

        assertSame("http://blob", packageUri);
    }

    @Test
    public void deployWithPackageUri() throws Exception {
        final MSDeployArtifactHandlerImpl handler = new MSDeployArtifactHandlerImpl(mojo);
        final FunctionApp app = mock(FunctionApp.class);
        final WithPackageUri withPackageUri = mock(WithPackageUri.class);
        doReturn(withPackageUri).when(app).deploy();
        final WithExecute withExecute = mock(WithExecute.class);
        doReturn(withExecute).when(withPackageUri).withPackageUri(anyString());
        doReturn(withExecute).when(withExecute).withExistingDeploymentsDeleted(false);
        final Runnable runnable = mock(Runnable.class);

        handler.deployWithPackageUri(app, "uri", runnable);

        verify(withExecute, times(1)).execute();
        verify(runnable, times(1)).run();
    }

    @Test
    public void deletePackageFromAzureStorage() throws Exception {
        final MSDeployArtifactHandlerImpl handler = new MSDeployArtifactHandlerImpl(mojo);
        final CloudStorageAccount storageAccount = mock(CloudStorageAccount.class);
        final CloudBlobClient blobClient = mock(CloudBlobClient.class);
        doReturn(blobClient).when(storageAccount).createCloudBlobClient();
        final CloudBlobContainer blobContainer = mock(CloudBlobContainer.class);
        doReturn(blobContainer).when(blobClient).getContainerReference(anyString());
        doReturn(true).when(blobContainer).exists();
        final CloudBlockBlob blob = mock(CloudBlockBlob.class);
        doReturn(blob).when(blobContainer).getBlockBlobReference(anyString());
        doReturn(true).when(blob).deleteIfExists();

        handler.deletePackageFromAzureStorage(storageAccount, "blob");
    }
}
