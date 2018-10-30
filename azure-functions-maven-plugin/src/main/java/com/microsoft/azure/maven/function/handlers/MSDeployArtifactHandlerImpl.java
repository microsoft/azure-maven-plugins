/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.maven.artifacthandler.ArtifactHandlerBase;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.function.AzureStorageHelper;
import com.microsoft.azure.storage.CloudStorageAccount;
import org.codehaus.plexus.util.StringUtils;
import org.zeroturnaround.zip.ZipUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class MSDeployArtifactHandlerImpl extends ArtifactHandlerBase {
    public static final String DEPLOYMENT_PACKAGE_CONTAINER = "java-functions-deployment-packages";
    public static final String ZIP_EXT = ".zip";
    public static final String CREATE_ZIP_START = "Step 1 of 4: Creating ZIP file...";
    public static final String CREATE_ZIP_DONE = "Successfully saved ZIP file at ";
    public static final String STAGE_DIR_NOT_FOUND = "Azure Functions stage directory not found. " +
            "Please run 'mvn package azure-functions:package' first.";
    public static final String LOCAL_SETTINGS_FILE = "local.settings.json";
    public static final String REMOVE_LOCAL_SETTINGS = "Remove local.settings.json from ZIP package.";
    public static final String INTERNAL_STORAGE_KEY = "AzureWebJobsStorage";
    public static final String INTERNAL_STORAGE_NOT_FOUND = "Application setting 'AzureWebJobsStorage' not found.";
    public static final String INTERNAL_STORAGE_CONNECTION_STRING = "Azure Function App's Storage Connection String: ";
    public static final String UPLOAD_PACKAGE_START = "Step 2 of 4: Uploading ZIP file to Azure Storage...";
    public static final String UPLOAD_PACKAGE_DONE = "Successfully uploaded ZIP file to ";
    public static final String DEPLOY_PACKAGE_START = "Step 3 of 4: Deploying Azure Function App with package...";
    public static final String DEPLOY_PACKAGE_DONE = "Successfully deployed Azure Function App.";
    public static final String DELETE_PACKAGE_START = "Step 4 of 4: Deleting deployment package from Azure Storage...";
    public static final String DELETE_PACKAGE_DONE = "Successfully deleted deployment package ";
    public static final String DELETE_PACKAGE_FAIL = "Failed to delete deployment package ";

    protected final String functionAppName;

    public static class Builder extends ArtifactHandlerBase.Builder<Builder> {
        private String functionAppName;

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public MSDeployArtifactHandlerImpl build() {
            return new MSDeployArtifactHandlerImpl(this);
        }

        public Builder functionAppName(final String value) {
            this.functionAppName = value;
            return self();
        }
    }

    private MSDeployArtifactHandlerImpl(@Nonnull final Builder builder) {
        super(builder);
        this.functionAppName = builder.functionAppName;
    }

    @Override
    public void publish(final DeployTarget target) throws Exception {
        final File zipPackage = createZipPackage();

        final CloudStorageAccount storageAccount = getCloudStorageAccount(target);

        final String blobName = getBlobName();

        final String packageUri = uploadPackageToAzureStorage(zipPackage, storageAccount, blobName);

        deployWithPackageUri(target, packageUri, () -> deletePackageFromAzureStorage(storageAccount, blobName));
    }

    protected void logInfo(final String message) {
        if (log != null) {
            log.info(message);
        }
    }

    protected void logDebug(final String message) {
        if (log != null) {
            log.debug(message);
        }
    }

    protected void logError(final String message) {
        if (log != null) {
            log.error(message);
        }
    }

    protected File createZipPackage() throws Exception {
        logInfo("");
        logInfo(CREATE_ZIP_START);

        final File stageDirectory = new File(stagingDirectoryPath);
        final File zipPackage = new File(stagingDirectoryPath.concat(ZIP_EXT));

        if (!stageDirectory.exists()) {
            logError(STAGE_DIR_NOT_FOUND);
            throw new Exception(STAGE_DIR_NOT_FOUND);
        }

        ZipUtil.pack(stageDirectory, zipPackage);

        logDebug(REMOVE_LOCAL_SETTINGS);
        ZipUtil.removeEntry(zipPackage, LOCAL_SETTINGS_FILE);

        logInfo(CREATE_ZIP_DONE + stagingDirectoryPath.concat(ZIP_EXT));
        return zipPackage;
    }

    protected CloudStorageAccount getCloudStorageAccount(final DeployTarget target) throws Exception {
        final Map<String, AppSetting> settingsMap = target.getAppSettings();

        if (settingsMap != null) {
            final AppSetting setting = settingsMap.get(INTERNAL_STORAGE_KEY);
            if (setting != null) {
                final String value = setting.value();
                if (StringUtils.isNotEmpty(value)) {
                    logDebug(INTERNAL_STORAGE_CONNECTION_STRING + value);
                    return CloudStorageAccount.parse(value);
                }
            }
        }
        logError(INTERNAL_STORAGE_NOT_FOUND);
        throw new Exception(INTERNAL_STORAGE_NOT_FOUND);
    }

    protected String getBlobName() {
        return functionAppName
                .concat(new SimpleDateFormat(".yyyyMMddHHmmssSSS").format(new Date()))
                .concat(ZIP_EXT);
    }

    protected String uploadPackageToAzureStorage(final File zipPackage, final CloudStorageAccount storageAccount,
                                                 final String blobName) throws Exception {
        logInfo(UPLOAD_PACKAGE_START);
        final String packageUri = AzureStorageHelper.uploadFileAsBlob(zipPackage, storageAccount,
                DEPLOYMENT_PACKAGE_CONTAINER, blobName);
        logInfo(UPLOAD_PACKAGE_DONE + packageUri);
        return packageUri;
    }

    protected void deployWithPackageUri(final DeployTarget target, final String packageUri, Runnable onDeployFinish) {
        try {
            logInfo(DEPLOY_PACKAGE_START);
            target.msDeploy(packageUri, false);
            logInfo(DEPLOY_PACKAGE_DONE);
        } finally {
            onDeployFinish.run();
        }
    }

    protected void deletePackageFromAzureStorage(final CloudStorageAccount storageAccount, final String blobName) {
        try {
            logInfo(DELETE_PACKAGE_START);
            AzureStorageHelper.deleteBlob(storageAccount, DEPLOYMENT_PACKAGE_CONTAINER, blobName);
            logInfo(DELETE_PACKAGE_DONE + blobName);
        } catch (Exception e) {
            logError(DELETE_PACKAGE_FAIL + blobName);
        }
    }
}
