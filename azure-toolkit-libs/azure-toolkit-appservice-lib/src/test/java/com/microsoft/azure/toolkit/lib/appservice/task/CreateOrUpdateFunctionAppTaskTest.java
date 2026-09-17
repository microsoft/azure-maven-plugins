/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.task.GetOrCreateApplicationInsightsTask;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlotDraft;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlotModule;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDraft;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppModule;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupModule;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccountModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask.APPINSIGHTS_INSTRUMENTATION_KEY;
import static com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask.APPLICATIONINSIGHTS_CONNECTION_STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateOrUpdateFunctionAppTaskTest {

    private static final String APP_NAME = "test-function";
    private static final String RESOURCE_GROUP = "test-rg";
    private static final String SUBSCRIPTION_ID = "test-subscription-id";
    private static final String CONNECTION_STRING = "InstrumentationKey=00000000;IngestionEndpoint=https://example.com/";

    @Mock
    private AzureFunctions azureFunctions;
    @Mock
    private FunctionAppModule functionAppModule;
    @Mock
    private FunctionAppDraft appDraft;
    @Mock
    private FunctionApp functionApp;
    @Mock
    private AzureResources azureResources;
    @Mock
    private ResourceGroupModule resourceGroupModule;
    @Mock
    private ResourceGroup resourceGroup;
    @Mock
    private AzureAccount azureAccount;

    /* mocks for the new-app (create) path */
    @Mock
    private AzureStorageAccount azureStorageAccount;
    @Mock
    private StorageAccountModule storageAccountModule;
    @Mock
    private StorageAccount storageAccount;

    /* mocks for the deployment-slot paths */
    @Mock
    private AppServicePlan appServicePlan;
    @Mock
    private PricingTier pricingTier;
    @Mock
    private FunctionAppDeploymentSlotModule slotModule;
    @Mock
    private FunctionAppDeploymentSlotDraft slotDraft;
    @Mock
    private FunctionAppDeploymentSlot deploymentSlot;

    private MockedStatic<Azure> azure;
    private MockedConstruction<GetOrCreateApplicationInsightsTask> mockedGetOrCreateAiTask;

    @Before
    public void setUp() {
        AzureMessager.setDefaultMessager(new AzureMessager.DummyMessager());

        azure = mockStatic(Azure.class);
        azure.when(() -> Azure.az(AzureFunctions.class)).thenReturn(azureFunctions);
        azure.when(() -> Azure.az(AzureResources.class)).thenReturn(azureResources);
        azure.when(() -> Azure.az(AzureAccount.class)).thenReturn(azureAccount);

        when(azureFunctions.functionApps(SUBSCRIPTION_ID)).thenReturn(functionAppModule);
        when(functionAppModule.updateOrCreate(APP_NAME, RESOURCE_GROUP)).thenReturn(appDraft);
        when(appDraft.isDraftForCreating()).thenReturn(false); // existing app
        when(appDraft.exists()).thenReturn(true);
        when(appDraft.getAppServicePlan()).thenReturn(null); // no flex consumption
        when(appDraft.updateIfExist()).thenReturn(functionApp);

        when(azureResources.groups(SUBSCRIPTION_ID)).thenReturn(resourceGroupModule);
        when(resourceGroupModule.getOrDraft(RESOURCE_GROUP, RESOURCE_GROUP)).thenReturn(resourceGroup);
        when(resourceGroup.isDraftForCreating()).thenReturn(false); // already exists, skip creation

        // Mock App Insight Subtask
        mockedGetOrCreateAiTask = mockConstruction(GetOrCreateApplicationInsightsTask.class, (mock, context) -> {
            final ApplicationInsight aiInstance = org.mockito.Mockito.mock(ApplicationInsight.class);
            when(aiInstance.getConnectionString()).thenReturn(CONNECTION_STRING);
            when(mock.getBody()).thenReturn(() -> aiInstance);
        });
    }

    @After
    public void tearDown() {
        azure.close();
        mockedGetOrCreateAiTask.close();
    }

    /**
     * Verifies that when appInsightsConnectionString is set on the config, it takes precedence over appInsightsKey.
     */
    @Test
    public void doExecute_withConnectionString_writesConnectionStringAndRemovesIKey() throws Exception {

        /* GIVEN */
        final FunctionAppConfig config = buildBaseConfig()
            .appInsightsConnectionString(CONNECTION_STRING);

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, String>> settingsCaptor = ArgumentCaptor.forClass(Map.class);

        /* WHEN */
        new CreateOrUpdateFunctionAppTask(config).doExecute();

        /* THEN */
        verify(appDraft).setAppSettings(settingsCaptor.capture());
        final Map<String, String> writtenSettings = settingsCaptor.getValue();
        assertEquals(CONNECTION_STRING, writtenSettings.get(APPLICATIONINSIGHTS_CONNECTION_STRING));
        assertFalse("instrumentation key must not be written when connection string is set",
            writtenSettings.containsKey(APPINSIGHTS_INSTRUMENTATION_KEY));
    }

    /**
     * Verifies that when appInsightsKey is set on the config (and no appInsightsConnectionString), it is written to the app settings.
     */
    @Test
    public void doExecute_withInstrumentationKeyOnly_writesIKey() throws Exception {
        /* GIVEN */
        final FunctionAppConfig config = buildBaseConfig()
            .appInsightsKey("legacy-ikey-value");

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, String>> settingsCaptor = ArgumentCaptor.forClass(Map.class);

        /* WHEN */
        new CreateOrUpdateFunctionAppTask(config).doExecute();

        /* THEN */

        verify(appDraft).setAppSettings(settingsCaptor.capture());

        final Map<String, String> writtenSettings = settingsCaptor.getValue();
        assertEquals("legacy-ikey-value", writtenSettings.get(APPINSIGHTS_INSTRUMENTATION_KEY));
        assertNull("Connection string must not be written when only instrumentation key is set",
            writtenSettings.get(APPLICATIONINSIGHTS_CONNECTION_STRING));
    }

    /**
     * Verifies that when no explicit App Insights config is set, the task resolves the AI instance and writes its connection string into the app settings.
     */
    @Test
    public void doExecute_withNoExplicitInsightsConfig_looksUpInstanceAndWritesConnectionString() throws Exception {
        /* GIVEN */
        final FunctionAppConfig config = buildBaseConfig(); // no appInsightsConnectionString, no appInsightsKey

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, String>> settingsCaptor = ArgumentCaptor.forClass(Map.class);

        /* WHEN */
        new CreateOrUpdateFunctionAppTask(config).doExecute();

        /* THEN */
        verify(appDraft).setAppSettings(settingsCaptor.capture());

        final Map<String, String> writtenSettings = settingsCaptor.getValue();
        assertEquals(CONNECTION_STRING, writtenSettings.get(APPLICATIONINSIGHTS_CONNECTION_STRING));
        assertFalse("Instrumentation key must not be written when connection string is resolved from AI instance",
            writtenSettings.containsKey(APPINSIGHTS_INSTRUMENTATION_KEY));
    }

    /**
     * Verifies that when the function app does not yet exist, the connection string is written into the new app's settings.
     */
    @Test
    public void doExecute_createPath_withConnectionString_writesConnectionString() throws Exception {
        /* GIVEN */
        azure.when(() -> Azure.az(AzureStorageAccount.class)).thenReturn(azureStorageAccount);
        when(azureStorageAccount.accounts(SUBSCRIPTION_ID)).thenReturn(storageAccountModule);
        when(storageAccountModule.get(anyString(), anyString())).thenReturn(storageAccount);
        when(storageAccount.exists()).thenReturn(true);

        when(appDraft.isDraftForCreating()).thenReturn(true);
        when(appDraft.exists()).thenReturn(false);
        when(appDraft.createIfNotExist()).thenReturn(functionApp);

        final FunctionAppConfig config = buildBaseConfig()
            .appInsightsConnectionString(CONNECTION_STRING);

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, String>> settingsCaptor = ArgumentCaptor.forClass(Map.class);

        /* WHEN */
        new CreateOrUpdateFunctionAppTask(config).doExecute();

        /* THEN */
        verify(appDraft).setAppSettings(settingsCaptor.capture());
        final Map<String, String> writtenSettings = settingsCaptor.getValue();
        assertEquals(CONNECTION_STRING, writtenSettings.get(APPLICATIONINSIGHTS_CONNECTION_STRING));
        assertFalse("Instrumentation key must not be written when connection string is set",
            writtenSettings.containsKey(APPINSIGHTS_INSTRUMENTATION_KEY));
    }

    /**
     * Verifies that when the deployment slot already exists, the connection string is written into the slot's settings.
     */
    @Test
    public void doExecute_updateSlotPath_withConnectionString_writesConnectionString() throws Exception {
        /* GIVEN */
        when(appDraft.getAppServicePlan()).thenReturn(appServicePlan);
        when(appServicePlan.getPricingTier()).thenReturn(pricingTier);

        when(appDraft.slots()).thenReturn(slotModule);
        when(slotModule.updateOrCreate("staging", RESOURCE_GROUP)).thenReturn(slotDraft);
        when(slotDraft.exists()).thenReturn(true);
        when(slotDraft.commit()).thenReturn(deploymentSlot);

        final FunctionAppConfig config = buildBaseConfig();
        config.deploymentSlotName("staging");
        config.appInsightsConnectionString(CONNECTION_STRING);

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, String>> settingsCaptor = ArgumentCaptor.forClass(Map.class);

        /* WHEN */
        new CreateOrUpdateFunctionAppTask(config).doExecute();

        /* THEN */
        verify(slotDraft).setAppSettings(settingsCaptor.capture());
        final Map<String, String> writtenSettings = settingsCaptor.getValue();
        assertEquals(CONNECTION_STRING, writtenSettings.get(APPLICATIONINSIGHTS_CONNECTION_STRING));
        assertFalse("Instrumentation key must not be written when connection string is set",
            writtenSettings.containsKey(APPINSIGHTS_INSTRUMENTATION_KEY));
    }

    private FunctionAppConfig buildBaseConfig() {
        return (FunctionAppConfig) new FunctionAppConfig()
            .appName(APP_NAME)
            .resourceGroup(RESOURCE_GROUP)
            .subscriptionId(SUBSCRIPTION_ID)
            .region(Region.EUROPE_WEST)
            .runtime(new RuntimeConfig().os(OperatingSystem.LINUX).javaVersion("Java 8"));
    }
}
