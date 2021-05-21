/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;

import java.io.File;

public class ZIPFunctionDeployHandler implements IFunctionDeployHandler {
    @Override
    public void deploy(File file, WebAppBase functionApp) {
        AzureMessager.getMessager().info(String.format(DEPLOY_START, functionApp.name()));
        functionApp.zipDeploy(file);
        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, functionApp.defaultHostname()));
    }
}
