/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandlerBase;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
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

public class ArtifactHandlerImplV2 extends ArtifactHandlerBase {
    private static final int MAX_RETRY_TIMES = 3;

    public static class Builder extends ArtifactHandlerBase.Builder<ArtifactHandlerImplV2.Builder> {
        @Override
        protected ArtifactHandlerImplV2.Builder self() {
            return this;
        }

        @Override
        public ArtifactHandlerImplV2 build() {
            return new ArtifactHandlerImplV2(this);
        }
    }

    protected ArtifactHandlerImplV2(final ArtifactHandlerImplV2.Builder builder) {
        super(builder);
    }

    @Override
    public void publish(final DeployTarget target) throws MojoExecutionException, IOException {
        if (resources == null || resources.size() < 1) {
            log.warn("No <resources> is found in <deployment> element in pom.xml, skip deployment.");
            return;
        }

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
                log.warn(
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
        Utils.copyResources(project, session, filtering, resources, stagingDirectoryPath);
    }

    protected void publishArtifactsViaZipDeploy(final DeployTarget target,
                                                final String stagingDirectoryPath) throws MojoExecutionException {
        final File stagingDirectory = new File(stagingDirectoryPath);
        final File zipFile = new File(stagingDirectoryPath + ".zip");
        ZipUtil.pack(stagingDirectory, zipFile);
        log.info(String.format("Deploying the zip package %s...", zipFile.getName()));

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                target.zipDeploy(zipFile);
            }
        };
        final boolean deploySuccess = performActionWithRetry(runnable, MAX_RETRY_TIMES, log);
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
        log.info(String.format("Deploying the war file %s...", warArtifact.getName()));

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
        final boolean deploySuccess = performActionWithRetry(executor, MAX_RETRY_TIMES, log);
        if (!deploySuccess) {
            throw new MojoExecutionException(
                String.format("Failed to deploy war file after %d times of retry.", MAX_RETRY_TIMES + 1));
        }
    }
}
