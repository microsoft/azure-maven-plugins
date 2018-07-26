/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingLinuxPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.ExistingWindowsPlanWithGroup;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.WebApp.DefinitionStages.WithDockerContainerImage;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.DockerImageType;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.Locale;

import static org.codehaus.plexus.util.StringUtils.isNotEmpty;

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

    public static final String TOMCAT_8_5_JRE8 = "tomcat 8.5-jre8";
    public static final String TOMCAT_9_0_JRE8 = "tomcat 9.0-jre8";
    public static final String JRE8 = "jre8";

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
        assureLinuxPlan(plan);

        final String resourceGroup = mojo.getResourceGroup();
        final ExistingLinuxPlanWithGroup existingLinuxPlanWithGroup = mojo.getAzureClient().webApps()
                .define(mojo.getAppName())
                .withExistingLinuxPlan(plan);
        return mojo.getAzureClient().resourceGroups().contain(resourceGroup) ?
                existingLinuxPlanWithGroup.withExistingResourceGroup(resourceGroup) :
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
        assureWindowsPlan(plan);

        final String resourceGroup = mojo.getResourceGroup();
        final ExistingWindowsPlanWithGroup existingWindowsPlanWithGroup = mojo.getAzureClient().webApps()
                .define(mojo.getAppName())
                .withExistingWindowsPlan(plan);
        return mojo.getAzureClient().resourceGroups().contain(resourceGroup) ?
                existingWindowsPlanWithGroup.withExistingResourceGroup(resourceGroup) :
                existingWindowsPlanWithGroup.withNewResourceGroup(resourceGroup);
    }

    private static void assureWindowsPlan(final AppServicePlan plan) throws MojoExecutionException {
        if (!plan.operatingSystem().equals(OperatingSystem.WINDOWS)) {
            throw new MojoExecutionException(String.format(SERVICE_PLAN_NOT_APPLICABLE,
                    plan.name(), OperatingSystem.WINDOWS.name()));
        }
    }

    public static AppServicePlan createOrGetAppServicePlan(final AbstractWebAppMojo mojo, OperatingSystem os)
            throws Exception {
        AppServicePlan plan = AppServiceUtils.getAppServicePlan(mojo);

        final Azure azure = mojo.getAzureClient();
        if (plan == null) {
            final String servicePlanName = AppServiceUtils.getAppServicePlanName(mojo);
            final String servicePlanResGrp = AppServiceUtils.getAppServicePlanResourceGroup(mojo);
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
            mojo.getLog().info(String.format(SERVICE_PLAN_EXIST, plan.name(), plan.resourceGroupName()));
        }

        return plan;
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
}
