/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.fluent.models.HostKeysInner;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlot;
import com.azure.resourcemanager.appservice.models.FunctionDeploymentSlotBasic;
import com.azure.resourcemanager.appservice.models.PlatformArchitecture;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FunctionAppDeploymentSlot extends FunctionAppBase<FunctionAppDeploymentSlot, FunctionApp, FunctionDeploymentSlot> {

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
    @AzureOperation(name = "function.enable_remote_debugging_in_azure.slot", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public void enableRemoteDebug() {
        final Map<String, String> appSettings = this.getAppSettings();
        getFullRemote().update()
                .withWebSocketsEnabled(true)
                .withPlatformArchitecture(PlatformArchitecture.X64)
                .withAppSetting(HTTP_PLATFORM_DEBUG_PORT, appSettings.getOrDefault(HTTP_PLATFORM_DEBUG_PORT, DEFAULT_REMOTE_DEBUG_PORT))
                .withAppSetting(JAVA_OPTS, getJavaOptsWithRemoteDebugEnabled(appSettings)).apply();
    }

    @Override
    @AzureOperation(name = "function.disable_remote_debugging_in_azure.slot", params = {"this.getName()"}, type = AzureOperation.Type.REQUEST)
    public void disableRemoteDebug() {
        final Map<String, String> appSettings = this.getAppSettings();
        final String javaOpts = this.getJavaOptsWithRemoteDebugDisabled(appSettings);
        if (StringUtils.isEmpty(javaOpts)) {
            getFullRemote().update().withoutAppSetting(HTTP_PLATFORM_DEBUG_PORT).withoutAppSetting(JAVA_OPTS).apply();
        } else {
            getFullRemote().update().withoutAppSetting(HTTP_PLATFORM_DEBUG_PORT).withAppSetting(JAVA_OPTS, javaOpts).apply();
        }
    }
}
