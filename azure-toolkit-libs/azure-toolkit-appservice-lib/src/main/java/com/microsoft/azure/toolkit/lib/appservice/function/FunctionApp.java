/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.models.FunctionAppBasic;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceManager;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class FunctionApp extends FunctionAppBase<FunctionApp, AppServiceResourceManager, com.azure.resourcemanager.appservice.models.FunctionApp>
    implements Removable {

    @Nonnull
    private final FunctionAppDeploymentSlotModule deploymentModule;

    protected FunctionApp(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull FunctionAppModule module) {
        super(name, resourceGroupName, module);
        this.deploymentModule = new FunctionAppDeploymentSlotModule(this);
    }

    /**
     * copy constructor
     */
    protected FunctionApp(@Nonnull FunctionApp origin) {
        super(origin);
        this.deploymentModule = origin.deploymentModule;
    }

    protected FunctionApp(@Nonnull com.azure.resourcemanager.appservice.models.FunctionApp remote, @Nonnull FunctionAppModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.deploymentModule = new FunctionAppDeploymentSlotModule(this);
        this.setRemote(remote);
    }

    protected FunctionApp(@Nonnull FunctionAppBasic basic, @Nonnull FunctionAppModule module) {
        super(basic.name(), basic.resourceGroupName(), module);
        this.deploymentModule = new FunctionAppDeploymentSlotModule(this);
        this.setBasic(basic);
    }

    @Nonnull
    @Override
    public List<AzResourceModule<?, FunctionApp, ?>> getSubModules() {
        return Collections.singletonList(deploymentModule);
    }

    @Nullable
    @Override
    public String getMasterKey() {
        return this.remoteOptional().map(com.azure.resourcemanager.appservice.models.FunctionApp::getMasterKey).orElse(null);
    }

    @Nonnull
    @Cacheable(cacheName = "appservice/functionapp/{}/functions", key = "${this.getName()}", condition = "!(force&&force[0])")
    public List<FunctionEntity> listFunctions(boolean... force) {
        return this.remoteOptional().map(r -> r.listFunctions().stream()
                .map(AppServiceUtils::fromFunctionAppEnvelope)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    public void triggerFunction(String functionName, Object input) {
        this.remoteOptional().ifPresent(r -> r.triggerFunction(functionName, input));
    }

    public void swap(String slotName) {
        this.remoteOptional().ifPresent(r -> r.swap(slotName));
    }

    public void syncTriggers() {
        this.remoteOptional().ifPresent(com.azure.resourcemanager.appservice.models.FunctionApp::syncTriggers);
    }

    @Nonnull
    public Map<String, String> listFunctionKeys(String functionName) {
        return this.remoteOptional().map(r -> r.listFunctionKeys(functionName)).orElseGet(HashMap::new);
    }

    @Nonnull
    public FunctionAppDeploymentSlotModule slots() {
        return this.deploymentModule;
    }
}
