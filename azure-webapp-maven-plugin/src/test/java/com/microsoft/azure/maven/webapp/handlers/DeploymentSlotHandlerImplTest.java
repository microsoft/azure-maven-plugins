/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.DeploymentSlots;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.DeployMojo;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.microsoft.azure.management.appservice.DeploymentSlot.DefinitionStages.Blank;
import org.mockito.junit.MockitoJUnitRunner;
import com.microsoft.azure.management.appservice.DeploymentSlot.DefinitionStages.WithCreate;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentSlotHandlerImplTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private DeploymentSlotHandlerImpl handler = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new DeploymentSlotHandlerImpl(mojo);
    }

    @Test
    public void handleDeploymentSlot() throws AzureAuthFailureException, MojoExecutionException {
        final DeploymentSlotHandlerImpl handlerSpy = spy(handler);
        final DeploymentSlotSetting slotSetting = mock(DeploymentSlotSetting.class);
        final WebApp app = mock(WebApp.class);

        doReturn(slotSetting).when(mojo).getDeploymentSlotSetting();
        doReturn(app).when(mojo).getWebApp();
        doReturn("").when(slotSetting).getSlotName();
        doReturn(true).when(handlerSpy).isDeploymentSlotExists(app, "");
        handlerSpy.handleDeploymentSlot();
        verify(handlerSpy, times(0)).
                createDeploymentSlotWithConfigurationSource(app, "", "");
        verify(handlerSpy, times(1)).handleDeploymentSlot();
    }

    @Test
    public void handleDeploymentSlot_2() throws AzureAuthFailureException, MojoExecutionException {
        final DeploymentSlotHandlerImpl handlerSpy = spy(handler);
        final DeploymentSlotSetting slotSetting = mock(DeploymentSlotSetting.class);
        final WebApp app = mock(WebApp.class);
        final Log logMock = mock(Log.class);

        doReturn(logMock).when(mojo).getLog();
        doReturn(slotSetting).when(mojo).getDeploymentSlotSetting();
        doReturn(app).when(mojo).getWebApp();
        doReturn("").when(slotSetting).getSlotName();
        doReturn("").when(slotSetting).getConfigurationSource();
        doReturn(false).when(handlerSpy).isDeploymentSlotExists(app, "");
        doNothing().when(handlerSpy).createDeploymentSlotWithConfigurationSource(app, "", "");

        handlerSpy.handleDeploymentSlot();
        verify(handlerSpy, times(1)).handleDeploymentSlot();
        verify(handlerSpy, times(1)).
                createDeploymentSlotWithConfigurationSource(app, "", "");
    }

    @Test
    public void createDeploymentSlotWithConfigurationSource() {
        final DeploymentSlotHandlerImpl handlerSpy = spy(handler);
        final WebApp app = mock(WebApp.class);
        final Log logMock = mock(Log.class);
        final DeploymentSlots slots = mock(DeploymentSlots.class);
        final Blank stage1 = mock(Blank.class);
        final WithCreate withCreate = mock(WithCreate.class);


        doReturn(logMock).when(mojo).getLog();
        doReturn(slots).when(app).deploymentSlots();
        doReturn(stage1).when(slots).define("");
        doReturn(null).when(mojo).getDeploymentSlot(app, "");
        doReturn(false).when(handlerSpy).isCreateSlotWithoutConfiguration("");
        doReturn(withCreate).when(stage1).withConfigurationFromParent();

        handlerSpy.createDeploymentSlotWithConfigurationSource(app, "", "");

        verify(withCreate, times(1)).create();
    }

    @Test
    public void createDeploymentSlotWithConfigurationSource_2() {
        final DeploymentSlotHandlerImpl handlerSpy = spy(handler);
        final WebApp app = mock(WebApp.class);
        final Log logMock = mock(Log.class);
        final DeploymentSlots slots = mock(DeploymentSlots.class);
        final DeploymentSlot slot = mock(DeploymentSlot.class);
        final Blank stage1 = mock(Blank.class);
        final WithCreate withCreate = mock(WithCreate.class);


        doReturn(logMock).when(mojo).getLog();
        doReturn(slots).when(app).deploymentSlots();
        doReturn(slot).when(mojo).getDeploymentSlot(app, "");
        doReturn(stage1).when(slots).define("");
        doReturn(withCreate).when(stage1).withConfigurationFromDeploymentSlot(slot);

        handlerSpy.createDeploymentSlotWithConfigurationSource(app, "", "");

        verify(withCreate, times(1)).create();
    }

    @Test
    public void createDeploymentSlotWithConfigurationSource_3() {
        final DeploymentSlotHandlerImpl handlerSpy = spy(handler);
        final WebApp app = mock(WebApp.class);
        final Log logMock = mock(Log.class);
        final DeploymentSlots slots = mock(DeploymentSlots.class);
        final Blank stage1 = mock(Blank.class);
        final WithCreate withCreate = mock(WithCreate.class);

        doReturn(logMock).when(mojo).getLog();
        doReturn(slots).when(app).deploymentSlots();
        doReturn(null).when(mojo).getDeploymentSlot(app, "");
        doReturn(true).when(handlerSpy).isCreateSlotWithoutConfiguration("");
        doReturn(stage1).when(slots).define("");
        doReturn(withCreate).when(stage1).withBrandNewConfiguration();

        handlerSpy.createDeploymentSlotWithConfigurationSource(app, "", "");

        verify(withCreate, times(1)).create();
    }

    @Test
    public void isDeploymentSlotExist() throws AzureAuthFailureException {
        final DeploymentSlotHandlerImpl handlerSpy = spy(handler);
        final WebApp app = mock(WebApp.class);
        doReturn(app).when(mojo).getWebApp();

        handlerSpy.isDeploymentSlotExists(app, "");

        verify(handlerSpy, times(1)).isDeploymentSlotExists(app, "");
    }
}
