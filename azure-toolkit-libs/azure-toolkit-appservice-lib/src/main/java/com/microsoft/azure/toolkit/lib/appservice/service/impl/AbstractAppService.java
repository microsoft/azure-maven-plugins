/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.service.impl;

import com.azure.resourcemanager.appservice.models.CsmPublishingProfileOptions;
import com.azure.resourcemanager.appservice.models.PublishingProfileFormat;
import com.azure.resourcemanager.appservice.models.WebAppBase;
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
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

abstract class AbstractAppService<T extends WebAppBase, R extends AppServiceBaseEntity> extends AbstractAzureManager<T> implements IAppService<R>,
    AzureOperationEvent.Source<AbstractAppService<T, R>> {

    protected static final String APP_SERVICE_ID_TEMPLATE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Web/sites/%s";
    protected AppServiceKuduManager kuduManager;
    protected R entity;
    protected String status = null;

    public AbstractAppService(@Nonnull final String id) {
        super(id);
    }

    public AbstractAppService(@Nonnull final String subscriptionId, @Nonnull final String resourceGroup, @Nonnull final String name) {
        super(subscriptionId, resourceGroup, name);
    }

    public AbstractAppService(@Nonnull WebSiteBase webSiteBase) {
        super(webSiteBase.id());
    }

    public AbstractAppService(@Nonnull T appService) {
        super(appService);
        this.entity = getEntityFromRemoteResource(remote);
    }

    @Override
    @AzureOperation(name = "common.refresh_resource", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public AbstractAppService<T, R> refresh() {
        this.status(Status.PENDING);
        super.refresh();
        this.entity = Optional.ofNullable(this.remote).map(this::getEntityFromRemoteResource).orElse(null);
        this.refreshStatus();
        return this;
    }

    @Override
    public void start() {
        this.status(Status.PENDING);
        remote().start();
        super.refresh(); // workaround as web app manager in sdk will not update status with start/stop/restart
        this.refreshStatus();
    }

    @Override
    public void stop() {
        this.status(Status.PENDING);
        remote().stop();
        super.refresh(); // workaround as web app manager in sdk will not update status with start/stop/restart
        this.refreshStatus();
    }

    @Override
    public void restart() {
        this.status(Status.PENDING);
        remote().restart();
        super.refresh(); // workaround as web app manager in sdk will not update status with start/stop/restart
        this.refreshStatus();
    }

    @Override
    public String id() {
        return String.format(APP_SERVICE_ID_TEMPLATE, subscriptionId, resourceGroup, name);
    }

    @Override
    @Nonnull
    public synchronized R entity() {
        if (entity == null) {
            entity = getEntityFromRemoteResource(remote());
        }
        return entity;
    }

    @Override
    public String hostName() {
        return remote().defaultHostname();
    }

    @Override
    public String state() {
        return remote().state();
    }

    @Override
    public Runtime getRuntime() {
        return entity().getRuntime();
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return AppServiceUtils.fromPublishingProfile(remote().getPublishingProfile());
    }

    @Override
    public DiagnosticConfig getDiagnosticConfig() {
        return AppServiceUtils.fromWebAppDiagnosticLogs(remote().diagnosticLogsConfig());
    }

    @Override
    public Flux<String> streamAllLogsAsync() {
        return remote().streamAllLogsAsync();
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
        final ResourceId resourceId = ResourceId.fromString(id());
        final String resourceName = StringUtils.equals(resourceId.resourceType(), "slots") ?
                String.format("%s/slots/%s", resourceId.parent().name(), resourceId.name()) : resourceId.name();
        final CsmPublishingProfileOptions csmPublishingProfileOptions = new CsmPublishingProfileOptions().withFormat(PublishingProfileFormat.FTP);
        return remote().manager().serviceClient().getWebApps()
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

    @Override
    public String status() {
        if (Objects.nonNull(this.status)) {
            return this.status;
        } else {
            this.refreshStatus();
            return Status.LOADING;
        }
    }

    public final void refreshStatus() {
        AzureTaskManager.getInstance().runOnPooledThread(() -> this.status(this.loadStatus()));
    }

    protected final void status(@Nonnull String status) {
        final String oldStatus = this.status;
        this.status = status;
        if (!StringUtils.equalsIgnoreCase(oldStatus, this.status)) {
            AzureEventBus.emit("common|resource.status_changed", this);
        }
    }

    protected String loadStatus() {
        if (!exists()) {
            return Status.UNKNOWN;
        }
        final String state = Optional.ofNullable(remote().state()).map(Objects::toString).orElse(StringUtils.EMPTY);
        return Status.status.stream().filter(status -> StringUtils.equalsIgnoreCase(status, state)).findFirst().orElse(Status.UNKNOWN);
    }

    protected IFileClient getFileClient() {
        return getKuduManager();
    }

    protected IProcessClient getProcessClient() {
        return getKuduManager();
    }

    protected AppServiceKuduManager getKuduManager() {
        if (kuduManager == null) {
            kuduManager = AppServiceKuduManager.getClient(remote(), this);
        }
        return kuduManager;
    }

    @Nonnull
    protected abstract R getEntityFromRemoteResource(@Nonnull T remote);

    @Nullable
    protected abstract T loadRemote();
}
