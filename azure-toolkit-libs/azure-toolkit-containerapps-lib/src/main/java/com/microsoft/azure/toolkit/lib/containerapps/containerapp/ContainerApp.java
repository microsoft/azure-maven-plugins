/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.containerapps.containerapp;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.azure.resourcemanager.appcontainers.fluent.models.ContainerAppInner;
import com.azure.resourcemanager.appcontainers.models.*;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Region;
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
import org.apache.http.client.utils.URIBuilder;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.*;

@SuppressWarnings("unused")
public class ContainerApp extends AbstractAzResource<ContainerApp, AzureContainerAppsServiceSubscription, com.azure.resourcemanager.appcontainers.models.ContainerApp> implements Deletable, ServiceLinkerConsumer  {
    public static final String LOG_TYPE_CONSOLE = "console";
    public static final String LOG_TYPE_SYSTEM = "system";
    @Getter
    private final RevisionModule revisionModule;
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
    }

    protected ContainerApp(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp remote, @Nonnull ContainerAppModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.revisionModule = new RevisionModule(this);
        this.linkerModule = new ServiceLinkerModule(getId(), this);
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
    public String loadStatus(@Nonnull com.azure.resourcemanager.appcontainers.models.ContainerApp remote) {
        return remote.provisioningState().toString();
    }

    // refer to https://github.com/microsoft/vscode-azurecontainerapps/main/src/commands/deployImage/deployImage.ts#L111
    public boolean hasUnsupportedFeatures() {
        final Optional<Template> opTemplate = this.remoteOptional(false)
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

    public List<Revision> getRevisionList() {
        return revisionModule.list();
    }

    // refer to https://github.com/Azure/azure-cli-extensions/blob/main/src/containerapp/azext_containerapp/custom.py
    public Flux<String> streamingLogs(String logType, String revisionName, String replicaName, String containerName,
                                      boolean follow, int tailLines) {
        final String endPoint = getLogStreamingEndpoint(logType, revisionName, replicaName, containerName);
        final String basicAuth = "Bearer " + getToken();
        try {
            final URIBuilder uriBuilder = new URIBuilder(endPoint);
            uriBuilder.addParameter("follow", String.valueOf(follow));
            if (tailLines > 0 && tailLines <= 300) {
                uriBuilder.addParameter("tailLines", String.valueOf(tailLines));
            }
            final HttpURLConnection connection = (HttpURLConnection) uriBuilder.build().toURL().openConnection();
            connection.setRequestProperty("Authorization", basicAuth);
            connection.setReadTimeout(600000);
            connection.setConnectTimeout(3000);
            connection.setRequestMethod("GET");
            connection.connect();
            return Flux.create((fluxSink) -> {
                try {
                    final InputStream is = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        fluxSink.next(line);
                    }
                    rd.close();
                } catch (final Exception e) {
                    throw new AzureToolkitRuntimeException(e);
                }
            });
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    @Nullable
    private String getLogStreamingEndpoint(String logType, String revisionName, String replicaName, String containerName) {
        final com.azure.resourcemanager.appcontainers.models.ContainerApp remoteApp = this.getRemote();
        if (Objects.isNull(remoteApp)) {
            return null;
        }
        final String eventStreamEndpoint = remoteApp.eventStreamEndpoint();
        final String baseUrl = eventStreamEndpoint.substring(0, eventStreamEndpoint.indexOf("/subscriptions/"));
        if (Objects.equals(LOG_TYPE_CONSOLE, logType)) {
            return String.format("%s/subscriptions/%s/resourceGroups/%s/containerApps/%s/revisions/%s/replicas/%s/containers/%s/logstream",
                    baseUrl, getSubscriptionId(), getResourceGroupName(), getName(), revisionName, replicaName, containerName);
        } else if (Objects.equals(LOG_TYPE_SYSTEM, logType)) {
            return String.format("%s/subscriptions/%s/resourceGroups/%s/containerApps/%s/eventstream",
                    baseUrl, getSubscriptionId(), getResourceGroupName(), getName());
        }
        return null;
    }

    @Nullable
    private String getToken() {
        final Account account = Azure.az(AzureAccount.class).account();
        final String[] scopes = ScopeUtil.resourceToScopes(account.getEnvironment().getManagementEndpoint());
        final TokenRequestContext request = new TokenRequestContext().addScopes(scopes);
        return Optional.ofNullable(account.getTokenCredential(getSubscriptionId()).getToken(request).block()).map(AccessToken::getToken).orElse(null);
    }

    @Override
    public ServiceLinkerModule getServiceLinkerModule() {
        return linkerModule;
    }
}
