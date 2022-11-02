/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.models.FunctionAppBasic;
import com.azure.resourcemanager.appservice.models.PlatformArchitecture;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class FunctionApp extends FunctionAppBase<FunctionApp, AppServiceServiceSubscription, com.azure.resourcemanager.appservice.models.FunctionApp>
    implements Deletable {

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

    protected FunctionApp(@Nonnull FunctionAppBasic remote, @Nonnull FunctionAppModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.deploymentModule = new FunctionAppDeploymentSlotModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(deploymentModule);
    }

    @Nullable
    @Override
    public String getMasterKey() {
        return Optional.ofNullable(this.getFullRemote()).map(com.azure.resourcemanager.appservice.models.FunctionApp::getMasterKey).orElse(null);
    }

    @Override
    public void enableRemoteDebug() {
        final Map<String, String> appSettings = Optional.ofNullable(this.getAppSettings()).orElseGet(HashMap::new);
        Objects.requireNonNull(getFullRemote()).update()
                .withWebSocketsEnabled(true)
                .withPlatformArchitecture(PlatformArchitecture.X64)
                .withAppSetting(HTTP_PLATFORM_DEBUG_PORT, appSettings.getOrDefault(HTTP_PLATFORM_DEBUG_PORT, DEFAULT_REMOTE_DEBUG_PORT))
                .withAppSetting(JAVA_OPTS, getJavaOptsWithRemoteDebugEnabled(appSettings)).apply();
    }

    @Override
    public void disableRemoteDebug() {
        final Map<String, String> appSettings = Objects.requireNonNull(this.getAppSettings());
        final String javaOpts = this.getJavaOptsWithRemoteDebugDisabled(appSettings);
        if (StringUtils.isEmpty(javaOpts)) {
            Objects.requireNonNull(getFullRemote()).update().withoutAppSetting(HTTP_PLATFORM_DEBUG_PORT).withoutAppSetting(JAVA_OPTS).apply();
        } else {
            Objects.requireNonNull(getFullRemote()).update().withoutAppSetting(HTTP_PLATFORM_DEBUG_PORT).withAppSetting(JAVA_OPTS, javaOpts).apply();
        }
    }

    @Nonnull
    public List<FunctionEntity> listFunctions(boolean... force) {
        return Optional.ofNullable(this.getFullRemote()).map(r -> r.listFunctions().stream()
                .map(envelope -> AppServiceUtils.fromFunctionAppEnvelope(envelope, this.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    public void triggerFunction(String functionName, Object input) {
        Optional.ofNullable(this.getFullRemote()).ifPresent(r -> r.triggerFunction(functionName, input));
    }

    public void swap(String slotName) {
        this.doModify(() -> {
            Objects.requireNonNull(this.getFullRemote()).swap(slotName);
            AzureMessager.getMessager().info(AzureString.format("Swap deployment slot %s into production successfully", slotName));
        }, Status.UPDATING);
    }

    public void syncTriggers() {
        Optional.ofNullable(this.getFullRemote()).ifPresent(com.azure.resourcemanager.appservice.models.FunctionApp::syncTriggers);
    }

    @Nonnull
    public Map<String, String> listFunctionKeys(String functionName) {
        return Optional.ofNullable(this.getFullRemote()).map(r -> r.listFunctionKeys(functionName)).orElseGet(HashMap::new);
    }

    @Nonnull
    public FunctionAppDeploymentSlotModule slots() {
        return this.deploymentModule;
    }
}
