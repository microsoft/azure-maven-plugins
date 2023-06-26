/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.resourcemanager.appcontainers.ContainerAppsApiManager;
import com.azure.resourcemanager.appcontainers.fluent.models.ContainerAppInner;
import com.azure.resourcemanager.appcontainers.models.Configuration;
import com.azure.resourcemanager.appcontainers.models.Container;
import com.azure.resourcemanager.appcontainers.models.Ingress;
import com.azure.resourcemanager.appcontainers.models.Template;
import com.azure.resourcemanager.appcontainers.models.Volume;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.utils.StreamingLogSupport;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.containerapps.model.RevisionMode;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerConsumer;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class ContainerApp extends AbstractAzResource<ContainerApp, AzureContainerAppsServiceSubscription, com.azure.resourcemanager.appcontainers.models.ContainerApp> implements Deletable, StreamingLogSupport, ServiceLinkerConsumer  {
    public static final String LOG_TYPE_CONSOLE = "console";
    public static final String LOG_TYPE_SYSTEM = "system";
    @Getter
    private final RevisionModule revisionModule;
    private Revision latestRevision = null;
    private final ServiceLinkerModule linkerModule;

    protected ContainerApp(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull ContainerAppModule module) {
        super(name, resourceGroupName, module);
        this.revisionModule = new RevisionModule(this);
        this.linkerModule = new ServiceLinkerModule(getId(), this);
    }

    protected ContainerApp(@Nonnull ContainerApp insight) {
        super(insight);
        this.revisionModule = insight.revisionModule;
        this.linkerModule = insight.linkerModule;
        this.latestRevision = insight.latestRevision;
    }

    protected ContainerApp(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp remote, @Nonnull ContainerAppModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.revisionModule = new RevisionModule(this);
        this.linkerModule = new ServiceLinkerModule(getId(), this);
    }

    @Override
    protected void updateAdditionalProperties(@Nullable com.azure.resourcemanager.appcontainers.models.ContainerApp newRemote, @Nullable com.azure.resourcemanager.appcontainers.models.ContainerApp oldRemote) {
        super.updateAdditionalProperties(newRemote, oldRemote);
        this.latestRevision = Optional.ofNullable(newRemote)
                .flatMap(c -> revisionModule.list().stream().filter(r -> Objects.equals(r.getName(), c.latestRevisionName())).findFirst())
                .orElse(null);
    }

    @Override
    public void invalidateCache() {
        super.invalidateCache();
        this.latestRevision = null;
    }

    public RevisionModule revisions() {
        return this.revisionModule;
    }

    @Nullable
    public RevisionMode revisionModel() {
        return Optional.ofNullable(getRemote())
                .map(com.azure.resourcemanager.appcontainers.models.ContainerApp::configuration)
                .map(Configuration::activeRevisionsMode)
                .map(mode -> RevisionMode.fromString(mode.toString()))
                .orElse(null);
    }

    @Nullable
    public IngressConfig getIngressConfig() {
        return Optional.ofNullable(getRemote())
                .map(com.azure.resourcemanager.appcontainers.models.ContainerApp::configuration)
                .map(conf -> IngressConfig.fromIngress(conf.ingress())).orElse(null);
    }

    @Nullable
    public RevisionMode getRevisionMode() {
        return Optional.ofNullable(getRemote())
                .map(com.azure.resourcemanager.appcontainers.models.ContainerApp::configuration)
                .map(Configuration::activeRevisionsMode)
                .map(arm -> RevisionMode.fromString(arm.toString())).orElse(null);
    }

    @Nullable
    public Region getRegion() {
        return Optional.ofNullable(getRemote()).map(remote -> Region.fromName(remote.region().name())).orElse(null);
    }

    public boolean isIngressEnabled() {
        return this.remoteOptional().map(com.azure.resourcemanager.appcontainers.models.ContainerApp::configuration)
            .map(Configuration::ingress).isPresent();
    }

    @Nullable
    public String getIngressFqdn() {
        return this.remoteOptional().map(com.azure.resourcemanager.appcontainers.models.ContainerApp::configuration)
            .map(Configuration::ingress).map(Ingress::fqdn).orElse(null);
    }

    @Nullable
    public ContainerAppsEnvironment getManagedEnvironment() {
        final String managedEnvironmentId = getManagedEnvironmentId();
        return StringUtils.isEmpty(managedEnvironmentId) ? null :
                Azure.az(AzureContainerApps.class).environments(this.getSubscriptionId()).get(managedEnvironmentId);
    }

    @Nullable
    public String getManagedEnvironmentId() {
        return Optional.ofNullable(getRemote()).map(com.azure.resourcemanager.appcontainers.models.ContainerApp::managedEnvironmentId).orElse(null);
    }

    @Nullable
    public String getEnvironmentId() {
        return Optional.ofNullable(getRemote()).map(com.azure.resourcemanager.appcontainers.models.ContainerApp::environmentId).orElse(null);
    }

    @Nullable
    public String getLatestRevisionName() {
        return Optional.ofNullable(getRemote()).map(com.azure.resourcemanager.appcontainers.models.ContainerApp::latestRevisionName).orElse(null);
    }

    @Nullable
    public Revision getLatestRevision() {
        return Optional.ofNullable(getLatestRevisionName())
                .map(name -> this.revisions().get(name, this.getResourceGroupName())).orElse(null);
    }

    @Nullable
    public Revision getCachedLatestRevision() {
        return this.latestRevision;
    }

    public void activate() {
        this.doModify(() -> Objects.requireNonNull(getLatestRevision()).activate(), Status.ACTIVATING);
    }

    public void deactivate() {
        this.doModify(() -> Objects.requireNonNull(getLatestRevision()).deactivate(), Status.DEACTIVATING);
    }

    public void restart() {
        this.doModify(() -> Objects.requireNonNull(getLatestRevision()).restart(), Status.RESTARTING);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Arrays.asList(revisionModule, linkerModule);
    }

    @Nullable
    public String getProvisioningState() {
        return Optional.ofNullable(getRemote()).map(remote -> remote.provisioningState().toString()).orElse(null);
    }

    @Nonnull
    @Override
    protected String loadStatus(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp remote) {
        return remote.provisioningState().toString();
    }

    // refer to https://github.com/microsoft/vscode-azurecontainerapps/main/src/commands/deployImage/deployImage.ts#L111
    public boolean hasUnsupportedFeatures() {
        final Optional<Template> opTemplate = this.remoteOptional()
            .map(com.azure.resourcemanager.appcontainers.models.ContainerApp::innerModel).map(ContainerAppInner::template);
        final List<Container> containers = opTemplate.map(Template::containers).filter(CollectionUtils::isNotEmpty).orElse(null);
        final List<Volume> volumes = opTemplate.map(Template::volumes).orElse(null);
        if (CollectionUtils.isNotEmpty(volumes)) {
            return true;
        } else if (CollectionUtils.isNotEmpty(containers)) {
            if (containers.size() > 1) {
                return true;
            }
            for (final Container container : containers) {
                // NOTE: these are all arrays so if they are empty, this will still return true
                // but these should be undefined if not being utilized
                return CollectionUtils.isNotEmpty(container.probes()) ||
                    CollectionUtils.isNotEmpty(container.volumeMounts()) ||
                    CollectionUtils.isNotEmpty(container.args());
            }
        }
        return false;
    }

    public List<Revision> getRevisions() {
        return revisionModule.list();
    }

    // refer to https://github.com/Azure/azure-cli-extensions/blob/main/src/containerapp/azext_containerapp/custom.py
    public @Nullable String getLogStreamEndpoint() {
        if (!this.exists()) {
            throw new AzureToolkitRuntimeException(AzureString.format("resource ({0}) not found", getName()).toString());
        }
        final String eventStreamEndpoint = Objects.requireNonNull(this.getRemote()).eventStreamEndpoint();
        final String baseUrl = eventStreamEndpoint.substring(0, eventStreamEndpoint.indexOf("/subscriptions/"));
        return String.format("%s/subscriptions/%s/resourceGroups/%s/containerApps/%s/eventstream",
            baseUrl, getSubscriptionId(), getResourceGroupName(), getName());
    }

    @Override
    public String getLogStreamAuthorization() {
        final ContainerAppsApiManager manager = getParent().getRemote();
        final String authToken = Optional.ofNullable(manager).map(m -> m.containerApps().getAuthToken(getResourceGroupName(), getName()).token()).orElse(null);
        return "Bearer " + authToken;
    }

    @Override
    public ServiceLinkerModule getServiceLinkerModule() {
        return linkerModule;
    }
}
