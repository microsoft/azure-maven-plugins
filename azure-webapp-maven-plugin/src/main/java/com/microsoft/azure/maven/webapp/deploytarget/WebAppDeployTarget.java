/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.deploytarget;

import com.microsoft.azure.common.appservice.DeployTargetType;
import com.microsoft.azure.common.appservice.DeployTarget;
import com.microsoft.azure.management.appservice.WebApp;

import java.io.File;

public class WebAppDeployTarget extends DeployTarget<WebApp> {
    public WebAppDeployTarget(final WebApp app) {
        super(app, DeployTargetType.WEBAPP);
    }

    public void warDeploy(File war, String path) {
        app.warDeploy(war, path);
    }
}
