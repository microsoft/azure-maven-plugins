/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.handlers.artifact;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.FTPUploader;
import com.microsoft.azure.maven.deploytarget.DeployTarget;

import java.io.IOException;

public class FTPArtifactHandlerImpl extends ArtifactHandlerBase {
    private static final String DEFAULT_WEBAPP_ROOT = "/site/wwwroot";
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;

    public static class Builder extends ArtifactHandlerBase.Builder<Builder> {
        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public FTPArtifactHandlerImpl build() {
            return new FTPArtifactHandlerImpl(this);
        }
    }

    private FTPArtifactHandlerImpl(final Builder builder) {
        super(builder);
    }

    protected boolean isResourcesPreparationRequired(final DeployTarget target) {
        return target.getApp() instanceof WebApp || target.getApp() instanceof DeploymentSlot;
    }

    @Override
    public void publish(final DeployTarget target) throws AzureExecutionException {
        if (isResourcesPreparationRequired(target)) {
            try {
                prepareResources();
            } catch (IOException e) {
                throw new AzureExecutionException("Encounter error when preparing resources:" + e.getMessage(), e);
            }
        }

        assureStagingDirectoryNotEmpty();
        Log.info(String.format(DEPLOY_START, target.getName()));

        uploadDirectoryToFTP(target);

        if (target.getApp() instanceof FunctionApp) {
            ((FunctionApp) target.getApp()).syncTriggers();
        }

        Log.info(String.format(DEPLOY_FINISH, target.getDefaultHostName()));
    }

    protected void uploadDirectoryToFTP(DeployTarget target) throws AzureExecutionException {
        final FTPUploader uploader = getUploader();
        final PublishingProfile profile = target.getPublishingProfile();
        final String serverUrl = profile.ftpUrl().split("/", 2)[0];

        uploader.uploadDirectoryWithRetries(serverUrl,
            profile.ftpUsername(),
            profile.ftpPassword(),
            stagingDirectoryPath,
            DEFAULT_WEBAPP_ROOT,
            DEFAULT_MAX_RETRY_TIMES);
    }

    protected FTPUploader getUploader() {
        return new FTPUploader();
    }
}
