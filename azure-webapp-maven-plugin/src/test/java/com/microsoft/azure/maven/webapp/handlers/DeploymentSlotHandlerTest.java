/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.DeploymentSlot.DefinitionStages.Blank;
import com.microsoft.azure.management.appservice.DeploymentSlot.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.DeploymentSlots;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.common.appservice.DeploymentSlotSetting;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentSlotHandlerTest {
    @Mock
    private AbstractWebAppMojo mojo;

    private DeploymentSlotHandler handler = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new DeploymentSlotHandler(mojo);
    }

    @Test
    public void notCreateDeploymentSlotIfExist() throws AzureAuthFailureException, AzureExecutionException {
        final DeploymentSlotHandler handlerSpy = spy(handler);
        final DeploymentSlotSetting slotSetting = mock(DeploymentSlotSetting.class);
        final WebApp app = mock(WebApp.class);
        final DeploymentSlot slot = mock(DeploymentSlot.class);

        doReturn(slotSetting).when(mojo).getDeploymentSlotSetting();
        doReturn(app).when(mojo).getWebApp();
        doReturn("").when(slotSetting).getName();
        doReturn(slot).when(mojo).getDeploymentSlot(app, "");
        handlerSpy.createDeploymentSlotIfNotExist();

        verify(handlerSpy, times(0)).createDeploymentSlot(app, "", "");
        verify(handlerSpy, times(1)).createDeploymentSlotIfNotExist();
    }

    @Test
    public void createDeploymentSlotIfNotExist() throws AzureAuthFailureException, AzureExecutionException {
        final DeploymentSlotHandler handlerSpy = spy(handler);
        final DeploymentSlotSetting slotSetting = mock(DeploymentSlotSetting.class);
        final WebApp app = mock(WebApp.class);

        doReturn(app).when(mojo).getWebApp();
        doReturn(slotSetting).when(mojo).getDeploymentSlotSetting();
        doReturn("").when(slotSetting).getConfigurationSource();
        doReturn("").when(slotSetting).getName();
        doReturn(null).when(mojo).getDeploymentSlot(app, "");
        doNothing().when(handlerSpy).createDeploymentSlot(app, "", "");

        handlerSpy.createDeploymentSlotIfNotExist();
        verify(handlerSpy, times(1)).createDeploymentSlotIfNotExist();
        verify(handlerSpy, times(1)).createDeploymentSlot(app, "", "");
    }

    @Test
    public void createDeploymentSlotFromParent() throws AzureExecutionException {
        final DeploymentSlotHandler handlerSpy = spy(handler);
        final WebApp app = mock(WebApp.class);
        final DeploymentSlots slots = mock(DeploymentSlots.class);
        final Blank stage1 = mock(Blank.class);
        final WithCreate withCreate = mock(WithCreate.class);

        doReturn(slots).when(app).deploymentSlots();
        doReturn(stage1).when(slots).define("test");
        doReturn(withCreate).when(stage1).withConfigurationFromParent();

        handlerSpy.createDeploymentSlot(app, "test", "parent");

        verify(withCreate, times(1)).create();
    }

    @Test
    public void createDeploymentSlotFromOtherDeploymentSlot() throws AzureExecutionException {
        final DeploymentSlotHandler handlerSpy = spy(handler);
        final WebApp app = mock(WebApp.class);
        final DeploymentSlots slots = mock(DeploymentSlots.class);
        final Blank stage1 = mock(Blank.class);
        final DeploymentSlot slot = mock(DeploymentSlot.class);
        final WithCreate withCreate = mock(WithCreate.class);

        doReturn(slots).when(app).deploymentSlots();
        doReturn(stage1).when(slots).define("");
        doReturn(slot).when(mojo).getDeploymentSlot(app, "otherSlot");
        doNothing().when(handlerSpy).assureValidSlotName("");
        doReturn(withCreate).when(stage1).withConfigurationFromDeploymentSlot(slot);

        handlerSpy.createDeploymentSlot(app, "", "otherSlot");

        verify(withCreate, times(1)).create();
    }

    @Test(expected = AzureExecutionException.class)
    public void createDeploymentSlotFromOtherDeploymentSlotThrowException() throws AzureExecutionException {
        final WebApp app = mock(WebApp.class);

        handler.createDeploymentSlot(app, "", "otherSlot");
    }

    @Test(expected = AzureExecutionException.class)
    public void assureValidSlotNameThrowException() throws AzureExecutionException {
        final DeploymentSlotHandler handlerSpy = spy(handler);
        handlerSpy.assureValidSlotName("@#123");
    }

    @Test
    public void assureValidSlotName() throws AzureExecutionException {
        final DeploymentSlotHandler handlerSpy = spy(handler);
        handlerSpy.assureValidSlotName("slot-Name");
        verify(handlerSpy, times(1)).assureValidSlotName("slot-Name");
    }

    @Test
    public void createBreadNewDeploymentSlot() throws AzureExecutionException {
        final DeploymentSlotHandler handlerSpy = spy(handler);
        final WebApp app = mock(WebApp.class);
        final DeploymentSlots slots = mock(DeploymentSlots.class);
        final Blank stage1 = mock(Blank.class);
        final WithCreate withCreate = mock(WithCreate.class);

        doReturn(slots).when(app).deploymentSlots();
        doReturn(stage1).when(slots).define("test");
        doReturn(withCreate).when(stage1).withBrandNewConfiguration();

        handlerSpy.createDeploymentSlot(app, "test", "new");

        verify(withCreate, times(1)).create();
    }
}
