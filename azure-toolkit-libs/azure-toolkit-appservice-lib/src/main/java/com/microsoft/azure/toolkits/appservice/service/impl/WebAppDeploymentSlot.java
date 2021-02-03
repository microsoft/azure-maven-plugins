/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.impl;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.microsoft.azure.toolkits.appservice.AzureAppService;
import com.microsoft.azure.toolkits.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.service.IWebApp;
import com.microsoft.azure.toolkits.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkits.appservice.service.IWebAppDeploymentSlotCreator;

import java.io.File;

public class WebAppDeploymentSlot implements IWebAppDeploymentSlot {

    private AzureAppService azureAppService;
    private WebAppDeploymentSlotEntity slotEntity;

    private DeploymentSlot deploymentSlotClient;
    private AzureResourceManager azureClient;

    public WebAppDeploymentSlot(WebAppDeploymentSlotEntity deploymentSlot, AzureAppService azureAppService) {
        this.slotEntity = deploymentSlot;
        this.azureAppService = azureAppService;
        this.azureClient = azureAppService.getAzureResourceManager();
    }

    @Override
    public IWebApp webApp() {
        return null;
    }

    @Override
    public IWebAppDeploymentSlotCreator create() {
        return null;
    }

    @Override
    public WebAppDeploymentSlotEntity entity() {
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void restart() {

    }

    @Override
    public void delete() {

    }

    @Override
    public void deploy(File file) {

    }

    @Override
    public void deploy(DeployType deployType, File file) {

    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public String hostName() {
        return null;
    }

    @Override
    public String state() {
        return null;
    }

    @Override
    public Runtime getRuntime() {
        return null;
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return null;
    }

    @Override
    public String id() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }
}