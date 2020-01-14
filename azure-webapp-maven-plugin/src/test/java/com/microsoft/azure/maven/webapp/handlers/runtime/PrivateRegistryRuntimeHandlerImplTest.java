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
import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;

import org.apache.commons.lang3.StringUtils;
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
public class PrivateRegistryRuntimeHandlerImplTest {
    @Mock
    private WebAppConfiguration config;

    @Mock
    private Azure azureClient;

    private final PrivateRegistryRuntimeHandlerImpl.Builder builder =
        new PrivateRegistryRuntimeHandlerImpl.Builder();

    private PrivateRegistryRuntimeHandlerImpl handler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void initHandlerForV2() {
        if (StringUtils.isNotBlank(config.getServerId())) {
            builder.dockerCredentialProvider(new MavenDockerCredentialProvider(config.getMavenSettings(), config.getServerId()));
        }
        handler = builder.appName(config.getAppName())
            .resourceGroup(config.getResourceGroup())
            .region(config.getRegion())
            .pricingTier(config.getPricingTier())
            .servicePlanName(config.getServicePlanName())
            .servicePlanResourceGroup((config.getServicePlanResourceGroup()))
            .azure(azureClient)
            .image(config.getImage())
            .registryUrl(config.getRegistryUrl())
            .build();
    }

    private void initHandlerV1() {
        if (StringUtils.isNotBlank(config.getServerId())) {
            builder.dockerCredentialProvider(new MavenDockerCredentialProvider(config.getMavenSettings(), config.getServerId()));
        }
        handler = builder.appName(config.getAppName())
            .resourceGroup(config.getResourceGroup())
            .region(config.getRegion())
            .pricingTier(config.getPricingTier())
            .servicePlanName(config.getServicePlanName())
            .servicePlanResourceGroup((config.getServicePlanResourceGroup()))
            .azure(azureClient)
            .image(config.getImage())
            .registryUrl(config.getRegistryUrl())
            .build();
    }

    @Test
    public void updateAppRuntimeV2() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        final WebApp.UpdateStages.WithCredentials withCredentials = mock(WebApp.UpdateStages.WithCredentials.class);
        final WebApp.Update update = mock(WebApp.Update.class);
        doReturn(withCredentials).when(update).withPrivateRegistryImage("", "");
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();
        doReturn(update).when(app).update();

        final Server server = mock(Server.class);
        final Settings settings = mock(Settings.class);
        doReturn(server).when(settings).getServer(anyString());
        doReturn(settings).when(config).getMavenSettings();
        doReturn("").when(config).getImage();
        doReturn("").when(config).getRegistryUrl();
        doReturn("serverId").when(config).getServerId();

        initHandlerForV2();

        handler.updateAppRuntime(app);

        verify(update, times(1)).withPrivateRegistryImage("", "");
        verify(server, times(1)).getUsername();
        verify(server, times(1)).getPassword();
    }

    @Test
    public void updateAppRuntimeV1() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        final WithCredentials withCredentials = mock(WithCredentials.class);
        final Update update = mock(Update.class);
        doReturn(withCredentials).when(update).withPrivateRegistryImage(null, null);
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();
        doReturn(update).when(app).update();
        doReturn("serverId").when(config).getServerId();

        final Server server = mock(Server.class);
        final Settings settings = mock(Settings.class);
        doReturn(server).when(settings).getServer(anyString());
        doReturn(settings).when(config).getMavenSettings();

        initHandlerV1();
        handler.updateAppRuntime(app);

        verify(update, times(1)).withPrivateRegistryImage(null, null);
        verify(server, times(1)).getUsername();
        verify(server, times(1)).getPassword();
    }

}
