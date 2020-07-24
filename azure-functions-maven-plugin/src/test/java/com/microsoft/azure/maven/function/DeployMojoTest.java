/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.common.appservice.DeployTargetType;
import com.microsoft.azure.common.appservice.DeploymentType;
import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.handlers.artifact.MSDeployArtifactHandlerImpl;
import com.microsoft.azure.common.function.handlers.runtime.FunctionRuntimeHandler;
import com.microsoft.azure.common.function.handlers.runtime.WindowsFunctionRuntimeHandler;
import com.microsoft.azure.common.handlers.ArtifactHandler;
import com.microsoft.azure.common.handlers.artifact.FTPArtifactHandlerImpl;
import com.microsoft.azure.common.handlers.artifact.ZIPArtifactHandlerImpl;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.maven.telemetry.TelemetryProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
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
    public void doExecute() throws Exception {
        final ArtifactHandler handler = mock(ArtifactHandler.class);
        final FunctionApp app = mock(FunctionApp.class);
        doReturn(app).when(mojoSpy).getFunctionApp();
        doReturn(handler).when(mojoSpy).getArtifactHandler();
        doCallRealMethod().when(mojoSpy).createOrUpdateFunctionApp();
        doCallRealMethod().when(mojoSpy).getAppName();
        final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.FUNCTION);
        doNothing().when(mojoSpy).updateFunctionApp(app);
        doNothing().when(mojoSpy).listHTTPTriggerUrls();
        mojoSpy.doExecute();
        verify(mojoSpy, times(1)).createOrUpdateFunctionApp();
        verify(mojoSpy, times(1)).doExecute();
        verify(mojoSpy, times(1)).updateFunctionApp(any(FunctionApp.class));
        verify(handler, times(1)).publish(refEq(deployTarget));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void createFunctionAppIfNotExist() throws Exception {
        doReturn(null).when(mojoSpy).getFunctionApp();
        doNothing().when(mojoSpy).createFunctionApp();

        mojoSpy.createOrUpdateFunctionApp();

        verify(mojoSpy).createFunctionApp();
    }

    @Test
    public void updateFunctionApp() throws Exception {
        final FunctionApp app = mock(FunctionApp.class);
        final Update update = mock(Update.class);
        doNothing().when(mojoSpy).configureAppSettings(any(Consumer.class), anyMap());
        final FunctionRuntimeHandler functionRuntimeHandler = mock(WindowsFunctionRuntimeHandler.class);
        doReturn(functionRuntimeHandler).when(mojoSpy).getFunctionRuntimeHandler();
        doReturn(update).when(functionRuntimeHandler).updateAppRuntime(app);
        mojoSpy.updateFunctionApp(app);

        verify(update, times(1)).apply();
    }

    @Test
    public void configureAppSettings() throws Exception {
        final WithCreate withCreate = mock(WithCreate.class);

        mojo.configureAppSettings(withCreate::withAppSettings, mojo.getAppSettingsWithDefaultValue());

        verify(withCreate, times(1)).withAppSettings(anyMap());
    }

    @Test
    public void testDefaultAppSettings() throws Exception {
        final Map settings = mojo.getAppSettingsWithDefaultValue();
        assertEquals("java", settings.get("FUNCTIONS_WORKER_RUNTIME"));
        assertEquals("~3", settings.get("FUNCTIONS_EXTENSION_VERSION"));
    }

    @Test
    public void testCustomAppSettings() throws Exception {
        final DeployMojo mojoWithSettings = (DeployMojo) getMojoFromPom("/pom-with-settings.xml", "deploy");
        final Map settings = mojoWithSettings.getAppSettingsWithDefaultValue();
        assertEquals("bar", settings.get("FOO"));
        assertEquals("java", settings.get("FUNCTIONS_WORKER_RUNTIME"));
        assertEquals("beta", settings.get("FUNCTIONS_EXTENSION_VERSION"));
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

    private DeployMojo getMojoFromPom() throws Exception {
        final DeployMojo mojoFromPom = (DeployMojo) getMojoFromPom("/pom.xml", "deploy");
        assertNotNull(mojoFromPom);
        return mojoFromPom;
    }
}
