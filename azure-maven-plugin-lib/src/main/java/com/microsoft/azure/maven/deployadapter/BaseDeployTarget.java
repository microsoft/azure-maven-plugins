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

public abstract class BaseDeployTarget {
    protected DeployTargetType type;
    protected WebAppBase baseApp;

    public BaseDeployTarget(WebAppBase baseApp, DeployTargetType type) {
        this.baseApp = baseApp;
        this.type = type;
    }

    public PublishingProfile getPublishingProfile() {
        return baseApp.getPublishingProfile();
    }

    public String getName() {
        return baseApp.name();
    }

    public String getType() {
        return type.toString();
    }

    public String getDefaultHostName() {
        return baseApp.defaultHostName();
    }

    public void postPublish() {
        // default do nothing
        // function app need to do syncTriggers() in post publish
    }

    public abstract void zipDeploy(File file);
}
