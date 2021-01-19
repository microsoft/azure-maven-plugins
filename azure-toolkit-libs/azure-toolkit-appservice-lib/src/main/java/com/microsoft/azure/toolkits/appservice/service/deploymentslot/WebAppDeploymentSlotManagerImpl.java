/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.deploymentslot;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.microsoft.azure.toolkits.appservice.AppService;
import com.microsoft.azure.toolkits.appservice.model.WebAppDeploymentSlot;
import com.microsoft.azure.toolkits.appservice.service.WebAppDeploymentSlotManager;
import org.apache.commons.lang3.StringUtils;

public class WebAppDeploymentSlotManagerImpl implements WebAppDeploymentSlotManager {

    private WebAppDeploymentSlot slot;

    private AppService appService;
    private DeploymentSlot deploymentSlotService;
    private AzureResourceManager azureResourceManager;

    public WebAppDeploymentSlotManagerImpl(WebAppDeploymentSlot deploymentSlot, AppService appService) {
        this.slot = deploymentSlot;
        this.appService = appService;
        this.azureResourceManager = appService.getAzureResourceManager();
    }

    @Override
    public void start() {
        getDeploymentSlotService().start();
    }

    @Override
    public void stop() {
        getDeploymentSlotService().stop();
    }

    @Override
    public void restart() {
        getDeploymentSlotService().restart();
    }

    @Override
    public void delete() {
        getDeploymentSlotService().parent().deploymentSlots().deleteByName(slot.getName());
    }

    @Override
    public boolean exists() {
        return getDeploymentSlotService(true) != null;
    }

    @Override
    public WebAppDeploymentSlot get() {
        this.slot = WebAppDeploymentSlot.createFromServiceModel(getDeploymentSlotService());
        return slot;
    }

    private com.azure.resourcemanager.appservice.models.DeploymentSlot getDeploymentSlotService() {
        return getDeploymentSlotService(false);
    }

    private synchronized com.azure.resourcemanager.appservice.models.DeploymentSlot getDeploymentSlotService(boolean force) {
        if (deploymentSlotService == null || force) {
            final WebApp webAppService = StringUtils.isNotEmpty(slot.getId()) ?
                    azureResourceManager.webApps().getById(slot.getId().substring(0, slot.getId().indexOf("/slots"))) :
                    azureResourceManager.webApps().getByResourceGroup(slot.getResourceGroup(), slot.getWebappName());
            this.deploymentSlotService = StringUtils.isNotEmpty(slot.getId()) ? webAppService.deploymentSlots().getById(slot.getId()) :
                    webAppService.deploymentSlots().getByName(slot.getName());
            this.slot = WebAppDeploymentSlot.createFromServiceModel(this.deploymentSlotService);
        }
        return deploymentSlotService;
    }
}
