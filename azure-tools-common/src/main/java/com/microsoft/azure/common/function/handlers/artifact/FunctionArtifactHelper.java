/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.handlers.artifact;

import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.common.function.Constants;
import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionDeploymentSlot;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.storage.CloudStorageAccount;

import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Map;

import static com.microsoft.azure.common.function.Constants.INTERNAL_STORAGE_KEY;
import static com.microsoft.azure.common.function.Constants.LOCAL_SETTINGS_FILE;

public class FunctionArtifactHelper {

    private static final String STAGE_DIR_NOT_FOUND = "Azure Functions stage directory not found. " +
            "Please run 'mvn clean azure-functions:package' first.";
    private static final String INTERNAL_STORAGE_NOT_FOUND = "Application setting 'AzureWebJobsStorage' not found.";
    private static final String UNSUPPORTED_DEPLOYMENT_TARGET = "Unsupported deployment target, only function is supported";

    public static File createFunctionArtifact(final String stagingDirectoryPath) throws AzureExecutionException {
        final File stageDirectory = new File(stagingDirectoryPath);
        final File zipPackage = new File(stagingDirectoryPath.concat(Constants.ZIP_EXT));

        if (!stageDirectory.exists() || !stageDirectory.isDirectory()) {
            throw new AzureExecutionException(STAGE_DIR_NOT_FOUND);
        }

        ZipUtil.pack(stageDirectory, zipPackage);
        ZipUtil.removeEntry(zipPackage, LOCAL_SETTINGS_FILE);

        return zipPackage;
    }

    public static void updateAppSetting(final DeployTarget deployTarget, final String key, final String value) throws AzureExecutionException {
        final WebAppBase targetApp = deployTarget.getApp();
        if (targetApp instanceof FunctionApp) {
            ((FunctionApp) targetApp).update().withAppSetting(key, value).apply();
        } else if (targetApp instanceof FunctionDeploymentSlot) {
            ((FunctionDeploymentSlot) targetApp).update().withAppSetting(key, value).apply();
        } else {
            throw new AzureExecutionException(UNSUPPORTED_DEPLOYMENT_TARGET);
        }
    }

    public static CloudStorageAccount getCloudStorageAccount(final DeployTarget target) throws AzureExecutionException {
        final Map<String, AppSetting> settingsMap = target.getAppSettings();

        if (settingsMap != null) {
            final AppSetting setting = settingsMap.get(INTERNAL_STORAGE_KEY);
            if (setting != null) {
                final String value = setting.value();
                if (StringUtils.isNotEmpty(value)) {
                    try {
                        return CloudStorageAccount.parse(value);
                    } catch (InvalidKeyException | URISyntaxException e) {
                        throw new AzureExecutionException("Cannot parse storage connection string due to error: " + e.getMessage(), e);
                    }
                }
            }
        }
        throw new AzureExecutionException(INTERNAL_STORAGE_NOT_FOUND);
    }
}
