/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.runtime;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.WebApp.UpdateStages.WithCredentials;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PrivateDockerHubRuntimeHandlerImplTest {
    @Mock
    private WebAppConfiguration config;

    @Mock
    private Azure azureClient;

    @Mock
    private Log log;

    private final PrivateDockerHubRuntimeHandlerImpl.Builder builder =
        new PrivateDockerHubRuntimeHandlerImpl.Builder();

    private PrivateDockerHubRuntimeHandlerImpl handler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void initHandlerV2() {
        handler = builder.appName(config.getAppName())
            .resourceGroup(config.getResourceGroup())
            .region(config.getRegion())
            .pricingTier(config.getPricingTier())
            .servicePlanName(config.getServicePlanName())
            .servicePlanResourceGroup((config.getServicePlanResourceGroup()))
            .azure(azureClient)
            .mavenSettings(config.getMavenSettings())
            .log(log)
            .image(config.getImage())
            .serverId(config.getServerId())
            .registryUrl(config.getRegistryUrl())
            .build();
    }

    private void initHandlerV1() {
        handler = builder.appName(config.getAppName())
            .resourceGroup(config.getResourceGroup())
            .region(config.getRegion())
            .pricingTier(config.getPricingTier())
            .servicePlanName(config.getServicePlanName())
            .servicePlanResourceGroup((config.getServicePlanResourceGroup()))
            .azure(azureClient)
            .mavenSettings(config.getMavenSettings())
            .image(config.getImage())
            .serverId(config.getServerId())
            .registryUrl(config.getRegistryUrl())
            .log(log)
            .build();
    }

    @Test
    public void updateAppRuntimeV2() throws Exception {
        final WebApp app = mock(WebApp.class);

        final SiteInner siteInner = mock(SiteInner.class);
        doReturn(siteInner).when(app).inner();
        doReturn("app,linux").when(siteInner).kind();

        final WebApp.Update update = mock(WebApp.Update.class);
        final WebApp.UpdateStages.WithCredentials withCredentials = mock(WebApp.UpdateStages.WithCredentials.class);
        doReturn(withCredentials).when(update).withPrivateDockerHubImage("");
        doReturn(update).when(app).update();

        final Server server = mock(Server.class);
        final Settings settings = mock(Settings.class);
        doReturn(server).when(settings).getServer(anyString());
        doReturn(settings).when(config).getMavenSettings();
        doReturn("").when(config).getImage();
        doReturn("serverId").when(config).getServerId();

        initHandlerV2();
        handler.updateAppRuntime(app);

        verify(update, times(1)).withPrivateDockerHubImage("");
        verify(server, times(1)).getUsername();
        verify(server, times(1)).getPassword();
    }

    @Test
    public void updateAppRuntimeV1() throws Exception {
        final WebApp app = mock(WebApp.class);
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        doReturn(siteInner).when(app).inner();
        final Update update = mock(Update.class);
        final WithCredentials withCredentials = mock(WithCredentials.class);
        doReturn(withCredentials).when(update).withPrivateDockerHubImage(null);
        doReturn(update).when(app).update();
        doReturn("serverId").when(config).getServerId();

        final Server server = mock(Server.class);
        final Settings settings = mock(Settings.class);
        doReturn(server).when(settings).getServer(anyString());
        doReturn(settings).when(config).getMavenSettings();

        initHandlerV1();
        handler.updateAppRuntime(app);

        verify(update, times(1)).withPrivateDockerHubImage(null);
        verify(server, times(1)).getUsername();
        verify(server, times(1)).getPassword();
    }
}
