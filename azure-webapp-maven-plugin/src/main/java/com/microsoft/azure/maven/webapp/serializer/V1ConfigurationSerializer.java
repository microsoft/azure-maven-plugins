/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.serializer;

import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.maven.appservice.PricingTierEnum;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.utils.XMLUtils;

import org.apache.maven.plugin.MojoFailureException;
import org.dom4j.dom.DOMElement;

import java.util.HashMap;
import java.util.Map;

public class V1ConfigurationSerializer extends ConfigurationSerializer {

    private static final Map<RuntimeStack, String> linuxRuntimeStackMap = new HashMap<>();

    static {
        linuxRuntimeStackMap.put(RuntimeStack.TOMCAT_8_5_JRE8, "tomcat 8.5-jre8");
        linuxRuntimeStackMap.put(RuntimeStack.TOMCAT_9_0_JRE8, "tomcat 9.0-jre8");
        linuxRuntimeStackMap.put(RuntimeStack.WILDFLY_14_JRE8, "wildfly 14-jre8");
        linuxRuntimeStackMap.put(RuntimeStack.JAVA_8_JRE8, "jre8");
    }

    @Override
    public DOMElement convertToXML(WebAppConfiguration webAppConfiguration) throws MojoFailureException {
        final DOMElement pluginConfigurationRoot = new DOMElement("configuration");

        pluginConfigurationRoot.add(XMLUtils.createSimpleElement("schemaVersion", "V1"));
        pluginConfigurationRoot.add(XMLUtils.createSimpleElement("resourceGroup",
                webAppConfiguration.getResourceGroup()));
        pluginConfigurationRoot.add(XMLUtils.createSimpleElement("appName", webAppConfiguration.getAppName()));

        if (webAppConfiguration.getRegion() != null) {
            pluginConfigurationRoot.add(XMLUtils.createSimpleElement("region",
                    webAppConfiguration.getRegion().name()));
        }
        if (webAppConfiguration.getPricingTier() != null) {
            pluginConfigurationRoot.add(XMLUtils.createSimpleElement("pricingTier",
                    PricingTierEnum.getPricingTierStringByPricingTierObject(webAppConfiguration.getPricingTier())));
        }
        if (webAppConfiguration.getDeploymentSlotSetting() != null) {
            pluginConfigurationRoot.add(createDeploymentSlotNode(webAppConfiguration.getDeploymentSlotSetting()));
        }
        coinfigRuntime(webAppConfiguration, pluginConfigurationRoot);
        return pluginConfigurationRoot;
    }

    private void coinfigRuntime(WebAppConfiguration webAppConfiguration, DOMElement domElement)
            throws MojoFailureException {
        switch (webAppConfiguration.getOs()) {
            case Linux:
                configLinuxRunTime(webAppConfiguration, domElement);
                break;
            case Windows:
                configWindowsRunTime(webAppConfiguration, domElement);
                break;
            case Docker:
                configDockerRunTime(webAppConfiguration, domElement);
                break;
            default:
                throw new MojoFailureException("The value of <os> is unknown.");
        }
    }

    private void configWindowsRunTime(WebAppConfiguration webAppConfiguration, DOMElement domElement) {
        domElement.add(XMLUtils.createSimpleElement("javaVersion",
                webAppConfiguration.getJavaVersion().toString()));
        domElement.add(XMLUtils.createSimpleElement("webContainer",
                webAppConfiguration.getWebContainer().toString()));
    }

    private void configLinuxRunTime(WebAppConfiguration webAppConfiguration, DOMElement domElement)
            throws MojoFailureException {
        domElement.add(XMLUtils.createSimpleElement("linuxRuntime",
                linuxRuntimeStackMap.get(webAppConfiguration.getRuntimeStack())));
    }

    private void configDockerRunTime(WebAppConfiguration webAppConfiguration, DOMElement domElement) {
        domElement.add(XMLUtils.createSimpleElement("image",
                webAppConfiguration.getImage()));
        domElement.add(XMLUtils.createSimpleElement("serverId",
                webAppConfiguration.getServerId()));
        domElement.add(XMLUtils.createSimpleElement("registryUrl",
                webAppConfiguration.getRegistryUrl()));
    }
}
