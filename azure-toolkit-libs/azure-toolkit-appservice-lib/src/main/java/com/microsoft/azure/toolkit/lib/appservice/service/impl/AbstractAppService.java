/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public abstract class AbstractAppService<T extends WebAppBase> implements IAppService {
    @Nullable
    protected abstract T getRemoteResource();

    @Nonnull
    protected T getNonnullRemoteResource() {
        return Optional.ofNullable(getRemoteResource()).orElseThrow(() -> new AzureToolkitRuntimeException("Target resource does not exist."));
    }

    @Override
    public void start() {
        getNonnullRemoteResource().start();
    }

    @Override
    public void stop() {
        getNonnullRemoteResource().stop();
    }

    @Override
    public void restart() {
        getNonnullRemoteResource().restart();
    }

    @Override
    public boolean exists() {
        try {
            return getRemoteResource() != null;
        } catch (ManagementException e) {
            // SDK will throw exception when resource not founded
            return false;
        }
    }

    @Override
    public String hostName() {
        return getNonnullRemoteResource().defaultHostname();
    }

    @Override
    public String state() {
        return getNonnullRemoteResource().state();
    }

    @Override
    public Runtime getRuntime() {
        return AppServiceUtils.getRuntimeFromWebApp(getNonnullRemoteResource());
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return AppServiceUtils.fromPublishingProfile(getNonnullRemoteResource().getPublishingProfile());
    }

    @Override
    public DiagnosticConfig getDiagnosticConfig() {
        return AppServiceUtils.fromWebAppDiagnosticLogs(getNonnullRemoteResource().diagnosticLogsConfig());
    }

    @Override
    public Flux<String> streamAllLogsAsync() {
        return getNonnullRemoteResource().streamAllLogsAsync();
    }

    @Override
    public String id() {
        return getNonnullRemoteResource().id();
    }

    @Override
    public String name() {
        return getNonnullRemoteResource().name();
    }
}
