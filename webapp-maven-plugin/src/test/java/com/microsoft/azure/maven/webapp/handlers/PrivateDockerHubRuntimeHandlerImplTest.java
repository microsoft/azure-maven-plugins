/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class PrivateDockerHubRuntimeHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private PrivateDockerHubRuntimeHandlerImpl handler = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        handler = new PrivateDockerHubRuntimeHandlerImpl(mojo);
    }
    @Test
    public void defineAppWithRunTime() throws Exception {
    }

    @Test
    public void updateAppRuntime() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        when(siteInner.kind()).thenReturn("app,linux");
        final WebApp.UpdateStages.WithCredentials withCredentials = mock(WebApp.UpdateStages.WithCredentials.class);
        final WebApp.Update update = mock(WebApp.Update.class);
        when(update.withPrivateDockerHubImage(null)).thenReturn(withCredentials);
        final WebApp app = mock(WebApp.class);
        when(app.inner()).thenReturn(siteInner);
        when(app.update()).thenReturn(update);
        when(mojo.getWebApp()).thenReturn(app);

        final ContainerSetting containerSetting = new ContainerSetting();
        containerSetting.setServerId("serverId");
        when(mojo.getContainerSettings()).thenReturn(containerSetting);

        final Server server = mock(Server.class);
        final Settings settings = mock(Settings.class);
        when(settings.getServer("serverId")).thenReturn(server);
        when(mojo.getSettings()).thenReturn(settings);

        handler.updateAppRuntime();
        verify(update, times(1)).withPrivateDockerHubImage(null);
        verify(server, times(1)).getUsername();
        verify(server, times(1)).getPassword();
    }
}
