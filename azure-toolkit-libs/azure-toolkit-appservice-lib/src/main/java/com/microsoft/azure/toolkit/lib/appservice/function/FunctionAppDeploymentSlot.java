/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.fluent.models.HostKeysInner;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlotBasic;
import com.azure.resourcemanager.appservice.models.PlatformArchitecture;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FunctionAppDeploymentSlot extends FunctionAppBase<FunctionAppDeploymentSlot, FunctionApp, FunctionDeploymentSlot> {

    public static final int MAX_PORT = 65535;

    protected FunctionAppDeploymentSlot(@Nonnull String name, @Nonnull FunctionAppDeploymentSlotModule module) {
        super(name, module);
    }

    /**
     * copy constructor
     */
    protected FunctionAppDeploymentSlot(@Nonnull FunctionAppDeploymentSlot origin) {
        super(origin);
    }

    protected FunctionAppDeploymentSlot(@Nonnull FunctionDeploymentSlotBasic remote, @Nonnull FunctionAppDeploymentSlotModule module) {
        super(remote.name(), module);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Override
    public String getMasterKey() {
        final String name = String.format("%s/slots/%s", getParent().getName(), this.getName());
        return getFullRemote().manager().serviceClient().getWebApps().listHostKeysAsync(this.getResourceGroupName(), name).map(HostKeysInner::masterKey).block();
    }

    @Override
    public void enableRemoteDebug() {
        final Map<String, String> appSettings = this.getAppSettings();
        final String debugPort = appSettings.getOrDefault(HTTP_PLATFORM_DEBUG_PORT, getRemoteDebugPort());
        getFullRemote().update()
                .withWebSocketsEnabled(true)
                .withPlatformArchitecture(PlatformArchitecture.X64)
                .withAppSetting(HTTP_PLATFORM_DEBUG_PORT, debugPort)
                .withAppSetting(JAVA_OPTS, getJavaOptsWithRemoteDebugEnabled(appSettings, debugPort)).apply();
    }

    @Override
    public void disableRemoteDebug() {
        final Map<String, String> appSettings = this.getAppSettings();
        final String javaOpts = this.getJavaOptsWithRemoteDebugDisabled(appSettings);
        if (StringUtils.isEmpty(javaOpts)) {
            getFullRemote().update().withoutAppSetting(HTTP_PLATFORM_DEBUG_PORT).withoutAppSetting(JAVA_OPTS).apply();
        } else {
            getFullRemote().update().withoutAppSetting(HTTP_PLATFORM_DEBUG_PORT).withAppSetting(JAVA_OPTS, javaOpts).apply();
        }
    }

    @Override
    protected String getRemoteDebugPort() {
        final List<FunctionAppDeploymentSlot> list = getParent().slots().list();
        final List<Integer> collect = list.stream().filter(slot -> slot.getAppSettings().keySet().contains(HTTP_PLATFORM_DEBUG_PORT))
                .map(slot -> slot.getAppSettings().get(HTTP_PLATFORM_DEBUG_PORT))
                .map(portValue -> NumberUtils.toInt(portValue))
                .filter(value -> value != 0)
                .collect(Collectors.toList());
        for (int i = Integer.valueOf(DEFAULT_REMOTE_DEBUG_PORT) + 1; i < MAX_PORT; i++) {
            if (!collect.contains(i)) {
                return String.valueOf(i);
            }
        }
        throw new AzureToolkitRuntimeException("Could not found free port to enable remote debug.");
    }
}
