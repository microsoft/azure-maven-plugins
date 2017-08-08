/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.DeploymentType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SettingsHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private SettingsHandlerImpl handler = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mojo.getAppSettings()).thenReturn(new HashMap() {
            {
                put("Key", "Value");
            }
        });
        when(mojo.getDeploymentType()).thenReturn(DeploymentType.LOCAL_GIT);
        handler = new SettingsHandlerImpl(mojo);
    }

    @Test
    public void processSettings() throws Exception {
        final WebApp.DefinitionStages.WithCreate withCreate = mock(WebApp.DefinitionStages.WithCreate.class);
        handler.processSettings(withCreate);
        verify(withCreate, times(1)).withAppSettings(ArgumentMatchers.<String, String>anyMap());
        verify(withCreate, times(1)).withLocalGitSourceControl();
    }

    @Test
    public void processSettings1() throws Exception {
        final WebApp.Update update = mock(WebApp.Update.class);
        handler.processSettings(update);
        verify(update, times(1)).withAppSettings(ArgumentMatchers.<String, String>anyMap());
        verify(update, times(1)).withLocalGitSourceControl();
    }
}
