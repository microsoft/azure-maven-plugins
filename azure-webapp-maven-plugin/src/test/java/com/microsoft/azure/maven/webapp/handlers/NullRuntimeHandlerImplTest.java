/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class NullRuntimeHandlerImplTest {
    private NullRuntimeHandlerImpl handler = null;

    @Before
    public void setup() throws Exception {
        handler = new NullRuntimeHandlerImpl();
    }

    @Test
    public void defineAppWithRuntime() throws Exception {
        MojoExecutionException exception = null;
        try {
            handler.defineAppWithRuntime(null);
        } catch (MojoExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }
    }

    @Test
    public void updateAppRuntime() {
        final WebApp app = mock(WebApp.class);
        assertNull(handler.updateAppRuntime(app));
    }
}
