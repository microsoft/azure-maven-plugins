/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.serializer;

import com.microsoft.azure.maven.appservice.PricingTierEnum;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.configuration.DeploymentSlotSetting;
import com.microsoft.azure.maven.webapp.configuration.RuntimeSetting;
import com.microsoft.azure.maven.webapp.utils.XMLUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

public class V2ConfigurationSerializer extends ConfigurationSerializer {

    public V2ConfigurationSerializer(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs) {
        super(newConfigs, oldConfigs);
    }

    @Override
    public void saveToXML(Element configurationElement)
        throws MojoFailureException {
        createOrUpdateAttribute("schemaVersion", "V2", oldConfigs.getSchemaVersion(), configurationElement);
        createOrUpdateAttribute("resourceGroup", newConfigs.getResourceGroup(),
            oldConfigs.getResourceGroup(), configurationElement);
        createOrUpdateAttribute("appName", newConfigs.getAppName(), oldConfigs.getAppName(), configurationElement);

        final String oldRegion = oldConfigs.getRegion() == null ? null : oldConfigs.getRegion().name();
        createOrUpdateAttribute("region", newConfigs.getRegion().name(), oldRegion, configurationElement);
        createOrUpdateAttribute("pricingTier",
            PricingTierEnum.getPricingTierStringByPricingTierObject(newConfigs.getPricingTier()),
            PricingTierEnum.getPricingTierStringByPricingTierObject(oldConfigs.getPricingTier()),
            configurationElement);

        if (newConfigs.getOs() != null) {
            updateRunTimeNode(newConfigs, oldConfigs, configurationElement);
        }

        // remove or update deploymentSlot node
        if (newConfigs.getDeploymentSlotSetting() == null) {
            XMLUtils.removeNode(configurationElement, "deploymentSlot");
        } else {
            final Element deploymentSlot = XMLUtils.getOrCreateSubElement("deploymentSlot", configurationElement);
            final DeploymentSlotSetting oldDeploymentSlotSetting = oldConfigs.getDeploymentSlotSetting() == null ?
                new DeploymentSlotSetting() : oldConfigs.getDeploymentSlotSetting();
            updateDeploymentSlotNode(newConfigs.getDeploymentSlotSetting(), oldDeploymentSlotSetting, deploymentSlot);
        }

        // only add deployment when init or convert
        if (configurationElement.element("deployment") == null && (newConfigs.getResources() != null)) {
            configurationElement.add(createDeploymentNode(newConfigs));
        }
    }

    private void updateRunTimeNode(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs,
                                   Element configurationElement) throws MojoFailureException {
        Element runtime = configurationElement.element("runtime");
        if (!(runtime != null && newConfigs.getOs().equals(oldConfigs.getOs()))) {
            XMLUtils.removeNode(configurationElement, "runtime");
            runtime = new DOMElement("runtime");
            configurationElement.add(runtime);
        }
        switch (newConfigs.getOs()) {
            case Linux:
                updateLinuxRunTimeNode(newConfigs, oldConfigs, runtime);
                break;
            case Windows:
                updateWindowsRunTimeNode(newConfigs, oldConfigs, runtime);
                break;
            case Docker:
                updateDockerRunTimeNode(newConfigs, oldConfigs, runtime);
                break;
            default:
                throw new MojoFailureException("The value of <os> is unknown.");
        }
    }

    private void updateLinuxRunTimeNode(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs,
                                        Element configurationElement) {
        final String oldOS = oldConfigs.getOs() == null ? null : oldConfigs.getOs().toString();
        createOrUpdateAttribute("os", "linux", oldOS, configurationElement);
        final String oldJavaVersion = oldConfigs.getJavaVersion() == null ? null :
            oldConfigs.getJavaVersion().toString();
        createOrUpdateAttribute("javaVersion", "jre8", oldJavaVersion, configurationElement);
        createOrUpdateAttribute("webContainer",
            RuntimeSetting.getLinuxWebContainerByRuntimeStack(newConfigs.getRuntimeStack())
            , RuntimeSetting.getLinuxWebContainerByRuntimeStack(oldConfigs.getRuntimeStack()), configurationElement);
    }

    private void updateWindowsRunTimeNode(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs,
                                          Element configurationElement) {

        final String oldOS = oldConfigs.getOs() == null ? null : oldConfigs.getOs().toString();
        createOrUpdateAttribute("os", "windows", oldOS, configurationElement);
        final String oldJavaVersion = oldConfigs.getJavaVersion() == null ? null :
            oldConfigs.getJavaVersion().toString();
        createOrUpdateAttribute("javaVersion", newConfigs.getJavaVersion().toString(),
            oldJavaVersion, configurationElement);
        final String oldWebContainer = oldConfigs.getWebContainer() == null ? null :
            oldConfigs.getWebContainer().toString();
        createOrUpdateAttribute("webContainer", newConfigs.getWebContainer().toString(), oldWebContainer,
            configurationElement);
    }

    private void updateDockerRunTimeNode(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs,
                                         Element configurationElement) {
        final String oldOS = oldConfigs.getOs() == null ? null : oldConfigs.getOs().toString();
        createOrUpdateAttribute("os", "docker", oldOS, configurationElement);
        createOrUpdateAttribute("image", newConfigs.getImage(), oldConfigs.getImage(), configurationElement);
        createOrUpdateAttribute("serverId", newConfigs.getServerId(), oldConfigs.getServerId(), configurationElement);
        createOrUpdateAttribute("registryUrl", newConfigs.getRegistryUrl(), oldConfigs.getRegistryUrl(),
            configurationElement);
    }

    protected void updateDeploymentSlotNode(DeploymentSlotSetting newConfigs, DeploymentSlotSetting oldConfigs,
                                            Element deploymentSlotRoot) {
        createOrUpdateAttribute("name", newConfigs.getName(), oldConfigs.getName(), deploymentSlotRoot);
        createOrUpdateAttribute("configurationSource", newConfigs.getConfigurationSource(),
            oldConfigs.getConfigurationSource(),
            deploymentSlotRoot);
    }

    private DOMElement createDeploymentNode(WebAppConfiguration webAppConfiguration) {
        final DOMElement deploymentNode = new DOMElement("deployment");
        deploymentNode.add(createResourcesNode(webAppConfiguration.getResources()));
        return deploymentNode;
    }
}
