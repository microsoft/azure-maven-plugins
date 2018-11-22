/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.AppServicePlans;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.Blank;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingLinuxPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingWindowsPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApps;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroups;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class WebAppUtilsTest {
    @Test
    public void assureLinuxWebApp() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app,linux").when(siteInner).kind();
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();

        // Linux Web App
        WebAppUtils.assureLinuxWebApp(app);

        // Non-Linux Web App
        doReturn("app").when(siteInner).kind();
        MojoExecutionException exception = null;
        try {
            WebAppUtils.assureLinuxWebApp(app);
        } catch (MojoExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }
    }

    @Test
    public void assureWindowsWebApp() throws Exception {
        final SiteInner siteInner = mock(SiteInner.class);
        doReturn("app").when(siteInner).kind();
        final WebApp app = mock(WebApp.class);
        doReturn(siteInner).when(app).inner();

        // Windows Web App
        WebAppUtils.assureWindowsWebApp(app);

        // Linux Web App
        doReturn("app,linux").when(siteInner).kind();
        MojoExecutionException exception = null;
        try {
            WebAppUtils.assureWindowsWebApp(app);
        } catch (MojoExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }
    }

    @Test
    public void getDockerImageType() {
        assertEquals(DockerImageType.NONE, WebAppUtils.getDockerImageType("", "", ""));

        assertEquals(DockerImageType.PUBLIC_DOCKER_HUB, WebAppUtils.getDockerImageType("imageName",
            "", ""));

        assertEquals(DockerImageType.PRIVATE_DOCKER_HUB, WebAppUtils.getDockerImageType("imageName",
            "serverId", ""));

        assertEquals(DockerImageType.PRIVATE_REGISTRY, WebAppUtils.getDockerImageType("imageName",
            "serverId", "https://microsoft.azurecr.io"));

        assertEquals(DockerImageType.UNKNOWN, WebAppUtils.getDockerImageType("imageName", "",
            "https://microsoft.azurecr.io"));
    }

    @Test
    public void getLinuxRunTimeStack() throws MojoExecutionException {
        assertEquals(RuntimeStack.TOMCAT_8_5_JRE8, WebAppUtils.getLinuxRunTimeStack("tomcat 8.5-jre8"));
        assertEquals(RuntimeStack.TOMCAT_9_0_JRE8, WebAppUtils.getLinuxRunTimeStack("tomcat 9.0-jre8"));
        assertEquals(RuntimeStack.JAVA_8_JRE8, WebAppUtils.getLinuxRunTimeStack("jre8"));
    }

    @Test(expected = MojoExecutionException.class)
    public void getLinuxRunTimeStackWithNonExistedInput() throws MojoExecutionException {
        WebAppUtils.getLinuxRunTimeStack("non-existed-input");
    }

    @Test(expected = MojoExecutionException.class)
    public void getLinuxRunTimeStackWithEmptyInput() throws MojoExecutionException {
        WebAppUtils.getLinuxRunTimeStack("");
    }

    @Test(expected = MojoExecutionException.class)
    public void defineLinuxAppWithWindowsPlan() throws Exception {
        final AbstractWebAppMojo mojo = mock(AbstractWebAppMojo.class);
        final AppServicePlan plan = mock(AppServicePlan.class);
        doReturn(OperatingSystem.WINDOWS).when(plan).operatingSystem();
        WebAppUtils.defineLinuxApp(mojo.getResourceGroup(), mojo.getAppName(), mojo.getAzureClient(), plan);
    }

    @Test
    public void defineLinuxApp() throws Exception {
        final String resourceGroup = "resource-group";
        final String appName = "app-name";
        final AppServicePlan planMock = mock(AppServicePlan.class);
        doReturn(OperatingSystem.LINUX).when(planMock).operatingSystem();

        final Azure azureMock = mock(Azure.class);
        final WebApps webAppsMock = mock(WebApps.class);
        doReturn(webAppsMock).when(azureMock).webApps();

        final Blank blankMock = mock(Blank.class);
        doReturn(blankMock).when(webAppsMock).define(anyString());

        final ExistingLinuxPlanWithGroup groupMock = mock(ExistingLinuxPlanWithGroup.class);
        doReturn(groupMock).when(blankMock).withExistingLinuxPlan(any(AppServicePlan.class));

        final ResourceGroups resourceGroupsMock = mock(ResourceGroups.class);
        doReturn(resourceGroupsMock).when(azureMock).resourceGroups();
        doReturn(true).when(resourceGroupsMock).contain(anyString());

        WebAppUtils.defineLinuxApp(resourceGroup, appName, azureMock, planMock);

        verify(groupMock, times(1)).withExistingResourceGroup(resourceGroup);
        verify(groupMock, never()).withNewResourceGroup(resourceGroup);

        reset(groupMock);
        doReturn(false).when(resourceGroupsMock).contain(anyString());

        WebAppUtils.defineLinuxApp(resourceGroup, appName, azureMock, planMock);

        verify(groupMock, never()).withExistingResourceGroup(resourceGroup);
        verify(groupMock, times(1)).withNewResourceGroup(resourceGroup);
    }

    @Test(expected = MojoExecutionException.class)
    public void defineWindowsAppWithLinuxPlan() throws Exception {
        final AbstractWebAppMojo mojo = mock(AbstractWebAppMojo.class);
        final AppServicePlan plan = mock(AppServicePlan.class);
        doReturn(OperatingSystem.LINUX).when(plan).operatingSystem();
        WebAppUtils.defineWindowsApp(mojo.getResourceGroup(), mojo.getAppName(), mojo.getAzureClient(), plan);
    }

    @Test
    public void defineWindowsApp() throws Exception {
        final String resourceGroup = "resource-group";
        final String appName = "app-name";

        final AppServicePlan planMock = mock(AppServicePlan.class);
        doReturn(OperatingSystem.WINDOWS).when(planMock).operatingSystem();

        final Azure azureMock = mock(Azure.class);
        final WebApps webAppsMock = mock(WebApps.class);
        doReturn(webAppsMock).when(azureMock).webApps();

        final Blank blankMock = mock(Blank.class);
        doReturn(blankMock).when(webAppsMock).define(anyString());

        final ExistingWindowsPlanWithGroup groupMock = mock(ExistingWindowsPlanWithGroup.class);
        doReturn(groupMock).when(blankMock).withExistingWindowsPlan(any(AppServicePlan.class));

        final ResourceGroups resourceGroupsMock = mock(ResourceGroups.class);
        doReturn(resourceGroupsMock).when(azureMock).resourceGroups();
        doReturn(true).when(resourceGroupsMock).contain(anyString());

        WebAppUtils.defineWindowsApp(resourceGroup, appName, azureMock, planMock);

        verify(groupMock, times(1)).withExistingResourceGroup(resourceGroup);
        verify(groupMock, never()).withNewResourceGroup(resourceGroup);

        reset(groupMock);
        doReturn(false).when(resourceGroupsMock).contain(anyString());

        WebAppUtils.defineWindowsApp(resourceGroup, appName, azureMock, planMock);

        verify(groupMock, never()).withExistingResourceGroup(resourceGroup);
        verify(groupMock, times(1)).withNewResourceGroup(resourceGroup);
    }

    @Test
    public void createAppServicePlan() throws MojoExecutionException {
        final String resourceGroup = "resource-group";
        final String servicePlanResourceGroup = "service-plan-resource-name";
        final String servicePlanName = "service-plan-name";
        final Region region = Region.EUROPE_WEST;

        final Log logMock = mock(Log.class);
        doNothing().when(logMock).info(anyString());

        final Azure azureMock = mock(Azure.class);
        final AppServiceManager appServiceManagerMock = mock(AppServiceManager.class);
        doReturn(appServiceManagerMock).when(azureMock).appServices();

        final AppServicePlans plansMock = mock(AppServicePlans.class);
        doReturn(plansMock).when(appServiceManagerMock).appServicePlans();

        final AppServicePlan.DefinitionStages.Blank blankMock = mock(AppServicePlan.DefinitionStages.Blank.class);
        doReturn(blankMock).when(plansMock).define(anyString());

        final AppServicePlan.DefinitionStages.WithGroup withGroupMock =
                mock(AppServicePlan.DefinitionStages.WithGroup.class);
        doReturn(withGroupMock).when(blankMock).withRegion(region);

        final ResourceGroups resourceGroupsMock = mock(ResourceGroups.class);
        doReturn(resourceGroupsMock).when(azureMock).resourceGroups();

        final AppServicePlan.DefinitionStages.WithPricingTier priceMock =
                mock(AppServicePlan.DefinitionStages.WithPricingTier.class);
        doReturn(priceMock).when(withGroupMock).withExistingResourceGroup(anyString());

        final AppServicePlan.DefinitionStages.WithOperatingSystem osMock =
                mock(AppServicePlan.DefinitionStages.WithOperatingSystem.class);
        doReturn(osMock).when(priceMock).withPricingTier(any(PricingTier.class));

        final AppServicePlan.DefinitionStages.WithCreate createMock =
                mock(AppServicePlan.DefinitionStages.WithCreate.class);
        doReturn(createMock).when(osMock).withOperatingSystem(any(OperatingSystem.class));

        // create App Service Plan in existing resource group with user defined plan name
        doReturn(true).when(resourceGroupsMock).contain(anyString());
        WebAppUtils.createAppServicePlan(servicePlanName, resourceGroup, azureMock,
            servicePlanResourceGroup, region, PricingTier.BASIC_B1, logMock, OperatingSystem.LINUX);
        verify(withGroupMock, times(1)).withExistingResourceGroup(anyString());
        verify(withGroupMock, never()).withNewResourceGroup(anyString());
        verify(createMock, times(1)).create();

        // create App Service Plan in new resource group with user defined plan name
        reset(withGroupMock);
        doReturn(false).when(resourceGroupsMock).contain(anyString());
        doReturn(priceMock).when(withGroupMock).withNewResourceGroup(anyString());
        WebAppUtils.createAppServicePlan(servicePlanName, resourceGroup, azureMock,
            servicePlanResourceGroup, region, PricingTier.BASIC_B1, logMock, OperatingSystem.LINUX);
        verify(withGroupMock, never()).withExistingResourceGroup(anyString());
        verify(withGroupMock, times(1)).withNewResourceGroup(anyString());
    }

    @Test
    public void updateAppServicePlan() {
        final AppServicePlan planMock = mock(AppServicePlan.class);
        final Log logMock = mock(Log.class);
        final PricingTier pricingTier = PricingTier.STANDARD_S1;
        final WebAppConfiguration configMock = mock(WebAppConfiguration.class);
        final AppServicePlan.Update updateMock = mock(AppServicePlan.Update.class);
        final AppServicePlan.Update updateMock2 = mock(AppServicePlan.Update.class);
        doReturn(pricingTier).when(configMock).getPricingTier();
        doReturn(PricingTier.BASIC_B1).when(planMock).pricingTier();
        doNothing().when(logMock).info(anyString());
        doReturn(updateMock).when(planMock).update();
        doReturn(updateMock2).when(updateMock).withPricingTier(pricingTier);

        WebAppUtils.updateAppServicePlan(planMock, configMock, logMock);
        verify(updateMock2, times(1)).apply();
        verifyNoMoreInteractions(updateMock2);
    }
}
