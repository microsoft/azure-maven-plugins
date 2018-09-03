/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v1;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.WebApp.UpdateStages.WithCredentials;
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

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PrivateRegistryRuntimeHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private PrivateRegistryRuntimeHandlerImpl handler = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        handler = new PrivateRegistryRuntimeHandlerImpl(mojo);
    }

    @Test
    public void defineAppWithRuntime() throws Exception {
    }

    @Test
    public void updateAppRuntime() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        final WithCredentials withCredentials = mock(WithCredentials.class);
        final Update update = mock(Update.class);
        doReturn(withCredentials).when(update).withPrivateRegistryImage(null, null);
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();
        doReturn(update).when(app).update();

        final ContainerSetting containerSetting = new ContainerSetting();
        containerSetting.setServerId("serverId");
        doReturn(containerSetting).when(mojo).getContainerSettings();

        final Server server = mock(Server.class);
        final Settings settings = mock(Settings.class);
        doReturn(server).when(settings).getServer(anyString());
        doReturn(settings).when(mojo).getSettings();

        handler.updateAppRuntime(app);

        verify(update, times(1)).withPrivateRegistryImage(null, null);
        verify(server, times(1)).getUsername();
        verify(server, times(1)).getPassword();
    }
}
