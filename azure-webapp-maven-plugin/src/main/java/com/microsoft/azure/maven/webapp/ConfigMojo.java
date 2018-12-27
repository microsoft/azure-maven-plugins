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
import com.microsoft.azure.maven.webapp.serializer.V2ConfigurationSerializer;
import com.microsoft.azure.maven.webapp.serializer.XMLSerializer;
import com.microsoft.azure.maven.webapp.utils.PomUtils;
import com.microsoft.azure.maven.webapp.utils.XMLUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.dom.DOMElement;
import org.dom4j.tree.AbstractElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "config")
public class ConfigMojo extends AbstractWebAppMojo {

    public static final String PLUGIN_GROUPID = "com.microsoft.azure";
    public static final String PLUGIN_ARTIFACTID = "azure-webapp-maven-plugin";
    public static final String DEFAULT_INPUT_ERROR_MESSAGE = "Invalid input, please check and try again.";
    public static final String NOT_EMPTY_REGEX = "[\\s\\S]+";

    private MavenPluginQueryer queryer;

    @Override
    protected void doExecute() throws Exception {
        queryer = QueryFactory.getQueryer(settings, getLog());

        WebAppConfiguration configurations = getConfigurationFromPom();
        configurations = getConfigurationFromUser(configurations);
        updatePom(configurations);

        queryer.close();
    }

    protected void updatePom(WebAppConfiguration configurations) throws IOException, MojoFailureException,
        DocumentException {
        // Serialize config to xml node
        final XMLSerializer serializer = new V2ConfigurationSerializer();
        final DOMElement newConfigurationNode = serializer.convertToXML(configurations);

        // Combine or create plugin node and save
        final Document pomModel = PomUtils.getModelFromPomFile("pom.xml");
        final Element buildNode = XMLUtils.getOrCreateSubElement("build", pomModel.getRootElement());
        final Element pluginsRootNode = XMLUtils.getOrCreateSubElement("plugins", buildNode);
        final Element pluginNode = getOrCreateMavenPlguinNodeFromPlugins(pluginsRootNode);
        final Element configuration = pluginNode.element("configuration");
        if (configuration == null) {
            pluginNode.add(newConfigurationNode);
        } else {
            XMLUtils.combineXMLNode(configuration, newConfigurationNode);
        }
        uniqueNamespace(configuration, pluginNode.getNamespace());
        PomUtils.saveModelToPomFile(pomModel, "pom.xml");
    }

    private void uniqueNamespace(Element element, Namespace namespace) {
        if (element instanceof AbstractElement) {
            ((AbstractElement) element).setNamespace(namespace);
            for (final Element child : element.elements()) {
                uniqueNamespace(child, namespace);
            }
        }
    }

    private Element getOrCreateMavenPlguinNodeFromPlugins(Element plugins) {
        for (final Element element : plugins.elements()) {
            final String groupId = XMLUtils.getChildValue("groupId", element);
            final String artifactId = XMLUtils.getChildValue("artifactId", element);
            if (PLUGIN_GROUPID.equals(groupId) && PLUGIN_ARTIFACTID.equals(artifactId)) {
                return element;
            }
        }
        final Element result = createNewMavenPluginNode();
        plugins.add(result);
        return result;
    }

    private Element createNewMavenPluginNode() {
        final Element result = new DOMElement("plugin");
        result.add(XMLUtils.createSimpleElement("groupId", PLUGIN_GROUPID));
        result.add(XMLUtils.createSimpleElement("artifactId", PLUGIN_ARTIFACTID));
        return result;
    }

    protected WebAppConfiguration getConfigurationFromPom() {
        WebAppConfiguration configuration;
        try {
            configuration = getWebAppConfiguration();
        } catch (MojoExecutionException e) {
            // There are no valid config in project pom, create empty one
            configuration = new WebAppConfiguration.Builder().build();
        }
        return configuration;
    }

    protected WebAppConfiguration getConfigurationFromUser(WebAppConfiguration configuration)
        throws MojoFailureException, MojoExecutionException {
        final String defaultAppName = getDefaultValue(configuration.appName, getProject().getArtifactId());
        final String appName = queryer.assureInputFromUser("appName", defaultAppName,
            NOT_EMPTY_REGEX, DEFAULT_INPUT_ERROR_MESSAGE);
        final String defaultResourceGroup = getDefaultValue(configuration.resourceGroup,
            String.format("%s-rg", appName));
        final String resourceGroup = queryer.assureInputFromUser("resourceGroup",
            defaultResourceGroup,
            NOT_EMPTY_REGEX, DEFAULT_INPUT_ERROR_MESSAGE);
        final String defaultRegion = configuration.region != null ? configuration.region.name() :
            Region.US_EAST2.name();
        final String region = queryer.assureInputFromUser("region", defaultRegion, NOT_EMPTY_REGEX,
            DEFAULT_INPUT_ERROR_MESSAGE);
        final String pricingTier = queryer.assureInputFromUser("pricingTier", PricingTierEnum.P1v2);

        List<Resource> resources = configuration.getResources();
        if (resources == null || resources.size() == 0) {
            resources = Deployment.getDefaultDeploymentConfiguration(getProject().getPackaging()).getResources();
        }
        final WebAppConfiguration.Builder builder = new WebAppConfiguration.Builder();
        builder.appName(appName)
            .resourceGroup(resourceGroup)
            .region(Region.fromName(region))
            .pricingTier(PricingTierEnum.valueOf(pricingTier).toPricingTier())
            .resources(resources);

        getRuntimeConfiguration(builder, configuration);
        getSlotConfiguration(builder, configuration);
        return builder.build();
    }

