/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NullRuntimeHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private NullRuntimeHandlerImpl handler = null;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        handler = new NullRuntimeHandlerImpl(mojo);
    }

    @Test
    public void defineAppWithRunTime() throws Exception {
        MojoExecutionException exception = null;
        try {
            handler.defineAppWithRunTime();
        } catch (MojoExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }
    }

    @Test
    public void updateAppRuntime() throws Exception {
        final WebApp.Update update = mock(WebApp.Update.class);
        final WebApp app = mock(WebApp.class);
        when(app.update()).thenReturn(update);
        when(mojo.getWebApp()).thenReturn(app);

        assertSame(update, handler.updateAppRuntime());
    }
}
