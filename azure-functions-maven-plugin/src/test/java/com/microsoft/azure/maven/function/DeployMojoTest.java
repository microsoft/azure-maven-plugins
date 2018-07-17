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
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.management.appservice.FunctionApps;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.function.handlers.ArtifactHandler;
import com.microsoft.azure.maven.function.handlers.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.function.handlers.MSDeployArtifactHandlerImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Paths;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.isNull;

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
        doCallRealMethod().when(mojoSpy).createOrUpdateFunctionApp();
        doCallRealMethod().when(mojoSpy).getAppName();
        final FunctionApp app = mock(FunctionApp.class);
        doReturn(app).when(mojoSpy).getFunctionApp();
        doNothing().when(mojoSpy).updateFunctionApp(app);

        mojoSpy.doExecute();
        verify(mojoSpy, times(1)).createOrUpdateFunctionApp();
        verify(mojoSpy, times(1)).doExecute();
        verify(mojoSpy, times(1)).updateFunctionApp(any(FunctionApp.class));
        verify(handler, times(1)).publish();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void createFunctionAppIfNotExist() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        doReturn(null).when(mojoSpy).getFunctionApp();
        final NewAppServicePlanWithGroup withGroup = mock(NewAppServicePlanWithGroup.class);
        doReturn(withGroup).when(mojoSpy).defineApp(anyString(), anyString());
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(mojoSpy).configureResourceGroup(any(NewAppServicePlanWithGroup.class), anyString());
        doNothing().when(mojoSpy).configurePricingTier(any(WithCreate.class), isNull());
        doNothing().when(mojoSpy).configureAppSettings(any(Consumer.class), anyMap());

        mojoSpy.createOrUpdateFunctionApp();

        verify(mojoSpy, times(2)).getAppName();
        verify(mojoSpy, times(1)).defineApp(anyString(), anyString());
        verify(mojoSpy, times(1))
                .configureResourceGroup(any(NewAppServicePlanWithGroup.class), anyString());
        verify(mojoSpy, times(1)).configurePricingTier(any(WithCreate.class), isNull());
        verify(mojoSpy, times(1)).configureAppSettings(any(Consumer.class), anyMap());
        verify(withCreate, times(1)).create();
    }

    @Test
    public void updateFunctionApp() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        final FunctionApp app = mock(FunctionApp.class);
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn(siteInner).when(app).inner();
        final Update update = mock(Update.class);
        doReturn(update).when(app).update();
        doNothing().when(mojoSpy).configureAppSettings(any(Consumer.class), anyMap());

        mojoSpy.updateFunctionApp(app);

        verify(update, times(1)).apply();
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
        final NewAppServicePlanWithGroup newAppServicePlanWithGroup = mock(NewAppServicePlanWithGroup.class);
        doReturn(newAppServicePlanWithGroup).when(blank).withRegion(anyString());

        final NewAppServicePlanWithGroup ret = mojoSpy.defineApp(anyString(), anyString());

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

        final WithCreate ret = mojoSpy.configureResourceGroup(newAppServicePlanWithGroup, "resourceGroup");

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

        final WithCreate ret = mojoSpy.configureResourceGroup(newAppServicePlanWithGroup, "resourceGroup");

        assertSame(withCreate, ret);
    }

    @Test
    public void configurePricingTier() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(withCreate).withNewAppServicePlan(any(PricingTier.class));

        mojoSpy.configurePricingTier(withCreate, PricingTier.STANDARD_S1);

        verify(withCreate, times(1)).withNewAppServicePlan(PricingTier.STANDARD_S1);
        verify(withCreate, times(1)).withWebAppAlwaysOn(true);
    }

    @Test
    public void configureAppSettings() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final WithCreate withCreate = mock(WithCreate.class);

        mojo.configureAppSettings(withCreate::withAppSettings, mojo.getAppSettings());

        verify(withCreate, times(1)).withAppSettings(anyMap());
    }

    @Test
    public void getMSDeployArtifactHandler() throws Exception {
        final DeployMojo mojo = getMojoFromPom();

        final ArtifactHandler handler = mojo.getArtifactHandler();

        assertNotNull(handler);
        assertTrue(handler instanceof MSDeployArtifactHandlerImpl);
    }

    @Test
    public void getFTPArtifactHandler() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        final DeployMojo mojoSpy = spy(mojo);
        doReturn("ftp").when(mojoSpy).getDeploymentType();

        final ArtifactHandler handler = mojoSpy.getArtifactHandler();

        assertNotNull(handler);
        assertTrue(handler instanceof FTPArtifactHandlerImpl);
    }

    private DeployMojo getMojoFromPom() throws Exception {
        final DeployMojo mojo = (DeployMojo) getMojoFromPom("/pom.xml", "deploy");
        assertNotNull(mojo);
        return mojo;
    }
}
