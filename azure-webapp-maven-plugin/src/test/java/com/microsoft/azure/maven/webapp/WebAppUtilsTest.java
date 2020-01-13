/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.AppServicePlans;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.Blank;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingLinuxPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingWindowsPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApps;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroups;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.appservice.DockerImageType;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        AzureExecutionException exception = null;
        try {
            WebAppUtils.assureLinuxWebApp(app);
        } catch (AzureExecutionException e) {
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
        AzureExecutionException exception = null;
        try {
            WebAppUtils.assureWindowsWebApp(app);
        } catch (AzureExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }
    }

    @Test
    public void getDockerImageType() {
        assertEquals(DockerImageType.NONE, AppServiceUtils.getDockerImageType("", false, ""));

        assertEquals(DockerImageType.PUBLIC_DOCKER_HUB, AppServiceUtils.getDockerImageType("imageName",
            false, ""));

        assertEquals(DockerImageType.PRIVATE_DOCKER_HUB, AppServiceUtils.getDockerImageType("imageName",
            true, ""));

        assertEquals(DockerImageType.PRIVATE_REGISTRY, AppServiceUtils.getDockerImageType("imageName", true, "https://microsoft.azurecr.io"));

        assertEquals(DockerImageType.UNKNOWN, AppServiceUtils.getDockerImageType("imageName", false,
            "https://microsoft.azurecr.io"));
    }

    @Test(expected = AzureExecutionException.class)
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

    @Test(expected = AzureExecutionException.class)
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
    public void createOrGetAppServicePlan() throws AzureExecutionException {
        final String resourceGroup = "resource-group";
        final String servicePlanResourceGroup = "service-plan-resource-name";
        final String servicePlanName = "service-plan-name";
        final Region region = Region.EUROPE_WEST;
        final String empty = "";

        final Azure azureMock = mock(Azure.class);
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
        WebAppUtils.createOrGetAppServicePlan(servicePlanName, resourceGroup, azureMock,
            servicePlanResourceGroup, region, PricingTier.BASIC_B1, OperatingSystem.LINUX);
        verify(withGroupMock, times(1)).withExistingResourceGroup(anyString());
        verify(withGroupMock, never()).withNewResourceGroup(anyString());
        verify(createMock, times(1)).create();

        // create App Service Plan in new resource group with user defined plan name
        reset(withGroupMock);
        doReturn(false).when(resourceGroupsMock).contain(anyString());
        doReturn(priceMock).when(withGroupMock).withNewResourceGroup(anyString());
        WebAppUtils.createOrGetAppServicePlan(servicePlanName, resourceGroup, azureMock,
            servicePlanResourceGroup, region, PricingTier.BASIC_B1, OperatingSystem.LINUX);
        verify(withGroupMock, never()).withExistingResourceGroup(anyString());
        verify(withGroupMock, times(1)).withNewResourceGroup(anyString());

        // found existing App Service Plan with user defined plan name
        reset(createMock);
        doReturn(planMock).when(plansMock).getByResourceGroup(anyString(), anyString());
        WebAppUtils.createOrGetAppServicePlan(servicePlanName, resourceGroup, azureMock,
            servicePlanResourceGroup, region, PricingTier.BASIC_B1, OperatingSystem.LINUX);
        verify(createMock, times(0)).create();

        // create App Service Plan due to no plan name is given
        reset(createMock);
        WebAppUtils.createOrGetAppServicePlan(empty, resourceGroup, azureMock,
            servicePlanResourceGroup, region, PricingTier.BASIC_B1, OperatingSystem.LINUX);
        verify(createMock, times(1)).create();
    }
}
