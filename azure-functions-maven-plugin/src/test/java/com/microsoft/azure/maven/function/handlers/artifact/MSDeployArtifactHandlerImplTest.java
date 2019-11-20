/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.artifact;

import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.WebDeployment.DefinitionStages.WithExecute;
import com.microsoft.azure.management.appservice.WebDeployment.DefinitionStages.WithPackageUri;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.function.AbstractFunctionMojo;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Map;

import static com.microsoft.azure.maven.function.Constants.INTERNAL_STORAGE_KEY;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MSDeployArtifactHandlerImpl.class,FunctionArtifactHelper.class })
public class MSDeployArtifactHandlerImplTest {
    @Mock
    AbstractFunctionMojo mojo;

    @Mock
    Log log;

    private MSDeployArtifactHandlerImpl handler;

    private MSDeployArtifactHandlerImpl handlerSpy;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    public void buildHandler() {
        when(mojo.getAppName()).thenReturn("appName");
        when(mojo.getLog()).thenReturn(log);
        handler = new MSDeployArtifactHandlerImpl.Builder()
            .stagingDirectoryPath(mojo.getDeploymentStagingDirectoryPath())
            .log(mojo.getLog())
            .build();
        handlerSpy = spy(handler);
    }

    @Test
    public void publish() throws Exception {
        final DeployTarget deployTarget = mock(DeployTarget.class);
        final Map mapSettings = mock(Map.class);
        final File file = mock(File.class);
        final AppSetting storageSetting = mock(AppSetting.class);

        mapSettings.put(INTERNAL_STORAGE_KEY, storageSetting);
        doReturn("azure-functions-maven-plugin").when(mojo).getPluginName();
        doReturn(mapSettings).when(deployTarget).getAppSettings();
        doReturn(storageSetting).when(mapSettings).get(anyString());
        buildHandler();

        PowerMockito.mockStatic(FunctionArtifactHelper.class);
        when(FunctionArtifactHelper.getCloudStorageAccount(any(), any())).thenReturn(null);
        doReturn("").when(handlerSpy).uploadPackageToAzureStorage(file, null, "");
        doReturn("").when(handlerSpy).getBlobName();
        doReturn(mapSettings).when(deployTarget).getAppSettings();
        doNothing().when(handlerSpy).deployWithPackageUri(eq(deployTarget), eq(""), any(Runnable.class));
        doReturn(file).when(handlerSpy).createZipPackage();

        handlerSpy.publish(deployTarget);

        verify(handlerSpy, times(1)).publish(deployTarget);
        verify(handlerSpy, times(1)).createZipPackage();
        verify(handlerSpy, times(1)).getBlobName();
        verify(handlerSpy, times(1)).uploadPackageToAzureStorage(file, null, "");
        verify(handlerSpy, times(1)).deployWithPackageUri(eq(deployTarget), eq(""), any(Runnable.class));
        verifyNoMoreInteractions(handlerSpy);
    }

    @Test
    public void createZipPackage() throws Exception {
        when(mojo.getDeploymentStagingDirectoryPath()).thenReturn("target/classes");

        buildHandler();
        final File zipPackage = handlerSpy.createZipPackage();

        assertTrue(zipPackage.exists());
    }

    @Test
    public void uploadPackageToAzureStorage() throws Exception {
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

        buildHandler();
        final String packageUri = handler.uploadPackageToAzureStorage(file, storageAccount, "blob");

        assertSame("http://blob", packageUri);
    }

    @Test
    public void deployWithPackageUri() throws Exception {
        final FunctionApp app = mock(FunctionApp.class);
        final DeployTarget deployTarget = mock(DeployTarget.class);
        final WithPackageUri withPackageUri = mock(WithPackageUri.class);
        doReturn(withPackageUri).when(app).deploy();
        final WithExecute withExecute = mock(WithExecute.class);
        doReturn(withExecute).when(withPackageUri).withPackageUri(anyString());
        doReturn(withExecute).when(withExecute).withExistingDeploymentsDeleted(false);
        final Runnable runnable = mock(Runnable.class);
        doNothing().when(deployTarget).msDeploy("uri", false);
        buildHandler();
        handlerSpy.deployWithPackageUri(deployTarget, "uri", runnable);

        verify(handlerSpy, times(1)).deployWithPackageUri(deployTarget, "uri", runnable);
        verify(runnable, times(1)).run();
        verify(deployTarget, times(1)).msDeploy("uri", false);
    }

    @Test
    public void deletePackageFromAzureStorage() throws Exception {
        final CloudStorageAccount storageAccount = mock(CloudStorageAccount.class);
        final CloudBlobClient blobClient = mock(CloudBlobClient.class);
        doReturn(blobClient).when(storageAccount).createCloudBlobClient();
        final CloudBlobContainer blobContainer = mock(CloudBlobContainer.class);
        doReturn(blobContainer).when(blobClient).getContainerReference(anyString());
        doReturn(true).when(blobContainer).exists();
        final CloudBlockBlob blob = mock(CloudBlockBlob.class);
        doReturn(blob).when(blobContainer).getBlockBlobReference(anyString());
        doReturn(true).when(blob).deleteIfExists();
        buildHandler();
        handler.deletePackageFromAzureStorage(storageAccount, "blob");
    }
}
