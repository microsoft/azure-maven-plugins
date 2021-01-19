/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.deploymentslot;

import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.microsoft.azure.toolkits.appservice.model.WebAppDeploymentSlot;
import com.microsoft.azure.toolkits.appservice.service.WebAppDeploymentSlotCreatable;
import com.microsoft.azure.toolkits.appservice.service.WebAppDeploymentSlotsManager;

import java.util.List;
import java.util.stream.Collectors;

public class WebAppDeploymentSlotsManagerImpl implements WebAppDeploymentSlotsManager {
    private WebApp webappService;

    public WebAppDeploymentSlotsManagerImpl(WebApp webappService) {
        this.webappService = webappService;
    }

    @Override
    public WebAppDeploymentSlotCreatable.WithName create() {
        return new WebAppDeploymentSlotCreatableImpl(webappService);
    }

    @Override
    public WebAppDeploymentSlot getById(String id) {
        final com.azure.resourcemanager.appservice.models.DeploymentSlot deploymentSlot = webappService.deploymentSlots().getById(id);
        return WebAppDeploymentSlot.createFromServiceModel(deploymentSlot);
    }

    @Override
    public WebAppDeploymentSlot getByName(String slotName) {
        final com.azure.resourcemanager.appservice.models.DeploymentSlot deploymentSlot = webappService.deploymentSlots().getByName(slotName);
        return WebAppDeploymentSlot.createFromServiceModel(deploymentSlot);
    }

    @Override
    public List<WebAppDeploymentSlot> list() {
        return webappService.deploymentSlots().list().stream()
                .map(slot -> WebAppDeploymentSlot.createFromServiceModel((DeploymentSlot) slot))
                .collect(Collectors.toList());
    }
}
