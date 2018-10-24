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
import java.util.List;

import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.areAllWarFiles;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.getArtifactsRecursively;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.getContextPathFromFileName;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.getRealWarDeployExecutor;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.hasWarFiles;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.performActionWithRetry;

public class ArtifactHandlerV2 implements ArtifactHandler {
    private AbstractWebAppMojo mojo;
    private static final int MAX_RETRY_TIMES = 3;

    public ArtifactHandlerV2(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public void publish(final DeployTarget target) throws MojoExecutionException, IOException {
        final Deployment deployment = mojo.getDeployment();
        final List<Resource> resources = deployment.getResources();
        if (resources == null || resources.size() < 1) {
            mojo.getLog().warn("No <resources> is found in <deployment> element in pom.xml, skip deployment.");
            return;
        }

        final String stagingDirectoryPath = mojo.getDeploymentStagingDirectoryPath();
        copyArtifactsToStagingDirectory(resources, stagingDirectoryPath);
        final List<File> allArtifacts = getAllArtifacts(stagingDirectoryPath);

        if (allArtifacts.size() == 0) {
            final String absolutePath = new File(stagingDirectoryPath).getAbsolutePath();
            throw new MojoExecutionException(
                String.format("There is no artifact to deploy in staging directory: '%s'", absolutePath));
        }

        if (areAllWarFiles(allArtifacts)) {
            publishArtifactsViaWarDeploy(target, stagingDirectoryPath, allArtifacts);
        } else {
            if (hasWarFiles(allArtifacts)) {
                mojo.getLog().warn(
                    "Deploying war artifact together with other kinds of artifacts is not suggested," +
                        " it will cause the content be overwritten or path incorrect issues.");
            }
            publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
        }
    }

    protected List<File> getAllArtifacts(final String stagingDirectoryPath) {
        final File stagingDirectory = new File(stagingDirectoryPath);
        return getArtifactsRecursively(stagingDirectory);
    }

    protected void copyArtifactsToStagingDirectory(final List<Resource> resources,
                                                   final String stagingDirectoryPath) throws IOException {
        Utils.copyResources(mojo.getProject(), mojo.getSession(), mojo.getMavenResourcesFiltering(),
        resources, stagingDirectoryPath);
    }

    protected void publishArtifactsViaZipDeploy(final DeployTarget target,
                                                final String stagingDirectoryPath) throws MojoExecutionException {
        final File stagingDirectory = new File(stagingDirectoryPath);
        final File zipFile = new File(stagingDirectoryPath + ".zip");
        ZipUtil.pack(stagingDirectory, zipFile);
        mojo.getLog().info(String.format("Deploying the zip package %s...", zipFile.getName()));

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                target.zipDeploy(zipFile);
            }
        };
        final boolean deploySuccess = performActionWithRetry(runnable, MAX_RETRY_TIMES, mojo.getLog());
        if (!deploySuccess) {
            throw new MojoExecutionException(
                String.format("The zip deploy failed after %d times of retry.", MAX_RETRY_TIMES + 1));
        }
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
        final boolean deploySuccess = performActionWithRetry(executor, MAX_RETRY_TIMES, mojo.getLog());
        if (!deploySuccess) {
            throw new MojoExecutionException(
                String.format("Failed to deploy war file after %d times of retry.", MAX_RETRY_TIMES + 1));
        }
    }
}
