/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp.Update;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployFacadeImplWithUpdateTest extends DeployFacadeTestBase {
    private DeployFacadeImplWithUpdate facade = null;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        facade = new DeployFacadeImplWithUpdate(mojo);
        setupHandlerFactory();
    }

    @Test
    public void setupRuntime() throws Exception {
        final DeployFacadeBaseImpl facadeSpy = spy(facade);

        facadeSpy.setupRuntime();
        verify(runtimeHandler, times(1)).updateAppRuntime();
        verifyNoMoreInteractions(runtimeHandler);
    }

    @Test
    public void applySettings() throws Exception {
        final DeployFacadeBaseImpl facadeSpy = spy(facade);

        facadeSpy.applySettings();
        verify(settingsHandler, times(1)).processSettings((Update) null);
        verifyNoMoreInteractions(settingsHandler);
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
