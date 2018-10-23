/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v2;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
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
public class PrivateDockerHubRuntimeHandlerImplV2Test {
    @Mock
    private AbstractWebAppMojo mojo;

    private final PrivateDockerHubRuntimeHandlerImplV2.Builder builder =
        new PrivateDockerHubRuntimeHandlerImplV2.Builder();

    private PrivateDockerHubRuntimeHandlerImplV2 handler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void initHandler() throws AzureAuthFailureException {
        handler = builder.runtime(mojo.getRuntime())
            .appName(mojo.getAppName())
            .resourceGroup(mojo.getResourceGroup())
            .region(mojo.getRegion())
            .pricingTier(mojo.getPricingTier())
            .servicePlanName(mojo.getAppServicePlanName())
            .servicePlanResourceGroup((mojo.getAppServicePlanResourceGroup()))
            .azure(mojo.getAzureClient())
            .settings(mojo.getSettings())
            .log(mojo.getLog())
            .build();
    }

    @Test
    public void updateAppRuntime() throws Exception {
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
        doReturn(settings).when(mojo).getSettings();

        final RuntimeSetting runtime = mock(RuntimeSetting.class);
        doReturn(runtime).when(mojo).getRuntime();
        doReturn("").when(runtime).getImage();
        doReturn("serverId").when(runtime).getServerId();

        initHandler();
        handler.updateAppRuntime(app);

        verify(update, times(1)).withPrivateDockerHubImage("");
        verify(server, times(1)).getUsername();
        verify(server, times(1)).getPassword();
    }
}
