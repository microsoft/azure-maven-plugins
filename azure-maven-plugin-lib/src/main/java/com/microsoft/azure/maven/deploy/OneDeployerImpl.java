/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.deploy;

import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appservice.DeployOptions;
import com.microsoft.azure.management.appservice.DeployType;
import com.microsoft.azure.management.appservice.OperatingSystem;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OneDeployerImpl implements Deployer {

    private static Map<DeployType, String> TYPE_TO_BASE_DIRECTORY_MAP;
    private static Map<DeployType, String> TYPE_TO_FILE_PATTERN_MAP;

    private static final String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";

    static {
        TYPE_TO_BASE_DIRECTORY_MAP = new HashMap<>();
        TYPE_TO_BASE_DIRECTORY_MAP.put(DeployType.JAR, "/home/site/wwwroot");
        TYPE_TO_BASE_DIRECTORY_MAP.put(DeployType.EAR, "/home/site/wwwroot");
        TYPE_TO_BASE_DIRECTORY_MAP.put(DeployType.STATIC, "/home/site/wwwroot");
        TYPE_TO_BASE_DIRECTORY_MAP.put(DeployType.ZIP, "/home/site/wwwroot");
        TYPE_TO_BASE_DIRECTORY_MAP.put(DeployType.WAR, "/home/site/wwwroot/webapps");
        TYPE_TO_BASE_DIRECTORY_MAP.put(DeployType.SCRIPT_STARTUP, "/home/site/scripts");
        TYPE_TO_BASE_DIRECTORY_MAP.put(DeployType.JAR_LIB, "/home/site/libs");
    }

    static {
        TYPE_TO_FILE_PATTERN_MAP = new HashMap<>();
        TYPE_TO_FILE_PATTERN_MAP.put(DeployType.JAR, ".*\\.jar");
        TYPE_TO_FILE_PATTERN_MAP.put(DeployType.EAR, ".*\\.ear");
        TYPE_TO_FILE_PATTERN_MAP.put(DeployType.WAR, ".*\\.war");
        TYPE_TO_FILE_PATTERN_MAP.put(DeployType.JAR_LIB, ".*\\.jar");
    }

    @Override
    public void deploy(DeployTarget deployTarget, String deployDir, DeployType deployType, DeployOptions options) throws AzureExecutionException {
        final List<File> deployFiles = FileUtils.listFiles(new File(deployDir), null, true).stream().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(deployFiles)) {
            final String absolutePath = new File(deployDir).getAbsolutePath();
            throw new AzureExecutionException(String.format("There is no artifact to deploy in staging directory: '%s'", absolutePath));
        }
        for (final File file : deployFiles) {
            // validate
            this.validateFileName(file, deployType);
            // parse deploy options
            final DeployOptions parsedOptions = this.parseDeployOptions(file, deployDir, deployType, options);
            // deploy
            this.deployInternal(file, deployDir, deployTarget, deployType, parsedOptions);
        }
        if (Boolean.TRUE.equals(options.restartSite())) {
            Log.info(String.format(DEPLOY_FINISH, deployTarget.getDefaultHostName()));
        }
    }

    private void validateFileName(File file, DeployType deployType) throws AzureExecutionException {
        if (StringUtils.isNotBlank(TYPE_TO_FILE_PATTERN_MAP.get(deployType))) {
            if (!Pattern.matches(TYPE_TO_FILE_PATTERN_MAP.get(deployType), file.getAbsolutePath())) {
                throw new AzureExecutionException(String.format("Illegal file [%s] for current deploy type [%s].", file.getAbsolutePath(), deployType));
            }
        }
    }

    private DeployOptions parseDeployOptions(File file, String deployDir, DeployType deployType, DeployOptions options) {
        final String relativePath = file.getAbsolutePath().substring(deployDir.length() + 1);
        final String newPath = StringUtils.isBlank(options.path()) ? relativePath : Paths.get(options.path(), relativePath).toString();
        if (DeployType.JAR_LIB.equals(deployType) || DeployType.STATIC.equals(deployType)) {
            return new DeployOptions().withRestartSite(options.restartSite()).withCleanDeployment(options.cleanDeployment()).withPath(newPath);
        }
        return options;
    }

    private void deployInternal(File file, String deployDir, DeployTarget deployTarget, DeployType deployType, DeployOptions deployOptions) {
        final String relativePath = file.getAbsolutePath().substring(deployDir.length() + 1);
        final String baseRemotePath;
        if (StringUtils.isNotBlank(deployOptions.path())) {
            baseRemotePath = Paths.get(TYPE_TO_BASE_DIRECTORY_MAP.get(deployType), deployOptions.path(), relativePath).toString();
        } else {
            baseRemotePath = Paths.get(TYPE_TO_BASE_DIRECTORY_MAP.get(deployType), relativePath).toString();
        }
        final String parsedRemotePath = OperatingSystem.WINDOWS.equals(deployTarget.getApp().operatingSystem()) ? "d:" + baseRemotePath : baseRemotePath;
        Log.info(String.format("- Deploying [%s] %s to %s...", deployType, relativePath, parsedRemotePath));
        // deploy
        deployTarget.deploy(deployType, file, deployOptions);
    }

}
