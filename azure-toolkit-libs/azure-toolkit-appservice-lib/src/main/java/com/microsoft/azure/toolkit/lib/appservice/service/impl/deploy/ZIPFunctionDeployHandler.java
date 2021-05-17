/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;

import java.io.File;

public class ZIPFunctionDeployHandler implements IFunctionDeployHandler {
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;

    @Override
    public void deploy(File file, WebAppBase functionApp) {
        Log.prompt(String.format(DEPLOY_START, functionApp.name()));

        // Add retry logic here to avoid Kudu's socket timeout issue.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/339
        int retryCount = 0;
        while (retryCount < DEFAULT_MAX_RETRY_TIMES) {
            retryCount += 1;
            try {
                functionApp.zipDeploy(file);
                Log.prompt(String.format(DEPLOY_FINISH, functionApp.defaultHostname()));
                return;
            } catch (Exception e) {
                Log.debug(
                        String.format("Exception occurred when deploying the zip package: %s, " +
                                "retrying immediately (%d/%d)", e.getMessage(), retryCount, DEFAULT_MAX_RETRY_TIMES));
            }
        }

        throw new AzureToolkitRuntimeException(String.format("The zip deploy failed after %d times of retry.", retryCount));
    }
}