    private WebAppConfiguration.Builder getSlotConfiguration(WebAppConfiguration.Builder builder,
                                                             WebAppConfiguration configuration)
        throws MojoFailureException {
        final DeploymentSlotSetting deploymentSlotSetting = configuration.getDeploymentSlotSetting();
        final String defaultIsSlotDeploy = deploymentSlotSetting == null ? "false" : "true";
        final String isSlotDeploy = queryer.assureInputFromUser("isSlotDeploy", defaultIsSlotDeploy,
            Arrays.asList("true", "false"));
        if (isSlotDeploy.equals("false")) {
            return builder;
        }
        final String defaultSlotName = deploymentSlotSetting == null ? String.format("%s-slot", builder.getAppName()) :
            deploymentSlotSetting.getName();
        final String slotName = queryer.assureInputFromUser("slotName", defaultSlotName, NOT_EMPTY_REGEX,
            DEFAULT_INPUT_ERROR_MESSAGE);

        final String defaultConfigurationSource = deploymentSlotSetting == null ? null :
            deploymentSlotSetting.getConfigurationSource();
        final String configurationSource = queryer.assureInputFromUser("configurationSource",
            defaultConfigurationSource, null, DEFAULT_INPUT_ERROR_MESSAGE);

        final DeploymentSlotSetting result = new DeploymentSlotSetting();
        result.setName(slotName);
        result.setConfigurationSource(configurationSource);
        return builder.deploymenySlotSetting(result);
    }

    private WebAppConfiguration.Builder getRuntimeConfiguration(WebAppConfiguration.Builder builder,
                                                                WebAppConfiguration configuration)
        throws MojoFailureException, MojoExecutionException {
        final String os = queryer.assureInputFromUser("os", OperatingSystemEnum.Linux);
        builder.os(OperatingSystemEnum.fromString(os));
        switch (os.toLowerCase()) {
            case "linux":
                return getRuntimeConfigurationOfLinux(builder, configuration);
            case "windows":
                return getRuntimeConfigurationOfWindows(builder, configuration);
            case "docker":
                return getRuntimeConfigurationOfDocker(builder, configuration);
            default:
                throw new MojoExecutionException("The value of <os> is unknown.");
        }
    }

    private WebAppConfiguration.Builder getRuntimeConfigurationOfLinux(WebAppConfiguration.Builder builder,
                                                                       WebAppConfiguration configuration)
        throws MojoFailureException {
        final String runtimeStack = queryer.assureInputFromUser("runtimeStack",
            RuntimeSetting.getDefaultLinuxRuntimeStack(),
            RuntimeSetting.getValidLinuxRuntime());
        return builder.runtimeStack(RuntimeSetting.getLinuxRuntimeStackByJavaVersion(runtimeStack));
    }

    private WebAppConfiguration.Builder getRuntimeConfigurationOfWindows(WebAppConfiguration.Builder builder,
                                                                         WebAppConfiguration configuration)
        throws MojoFailureException {
        final String defaultJavaVersion = configuration.getJavaVersion() == null ?
            JavaVersion.JAVA_ZULU_1_8_0_144.toString() : configuration.getJavaVersion().toString();
        final String javaVersion = queryer.assureInputFromUser("javaVersion",
            defaultJavaVersion, getValidJavaVersion());

        final String defaultWebContainer = configuration.getWebContainer() == null ? null :
            configuration.getWebContainer().toString();
        final String webContainer = queryer.assureInputFromUser("webContainer",
            defaultWebContainer, getValidWebContainer());
        return builder.javaVersion(JavaVersion.fromString(javaVersion))
            .webContainer(WebContainer.fromString(webContainer));
    }

    private WebAppConfiguration.Builder getRuntimeConfigurationOfDocker(WebAppConfiguration.Builder builder,
                                                                        WebAppConfiguration configuration)
        throws MojoFailureException {
        final String image = queryer.assureInputFromUser("image", configuration.image,
            NOT_EMPTY_REGEX, DEFAULT_INPUT_ERROR_MESSAGE);
        final String serverId = queryer.assureInputFromUser("serverId", configuration.serverId,
            null, DEFAULT_INPUT_ERROR_MESSAGE);
        final String registryUrl = queryer.assureInputFromUser("registryUrl", configuration.registryUrl,
            null, DEFAULT_INPUT_ERROR_MESSAGE);
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
