/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.FTPUploader;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class FTPArtifactHandlerImpl<T extends AbstractAppServiceMojo> implements ArtifactHandler {
    private static final String DEFAULT_WEBAPP_ROOT = "/site/wwwroot";
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;
    private static final String MAVEN_PLUGIN_POSTFIX = "-maven-plugin";
    private static final String NO_RESOURCES = "No resources were specified in pom.xml or copied to staging directory.";

    protected T mojo;

    public FTPArtifactHandlerImpl(final T mojo) {
        this.mojo = mojo;
    }

    protected String getDeploymentStagingDirectory() {
        final String outputFolder = this.mojo.getPluginName().replaceAll(MAVEN_PLUGIN_POSTFIX, "");
        return Paths.get(mojo.getBuildDirectoryAbsolutePath(), outputFolder, this.mojo.getAppName()).toString();
    }

    @Override
    public void publish(DeployTarget target) throws IOException, MojoExecutionException {
        if (target.getApp() instanceof WebApp) {
            prepareResources();
        }
        
        assureStagingDirectoryNotEmpty();

        final FTPUploader uploader = new FTPUploader(mojo.getLog());
        final PublishingProfile profile = target.getPublishingProfile();
        final String serverUrl = profile.ftpUrl().split("/", 2)[0];

        uploader.uploadDirectoryWithRetries(serverUrl,
            profile.ftpUsername(),
            profile.ftpPassword(),
            getDeploymentStagingDirectory(),
            DEFAULT_WEBAPP_ROOT,
            DEFAULT_MAX_RETRY_TIMES);

        if (target.getApp() instanceof FunctionApp) {
            ((FunctionApp) target.getApp()).syncTriggers();
        }
    }

    protected void prepareResources() throws IOException {
        final List<Resource> resources = this.mojo.getResources();

        if (resources != null && !resources.isEmpty()) {
            Utils.copyResources(mojo.getProject(), mojo.getSession(),
                mojo.getMavenResourcesFiltering(), resources, getDeploymentStagingDirectory());
        }
    }

    protected void assureStagingDirectoryNotEmpty() throws MojoExecutionException {
        final String stagingDirectory = getDeploymentStagingDirectory();
        final File stagingFolder = new File(stagingDirectory);
        final File[] files = stagingFolder.listFiles();
        if (!stagingFolder.exists() || !stagingFolder.isDirectory() || files == null || files.length == 0) {
            throw new MojoExecutionException(NO_RESOURCES);
        }
    }
}
