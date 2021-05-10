/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.manager.AppServiceKuduManager;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.model.CommandOutput;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.TunnelStatus;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

abstract class AbstractAppService<T extends WebAppBase> implements IAppService {

    protected AppServiceKuduManager kuduManager;

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

    @Override
    public Mono<byte[]> getFileContent(String path) {
        return getKuduManager().getFileContent(path);
    }

    @Override
    public List<? extends AppServiceFile> getFilesInDirectory(String dir) {
        return getKuduManager().getFilesInDirectory(dir);
    }

    @Override
    public AppServiceFile getFileByPath(String path) {
        return getKuduManager().getFileByPath(path);
    }

    @Override
    public void uploadFileToPath(String content, String path) {
        getKuduManager().uploadFileToPath(content, path);
    }

    @Override
    public void createDirectory(String path) {
        getKuduManager().createDirectory(path);
    }

    @Override
    public void deleteFile(String path) {
        getKuduManager().deleteFile(path);
    }

    @Override
    public List<ProcessInfo> listProcess() {
        return getKuduManager().listProcess();
    }

    @Override
    public CommandOutput execute(String command, String dir) {
        return getKuduManager().execute(command, dir);
    }

    @Override
    public TunnelStatus getAppServiceTunnelStatus() {
        return getKuduManager().getAppServiceTunnelStatus();
    }

    protected AppServiceKuduManager getKuduManager() {
        if (kuduManager == null) {
            kuduManager = AppServiceKuduManager.getClient(getRemoteResource(), this);
        }
        return kuduManager;
    }

    @Nonnull
    protected T getRemoteResource() {
        return Objects.requireNonNull(remote(), "Target resource does not exist.");
    }

    @Nullable
    protected abstract T remote();
}
