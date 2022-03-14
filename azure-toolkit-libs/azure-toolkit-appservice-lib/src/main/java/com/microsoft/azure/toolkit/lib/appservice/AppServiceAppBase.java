/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.resourcemanager.appservice.models.CsmPublishingProfileOptions;
import com.azure.resourcemanager.appservice.models.DeployOptions;
import com.azure.resourcemanager.appservice.models.PublishingProfileFormat;
import com.azure.resourcemanager.appservice.models.SupportsOneDeploy;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceKuduManager;
import com.microsoft.azure.toolkit.lib.appservice.file.IFileClient;
import com.microsoft.azure.toolkit.lib.appservice.file.IProcessClient;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.model.CommandOutput;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.TunnelStatus;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanModule;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.entity.Startable;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class AppServiceAppBase<T extends AppServiceAppBase<T, P, R>, P extends AbstractAzResource<P, ?, ?>, R extends WebAppBase>
    extends AbstractAzResource<T, P, R> implements Startable, Removable {
    protected AppServiceKuduManager kuduManager;

    protected AppServiceAppBase(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        super(name, resourceGroupName, module);
    }

    protected AppServiceAppBase(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, P, R> module) {
        super(name, module);
    }

    protected AppServiceAppBase(@Nonnull T origin) {
        super(origin);
    }

    // MODIFY
    @AzureOperation(name = "appservice.start.app", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void start() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).start(), IAzureBaseResource.Status.STARTING);
    }

    @AzureOperation(name = "appservice.stop.app", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void stop() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).stop(), IAzureBaseResource.Status.STOPPING);
    }

    @AzureOperation(name = "appservice.restart.app", params = {"this.name()"}, type = AzureOperation.Type.SERVICE)
    public void restart() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).restart(), IAzureBaseResource.Status.RESTARTING);
    }

    @Override
    public void remove() {
        this.delete();
    }

    public void deploy(@Nonnull DeployType deployType, @Nonnull File targetFile, @Nullable String targetPath) {
        final R remote = this.getRemote();
        if (remote instanceof SupportsOneDeploy) {
            final DeployOptions options = new DeployOptions().withPath(targetPath);
            AzureMessager.getMessager().info(AzureString.format("Deploying (%s)[%s] %s ...", targetFile.toString(),
                (deployType.toString()), StringUtils.isBlank(targetPath) ? "" : (" to " + (targetPath))));
            final com.azure.resourcemanager.appservice.models.DeployType type =
                com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue());
            this.doModify(() -> Objects.requireNonNull(((SupportsOneDeploy) remote)).deploy(type, targetFile, options), Status.DEPLOYING);
        }
    }

    @Nullable
    public String getHostName() {
        return this.remoteOptional().map(WebSiteBase::defaultHostname).orElse(null);
    }

    @Nullable
    public String getLinuxFxVersion() {
        return this.remoteOptional().map(WebAppBase::linuxFxVersion).orElse(null);
    }

    @Nullable
    public PublishingProfile getPublishingProfile() {
        return this.remoteOptional().map(WebAppBase::getPublishingProfile).map(AppServiceUtils::fromPublishingProfile).orElse(null);
    }

    @Nullable
    public DiagnosticConfig getDiagnosticConfig() {
        return this.remoteOptional().map(WebAppBase::diagnosticLogsConfig).map(AppServiceUtils::fromWebAppDiagnosticLogs).orElse(null);
    }

    @Nonnull
    public Flux<String> streamAllLogsAsync() {
        return this.remoteOptional().map(WebAppBase::streamAllLogsAsync).orElseGet(Flux::empty);
    }

    public Flux<ByteBuffer> getFileContent(String path) {
        return getFileClient().getFileContent(path);
    }

    public List<? extends AppServiceFile> getFilesInDirectory(String dir) {
        return getFileClient().getFilesInDirectory(dir);
    }

    public AppServiceFile getFileByPath(String path) {
        return getFileClient().getFileByPath(path);
    }

    public void uploadFileToPath(String content, String path) {
        getFileClient().uploadFileToPath(content, path);
    }

    public void createDirectory(String path) {
        getFileClient().createDirectory(path);
    }

    public void deleteFile(String path) {
        getFileClient().deleteFile(path);
    }

    public List<ProcessInfo> listProcess() {
        return getProcessClient().listProcess();
    }

    public CommandOutput execute(String command, String dir) {
        return getProcessClient().execute(command, dir);
    }

    public InputStream listPublishingProfileXmlWithSecrets() {
        final ResourceId resourceId = ResourceId.fromString(id());
        final String resourceName = StringUtils.equals(resourceId.resourceType(), "slots") ?
            String.format("%s/slots/%s", resourceId.parent().name(), resourceId.name()) : resourceId.name();
        final CsmPublishingProfileOptions csmPublishingProfileOptions = new CsmPublishingProfileOptions().withFormat(PublishingProfileFormat.FTP);
        return Objects.requireNonNull(getRemote()).manager().serviceClient().getWebApps()
            .listPublishingProfileXmlWithSecrets(resourceId.resourceGroupName(), resourceName, csmPublishingProfileOptions);
    }

    public TunnelStatus getAppServiceTunnelStatus() {
        return getProcessClient().getAppServiceTunnelStatus();
    }

    @Nullable
    public AppServicePlan getAppServicePlan() {
        final AppServicePlanModule plans = Azure.az(AzureAppService.class).plans(this.getSubscriptionId());
        return this.remoteOptional().map(WebSiteBase::appServicePlanId).map(plans::get).orElse(null);
    }

    @Nullable
    public Region getRegion() {
        return this.remoteOptional().map(WebSiteBase::regionName).map(Region::fromName).orElse(null);
    }

    @Nullable
    @Cacheable(cacheName = "appservice/{}/appSettings", key = "${this.getId()}")
    public Map<String, String> getAppSettings() {
        return this.remoteOptional().map(WebAppBase::getAppSettings).map(Utils::normalizeAppSettings).orElse(null);
    }

    @Nullable
    @Cacheable(cacheName = "appservice/{}/runtime", key = "${this.getId()}")
    public Runtime getRuntime() {
        return this.remoteOptional().map(AppServiceUtils::getRuntimeFromAppService).orElse(null);
    }

    @Nonnull
    @Override
    public String loadStatus() {
        return this.remoteOptional().map(WebSiteBase::state).orElse(Status.UNKNOWN);
    }

    @Override
    public String status() {
        return super.getStatus();
    }

    protected IFileClient getFileClient() {
        return getKuduManager();
    }

    protected IProcessClient getProcessClient() {
        return getKuduManager();
    }

    @Nullable
    protected AppServiceKuduManager getKuduManager() {
        if (kuduManager == null) {
            kuduManager = this.remoteOptional().map(r -> AppServiceKuduManager.getClient(r, this)).orElse(null);
        }
        return kuduManager;
    }
}
