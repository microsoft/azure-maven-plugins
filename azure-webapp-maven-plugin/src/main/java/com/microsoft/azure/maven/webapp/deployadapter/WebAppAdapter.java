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
}
