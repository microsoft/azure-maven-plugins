/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy;

import com.azure.resourcemanager.appservice.models.FunctionApp;
import com.azure.resourcemanager.appservice.models.PublishingProfile;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.artifact.FTPUploader;

import java.io.File;

public class FTPFunctionDeployHandler implements IFunctionDeployHandler {
    private static final String DEFAULT_WEBAPP_ROOT = "/site/wwwroot";
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;

    @Override
    public void deploy(final File file, final WebAppBase webAppBase) {
        final FTPUploader uploader = new FTPUploader();
        final PublishingProfile profile = webAppBase.getPublishingProfile();
        final String serverUrl = profile.ftpUrl().split("/", 2)[0];

        try {
            uploader.uploadDirectoryWithRetries(serverUrl, profile.ftpUsername(), profile.ftpPassword(),
                    file.getAbsolutePath(), DEFAULT_WEBAPP_ROOT, DEFAULT_MAX_RETRY_TIMES);
        } catch (AzureExecutionException e) {
            throw new AzureToolkitRuntimeException("Failed to upload artifact to azure", e);
        }

        if (webAppBase instanceof FunctionApp) {
            ((FunctionApp) webAppBase).syncTriggers();
        }

        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, webAppBase.defaultHostname()));
    }
}
