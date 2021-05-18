/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionDeploymentSlot;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.telemetry.TelemetryProxy;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTarget;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTargetType;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.ArtifactHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.runtime.FunctionRuntimeHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class DeployMojoTest extends MojoTestBase {
    private DeployMojo mojo = null;
    private DeployMojo mojoSpy = null;

    @Before
    public void setUp() throws Exception {
        mojo = getMojoFromPom();
        mojoSpy = spy(mojo);
    }

    @Test
    public void getConfiguration() throws Exception {
        assertEquals("resourceGroupName", mojo.getResourceGroup());

        assertEquals("appName", mojo.getAppName());

        assertEquals("westeurope", mojo.getRegion());
    }

    @Test
    @Ignore
    public void doExecute() throws Exception {
        final ArtifactHandler handler = mock(ArtifactHandler.class);
        final FunctionApp app = mock(FunctionApp.class);
        doReturn(app).when(mojoSpy).getFunctionApp();
        doCallRealMethod().when(mojoSpy).createOrUpdateResource();
        final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.FUNCTION);
        doNothing().when(mojoSpy).listHTTPTriggerUrls(any());
        doReturn(null).when(mojoSpy).getResourcePortalUrl(any());
        final TelemetryProxy telemetryProxy = mock(TelemetryProxy.class);
        doNothing().when(telemetryProxy).addDefaultProperty(any(), any());
        doReturn(telemetryProxy).when(mojoSpy).getTelemetryProxy();
        mojoSpy.doExecute();
        verify(mojoSpy, times(1)).createOrUpdateResource();
        verify(mojoSpy, times(1)).doExecute();
        verify(handler, times(1)).publish(refEq(deployTarget));
        verifyNoMoreInteractions(handler);
    }

    @Ignore
    @Test(expected = AzureExecutionException.class)
    public void testDeploymentSlotThrowExceptionIfFunctionNotExists() throws AzureAuthFailureException, AzureExecutionException {
        final DeploymentSlotSetting slotSetting = new DeploymentSlotSetting();
        slotSetting.setName("Exception");
        doReturn(slotSetting).when(mojoSpy).getDeploymentSlotSetting();
        doReturn(null).when(mojoSpy).getFunctionApp();
        mojoSpy.doExecute();
    }

    @Test
    @Ignore
    public void testCreateDeploymentSlot() throws AzureAuthFailureException, AzureExecutionException {
        final FunctionDeploymentSlot slot = mock(FunctionDeploymentSlot.class);
        final DeploymentSlotSetting slotSetting = new DeploymentSlotSetting();
        slotSetting.setName("Test");
        doReturn(slotSetting).when(mojoSpy).getDeploymentSlotSetting();

        final FunctionRuntimeHandler runtimeHandler = mock(FunctionRuntimeHandler.class);
        final FunctionDeploymentSlot.DefinitionStages.WithCreate mockWithCreate = mock(FunctionDeploymentSlot.DefinitionStages.WithCreate.class);
        doReturn(slot).when(mockWithCreate).create();
        doReturn(mockWithCreate).when(runtimeHandler).createDeploymentSlot(any(), any());

        final ArtifactHandler artifactHandler = mock(ArtifactHandler.class);
        doNothing().when(artifactHandler).publish(any());

        final FunctionApp app = mock(FunctionApp.class);
        doReturn(app).when(mojoSpy).getFunctionApp();
        doReturn(slot).when(mojoSpy).updateDeploymentSlot(any());
        doCallRealMethod().when(mojoSpy).createDeploymentSlot(any());
        doReturn(null).when(mojoSpy).getResourcePortalUrl(any());
        final TelemetryProxy telemetryProxy = mock(TelemetryProxy.class);
        doNothing().when(telemetryProxy).addDefaultProperty(any(), any());
        doReturn(telemetryProxy).when(mojoSpy).getTelemetryProxy();

        try (MockedStatic<FunctionUtils> mockFunctionUtils = Mockito.mockStatic(FunctionUtils.class)) {
            mockFunctionUtils.when(() -> FunctionUtils.getFunctionDeploymentSlotByName(any(), any())).thenReturn(null);
            mojoSpy.doExecute();
        }

        verify(mojoSpy, times(1)).doExecute();
        verify(mojoSpy, times(1)).createOrUpdateResource();
        verify(mojoSpy, times(1)).createDeploymentSlot(any());
        // Will call slot update while creation as we can't modify app settings during creation
        verify(mojoSpy, times(1)).updateDeploymentSlot(any());
        verify(artifactHandler, times(1)).publish(any());
        verifyNoMoreInteractions(artifactHandler);
    }

    private DeployMojo getMojoFromPom() throws Exception {
        final DeployMojo mojoFromPom = (DeployMojo) getMojoFromPom("/pom.xml", "deploy");
        assertNotNull(mojoFromPom);
        return mojoFromPom;
    }
}
