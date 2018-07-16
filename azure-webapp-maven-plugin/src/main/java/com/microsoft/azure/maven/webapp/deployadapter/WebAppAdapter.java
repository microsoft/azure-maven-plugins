/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.deployadapter;

import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;

import java.io.File;

public class WebAppAdapter implements IDeployTargetAdapter {
    private static final String TYPE = "Web App";
    private WebApp app;

    public WebAppAdapter(WebApp app) {
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
        return TYPE;
    }

    @Override
    public String getDefaultHostName() {
        return app.defaultHostName();
    }
}
