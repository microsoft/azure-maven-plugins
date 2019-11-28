/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingLinuxPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingWindowsPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithDockerContainerImage;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class WebAppUtils {
    public static final String SERVICE_PLAN_NOT_APPLICABLE = "The App Service Plan '%s' is not a %s Plan";
    public static final String CREATE_SERVICE_PLAN = "Creating App Service Plan '%s'...";
    public static final String SERVICE_PLAN_CREATED = "Successfully created App Service Plan.";
    public static final String CONFIGURATION_NOT_APPLICABLE =
            "The configuration is not applicable for the target Web App (%s). Please correct it in pom.xml.";

    public static void assureLinuxWebApp(final WebApp app) throws MojoExecutionException {
        if (!isLinuxWebApp(app)) {
            throw new MojoExecutionException(String.format(CONFIGURATION_NOT_APPLICABLE, "Windows"));
        }
    }

    public static void assureWindowsWebApp(final WebApp app) throws MojoExecutionException {
        if (isLinuxWebApp(app)) {
            throw new MojoExecutionException(String.format(CONFIGURATION_NOT_APPLICABLE, "Linux"));
        }
    }

    public static WithDockerContainerImage defineLinuxApp(final String resourceGroup,
                                                          final String appName,
                                                          final Azure azureClient,
                                                          final AppServicePlan plan) throws MojoExecutionException {
        assureLinuxPlan(plan);

        final ExistingLinuxPlanWithGroup existingLinuxPlanWithGroup = azureClient.webApps()
                .define(appName).withExistingLinuxPlan(plan);
        return azureClient.resourceGroups().contain(resourceGroup) ?
                existingLinuxPlanWithGroup.withExistingResourceGroup(resourceGroup) :
                existingLinuxPlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    public static WithCreate defineWindowsApp(final String resourceGroup,
                                              final String appName,
                                              final Azure azureClient, final AppServicePlan plan) throws MojoExecutionException {
        assureWindowsPlan(plan);

        final ExistingWindowsPlanWithGroup existingWindowsPlanWithGroup = azureClient.webApps()
                .define(appName).withExistingWindowsPlan(plan);
        return azureClient.resourceGroups().contain(resourceGroup) ?
                existingWindowsPlanWithGroup.withExistingResourceGroup(resourceGroup) :
                existingWindowsPlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    public static AppServicePlan createOrGetAppServicePlan(String servicePlanName,
                                                           final String resourceGroup,
                                                           final Azure azure,
                                                           final String servicePlanResourceGroup,
                                                           final Region region,
                                                           final PricingTier pricingTier,
                                                           final Log log,
                                                           final OperatingSystem os) throws MojoExecutionException {
        final AppServicePlan plan = AppServiceUtils.getAppServicePlan(servicePlanName, azure,
                resourceGroup, servicePlanResourceGroup);

        return plan != null ? plan : createAppServicePlan(servicePlanName, resourceGroup, azure,
                servicePlanResourceGroup, region, pricingTier, log, os);
    }

    public static AppServicePlan createAppServicePlan(String servicePlanName,
                                                           final String resourceGroup,
                                                           final Azure azure,
                                                           final String servicePlanResourceGroup,
                                                           final Region region,
                                                           final PricingTier pricingTier,
                                                           final Log log,
                                                           final OperatingSystem os) throws MojoExecutionException {
        if (region == null) {
            throw new MojoExecutionException("Please config the <region> in pom.xml, " +
                    "it is required to create a new Azure App Service Plan.");
        }
        servicePlanName = AppServiceUtils.getAppServicePlanName(servicePlanName);
        final String servicePlanResGrp = AppServiceUtils.getAppServicePlanResourceGroup(
                resourceGroup, servicePlanResourceGroup);
        log.info(String.format(CREATE_SERVICE_PLAN, servicePlanName));

        final AppServicePlan.DefinitionStages.WithGroup withGroup = azure.appServices().appServicePlans()
                .define(servicePlanName).withRegion(region);

        final AppServicePlan.DefinitionStages.WithPricingTier withPricingTier
                = azure.resourceGroups().contain(servicePlanResGrp) ?
                withGroup.withExistingResourceGroup(servicePlanResGrp) :
                withGroup.withNewResourceGroup(servicePlanResGrp);

        final AppServicePlan result = withPricingTier.withPricingTier(pricingTier).withOperatingSystem(os).create();

        log.info(SERVICE_PLAN_CREATED);
        return result;
    }

    public static DockerImageType getDockerImageType(final String imageName, final String serverId,
                                                     final String registryUrl) {
        if (StringUtils.isEmpty(imageName)) {
            return DockerImageType.NONE;
        }

        final boolean isCustomRegistry = StringUtils.isNotEmpty(registryUrl);
        final boolean isPrivate = StringUtils.isNotEmpty(serverId);

        if (isCustomRegistry) {
            return isPrivate ? DockerImageType.PRIVATE_REGISTRY : DockerImageType.UNKNOWN;
        } else {
            return isPrivate ? DockerImageType.PRIVATE_DOCKER_HUB : DockerImageType.PUBLIC_DOCKER_HUB;
        }
    }

    /**
     * Workaround:
     * When a web app is created from Azure Portal, there are hidden tags associated with the app.
     * It will be messed up when calling "update" API.
     * An issue is logged at https://github.com/Azure/azure-sdk-for-java/issues/1755 .
     * Remove all tags here to make it work.
     *
     * @param app
     */
    public static void clearTags(final WebApp app) {
        app.inner().withTags(null);
    }

    private static void assureWindowsPlan(final AppServicePlan plan) throws MojoExecutionException {
        if (!plan.operatingSystem().equals(OperatingSystem.WINDOWS)) {
            throw new MojoExecutionException(String.format(SERVICE_PLAN_NOT_APPLICABLE,
                    plan.name(), OperatingSystem.WINDOWS.name()));
        }
    }

    private static void assureLinuxPlan(final AppServicePlan plan) throws MojoExecutionException {
        if (!plan.operatingSystem().equals(OperatingSystem.LINUX)) {
            throw new MojoExecutionException(String.format(SERVICE_PLAN_NOT_APPLICABLE,
                    plan.name(), OperatingSystem.LINUX.name()));
        }
    }

    private static boolean isLinuxWebApp(final WebApp app) {
        return app.inner().kind().contains("linux");
    }
}
