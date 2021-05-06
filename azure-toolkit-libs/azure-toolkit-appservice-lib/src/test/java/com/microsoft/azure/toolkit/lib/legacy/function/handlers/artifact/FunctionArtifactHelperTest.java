/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.handlers.artifact;

import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTarget;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Map;

import static com.microsoft.azure.toolkit.lib.legacy.function.Constants.INTERNAL_STORAGE_KEY;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class FunctionArtifactHelperTest {

    @Test
    public void testGetCloudStorageAccount() throws Exception {
        final String storageConnection =
                "DefaultEndpointsProtocol=https;AccountName=123456;AccountKey=12345678;EndpointSuffix=core.windows.net";
        final Map mapSettings = Mockito.mock(Map.class);
        final DeployTarget deployTarget = Mockito.mock(DeployTarget.class);
        final AppSetting storageSetting = Mockito.mock(AppSetting.class);
        mapSettings.put(INTERNAL_STORAGE_KEY, storageSetting);
        Mockito.doReturn(mapSettings).when(deployTarget).getAppSettings();
        Mockito.doReturn(storageSetting).when(mapSettings).get(ArgumentMatchers.anyString());
        Mockito.doReturn(storageConnection).when(storageSetting).value();
        final CloudStorageAccount storageAccount = FunctionArtifactHelper.getCloudStorageAccount(deployTarget);
        Assert.assertNotNull(storageAccount);
    }

    @Test
    public void testGetCloudStorageAccountWithException() {
        final FunctionApp app = Mockito.mock(FunctionApp.class);
        final DeployTarget deployTarget = Mockito.mock(DeployTarget.class);
        final Map appSettings = Mockito.mock(Map.class);
        Mockito.doReturn(appSettings).when(app).getAppSettings();
        Mockito.doReturn(null).when(appSettings).get(ArgumentMatchers.anyString());
        String exceptionMessage = null;
        try {
            FunctionArtifactHelper.getCloudStorageAccount(deployTarget);
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        } finally {
            Assert.assertEquals("Application setting 'AzureWebJobsStorage' not found.", exceptionMessage);
        }
    }

    @Test
    public void testUpdateAppSetting() throws Exception {
        final DeployTarget deployTarget = Mockito.mock(DeployTarget.class);
        final FunctionApp functionApp = Mockito.mock(FunctionApp.class);
        Mockito.doReturn(functionApp).when(deployTarget).getApp();
        final FunctionApp.Update update = Mockito.mock(FunctionApp.Update.class);
        Mockito.doReturn(update).when(functionApp).update();
        Mockito.doReturn(update).when(update).withAppSetting(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.doReturn(functionApp).when(update).apply();
        final String appSettingKey = "KEY";
        final String appSettingValue = "VALUE";
        FunctionArtifactHelper.updateAppSetting(deployTarget, appSettingKey, appSettingValue);

        Mockito.verify(deployTarget, Mockito.times(1)).getApp();
        Mockito.verify(functionApp, Mockito.times(1)).update();
        Mockito.verify(update, Mockito.times(1)).withAppSetting(appSettingKey, appSettingValue);
        Mockito.verify(update, Mockito.times(1)).apply();
        Mockito.verifyNoMoreInteractions(update);
        Mockito.verifyNoMoreInteractions(functionApp);
        verifyNoMoreInteractions(deployTarget);
    }

    @Test
    public void testUpdateAppSettingWithException() {
        final DeployTarget deployTarget = Mockito.mock(DeployTarget.class);
        final WebApp webapp = Mockito.mock(WebApp.class);
        Mockito.doReturn(webapp).when(deployTarget).getApp();
        final String appSettingKey = "KEY";
        final String appSettingValue = "VALUE";
        String exceptionMessage = null;
        try {
            FunctionArtifactHelper.updateAppSetting(deployTarget, appSettingKey, appSettingValue);
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        } finally {
            Assert.assertEquals("Unsupported deployment target, only function is supported", exceptionMessage);
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
            Assert.assertEquals("Azure Functions stage directory not found. Please run 'mvn clean" +
                    " azure-functions:package' first.", exceptionMessage);
        }
    }
}
