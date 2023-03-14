/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.deploy;

import com.microsoft.azure.toolkit.lib.appservice.model.CsmDeploymentStatus;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployOptions;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.KuduDeploymentResult;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;

import java.io.File;

public interface IOneDeploy {
    default void deploy(File targetFile) {
        deploy(Utils.getDeployTypeByFileExtension(targetFile), targetFile);
    }

    default void deploy(DeployType deployType, File targetFile) {
        deploy(deployType, targetFile, (String) null);
    }

    default void deploy(WebAppArtifact webAppArtifact) {
        deploy(webAppArtifact.getDeployType(), webAppArtifact.getFile(), webAppArtifact.getPath());
    }

    default void deploy(DeployType deployType, File targetFile, String targetPath) {
        deploy(deployType, targetFile, DeployOptions.builder().path(targetPath).build());
    }

    void deploy(DeployType deployType, File targetFile, DeployOptions deployOptions);

    void pushDeploy(DeployType var1, File var2, DeployOptions var3);

    CsmDeploymentStatus getDeploymentStatus(String var1);

}
