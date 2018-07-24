/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.deploytarget;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.deployadapter.BaseDeployTarget;

import java.io.File;

public class FunctionAppDeployTarget extends BaseDeployTarget<FunctionApp> {
    public FunctionAppDeployTarget(final FunctionApp app) {
        super(app, DeployTargetType.FUNCTION);
    }

    public void deploy(final String packageUri,
                       final boolean deleteExistingDeploymentSlot) {
        app.deploy()
            .withPackageUri(packageUri)
            .withExistingDeploymentsDeleted(deleteExistingDeploymentSlot)
            .execute();
    }

    @Override
    public void zipDeploy(File file) {
        // do nothing
    }
}
