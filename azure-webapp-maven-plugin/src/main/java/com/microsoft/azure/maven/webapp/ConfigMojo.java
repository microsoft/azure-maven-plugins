/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.maven.appservice.PricingTierEnum;
import com.microsoft.azure.maven.queryer.MavenPluginQueryer;
import com.microsoft.azure.maven.queryer.QueryFactory;
import com.microsoft.azure.maven.webapp.configuration.Deployment;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.configuration.OperatingSystemEnum;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.utils.WebAppPomHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "config")
public class ConfigMojo extends AbstractWebAppMojo {

    public static final String NOT_EMPTY_REGEX = "[\\s\\S]+";

    private MavenPluginQueryer queryer;
    private WebAppPomHandler pomHandler;
    private static final String[] configTypes = {"WebApp", "Runtime", "Slot"};

    @Override
    protected void doExecute() throws Exception {
        queryer = QueryFactory.getQueryer(settings, getLog());
        pomHandler = new WebAppPomHandler("pom.xml");

        WebAppConfiguration configuration;
        if (pomHandler.hasConfiguration()) {
            configuration = config(getWebAppConfiguration());
        } else {
            configuration = initConfig();
        }
        while (!confirmConfiguration(configuration)) {
            configuration = config(configuration);
        }

        pomHandler.savePluginConfiguration(configuration);
        queryer.close();
    }

    protected WebAppConfiguration initConfig() throws MojoFailureException, MojoExecutionException {
        final WebAppConfiguration result = getDefaultConfiguration();
        return getRuntimeConfiguration(result);
    }

    private WebAppConfiguration getDefaultConfiguration() throws MojoExecutionException {
        final WebAppConfiguration.Builder builder = new WebAppConfiguration.Builder();
        final String defaultName = getProject().getArtifactId() + "-" + System.currentTimeMillis();
        final String resourceGroup = defaultName + "-rg";
        final String defaultSchemaVersion = "V2";
        final Region defaultRegion = Region.US_WEST2;
        final PricingTierEnum pricingTierEnum = PricingTierEnum.P1V2;
        return builder.appName(defaultName)
            .resourceGroup(resourceGroup)
            .region(defaultRegion)
            .pricingTier(pricingTierEnum.toPricingTier())
            .resources(Deployment.getDefaultDeploymentConfiguration(getProject().getPackaging()).getResources())
            .schemaVersion(defaultSchemaVersion)
            .build();
    }

    protected boolean confirmConfiguration(WebAppConfiguration configuration) throws MojoExecutionException,
        MojoFailureException {
        System.out.println("Please confirm webapp properties");
        System.out.println("AppName : " + configuration.getAppName());
        System.out.println("ResourceGroup : " + configuration.getResourceGroup());
        System.out.println("Region : " + configuration.getRegion());
        System.out.println("PricingTier : " + configuration.getPricingTier());
        System.out.println("OS : " + configuration.getOs());
        switch (configuration.getOs()) {
            case Windows:
                System.out.println("Java : " + configuration.getJavaVersion());
                System.out.println("WebContainer : " + configuration.getWebContainer());
                break;
            case Linux:
                System.out.println("RuntimeStack : " + configuration.getRuntimeStack());
                break;
            case Docker:
                System.out.println("Image : " + configuration.getImage());
                System.out.println("ServerId : " + configuration.getServerId());
                System.out.println("RegistryUrl : " + configuration.getRegistryUrl());
                break;
            default:
                throw new MojoExecutionException("The value of <os> is unknown.");
        }
        final String result = queryer.assureInputFromUser("confirm", "Y", null, "Confirm?(Y/N)", null);
        return result.equalsIgnoreCase("Y");
    }

    protected WebAppConfiguration config(WebAppConfiguration configuration)
        throws MojoFailureException, MojoExecutionException {
        final String selection = queryer.assureInputFromUser("selection", "webapp", Arrays.asList(configTypes),
            "Please choose which part to config");
        switch (selection) {
            case "WebApp":
                return getWebAppConfiguration(configuration);
            case "Runtime":
                return getRuntimeConfiguration(configuration);
            case "Slot":
                return getSlotConfiguration(configuration);
            default:
                throw new MojoExecutionException("Unknow webapp setting");
        }
    }

    private WebAppConfiguration getWebAppConfiguration(WebAppConfiguration configuration)
        throws MojoFailureException, MojoExecutionException {
        final WebAppConfiguration.Builder builder = configuration.getBuilderFromConfiguration();

        final String defaultAppName = getDefaultValue(configuration.appName, getProject().getArtifactId());
        final String appName = queryer.assureInputFromUser("appName", defaultAppName,
            NOT_EMPTY_REGEX, null, null);

        final String defaultResourceGroup = getDefaultValue(configuration.resourceGroup,
            String.format("%s-rg", appName));
        final String resourceGroup = queryer.assureInputFromUser("resourceGroup",
            defaultResourceGroup,
            NOT_EMPTY_REGEX, null, null);

        final String defaultRegion = configuration.region != null ? configuration.region.name() :
            Region.US_EAST2.name();
        final String region = queryer.assureInputFromUser("region", defaultRegion, NOT_EMPTY_REGEX,
            null, null);
        final String pricingTier = queryer.assureInputFromUser("pricingTier", PricingTierEnum.P1V2
            , null);

        return builder.appName(appName)
            .resourceGroup(resourceGroup)
            .region(Region.fromName(region))
            .pricingTier(PricingTierEnum.valueOf(pricingTier).toPricingTier())
            .resources(resources).build();
    }

