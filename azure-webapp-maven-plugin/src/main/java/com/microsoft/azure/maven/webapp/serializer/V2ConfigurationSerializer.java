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
import org.dom4j.dom.DOMElement;

public class V2ConfigurationSerializer extends XMLSerializer {

    @Override
    public DOMElement convertToXML(WebAppConfiguration webAppConfiguration) throws MojoFailureException {
        final DOMElement pluginConfigurationRoot = new DOMElement("configuration");

        pluginConfigurationRoot.add(XMLUtils.createSimpleElement("schemaVersion", "V2"));
        pluginConfigurationRoot.add(XMLUtils.createSimpleElement("resourceGroup",
            webAppConfiguration.getResourceGroup()));
        pluginConfigurationRoot.add(XMLUtils.createSimpleElement("appName", webAppConfiguration.getAppName()));
        pluginConfigurationRoot.add(XMLUtils.createSimpleElement("region",
            webAppConfiguration.getRegion().name()));
        pluginConfigurationRoot.add(XMLUtils.createSimpleElement("pricingTier",
            PricingTierEnum.getPricingTierStringByPricingTierObject(webAppConfiguration.getPricingTier())));
        pluginConfigurationRoot.add(createRunTimeNode(webAppConfiguration));
        pluginConfigurationRoot.add(createDeploymentNode(webAppConfiguration));
        if (webAppConfiguration.getDeploymentSlotSetting() != null) {
            pluginConfigurationRoot.add(createDeploymentSlotNode(webAppConfiguration.getDeploymentSlotSetting()));
        }
        return pluginConfigurationRoot;
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
                throw new MojoFailureException("");
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
        runtimeRoot.add(XMLUtils.createSimpleElement("image", webAppConfiguration.getImage()));
        runtimeRoot.add(XMLUtils.createSimpleElement("serverId", webAppConfiguration.getServerId()));
        runtimeRoot.add(XMLUtils.createSimpleElement("registryUrl", webAppConfiguration.getRegistryUrl()));
        return runtimeRoot;
    }

    private DOMElement createDeploymentNode(WebAppConfiguration webAppConfiguration) {
        final DOMElement deploymentNode = new DOMElement("deployment");
        deploymentNode.add(createResourcesNode(webAppConfiguration.getResources()));
        return deploymentNode;
    }
}
