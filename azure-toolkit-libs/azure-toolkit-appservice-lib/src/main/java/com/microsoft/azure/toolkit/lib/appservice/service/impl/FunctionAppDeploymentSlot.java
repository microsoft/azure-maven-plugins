/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.FunctionApp;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppDeploymentSlot;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class FunctionAppDeploymentSlot extends AbstractAppService<FunctionDeploymentSlot, FunctionAppDeploymentSlotEntity>
        implements IFunctionAppDeploymentSlot {

    private final AzureResourceManager azureClient;

    public FunctionAppDeploymentSlot(FunctionAppDeploymentSlotEntity deploymentSlot, AzureResourceManager azureClient) {
        this.entity = deploymentSlot;
        this.azureClient = azureClient;
    }

    @Override
    public IFunctionApp functionApp() {
        return Azure.az(AzureAppService.class).functionApp(getRemoteResource().id());
    }

    @Override
    public Creator create() {
        return null;
    }

    @Override
    public Updater update() {
        return null;
    }

    @Override
    public void deploy(File targetFile) {

    }

    @Override
    public void deploy(File targetFile, FunctionDeployType functionDeployType) {

    }

    @NotNull
    @Override
    protected FunctionAppDeploymentSlotEntity getEntityFromRemoteResource(@NotNull FunctionDeploymentSlot remote) {
        return AppServiceUtils.fromFunctionAppDeploymentSlot(remote);
    }

    @Override
    public void delete() {
        getParentWebApp().deploymentSlots().deleteById(getRemoteResource().id());
    }

    @Nullable
    @Override
    protected FunctionDeploymentSlot remote() {
        final FunctionApp parentFunctionApp = getParentWebApp();
        return StringUtils.isNotEmpty(entity.getId()) ?
                parentFunctionApp.deploymentSlots().getById(entity.getId()) :
                parentFunctionApp.deploymentSlots().getByName(entity.getName());
    }

    private FunctionApp getParentWebApp() {
        return StringUtils.isNotEmpty(entity.getId()) ?
                azureClient.functionApps().getById(ResourceId.fromString(entity().getId()).parent().id()) :
                azureClient.functionApps().getByResourceGroup(entity.getResourceGroup(), entity.getFunctionAppName());
    }
}
