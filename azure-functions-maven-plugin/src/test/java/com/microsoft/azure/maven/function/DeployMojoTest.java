/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask.APPINSIGHTS_INSTRUMENTATION_KEY;
import static com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask.APPLICATIONINSIGHTS_CONNECTION_STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeployMojoTest extends MojoTestBase {
    private DeployMojo mojo = null;
    private DeployMojo mojoSpy = null;

    @Mock
    private FunctionApp mockApp;

    @Before
    public void setUp() throws Exception {
        mojo = getMojoFromPom();
        mojoSpy = spy(mojo);
    }

    @Test
    public void getConfiguration() {
        assertEquals("resourceGroupName", mojo.getResourceGroup());
        assertEquals("appName", mojo.getAppName());
    }

    @Test
    public void test_createOrUpdateResource_pickUpExistingSettings() throws Throwable {

        /* GIVEN */
        final String existingConnectionString = "InstrumentationKey=00000000;IngestionEndpoint=https://example.com/";

        when(mockApp.exists()).thenReturn(true);
        final Map<String, String> appSettings = new HashMap<>();
        appSettings.put(APPLICATIONINSIGHTS_CONNECTION_STRING, existingConnectionString);
        appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, "stale-ikey-that-should-be-ignored");
        when(mockApp.getAppSettings()).thenReturn(appSettings);

        final ArgumentCaptor<FunctionAppConfig> configCaptor = ArgumentCaptor.forClass(FunctionAppConfig.class);
        doReturn(mock(FunctionAppBase.class)).when(mojoSpy).executeDeploymentTask(configCaptor.capture());

        try (final MockedStatic<AppServiceConfigUtils> utils = mockStatic(AppServiceConfigUtils.class)) {
            utils.when(() -> AppServiceConfigUtils.fromFunctionApp(mockApp)).thenReturn(new FunctionAppConfig());
            utils.when(() -> AppServiceConfigUtils.mergeAppServiceConfig(any(), any())).thenAnswer(inv -> null);

            /* WHEN */
            mojoSpy.createOrUpdateResource(mockApp);
        }

        /* THEN */
        verify(mojoSpy).executeDeploymentTask(any());

        final FunctionAppConfig deployedConfig = configCaptor.getValue();
        assertEquals(existingConnectionString, deployedConfig.appInsightsConnectionString());
        assertNull("Instrumentation key should not be backfilled when connection string is already present", deployedConfig.appInsightsKey());
    }

    @Test
    public void test_createOrUpdateResource_pickUpInstrumentationKey_noExistingConnectionString() throws Throwable {

        /* GIVEN */
        when(mockApp.exists()).thenReturn(true);
        final Map<String, String> appSettings = new HashMap<>();
        // No APPLICATIONINSIGHTS_CONNECTION_STRING yet
        appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, "00000000");
        when(mockApp.getAppSettings()).thenReturn(appSettings);

        final ArgumentCaptor<FunctionAppConfig> configCaptor = ArgumentCaptor.forClass(FunctionAppConfig.class);
        doReturn(mock(FunctionAppBase.class)).when(mojoSpy).executeDeploymentTask(configCaptor.capture());

        try (final MockedStatic<AppServiceConfigUtils> utils = mockStatic(AppServiceConfigUtils.class)) {
            utils.when(() -> AppServiceConfigUtils.fromFunctionApp(mockApp)).thenReturn(new FunctionAppConfig());
            utils.when(() -> AppServiceConfigUtils.mergeAppServiceConfig(any(), any())).thenAnswer(inv -> null);

            /* WHEN */
            mojoSpy.createOrUpdateResource(mockApp);
        }

        /* THEN */
        verify(mojoSpy).executeDeploymentTask(any());

        final FunctionAppConfig deployedConfig = configCaptor.getValue();
        assertNull(deployedConfig.appInsightsConnectionString());
        assertEquals("00000000", deployedConfig.appInsightsKey());
    }

    private DeployMojo getMojoFromPom() throws Exception {
        final DeployMojo mojoFromPom = (DeployMojo) getMojoFromPom("/pom.xml", "deploy");
        assertNotNull(mojoFromPom);
        return mojoFromPom;
    }
}
