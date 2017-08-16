/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.Blank;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.NewAppServicePlanWithGroup;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.FunctionApps;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.maven.function.handlers.ArtifactHandler;
import com.microsoft.azure.maven.function.handlers.MSDeployArtifactHandlerImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Paths;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployMojoTest extends MojoTestBase {
    @Test
    public void getConfiguration() throws Exception {
        final DeployMojo mojo = getMojoFromPom();

        assertEquals("resourceGroupName", mojo.getResourceGroup());

        assertEquals("appName", mojo.getAppName());

        assertEquals("westeurope", mojo.getRegion());
    }

    @Test
    public void getDeploymentStageDirectory() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        doReturn("target").when(mojoSpy).getBuildDirectoryAbsolutePath();
        assertEquals(Paths.get("target", "azure-functions", "appName").toString(),
                mojoSpy.getDeploymentStageDirectory());
    }

    @Test
    public void getFunctionApp() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        doReturn(null).when(mojoSpy).getAzureClient();
        assertNull(mojoSpy.getFunctionApp());
    }

    @Test
    public void doExecute() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        doCallRealMethod().when(mojoSpy).getLog();
        final ArtifactHandler handler = mock(ArtifactHandler.class);
        doReturn(handler).when(mojoSpy).getArtifactHandler();
        doCallRealMethod().when(mojoSpy).createFunctionAppIfNotExist();
        doCallRealMethod().when(mojoSpy).getAppName();
        final FunctionApp app = mock(FunctionApp.class);
        doReturn(app).when(mojoSpy).getFunctionApp();

        mojoSpy.doExecute();
        verify(mojoSpy, times(1)).createFunctionAppIfNotExist();
        verify(mojoSpy, times(1)).doExecute();
        verify(handler, times(1)).publish();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void createFunctionAppIfNotExist() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        doReturn(null).when(mojoSpy).getFunctionApp();
        final Blank blank = mock(Blank.class);
        doReturn(blank).when(mojoSpy).defineApp(anyString());
        final NewAppServicePlanWithGroup withGroup = mock(NewAppServicePlanWithGroup.class);
        doReturn(withGroup).when(mojoSpy).configureRegion(any(Blank.class));
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(mojoSpy).configureResourceGroup(any(NewAppServicePlanWithGroup.class));
        doReturn(withCreate).when(mojoSpy).configurePricingTier(any(WithCreate.class));
        doReturn(withCreate).when(mojoSpy).configureAppSettings(any(WithCreate.class));

        mojoSpy.createFunctionAppIfNotExist();

        verify(mojoSpy, times(2)).getAppName();
        verify(mojoSpy, times(1)).defineApp(anyString());
        verify(mojoSpy, times(1)).configureRegion(any(Blank.class));
        verify(mojoSpy, times(1)).configureResourceGroup(any(NewAppServicePlanWithGroup.class));
        verify(mojoSpy, times(1)).configurePricingTier(any(WithCreate.class));
        verify(mojoSpy, times(1)).configureAppSettings(any(WithCreate.class));
        verify(withCreate, times(1)).create();
    }

    @Test
    public void defineApp() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        final Azure azure = mock(Azure.class);
        doReturn(azure).when(mojoSpy).getAzureClient();
        final AppServiceManager appServiceManager = mock(AppServiceManager.class);
        doReturn(appServiceManager).when(azure).appServices();
        final FunctionApps functionApps = mock(FunctionApps.class);
        doReturn(functionApps).when(appServiceManager).functionApps();
        final Blank blank = mock(Blank.class);
        doReturn(blank).when(functionApps).define(anyString());

        final Blank ret = mojoSpy.defineApp("appName");

        assertSame(blank, ret);
    }

    @Test
    public void configureRegion() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        final Blank blank = mock(Blank.class);
        final NewAppServicePlanWithGroup newAppServicePlanWithGroup = mock(NewAppServicePlanWithGroup.class);
        doReturn(newAppServicePlanWithGroup).when(blank).withRegion(anyString());

        final NewAppServicePlanWithGroup ret = mojoSpy.configureRegion(blank);

        assertSame(newAppServicePlanWithGroup, ret);
    }

    @Test
    public void configureExistingResourceGroup() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        doReturn(true).when(mojoSpy).isResourceGroupExist(anyString());
        final NewAppServicePlanWithGroup newAppServicePlanWithGroup = mock(NewAppServicePlanWithGroup.class);
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(newAppServicePlanWithGroup).withExistingResourceGroup(anyString());

        final WithCreate ret = mojoSpy.configureResourceGroup(newAppServicePlanWithGroup);

        assertSame(withCreate, ret);
    }

    @Test
    public void configureNewResourceGroup() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        doReturn(false).when(mojoSpy).isResourceGroupExist(anyString());
        final NewAppServicePlanWithGroup newAppServicePlanWithGroup = mock(NewAppServicePlanWithGroup.class);
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(newAppServicePlanWithGroup).withNewResourceGroup(anyString());

        final WithCreate ret = mojoSpy.configureResourceGroup(newAppServicePlanWithGroup);

        assertSame(withCreate, ret);
    }

    @Test
    public void configurePricingTier() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        final WithCreate withCreate = mock(WithCreate.class);

        final WithCreate ret = mojoSpy.configurePricingTier(withCreate);

        assertSame(withCreate, ret);
        verify(withCreate, times(1)).withNewAppServicePlan(PricingTier.STANDARD_S1);
    }

    @Test
    public void configureAppSettings() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final WithCreate withCreate = mock(WithCreate.class);

        final WithCreate ret = mojo.configureAppSettings(withCreate);

        assertSame(withCreate, ret);
        verify(withCreate, times(1)).withAppSettings(anyMap());
    }

    @Test
    public void getArtifactHandler() throws Exception {
        final DeployMojo mojo = getMojoFromPom();

        final ArtifactHandler handler = mojo.getArtifactHandler();

        assertNotNull(handler);
        assertTrue(handler instanceof MSDeployArtifactHandlerImpl);
    }

    private DeployMojo getMojoFromPom() throws Exception {
        final DeployMojo mojo = (DeployMojo) getMojoFromPom("/pom.xml", "deploy");
        assertNotNull(mojo);
        return mojo;
    }
}
