/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v1;

import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LinuxRuntimeHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private LinuxRuntimeHandlerImpl handler = null;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new LinuxRuntimeHandlerImpl(mojo);
    }

    @Test
    public void updateAppRuntimeTest() throws Exception {
        final WebApp app = mock(WebApp.class);
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        doReturn(siteInner).when(app).inner();
        final Update update = mock(Update.class);
        doReturn(update).when(app).update();
        doReturn(update).when(update).withBuiltInImage(any(RuntimeStack.class));
        doReturn(RuntimeStack.TOMCAT_8_5_JRE8.toString()).when(mojo).getLinuxRuntime();

        assertSame(update, handler.updateAppRuntime(app));
        verify(update, times(1)).withBuiltInImage(any(RuntimeStack.class));
    }
}
