/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.service.webapp.WebApp;

import java.io.File;
import java.util.List;

public interface IWebApp {
    void start();

    void stop();

    void restart();

    void delete();

    void deploy(File file);

    void deploy(DeployType deployType, File file);

    boolean exists();

    WebAppEntity get();

    WebApp.WebAppCreator create();

    WebApp.WebAppUpdater update();

    Runtime getRuntime();

    PublishingProfile getPublishingProfile();

    IWebAppDeploymentSlot deploymentSlot(String slotName);

    List<IWebAppDeploymentSlot> deploymentSlots();
}
