/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JavaRuntimeHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private JavaRuntimeHandlerImpl handler = null;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        handler = new JavaRuntimeHandlerImpl(mojo);
    }

    @Test
    public void defineAppWithRunTime() throws Exception {
    }

    @Test
    public void updateAppRuntime() throws Exception {
        // Success
        final SiteInner siteInner = mock(SiteInner.class);
        when(siteInner.kind()).thenReturn("app");
        final WebAppBase.UpdateStages.WithWebContainer withWebContainer =
                mock(WebAppBase.UpdateStages.WithWebContainer.class);
        final WebApp.Update update = mock(WebApp.Update.class);
        when(update.withJavaVersion(null)).thenReturn(withWebContainer);
        final WebApp app = mock(WebApp.class);
        when(app.inner()).thenReturn(siteInner);
        when(app.update()).thenReturn(update);
        when(mojo.getWebApp()).thenReturn(app);
        when(mojo.getJavaWebContainer()).thenReturn(WebContainer.TOMCAT_8_5_NEWEST);

        assertSame(update, handler.updateAppRuntime());
        verify(withWebContainer, times(1)).withWebContainer(WebContainer.TOMCAT_8_5_NEWEST);
        verifyNoMoreInteractions(withWebContainer);
    }

}
