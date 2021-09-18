/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.task;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation.Type;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CreateOrUpdateWebAppTask extends AzureTask<IWebApp> {
    private static final String CREATE_NEW_WEB_APP = "createNewWebApp";

    private static final String CREATE_WEBAPP = "Creating web app %s...";
    private static final String CREATE_WEB_APP_DONE = "Successfully created Web App %s.";
    private static final String UPDATE_WEBAPP = "Updating target Web App %s...";
    private static final String UPDATE_WEBAPP_DONE = "Successfully updated Web App %s.";

    private final AppServiceConfig config;
    private final AppServiceConfig defaultConfig;
    private final List<AzureTask<?>> subTasks;

    public CreateOrUpdateWebAppTask(AppServiceConfig config, AppServiceConfig defaultConfig) {
        this.config = config;
        this.defaultConfig = defaultConfig;
        this.subTasks = this.initTasks();
    }

    private List<AzureTask<?>> initTasks() {
        final List<AzureTask<?>> tasks = new ArrayList<>();
        final AzureString title = AzureString.format("Create new web app({0})", this.config.appName());
        AzureAppService az = Azure.az(AzureAppService.class);
        tasks.add(new AzureTask<>(title, () -> {
            final IWebApp target = az.subscription(config.subscriptionId())
                .webapp(config.resourceGroup(), config.appName());
            if (!target.exists()) {
                CheckNameAvailabilityResultEntity result = az.checkNameAvailability(config.subscriptionId(), config.appName());
                if (!result.isAvailable()) {
                    throw new AzureToolkitRuntimeException(AzureString.format("Cannot create webapp {0} due to error: {1}",
                            config.appName(),
                            result.getUnavailabilityReason()).getString());
                }
                return create();
            }
            return update(target);
        }));
        return tasks;
    }

    @AzureOperation(name = "webapp.create", params = {"this.config.appName()"}, type = Type.SERVICE)
    private IWebApp create() {
        AzureTelemetry.getActionContext().setProperty(CREATE_NEW_WEB_APP, String.valueOf(true));
        AzureMessager.getMessager().info(String.format(CREATE_WEBAPP, config.appName()));

        // handle default region for resource group
        final Region region = ObjectUtils.firstNonNull(this.config.region(), this.defaultConfig.region());
        final ResourceGroup resourceGroup = new CreateResourceGroupTask(this.config.subscriptionId(), this.config.resourceGroup(), region).execute();

        final AzureAppService az = Azure.az(AzureAppService.class).subscription(config.subscriptionId());
        final IWebApp webapp = az.webapp(config.resourceGroup(), config.appName());
        final AppServicePlanConfig servicePlanConfig = config.getServicePlanConfig();
        final RuntimeConfig runtimeConfigOrDefault = getRuntimeConfigOrDefault(config.runtime(), defaultConfig.runtime());
        final Runtime runtime = getRuntime(runtimeConfigOrDefault);
        final IAppServicePlan appServicePlan = new CreateOrUpdateAppServicePlanTask(servicePlanConfig,
            buildDefaultAppServicePlanConfig(Region.fromName(resourceGroup.getRegion()), runtime)).execute();

        final IWebApp result = webapp.create().withName(config.appName())
            .withResourceGroup(config.resourceGroup())
            .withPlan(appServicePlan.id())
            .withRuntime(runtime)
            .withDockerConfiguration(getDockerConfiguration(runtimeConfigOrDefault))
            .withAppSettings(config.appSettings())
            .commit();
        AzureMessager.getMessager().info(String.format(CREATE_WEB_APP_DONE, result.name()));
        return result;
    }

    @AzureOperation(name = "webapp.update", params = {"this.config.appName()"}, type = Type.SERVICE)
    private IWebApp update(final IWebApp webApp) {
        AzureMessager.getMessager().info(String.format(UPDATE_WEBAPP, webApp.name()));
        final IAppServicePlan currentPlan = webApp.plan();
        final AppServicePlanConfig servicePlanConfig = config.getServicePlanConfig();

        if (StringUtils.isAllBlank(config.servicePlanResourceGroup(), config.servicePlanName())) {
            // initialize service plan creation from exising webapp if user doesn't specify it in config
            servicePlanConfig.servicePlanResourceGroup(currentPlan.resourceGroup());
            servicePlanConfig.servicePlanName(currentPlan.name());
        } else if (StringUtils.equalsIgnoreCase(config.servicePlanName(), currentPlan.name()) && StringUtils.isBlank(config.servicePlanResourceGroup())) {
            servicePlanConfig.servicePlanResourceGroup(currentPlan.resourceGroup());
        } else if (StringUtils.equalsIgnoreCase(servicePlanConfig.servicePlanResourceGroup(), currentPlan.resourceGroup()) &&
                StringUtils.isBlank(config.servicePlanName())) {
            servicePlanConfig.servicePlanName(currentPlan.name());
        }
        final Runtime runtime = getRuntime(config.runtime(), webApp.entity().getRuntime());
        final IAppServicePlan appServicePlan = new CreateOrUpdateAppServicePlanTask(servicePlanConfig,
            buildDefaultAppServicePlanConfig(webApp.entity().getRegion(), ObjectUtils.firstNonNull(runtime, webApp.entity().getRuntime()))).execute();
        final IWebApp result = webApp.update().withPlan(appServicePlan.id())
            .withRuntime(runtime)
            .withDockerConfiguration(getDockerConfiguration(config.runtime()))
            .withAppSettings(ObjectUtils.firstNonNull(config.appSettings(), new HashMap<>()))
            .commit();
        AzureMessager.getMessager().info(String.format(UPDATE_WEBAPP_DONE, webApp.name()));
        return result;
    }

    private Runtime getRuntime(RuntimeConfig runtime, Runtime remote) {
        if (runtime == null) {
            return null;
        }
        if (runtime.os() == null) {
            runtime.os(remote.getOperatingSystem());
        }
        if (OperatingSystem.DOCKER == runtime.os()) {
            return Runtime.getRuntime(OperatingSystem.DOCKER, WebContainer.JAVA_OFF, JavaVersion.OFF);
        } else {
            return Runtime.getRuntime(runtime.os(),
                ObjectUtils.firstNonNull(runtime.webContainer(), remote.getWebContainer()),
                ObjectUtils.firstNonNull(runtime.javaVersion(), remote.getJavaVersion()));
        }
    }

    private AppServicePlanConfig buildDefaultAppServicePlanConfig(Region region, Runtime runtime) {
        final AppServicePlanConfig defaultServicePlanConfig = new AppServicePlanConfig();
        defaultServicePlanConfig.region(region);
        defaultServicePlanConfig.os(OperatingSystem.LINUX);
        defaultServicePlanConfig.pricingTier(runtime.getWebContainer().equals(WebContainer.JBOSS_7) ? PricingTier.PREMIUM_P1V3 : PricingTier.PREMIUM_P1V2);
        return defaultServicePlanConfig;
    }

    private DockerConfiguration getDockerConfiguration(RuntimeConfig runtime) {
        if (runtime != null && OperatingSystem.DOCKER == runtime.os()) {
            return DockerConfiguration.builder()
                .userName(runtime.username())
                .password(runtime.password())
                .registryUrl(runtime.registryUrl())
                .image(runtime.image())
                .startUpCommand(runtime.startUpCommand())
                .build();
        }
        return null;

    }

    private RuntimeConfig getRuntimeConfigOrDefault(RuntimeConfig runtime, RuntimeConfig defaultRuntime) {
        if (runtime == null) {
            return defaultRuntime;
        }

        final RuntimeConfig result = new RuntimeConfig();
        result.os(ObjectUtils.firstNonNull(runtime.os(), defaultRuntime.os()));
        result.image(ObjectUtils.firstNonNull(runtime.image(), defaultRuntime.image()));
        result.username(ObjectUtils.firstNonNull(runtime.username(), defaultRuntime.username()));
        result.password(ObjectUtils.firstNonNull(runtime.password(), defaultRuntime.password()));
        result.startUpCommand(ObjectUtils.firstNonNull(runtime.startUpCommand(), defaultRuntime.startUpCommand()));
        result.registryUrl(ObjectUtils.firstNonNull(runtime.registryUrl(), defaultRuntime.registryUrl()));
        result.javaVersion(ObjectUtils.firstNonNull(runtime.javaVersion(), defaultRuntime.javaVersion()));
        result.webContainer(ObjectUtils.firstNonNull(runtime.webContainer(), defaultRuntime.webContainer()));
        return result;
    }

    private Runtime getRuntime(RuntimeConfig runtime) {
        if (runtime != null && OperatingSystem.DOCKER != runtime.os()) {
            return Runtime.getRuntime(runtime.os(),
                runtime.webContainer(),
                runtime.javaVersion());
        } else if (runtime != null && OperatingSystem.DOCKER == runtime.os()) {
            return Runtime.getRuntime(OperatingSystem.DOCKER, WebContainer.JAVA_OFF, JavaVersion.OFF);
        }
        return null;
    }

    @Override
    @AzureOperation(name = "webapp.create_update", params = {"this.config.appName()"}, type = Type.SERVICE)
    public IWebApp execute() {
        return (IWebApp) Flux.fromStream(this.subTasks.stream().map(t -> t.getSupplier().get())).last().block();
    }
}
