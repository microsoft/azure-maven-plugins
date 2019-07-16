/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.serializer;

import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.utils.XMLUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoFailureException;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.util.List;

public abstract class ConfigurationSerializer {

    final WebAppConfiguration newConfigs;
    final WebAppConfiguration oldConfigs;

    public ConfigurationSerializer(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs) {
        this.newConfigs = newConfigs;
        // when there is no old configuration, just compare to an empty one
        this.oldConfigs = oldConfigs == null ? new WebAppConfiguration.Builder().build() : oldConfigs;
    }

    public abstract void saveToXML(Element document)
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

    protected void createOrUpdateAttribute(String attribute, String value, String oldValue, Element element) {
        // if value is not changed, just return , in case overwrite ${} properties
        if (value == null || value.equalsIgnoreCase(oldValue)) {
            return;
        } else {
            XMLUtils.setChildValue(attribute, value, element);
        }
    }
}
