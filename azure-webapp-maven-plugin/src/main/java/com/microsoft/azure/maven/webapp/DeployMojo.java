/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotConfig;
import com.microsoft.azure.maven.webapp.task.DeployExternalResourcesTask;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.utils.AppServiceConfigUtils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
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
 * Deploy an Azure Web App, either Windows-based or Linux-based.
 */
@Mojo(name = "deploy", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployMojo extends AbstractWebAppMojo {
    private static final String WEBAPP_NOT_EXIST_FOR_SLOT = "The Web App specified in pom.xml does not exist. " +
            "Please make sure the Web App name is correct.";
    private static final String CREATE_DEPLOYMENT_SLOT = "Creating deployment slot %s in web app %s";
    private static final String CREATE_DEPLOYMENT_SLOT_DONE = "Successfully created the Deployment Slot.";
    private static final String CREATE_NEW_DEPLOYMENT_SLOT = "createNewDeploymentSlot";

    @Override
    protected void doExecute() throws AzureExecutionException {
        validateConfiguration(message -> AzureMessager.getMessager().error(message.getMessage()), true);
        // initialize library client
        az = getOrCreateAzureAppServiceClient();
        final IWebAppBase<?> target = createOrUpdateResource();
        deployExternalResources(target, getConfigParser().getExternalArtifacts());
        deploy(target, getConfigParser().getArtifacts());
    }

    private IWebAppBase<?> createOrUpdateResource() throws AzureExecutionException {
        final boolean skipCreate = skipAzureResourceCreate || skipCreateAzureResource;
        if (!isDeployToDeploymentSlot()) {
            final AppServiceConfig appServiceConfig = getConfigParser().getAppServiceConfig();
            IWebApp app = Azure.az(AzureAppService.class).webapp(appServiceConfig.resourceGroup(), appServiceConfig.appName());
            final boolean newWebApp = !app.exists();
            AppServiceConfig defaultConfig = !newWebApp ? fromAppService(app, app.plan()) : buildDefaultConfig(appServiceConfig.subscriptionId(),
                appServiceConfig.resourceGroup(), appServiceConfig.appName());
            mergeAppServiceConfig(appServiceConfig, defaultConfig);
            if (appServiceConfig.pricingTier() == null) {
                appServiceConfig.pricingTier(appServiceConfig.runtime().webContainer() == WebContainer.JBOSS_7 ? PricingTier.PREMIUM_P1V3 : PricingTier.PREMIUM_P1V2);
            }
            final CreateOrUpdateWebAppTask task = new CreateOrUpdateWebAppTask(appServiceConfig);
            task.setSkipCreateAzureResource(skipCreate);
            return task.execute();
        } else {
            // todo: New CreateOrUpdateDeploymentSlotTask
            final DeploymentSlotConfig config = getConfigParser().getDeploymentSlotConfig();
            final IWebAppDeploymentSlot slot = getDeploymentSlot(config);
            final boolean slotExists = slot.exists();
            if (!slotExists && skipCreate) {
                throwForbidCreateResourceWarning("Deployment slot", config.getName());
            }
            return slotExists ? updateDeploymentSlot(slot, config) : createDeploymentSlot(slot, config);
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

    private IWebAppDeploymentSlot getDeploymentSlot(final DeploymentSlotConfig config) throws AzureExecutionException {
        final IWebApp webApp = az.webapp(config.getResourceGroup(), config.getAppName());
        if (!webApp.exists()) {
            throw new AzureExecutionException(WEBAPP_NOT_EXIST_FOR_SLOT);
        }
        return webApp.deploymentSlot(config.getName());
    }

    private IWebAppDeploymentSlot createDeploymentSlot(final IWebAppDeploymentSlot slot, final DeploymentSlotConfig slotConfig) {
        AzureMessager.getMessager().info(AzureString.format(CREATE_DEPLOYMENT_SLOT, slotConfig.getName(), slotConfig.getAppName()));
        getTelemetryProxy().addDefaultProperty(CREATE_NEW_DEPLOYMENT_SLOT, String.valueOf(true));
        final IWebAppDeploymentSlot result = slot.create().withName(slotConfig.getName())
                .withConfigurationSource(slotConfig.getConfigurationSource())
                .withAppSettings(slotConfig.getAppSettings())
                .commit();
        AzureMessager.getMessager().info(CREATE_DEPLOYMENT_SLOT_DONE);
        return result;
    }

    // update existing slot is not supported in current version, will implement it later
    private IWebAppDeploymentSlot updateDeploymentSlot(final IWebAppDeploymentSlot slot, final DeploymentSlotConfig slotConfig) {
        AzureMessager.getMessager().warning("update existing slot is not supported in current version");
        return slot;
    }

    private void deploy(IWebAppBase<?> target, List<WebAppArtifact> artifacts) {
        new DeployWebAppTask(target, artifacts, isStopAppDuringDeployment()).execute();
    }

    private void deployExternalResources(final IWebAppBase<?> target, final List<DeploymentResource> resources) {
        new DeployExternalResourcesTask(target, resources).execute();
    }
}
