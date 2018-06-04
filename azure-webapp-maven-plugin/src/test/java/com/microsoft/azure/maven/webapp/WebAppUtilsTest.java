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
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    public void getDockerImageType() throws Exception {
        final ContainerSetting containerSetting = new ContainerSetting();
        assertEquals(DockerImageType.NONE, WebAppUtils.getDockerImageType(containerSetting));

        containerSetting.setImageName("imageName");
        assertEquals(DockerImageType.PUBLIC_DOCKER_HUB, WebAppUtils.getDockerImageType(containerSetting));

        containerSetting.setServerId("serverId");
        assertEquals(DockerImageType.PRIVATE_DOCKER_HUB, WebAppUtils.getDockerImageType(containerSetting));

        containerSetting.setRegistryUrl(new URL("https://microsoft.azurecr.io"));
        assertEquals(DockerImageType.PRIVATE_REGISTRY, WebAppUtils.getDockerImageType(containerSetting));

        containerSetting.setServerId("");
        assertEquals(DockerImageType.UNKNOWN, WebAppUtils.getDockerImageType(containerSetting));
    }

    @Test
    public void getLinuxRunTimeStack() throws MojoExecutionException {
        assertEquals(RuntimeStack.TOMCAT_8_5_JRE8, WebAppUtils.getLinuxRunTimeStack("tomcat 8.5-jre8"));
        assertEquals(RuntimeStack.TOMCAT_9_0_JRE8, WebAppUtils.getLinuxRunTimeStack("tomcat 9.0-jre8"));
        assertEquals(RuntimeStack.JAVA_8_JRE8, WebAppUtils.getLinuxRunTimeStack("java 8-jre8"));
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
        WebAppUtils.defineLinuxApp(mojo, plan);
    }

    @Test
    public void defineLinuxApp() throws Exception {
        final AbstractWebAppMojo mojoMock = mock(AbstractWebAppMojo.class);
        final String resourceGroup = "resource-group";
        final String appName = "app-name";
        doReturn(resourceGroup).when(mojoMock).getResourceGroup();
        doReturn(appName).when(mojoMock).getAppName();

        final AppServicePlan planMock = mock(AppServicePlan.class);
        doReturn(OperatingSystem.LINUX).when(planMock).operatingSystem();

        final Azure azureMock = mock(Azure.class);
        doReturn(azureMock).when(mojoMock).getAzureClient();

        final WebApps webAppsMock = mock(WebApps.class);
        doReturn(webAppsMock).when(azureMock).webApps();

        final Blank blankMock = mock(Blank.class);
        doReturn(blankMock).when(webAppsMock).define(anyString());

        final ExistingLinuxPlanWithGroup groupMock = mock(ExistingLinuxPlanWithGroup.class);
        doReturn(groupMock).when(blankMock).withExistingLinuxPlan(any(AppServicePlan.class));

        final ResourceGroups resourceGroupsMock = mock(ResourceGroups.class);
        doReturn(resourceGroupsMock).when(azureMock).resourceGroups();
        doReturn(true).when(resourceGroupsMock).contain(anyString());

        WebAppUtils.defineLinuxApp(mojoMock, planMock);
        verify(groupMock, times(1)).withExistingResourceGroup(resourceGroup);
        verify(groupMock, never()).withNewResourceGroup(mojoMock.resourceGroup);

        doReturn(false).when(resourceGroupsMock).contain(anyString());

        WebAppUtils.defineLinuxApp(mojoMock, planMock);
        verify(groupMock, never()).withExistingResourceGroup(mojoMock.resourceGroup);
        verify(groupMock, times(1)).withNewResourceGroup(resourceGroup);
    }

    @Test(expected = MojoExecutionException.class)
    public void defineWindowsAppWithLinuxPlan() throws Exception {
        final AbstractWebAppMojo mojo = mock(AbstractWebAppMojo.class);
        final AppServicePlan plan = mock(AppServicePlan.class);
        doReturn(OperatingSystem.LINUX).when(plan).operatingSystem();
        WebAppUtils.defineWindowsApp(mojo, plan);
    }

    @Test
    public void defineWindowsApp() throws Exception {
        final AbstractWebAppMojo mojoMock = mock(AbstractWebAppMojo.class);
        final String resourceGroup = "resource-group";
        final String appName = "app-name";
        doReturn(resourceGroup).when(mojoMock).getResourceGroup();
        doReturn(appName).when(mojoMock).getAppName();

        final AppServicePlan planMock = mock(AppServicePlan.class);
        doReturn(OperatingSystem.WINDOWS).when(planMock).operatingSystem();

        final Azure azureMock = mock(Azure.class);
        doReturn(azureMock).when(mojoMock).getAzureClient();

        final WebApps webAppsMock = mock(WebApps.class);
        doReturn(webAppsMock).when(azureMock).webApps();

        final Blank blankMock = mock(Blank.class);
        doReturn(blankMock).when(webAppsMock).define(anyString());

        final ExistingWindowsPlanWithGroup groupMock = mock(ExistingWindowsPlanWithGroup.class);
        doReturn(groupMock).when(blankMock).withExistingWindowsPlan(any(AppServicePlan.class));

        final ResourceGroups resourceGroupsMock = mock(ResourceGroups.class);
        doReturn(resourceGroupsMock).when(azureMock).resourceGroups();
        doReturn(true).when(resourceGroupsMock).contain(anyString());

        WebAppUtils.defineWindowsApp(mojoMock, planMock);
        verify(groupMock, times(1)).withExistingResourceGroup(resourceGroup);
        verify(groupMock, never()).withNewResourceGroup(mojoMock.resourceGroup);

        doReturn(false).when(resourceGroupsMock).contain(anyString());

        WebAppUtils.defineWindowsApp(mojoMock, planMock);
        verify(groupMock, never()).withExistingResourceGroup(mojoMock.resourceGroup);
        verify(groupMock, times(1)).withNewResourceGroup(resourceGroup);
    }

    @Test
    public void createOrGetAppServicePlan() throws Exception {
        final String resourceGroup = "resource-group";
        final String servicePlanResourceGroup = "service-plan-resource-name";
        final String servicePlanName = "service-plan-name";
        final String region = "region";
        final String empty = "";

        final AbstractWebAppMojo mojoMock = mock(AbstractWebAppMojo.class);
        doReturn(resourceGroup).when(mojoMock).getResourceGroup();
        doReturn(servicePlanResourceGroup).when(mojoMock).getAppServicePlanResourceGroup();
        doReturn(PricingTier.BASIC_B1).when(mojoMock).getPricingTier();
        doReturn(region).when(mojoMock).getRegion();

        final Log logMock = mock(Log.class);
        doReturn(logMock).when(mojoMock).getLog();
        doNothing().when(logMock).info(anyString());

        final Azure azureMock = mock(Azure.class);
        doReturn(azureMock).when(mojoMock).getAzureClient();

        final AppServiceManager appServiceManagerMock = mock(AppServiceManager.class);
        doReturn(appServiceManagerMock).when(azureMock).appServices();

        final AppServicePlans plansMock = mock(AppServicePlans.class);
        doReturn(plansMock).when(appServiceManagerMock).appServicePlans();

        final AppServicePlan planMock = mock(AppServicePlan.class);
        doReturn(null).when(plansMock).getByResourceGroup(anyString(), anyString());

        final AppServicePlan.DefinitionStages.Blank blankMock = mock(AppServicePlan.DefinitionStages.Blank.class);
        doReturn(blankMock).when(plansMock).define(anyString());

        final AppServicePlan.DefinitionStages.WithGroup withGroupMock =
                mock(AppServicePlan.DefinitionStages.WithGroup.class);
        doReturn(withGroupMock).when(blankMock).withRegion(anyString());

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
        doReturn(servicePlanName).when(mojoMock).getAppServicePlanName();
        doReturn(true).when(resourceGroupsMock).contain(anyString());
        WebAppUtils.createOrGetAppServicePlan(mojoMock, OperatingSystem.LINUX);
        verify(withGroupMock, times(1)).withExistingResourceGroup(anyString());
        verify(withGroupMock, never()).withNewResourceGroup(anyString());
        verify(createMock, times(1)).create();

        // create App Service Plan in new resource group with user defined plan name
        reset(withGroupMock);
        doReturn(false).when(resourceGroupsMock).contain(anyString());
        doReturn(priceMock).when(withGroupMock).withNewResourceGroup(anyString());
        WebAppUtils.createOrGetAppServicePlan(mojoMock, OperatingSystem.LINUX);
        verify(withGroupMock, never()).withExistingResourceGroup(anyString());
        verify(withGroupMock, times(1)).withNewResourceGroup(anyString());

        // found existing App Service Plan with user defined plan name
        reset(createMock);
        doReturn(planMock).when(plansMock).getByResourceGroup(anyString(), anyString());
        WebAppUtils.createOrGetAppServicePlan(mojoMock, OperatingSystem.LINUX);
        verify(createMock, times(0)).create();

        // create App Service Plan due to no plan name is given
        reset(createMock);
        doReturn(empty).when(mojoMock).getAppServicePlanName();
        WebAppUtils.createOrGetAppServicePlan(mojoMock, OperatingSystem.LINUX);
        verify(createMock, times(1)).create();
    }
}
