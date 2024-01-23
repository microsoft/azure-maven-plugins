/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;
import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.maven.webapp.task.DeployExternalResourcesTask;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.StreamingLogTask;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils.fromAppService;
import static com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils.mergeAppServiceConfig;

/**
 * Deploy your project to Azure Web App. If target app doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {

    /**
     * Boolean flag to control whether to wait deployment complete in app service with deployment status API.
     * @since 2.8.0
     */
    @Getter
    @Parameter(property = "webapp.waitDeploymentComplete")
    protected Boolean waitDeploymentComplete;

    /**
     *  The interval in seconds to check deployment status.
     *  @since 2.8.0
     */
    @Getter
    @Parameter(property = "webapp.deploymentStatusRefreshInterval")
    protected Long deploymentStatusRefreshInterval;

    /**
     *  The max retry times to check deployment status
     *  @since 2.8.0
     */
    @Getter
    @Parameter(property = "webapp.deploymentStatusMaxRefreshTimes")
    protected Long deploymentStatusMaxRefreshTimes;

    @Override
    @AzureOperation(name = "user/webapp.deploy_app")
    protected void doExecute() throws AzureExecutionException {
        mergeCommandLineConfig();
        // initialize library client
        az = initAzureAppServiceClient();
        WebAppRuntime.tryLoadingAllRuntimes();
        doValidate();
        final AppServiceConfig appServiceConfig = getConfigParser().getAppServiceConfig();
        final WebApp app = Azure.az(AzureWebApp.class).webApps(appServiceConfig.subscriptionId())
                .getOrDraft(appServiceConfig.appName(), appServiceConfig.resourceGroup());
        try {
            final WebAppBase<?, ?, ?> target = createOrUpdateResource(app);
            deployExternalResources(target, getConfigParser().getExternalArtifacts());
            deploy(target, getConfigParser().getArtifacts());
            AzureMessager.getMessager().info(AzureString.format("Application url: %s", "https://" + target.getHostName()));
        } catch (final Exception e) {
            new StreamingLogTask(app).execute();
            throw new AzureToolkitRuntimeException(e);
        }
        updateTelemetryProperties();
    }

    private void doValidate() throws AzureExecutionException {
        validateConfiguration(message -> AzureMessager.getMessager().error(message.getMessage()), true);
        validateArtifactCompileVersion();
    }

    private void validateArtifactCompileVersion() throws AzureExecutionException {
        final RuntimeConfig runtime = getConfigParser().getRuntimeConfig();
        final List<WebAppArtifact> artifacts = Optional.ofNullable(getConfigParser().getArtifacts())
                .orElse(Collections.emptyList());
        if (Objects.isNull(runtime) || runtime.os() == OperatingSystem.DOCKER || CollectionUtils.isEmpty(artifacts)) {
            return;
        }
        final String javaVersion = Optional.of(runtime).map(RuntimeConfig::javaVersion).orElse(StringUtils.EMPTY);
        artifacts.stream().map(WebAppArtifact::getFile).filter(Objects::nonNull)
                .forEach(artifact -> validateArtifactCompileVersion(javaVersion, artifact, getFailsOnRuntimeValidationError()));
    }

    private void mergeCommandLineConfig() {
        try {
            final JavaPropsMapper mapper = new JavaPropsMapper();
            mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            final DeployMojo commandLineConfig = mapper.readSystemPropertiesAs(JavaPropsSchema.emptySchema(), DeployMojo.class);
            Utils.copyProperties(this, commandLineConfig, false);
        } catch (IOException | IllegalAccessException e) {
            throw new AzureToolkitRuntimeException("failed to merge command line configuration", e);
        }
    }

    private WebAppBase<?, ?, ?> createOrUpdateResource(WebAppBase<?, ?, ?> app) throws AzureExecutionException {
        final boolean skipCreate = skipAzureResourceCreate || BooleanUtils.isTrue(skipCreateAzureResource);
        final AppServiceConfig appServiceConfig = getConfigParser().getAppServiceConfig();
        final AppServiceConfig defaultConfig = app.exists() ? fromAppService(app, Objects.requireNonNull(app.getAppServicePlan())) :
                buildDefaultConfig(appServiceConfig.subscriptionId(), appServiceConfig.resourceGroup(), appServiceConfig.appName());
        mergeAppServiceConfig(appServiceConfig, defaultConfig);
        if (appServiceConfig.pricingTier() == null) {
            final String container = Optional.ofNullable(appServiceConfig.runtime()).map(RuntimeConfig::webContainer).orElse(StringUtils.EMPTY);
            appServiceConfig.pricingTier(StringUtils.startsWithIgnoreCase(container, "JBOSS") ?
                PricingTier.PREMIUM_P1V3 : PricingTier.PREMIUM_P1V2);
        }
        final CreateOrUpdateWebAppTask task = new CreateOrUpdateWebAppTask(appServiceConfig);
        task.setSkipCreateAzureResource(skipCreate);
        return task.doExecute();
    }

    private AppServiceConfig buildDefaultConfig(String subscriptionId, String resourceGroup, String appName) {
        return AppServiceConfigUtils.buildDefaultWebAppConfig(subscriptionId, resourceGroup, appName, this.project.getPackaging());
    }

    private void deploy(WebAppBase<?, ?, ?> target, List<WebAppArtifact> artifacts) {
        final DeployWebAppTask deployWebAppTask = new DeployWebAppTask(target, artifacts, this.getRestartSite(), this.getWaitDeploymentComplete(), true);
        Optional.ofNullable(this.getDeploymentStatusRefreshInterval()).ifPresent(deployWebAppTask::setDeploymentStatusRefreshInterval);
        Optional.ofNullable(this.getDeploymentStatusMaxRefreshTimes()).ifPresent(deployWebAppTask::setDeploymentStatusMaxRefreshTimes);
        deployWebAppTask.setDeploymentStatusStream(System.out);
        deployWebAppTask.doExecute();
    }

    private void deployExternalResources(final WebAppBase<?, ?, ?> target, final List<DeploymentResource> resources) {
        new DeployExternalResourcesTask(target, resources).doExecute();
    }
}
