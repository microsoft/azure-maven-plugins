/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.artifact;

import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.storage.CloudStorageAccount;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

import java.util.Map;

import static com.microsoft.azure.maven.function.Constants.INTERNAL_STORAGE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class FunctionArtifactHelperTest {

    @Test
    public void getCloudStorageAccount() throws Exception {
        final String storageConnection =
                "DefaultEndpointsProtocol=https;AccountName=123456;AccountKey=12345678;EndpointSuffix=core.windows.net";
        final Map mapSettings = mock(Map.class);
        final DeployTarget deployTarget = mock(DeployTarget.class);
        final AppSetting storageSetting = mock(AppSetting.class);
        mapSettings.put(INTERNAL_STORAGE_KEY, storageSetting);
        doReturn(mapSettings).when(deployTarget).getAppSettings();
        doReturn(storageSetting).when(mapSettings).get(anyString());
        doReturn(storageConnection).when(storageSetting).value();
        final Log log = mock(Log.class);
        final CloudStorageAccount storageAccount = FunctionArtifactHelper.getCloudStorageAccount(deployTarget,log);
        assertNotNull(storageAccount);
    }

    @Test
    public void getCloudStorageAccountWithException() {
        final FunctionApp app = mock(FunctionApp.class);
        final DeployTarget deployTarget = mock(DeployTarget.class);
        final Map appSettings = mock(Map.class);
        doReturn(appSettings).when(app).getAppSettings();
        doReturn(null).when(appSettings).get(anyString());
        final Log log = mock(Log.class);
        String exceptionMessage = null;
        try {
            FunctionArtifactHelper.getCloudStorageAccount(deployTarget,log);
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        } finally {
            assertEquals("Application setting 'AzureWebJobsStorage' not found.", exceptionMessage);
        }
    }

    @Test
    public void createZipPackageWithException() {
        String exceptionMessage = null;
        final Log log = mock(Log.class);
        try {
            FunctionArtifactHelper.createZipPackage("", log);
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        } finally {
            assertEquals("Azure Functions stage directory not found. Please run 'mvn package" +
                    " azure-functions:package' first.", exceptionMessage);
        }
    }
}
