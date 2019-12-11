/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers.artifact;

import com.google.common.io.Files;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.webapp.deploytarget.DeploymentSlotDeployTarget;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArtifactHandlerUtils {

    public static final String DEFAULT_APP_SERVICE_JAR_NAME = "app.jar";

    /**
     * Interfaces WebApp && DeploymentSlot define their own warDeploy API separately.
     * Ideally, it should be defined in their base interface WebAppBase.
     * {@link com.microsoft.azure.management.appservice.WebAppBase}
     * Comparing to abstracting an adapter for WebApp && DeploymentSlot, we choose a lighter solution:
     * work around to get the real implementation of warDeploy.
     */
    public static Runnable getRealWarDeployExecutor(final DeployTarget target, final File war, final String path)
        throws MojoExecutionException {
        if (target instanceof WebAppDeployTarget) {
            return new Runnable() {
                @Override
                public void run() {
                    ((WebAppDeployTarget) target).warDeploy(war, path);
                }
            };
        }

        if (target instanceof DeploymentSlotDeployTarget) {
            return new Runnable() {
                @Override
                public void run() {
                    ((DeploymentSlotDeployTarget) target).warDeploy(war, path);
                }
            };
        }
        throw new MojoExecutionException(
            "The type of deploy target is unknown, supported types are WebApp and DeploymentSlot.");
    }

    public static boolean performActionWithRetry(final Runnable runnable, final int maxRetryTimes, final Log log) {
        int retryCount = 0;
        while (retryCount < maxRetryTimes) {
            retryCount += 1;
            try {
                runnable.run();
                return true;
            } catch (Exception e) {
                log.info(String.format("Exception occurred during deployment: %s, retry immediately(%d/%d)...",
                    e.getMessage(), retryCount, maxRetryTimes));
            }
        }
        return false;
    }

    public static String getContextPathFromFileName(final String stagingDirectoryPath,
                                                    final String filePath) throws MojoExecutionException {
        if (StringUtils.isEmpty(stagingDirectoryPath)) {
            throw new MojoExecutionException(
                "Can not get the context path because the staging directory path is null or empty. .");
        }
        if (StringUtils.isEmpty(filePath)) {
            throw new MojoExecutionException("Can not get the context path because the file path is null or empty");
        }
        return Paths.get(stagingDirectoryPath).relativize(Paths.get(filePath).getParent()).toString();
    }

    public static List<File> getArtifacts(final File directory) {
        final File[] files = directory.listFiles();
        return Arrays.stream(files).filter(File::isFile).collect(Collectors.toList());
    }

    public static boolean areAllWarFiles(final List<File> allArtifacts) {
        for (final File artifacts : allArtifacts) {
            if (!"war".equalsIgnoreCase(Files.getFileExtension(artifacts.getName()))) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasWarFiles(final List<File> allArtifacts) {
        for (final File artifacts : allArtifacts) {
            if ("war".equalsIgnoreCase(Files.getFileExtension(artifacts.getName()))) {
                return true;
            }
        }
        return false;
    }
}