    private WebAppConfiguration getSlotConfiguration(WebAppConfiguration configuration)
        throws MojoFailureException {
        final WebAppConfiguration.Builder builder = configuration.getBuilderFromConfiguration();

        final DeploymentSlotSetting deploymentSlotSetting = configuration.getDeploymentSlotSetting();
        final String defaultIsSlotDeploy = deploymentSlotSetting == null ? "false" : "true";
        final String isSlotDeploy = queryer.assureInputFromUser("isSlotDeploy", defaultIsSlotDeploy,
            Arrays.asList("true", "false"), null);
        if (isSlotDeploy.equals("false")) {
            return builder.deploymentSlotSetting(null).build();
        }

        final String defaultSlotName = deploymentSlotSetting == null ? String.format("%s-slot", builder.getAppName()) :
            deploymentSlotSetting.getName();
        final String slotName = queryer.assureInputFromUser("slotName", defaultSlotName, NOT_EMPTY_REGEX,
            null, null);

        final String defaultConfigurationSource = deploymentSlotSetting == null ? null :
            deploymentSlotSetting.getConfigurationSource();
        final String configurationSource = queryer.assureInputFromUser("configurationSource",
            defaultConfigurationSource, null, null);

        final DeploymentSlotSetting result = new DeploymentSlotSetting();
        result.setName(slotName);
        result.setConfigurationSource(configurationSource);
        return builder.deploymentSlotSetting(result).build();
    }

    private WebAppConfiguration getRuntimeConfiguration(WebAppConfiguration configuration)
        throws MojoFailureException, MojoExecutionException {
        WebAppConfiguration.Builder builder = configuration.getBuilderFromConfiguration();

        final String os = queryer.assureInputFromUser("os", OperatingSystemEnum.Linux, null);
        builder.os(OperatingSystemEnum.fromString(os));
        switch (os.toLowerCase()) {
            case "linux":
                builder = getRuntimeConfigurationOfLinux(builder, configuration);
                break;
            case "windows":
                builder = getRuntimeConfigurationOfWindows(builder, configuration);
                break;
            case "docker":
                builder = getRuntimeConfigurationOfDocker(builder, configuration);
                break;
            default:
                throw new MojoExecutionException("The value of <os> is unknown.");
        }
        return builder.build();
    }

    private WebAppConfiguration.Builder getRuntimeConfigurationOfLinux(WebAppConfiguration.Builder builder,
                                                                       WebAppConfiguration configuration)
        throws MojoFailureException {
        final String defaultLinuxRuntimeStack = getDefaultValue(
            RuntimeSetting.getLinuxJavaVersionByRuntimeStack(configuration.getRuntimeStack()),
            RuntimeSetting.getDefaultLinuxRuntimeStack());
        final String runtimeStack = queryer.assureInputFromUser("runtimeStack",
            defaultLinuxRuntimeStack, RuntimeSetting.getValidLinuxRuntime(), null);
        return builder.runtimeStack(RuntimeSetting.getLinuxRuntimeStackByJavaVersion(runtimeStack));
    }

    private WebAppConfiguration.Builder getRuntimeConfigurationOfWindows(WebAppConfiguration.Builder builder,
                                                                         WebAppConfiguration configuration)
        throws MojoFailureException {
        final String defaultJavaVersion = configuration.getJavaVersion() == null ?
            JavaVersion.JAVA_ZULU_1_8_0_144.toString() : configuration.getJavaVersion().toString();
        final String javaVersion = queryer.assureInputFromUser("javaVersion",
            defaultJavaVersion, getValidJavaVersion(), null);

        final String defaultWebContainer = configuration.getWebContainer() == null ? null :
            configuration.getWebContainer().toString();
        final String webContainer = queryer.assureInputFromUser("webContainer",
            defaultWebContainer, getValidWebContainer(), null);
        return builder.javaVersion(JavaVersion.fromString(javaVersion))
            .webContainer(WebContainer.fromString(webContainer));
    }

    private WebAppConfiguration.Builder getRuntimeConfigurationOfDocker(WebAppConfiguration.Builder builder,
                                                                        WebAppConfiguration configuration)
        throws MojoFailureException {
        final String image = queryer.assureInputFromUser("image", configuration.image,
            NOT_EMPTY_REGEX, null, null);
        final String serverId = queryer.assureInputFromUser("serverId", configuration.serverId,
            null, null, null);
        final String registryUrl = queryer.assureInputFromUser("registryUrl", configuration.registryUrl,
            null, null, null);
        return builder.image(image)
            .serverId(serverId)
            .registryUrl(registryUrl);
    }

    public static List<String> getValidJavaVersion() {
        final List<String> result = new ArrayList<>();
        for (final JavaVersion javaVersion : JavaVersion.values()) {
            if (!javaVersion.toString().equals("null")) {
                result.add(javaVersion.toString());
            }
        }
        return result;
    }

    public static List<String> getValidWebContainer() {
        final List<String> result = new ArrayList<>();
        for (final WebContainer webContainer : WebContainer.values()) {
            result.add(webContainer.toString());
        }
        return result;
    }

    private String getDefaultValue(String defaultValue, String fallBack) {
        return StringUtils.isNotEmpty(defaultValue) ? defaultValue : fallBack;
    }

}
