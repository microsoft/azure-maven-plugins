/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandlerBase;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.utils.WebAppUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.areAllWarFiles;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.getArtifactsRecursively;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.getContextPathFromFileName;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.getRealWarDeployExecutor;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.hasWarFiles;
import static com.microsoft.azure.maven.webapp.handlers.ArtifactHandlerUtils.performActionWithRetry;

public class ArtifactHandlerImplV2 extends ArtifactHandlerBase {
    private static final int MAX_RETRY_TIMES = 3;
    private static final String ALWAYS_DEPLOY_PROPERTY = "alwaysDeploy";
    public static final String DEFAULT_LINUX_JAR_NAME = "app.jar";
    public static final String RENAMING_MESSAGE = "Renaming %s to %s";

    private RuntimeSetting runtimeSetting;

    public static class Builder extends ArtifactHandlerBase.Builder<ArtifactHandlerImplV2.Builder> {

        private RuntimeSetting runtimeSetting;

        public RuntimeSetting getRuntimeSetting() {
            return runtimeSetting;
        }

        @Override
        protected ArtifactHandlerImplV2.Builder self() {
            return this;
        }

        public ArtifactHandlerImplV2.Builder runtime(RuntimeSetting runtimeSetting) {
            this.runtimeSetting = runtimeSetting;
            return self();
        }

        @Override
        public ArtifactHandlerImplV2 build() {
            return new ArtifactHandlerImplV2(this);
        }
    }

    protected ArtifactHandlerImplV2(final ArtifactHandlerImplV2.Builder builder) {
        super(builder);
        this.runtimeSetting = builder.getRuntimeSetting();
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

        log.info(String.format(DEPLOY_START, target.getName()));

        if (areAllWarFiles(allArtifacts)) {
            publishArtifactsViaWarDeploy(target, stagingDirectoryPath, allArtifacts);
            log.info(String.format(DEPLOY_FINISH, target.getDefaultHostName()));
            return;
        }

        if (!hasWarFiles(allArtifacts)) {
            publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
            log.info(String.format(DEPLOY_FINISH, target.getDefaultHostName()));
            return;
        }

        if (isDeployMixedArtifactsConfirmed()) {
            publishArtifactsViaZipDeploy(target, stagingDirectoryPath);
            log.info(String.format(DEPLOY_FINISH, target.getDefaultHostName()));
        } else {
            log.info(DEPLOY_ABORT);
        }
    }

    protected boolean isDeployMixedArtifactsConfirmed() {
        if ("true".equalsIgnoreCase(System.getProperty(ALWAYS_DEPLOY_PROPERTY))) {
            return true;
        }

        log.info(String.format("To get rid of the following message, set the property %s to true to always proceed " +
            "with the deploy.", ALWAYS_DEPLOY_PROPERTY));

        final Scanner scanner = new Scanner(System.in, "UTF-8");
        while (true) {
            log.warn("Deploying war along with other kinds of artifacts might make the web app inaccessible, " +
                "are you sure to proceed (y/n)?");
            final String input = scanner.nextLine();
            if ("y".equalsIgnoreCase(input)) {
                return true;
            } else if ("n".equalsIgnoreCase(input)) {
                return false;
            }
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
        if (isJavaSERuntime()) {
            prepareJavaSERuntime(getAllArtifacts(stagingDirectoryPath));
        }
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
                String.format("Failed to deploy war file after %d times of retry.", MAX_RETRY_TIMES));
        }
    }

    private boolean isJavaSERuntime() throws MojoExecutionException {
        return runtimeSetting != null &&
            ((runtimeSetting.getOs().equals("windows") && this.project.getPackaging().equals("jar")) ||
                (runtimeSetting.getOs().equals("linux") &&
                    this.runtimeSetting.getLinuxRuntime().stack().equals("JAVA")));
    }

    private File getProjectJarArtifact(final List<File> artifacts) {
        final String finalName = project.getBuild().getFinalName();
        final String fileName = String.format("%s.jar", finalName, project.getPackaging());
        for (final File file : artifacts) {
            if (file.getName().equals(fileName)) {
                return file;
            }
        }
        return null;
    }

    /**
     * Rename project jar to app.jar for linux app service or generate web.config for windows javase app service
     */
    private void prepareJavaSERuntime(final List<File> artifacts) throws MojoExecutionException {
        final File artifact = getProjectJarArtifact(artifacts);
        if (artifact == null) {
            return;
        }
        if (runtimeSetting.getOs().equals("windows")) {
            // Windows: Generate web.config to staging folder
            try {
                WebAppUtils.generateWebConfigFile(artifact.getName(), log, stagingDirectoryPath, this.getClass());
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to generate web.config file");
            }
        } else {
            // Linux: Rename project artifact to app.jar
            log.info(String.format(RENAMING_MESSAGE, artifact.getAbsolutePath(), DEFAULT_LINUX_JAR_NAME));
            artifact.renameTo(new File(artifact.getParent(), "app.jar"));
        }
    }
}
