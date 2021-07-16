/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appservice.models.CsmPublishingProfileOptions;
import com.azure.resourcemanager.appservice.models.PublishingProfileFormat;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.arm.resources.ResourceId;
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
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

abstract class AbstractAppService<T extends WebAppBase, R extends AppServiceBaseEntity> implements IAppService<R> {

    @Nonnull
    protected String name;
    @Nonnull
    protected String resourceGroup;
    @Nonnull
    protected String subscriptionId;

    protected AppServiceKuduManager kuduManager;
    protected R entity;
    protected T remote;

    public AbstractAppService(@Nonnull final String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        this.name = resourceId.name();
        this.resourceGroup = resourceId.resourceGroupName();
        this.subscriptionId = resourceId.subscriptionId();
    }

    public AbstractAppService(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        this.name = name;
        this.resourceGroup = resourceGroup;
        this.subscriptionId = subscriptionId;
    }

    public AbstractAppService(@Nonnull WebSiteBase webSiteBase) {
        this.name = webSiteBase.name();
        this.resourceGroup = webSiteBase.resourceGroupName();
        this.subscriptionId = Utils.getSubscriptionId(webSiteBase.id());
    }

    public AbstractAppService(@Nonnull T appService) {
        this((WebAppBasic) appService);
        this.remote = appService;
        this.entity = getEntityFromRemoteResource(remote);
    }

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
    public String id() {
        return entity().getId();
    }

    @Override
    @Nonnull
    public synchronized R entity() {
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
        return getFileClient().getFileContent(path);
    }

    @Override
    public List<? extends AppServiceFile> getFilesInDirectory(String dir) {
        return getFileClient().getFilesInDirectory(dir);
    }

    @Override
    public AppServiceFile getFileByPath(String path) {
        return getFileClient().getFileByPath(path);
    }

    @Override
    public void uploadFileToPath(String content, String path) {
        getFileClient().uploadFileToPath(content, path);
    }

    @Override
    public void createDirectory(String path) {
        getFileClient().createDirectory(path);
    }

    @Override
    public void deleteFile(String path) {
        getFileClient().deleteFile(path);
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
    public InputStream listPublishingProfileXmlWithSecrets() {
        final ResourceId resourceId = ResourceId.fromString(getRemoteResource().id());
        final String resourceName = StringUtils.equals(resourceId.resourceType(), "slots") ?
                String.format("%s/slots/%s", resourceId.parent().name(), resourceId.name()) : resourceId.name();
        final CsmPublishingProfileOptions csmPublishingProfileOptions = new CsmPublishingProfileOptions().withFormat(PublishingProfileFormat.FTP);
        return getRemoteResource().manager().serviceClient().getWebApps()
                .listPublishingProfileXmlWithSecrets(resourceId.resourceGroupName(), resourceName, csmPublishingProfileOptions);
    }

    @Override
    public TunnelStatus getAppServiceTunnelStatus() {
        return getProcessClient().getAppServiceTunnelStatus();
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String subscriptionId() {
        return this.subscriptionId;
    }

    @Override
    public String resourceGroup() {
        return this.resourceGroup;
    }

    protected IFileClient getFileClient() {
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
