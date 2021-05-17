/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy;

import com.azure.resourcemanager.appservice.models.WebAppBase;

import java.io.File;

public interface IFunctionDeployHandler {
    String DEPLOY_START = "Trying to deploy artifact to %s...";
    String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";

    void deploy(final File file, final WebAppBase webAppBase);
}
