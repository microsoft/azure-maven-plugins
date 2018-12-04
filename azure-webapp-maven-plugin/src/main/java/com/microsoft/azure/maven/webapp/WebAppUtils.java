/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingLinuxPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingWindowsPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithDockerContainerImage;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
import java.util.Locale;

import static org.codehaus.plexus.util.StringUtils.isNotEmpty;

public class WebAppUtils {
    public static final String NOT_SUPPORTED_IMAGE = "The image: '%s' is not supported.";
    public static final String IMAGE_NOT_GIVEN = "Image name is not specified.";
    public static final String SERVICE_PLAN_NOT_APPLICABLE = "The App Service Plan '%s' is not a %s Plan";
    public static final String CREATE_SERVICE_PLAN = "Creating App Service Plan '%s'...";
    public static final String SERVICE_PLAN_EXIST = "Found existing App Service Plan '%s' in Resource Group '%s'.";
    public static final String SERVICE_PLAN_CREATED = "Successfully created App Service Plan.";
    public static final String TOMCAT_8_5_JRE8 = "tomcat 8.5-jre8";
    public static final String TOMCAT_9_0_JRE8 = "tomcat 9.0-jre8";
    public static final String JRE8 = "jre8";
    private static final String CONFIGURATION_NOT_APPLICABLE =
        "The configuration is not applicable for the target Web App (%s). Please correct it in pom.xml.";

    private static boolean isLinuxWebApp(final WebApp app) {
        return app.inner().kind().contains("linux");
    }

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
                                                          final AppServicePlan plan) throws Exception {
        assureLinuxPlan(plan);

        final ExistingLinuxPlanWithGroup existingLinuxPlanWithGroup = azureClient.webApps()
            .define(appName).withExistingLinuxPlan(plan);
        return azureClient.resourceGroups().contain(resourceGroup) ?
            existingLinuxPlanWithGroup.withExistingResourceGroup(resourceGroup) :
            existingLinuxPlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    private static void assureLinuxPlan(final AppServicePlan plan) throws MojoExecutionException {
        if (!plan.operatingSystem().equals(OperatingSystem.LINUX)) {
            throw new MojoExecutionException(String.format(SERVICE_PLAN_NOT_APPLICABLE,
                plan.name(), OperatingSystem.LINUX.name()));
        }
    }

    public static WithCreate defineWindowsApp(final String resourceGroup,
                                              final String appName,
                                              final Azure azureClient, final AppServicePlan plan) throws Exception {
        assureWindowsPlan(plan);

        final ExistingWindowsPlanWithGroup existingWindowsPlanWithGroup = azureClient.webApps()
            .define(appName).withExistingWindowsPlan(plan);
        return azureClient.resourceGroups().contain(resourceGroup) ?
            existingWindowsPlanWithGroup.withExistingResourceGroup(resourceGroup) :
            existingWindowsPlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    private static void assureWindowsPlan(final AppServicePlan plan) throws MojoExecutionException {
        if (!plan.operatingSystem().equals(OperatingSystem.WINDOWS)) {
            throw new MojoExecutionException(String.format(SERVICE_PLAN_NOT_APPLICABLE,
                plan.name(), OperatingSystem.WINDOWS.name()));
        }
    }

    public static AppServicePlan createOrGetAppServicePlan(String servicePlanName,
                                                           final String resourceGroup,
                                                           final Azure azure,
                                                           final String servicePlanResourceGroup,
                                                           final Region region,
                                                           final PricingTier pricingTier,
                                                           final Log log,
                                                           final OperatingSystem os) throws MojoExecutionException {
        AppServicePlan plan = AppServiceUtils.getAppServicePlan(servicePlanName, azure,
            resourceGroup, servicePlanResourceGroup);

        if (plan == null) {
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

            plan = withPricingTier.withPricingTier(pricingTier).withOperatingSystem(os).create();

            log.info(SERVICE_PLAN_CREATED);
        } else {
            log.info(String.format(SERVICE_PLAN_EXIST, plan.name(), plan.resourceGroupName()));
        }

        return plan;
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

    public static RuntimeStack getLinuxRunTimeStack(String imageName) throws MojoExecutionException {
        if (isNotEmpty(imageName)) {
            switch (imageName.toLowerCase(Locale.ENGLISH)) {
                case TOMCAT_8_5_JRE8:
                    return RuntimeStack.TOMCAT_8_5_JRE8;
                case TOMCAT_9_0_JRE8:
                    return RuntimeStack.TOMCAT_9_0_JRE8;
                case JRE8:
                    return RuntimeStack.JAVA_8_JRE8;
                default:
                    throw new MojoExecutionException(String.format(NOT_SUPPORTED_IMAGE, imageName));
            }
        }
        throw new MojoExecutionException(IMAGE_NOT_GIVEN);
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
}
