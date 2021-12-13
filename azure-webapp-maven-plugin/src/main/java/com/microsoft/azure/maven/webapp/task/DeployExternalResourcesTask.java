/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.task;

import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.maven.webapp.utils.FTPUtils;
import com.microsoft.azure.maven.webapp.utils.Utils;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppBase;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Deprecated
public class DeployExternalResourcesTask extends AzureTask<IWebAppBase<?>> {
    private static final String DEPLOY_START = "Trying to deploy external resources to %s...";
    private static final String DEPLOY_FINISH = "Successfully deployed the resources to %s";

    final IWebAppBase<?> target;
    final List<DeploymentResource> resources;

    public DeployExternalResourcesTask(final IWebAppBase<?> target, final List<DeploymentResource> resources) {
        this.target = target;
        this.resources = resources;
    }

    @Override
    public IWebAppBase<?> execute() {
        AzureMessager.getMessager().info(AzureString.format(DEPLOY_START, target.name()));
        deployExternalResources(target, resources);
        AzureMessager.getMessager().info(AzureString.format(DEPLOY_FINISH, target.name()));
        return target;
    }

    private void deployExternalResources(final IAppService<?> target, final List<DeploymentResource> resources) {
        if (resources.isEmpty()) {
            return;
        }
        AzureMessager.getMessager().info(AzureString.format("Uploading resources to %s", target.name()));
        final PublishingProfile publishingProfile = target.getPublishingProfile();
        final String serverUrl = publishingProfile.getFtpUrl().split("/", 2)[0];
        try {
            final FTPClient ftpClient = FTPUtils.getFTPClient(serverUrl, publishingProfile.getFtpUsername(), publishingProfile.getFtpPassword());
            for (final DeploymentResource externalResource : resources) {
                uploadResource(externalResource, ftpClient);
            }
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    private static void uploadResource(DeploymentResource resource, FTPClient ftpClient) throws IOException {
        final List<File> files = Utils.getArtifacts(resource);
        final String target = resource.getAbsoluteTargetPath();
        for (final File file : files) {
            FTPUtils.uploadFile(ftpClient, file.getPath(), target);
        }
    }
}
