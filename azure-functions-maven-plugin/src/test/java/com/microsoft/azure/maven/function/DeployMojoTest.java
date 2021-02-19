/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.common.appservice.DeployTargetType;
import com.microsoft.azure.common.appservice.DeploymentSlotSetting;
import com.microsoft.azure.common.appservice.DeploymentType;
import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.handlers.artifact.MSDeployArtifactHandlerImpl;
import com.microsoft.azure.common.function.handlers.runtime.FunctionRuntimeHandler;
import com.microsoft.azure.common.function.handlers.runtime.WindowsFunctionRuntimeHandler;
import com.microsoft.azure.common.function.utils.FunctionUtils;
import com.microsoft.azure.common.handlers.ArtifactHandler;
import com.microsoft.azure.common.handlers.artifact.FTPArtifactHandlerImpl;
import com.microsoft.azure.common.handlers.artifact.ZIPArtifactHandlerImpl;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.management.appservice.FunctionDeploymentSlot;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.telemetry.TelemetryProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.function.Consumer;

import static com.microsoft.azure.common.appservice.DeploymentType.DOCKER;
import static com.microsoft.azure.common.appservice.DeploymentType.RUN_FROM_BLOB;
import static com.microsoft.azure.common.appservice.DeploymentType.RUN_FROM_ZIP;
import static com.microsoft.azure.common.appservice.OperatingSystemEnum.Docker;
import static com.microsoft.azure.common.appservice.OperatingSystemEnum.Linux;
import static com.microsoft.azure.common.appservice.OperatingSystemEnum.Windows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FunctionUtils.class})
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
    public void doExecute() throws Exception {
        final ArtifactHandler handler = mock(ArtifactHandler.class);
        final FunctionRuntimeHandler runtimeHandler = mock(FunctionRuntimeHandler.class);
        final FunctionApp app = mock(FunctionApp.class);
        doReturn(app).when(mojoSpy).getFunctionApp();
        doReturn(app).when(mojoSpy).updateFunctionApp(app, runtimeHandler);
        doReturn(handler).when(mojoSpy).getArtifactHandler();
        doReturn(runtimeHandler).when(mojoSpy).getFunctionRuntimeHandler();
        doCallRealMethod().when(mojoSpy).createOrUpdateResource();
        final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.FUNCTION);
        doNothing().when(mojoSpy).listHTTPTriggerUrls();
        doNothing().when(mojoSpy).checkArtifactCompileVersion();
        doNothing().when(mojoSpy).parseConfiguration();
        doReturn(null).when(mojoSpy).getResourcePortalUrl(any());
        final TelemetryProxy telemetryProxy = mock(TelemetryProxy.class);
        doNothing().when(telemetryProxy).addDefaultProperty(any(), any());
        doReturn(telemetryProxy).when(mojoSpy).getTelemetryProxy();
        mojoSpy.doExecute();
        verify(mojoSpy, times(1)).createOrUpdateResource();
        verify(mojoSpy, times(1)).doExecute();
        verify(mojoSpy, times(1)).updateFunctionApp(any(FunctionApp.class), any());
        verify(handler, times(1)).publish(refEq(deployTarget));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void createFunctionAppIfNotExist() throws Exception {
        doReturn(null).when(mojoSpy).getFunctionApp();
        doReturn(null).when(mojoSpy).getFunctionRuntimeHandler();
        doReturn(null).when(mojoSpy).createFunctionApp(any());

        mojoSpy.createOrUpdateResource();

        verify(mojoSpy).createFunctionApp(any());
    }

    @Test
    public void updateFunctionApp() throws Exception {
        final FunctionApp app = mock(FunctionApp.class);
        final Update update = mock(Update.class);
        doNothing().when(mojoSpy).configureAppSettings(any(Consumer.class), anyMap());
        final FunctionRuntimeHandler functionRuntimeHandler = mock(WindowsFunctionRuntimeHandler.class);
        doReturn(update).when(functionRuntimeHandler).updateAppRuntime(app);
        mojoSpy.updateFunctionApp(app, functionRuntimeHandler);

        verify(update, times(1)).apply();
    }

    @Test
    public void configureAppSettings() throws Exception {
        final WithCreate withCreate = mock(WithCreate.class);

        mojo.configureAppSettings(withCreate::withAppSettings, mojo.getAppSettings());

        verify(withCreate, times(1)).withAppSettings(anyMap());
    }

    @Test
    public void getMSDeployArtifactHandler() throws AzureExecutionException {
        final TelemetryProxy mockProxy = mock(TelemetryProxy.class);
        doReturn(mockProxy).when(mojoSpy).getTelemetryProxy();
        doNothing().when(mockProxy).addDefaultProperty(any(), any());
        doReturn("azure-functions-maven-plugin").when(mojoSpy).getPluginName();
        doReturn("test-path").when(mojoSpy).getBuildDirectoryAbsolutePath();
        doReturn(DeploymentType.MSDEPLOY).when(mojoSpy).getDeploymentType();
        final ArtifactHandler handler = mojoSpy.getArtifactHandler();

        assertNotNull(handler);
        assertTrue(handler instanceof MSDeployArtifactHandlerImpl);
    }

    @Test
    public void getFTPArtifactHandler() throws AzureExecutionException {
        final TelemetryProxy mockProxy = mock(TelemetryProxy.class);
        doReturn(mockProxy).when(mojoSpy).getTelemetryProxy();
        doNothing().when(mockProxy).addDefaultProperty(any(), any());
        doReturn("azure-functions-maven-plugin").when(mojoSpy).getPluginName();
        doReturn("test-path").when(mojoSpy).getBuildDirectoryAbsolutePath();
        doReturn(DeploymentType.FTP).when(mojoSpy).getDeploymentType();
        final ArtifactHandler handler = mojoSpy.getArtifactHandler();

        assertNotNull(handler);
        assertTrue(handler instanceof FTPArtifactHandlerImpl);
    }

    @Test
    public void getZIPArtifactHandler() throws AzureExecutionException {
        final TelemetryProxy mockProxy = mock(TelemetryProxy.class);
        doReturn(mockProxy).when(mojoSpy).getTelemetryProxy();
        doNothing().when(mockProxy).addDefaultProperty(any(), any());
        doReturn("azure-functions-maven-plugin").when(mojoSpy).getPluginName();
        doReturn("test-path").when(mojoSpy).getBuildDirectoryAbsolutePath();
        doReturn(DeploymentType.ZIP).when(mojoSpy).getDeploymentType();
        final ArtifactHandler handler = mojoSpy.getArtifactHandler();

        assertNotNull(handler);
        assertTrue(handler instanceof ZIPArtifactHandlerImpl);
    }

    @Test(expected = AzureExecutionException.class)
    public void getArtifactHandlerThrowException() throws Exception {
        getMojoFromPom().getArtifactHandler();
    }

    @Test
    public void testGetDeploymentTypeByRuntime() throws AzureExecutionException {
        // Windows
        doReturn(Windows).when(mojoSpy).getOsEnum();
        assertEquals(RUN_FROM_ZIP, mojoSpy.getDeploymentTypeByRuntime());
        // Linux
        doReturn(Linux).when(mojoSpy).getOsEnum();
        doReturn(true).when(mojoSpy).isDedicatedPricingTier();
        assertEquals(RUN_FROM_ZIP, mojoSpy.getDeploymentTypeByRuntime());
        doReturn(false).when(mojoSpy).isDedicatedPricingTier();
        assertEquals(RUN_FROM_BLOB, mojoSpy.getDeploymentTypeByRuntime());
        // Docker
        doReturn(Docker).when(mojoSpy).getOsEnum();
        assertEquals(DOCKER, mojoSpy.getDeploymentTypeByRuntime());
    }

    @Test(expected = AzureExecutionException.class)
    public void testDeploymentSlotThrowExceptionIfFunctionNotExists() throws AzureAuthFailureException, AzureExecutionException {
        final DeploymentSlotSetting slotSetting = new DeploymentSlotSetting();
        slotSetting.setName("Exception");
        doReturn(slotSetting).when(mojoSpy).getDeploymentSlotSetting();
        doReturn(null).when(mojoSpy).getFunctionApp();
        doReturn(null).when(mojoSpy).getFunctionRuntimeHandler();
        doNothing().when(mojoSpy).parseConfiguration();
        doNothing().when(mojoSpy).checkArtifactCompileVersion();
        mojoSpy.doExecute();
    }

    @Test
    public void testCreateDeploymentSlot() throws AzureAuthFailureException, AzureExecutionException {
        final FunctionDeploymentSlot slot = mock(FunctionDeploymentSlot.class);
        final DeploymentSlotSetting slotSetting = new DeploymentSlotSetting();
        slotSetting.setName("Test");
        doReturn(slotSetting).when(mojoSpy).getDeploymentSlotSetting();

        final FunctionRuntimeHandler runtimeHandler = mock(FunctionRuntimeHandler.class);
        final FunctionDeploymentSlot.DefinitionStages.WithCreate mockWithCreate = mock(FunctionDeploymentSlot.DefinitionStages.WithCreate.class);
        doReturn(slot).when(mockWithCreate).create();
        doReturn(mockWithCreate).when(runtimeHandler).createDeploymentSlot(any(), any());
        doReturn(runtimeHandler).when(mojoSpy).getFunctionRuntimeHandler();

        final ArtifactHandler artifactHandler = mock(ArtifactHandler.class);
        doNothing().when(artifactHandler).publish(any());
        doReturn(artifactHandler).when(mojoSpy).getArtifactHandler();

        final FunctionApp app = mock(FunctionApp.class);
        doReturn(app).when(mojoSpy).getFunctionApp();
        doNothing().when(mojoSpy).parseConfiguration();
        doNothing().when(mojoSpy).checkArtifactCompileVersion();
        doReturn(slot).when(mojoSpy).updateDeploymentSlot(any(), any());
        doCallRealMethod().when(mojoSpy).createDeploymentSlot(any(), any());
        doReturn(null).when(mojoSpy).getResourcePortalUrl(any());
        PowerMockito.mockStatic(FunctionUtils.class);
        PowerMockito.when(FunctionUtils.getFunctionDeploymentSlotByName(any(), any())).thenReturn(null);
        final TelemetryProxy telemetryProxy = mock(TelemetryProxy.class);
        doNothing().when(telemetryProxy).addDefaultProperty(any(), any());
        doReturn(telemetryProxy).when(mojoSpy).getTelemetryProxy();
        mojoSpy.doExecute();

        verify(mojoSpy, times(1)).doExecute();
        verify(mojoSpy, times(1)).createOrUpdateResource();
        verify(mojoSpy, times(1)).createDeploymentSlot(any(), any());
        // Will call slot update while creation as we can't modify app settings during creation
        verify(mojoSpy, times(1)).updateDeploymentSlot(any(), any());
        verify(artifactHandler, times(1)).publish(any());
        verifyNoMoreInteractions(artifactHandler);
    }

    private DeployMojo getMojoFromPom() throws Exception {
        final DeployMojo mojoFromPom = (DeployMojo) getMojoFromPom("/pom.xml", "deploy");
        assertNotNull(mojoFromPom);
        return mojoFromPom;
    }
}
