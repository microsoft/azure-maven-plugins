/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.v2;

import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandler;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.zeroturnaround.zip.ZipUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.getArtifactsRecursively;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.getContextPathFromFileName;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.getRealWarDeployExecutor;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.isDeployingOnlyWarArtifacts;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.isMixingWarArtifactWithOtherArtifacts;

public class ArtifactHandlerV2 implements ArtifactHandler {
    private AbstractWebAppMojo mojo;
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;

    public ArtifactHandlerV2(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    public void assureDeploymentResourcesNotEmpty() throws MojoExecutionException {
        final Deployment deployment = mojo.getDeployment();
        final List<Resource> resources = deployment.getResources();
        if (resources == null || resources.size() < 1) {
            throw new MojoExecutionException("The element <resources> inside deployment has to be set to do deploy.");
        }
    }

    @Override
    public void publish(final DeployTarget target) throws MojoExecutionException, IOException {
        assureDeploymentResourcesNotEmpty();

        final Deployment deployment = mojo.getDeployment();
        final List<Resource> resources = deployment.getResources();
        final String stagingDirectoryPath = mojo.getDeploymentStagingDirectoryPath();
        Utils.copyResources(mojo.getProject(), mojo.getSession(), mojo.getMavenResourcesFiltering(),
            resources, stagingDirectoryPath);

        final List<File> allArtifacts = new ArrayList<File>();
        final File stagingDirectory = new File(stagingDirectoryPath);
        getArtifactsRecursively(stagingDirectory, allArtifacts);

        if (allArtifacts.size() == 0) {
            throw new MojoExecutionException(
                String.format(
                    "There is no artifact to deploy in staging directory: '%s'",
                    stagingDirectory.getAbsolutePath()));
        }

        if (isDeployingOnlyWarArtifacts(allArtifacts)) {
            publishArtifactsViaWarDeploy(target, stagingDirectoryPath, allArtifacts);
        } else {
            if (isMixingWarArtifactWithOtherArtifacts(allArtifacts)) {
                mojo.getLog().warn(
                    "Deploying war artifact together with other kinds of artifacts is not suggested," +
                        " it will cause the content be overwritten or path incorrect issues.");
            }
            publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
        }
    }

    protected void publishArtifactsViaZipDeploy(final DeployTarget target,
                                                final String stagingDirectoryPath) throws MojoExecutionException {
        final File stagingDirectory = new File(stagingDirectoryPath);
        final File zipFile = new File(stagingDirectoryPath + ".zip");
        ZipUtil.pack(stagingDirectory, zipFile);
        mojo.getLog().info(String.format("Deploying the zip package %s...", zipFile.getName()));

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
        int retryCount = 0;
        while (retryCount < DEFAULT_MAX_RETRY_TIMES) {
            retryCount += 1;
            try {
                target.zipDeploy(zipFile);
                return;
            } catch (Exception e) {
                mojo.getLog().debug(
                    String.format("Exception occurred when deploying the zip package: %s, " +
                        "retrying immediately (%d/%d)", e.getMessage(), retryCount, DEFAULT_MAX_RETRY_TIMES));
            }
        }

        throw new MojoExecutionException(String.format("The zip deploy failed after %d times of retry.", retryCount));
    }

    protected void publishArtifactsViaWarDeploy(final DeployTarget target, final String stagingDirectoryPath,
                                                final List<File> warArtifacts) throws MojoExecutionException {
        if (warArtifacts == null || warArtifacts.size() == 0) {
            throw new MojoExecutionException(
                String.format("There is no war artifacts to deploy in staging path %s.", stagingDirectoryPath));
        }
        for (final File warArtifact : warArtifacts) {
            final String contextPath = getContextPathFromFileName(stagingDirectoryPath, warArtifact.getAbsolutePath());
            publishWarArtifact(target, warArtifact, contextPath);
        }
    }

    public void publishWarArtifact(final DeployTarget target, final File warArtifact,
                                   final String contextPath) throws MojoExecutionException {
        final Runnable executor = getRealWarDeployExecutor(target, warArtifact, contextPath);
        mojo.getLog().info(String.format("Deploying the war file %s...", warArtifact.getName()));

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
        int retryCount = 0;
        while (retryCount < DEFAULT_MAX_RETRY_TIMES) {
            retryCount++;
            try {
                executor.run();
                return;
            } catch (Exception e) {
                mojo.getLog().debug(String.format("Exception occurred when deploying war file to server: %s, " +
                    "retrying immediately (%d/%d)", e.getMessage(), retryCount, DEFAULT_MAX_RETRY_TIMES));
            }
        }
        throw new MojoExecutionException(
            String.format("Failed to deploy war file after %d times of retry.", retryCount));
    }
}
