/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployFacadeImplWithCreateTest extends DeployFacadeTestBase {
    private DeployFacadeImplWithCreate facade = null;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(log).when(mojo).getLog();
        facade = new DeployFacadeImplWithCreate(mojo);
        setupHandlerFactory();
    }

    @Test
    public void setupRuntime() throws Exception {
        final DeployFacadeBaseImpl facadeSpy = spy(facade);

        facadeSpy.setupRuntime();
        verify(runtimeHandler, times(1)).defineAppWithRunTime();
        verifyNoMoreInteractions(runtimeHandler);
    }

    @Test
    public void applySettings() throws Exception {
        final DeployFacadeBaseImpl facadeSpy = spy(facade);

        facadeSpy.applySettings();
        verify(settingsHandler, times(1)).processSettings((WithCreate) null);
        verifyNoMoreInteractions(settingsHandler);
    }

    @Test
    public void commitChanges() throws Exception {
        final WithCreate withCreate = mock(WithCreate.class);
        ReflectionTestUtils.setField(facade, "withCreate", withCreate);

        facade.commitChanges();
        verify(withCreate, times(1)).create();
        verifyNoMoreInteractions(withCreate);
    }
}
