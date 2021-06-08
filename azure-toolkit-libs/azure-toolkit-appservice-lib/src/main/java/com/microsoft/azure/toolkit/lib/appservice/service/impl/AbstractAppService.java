/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServiceBaseEntity;
import com.microsoft.azure.toolkit.lib.appservice.manager.AppServiceKuduManager;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.model.CommandOutput;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.TunnelStatus;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.IFileClient;
import com.microsoft.azure.toolkit.lib.appservice.service.IProcessClient;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

abstract class AbstractAppService<T extends WebAppBase, R extends AppServiceBaseEntity> implements IAppService<R> {

    protected AppServiceKuduManager kuduManager;
    protected R entity;
    protected T remote;

    @Override
    public AbstractAppService<T, R> refresh() {
        this.remote = remote();
        this.entity = Optional.ofNullable(this.remote).map(this::getEntityFromRemoteResource)
                .orElseThrow(() -> new AzureToolkitRuntimeException("Target resource does not exist."));
        return this;
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
    public String name() {
        return entity().getName();
    }

    @Override
    public String id() {
        return entity().getId();
    }

    @Override
    @Nonnull
    public R entity() {
        if (remote == null) {
            refresh();
        }
        return entity;
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
        return entity().getRuntime();
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
    public Flux<ByteBuffer> getFileContent(String path) {
        return getFileManager().getFileContent(path);
    }

    @Override
    public List<? extends AppServiceFile> getFilesInDirectory(String dir) {
        return getFileManager().getFilesInDirectory(dir);
    }

    @Override
    public AppServiceFile getFileByPath(String path) {
        return getFileManager().getFileByPath(path);
    }

    @Override
    public void uploadFileToPath(String content, String path) {
        getFileManager().uploadFileToPath(content, path);
    }

    @Override
    public void createDirectory(String path) {
        getFileManager().createDirectory(path);
    }

    @Override
    public void deleteFile(String path) {
        getFileManager().deleteFile(path);
    }

    @Override
    public List<ProcessInfo> listProcess() {
        return getProcessClient().listProcess();
    }

    @Override
    public CommandOutput execute(String command, String dir) {
        return getProcessClient().execute(command, dir);
    }

    @Override
    public TunnelStatus getAppServiceTunnelStatus() {
        return getProcessClient().getAppServiceTunnelStatus();
    }

    protected IFileClient getFileManager() {
        return getKuduManager();
    }

    protected IProcessClient getProcessClient() {
        return getKuduManager();
    }

    protected AppServiceKuduManager getKuduManager() {
        if (kuduManager == null) {
            kuduManager = AppServiceKuduManager.getClient(getRemoteResource(), this);
        }
        return kuduManager;
    }

    @Nonnull
    protected T getRemoteResource() {
        if (remote == null) {
            refresh();
        }
        return Objects.requireNonNull(remote, "Target resource does not exist.");
    }

    @Nonnull
    protected abstract R getEntityFromRemoteResource(@Nonnull T remote);

    @Nullable
    protected abstract T remote();
}
