/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.handlers.RuntimeHandler;
import com.microsoft.azure.maven.webapp.handlers.SettingsHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployFacadeImplWithUpdateTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private DeployFacadeImplWithUpdate facade = null;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        facade = new DeployFacadeImplWithUpdate(mojo);
    }

    @Test
    public void setupRuntime() throws Exception {
        final DeployFacadeBaseImpl facadeSpy = spy(facade);
        final RuntimeHandler handler = mock(RuntimeHandler.class);
        doReturn(handler).when(facadeSpy).getRuntimeHandler();

        facadeSpy.setupRuntime();
        verify(handler, times(1)).updateAppRuntime();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void applySettings() throws Exception {
        final DeployFacadeBaseImpl facadeSpy = spy(facade);
        final SettingsHandler handler = mock(SettingsHandler.class);
        doReturn(handler).when(facadeSpy).getSettingsHandler();

        facadeSpy.applySettings();
        verify(handler, times(1)).processSettings((Update) null);
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void commitChanges() throws Exception {
        final Update update = mock(Update.class);
        ReflectionTestUtils.setField(facade, "update", update);

        facade.commitChanges();
        verify(update, times(1)).apply();
        verifyNoMoreInteractions(update);
    }
}
