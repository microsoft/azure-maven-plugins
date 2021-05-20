/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppDeploymentSlotEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServiceCreator;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.legacy.function.model.FunctionResource;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionApp extends AbstractAppService<com.azure.resourcemanager.appservice.models.FunctionApp, FunctionAppEntity> implements IFunctionApp {
    private final AzureResourceManager azureClient;

    public FunctionApp(FunctionAppEntity entity, AzureResourceManager azureClient) {
        this.entity = entity;
        this.azureClient = azureClient;
    }

    @Override
    public IAppServicePlan plan() {
        return Azure.az(AzureAppService.class).appServicePlan(getRemoteResource().appServicePlanId());
    }

    @Override
    public IAppServiceCreator<? extends IFunctionApp> create() {
        return null;
    }

    @Override
    public IAppServiceUpdater<? extends IFunctionApp> update() {
        return null;
    }

    @Override
    public IFunctionAppDeploymentSlot deploymentSlot(String slotName) {
        final FunctionAppDeploymentSlotEntity slotEntity = FunctionAppDeploymentSlotEntity.builder()
                .functionAppName(name()).resourceGroup(getRemoteResource().resourceGroupName()).name(slotName).build();
        return new FunctionAppDeploymentSlot(slotEntity, azureClient);
    }

    @Override
    public List<IFunctionAppDeploymentSlot> deploymentSlots() {
        return getRemoteResource().deploymentSlots().list().stream().parallel()
                .map(functionSlotBasic -> FunctionAppDeploymentSlotEntity.builder().id(functionSlotBasic.id()).build())
                .map(slotEntity -> new FunctionAppDeploymentSlot(slotEntity, azureClient))
                .collect(Collectors.toList());
    }

    @Override
    public List<FunctionEntity> listFunctions() {
        return azureClient.functionApps()
                .listFunctions(getRemoteResource().resourceGroupName(), getRemoteResource().name()).stream()
                .map(AppServiceUtils::fromFunctionAppEnvelope)
                .filter(function -> function != null)
                .collect(Collectors.toList());
    }

    @Override
    public void triggerFunction(String functionName) {
        throw new NotImplementedException();
    }

    @Override
    public void swap(String slotName) {
        getRemoteResource().swap(slotName);
    }

    @Override
    public void syncTriggers() {
        getRemoteResource().syncTriggers();
    }

    @Override
    public void deploy(File targetFile) {

    }

    @Override
    public void deploy(File targetFile, FunctionDeployType functionDeployType) {

    }

    @Override
    public void delete() {
        azureClient.functionApps().deleteById(getRemoteResource().id());
    }

    @Nonnull
    @Override
    protected FunctionAppEntity getEntityFromRemoteResource(@NotNull com.azure.resourcemanager.appservice.models.FunctionApp remote) {
        return AppServiceUtils.fromFunctionApp(remote);
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.appservice.models.FunctionApp remote() {
        return StringUtils.isNotEmpty(entity.getId()) ?
                azureClient.functionApps().getById(entity.getId()) :
                azureClient.functionApps().getByResourceGroup(entity.getResourceGroup(), entity.getName());
    }
}
