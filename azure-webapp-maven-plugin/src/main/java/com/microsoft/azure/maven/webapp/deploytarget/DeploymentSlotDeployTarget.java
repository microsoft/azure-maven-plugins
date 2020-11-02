/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.deploytarget;

import com.microsoft.azure.common.appservice.DeployTargetType;
import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.management.appservice.DeployOptions;
import com.microsoft.azure.management.appservice.DeployType;
import com.microsoft.azure.management.appservice.DeploymentSlot;

import java.io.File;

public class DeploymentSlotDeployTarget extends DeployTarget<DeploymentSlot> {
    public DeploymentSlotDeployTarget(final DeploymentSlot slot) {
        super(slot, DeployTargetType.SLOT);
    }

    public void warDeploy(final File war, final String path) {
        app.warDeploy(war, path);
    }

    @Override
    public void deploy(DeployType type, File file, DeployOptions deployOptions) {
        app.deploy(type, file, deployOptions);
    }
}
