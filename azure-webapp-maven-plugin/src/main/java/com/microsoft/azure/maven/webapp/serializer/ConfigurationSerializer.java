/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.serializer;

import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.utils.XMLUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;
import org.dom4j.dom.DOMElement;

import java.util.List;

public abstract class ConfigurationSerializer {
    public abstract DOMElement convertToXML(WebAppConfiguration webAppConfiguration) throws MojoFailureException;

    protected DOMElement createResourcesNode(List<Resource> resources) {
        final DOMElement resourceRootNode = new DOMElement("resources");
        for (final Resource resource : resources) {
            final DOMElement resourceNode = new DOMElement("resource");
            if (StringUtils.isNotEmpty(resource.getFiltering())) {
                resourceNode.add(XMLUtils.createSimpleElement("filtering", resource.getFiltering()));
            }

            if (StringUtils.isNotEmpty(resource.getMergeId())) {
                resourceNode.add(XMLUtils.createSimpleElement("mergeId", resource.getMergeId()));
            }

            if (StringUtils.isNotEmpty(resource.getFiltering())) {
                resourceNode.add(XMLUtils.createSimpleElement("targetPath", resource.getTargetPath()));
            }

            if (StringUtils.isNotEmpty(resource.getDirectory())) {
                resourceNode.add(XMLUtils.createSimpleElement("directory", resource.getDirectory()));
            }

            if (resource.getIncludes() != null && resource.getIncludes().size() > 0) {
                resourceNode.add(XMLUtils.createListElement("includes", "include", resource.getIncludes()));
            }

            if (resource.getExcludes() != null && resource.getExcludes().size() > 0) {
                resourceNode.add(XMLUtils.createListElement("excludes", "exclude", resource.getExcludes()));
            }
            resourceRootNode.add(resourceNode);
        }
        return resourceRootNode;
    }

    protected DOMElement createDeploymentSlotNode(DeploymentSlotSetting deploymentSlotSetting) {
        final DOMElement deploymentSlotRoot = new DOMElement("deploymentSlot");
        deploymentSlotRoot.add(XMLUtils.createSimpleElement("name", deploymentSlotSetting.getName()));
        deploymentSlotRoot.add(XMLUtils.createSimpleElement("configurationSource",
            deploymentSlotSetting.getConfigurationSource()));
        return deploymentSlotRoot;
    }

}
