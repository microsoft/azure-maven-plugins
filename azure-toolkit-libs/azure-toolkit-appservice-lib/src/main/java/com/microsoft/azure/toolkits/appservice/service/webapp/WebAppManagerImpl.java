/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.webapp;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.JavaVersion;
import com.azure.resourcemanager.appservice.models.RuntimeStack;
import com.azure.resourcemanager.appservice.models.WebApp.Update;
import com.azure.resourcemanager.appservice.models.WebContainer;
import com.microsoft.azure.toolkits.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkits.appservice.service.WebAppDeploymentSlotsManager;
import com.microsoft.azure.toolkits.appservice.service.WebAppManager;
import com.microsoft.azure.toolkits.appservice.service.deploymentslot.WebAppDeploymentSlotsManagerImpl;
import com.microsoft.azure.toolkits.appservice.AppService;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.model.WebApp;
import com.microsoft.azure.toolkits.appservice.service.AbstractAppServiceUpdatable;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class WebAppManagerImpl implements WebAppManager {

    private WebApp webApp;
    private AppService appService;
    private AzureResourceManager azureResourceManager;
    private com.azure.resourcemanager.appservice.models.WebApp webAppService;

    public WebAppManagerImpl(WebApp webApp, AppService appService) {
        this.webApp = webApp;
        this.appService = appService;
        this.azureResourceManager = appService.getAzureResourceManager();
    }

    @Override
    public void start() {
        getWebAppService().start();
    }

    @Override
    public void stop() {
        getWebAppService().stop();
    }

    @Override
    public void restart() {
        getWebAppService().restart();
    }

    @Override
    public void delete() {
        azureResourceManager.webApps().deleteById(getWebAppService().id());
    }

    @Override
    public WebApp get() {
        this.webApp = WebApp.createFromWebAppBase(getWebAppService());
        return webApp;
    }

    public void deploy(DeployType deployType, File target) {
        getWebAppService().deploy(com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue()), target);
    }

    @Override
    public boolean exists() {
        return getWebAppService(true) != null;
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        final com.azure.resourcemanager.appservice.models.PublishingProfile publishingProfile = getWebAppService().getPublishingProfile();
        return PublishingProfile.createFromServiceModel(publishingProfile);
    }

    @Override
    public Runtime getRuntime() {
        return Runtime.createFromServiceInstance(getWebAppService());
    }

    @Override
    public WebAppUpdatable update() {
        return new WebAppUpdatable();
    }

    @Override
    public WebAppDeploymentSlotsManager deploymentSlots() {
        return new WebAppDeploymentSlotsManagerImpl(getWebAppService());
    }

    private com.azure.resourcemanager.appservice.models.WebApp getWebAppService() {
        return getWebAppService(false);
    }

    private synchronized com.azure.resourcemanager.appservice.models.WebApp getWebAppService(boolean force) {
        if (webAppService == null || force) {
            this.webAppService = StringUtils.isEmpty(webApp.getId()) ?
                    azureResourceManager.webApps().getById(webApp.getId()) :
                    azureResourceManager.webApps().getByResourceGroup(webApp.getResourceGroup().getName(), webApp.getName());
            this.webApp = WebApp.createFromWebAppBase(webAppService);
        }
        return webAppService;
    }

    public class WebAppUpdatable extends AbstractAppServiceUpdatable<com.azure.resourcemanager.appservice.models.WebApp> {

        public static final String CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS = "Can not update the operation system for existing app service";

        @Override
        public com.azure.resourcemanager.appservice.models.WebApp update() {
            Update update = getWebAppService().update();
            if (getAppServicePlan() != null && getAppServicePlan().isPresent()) {
                update = updateAppServicePlan(update, getAppServicePlan().get());
            }
            if (getRuntime() != null && getRuntime().isPresent()) {
                update = updateRuntime(update, getRuntime().get());
            }
            if (getAppSettings() != null) {
                // todo: enhance app settings update, as now we could only add new app settings but can not remove existing values
                update.withAppSettings(getAppSettings().get());
            }
            // todo: handling pricing tier updates, need to discuss whether update pricing in WebApp or ServicePlan
            WebAppManagerImpl.this.webAppService = update.apply();
            WebAppManagerImpl.this.webApp = WebApp.createFromWebAppBase(WebAppManagerImpl.this.webAppService);
            return WebAppManagerImpl.this.webAppService;
        }

        private Update updateAppServicePlan(Update update, com.microsoft.azure.toolkits.appservice.model.AppServicePlan newServicePlan) {
            final com.microsoft.azure.toolkits.appservice.model.AppServicePlan currentServicePlan = WebAppManagerImpl.this.webApp.getAppServicePlan();
            if (com.microsoft.azure.toolkits.appservice.model.AppServicePlan.equals(currentServicePlan, newServicePlan)) {
                return update;
            }
            final AppServicePlan newPlanServiceModel = appService.appServicePlan(newServicePlan).getPlanService();
            if (newPlanServiceModel == null) {
                throw new RuntimeException("Target app service plan not exists");
            }
            return update.withExistingAppServicePlan(newPlanServiceModel);
        }

        private Update updateRuntime(Update update, Runtime newRuntime) {
            final Runtime current = WebAppManagerImpl.this.webApp.getRuntime();
            if (current.getOperatingSystem() != newRuntime.getOperatingSystem()) {
                throw new RuntimeException(CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS);
            }
            switch (newRuntime.getOperatingSystem()) {
                case LINUX:
                    final RuntimeStack runtimeStack = com.microsoft.azure.toolkits.appservice.model.RuntimeStack.
                            convertToServiceModel(((Runtime.Linux) newRuntime).getRuntimeStack());
                    return update.withBuiltInImage(runtimeStack);
                case WINDOWS:
                    final Runtime.Windows windows = (Runtime.Windows) newRuntime;
                    final JavaVersion javaVersion = com.microsoft.azure.toolkits.appservice.model.JavaVersion.convertToServiceModel(windows.getJavaVersion());
                    final WebContainer webContainer = com.microsoft.azure.toolkits.appservice.model.WebContainer.convertToServiceModel(windows.getWebContainer());
                    return (Update) update.withJavaVersion(javaVersion).withWebContainer(webContainer);
                case DOCKER:
                    final Runtime.Docker docker = (Runtime.Docker) newRuntime;
                    if (StringUtils.isNotEmpty(docker.getRegistryUrl())) {
                        return update.withPrivateRegistryImage(docker.getImage(), docker.getRegistryUrl())
                                .withCredentials(docker.getUserName(), docker.getPassword());
                    }
                    return docker.isPublic() ? update.withPublicDockerHubImage(docker.getImage()) :
                            update.withPrivateDockerHubImage(docker.getImage()).withCredentials(docker.getUserName(), docker.getPassword());
                default:
                    throw new RuntimeException();
            }
        }
    }
}
