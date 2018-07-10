/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentSlotHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private DeploymentSlotHandlerImpl handler = null;

    private DeploymentSlotHandlerImpl handlerSpy = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new DeploymentSlotHandlerImpl(mojo);
        handlerSpy = spy(handler);
    }

    @Test
    public void handleDeploymentSlot() throws AzureAuthFailureException, MojoExecutionException {
        doNothing().when(handlerSpy).handleDeploymentSlot();

        handlerSpy.handleDeploymentSlot();

        verify(handlerSpy, times(1)).handleDeploymentSlot();
    }

    @Test
    public void assureValidSlotSetting() throws MojoExecutionException {
        final DeploymentSlotSetting slotSetting = mock(DeploymentSlotSetting.class);

        handlerSpy.assureValidSlotSetting(slotSetting);

        assertNotNull(slotSetting);
    }
}
