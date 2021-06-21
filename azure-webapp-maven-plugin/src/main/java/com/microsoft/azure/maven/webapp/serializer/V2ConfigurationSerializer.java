/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.serializer;

import com.microsoft.azure.maven.webapp.WebAppConfiguration;
import com.microsoft.azure.maven.webapp.utils.XMLUtils;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import org.apache.maven.plugin.MojoFailureException;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.util.Objects;

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
                newConfigs.getPricingTier(),
                oldConfigs.getPricingTier(),
                configurationElement);

        if (newConfigs.getRegion() != null) {
            final String oldRegion = oldConfigs.getRegion() == null ? null : oldConfigs.getRegion().toString();
            createOrUpdateAttribute("region", newConfigs.getRegion().toString(), oldRegion, configurationElement);
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
        if (!newConfigs.getOs().equals(oldConfigs.getOs()) && isDockerOs(newConfigs.getOs()) != isDockerOs(oldConfigs.getOs())) {
            // clear docker and non-docker related configurations for new runtime
            if (isDockerOs(newConfigs.getOs())) {
                XMLUtils.removeSubNode(runtime, "javaVersion");
                XMLUtils.removeSubNode(runtime, "webContainer");
            } else {
                XMLUtils.removeSubNode(runtime, "image");
                XMLUtils.removeSubNode(runtime, "serverId");
                XMLUtils.removeSubNode(runtime, "registryUrl");
            }
        }

        switch (newConfigs.getOs()) {
            case LINUX:
            case WINDOWS:
                updateNonDockerRunTimeNode(newConfigs, oldConfigs, runtime);
                break;
            case DOCKER:
                updateDockerRunTimeNode(newConfigs, oldConfigs, runtime);
                break;
            default:
                throw new MojoFailureException("The value of <os> is unknown.");
        }
    }

    private void updateNonDockerRunTimeNode(WebAppConfiguration newConfigs, WebAppConfiguration oldConfigs,
                                            Element configurationElement) {

        final String oldOS = formatOperationSystem(oldConfigs.getOs());
        createOrUpdateAttribute("os", newConfigs.getOs().toString(), oldOS, configurationElement);
        if (newConfigs.getJavaVersion() != null) {
            final String oldJavaVersion = Objects.toString(oldConfigs.getJavaVersion(), null);
            createOrUpdateAttribute("javaVersion", Objects.toString(newConfigs.getJavaVersion(), null),
                    oldJavaVersion, configurationElement);
        }
        if (newConfigs.getWebContainer() != null) {
            final String oldWebContainer = Objects.toString(oldConfigs.getWebContainer());
            createOrUpdateAttribute("webContainer", Objects.toString(newConfigs.getWebContainer()), oldWebContainer,
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

    private static boolean isDockerOs(OperatingSystem os) {
        return Objects.equals(os, OperatingSystem.DOCKER);
    }

    private static String formatOperationSystem(OperatingSystem osEnum) {
        return Objects.toString(osEnum, null);
    }
}
