/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.azure.resourcemanager.appservice.models.CsmPublishingProfileOptions;
import com.azure.resourcemanager.appservice.models.HostType;
import com.azure.resourcemanager.appservice.models.HostnameSslState;
import com.azure.resourcemanager.appservice.models.PublishingProfileFormat;
import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceKuduClient;
import com.microsoft.azure.toolkit.lib.appservice.file.IFileClient;
import com.microsoft.azure.toolkit.lib.appservice.file.IProcessClient;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.model.CommandOutput;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.ProcessInfo;
import com.microsoft.azure.toolkit.lib.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.TunnelStatus;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanModule;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Startable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.StreamingLogSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
@Slf4j
public abstract class AppServiceAppBase<
    T extends AppServiceAppBase<T, P, F>,
    P extends AbstractAzResource<P, ?, ?>,
    F extends WebAppBase>
    extends AbstractAzResource<T, P, F> implements Startable, Deletable, StreamingLogSupport {
    public static final String SETTING_DOCKER_IMAGE = "DOCKER_CUSTOM_IMAGE_NAME";
    public static final String SETTING_REGISTRY_SERVER = "DOCKER_REGISTRY_SERVER_URL";

    protected AppServiceKuduClient kuduManager;
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> OPEN_IN_BROWSER = Action.Id.of("user/webapp.open_in_browser.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> START_STREAM_LOG = Action.Id.of("user/$appservice.open_log_stream.app");

    protected AppServiceAppBase(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, F> module) {
        super(name, resourceGroupName, module);
    }

    protected AppServiceAppBase(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, P, F> module) {
        super(name, module);
    }

    /**
     * copy constructor
     */
    protected AppServiceAppBase(@Nonnull T origin) {
        super(origin);
        this.kuduManager = origin.kuduManager;
    }

    // MODIFY
    @AzureOperation(name = "azure/$appservice.start_app.name", params = {"this.getName()"})
    public void start() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).start(), AzResource.Status.STARTING);
    }

    @AzureOperation(name = "azure/$appservice.stop_app.name", params = {"this.getName()"})
    public void stop() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).stop(), AzResource.Status.STOPPING);
    }

    @AzureOperation(name = "azure/$appservice.restart_app.name", params = {"this.getName()"})
    public void restart() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).restart(), AzResource.Status.RESTARTING);
    }

    @Nullable
    public String getHostName() {
        return this.remoteOptional().map(WebSiteBase::defaultHostname).orElse(null);
    }

    @Nullable
    public String getKuduHostName() {
        return this.remoteOptional().flatMap(siteBase -> siteBase.hostnameSslStates().values().stream()
                .filter(sslState -> sslState.hostType() == HostType.REPOSITORY)
                .map(HostnameSslState::name)
                .findFirst())
                .orElse(null);
    }

    @Nullable
    public String getLinuxFxVersion() {
        return Optional.ofNullable(this.getRemote()).map(WebAppBase::linuxFxVersion).orElse(null);
    }

    @Nullable
    public PublishingProfile getPublishingProfile() {
        return Optional.ofNullable(this.getRemote()).map(WebAppBase::getPublishingProfile).map(AppServiceUtils::fromPublishingProfile).orElse(null);
    }

    @Nullable
    public DiagnosticConfig getDiagnosticConfig() {
        return Optional.ofNullable(this.getRemote()).map(WebAppBase::diagnosticLogsConfig).map(AppServiceUtils::fromWebAppDiagnosticLogs).orElse(null);
    }

    @Override
    public Flux<String> streamingLogs(boolean follow, @Nonnull Map<String, String> params) {
        return Optional.ofNullable(this.getRemote()).map(WebAppBase::streamAllLogsAsync).orElseGet(Flux::empty);
    }

    @Nonnull
    public Flux<ByteBuffer> getFileContent(String path) {
        return Optional.ofNullable(getFileClient()).map(c -> c.getFileContent(path)).orElseGet(Flux::empty);
    }

    @Nonnull
    public List<? extends AppServiceFile> getFilesInDirectory(String dir) {
        return Optional.ofNullable(getFileClient()).map(c -> c.getFilesInDirectory(dir)).orElseGet(Collections::emptyList);
    }

    @Nullable
    public AppServiceFile getFileByPath(String path) {
        return Optional.ofNullable(getFileClient()).map(c -> c.getFileByPath(path)).orElse(null);
    }

    public void uploadFileToPath(String content, String path) {
        Optional.ofNullable(getFileClient()).ifPresent(c -> c.uploadFileToPath(content, path));
    }

    public void createDirectory(String path) {
        Optional.ofNullable(getFileClient()).ifPresent(c -> c.createDirectory(path));
    }

    public void deleteFile(String path) {
        Optional.ofNullable(getFileClient()).ifPresent(c -> c.deleteFile(path));
    }

    @Nonnull
    public List<ProcessInfo> listProcess() {
        return Optional.ofNullable(getProcessClient()).map(IProcessClient::listProcess).orElseGet(Collections::emptyList);
    }

    @Nullable
    public CommandOutput execute(String command, String dir) {
        return Optional.ofNullable(getProcessClient()).map(c -> c.execute(command, dir)).orElse(null);
    }

    public InputStream listPublishingProfileXmlWithSecrets() {
        final ResourceId resourceId = ResourceId.fromString(getId());
        final String resourceName = StringUtils.equals(resourceId.resourceType(), "slots") ?
            String.format("%s/slots/%s", resourceId.parent().name(), resourceId.name()) : resourceId.name();
        final CsmPublishingProfileOptions csmPublishingProfileOptions = new CsmPublishingProfileOptions().withFormat(PublishingProfileFormat.FTP);
        return Objects.requireNonNull(getRemote()).manager().serviceClient().getWebApps()
            .listPublishingProfileXmlWithSecrets(resourceId.resourceGroupName(), resourceName, csmPublishingProfileOptions).toStream();
    }

    @Nullable
    public TunnelStatus getAppServiceTunnelStatus() {
        return Optional.ofNullable(getProcessClient()).map(IProcessClient::getAppServiceTunnelStatus).orElse(null);
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
    public Map<String, String> getAppSettings() {
        return Optional.ofNullable(this.getRemote()).map(WebAppBase::getAppSettings).map(Utils::normalizeAppSettings).orElse(null);
    }

    @Nullable
    public abstract Runtime getRuntime();

    @Nullable
    public RuntimeConfig getDockerRuntimeConfig() {
        final String linuxFxVersion = getLinuxFxVersion();
        if (!StringUtils.startsWithIgnoreCase(linuxFxVersion, "docker")) {
            return null;
        }
        final RuntimeConfig runtimeConfig = new RuntimeConfig();
        runtimeConfig.os(OperatingSystem.DOCKER);
        final Map<String, String> settings = Optional.ofNullable(getAppSettings()).orElse(Collections.emptyMap());
        final String imageSetting = Optional.ofNullable(settings.get(SETTING_DOCKER_IMAGE)).filter(StringUtils::isNotBlank)
            .orElseGet(() -> Utils.getDockerImageNameFromLinuxFxVersion(Objects.requireNonNull(linuxFxVersion)));
        final String registryServerSetting = Optional.ofNullable(settings.get(SETTING_REGISTRY_SERVER))
            .filter(StringUtils::isNotBlank).orElse(null);
        return runtimeConfig.image(imageSetting).registryUrl(registryServerSetting);
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull WebAppBase remote) {
        return remote.state();
    }

    @Nullable
    protected IFileClient getFileClient() {
        return getKuduManager();
    }

    @Nullable
    protected IProcessClient getProcessClient() {
        return getKuduManager();
    }

    @Nullable
    public AppServiceKuduClient getKuduManager() {
        if (kuduManager == null) {
            kuduManager = Optional.ofNullable(this.getRemote()).map(r -> AppServiceKuduClient.getClient(r, this)).orElse(null);
        }
        return kuduManager;
    }

    public boolean isStreamingLogSupported() {
        return false;
    }

    @Override
    protected void setRemote(F remote) {
        super.setRemote(remote);
    }
}
