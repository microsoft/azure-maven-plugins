/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.utils.ZipUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ZIPArtifactHandler<T extends AbstractAppServiceMojo> implements ArtifactHandler {
    private static final String NO_RESOURCES = "No resources were specified in pom.xml or copied to staging directory.";
    protected T mojo;

    public ZIPArtifactHandler(final T mojo) {
        this.mojo = mojo;
    }

    protected String getDeploymentStageDirectory() {
        return Paths.get(mojo.getBuildDirectoryAbsolutePath(), this.mojo.getAppName()).toString();
    }

    @Override
    public void publish(DeployTarget target) throws MojoExecutionException, IOException {
        // todo: function app zip deploy could not be tested until the sdk 1.14.0 release
        prepareResources();
        assureStageDirectoryNotEmpty();

        target.zipDeploy(new File(getZipFile()));
    }

    protected void prepareResources() throws IOException {
        final List<Resource> resources = this.mojo.getResources();

        if (resources != null && !resources.isEmpty()) {
            Utils.copyResources(mojo.getProject(), mojo.getSession(),
                mojo.getMavenResourcesFiltering(), resources, getDeploymentStageDirectory());
        }
    }

    protected void assureStageDirectoryNotEmpty() throws MojoExecutionException {
        final String stageDirectory = getDeploymentStageDirectory();
        final File stageFolder = new File(stageDirectory);
        final File[] files = stageFolder.listFiles();
        if (!stageFolder.exists() || !stageFolder.isDirectory() || files == null || files.length == 0) {
            throw new MojoExecutionException(NO_RESOURCES);
        }
    }

    protected String getZipFile() throws MojoExecutionException {
        final String stageDirectory = getDeploymentStageDirectory();
        final File stageFolder = new File(stageDirectory);

        final String zipFile = Paths.get(mojo.getBuildDirectoryAbsolutePath(), this.mojo.getAppName() + ".zip")
            .toString();
        final List<File> fileList = new ArrayList<>();

        try {
            ZipUtils.getAllFiles(stageFolder, fileList);
            ZipUtils.writeZipFile(stageFolder, fileList, zipFile);
            return zipFile;
        } catch (Exception e) {
            throw new MojoExecutionException("Exception when zip staging folder contents: " + e.getMessage());
        }
    }
}
