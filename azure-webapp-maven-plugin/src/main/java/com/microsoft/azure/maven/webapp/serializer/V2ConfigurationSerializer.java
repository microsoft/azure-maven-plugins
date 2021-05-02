/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.serializer;

import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import com.microsoft.azure.toolkit.lib.legacy.appservice.OperatingSystemEnum;
import com.microsoft.azure.toolkit.lib.legacy.appservice.AppServiceUtils;
import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.utils.JavaVersionUtils;
import com.microsoft.azure.maven.webapp.utils.RuntimeStackUtils;
import com.microsoft.azure.maven.webapp.utils.WebContainerUtils;
import com.microsoft.azure.maven.webapp.utils.XMLUtils;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

public class V2ConfigurationSerializer extends ConfigurationSerializer {

    public V2ConfigurationSerializer(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs) {
        super(newConfigs, oldConfigs);
    }

    @Override
    public void saveToXML(Element configurationElement)
        throws MojoFailureException {
        createOrUpdateAttribute("schemaVersion", "v2", oldConfigs.getSchemaVersion(), configurationElement);
        createOrUpdateAttribute("subscriptionId", newConfigs.getSubscriptionId(), oldConfigs.getSubscriptionId(), configurationElement);
        createOrUpdateAttribute("resourceGroup", newConfigs.getResourceGroup(),
            oldConfigs.getResourceGroup(), configurationElement);
        createOrUpdateAttribute("appName", newConfigs.getAppName(), oldConfigs.getAppName(), configurationElement);
        createOrUpdateAttribute("pricingTier",
                AppServiceUtils.convertPricingTierToString(newConfigs.getPricingTier()),
                AppServiceUtils.convertPricingTierToString(oldConfigs.getPricingTier()),
                configurationElement);

        if (newConfigs.getRegion() != null) {
            final String oldRegion = oldConfigs.getRegion() == null ? null : oldConfigs.getRegion().name();
            createOrUpdateAttribute("region", newConfigs.getRegion().name(), oldRegion, configurationElement);
        }

        createOrUpdateAttribute("appServicePlanName", newConfigs.getServicePlanName(), oldConfigs.getServicePlanName(), configurationElement);
        createOrUpdateAttribute("appServicePlanResourceGroup", newConfigs.getServicePlanResourceGroup(),
                oldConfigs.getServicePlanResourceGroup(), configurationElement);

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
        final Element runtime = XMLUtils.getOrCreateSubElement("runtime", configurationElement);
        if (!newConfigs.getOs().equals(oldConfigs.getOs())) {
            XMLUtils.clearNode(runtime);
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
        final String oldOS = formatOperationSystem(oldConfigs.getOs());
        createOrUpdateAttribute("os", "Linux", oldOS, configurationElement);
        if (newConfigs.getRuntimeStack() != null) {
            final String oldJavaVersion = oldConfigs.getRuntimeStack() == null ? null :
                    RuntimeStackUtils.getJavaVersionFromRuntimeStack(oldConfigs.getRuntimeStack());
            createOrUpdateAttribute("javaVersion",
                    RuntimeStackUtils.getJavaVersionFromRuntimeStack(newConfigs.getRuntimeStack()), oldJavaVersion,
                    configurationElement);

            final String oldWebContainer = oldConfigs.getRuntimeStack() == null ? null :
                    RuntimeStackUtils.getWebContainerFromRuntimeStack(oldConfigs.getRuntimeStack());
            createOrUpdateAttribute("webContainer",
                    RuntimeStackUtils.getWebContainerFromRuntimeStack(newConfigs.getRuntimeStack())
                    , oldWebContainer, configurationElement);
        }
    }

    private void updateWindowsRunTimeNode(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs,
                                          Element configurationElement) {

        final String oldOS = formatOperationSystem(oldConfigs.getOs());
        createOrUpdateAttribute("os", "Windows", oldOS, configurationElement);
        if (newConfigs.getJavaVersion() != null) {
            final String oldJavaVersion = oldConfigs.getJavaVersion() == null ? null :
                    JavaVersionUtils.formatJavaVersion(oldConfigs.getJavaVersion());
            createOrUpdateAttribute("javaVersion", JavaVersionUtils.formatJavaVersion(newConfigs.getJavaVersion()),
                    oldJavaVersion, configurationElement);
        }
        if (newConfigs.getWebContainer() != null) {
            final String oldWebContainer = WebContainerUtils.formatWebContainer(oldConfigs.getWebContainer());
            createOrUpdateAttribute("webContainer", WebContainerUtils.formatWebContainer(newConfigs.getWebContainer()), oldWebContainer,
                    configurationElement);
        }
    }

    private void updateDockerRunTimeNode(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs,
                                         Element configurationElement) {
        final String oldOS = formatOperationSystem(oldConfigs.getOs());
        createOrUpdateAttribute("os", "Docker", oldOS, configurationElement);
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

    private static String formatOperationSystem(OperatingSystemEnum osEnum) {
        return osEnum == null ? null : StringUtils.capitalise(osEnum.toString());
    }
}
