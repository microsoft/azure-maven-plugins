/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.AppServicePlans;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.Blank;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.ExistingAppServicePlanWithGroup;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.NewAppServicePlanWithGroup;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.management.appservice.FunctionApps;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.appservice.DeploymentType;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.artifacthandler.FTPArtifactHandlerImpl;
import com.microsoft.azure.maven.artifacthandler.ZIPArtifactHandlerImpl;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.function.handlers.MSDeployArtifactHandlerImpl;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    public void getFunctionApp() throws Exception {
        doReturn(null).when(mojoSpy).getAzureClient();
        assertNull(mojoSpy.getFunctionApp());
    }

    @Test
    public void doExecute() throws Exception {
        doCallRealMethod().when(mojoSpy).getLog();
        final ArtifactHandler handler = mock(ArtifactHandler.class);
        final FunctionApp app = mock(FunctionApp.class);
        doReturn(app).when(mojoSpy).getFunctionApp();
        doReturn(handler).when(mojoSpy).getArtifactHandler();
        doCallRealMethod().when(mojoSpy).createOrUpdateFunctionApp();
        doCallRealMethod().when(mojoSpy).getAppName();
        final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.FUNCTION);
        doNothing().when(mojoSpy).updateFunctionApp(app);

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
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn(siteInner).when(app).inner();
        final Update update = mock(Update.class);
        doReturn(update).when(app).update();
        doNothing().when(mojoSpy).configureAppSettings(any(Consumer.class), anyMap());

        mojoSpy.updateFunctionApp(app);

        verify(update, times(1)).apply();
    }

    @Test
    public void createFunctionAppWithExistingAppServicePlan() throws Exception {
        final Azure azure = mock(Azure.class);
        final AppServiceManager appServiceManager = mock(AppServiceManager.class);
        final AppServicePlans appServicePlans = mock(AppServicePlans.class);
        final FunctionApps functionApps = mock(FunctionApps.class);
        final Blank blank = mock(Blank.class);
        prepareFunctionAppCreation(azure, appServiceManager, appServicePlans, functionApps, blank);

        final AppServicePlan appServicePlan = mock(AppServicePlan.class);
        doReturn(appServicePlan).when(appServicePlans).getByResourceGroup(anyString(), anyString());
        final ExistingAppServicePlanWithGroup existingAppServicePlanWithGroup =
                mock(ExistingAppServicePlanWithGroup.class);
        doReturn(existingAppServicePlanWithGroup).when(blank).withExistingAppServicePlan(appServicePlan);
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(existingAppServicePlanWithGroup).withExistingResourceGroup(anyString());
        doReturn(true).when(mojoSpy).isResourceGroupExist(anyString());
        doNothing().when(mojoSpy).configureAppSettings(any(), anyMap());
        doReturn(null).when(withCreate).create();

        mojoSpy.createFunctionApp();

        verify(existingAppServicePlanWithGroup).withExistingResourceGroup(anyString());
        verify(existingAppServicePlanWithGroup, never()).withNewResourceGroup(anyString());
        verify(mojoSpy).configureAppSettings(any(), anyMap());
        verify(withCreate).create();
    }

    @Test
    public void createFunctionAppWithNewAppServicePlan() throws Exception {
        final Azure azure = mock(Azure.class);
        final AppServiceManager appServiceManager = mock(AppServiceManager.class);
        final AppServicePlans appServicePlans = mock(AppServicePlans.class);
        final FunctionApps functionApps = mock(FunctionApps.class);
        final Blank blank = mock(Blank.class);
        prepareFunctionAppCreation(azure, appServiceManager, appServicePlans, functionApps, blank);


        final NewAppServicePlanWithGroup newAppServicePlanWithGroup = mock(NewAppServicePlanWithGroup.class);
        doReturn(newAppServicePlanWithGroup).when(blank).withRegion(anyString());
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(mojoSpy).configureResourceGroup(any(), anyString());
        doReturn(PricingTier.STANDARD_S1).when(mojoSpy).getPricingTier();
        doNothing().when(mojoSpy).configurePricingTier(any(), any());

        mojoSpy.createFunctionApp();

        verify(mojoSpy).configurePricingTier(any(), any());
        verify(mojoSpy).configureAppSettings(any(), anyMap());
        verify(withCreate).create();
    }

    @Test
    public void configureExistingResourceGroup() throws Exception {
        doReturn(true).when(mojoSpy).isResourceGroupExist(anyString());
        final NewAppServicePlanWithGroup newAppServicePlanWithGroup = mock(NewAppServicePlanWithGroup.class);
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(newAppServicePlanWithGroup).withExistingResourceGroup(anyString());

        final WithCreate ret = mojoSpy.configureResourceGroup(newAppServicePlanWithGroup, "resourceGroup");

        assertSame(withCreate, ret);
    }

    @Test
    public void configureNewResourceGroup() throws Exception {
        doReturn(false).when(mojoSpy).isResourceGroupExist(anyString());
        final NewAppServicePlanWithGroup newAppServicePlanWithGroup = mock(NewAppServicePlanWithGroup.class);
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(newAppServicePlanWithGroup).withNewResourceGroup(anyString());

        final WithCreate ret = mojoSpy.configureResourceGroup(newAppServicePlanWithGroup, "resourceGroup");

        assertSame(withCreate, ret);
    }

    @Test
    public void configurePricingTier() throws Exception {
        final WithCreate withCreate = mock(WithCreate.class);
        doReturn(withCreate).when(withCreate).withNewAppServicePlan(any(PricingTier.class));

        mojoSpy.configurePricingTier(withCreate, PricingTier.STANDARD_S1);

        verify(withCreate, times(1)).withNewAppServicePlan(PricingTier.STANDARD_S1);
        verify(withCreate, times(1)).withWebAppAlwaysOn(true);
    }

    @Test
    public void configureAppSettings() throws Exception {
        final WithCreate withCreate = mock(WithCreate.class);

        mojo.configureAppSettings(withCreate::withAppSettings, mojo.getAppSettings());

        verify(withCreate, times(1)).withAppSettings(anyMap());
    }

    @Test
    public void getMSDeployArtifactHandler() throws MojoExecutionException {
        doReturn(DeploymentType.MSDEPLOY).when(mojoSpy).getDeploymentType();
        final ArtifactHandler handler = mojoSpy.getArtifactHandler();

        assertNotNull(handler);
        assertTrue(handler instanceof MSDeployArtifactHandlerImpl);
    }

    @Test
    public void getFTPArtifactHandler() throws MojoExecutionException {
        doReturn(DeploymentType.FTP).when(mojoSpy).getDeploymentType();
        final ArtifactHandler handler = mojoSpy.getArtifactHandler();

        assertNotNull(handler);
        assertTrue(handler instanceof FTPArtifactHandlerImpl);
    }

    @Test
    public void getZIPArtifactHandler() throws MojoExecutionException {
        doReturn(DeploymentType.ZIP).when(mojoSpy).getDeploymentType();
        final ArtifactHandler handler = mojoSpy.getArtifactHandler();

        assertNotNull(handler);
        assertTrue(handler instanceof ZIPArtifactHandlerImpl);
    }

    @Test(expected = MojoExecutionException.class)
    public void getArtifactHandlerThrowException() throws Exception {
        final DeployMojo mojo = getMojoFromPom();
        mojo.getArtifactHandler();
    }

    private DeployMojo getMojoFromPom() throws Exception {
        final DeployMojo mojo = (DeployMojo) getMojoFromPom("/pom.xml", "deploy");
        assertNotNull(mojo);
        return mojo;
    }

    private void prepareFunctionAppCreation(Azure azure, AppServiceManager appServiceManager,
                                            AppServicePlans appServicePlans, FunctionApps functionApps,
                                            Blank blank) throws AzureAuthFailureException {
        doNothing().when(mojoSpy).info(anyString());
        doReturn(azure).when(mojoSpy).getAzureClient();
        doReturn("resGrp").when(mojoSpy).getResourceGroup();
        doReturn("").when(mojoSpy).getAppServicePlanResourceGroup();
        doReturn("appServicePlanName").when(mojoSpy).getAppServicePlanName();
        doReturn(appServiceManager).when(azure).appServices();
        doReturn(appServicePlans).when(appServiceManager).appServicePlans();
        doReturn(functionApps).when(appServiceManager).functionApps();
        doReturn(blank).when(functionApps).define(anyString());
    }
}
