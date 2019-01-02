/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.serializer;

import com.microsoft.azure.maven.appservice.PricingTierEnum;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.utils.XMLUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

public class V2ConfigurationSerializer extends ConfigurationSerializer {

    @Override
    public void saveToXML(WebAppConfiguration webAppConfiguration, Element configurationElement)
        throws MojoFailureException {
        createOrUpdateAttribute("schemaVersion", "V2", configurationElement);
        createOrUpdateAttribute("resourceGroup", webAppConfiguration.getResourceGroup(), configurationElement);
        createOrUpdateAttribute("appName", webAppConfiguration.getAppName(), configurationElement);
        createOrUpdateAttribute("region", webAppConfiguration.getRegion().name(), configurationElement);
        createOrUpdateAttribute("pricingTier",
            PricingTierEnum.getPricingTierStringByPricingTierObject(webAppConfiguration.getPricingTier()),
            configurationElement);

        XMLUtils.removeNode(configurationElement, "runtime");
        configurationElement.add(createRunTimeNode(webAppConfiguration));

        XMLUtils.removeNode(configurationElement, "deploymentSlot");
        if (webAppConfiguration.getDeploymentSlotSetting() != null) {
            configurationElement.add(createDeploymentSlotNode(webAppConfiguration.getDeploymentSlotSetting()));
        }

        // only add deployment if user didn't set it
        if (configurationElement.element("deployment") == null) {
            configurationElement.add(createDeploymentNode(webAppConfiguration));
        }
    }

    private DOMElement createRunTimeNode(WebAppConfiguration webAppConfiguration) throws MojoFailureException {
        switch (webAppConfiguration.getOs()) {
            case Linux:
                return createLinuxRunTimeNode(webAppConfiguration);
            case Windows:
                return createWindowsRunTimeNode(webAppConfiguration);
            case Docker:
                return createDockerRunTimeNode(webAppConfiguration);
            default:
                throw new MojoFailureException("The value of <os> is unknown.");
        }
    }

    private static DOMElement createLinuxRunTimeNode(WebAppConfiguration webAppConfiguration) {
        final DOMElement runtimeRoot = new DOMElement("runtime");
        runtimeRoot.add(XMLUtils.createSimpleElement("os", "linux"));
        runtimeRoot.add(XMLUtils.createSimpleElement("javaVersion", "jre8"));
        runtimeRoot.add(XMLUtils.createSimpleElement("webContainer",
            RuntimeSetting.getLinuxJavaVersionByRuntimeStack(webAppConfiguration.getRuntimeStack())));
        return runtimeRoot;
    }

    private static DOMElement createWindowsRunTimeNode(WebAppConfiguration webAppConfiguration) {
        final DOMElement runtimeRoot = new DOMElement("runtime");
        runtimeRoot.add(XMLUtils.createSimpleElement("os", "windows"));
        runtimeRoot.add(XMLUtils.createSimpleElement("javaVersion",
            webAppConfiguration.getJavaVersion().toString()));
        runtimeRoot.add(XMLUtils.createSimpleElement("webContainer",
            webAppConfiguration.getWebContainer().toString()));
        return runtimeRoot;
    }

    private static DOMElement createDockerRunTimeNode(WebAppConfiguration webAppConfiguration) {
        final DOMElement runtimeRoot = new DOMElement("runtime");
        runtimeRoot.add(XMLUtils.createSimpleElement("os", "docker"));
        runtimeRoot.add(XMLUtils.createSimpleElement("image", webAppConfiguration.getImage()));
        if (webAppConfiguration.getServerId() != null) {
            runtimeRoot.add(XMLUtils.createSimpleElement("serverId", webAppConfiguration.getServerId()));
        }
        if (webAppConfiguration.getRegistryUrl() != null) {
            runtimeRoot.add(XMLUtils.createSimpleElement("registryUrl", webAppConfiguration.getRegistryUrl()));
        }
        return runtimeRoot;
    }

    private DOMElement createDeploymentNode(WebAppConfiguration webAppConfiguration) {
        final DOMElement deploymentNode = new DOMElement("deployment");
        deploymentNode.add(createResourcesNode(webAppConfiguration.getResources()));
        return deploymentNode;
    }
}
