/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.DeployMojo;
import org.apache.maven.plugin.MojoExecutionException;

abstract class ContainerDeployHandler implements DeployHandler {
    public static final String SERVER_ID_NOT_FOUND = "Server Id not found in settings.xml. ServerId=";
    public static final String WEBAPP_NOT_FOUND = "Web app not found. A new one will be created.";
    public static final String CONTAINER_NOT_SUPPORTED =
            "Web app is not Linux-based. ContainerSetting is only supported in Linux web app.";

    protected final DeployMojo mojo;

    public ContainerDeployHandler(final DeployMojo mojo) {
        this.mojo = mojo;
    }

    public void validate(final WebApp app) throws Exception {
        // Skip validation for app to be created.
        if (app == null) {
            mojo.getLog().info(WEBAPP_NOT_FOUND);
            return;
        }

        if (app.operatingSystem() != OperatingSystem.LINUX) {
            throw new MojoExecutionException(CONTAINER_NOT_SUPPORTED);
        }
    }

    public abstract void deploy(final WebApp app) throws Exception;

    protected WebApp.DefinitionStages.WithDockerContainerImage defineApp() {
        final boolean isGroupExisted = mojo.getAzureClient()
                .resourceGroups()
                .checkExistence(mojo.getResourceGroup());

        if (isGroupExisted) {
            return mojo.getAzureClient().webApps()
                    .define(mojo.getAppName())
                    .withRegion(mojo.getRegion())
                    .withExistingResourceGroup(mojo.getResourceGroup())
                    .withNewLinuxPlan(mojo.getPricingTier());
        } else {
            return mojo.getAzureClient().webApps()
                    .define(mojo.getAppName())
                    .withRegion(mojo.getRegion())
                    .withNewResourceGroup(mojo.getResourceGroup())
                    .withNewLinuxPlan(mojo.getPricingTier());
        }
    }

    protected WebApp.UpdateStages.WithDockerContainerImage updateApp(final WebApp app) {
        // Work Around:
        // When a web app is created from Azure Portal, there are hidden tags associated with the app.
        // It will be messed up when calling "update" API.
        // An issue is logged at https://github.com/Azure/azure-sdk-for-java/issues/1755 .
        // Remove all tags here to make it work.
        app.inner().withTags(null);
        // Work Around:
        // The actual kind now is "app;linux" which will cause bad request exception,
        // because Azure doesn't accept semicolon as parameter value.
        // Hard-code to "app" to make it work.
        app.inner().withKind("app");
        return app.update();
    }
}
