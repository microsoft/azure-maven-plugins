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
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

abstract class AbstractAppService<T extends WebAppBase, R extends AppServiceBaseEntity> extends AbstractAzureManager<T> implements IAppService<R>,
        AzureOperationEvent.Source<AbstractAppService<T, R>> {

    protected static final String APP_SERVICE_ID_TEMPLATE = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Web/sites/%s";
    protected AppServiceKuduManager kuduManager;
    protected R entity;

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
    public AbstractAppService<T, R> refresh() {
        super.refresh();
        this.entity = Optional.ofNullable(this.remote).map(this::getEntityFromRemoteResource).orElse(null);
        return this;
    }

    @Override
    public void start() {
        remote().start();
    }

    @Override
    public void stop() {
        remote().stop();
    }

    @Override
    public void restart() {
        remote().restart();
    }

    @Override
    public String id() {
        return String.format(APP_SERVICE_ID_TEMPLATE, subscriptionId, resourceGroup, name);
    }

    @Override
    @Nonnull
    @Deprecated
    public synchronized R entity() {
        if (entity == null) {
            entity = getEntityFromRemoteResource(remote());
        }
        return entity;
    }

    @Override
    @Cacheable(cacheName = "appservice/{}/appSettings", key = "$(this.id())", condition = "!(force&&force[0])") // add cache as each call will send a request
    public Map<String, String> appSettings(boolean... force) {
        return Optional.ofNullable(remote().getAppSettings()).map(Utils::normalizeAppSettings).orElse(Collections.emptyMap());
    }

    @Override
    public Runtime getRuntime() {
        return AppServiceUtils.getRuntimeFromAppService(remote());
    }

    @Override
    public Region region() {
        return Region.fromName(remote().regionName());
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
