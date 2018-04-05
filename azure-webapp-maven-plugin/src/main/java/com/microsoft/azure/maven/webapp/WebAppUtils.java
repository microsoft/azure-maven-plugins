/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import static org.codehaus.plexus.util.StringUtils.isNotEmpty;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithDockerContainerImage;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingLinuxPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingWindowsPlanWithGroup;

import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.UUID;

public class WebAppUtils {
    public static final String CONTAINER_SETTING_NOT_APPLICABLE =
            "<containerSettings> is not applicable to Web App on Windows; " +
                    "please use <javaVersion> and <javaWebContainer> to configure your runtime.";
    public static final String JAVA_VERSION_NOT_APPLICABLE = "<javaVersion> is not applicable to Web App on Linux; " +
            "please use <containerSettings> to specify your runtime.";
    public static final String NOT_SUPPORTED_IMAGE = "The image: '%s' is not supported.";
    public static final String IMAGE_NOT_GIVEN = "Image name is not specified.";
    public static final String SERVICE_PLAN_NOT_APPLICABLE = "The App Service Plan '%s' is not a %s Plan";

    public static final String CREATE_SERVICE_PLAN = "Creating App Service Plan '%s'...";
    public static final String SERVICE_PLAN_EXIST = "Found existing App Service Plan '%s' in Resource Group '%s'.";
    public static final String SERVICE_PLAN_CREATED = "Successfully created App Service Plan.";

    private static boolean isLinuxWebApp(final WebApp app) {
        return app.inner().kind().contains("linux");
    }

    public static void assureLinuxWebApp(final WebApp app) throws MojoExecutionException {
        if (!isLinuxWebApp(app)) {
            throw new MojoExecutionException(CONTAINER_SETTING_NOT_APPLICABLE);
        }
    }

    public static void assureWindowsWebApp(final WebApp app) throws MojoExecutionException {
        if (isLinuxWebApp(app)) {
            throw new MojoExecutionException(JAVA_VERSION_NOT_APPLICABLE);
        }
    }

    public static WithDockerContainerImage defineLinuxApp(final AbstractWebAppMojo mojo, final AppServicePlan plan)
            throws Exception {
        final String resourceGroup = mojo.getResourceGroup();
        final boolean isResourceGroupExist = mojo.getAzureClient().resourceGroups().contain(resourceGroup);

        assureLinuxPlan(plan);

        final ExistingLinuxPlanWithGroup existingLinuxPlanWithGroup = mojo.getAzureClient().webApps()
                .define(mojo.getAppName())
                .withExistingLinuxPlan(plan);
        return isResourceGroupExist ? existingLinuxPlanWithGroup.withExistingResourceGroup(resourceGroup) :
                existingLinuxPlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    private static void assureLinuxPlan(final AppServicePlan plan) throws MojoExecutionException {
        if (!plan.operatingSystem().equals(OperatingSystem.LINUX)) {
            throw new MojoExecutionException(String.format(SERVICE_PLAN_NOT_APPLICABLE,
                    plan.name(), OperatingSystem.LINUX.name()));
        }
    }

    public static WithCreate defineWindowsApp(final AbstractWebAppMojo mojo, final AppServicePlan plan)
            throws Exception {
        final String resourceGroup = mojo.getResourceGroup();
        final boolean isResourceGroupExist = mojo.getAzureClient().resourceGroups().contain(resourceGroup);

        assureWindowsPlan(plan);

        final ExistingWindowsPlanWithGroup existingWindowsPlanWithGroup = mojo.getAzureClient().webApps()
                .define(mojo.getAppName())
                .withExistingWindowsPlan(plan);
        return isResourceGroupExist ? existingWindowsPlanWithGroup.withExistingResourceGroup(resourceGroup) :
                existingWindowsPlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    private static void assureWindowsPlan(final AppServicePlan plan) throws MojoExecutionException {
        if (!plan.operatingSystem().equals(OperatingSystem.WINDOWS)) {
            throw new MojoExecutionException(String.format(SERVICE_PLAN_NOT_APPLICABLE,
                    plan.name(), OperatingSystem.LINUX.name()));
        }
    }

    public static AppServicePlan createOrGetAppServicePlan(final AbstractWebAppMojo mojo, OperatingSystem os)
            throws Exception {
        AppServicePlan plan = null;
        final String servicePlanResGrp = StringUtils.isNotEmpty(mojo.getAppServicePlanResourceGroup()) ?
                mojo.getAppServicePlanResourceGroup() : mojo.getResourceGroup();

        String servicePlanName = mojo.getAppServicePlanName();
        if (StringUtils.isNotEmpty(servicePlanName)) {
            plan = mojo.getAzureClient().appServices().appServicePlans()
                    .getByResourceGroup(servicePlanResGrp, servicePlanName);
        } else {
            servicePlanName = generateRandomServicePlanName();
        }

        final Azure azure = mojo.getAzureClient();
        if (plan == null) {
            mojo.getLog().info(String.format(CREATE_SERVICE_PLAN, servicePlanName));

            final AppServicePlan.DefinitionStages.WithGroup withGroup = azure.appServices().appServicePlans()
                    .define(servicePlanName)
                    .withRegion(mojo.getRegion());

            final AppServicePlan.DefinitionStages.WithPricingTier withPricingTier
                    = azure.resourceGroups().contain(servicePlanResGrp) ?
                    withGroup.withExistingResourceGroup(servicePlanResGrp) :
                    withGroup.withNewResourceGroup(servicePlanResGrp);

            plan = withPricingTier.withPricingTier(mojo.getPricingTier())
                    .withOperatingSystem(os).create();

            mojo.getLog().info(SERVICE_PLAN_CREATED);
        } else {
            mojo.getLog().info(String.format(SERVICE_PLAN_EXIST, servicePlanName, servicePlanResGrp));
        }

        return plan;
    }

    private static String generateRandomServicePlanName() {
        return "ServicePlan" + UUID.randomUUID().toString().substring(0, 18);
    }

    public static DockerImageType getDockerImageType(final ContainerSetting containerSetting) {
        if (containerSetting == null || StringUtils.isEmpty(containerSetting.getImageName())) {
            return DockerImageType.NONE;
        }

        final boolean isCustomRegistry = StringUtils.isNotEmpty(containerSetting.getRegistryUrl());
        final boolean isPrivate = StringUtils.isNotEmpty(containerSetting.getServerId());

        if (isCustomRegistry) {
            return isPrivate ? DockerImageType.PRIVATE_REGISTRY : DockerImageType.UNKNOWN;
        } else {
            return isPrivate ? DockerImageType.PRIVATE_DOCKER_HUB : DockerImageType.PUBLIC_DOCKER_HUB;
        }
    }

    public static RuntimeStack getLinuxRunTimeStack(String imageName) throws MojoExecutionException {
        if (isNotEmpty(imageName)) {
            if (imageName.equalsIgnoreCase(RuntimeStack.TOMCAT_8_5_JRE8.toString())) {
                return RuntimeStack.TOMCAT_8_5_JRE8;
            } else if (imageName.equalsIgnoreCase(RuntimeStack.TOMCAT_9_0_JRE8.toString())) {
                return RuntimeStack.TOMCAT_9_0_JRE8;
            } else {
                throw new MojoExecutionException(String.format(NOT_SUPPORTED_IMAGE, imageName));
            }
        }
        throw new MojoExecutionException(IMAGE_NOT_GIVEN);
    }
}
