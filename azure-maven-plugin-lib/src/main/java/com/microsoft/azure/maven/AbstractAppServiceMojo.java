/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven;

import com.azure.core.management.AzureEnvironment;
import com.microsoft.azure.common.appservice.DeploymentSlotSetting;
import com.microsoft.azure.common.appservice.DeploymentType;
import com.microsoft.azure.common.exceptions.AzureExecutionException;

import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Parameter;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Base abstract class for all Azure App Service Mojos.
 */
public abstract class AbstractAppServiceMojo extends AbstractAzureMojo {
    protected static final String MAVEN_PLUGIN_POSTFIX = "-maven-plugin";
    protected static final String PORTAL_URL_PATTERN = "%s/#@%s/resource%s";

    /**
     * Resource group of App Service. It will be created if it doesn't exist.
     */
    @Parameter(property = "resourceGroup", required = false)
    protected String resourceGroup;

    /**
     * App Service name. It will be created if it doesn't exist.
     */
    @Parameter(property = "appName", required = false)
    protected String appName;

    /**
     * Deployment type to deploy Web App or Function App.
     *
     * Supported values for Web App:
     * <ul>
     *      <li>FTP - {@code <resources>} specifies configurations for this kind of deployment.</li>
     *      <li>ZIP - {@code <resources>} specifies configurations for this kind of deployment.</li>
     *      <li>WAR - {@code <warFile>} and {@code <path>} specifies configurations for this kind of deployment.</li>
     *      <li>JAR - {@code <jarFile>} and {@code <path>} specifies configurations for this kind of deployment.</li>
     *      <li>AUTO - inspects {@code <packaging>} of the Maven project and uses WAR, JAR </li>
     *      <li>NONE - does nothing</li>
     *      <li>* defaults to AUTO if nothing is specified</li>
     * </ul>
     *
     * Supported values for Function App:
     * <ul>
     *      <li>MSDEPLOY</li>
     *      <li>FTP</li>
     *      <li>ZIP</li>
     *      <li>* defaults to ZIP if nothing is specified</li>
     * </ul>
     * @since 0.1.0
     */
    @Parameter(property = "deploymentType")
    protected String deploymentType;

    /**
     * Resource group of App Service Plan. It will be created if it doesn't exist.
     */
    @Parameter(property = "appServicePlanResourceGroup")
    protected String appServicePlanResourceGroup;

    /**
     * App Service Plan name. It will be created if it doesn't exist.
     */
    @Parameter(property = "appServicePlanName")
    protected String appServicePlanName;

    /**
     * Deployment Slot. It will be created if it does not exist.
     * It requires the web app exists already.
     */
    @Parameter(alias = "deploymentSlot")
    protected DeploymentSlotSetting deploymentSlotSetting;

    /**
     * Application settings of App Service, in the form of name-value pairs.
     * <pre>
     * {@code
     * <appSettings>
     *         <property>
     *                 <name>setting-name</name>
     *                 <value>setting-value</value>
     *         </property>
     * </appSettings>
     * }
     * </pre>
     */
    @Parameter
    protected Properties appSettings;

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppServicePlanResourceGroup() {
        return appServicePlanResourceGroup;
    }

    public String getAppServicePlanName() {
        return appServicePlanName;
    }

    public Map getAppSettings() {
        if (appSettings == null) {
            appSettings = new Properties();
        }
        return appSettings;
    }

    public DeploymentType getDeploymentType() throws AzureExecutionException {
        return DeploymentType.fromString(deploymentType);
    }

    public DeploymentSlotSetting getDeploymentSlotSetting() {
        return deploymentSlotSetting;
    }

    public List<Resource> getResources() {
        return Collections.EMPTY_LIST;
    }

    public String getDeploymentStagingDirectoryPath() {
        final String outputFolder = this.getPluginName().replaceAll(MAVEN_PLUGIN_POSTFIX, "");
        return Paths.get(
                this.getBuildDirectoryAbsolutePath(),
                outputFolder, this.getAppName()
        ).toString();
    }

    public void setDeploymentSlot(DeploymentSlotSetting slotSetting) {
        this.deploymentSlotSetting = slotSetting;
    }

    public String getResourcePortalUrl(WebAppBase resource) throws AzureAuthFailureException, AzureExecutionException {
        final AzureEnvironment environment = Azure.az(AzureAccount.class).account().getEnvironment();
        final String tenantId = getAzureClient().tenantId();
        return String.format(PORTAL_URL_PATTERN, getPortalUrl(environment), tenantId, resource.id());
    }

    private static String getPortalUrl(AzureEnvironment azureEnvironment) {
        if (azureEnvironment == null || azureEnvironment == AzureEnvironment.AZURE) {
            return "https://ms.portal.azure.com";
        }
        if (azureEnvironment == AzureEnvironment.AZURE_CHINA) {
            return "https://portal.azure.cn";
        }
        return azureEnvironment.getPortal();
    }
}
