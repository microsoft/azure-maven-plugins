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
        when(siteInner.kind()).thenReturn("app,linux");
        final WebApp.Update update = mock(WebApp.Update.class);
        final WebApp app = mock(WebApp.class);
        when(app.inner()).thenReturn(siteInner);
        when(app.update()).thenReturn(update);
        when(mojo.getWebApp()).thenReturn(app);
        final ContainerSetting containerSetting = new ContainerSetting();
        containerSetting.setImageName("nginx");
        when(mojo.getContainerSettings()).thenReturn(containerSetting);

        handler.updateAppRuntime();
        verify(update, times(1)).withPublicDockerHubImage(any(String.class));
        verifyNoMoreInteractions(update);
    }
}
