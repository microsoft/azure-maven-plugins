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
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlotDraft;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDraft;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation.Type;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.appservice.utils.Utils.throwForbidCreateResourceWarning;

@Slf4j
public class CreateOrUpdateWebAppTask extends AzureTask<WebAppBase<?, ?, ?>> {
    private static final String CREATE_NEW_WEB_APP = "createNewWebApp";
    private static final String WEBAPP_NOT_EXIST_FOR_SLOT = "The Web App specified in pom.xml does not exist. " +
            "Please make sure the Web App name is correct.";

    private final AppServiceConfig config;
    private final List<AzureTask<?>> subTasks;

    @Setter
    private boolean skipCreateAzureResource;

    public CreateOrUpdateWebAppTask(AppServiceConfig config) {
        this.config = config;
        this.subTasks = this.initTasks();
    }

    private List<AzureTask<?>> initTasks() {
        final List<AzureTask<?>> tasks = new ArrayList<>();
        final AzureString title = AzureString.format("Create new web app({0})", this.config.appName());
        tasks.add(new AzureTask<>(title, this::createOrUpdateResource));
        return tasks;
    }

    private WebAppBase<?, ?, ?> createOrUpdateResource() {
        final AzureWebApp az = Azure.az(AzureWebApp.class);
        final WebApp target = az.webApps(config.subscriptionId()).getOrDraft(config.appName(), config.resourceGroup());
        if (!isDeployToDeploymentSlot()) {
            if (!target.exists()) {
                if (skipCreateAzureResource) {
                    throwForbidCreateResourceWarning("Web app", config.appName());
                }
                final Availability result = Objects.requireNonNull(az.get(config.subscriptionId(), null)).checkNameAvailability(config.appName());
                if (!result.isAvailable()) {
                    throw new AzureToolkitRuntimeException(AzureString.format("Cannot create webapp {0} due to error: {1}",
                            config.appName(), result.getUnavailabilityReason()).getString());
                }
                return create();
            } else {
                return update(target);
            }
        } else {
            if (!target.exists()) {
                throw new AzureToolkitRuntimeException(WEBAPP_NOT_EXIST_FOR_SLOT);
            }
            final WebAppDeploymentSlotDraft slotDraft = target.slots().updateOrCreate(config.deploymentSlotName(), config.resourceGroup());
            final boolean slotExists = slotDraft.exists();
            if (!slotExists && skipCreateAzureResource) {
                throwForbidCreateResourceWarning("Deployment slot", config.deploymentSlotName());
            }
            return slotExists ? updateDeploymentSlot(slotDraft) : createDeploymentSlot(slotDraft);
        }
    }

    @AzureOperation(name = "webapp.create_app.app", params = {"this.config.appName()"}, type = Type.SERVICE)
    private WebApp create() {
        OperationContext.action().setTelemetryProperty(CREATE_NEW_WEB_APP, String.valueOf(true));
        final Region region = this.config.region();
        final AppServicePlanConfig planConfig = config.getServicePlanConfig();

        new CreateResourceGroupTask(this.config.subscriptionId(), this.config.resourceGroup(), region).doExecute();

        final AppServicePlanDraft planDraft = Azure.az(AzureAppService.class).plans(planConfig.subscriptionId())
            .updateOrCreate(planConfig.servicePlanName(), planConfig.servicePlanResourceGroup());
        planDraft.setPlanConfig(planConfig);

        final WebAppDraft appDraft = Azure.az(AzureWebApp.class).webApps(config.subscriptionId()).create(config.appName(), config.resourceGroup());
        appDraft.setAppServicePlan(planDraft.commit());
        appDraft.setRuntime(getRuntime(config.runtime()));
        appDraft.setDockerConfiguration(getDockerConfiguration(config.runtime()));
        appDraft.setAppSettings(config.appSettings());
        return appDraft.createIfNotExist();
    }

    @AzureOperation(name = "webapp.update_app.app", params = {"this.config.appName()"}, type = Type.SERVICE)
    private WebApp update(final WebApp webApp) {
        final WebAppDraft draft = (WebAppDraft) webApp.update();
        final AppServicePlanConfig servicePlanConfig = config.getServicePlanConfig();
        final Runtime runtime = getRuntime(config.runtime());

        AppServicePlanDraft planDraft = Azure.az(AzureAppService.class).plans(servicePlanConfig.subscriptionId())
                .updateOrCreate(servicePlanConfig.servicePlanName(), servicePlanConfig.servicePlanResourceGroup());
        if (skipCreateAzureResource && !planDraft.exists()) {
            throwForbidCreateResourceWarning("Service plan", servicePlanConfig.servicePlanResourceGroup() + "/" + servicePlanConfig.servicePlanName());
        }
        planDraft.setPlanConfig(servicePlanConfig);

        draft.setAppServicePlan(planDraft.commit());
        draft.setRuntime(runtime);
        draft.setDockerConfiguration(getDockerConfiguration(config.runtime()));
        draft.setAppSettings(ObjectUtils.firstNonNull(config.appSettings(), new HashMap<>()));
        return draft.updateIfExist();
    }

    @AzureOperation(name = "webapp.create_slot.slot|app", params = {"this.config.deploymentSlotName()", "this.config.appName()"}, type = Type.SERVICE)
    private WebAppDeploymentSlot createDeploymentSlot(final WebAppDeploymentSlotDraft draft) {
        draft.setAppSettings(config.appSettings());
        draft.setConfigurationSource(config.deploymentSlotConfigurationSource());
        return draft.commit();
    }

    @AzureOperation(name = "webapp.update_slot.slot|app", params = {"this.config.deploymentSlotName()", "this.config.appName()"}, type = Type.SERVICE)
    private WebAppDeploymentSlot updateDeploymentSlot(final WebAppDeploymentSlotDraft draft) {
        draft.setAppSettings(config.appSettings());
        return draft.commit();
    }

    private boolean isDeployToDeploymentSlot() {
        return StringUtils.isNoneBlank(config.deploymentSlotName());
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
    @AzureOperation(name = "webapp.create_update_app.app", params = {"this.config.appName()"}, type = Type.SERVICE)
    public WebAppBase<?, ?, ?> doExecute() {
        return (WebAppBase<?, ?, ?>) Flux.fromStream(this.subTasks.stream().map(t -> {
            try {
                return t.getBody().call();
            } catch (Throwable e) {
                log.warn(e.getMessage(), e);
            }
            return null;
        })).last().block();
    }
}
