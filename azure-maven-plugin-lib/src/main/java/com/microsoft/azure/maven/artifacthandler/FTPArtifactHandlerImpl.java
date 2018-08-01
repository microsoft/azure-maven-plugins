/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.management.appservice.DeploymentSlot;
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
    private static final String NO_RESOURCES = "Staging directory: '%s' is empty.";

    protected T mojo;

    public FTPArtifactHandlerImpl(final T mojo) {
        this.mojo = mojo;
    }

    protected String getDeploymentStagingDirectoryPath() {
        final String outputFolder = this.mojo.getPluginName().replaceAll(MAVEN_PLUGIN_POSTFIX, "");
        return Paths.get(mojo.getBuildDirectoryAbsolutePath(), outputFolder, this.mojo.getAppName()).toString();
    }

    protected boolean isPrepareResourceRequired(final DeployTarget target) {
        return target.getApp() instanceof WebApp || target.getApp() instanceof DeploymentSlot;
    }

    @Override
    public void publish(final DeployTarget target) throws IOException, MojoExecutionException {
        if (isPrepareResourceRequired(target)) {
            prepareResources();
        }
        
        assureStagingDirectoryNotEmpty();

        uploadDirectoryToFTP(target);

        if (target.getApp() instanceof FunctionApp) {
            ((FunctionApp) target.getApp()).syncTriggers();
        }
    }

    protected void uploadDirectoryToFTP(DeployTarget target) throws MojoExecutionException {
        final FTPUploader uploader = getUploader();
        final PublishingProfile profile = target.getPublishingProfile();
        final String serverUrl = profile.ftpUrl().split("/", 2)[0];

        uploader.uploadDirectoryWithRetries(serverUrl,
            profile.ftpUsername(),
            profile.ftpPassword(),
            getDeploymentStagingDirectoryPath(),
            DEFAULT_WEBAPP_ROOT,
            DEFAULT_MAX_RETRY_TIMES);
    }

    protected FTPUploader getUploader() {
        return new FTPUploader(mojo.getLog());
    }

    protected void prepareResources() throws IOException {
        final List<Resource> resources = this.mojo.getResources();

        if (resources != null && !resources.isEmpty()) {
            Utils.copyResources(mojo.getProject(), mojo.getSession(),
                mojo.getMavenResourcesFiltering(), resources, getDeploymentStagingDirectoryPath());
        }
    }

    protected void assureStagingDirectoryNotEmpty() throws MojoExecutionException {
        final String stagingDirectoryPath = getDeploymentStagingDirectoryPath();
        final File stagingDirectory = new File(stagingDirectoryPath);
        final File[] files = stagingDirectory.listFiles();
        if (!stagingDirectory.exists() || !stagingDirectory.isDirectory() || files == null || files.length == 0) {
            throw new MojoExecutionException(String.format(NO_RESOURCES, stagingDirectoryPath));
        }
    }
}
