/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.deployadapter;

import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.maven.appservice.DeployTargetType;

import java.io.File;

public abstract class BaseDeployTarget implements IDeployTargetAdapter {
    protected DeployTargetType type;
    protected WebAppBase baseApp;

    public BaseDeployTarget(WebAppBase baseApp, DeployTargetType type) {
        this.baseApp = baseApp;
        this.type = type;
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return baseApp.getPublishingProfile();
    }

    @Override
    public String getName() {
        return baseApp.name();
    }

    @Override
    public String getType() {
        return type.toString();
    }

    @Override
    public String getDefaultHostName() {
        return baseApp.defaultHostName();
    }

    @Override
    public void postPublish() {
        // do nothing
    }

    public abstract void zipDeploy(File file);
}
