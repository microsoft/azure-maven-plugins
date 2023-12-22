/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.models.JavaVersion;
import com.azure.resourcemanager.appservice.models.OperatingSystem;
import com.azure.resourcemanager.appservice.models.SupportsOneDeploy;
import com.azure.resourcemanager.appservice.models.WebSiteBase;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.deploy.IOneDeploy;
import com.microsoft.azure.toolkit.lib.appservice.model.*;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Objects;
import java.util.Optional;

public abstract class WebAppBase<T extends WebAppBase<T, P, F>, P extends AbstractAzResource<P, ?, ?>, F extends com.azure.resourcemanager.appservice.models.WebAppBase>
    extends AppServiceAppBase<T, P, F> implements IOneDeploy {

    protected WebAppBase(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull AbstractAzResourceModule<T, P, F> module) {
        super(name, resourceGroupName, module);
    }

    protected WebAppBase(@Nonnull String name, @Nonnull AbstractAzResourceModule<T, P, F> module) {
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
    public KuduDeploymentResult pushDeploy(@Nonnull DeployType deployType, @Nonnull File targetFile,
                                           @Nullable DeployOptions deployOptions) {
        final WebSiteBase remote = this.getRemote();
        if (remote instanceof SupportsOneDeploy) {
            final com.azure.resourcemanager.appservice.models.DeployOptions options =
                    deployOptions == null ? null : AppServiceUtils.toDeployOptions(deployOptions);
            AzureMessager.getMessager().info(AzureString.format("Deploying (%s)[%s] %s ...", targetFile.toString(),
                    (deployType.toString()), StringUtils.isBlank(deployOptions.getPath()) ? "" : (" to " + (deployOptions.getPath()))));
            final com.azure.resourcemanager.appservice.models.DeployType type =
                    com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue());
            return AppServiceUtils.fromKuduDeploymentResult(((SupportsOneDeploy) remote).pushDeploy(type, targetFile, options));
        } else {
            return null;
        }
    }

    @Override
    public WebAppRuntime getRuntime() {
        return this.remoteOptional().map(r -> {
            if (r.operatingSystem() == OperatingSystem.WINDOWS) {
                final String container = r.javaContainer();
                final String containerVersion = r.javaContainerVersion();
                final JavaVersion javaVersion = r.javaVersion();
                return WebAppWindowsRuntime.fromContainerAndJavaVersion(container, containerVersion, javaVersion);
            } else {
                final String fxString = r.linuxFxVersion();
                if (StringUtils.isEmpty(fxString)) {
                    return null;
                } else if (StringUtils.startsWithIgnoreCase(fxString, "docker")) {
                    return WebAppDockerRuntime.INSTANCE;
                }
                return WebAppLinuxRuntime.fromFxString(fxString);
            }
        }).orElse(null);
    }

    @Override
    @Nullable
    public CsmDeploymentStatus getDeploymentStatus(@Nonnull final String deploymentId) {
        final WebSiteBase remote = this.getRemote();
        if (remote instanceof SupportsOneDeploy) {
            return AppServiceUtils.fromCsmDeploymentStatus(((SupportsOneDeploy) remote).getDeploymentStatus(deploymentId));
        } else {
            return null;
        }
    }

    @Override
    public boolean isStreamingLogSupported() {
        return Optional.ofNullable(getDiagnosticConfig()).map(DiagnosticConfig::isEnableWebServerLogging).orElse(false);
    }
}
