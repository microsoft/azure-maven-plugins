/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.google.common.io.Files;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.webapp.deploytarget.DeploymentSlotDeployTarget;
import com.microsoft.azure.maven.webapp.deploytarget.WebAppDeployTarget;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class ArtifactHandlerUtils {
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

    public static void getArtifactsRecursively(final File directory, final List<File> allFiles) {
        final File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                getArtifactsRecursively(file, allFiles);
            } else {
                allFiles.add(file);
            }
        }
    }

    public static boolean isDeployingOnlyWarArtifacts(final List<Resource> resources) {
        File tempDirectory;
        File[] tempFiles;
        for (final Resource resource : resources) {
            tempDirectory = new File(resource.getTargetPath());
            tempFiles = tempDirectory.listFiles();
            if (tempFiles != null && tempFiles.length != 0) {
                for (final File file : tempFiles) {
                    if (!"war".equalsIgnoreCase(Files.getFileExtension(file.getName()))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
