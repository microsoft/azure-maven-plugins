/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class MavenUtils {

    public static Xpp3Dom getPluginConfiguration(MavenProject mavenProject, String pluginKey) {
        final Plugin plugin = getPluginFromMavenModel(mavenProject.getModel(), pluginKey, true);
        return plugin == null ? null : (Xpp3Dom) plugin.getConfiguration();
    }

    public static Plugin getPluginFromMavenModel(Model model, String pluginKey, boolean firstSeen) {
        Plugin res = null;
        if (model.getBuild() == null) {
            return res;
        }
        for (final Plugin plugin: model.getBuild().getPlugins()) {
            if (pluginKey.equalsIgnoreCase(plugin.getKey())) {
                if (firstSeen) {
                    return plugin;
                }
                res = plugin;
            }
        }

        if (model.getBuild().getPluginManagement() == null) {
            return res;
        }
        for (final Plugin plugin: model.getBuild().getPluginManagement().getPlugins()) {
            if (pluginKey.equalsIgnoreCase(plugin.getKey())) {
                if (firstSeen) {
                    return plugin;
                }
                res = plugin;
            }
        }

        return res;
    }

    public static Plugin createPlugin(String groupId, String artifactId, String version) {
        final Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);

        return plugin;
    }

    private MavenUtils() {

    }

}
