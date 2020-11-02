/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.deploytarget;

import com.microsoft.azure.common.appservice.DeployTargetType;
import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.DeployOptions;
import com.microsoft.azure.management.appservice.DeployType;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebAppBase;

import javax.annotation.Nonnull;

import java.io.File;
import java.util.Map;

public class DeployTarget<T extends WebAppBase> {
    protected DeployTargetType type;
    protected T app;

    public DeployTarget(@Nonnull final T app, final DeployTargetType type) {
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

    public void zipDeploy(final File file) {
        app.zipDeploy(file);
    }

    public void msDeploy(final String packageUri, final boolean deleteExistingDeploymentSlot) {
        app.deploy()
            .withPackageUri(packageUri)
            .withExistingDeploymentsDeleted(deleteExistingDeploymentSlot)
            .execute();
    }

    public T getApp() {
        return this.app;
    }

    public void deploy(DeployType type, File file, DeployOptions deployOptions) {
        throw new UnsupportedOperationException("calling com.microsoft.azure.common.deploytarget.DeployTarget.deploy is unsupported currently.");
    }
}
