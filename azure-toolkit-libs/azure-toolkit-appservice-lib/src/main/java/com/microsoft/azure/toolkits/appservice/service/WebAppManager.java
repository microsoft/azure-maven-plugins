/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;

import com.microsoft.azure.toolkits.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.model.WebApp;
import com.microsoft.azure.toolkits.appservice.service.webapp.WebAppManagerImpl;

import java.io.File;

public interface WebAppManager {
    void start();

    void stop();

    void restart();

    void delete();

    WebApp get();

    // todo: leverage deployment status api once done
    void deploy(DeployType deployType, File file);

    boolean exists();

    PublishingProfile getPublishingProfile();

    Runtime getRuntime();

    WebAppManagerImpl.WebAppUpdatable update();

    WebAppDeploymentSlotsManager deploymentSlots();
}
