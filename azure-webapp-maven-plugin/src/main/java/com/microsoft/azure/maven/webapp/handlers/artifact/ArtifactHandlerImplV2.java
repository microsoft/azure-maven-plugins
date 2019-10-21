/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.artifact;

import com.microsoft.azure.maven.artifacthandler.ArtifactHandlerBase;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static com.microsoft.azure.maven.webapp.handlers.artifact.ArtifactHandlerUtils.DEFAULT_APP_SERVICE_JAR_NAME;
import static com.microsoft.azure.maven.webapp.handlers.artifact.ArtifactHandlerUtils.areAllWarFiles;
import static com.microsoft.azure.maven.webapp.handlers.artifact.ArtifactHandlerUtils.getArtifacts;
import static com.microsoft.azure.maven.webapp.handlers.artifact.ArtifactHandlerUtils.getContextPathFromFileName;
import static com.microsoft.azure.maven.webapp.handlers.artifact.ArtifactHandlerUtils.getRealWarDeployExecutor;
import static com.microsoft.azure.maven.webapp.handlers.artifact.ArtifactHandlerUtils.hasWarFiles;
import static com.microsoft.azure.maven.webapp.handlers.artifact.ArtifactHandlerUtils.performActionWithRetry;

public class ArtifactHandlerImplV2 extends ArtifactHandlerBase {
    private static final int MAX_RETRY_TIMES = 3;
    private static final String ALWAYS_DEPLOY_PROPERTY = "alwaysDeploy";

    private static final String WEB_CONFIG = "web.config";
    private static final String RENAMING_MESSAGE = "Renaming %s to %s";
    private static final String RENAMING_FAILED_MESSAGE = "Failed to rename artifact to %s, which is required in Java SE environment, " +
            "refer to https://docs.microsoft.com/en-us/azure/app-service/containers/configure-language-java#set-java-runtime-options for details.";
    private static final String NO_EXECUTABLE_JAR = "No executable jar found in target folder according to resource filter '%s', " +
            "please make sure the resource filter is correct and you have built the jar.";
    private static final String MULTI_EXECUTABLE_JARS = "Multi executable jars found in <resources>, please check the configuration";


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
        copyArtifactsToStagingDirectory();

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
        return getArtifacts(stagingDirectory);
    }

    protected void copyArtifactsToStagingDirectory() throws IOException, MojoExecutionException {
        prepareResources();
        assureStagingDirectoryNotEmpty();
    }

    protected void publishArtifactsViaZipDeploy(final DeployTarget target,
                                                final String stagingDirectoryPath) throws MojoExecutionException {
        if (isJavaSERuntime()) {
            prepareJavaSERuntime(getAllArtifacts(stagingDirectoryPath));
        }
        final File stagingDirectory = new File(stagingDirectoryPath);
        final File zipFile = new File(stagingDirectoryPath + ".zip");
        ZipUtil.pack(stagingDirectory, zipFile);
        try {
            FileUtils.forceDeleteOnExit(zipFile);
        } catch (IOException e) {
            // swallow this exception for it will not block deployment
        }
        log.info(String.format("Deploying the zip package %s...", zipFile.getName()));

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
        final boolean deploySuccess = performActionWithRetry(() -> target.zipDeploy(zipFile), MAX_RETRY_TIMES, log);
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

    private boolean isJavaSERuntime() {
        if (runtimeSetting != null) {
            final String webContainer = runtimeSetting.getOsEnum() == OperatingSystemEnum.Windows ?
                    runtimeSetting.getWebContainer().toString() : runtimeSetting.getLinuxRuntime().stack();
            return StringUtils.containsIgnoreCase(webContainer, "java");
        }
        return false;
    }

    /**
     * Rename project jar to app.jar for java se app service
     */
    private void prepareJavaSERuntime(final List<File> artifacts) throws MojoExecutionException {
        if (existsWebConfig(artifacts)) {
            return;
        }
        final File artifact = getProjectJarArtifact(artifacts);
        final File renamedArtifact = new File(artifact.getParent(), DEFAULT_APP_SERVICE_JAR_NAME);
        if (!StringUtils.equals(artifact.getName(), DEFAULT_APP_SERVICE_JAR_NAME)) {
            log.info(String.format(RENAMING_MESSAGE, artifact.getAbsolutePath(), DEFAULT_APP_SERVICE_JAR_NAME));
            if (!artifact.renameTo(renamedArtifact)) {
                throw new MojoExecutionException(String.format(RENAMING_FAILED_MESSAGE, DEFAULT_APP_SERVICE_JAR_NAME));
            }
        }
    }


    private File getProjectJarArtifact(final List<File> artifacts) throws MojoExecutionException {
        final String fileName = String.format("%s.%s", project.getBuild().getFinalName(), project.getPackaging());
        final List<File> executableArtifacts = artifacts.stream()
                .filter(file -> isExecutableJar(file)).collect(Collectors.toList());
        final File finalArtifact = executableArtifacts.stream()
                .filter(file -> StringUtils.equals(fileName, file.getName())).findFirst().orElse(null);
        if (executableArtifacts.size() == 0) {
            throw new MojoExecutionException(String.format(NO_EXECUTABLE_JAR, getResourceConfiguration()));
        } else if (finalArtifact == null && executableArtifacts.size() > 1) {
            throw new MojoExecutionException(MULTI_EXECUTABLE_JARS);
        }
        return finalArtifact == null ? executableArtifacts.get(0) : finalArtifact;
    }

    private String getResourceConfiguration() {
        return resources.stream().map(resource -> resource.toString()).collect(Collectors.joining(","));
    }

    private static boolean existsWebConfig(final List<File> artifacts) {
        return artifacts.stream().anyMatch(file -> StringUtils.equals(file.getName(), WEB_CONFIG));
    }

    private static boolean isExecutableJar(File file) {
        try (final FileInputStream fileInputStream = new FileInputStream(file);
             final JarInputStream jarInputStream = new JarInputStream(fileInputStream)) {
            final Manifest manifest = jarInputStream.getManifest();
            return manifest != null && manifest.getMainAttributes().getValue("Main-Class") != null;
        } catch (IOException e) {
            return false;
        }
    }
}
