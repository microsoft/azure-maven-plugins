/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.webapp;

import com.azure.core.management.Region;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.JavaVersion;
import com.azure.resourcemanager.appservice.models.PricingTier;
import com.azure.resourcemanager.appservice.models.RuntimeStack;
import com.azure.resourcemanager.appservice.models.WebApp.DefinitionStages;
import com.azure.resourcemanager.appservice.models.WebContainer;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.azure.toolkits.appservice.service.WebAppsManager;
import com.microsoft.azure.toolkits.appservice.AppService;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.model.WebApp;
import com.microsoft.azure.toolkits.appservice.service.AbstractAppServiceCreatable;
import com.microsoft.azure.toolkits.appservice.service.AppServiceCreatable;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WebAppsManagerImpl implements WebAppsManager {

    private static final Map<AppService, WebAppsManagerImpl> map = new HashMap<>();

    private AppService appService;
    private AzureResourceManager azureResourceManager;

    private WebAppsManagerImpl(AppService appService) {
        this.appService = appService;
        this.azureResourceManager = appService.getAzureResourceManager();
    }

    public static WebAppsManagerImpl getInstance(AppService resourceManager) {
        return map.computeIfAbsent(resourceManager, key -> new WebAppsManagerImpl(key));
    }

    public AppServiceCreatable.WithName create() {
        return new WebAppCreatable();
    }

    public WebApp get(String id) {
        return WebApp.createFromWebAppBase(azureResourceManager.webApps().getById(id));
    }

    public WebApp get(String resourceGroup, String name) {
        return WebApp.createFromWebAppBase(azureResourceManager.webApps().getByResourceGroup(resourceGroup, name));
    }

    /**
     * List web apps in current subscription and return basic info of each web app
     * Some info like app settings and run time will not be include in result
     *
     * @return
     */
    public List<WebApp> list() {
        return azureResourceManager.webApps().list().stream()
                .map(resource -> WebApp.createFromWebAppBasic(resource))
                .collect(Collectors.toList());
    }

    public class WebAppCreatable extends AbstractAppServiceCreatable<WebApp> {
        @Override
        public WebApp create() {
            final DefinitionStages.Blank blank = azureResourceManager.webApps().define(getName());
            final Runtime runtime = getRuntime();
            final Region region = Region.fromName(getRegion().getName());
            final AppServicePlan appServicePlan = appService.appServicePlan(getAppServicePlan()).getPlanService();
            final ResourceGroup resourceGroup = azureResourceManager.resourceGroups().getByName(getResourceGroup().getName());
            final PricingTier pricing = com.microsoft.azure.toolkits.appservice.model.PricingTier.convertToServiceModel(getPricingTier());
            final DefinitionStages.WithCreate withCreate;
            switch (runtime.getOperatingSystem()) {
                case LINUX:
                    withCreate = createLinuxWebApp(blank, resourceGroup, region, appServicePlan, pricing, runtime);
                    break;
                case WINDOWS:
                    withCreate = createWindowsWebApp(blank, resourceGroup, region, appServicePlan, pricing, runtime);
                    break;
                case DOCKER:
                    withCreate = createDockerWebApp(blank, resourceGroup, region, appServicePlan, pricing, runtime);
                    break;
                default:
                    throw new RuntimeException();
            }
            if (getAppSettings() != null) {
                withCreate.withAppSettings(getAppSettings().get());
            }
            return WebApp.createFromWebAppBase(withCreate.create());
        }

        DefinitionStages.WithCreate createWindowsWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, Region region, AppServicePlan appServicePlan,
                                                        PricingTier pricing, Runtime runtime) {
            final DefinitionStages.WithWindowsAppFramework withFramework;
            if (appServicePlan == null) {
                final DefinitionStages.NewAppServicePlanWithGroup withGroup = blank.withRegion(region);
                final DefinitionStages.WithNewAppServicePlan withNewAppServicePlan =
                        resourceGroup == null ? withGroup.withNewResourceGroup(getResourceGroup().getName()) :
                                withGroup.withExistingResourceGroup(resourceGroup);
                withFramework = StringUtils.isEmpty(getAppServicePlan().getName()) ? withNewAppServicePlan.withNewWindowsPlan(pricing) :
                        withNewAppServicePlan.withNewWindowsPlan(getAppServicePlan().getName(), pricing);
            } else {
                DefinitionStages.ExistingWindowsPlanWithGroup withGroup = blank.withExistingWindowsPlan(appServicePlan);
                withFramework = resourceGroup == null ? withGroup.withNewResourceGroup(getResourceGroup().getName()) :
                        withGroup.withExistingResourceGroup(resourceGroup);
            }
            final Runtime.Windows windows = (Runtime.Windows) runtime;
            final JavaVersion javaVersion = com.microsoft.azure.toolkits.appservice.model.JavaVersion.convertToServiceModel(windows.getJavaVersion());
            final WebContainer webContainer = com.microsoft.azure.toolkits.appservice.model.WebContainer.convertToServiceModel(windows.getWebContainer());
            return (DefinitionStages.WithCreate) withFramework.withJavaVersion(javaVersion).withWebContainer(webContainer);
        }

        DefinitionStages.WithCreate createLinuxWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, Region region, AppServicePlan appServicePlan,
                                                      PricingTier pricing, Runtime runtime) {
            final DefinitionStages.WithLinuxAppFramework withFramework = createLinuxWebAppWithoutRuntime(blank, resourceGroup, region, appServicePlan, pricing);
            final RuntimeStack runtimeStack = com.microsoft.azure.toolkits.appservice.model.RuntimeStack.convertToServiceModel(
                    ((Runtime.Linux) runtime).getRuntimeStack());
            return withFramework.withBuiltInImage(runtimeStack);
        }

        DefinitionStages.WithCreate createDockerWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, Region region, AppServicePlan appServicePlan,
                                                       PricingTier pricing, Runtime runtime) {
            final DefinitionStages.WithLinuxAppFramework withFramework = createLinuxWebAppWithoutRuntime(blank, resourceGroup, region, appServicePlan, pricing);
            final Runtime.Docker docker = (Runtime.Docker) runtime;
            if (StringUtils.isNotEmpty(docker.getRegistryUrl())) {
                return withFramework.withPrivateRegistryImage(docker.getImage(), docker.getRegistryUrl())
                        .withCredentials(docker.getUserName(), docker.getPassword());
            }
            return docker.isPublic() ? withFramework.withPublicDockerHubImage(docker.getImage()) :
                    withFramework.withPrivateDockerHubImage(docker.getImage()).withCredentials(docker.getUserName(), docker.getPassword());
        }

        DefinitionStages.WithLinuxAppFramework createLinuxWebAppWithoutRuntime(DefinitionStages.Blank blank, ResourceGroup resourceGroup,
                                                                               Region region, AppServicePlan appServicePlan, PricingTier pricing) {
            if (appServicePlan == null) {
                final DefinitionStages.NewAppServicePlanWithGroup withGroup = blank.withRegion(region);
                final DefinitionStages.WithNewAppServicePlan withNewAppServicePlan =
                        resourceGroup == null ? withGroup.withNewResourceGroup(getResourceGroup().getName()) :
                                withGroup.withExistingResourceGroup(resourceGroup);
                return StringUtils.isEmpty(getAppServicePlan().getName()) ? withNewAppServicePlan.withNewLinuxPlan(pricing) :
                        withNewAppServicePlan.withNewLinuxPlan(getAppServicePlan().getName(), pricing);
            } else {
                com.azure.resourcemanager.appservice.models.WebApp.DefinitionStages.ExistingLinuxPlanWithGroup withGroup =
                        blank.withExistingLinuxPlan(appServicePlan);
                return resourceGroup == null ? withGroup.withNewResourceGroup(getResourceGroup().getName()) :
                        withGroup.withExistingResourceGroup(resourceGroup);
            }
        }
    }
}
