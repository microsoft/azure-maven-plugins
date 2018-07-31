/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class ZIPArtifactHandler<T extends AbstractAppServiceMojo> implements ArtifactHandler {
    private static final String MAVEN_PLUGIN_POSTFIX = "-maven-plugin";
    private static final String NO_RESOURCES = "Staging directory: '%s' is empty.";
    protected T mojo;

    public ZIPArtifactHandler(final T mojo) {
        this.mojo = mojo;
    }

    protected String getDeploymentStagingDirectory() {
        final String outputFolder = this.mojo.getPluginName().replaceAll(MAVEN_PLUGIN_POSTFIX, "");
        return Paths.get(mojo.getBuildDirectoryAbsolutePath(), outputFolder, this.mojo.getAppName()).toString();
    }

    @Override
    public void publish(DeployTarget target) throws MojoExecutionException, IOException {
        // todo: function app zip deploy could not be tested until the sdk 1.14.0 release
        prepareResources();
        assureStagingDirectoryNotEmpty();

        target.zipDeploy(getZipFile());
    }

    protected void prepareResources() throws IOException {
        final List<Resource> resources = this.mojo.getResources();

        if (resources != null && !resources.isEmpty()) {
            Utils.copyResources(mojo.getProject(), mojo.getSession(),
                mojo.getMavenResourcesFiltering(), resources, getDeploymentStagingDirectory());
        }
    }

    protected void assureStagingDirectoryNotEmpty() throws MojoExecutionException {
        final String stagingDirectoryPath = getDeploymentStagingDirectory();
        final File stagingDirectory = new File(stagingDirectoryPath);
        final File[] files = stagingDirectory.listFiles();
        if (!stagingDirectory.exists() || !stagingDirectory.isDirectory() || files == null || files.length == 0) {
            throw new MojoExecutionException(String.format(NO_RESOURCES, stagingDirectoryPath));
        }
    }

    protected File getZipFile() throws MojoExecutionException {
        final String stagingDirectoryPath = getDeploymentStagingDirectory();
        final String outputFolder = this.mojo.getPluginName().replaceAll(MAVEN_PLUGIN_POSTFIX, "");
        final File zipFile = new File(
            Paths.get(mojo.getBuildDirectoryAbsolutePath(), outputFolder, this.mojo.getAppName() + ".zip")
                .toString());
        final File stagingDirectory = new File(stagingDirectoryPath);

        ZipUtil.pack(stagingDirectory, zipFile);

        return zipFile;
    }
}
