/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionAppEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServiceCreator;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServiceUpdater;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppDeploymentSlot;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

public class FunctionApp extends AbstractAppService<com.azure.resourcemanager.appservice.models.FunctionApp, FunctionAppEntity> implements IFunctionApp {
    private final AzureResourceManager azureClient;

    public FunctionApp(FunctionAppEntity entity, AzureResourceManager azureClient) {
        this.entity = entity;
        this.azureClient = azureClient;
    }

    @Override
    public IAppServicePlan plan() {
        return null;
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
        return null;
    }

    @Override
    public List<IFunctionAppDeploymentSlot> deploymentSlots() {
        return null;
    }

    @Override
    public List<FunctionEntity> listFunctions() {
        return null;
    }

    @Override
    public void triggerFunction(String functionName) {

    }

    @Override
    public void swap(String slotName) {

    }

    @Override
    public void syncTriggers() {

    }

    @Override
    public void deploy(File targetFile) {

    }

    @Override
    public void deploy(File targetFile, FunctionDeployType functionDeployType) {

    }

    @Override
    public void delete() {

    }

    @Nonnull
    @Override
    protected FunctionAppEntity getEntityFromRemoteResource(@NotNull com.azure.resourcemanager.appservice.models.FunctionApp remote) {
        return null;
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.appservice.models.FunctionApp remote() {
        return StringUtils.isNotEmpty(entity.getId()) ?
                azureClient.functionApps().getById(entity.getId()) :
                azureClient.functionApps().getByResourceGroup(entity.getResourceGroup(), entity.getName());
    }
}
