/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MavenConfigUtils {
    private static final String POM = "pom";
    private static final String JAR = "jar";

    private static final String DEFAULT_DIRECTORY = "${project.basedir}/target";
    private static final String DEFAULT_INCLUDE = "*.jar";

    public static boolean isPomPackaging(MavenProject mavenProject) {
        return POM.equalsIgnoreCase(mavenProject.getPackaging());
    }

    public static boolean isJarPackaging(MavenProject mavenProject) {
        return JAR.equalsIgnoreCase(mavenProject.getPackaging());
    }

    public static void addResourcesConfig(Element root, List<Resource> resources) {
        final DOMElement resourceRootNode = new DOMElement("resources");
        for (final Resource resource : resources) {
            final DOMElement resourceNode = new DOMElement("resource");
            Optional.ofNullable(resource.getFiltering()).filter(StringUtils::isNoneBlank)
                    .ifPresent(value -> XmlUtils.addDomWithKeyValue(resourceNode, "filtering", value));
            Optional.ofNullable(resource.getTargetPath()).filter(StringUtils::isNoneBlank)
                .ifPresent(value -> XmlUtils.addDomWithKeyValue(resourceNode, "targetPath", value));
            Optional.ofNullable(resource.getDirectory()).filter(StringUtils::isNoneBlank)
                .ifPresent(value -> XmlUtils.addDomWithKeyValue(resourceNode, "directory", value));
            Optional.ofNullable(resource.getIncludes()).filter(CollectionUtils::isNotEmpty)
                .ifPresent(value -> XmlUtils.addDomWithValueList(resourceNode, "includes", "include", value));
            Optional.ofNullable(resource.getExcludes()).filter(CollectionUtils::isNotEmpty)
                .ifPresent(value -> XmlUtils.addDomWithValueList(resourceNode, "excludes", "exclude", value));

            resourceRootNode.add(resourceNode);
        }
        root.add(resourceRootNode);
    }

    public static List<Resource> getDefaultResources() {
        final Resource resource = new Resource();
        resource.setDirectory(DEFAULT_DIRECTORY);
        resource.addInclude(DEFAULT_INCLUDE);
        return Collections.singletonList(resource);
    }

    public static Xpp3Dom getPluginConfiguration(MavenProject proj, String pluginIdentifier) {
        return MavenUtils.getPluginConfiguration(proj, pluginIdentifier);
    }
}
