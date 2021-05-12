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
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

abstract class AbstractAppService<T extends WebAppBase> implements IAppService {
    @Nullable
    protected abstract T remote();

    @Nonnull
    protected T getRemoteResource() {
        return Objects.requireNonNull(remote(), "Target resource does not exist.");
    }

    @Override
    public void start() {
        getRemoteResource().start();
    }

    @Override
    public void stop() {
        getRemoteResource().stop();
    }

    @Override
    public void restart() {
        getRemoteResource().restart();
    }

    @Override
    public boolean exists() {
        try {
            return remote() != null;
        } catch (ManagementException e) {
            // SDK will throw exception when resource not founded
            return false;
        }
    }

    @Override
    public String hostName() {
        return getRemoteResource().defaultHostname();
    }

    @Override
    public String state() {
        return getRemoteResource().state();
    }

    @Override
    public Runtime getRuntime() {
        return AppServiceUtils.getRuntimeFromWebApp(getRemoteResource());
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return AppServiceUtils.fromPublishingProfile(getRemoteResource().getPublishingProfile());
    }

    @Override
    public DiagnosticConfig getDiagnosticConfig() {
        return AppServiceUtils.fromWebAppDiagnosticLogs(getRemoteResource().diagnosticLogsConfig());
    }

    @Override
    public Flux<String> streamAllLogsAsync() {
        return getRemoteResource().streamAllLogsAsync();
    }

    @Override
    public String id() {
        return getRemoteResource().id();
    }

    @Override
    public String name() {
        return getRemoteResource().name();
    }
}
