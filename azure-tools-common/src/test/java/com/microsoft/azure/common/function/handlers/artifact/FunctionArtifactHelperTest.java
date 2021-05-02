/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.handlers.artifact;

import com.microsoft.azure.common.appservice.DeployTarget;
import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.storage.CloudStorageAccount;

import org.junit.Test;

import java.util.Map;

import static com.microsoft.azure.common.function.Constants.INTERNAL_STORAGE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class FunctionArtifactHelperTest {

    @Test
    public void testGetCloudStorageAccount() throws Exception {
        final String storageConnection =
                "DefaultEndpointsProtocol=https;AccountName=123456;AccountKey=12345678;EndpointSuffix=core.windows.net";
        final Map mapSettings = mock(Map.class);
        final DeployTarget deployTarget = mock(DeployTarget.class);
        final AppSetting storageSetting = mock(AppSetting.class);
        mapSettings.put(INTERNAL_STORAGE_KEY, storageSetting);
        doReturn(mapSettings).when(deployTarget).getAppSettings();
        doReturn(storageSetting).when(mapSettings).get(anyString());
        doReturn(storageConnection).when(storageSetting).value();
        final CloudStorageAccount storageAccount = FunctionArtifactHelper.getCloudStorageAccount(deployTarget);
        assertNotNull(storageAccount);
    }

    @Test
    public void testGetCloudStorageAccountWithException() {
        final FunctionApp app = mock(FunctionApp.class);
        final DeployTarget deployTarget = mock(DeployTarget.class);
        final Map appSettings = mock(Map.class);
        doReturn(appSettings).when(app).getAppSettings();
        doReturn(null).when(appSettings).get(anyString());
        String exceptionMessage = null;
        try {
            FunctionArtifactHelper.getCloudStorageAccount(deployTarget);
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        } finally {
            assertEquals("Application setting 'AzureWebJobsStorage' not found.", exceptionMessage);
        }
    }

    @Test
    public void testUpdateAppSetting() throws Exception {
        final DeployTarget deployTarget = mock(DeployTarget.class);
        final FunctionApp functionApp = mock(FunctionApp.class);
        doReturn(functionApp).when(deployTarget).getApp();
        final FunctionApp.Update update = mock(FunctionApp.Update.class);
        doReturn(update).when(functionApp).update();
        doReturn(update).when(update).withAppSetting(any(), any());
        doReturn(functionApp).when(update).apply();
        final String appSettingKey = "KEY";
        final String appSettingValue = "VALUE";
        FunctionArtifactHelper.updateAppSetting(deployTarget, appSettingKey, appSettingValue);

        verify(deployTarget, times(1)).getApp();
        verify(functionApp, times(1)).update();
        verify(update, times(1)).withAppSetting(appSettingKey, appSettingValue);
        verify(update, times(1)).apply();
        verifyNoMoreInteractions(update);
        verifyNoMoreInteractions(functionApp);
        verifyNoMoreInteractions(deployTarget);
    }

    @Test
    public void testUpdateAppSettingWithException() {
        final DeployTarget deployTarget = mock(DeployTarget.class);
        final WebApp webapp = mock(WebApp.class);
        doReturn(webapp).when(deployTarget).getApp();
        final String appSettingKey = "KEY";
        final String appSettingValue = "VALUE";
        String exceptionMessage = null;
        try {
            FunctionArtifactHelper.updateAppSetting(deployTarget, appSettingKey, appSettingValue);
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        } finally {
            assertEquals("Unsupported deployment target, only function is supported", exceptionMessage);
        }
    }

    @Test
    public void testCreateFunctionArtifactWithException() {
        String exceptionMessage = null;
        try {
            FunctionArtifactHelper.createFunctionArtifact("");
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        } finally {
            assertEquals("Azure Functions stage directory not found. Please run 'mvn clean" +
                    " azure-functions:package' first.", exceptionMessage);
        }
    }
}
