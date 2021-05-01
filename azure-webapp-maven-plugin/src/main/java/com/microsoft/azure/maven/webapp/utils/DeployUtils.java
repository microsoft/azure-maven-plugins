/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.maven.model.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class DeployUtils {
    private static final Path FTP_ROOT = Paths.get("/site/wwwroot");
    private static final String DEFAULT_APP_SERVICE_JAR_NAME = "app.jar";
    private static final String WEB_CONFIG = "web.config";
    private static final String RENAMING_MESSAGE = "Renaming %s to %s";
    private static final String RENAMING_FAILED_MESSAGE = "Failed to rename artifact to %s, which is required in Java SE environment, " +
            "refer to https://docs.microsoft.com/en-us/azure/app-service/containers/configure-language-java#set-java-runtime-options for details.";
    private static final String NO_EXECUTABLE_JAR = "No executable jar found in target folder according to resource filter in <resource>, " +
            "please make sure the resource filter is correct and you have built the jar.";
    private static final String MULTI_EXECUTABLE_JARS = "Multi executable jars found in <resources>, please check the configuration";

    public static boolean isExternalResource(Resource resource) {
        final Path target = Paths.get(getAbsoluteTargetPath(resource.getTargetPath()));
        return !target.startsWith(FTP_ROOT);
    }

    public static void deployResourcesWithFtp(IAppService appService, List<Resource> externalResources) throws AzureExecutionException {
        if (externalResources.isEmpty()) {
            return;
        }
        final PublishingProfile publishingProfile = appService.getPublishingProfile();
        final String serverUrl = publishingProfile.getFtpUrl().split("/", 2)[0];
        try {

            final FTPClient ftpClient = FTPUtils.getFTPClient(serverUrl, publishingProfile.getFtpUsername(), publishingProfile.getFtpPassword());
            for (final Resource externalResource : externalResources) {
                uploadResource(externalResource, ftpClient);
            }
        } catch (IOException e) {
            throw new AzureExecutionException(e.getMessage(), e);
        }
    }

    public static boolean isAllWarArtifacts(List<WebAppArtifact> webAppArtifacts) {
        final Set<DeployType> deployTypes = webAppArtifacts.stream().map(WebAppArtifact::getDeployType).collect(Collectors.toSet());
        return deployTypes.size() == 1 && deployTypes.iterator().next() == DeployType.WAR;
    }

    private static void uploadResource(Resource resource, FTPClient ftpClient) throws IOException {
        final List<File> files = Utils.getArtifacts(resource);
        final String target = getAbsoluteTargetPath(resource.getTargetPath());
        for (final File file : files) {
            FTPUtils.uploadFile(ftpClient, file.getPath(), target);
        }
    }

    public static String getAbsoluteTargetPath(String targetPath) {
        // convert null to empty string
        targetPath = StringUtils.defaultString(targetPath);
        return StringUtils.startsWith(targetPath, "/") ? targetPath :
                FTP_ROOT.resolve(Paths.get(targetPath)).normalize().toString();
    }

    /**
     * Rename project jar to app.jar for java se app service
     */
    public static void prepareJavaSERuntimeJarArtifact(final List<File> artifacts, final String finalName) throws AzureExecutionException {
        if (existsWebConfig(artifacts)) {
            return;
        }
        final File artifact = getProjectJarArtifact(artifacts, finalName);
        final File renamedArtifact = new File(artifact.getParent(), DEFAULT_APP_SERVICE_JAR_NAME);
        if (!StringUtils.equals(artifact.getName(), DEFAULT_APP_SERVICE_JAR_NAME)) {
            Log.info(String.format(RENAMING_MESSAGE, artifact.getAbsolutePath(), DEFAULT_APP_SERVICE_JAR_NAME));
            if (!artifact.renameTo(renamedArtifact)) {
                throw new AzureExecutionException(String.format(RENAMING_FAILED_MESSAGE, DEFAULT_APP_SERVICE_JAR_NAME));
            }
        }
    }

    private static File getProjectJarArtifact(final List<File> artifacts, final String finalName) throws AzureExecutionException {
        final List<File> executableArtifacts = artifacts.stream()
                .filter(file -> isExecutableJar(file)).collect(Collectors.toList());
        final File finalArtifact = executableArtifacts.stream()
                .filter(file -> StringUtils.equals(finalName, file.getName())).findFirst().orElse(null);
        if (executableArtifacts.size() == 0) {
            throw new AzureExecutionException(NO_EXECUTABLE_JAR);
        } else if (finalArtifact == null && executableArtifacts.size() > 1) {
            throw new AzureExecutionException(MULTI_EXECUTABLE_JARS);
        }
        return finalArtifact == null ? executableArtifacts.get(0) : finalArtifact;
    }

    private static boolean existsWebConfig(final List<File> artifacts) {
        return artifacts.stream().anyMatch(file -> StringUtils.equals(file.getName(), WEB_CONFIG));
    }

    private static boolean isExecutableJar(File file) {
        if (!StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(file.getName()), "jar")) {
            return false;
        }
        try (final FileInputStream fileInputStream = new FileInputStream(file);
             final JarInputStream jarInputStream = new JarInputStream(fileInputStream)) {
            final Manifest manifest = jarInputStream.getManifest();
            return manifest != null && manifest.getMainAttributes().getValue("Main-Class") != null;
        } catch (IOException e) {
            return false;
        }
    }
}
