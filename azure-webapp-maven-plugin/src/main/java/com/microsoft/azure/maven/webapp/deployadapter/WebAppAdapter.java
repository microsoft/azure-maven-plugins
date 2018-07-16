/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.deployadapter;

import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.configuration.DeployTargetType;

import java.io.File;

public class WebAppAdapter implements IDeployTargetAdapter {
    private DeployTargetType type;
    private WebApp app;

    public WebAppAdapter(WebApp app) {
        this.type = DeployTargetType.WEBAPP;
        this.app = app;
    }

    @Override
    public void warDeploy(File war, String contextPath) {
        app.warDeploy(war, contextPath);
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return app.getPublishingProfile();
    }

    @Override
    public String getName() {
        return app.name();
    }

    @Override
    public String getType() {
        return type.toString();
    }

    @Override
    public String getDefaultHostName() {
        return app.defaultHostName();
    }
}
