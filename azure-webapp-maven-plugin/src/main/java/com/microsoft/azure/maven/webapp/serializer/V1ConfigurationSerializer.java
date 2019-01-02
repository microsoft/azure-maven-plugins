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
import org.dom4j.Element;

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
    public void saveToXML(WebAppConfiguration webAppConfiguration, Element configurationElement)
        throws MojoFailureException {
        createOrUpdateAttribute("schemaVersion", "V1", configurationElement);
        createOrUpdateAttribute("resourceGroup", webAppConfiguration.getResourceGroup(), configurationElement);
        createOrUpdateAttribute("appName", webAppConfiguration.getAppName(), configurationElement);
        createOrUpdateAttribute("region", webAppConfiguration.getRegion().name(), configurationElement);
        createOrUpdateAttribute("pricingTier",
            PricingTierEnum.getPricingTierStringByPricingTierObject(webAppConfiguration.getPricingTier()),
            configurationElement);

        coinfigRuntime(webAppConfiguration, configurationElement);

        XMLUtils.removeNode(configurationElement, "deploymentSlot");
        if (webAppConfiguration.getDeploymentSlotSetting() != null) {
            configurationElement.add(createDeploymentSlotNode(webAppConfiguration.getDeploymentSlotSetting()));
        }
    }

    private void coinfigRuntime(WebAppConfiguration webAppConfiguration, Element domElement)
        throws MojoFailureException {
        // remove old runtime configs
        removeRuntimeConfigs(domElement);
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

    private void removeRuntimeConfigs(Element domElement) {
        XMLUtils.removeNode(domElement, "javaVersion");
        XMLUtils.removeNode(domElement, "webContainer");
        XMLUtils.removeNode(domElement, "linuxRuntime");
        XMLUtils.removeNode(domElement, "image");
        XMLUtils.removeNode(domElement, "serverId");
        XMLUtils.removeNode(domElement, "registryUrl");
    }

    private void configWindowsRunTime(WebAppConfiguration webAppConfiguration, Element domElement) {
        domElement.add(XMLUtils.createSimpleElement("javaVersion",
            webAppConfiguration.getJavaVersion().toString()));
        domElement.add(XMLUtils.createSimpleElement("webContainer",
            webAppConfiguration.getWebContainer().toString()));
    }

    private void configLinuxRunTime(WebAppConfiguration webAppConfiguration, Element domElement)
        throws MojoFailureException {
        domElement.add(XMLUtils.createSimpleElement("linuxRuntime",
            linuxRuntimeStackMap.get(webAppConfiguration.getRuntimeStack())));
    }

    private void configDockerRunTime(WebAppConfiguration webAppConfiguration, Element domElement) {
        domElement.add(XMLUtils.createSimpleElement("image",
            webAppConfiguration.getImage()));
        domElement.add(XMLUtils.createSimpleElement("serverId",
            webAppConfiguration.getServerId()));
        domElement.add(XMLUtils.createSimpleElement("registryUrl",
            webAppConfiguration.getRegistryUrl()));
    }

}
