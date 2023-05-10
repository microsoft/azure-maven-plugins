/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.util.ExpandableStringEnum;
import com.azure.resourcemanager.appplatform.AppPlatformManager;
import com.azure.resourcemanager.appplatform.fluent.AppPlatformManagementClient;
import com.azure.resourcemanager.appplatform.fluent.models.RemoteDebuggingInner;
import com.azure.resourcemanager.appplatform.models.DeploymentSettings;
import com.azure.resourcemanager.appplatform.models.RemoteDebuggingPayload;
import com.azure.resourcemanager.appplatform.models.SpringAppDeployment;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerConsumer;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class SpringCloudDeployment extends AbstractAzResource<SpringCloudDeployment, SpringCloudApp, SpringAppDeployment>
        implements ServiceLinkerConsumer {
    @Nonnull
    private final SpringCloudAppInstanceModule instanceModule;
    private final ServiceLinkerModule linkerModule;
    private boolean remoteDebuggingEnabled;

    protected SpringCloudDeployment(@Nonnull String name, @Nonnull SpringCloudDeploymentModule module) {
        super(name, module);
        this.instanceModule = new SpringCloudAppInstanceModule(this);
        this.linkerModule = new ServiceLinkerModule(getId(), this);
    }

    /**
     * copy constructor
     */
    protected SpringCloudDeployment(@Nonnull SpringCloudDeployment origin) {
        super(origin);
        this.instanceModule = origin.instanceModule;
        this.remoteDebuggingEnabled = origin.remoteDebuggingEnabled;
        this.linkerModule = origin.linkerModule;
    }

    protected SpringCloudDeployment(@Nonnull SpringAppDeployment remote, @Nonnull SpringCloudDeploymentModule module) {
        super(remote.name(), module);
        this.instanceModule = new SpringCloudAppInstanceModule(this);
        this.linkerModule = new ServiceLinkerModule(getId(), this);
    }

    // MODIFY
    @AzureOperation(name = "azure/resource.start_resource.resource", params = {"this.name()"})
    public void start() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).start(), Status.STARTING);
    }

    @AzureOperation(name = "azure/resource.stop_resource.resource", params = {"this.name()"})
    public void stop() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).stop(), Status.STOPPING);
    }

    @AzureOperation(name = "azure/resource.restart_resource.resource", params = {"this.name()"})
    public void restart() {
        this.doModify(() -> Objects.requireNonNull(this.getRemote()).restart(), Status.RESTARTING);
    }

    @Nonnull
    @Override
    public String loadStatus(@Nonnull SpringAppDeployment remote) {
        return Optional.of(remote)
            .map(SpringAppDeployment::status)
            .map(ExpandableStringEnum::toString)
            .orElse(Status.UNKNOWN);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Arrays.asList(instanceModule, linkerModule);
    }

    @Override
    protected void updateAdditionalProperties(SpringAppDeployment newRemote, SpringAppDeployment oldRemote) {
        final SpringCloudApp app = this.getParent();
        final SpringCloudCluster cluster = app.getParent();
        if (Objects.nonNull(newRemote) && isRemoteDebuggingSupported()) {
            this.remoteDebuggingEnabled = Optional.ofNullable(cluster.getRemote())
                .map(HasManager::manager)
                .map(AppPlatformManager::serviceClient)
                .map(AppPlatformManagementClient::getDeployments)
                .map(c -> c.getRemoteDebuggingConfig(this.getResourceGroupName(), cluster.getName(), app.getName(), this.getName()))
                .map(RemoteDebuggingInner::enabled).orElse(false);
        } else {
            this.remoteDebuggingEnabled = false;
        }
    }

    @AzureOperation(name = "internal/springcloud.wait_until_deployment_ready.deployment|app", params = {"this.getName()", "this.getParent().getName()"})
    public boolean waitUntilReady(int timeoutInSeconds) {
        final SpringCloudDeployment deployment = Utils.pollUntil(() -> {
            this.invalidateCache();
            return this;
        }, Utils::isDeploymentDone, timeoutInSeconds);
        return Utils.isDeploymentDone(deployment);
    }

    @Nullable
    public Double getCpu() {
        return Optional.ofNullable(this.getRemote())
            .map(SpringAppDeployment::cpu)
            .orElse(null);
    }

    @Nullable
    public Double getMemoryInGB() {
        return Optional.ofNullable(this.getRemote())
            .map(SpringAppDeployment::memoryInGB)
            .orElse(null);
    }

    @Nullable
    public String getRuntimeVersion() {
        return Optional.ofNullable(this.getRemote())
            .map(SpringAppDeployment::runtimeVersion)
            .map(ExpandableStringEnum::toString)
            .orElse(null);
    }

    @Nullable
    public String getJvmOptions() {
        return Optional.ofNullable(this.getRemote())
            .map(SpringAppDeployment::jvmOptions)
            .orElse(null);
    }

    @Nullable
    public Map<String, String> getEnvironmentVariables() {
        return Optional.ofNullable(this.getRemote())
            .map(SpringAppDeployment::settings)
            .map(DeploymentSettings::environmentVariables)
            .map(v -> {
                final HashMap<String, String> variables = new HashMap<>(v);
                if (this.getParent().getParent().isEnterpriseTier() && StringUtils.isBlank(variables.get("JAVA_OPTS"))) {
                    // jvmOptions are part of environment variables in enterprise tier.
                    // refer to `com.azure.resourcemanager.appplatform.implementation.SpringAppDeploymentImpl.jvmOptions`
                    variables.remove("JAVA_OPTS");
                }
                return variables;
            }).orElse(null);
    }

    public List<SpringCloudAppInstance> getInstances() {
        return this.instanceModule.list();
    }

    @Nullable
    public Integer getInstanceNum() {
        return this.remoteOptional().map(SpringAppDeployment::instances).map(List::size).orElse(null);
    }

    @Nonnull
    public Boolean isActive() {
        return Optional.ofNullable(this.getRemote()).map(SpringAppDeployment::isActive).orElse(false);
    }

    @Override
    public void setStatus(@Nonnull String status) {
        super.setStatus(status);
        // update app status when active deployment status changed
        if (this.isActive()) {
            getParent().reloadStatus();
        }
    }

    @AzureOperation(name = "azure/springcloud.enable_remote_debugging.deployment", params = {"this.getName()"})
    public void enableRemoteDebugging(int port) {
        this.validateRemoteDebuggingSupport();
        final SpringCloudApp app = this.getParent();
        final SpringCloudCluster cluster = app.getParent();
        final RemoteDebuggingPayload payload = new RemoteDebuggingPayload().withPort(port);
        Optional.ofNullable(cluster.getRemote())
            .map(HasManager::manager)
            .map(AppPlatformManager::serviceClient)
            .map(AppPlatformManagementClient::getDeployments)
            .ifPresent(c -> doModify(() -> c.enableRemoteDebuggingAsync(this.getResourceGroupName(), cluster.getName(), app.getName(), this.getName(), payload).block(), Status.UPDATING));
    }

    @AzureOperation(name = "azure/springcloud.disable_remote_debugging.deployment", params = {"this.getName()"})
    public void disableRemoteDebugging() {
        final SpringCloudApp app = this.getParent();
        final SpringCloudCluster cluster = app.getParent();
        Optional.ofNullable(cluster.getRemote())
            .map(HasManager::manager)
            .map(AppPlatformManager::serviceClient)
            .map(AppPlatformManagementClient::getDeployments)
            .ifPresent(c -> doModify(() -> c.disableRemoteDebugging(this.getResourceGroupName(), cluster.getName(), app.getName(), this.getName()), Status.UPDATING));
    }

    public boolean isRemoteDebuggingEnabled() {
        return this.remoteDebuggingEnabled;
    }

    public int getRemoteDebuggingPort() {
        this.validateRemoteDebuggingSupport();
        final SpringCloudApp app = this.getParent();
        final SpringCloudCluster cluster = app.getParent();
        return Optional.ofNullable(cluster.getRemote())
            .map(HasManager::manager)
            .map(AppPlatformManager::serviceClient)
            .map(AppPlatformManagementClient::getDeployments)
            .map(c -> c.getRemoteDebuggingConfig(this.getResourceGroupName(), cluster.getName(), app.getName(), this.getName()))
            .map(RemoteDebuggingInner::port)
            .orElseThrow(() -> new AzureToolkitRuntimeException("Failed to get remote debugging port."));
    }

    private void validateRemoteDebuggingSupport() {
        if (!isRemoteDebuggingSupported()) {
            throw new AzureToolkitRuntimeException("Remote debugging is not supported for Azure Spring apps of consumption plan for now.");
        }
    }

    private boolean isRemoteDebuggingSupported() {
        return !this.getParent().getParent().isConsumptionTier();
    }

    @Nullable
    public SpringCloudAppInstance getLatestInstance() {
        return getInstances().stream().filter(springCloudAppInstance -> Objects.nonNull(springCloudAppInstance.getRemote()))
            .max(Comparator.comparing(instance -> Objects.requireNonNull(instance.getRemote()).startTime())).orElse(null);
    }

    @Override
    public ServiceLinkerModule getServiceLinkerModule() {
        return linkerModule;
    }
}
