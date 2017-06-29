/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.DeployMojo;
import com.microsoft.azure.maven.webapp.OperationResult;

abstract class ContainerDeployHandler implements DeployHandler {
    public static final String SERVER_ID_NOT_FOUND = "Server Id not found in settings.xml. ServerId=";
    public static final String WEBAPP_NOT_FOUND = "Web app not found. A new one will be created.";
    public static final String CONTAINER_NOT_SUPPORTED =
            "Web app is not Linux-based. ContainerSetting is only supported in Linux web app.";

    protected final DeployMojo mojo;

    public ContainerDeployHandler(final DeployMojo mojo) {
        this.mojo = mojo;
    }

    public OperationResult validate(final WebApp app) {
        // Skip validation for app to be created.
        if (app == null) {
            mojo.getLog().info(WEBAPP_NOT_FOUND);
            return new OperationResult(true, null);
        }

        if (app.operatingSystem() != OperatingSystem.LINUX) {
            return new OperationResult(false, CONTAINER_NOT_SUPPORTED);
        }

        return new OperationResult(true, null);
    }

    public OperationResult deploy(final WebApp app) {
        OperationResult result;
        try {
            internalDeploy(app);
            result = new OperationResult(true, null);
        } catch (Exception e) {
            mojo.getLog().debug(e);
            result = new OperationResult(false, e.getMessage());
        }
        return result;
    }

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

    protected abstract void internalDeploy(final WebApp app) throws Exception;
}
