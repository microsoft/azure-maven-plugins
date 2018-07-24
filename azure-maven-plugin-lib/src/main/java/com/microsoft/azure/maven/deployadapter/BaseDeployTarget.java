/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.deployadapter;

import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.maven.appservice.DeployTargetType;

import java.io.File;
import java.util.Map;

public abstract class BaseDeployTarget <T extends WebAppBase> {
    protected DeployTargetType type;
    protected T app;

    public BaseDeployTarget(T app, DeployTargetType type) {
        this.app = app;
        this.type = type;
    }

    public PublishingProfile getPublishingProfile() {
        return app.getPublishingProfile();
    }

    public String getName() {
        return app.name();
    }

    public String getType() {
        return type.toString();
    }

    public String getDefaultHostName() {
        return app.defaultHostName();
    }

    public Map<String, AppSetting> getAppSettings() {
        return app.getAppSettings();
    }

    public abstract void zipDeploy(File file);
}