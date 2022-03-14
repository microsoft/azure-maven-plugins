/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotConfig;
import com.microsoft.azure.maven.webapp.task.DeployExternalResourcesTask;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlotDraft;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.List;

import static com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils.fromAppService;
import static com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils.mergeAppServiceConfig;
import static com.microsoft.azure.toolkit.lib.appservice.utils.Utils.throwForbidCreateResourceWarning;

/**
 * Deploy your project to Azure Web App. If target app doesn't exist, it will be created.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {
    private static final String WEBAPP_NOT_EXIST_FOR_SLOT = "The Web App specified in pom.xml does not exist. " +
            "Please make sure the Web App name is correct.";

    @Override
    @AzureOperation(name = "webapp.deploy_app", type = AzureOperation.Type.ACTION)
    protected void doExecute() throws AzureExecutionException {
        validateConfiguration(message -> AzureMessager.getMessager().error(message.getMessage()), true);
        // initialize library client
        az = getOrCreateAzureAppServiceClient();
        final WebAppBase<?, ?, ?> target = createOrUpdateResource();
        deployExternalResources(target, getConfigParser().getExternalArtifacts());
        deploy(target, getConfigParser().getArtifacts());
        updateTelemetryProperties();
    }

    private WebAppBase<?, ?, ?> createOrUpdateResource() throws AzureExecutionException {
        final boolean skipCreate = skipAzureResourceCreate || skipCreateAzureResource;
        if (!isDeployToDeploymentSlot()) {
            final AppServiceConfig appServiceConfig = getConfigParser().getAppServiceConfig();
            WebApp app = az.webApps(appServiceConfig.subscriptionId()).getOrDraft(appServiceConfig.appName(), appServiceConfig.resourceGroup());
            AppServiceConfig defaultConfig = app.exists() ? fromAppService(app, app.getAppServicePlan()) : buildDefaultConfig(appServiceConfig.subscriptionId(),
                appServiceConfig.resourceGroup(), appServiceConfig.appName());
            mergeAppServiceConfig(appServiceConfig, defaultConfig);
            if (appServiceConfig.pricingTier() == null) {
                appServiceConfig.pricingTier(appServiceConfig.runtime().webContainer() == WebContainer.JBOSS_7 ?
                        PricingTier.PREMIUM_P1V3 : PricingTier.PREMIUM_P1V2);
            }
            final CreateOrUpdateWebAppTask task = new CreateOrUpdateWebAppTask(appServiceConfig);
            task.setSkipCreateAzureResource(skipCreate);
            return task.doExecute();
        } else {
            // todo: New CreateOrUpdateDeploymentSlotTask
            final DeploymentSlotConfig config = getConfigParser().getDeploymentSlotConfig();
            final WebAppDeploymentSlotDraft slotDraft = getDeploymentSlot(config);
            final boolean slotExists = slotDraft.exists();
            if (!slotExists && skipCreate) {
                throwForbidCreateResourceWarning("Deployment slot", config.getName());
            }
            return slotExists ? updateDeploymentSlot(slotDraft, config) : createDeploymentSlot(slotDraft, config);
        }
    }

    private AppServiceConfig buildDefaultConfig(String subscriptionId, String resourceGroup, String appName) {
        ComparableVersion javaVersionForProject = null;
        final String outputFileName = project.getBuild().getFinalName() + "." + project.getPackaging();
        File outputFile = new File(project.getBuild().getDirectory(), outputFileName);
        if (outputFile.exists() && StringUtils.equalsIgnoreCase("jar", project.getPackaging())) {
            try {
                javaVersionForProject = new ComparableVersion(Utils.getArtifactCompileVersion(outputFile));
            } catch (Exception e) {
                // it is acceptable that java version from jar file cannot be retrieved
            }
        }

        javaVersionForProject = ObjectUtils.firstNonNull(javaVersionForProject, new ComparableVersion(System.getProperty("java.version")));
        // get java version according to project java version
        JavaVersion javaVersion = javaVersionForProject.compareTo(new ComparableVersion("9")) < 0 ? JavaVersion.JAVA_8 : JavaVersion.JAVA_11;
        return AppServiceConfigUtils.buildDefaultWebAppConfig(subscriptionId, resourceGroup, appName, this.project.getPackaging(), javaVersion);
    }

    private WebAppDeploymentSlotDraft getDeploymentSlot(final DeploymentSlotConfig config) throws AzureExecutionException {
        final WebApp webApp = az.webApps(config.getSubscriptionId()).getOrDraft(config.getAppName(), config.getResourceGroup());
        if (!webApp.exists()) {
            throw new AzureExecutionException(WEBAPP_NOT_EXIST_FOR_SLOT);
        }
        return webApp.slots().updateOrCreate(config.getName(), config.getResourceGroup());
    }

    private WebAppDeploymentSlot createDeploymentSlot(final WebAppDeploymentSlotDraft draft, final DeploymentSlotConfig slotConfig) {
        draft.setConfigurationSource(slotConfig.getConfigurationSource());
        draft.setAppSettings(slotConfig.getAppSettings());
        return draft.commit();
    }

    // update existing slot is not supported in current version, will implement it later
    private WebAppDeploymentSlot updateDeploymentSlot(final WebAppDeploymentSlotDraft slot, final DeploymentSlotConfig slotConfig) {
        AzureMessager.getMessager().warning("update existing slot is not supported in current version");
        return slot;
    }

    private void deploy(WebAppBase<?, ?, ?> target, List<WebAppArtifact> artifacts) {
        new DeployWebAppTask(target, artifacts, isStopAppDuringDeployment()).doExecute();
    }

    private void deployExternalResources(final WebAppBase<?, ?, ?> target, final List<DeploymentResource> resources) {
        new DeployExternalResourcesTask(target, resources).doExecute();
    }
}
