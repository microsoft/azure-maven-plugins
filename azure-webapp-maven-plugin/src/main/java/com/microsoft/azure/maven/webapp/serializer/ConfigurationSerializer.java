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
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.util.List;

public abstract class ConfigurationSerializer {

    public abstract void saveToXML(WebAppConfiguration webAppConfiguration, Element document)
        throws MojoFailureException;

    protected DOMElement createResourcesNode(List<Resource> resources) {
        final DOMElement resourceRootNode = new DOMElement("resources");
        for (final Resource resource : resources) {
            final DOMElement resourceNode = new DOMElement("resource");

            XMLUtils.addNotEmptyElement(resourceNode, "filtering", resource.getFiltering());
            XMLUtils.addNotEmptyElement(resourceNode, "mergeId", resource.getMergeId());
            XMLUtils.addNotEmptyElement(resourceNode, "targetPath", resource.getTargetPath());
            XMLUtils.addNotEmptyElement(resourceNode, "directory", resource.getDirectory());
            XMLUtils.addNotEmptyListElement(resourceNode, "includes", "include", resource.getIncludes());
            XMLUtils.addNotEmptyListElement(resourceNode, "excludes", "exclude", resource.getExcludes());

            resourceRootNode.add(resourceNode);
        }
        return resourceRootNode;
    }

    protected DOMElement createDeploymentSlotNode(DeploymentSlotSetting deploymentSlotSetting) {
        final DOMElement deploymentSlotRoot = new DOMElement("deploymentSlot");
        deploymentSlotRoot.add(XMLUtils.createSimpleElement("name", deploymentSlotSetting.getName()));
        if (deploymentSlotSetting.getConfigurationSource() != null) {
            deploymentSlotRoot.add(XMLUtils.createSimpleElement("configurationSource",
                deploymentSlotSetting.getConfigurationSource()));
        }
        return deploymentSlotRoot;
    }

    protected void createOrUpdateAttribute(String attribute, String value, Element element) {
        if (element.element(attribute) == null) {
            element.add(XMLUtils.createSimpleElement(attribute, value));
        } else {
            element.element(attribute).setText(value);
        }
    }

}
