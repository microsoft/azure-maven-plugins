/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ZIPArtifactHandler<T extends AbstractAppServiceMojo> implements ArtifactHandler {
    private static final String NO_RESOURCES = "No resources were specified in pom.xml or copied to stage directory.";
    protected T mojo;

    public ZIPArtifactHandler(final T mojo) {
        this.mojo = mojo;
    }

    protected String getDeploymentStageDirectory() {
        return Paths.get(mojo.getBuildDirectoryAbsolutePath(), this.mojo.getAppName()).toString();
    }

    @Override
    public void publish(DeployTarget target) throws ZipException, MojoExecutionException, IOException {
        // todo: function app zip deploy could not be tested until the sdk 1.14.0 release
        prepareResources();
        assureStageDirectoryNotEmpty();
        target.zipDeploy(getZipFile());
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

    protected File getZipFile() throws ZipException {
        final String stageDirectory = getDeploymentStageDirectory();
        final File stageFolder = new File(stageDirectory);

        final ZipFile zipFile = new ZipFile(Paths.get(mojo.getBuildDirectoryAbsolutePath(),
            new SimpleDateFormat("yyyyMMddHHmm'_app.zip'").format(new Date())).toString());
        final ZipParameters zipParameters = new ZipParameters();
        zipParameters.setIncludeRootFolder(false);
        zipFile.createZipFileFromFolder(stageFolder, zipParameters, false, -1);
        return zipFile.getFile();
    }
}
