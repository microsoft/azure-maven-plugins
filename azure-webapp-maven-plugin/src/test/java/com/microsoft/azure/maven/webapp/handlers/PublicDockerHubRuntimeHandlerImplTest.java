/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class PublicDockerHubRuntimeHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private PublicDockerHubRuntimeHandlerImpl handler = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        handler = new PublicDockerHubRuntimeHandlerImpl(mojo);
    }

    @Test
    public void defineAppWithRunTime() throws Exception {
    }

    @Test
    public void updateAppRuntime() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        final Update update = mock(Update.class);
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();
        doReturn(update).when(app).update();
        doReturn(app).when(mojo).getWebApp();
        final ContainerSetting containerSetting = new ContainerSetting();
        containerSetting.setImageName("nginx");
        doReturn(containerSetting).when(mojo).getContainerSettings();

        handler.updateAppRuntime();
        verify(update, times(1)).withPublicDockerHubImage(any(String.class));
        verifyNoMoreInteractions(update);
    }
}
