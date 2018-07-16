/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.WebAppBase.UpdateStages.WithWebContainer;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;

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
    public void defineAppWithRuntime() throws Exception {
    }

    @Test
    public void updateAppRuntime() throws Exception {
        // Success
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app").when(siteInner).kind();
        final WithWebContainer withWebContainer = mock(WithWebContainer.class);
        final Update update = mock(Update.class);
        doReturn(withWebContainer).when(update).withJavaVersion(null);
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();
        doReturn(update).when(app).update();
        doReturn(WebContainer.TOMCAT_8_5_NEWEST).when(mojo).getJavaWebContainer();

        assertSame(update, handler.updateAppRuntime(app));
        verify(withWebContainer, times(1)).withWebContainer(WebContainer.TOMCAT_8_5_NEWEST);
        verifyNoMoreInteractions(withWebContainer);
    }

}
