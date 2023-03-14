/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.models.SupportsOneDeploy;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.deploy.IOneDeploy;
import com.microsoft.azure.toolkit.lib.appservice.model.*;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public abstract class WebAppBase<T extends WebAppBase<T, P, F>, P extends AbstractAzResource<P, ?, ?>, F extends com.azure.resourcemanager.appservice.models.WebAppBase>
    extends AppServiceAppBase<T, P, F> implements IOneDeploy {

    private AtomicReference<KuduDeploymentResult> deploymentResultAtomicReference = new AtomicReference<>();;

    protected WebAppBase(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, WebSiteBase> module) {
        super(name, resourceGroupName, module);
    }

    protected WebAppBase(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, P, WebSiteBase> module) {
        super(name, module);
    }

    protected WebAppBase(@Nonnull T origin) {
        super(origin);
    }

    @Override
    public void deploy(@Nonnull DeployType deployType, @Nonnull File targetFile,
                       @Nullable DeployOptions deployOptions) {
        final WebSiteBase remote = this.getRemote();
        if (remote instanceof SupportsOneDeploy) {
            final com.azure.resourcemanager.appservice.models.DeployOptions options =
                    deployOptions == null ? null : AppServiceUtils.toDeployOptions(deployOptions);
            AzureMessager.getMessager().info(AzureString.format("Deploying (%s)[%s] %s ...", targetFile.toString(),
                    (deployType.toString()), StringUtils.isBlank(deployOptions.getPath()) ? "" : (" to " + (deployOptions.getPath()))));
            final com.azure.resourcemanager.appservice.models.DeployType type =
                    com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue());
            this.doModify(() -> Objects.requireNonNull(((SupportsOneDeploy) remote)).deploy(type, targetFile, options), Status.DEPLOYING);
        }
    }

    @Override
    @Nullable
    public void pushDeploy(@Nonnull DeployType deployType, @Nonnull File targetFile,
                                           @Nullable DeployOptions deployOptions) {
        final WebSiteBase remote = this.getRemote();
        if (remote instanceof SupportsOneDeploy) {
            final com.azure.resourcemanager.appservice.models.DeployOptions options =
                    deployOptions == null ? null : AppServiceUtils.toDeployOptions(deployOptions);
            AzureMessager.getMessager().info(AzureString.format("Deploying (%s)[%s] %s ...", targetFile.toString(),
                    (deployType.toString()), StringUtils.isBlank(deployOptions.getPath()) ? "" : (" to " + (deployOptions.getPath()))));
            final com.azure.resourcemanager.appservice.models.DeployType type =
                    com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue());
            deploymentResultAtomicReference.set(AppServiceUtils.fromKuduDeploymentResult(((SupportsOneDeploy) remote).pushDeploy(type, targetFile, options)));
        }
    }

    @Override
    @Nullable
    public CsmDeploymentStatus getDeploymentStatus(@Nonnull final String deploymentId) {
        final WebSiteBase remote = this.getFullRemote();
        if (remote instanceof SupportsOneDeploy) {
            return AppServiceUtils.fromCsmDeploymentStatus(((SupportsOneDeploy) remote).getDeploymentStatus(deploymentId));
        } else {
            return null;
        }
    }

    @Override
    public boolean isEnableWebServerLogging() {
        return Optional.ofNullable(getDiagnosticConfig()).map(DiagnosticConfig::isEnableWebServerLogging).orElse(false);
    }

    public boolean waitUntilReady(int deploymentStatusRefreshInterval, int deploymentStatusMaxRefreshTimes) {
        if (isWaitDeploymentComplete()) {
            return waitUntilDeploymentReady(deploymentStatusRefreshInterval, deploymentStatusMaxRefreshTimes);
        }
        return false;
    }

    public boolean waitUntilDeploymentReady(int deploymentStatusRefreshInterval, int deploymentStatusMaxRefreshTimes) {
        final KuduDeploymentResult kuduDeploymentResult = deploymentResultAtomicReference.get();
        if (kuduDeploymentResult == null) {
            return false;
        }
        final CsmDeploymentStatus status = Mono.fromCallable(this::getDeploymentStatus)
                .delayElement(Duration.ofSeconds(deploymentStatusRefreshInterval))
                .subscribeOn(Schedulers.boundedElastic())
                .repeat(deploymentStatusMaxRefreshTimes)
                .takeUntil(csmDeploymentStatus -> !csmDeploymentStatus.getStatus().isRunning())
                .blockLast();
        final DeploymentBuildStatus buildStatus = status.getStatus();
        if (buildStatus.isSucceed()) {
            return true;
        }
        if (buildStatus.isTimeout()) {
            AzureMessager.getMessager().warning("Resource deployed, but failed to get the deployment status as timeout");
        } else if (buildStatus.isRunning()) {
            AzureMessager.getMessager().warning("Resource deployed, but the deployment is still in process in Azure");
        } else if (buildStatus.isFailed()) {
            final String errorMessages = CollectionUtils.isNotEmpty(status.getErrors()) ?
                    status.getErrors().stream().map(ErrorEntity::getMessage).collect(Collectors.joining(StringUtils.LF)) : StringUtils.EMPTY;
            final String failedInstancesLogs = CollectionUtils.isEmpty(status.getFailedInstancesLogs()) ?
                    StringUtils.join(status.getFailedInstancesLogs(), StringUtils.LF) : StringUtils.EMPTY;
            throw new AzureToolkitRuntimeException(String.format("Failed to start app %s. %s %s", this.getName(), errorMessages, failedInstancesLogs));
        }
        return false;
    }

    private boolean isWaitDeploymentComplete() {
        if (this.getFormalStatus().isStopped()) {
            AzureMessager.getMessager().info("Skip waiting deployment status for stopped web app.");
            return false;
        }
        if (this.getRuntime().isWindows()) {
            AzureMessager.getMessager().warning("`waitDeploymentComplete` is not supported in Windows runtime, skip waiting for deployment status.");
            return false;
        }
        return this.getRuntime().isLinux();
    }

    private @Nullable CsmDeploymentStatus getDeploymentStatus() {
        final CsmDeploymentStatus deploymentStatus = this.getDeploymentStatus(deploymentResultAtomicReference.get().getDeploymentId());
        if (Objects.isNull(deploymentStatus)) {
            return null;
        }
        final String statusMessage = String.format("Deployment Status: %s; Successful Instance Count: %s; In-progress Instance Count: %s; Failed Instance Count: %s",
                deploymentStatus.getStatus().getValue(), deploymentStatus.getNumberOfInstancesSuccessful(), deploymentStatus.getNumberOfInstancesInProgress(), deploymentStatus.getNumberOfInstancesFailed());
        AzureMessager.getMessager().debug(statusMessage);
        return deploymentStatus;
    }

}
