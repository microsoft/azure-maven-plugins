/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.microsoft.azure.maven.utils.MavenConfigUtils;
import com.microsoft.azure.maven.utils.PomUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ConfigurationUpdater {
    public static void updateAppConfigToPom(AppRawConfig config, MavenProject project, PluginDescriptor plugin) throws DocumentException, IOException {
        final File pom = project.getFile();
        final Element pluginConfigNode = PomUtils.getPluginConfigNode(plugin, pom);
        Element newNode = config != null ? createOrUpdateAppConfigNode(pluginConfigNode, config) : pluginConfigNode;

        // newly created nodes are not LocationAwareElement
        while (!(newNode.getParent() instanceof PomUtils.LocationAwareElement)) {
            newNode = newNode.getParent();
        }
        FileUtils.fileWrite(pom, PomUtils.formatNode(FileUtils.fileRead(pom), (PomUtils.LocationAwareElement) newNode.getParent(), newNode));
    }

    private static Element createOrUpdateAppConfigNode(Element pluginNode, AppRawConfig config) {
        final Element appConfigNode = PomUtils.getOrCreateNode(pluginNode, "configuration");
        PomUtils.updateNode(appConfigNode, ConfigurationUpdater.toMap(config));
        if (Objects.nonNull(config.getDeployment())) {
            createOrUpdateDeploymentConfigNode(appConfigNode, config.getDeployment());
        }
        return appConfigNode;
    }

    private static Element createOrUpdateDeploymentConfigNode(Element appConfigNode, AppDeploymentRawConfig config) {
        final Element deploymentConfigNode = PomUtils.getOrCreateNode(appConfigNode, "deployment");
        PomUtils.updateNode(deploymentConfigNode, ConfigurationUpdater.toMap(config));
        MavenConfigUtils.addResourcesConfig(deploymentConfigNode, MavenConfigUtils.getDefaultResources());
        return deploymentConfigNode;
    }

    public static Map<String, Object> toMap(AppRawConfig app) {
        return MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[]{
            new DefaultMapEntry<>("subscriptionId", app.getSubscriptionId()),
            new DefaultMapEntry<>("clusterName", app.getClusterName()),
            new DefaultMapEntry<>("appName", app.getAppName()),
            new DefaultMapEntry<>("isPublic", app.getIsPublic())
        });
    }

    public static Map<String, Object> toMap(AppDeploymentRawConfig deployment) {
        return MapUtils.putAll(new LinkedHashMap<>(), new Map.Entry[]{
            new DefaultMapEntry<>("cpu", deployment.getCpu()),
            new DefaultMapEntry<>("memoryInGB", deployment.getMemoryInGB()),
            new DefaultMapEntry<>("instanceCount", deployment.getInstanceCount()),
            new DefaultMapEntry<>("jvmOptions", deployment.getJvmOptions()),
            new DefaultMapEntry<>("runtimeVersion", deployment.getRuntimeVersion()),
        });
    }
}
