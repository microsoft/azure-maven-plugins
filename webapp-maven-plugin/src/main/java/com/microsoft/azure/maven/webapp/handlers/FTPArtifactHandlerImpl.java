/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.FTPUploader;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.util.List;

public class FTPArtifactHandlerImpl implements ArtifactHandler {
    public static final String DEFAULT_WEBAPP_ROOT = "/site/wwwroot/";
    public static final int DEFAULT_MAX_RETRY_TIMES = 3;

    private AbstractWebAppMojo mojo;

    public FTPArtifactHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public void publish(final List<Resource> resources) throws Exception {
        copyResourcesToStageDirectory(resources);
        uploadDirectoryToFTP();
    }

    protected void copyResourcesToStageDirectory(final List<Resource> resources) throws IOException {
        Utils.copyResources(mojo.getProject(),
                mojo.getSession(),
                mojo.getMavenResourcesFiltering(),
                resources,
                mojo.getDeploymentStageDirectory());
    }

    protected void uploadDirectoryToFTP() throws MojoExecutionException {
        final FTPUploader uploader = getUploader();
        final WebApp app = mojo.getWebApp();
        final PublishingProfile profile = app.getPublishingProfile();
        final String serverUrl = profile.ftpUrl().split("/", 2)[0];

        uploader.uploadDirectoryWithRetries(serverUrl,
                profile.ftpUsername(),
                profile.ftpPassword(),
                mojo.getDeploymentStageDirectory(),
                DEFAULT_WEBAPP_ROOT,
                DEFAULT_MAX_RETRY_TIMES);
    }

    protected FTPUploader getUploader() {
        return new FTPUploader(mojo.getLog());
    }
}
