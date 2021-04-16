/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service;

import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;

import java.io.File;

public interface IAppService extends IResource {
    void start();

    void stop();

    void restart();

    void delete();

    default void deploy(File targetFile) {
        deploy(Utils.getDeployTypeByFileExtension(targetFile), targetFile);
    }

    default void deploy(DeployType deployType, File targetFile) {
        deploy(deployType, targetFile, null);
    }

    default void deploy(WebAppArtifact webAppArtifact) {
        deploy(webAppArtifact.getDeployType(), webAppArtifact.getFile(), webAppArtifact.getPath());
    }

    void deploy(DeployType deployType, File targetFile, String targetPath);

    boolean exists();

    String hostName();

    String state();

    Runtime getRuntime();

    PublishingProfile getPublishingProfile();

    DiagnosticConfig getDiagnosticConfig();
}
