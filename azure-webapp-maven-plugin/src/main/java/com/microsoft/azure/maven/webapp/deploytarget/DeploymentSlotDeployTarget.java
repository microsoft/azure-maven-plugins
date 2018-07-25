/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.deploytarget;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.maven.appservice.DeployTargetType;
import com.microsoft.azure.maven.deployadapter.BaseDeployTarget;

import java.io.File;

public class DeploymentSlotDeployTarget extends BaseDeployTarget<DeploymentSlot> {
    public DeploymentSlotDeployTarget(final DeploymentSlot slot) {
        super(slot, DeployTargetType.SLOT);
    }

    @Override
    public void zipDeploy(File file) {
        app.zipDeploy(file);
    }

    public void warDeploy(final File war, final String path) {
        app.warDeploy(war, path);
    }
}
