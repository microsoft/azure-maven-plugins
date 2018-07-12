/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SettingsHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private SettingsHandlerImpl handler = null;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(new HashMap() {
            {
                put("Key", "Value");
            }
        }).when(mojo).getAppSettings();
        handler = new SettingsHandlerImpl(mojo);
    }

    @Test
    public void processSettings() throws Exception {
        final WithCreate withCreate = mock(WithCreate.class);
        handler.processSettings(withCreate);
        verify(withCreate, times(1)).withAppSettings(ArgumentMatchers.<String, String>anyMap());
    }

    @Test
    public void processSettings1() throws Exception {
        final Update update = mock(Update.class);
        handler.processSettings(update);
        verify(update, times(1)).withAppSettings(ArgumentMatchers.<String, String>anyMap());
    }
}
