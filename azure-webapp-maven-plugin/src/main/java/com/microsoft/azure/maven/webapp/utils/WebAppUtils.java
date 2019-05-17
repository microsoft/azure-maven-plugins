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
    public static final String SERVICE_PLAN_EXIST = "Found existing App Service Plan '%s' in Resource Group '%s'.";
    public static final String SERVICE_PLAN_CREATED = "Successfully created App Service Plan.";
    public static final String GENERATE_WEB_CONFIG_FAIL = "Failed to generate web.config file for JAR deployment.";
    public static final String READ_WEB_CONFIG_TEMPLATE_FAIL = "Failed to read the content of web.config.template.";
    public static final String GENERATING_WEB_CONFIG = "Generating web.config for Web App on Windows.";
    public static final String CONFIGURATION_NOT_APPLICABLE =
            "The configuration is not applicable for the target Web App (%s). Please correct it in pom.xml.";

    private static final String JAR_CMD = ":JAR_COMMAND:";
    private static final String JAR_COMMAND_PATTERN = " %s -Djava.net.preferIPv4Stack=true -Dserver.port=" +
            "%%HTTP_PLATFORM_PORT%% -jar &quot;%%HOME%%\\\\site\\\\wwwroot\\\\%s&quot;";

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

    public static void generateWebConfigFile(final DeployTarget deployTarget, final String jarFileName,
                                             final String stagingDirectoryPath, final Log log) throws IOException {
        log.info(GENERATING_WEB_CONFIG);
        final String templateContent;
        try (final InputStream is = WebAppUtils.class.getClassLoader()
                .getResourceAsStream("web.config.template")) {
            templateContent = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            log.error(READ_WEB_CONFIG_TEMPLATE_FAIL);
            throw e;
        }

        final String javaOpts = deployTarget.getAppSettings().containsKey("JAVA_OPTS") ?
                "%JAVA_OPTS%" : StringUtils.EMPTY;
        final String jarCommand = String.format(JAR_COMMAND_PATTERN, javaOpts, jarFileName);
        final String webConfigFile = templateContent.replaceAll(JAR_CMD, jarCommand);

        final File webConfig = new File(stagingDirectoryPath, "web.config");
        webConfig.createNewFile();

        try (final FileOutputStream fos = new FileOutputStream(webConfig)) {
            IOUtils.write(webConfigFile, fos, "UTF-8");
        } catch (Exception e) {
            log.error(GENERATE_WEB_CONFIG_FAIL);
        }
    }

    public static AppServicePlan getAppServicePlanByWebApp(final WebApp webApp) {
        return webApp.manager().appServicePlans().getById(webApp.appServicePlanId());
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
