/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven;

import com.azure.core.management.AzureEnvironment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.maven.model.DeploymentResource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureLoginException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
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
    protected static final String PORTAL_URL_PATTERN = "%s/#@/resource%s";

    /**
     * Name of the resource group. It will be created if it doesn't exist.
     */
    @JsonProperty
    @Parameter(property = "resourceGroup", required = false)
    protected String resourceGroup;

    /**
     * Name of the app service. It will be created if it doesn't exist.
     */
    @JsonProperty
    @Parameter(property = "appName", required = false)
    protected String appName;

    /**
     * Resource group of app service plan. It will be created if it doesn't exist.
     */
    @JsonProperty
    @Parameter(property = "appServicePlanResourceGroup")
    protected String appServicePlanResourceGroup;

    /**
     * Name of the app service plan. It will be created if it doesn't exist.
     */
    @JsonProperty
    @Parameter(property = "appServicePlanName")
    protected String appServicePlanName;

    /**
     * Configuration for deployment Slot, will create new slot if target does not exist. <p>
     * Require the web app exists already. <p>
     * Parameters for deployment slot
     * <ul>
     * <li> name: Specifies the name for deployment slot. </li>
     * <li> configurationSource: Specifies the configuration source of new created deployment slot, could be parent or existing deployment slot name,
     * default value is parent </li>
     * </ul>
     * <pre>
     * {@code
     * <deploymentSlotSetting>
     *     <name>Slot-Name</name>
     *     <configurationSource>Source</value>
     * </deploymentSlotSetting>
     * }
     * </pre>
     */
    @JsonProperty("deploymentSlot")
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
    @JsonProperty
    @Parameter
    protected Properties appSettings;

    @JsonIgnore
    protected AzureAppService appServiceClient;

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

    public DeploymentSlotSetting getDeploymentSlotSetting() {
        return deploymentSlotSetting;
    }

    public List<DeploymentResource> getResources() {
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

    public String getResourcePortalUrl(String id) {
        final AzureEnvironment environment = Azure.az(AzureAccount.class).account().getEnvironment();
        return String.format(PORTAL_URL_PATTERN, getPortalUrl(environment), id);
    }

    protected static String getPortalUrl(AzureEnvironment azureEnvironment) {
        if (azureEnvironment == null || azureEnvironment == AzureEnvironment.AZURE) {
            return "https://ms.portal.azure.com";
        }
        if (azureEnvironment == AzureEnvironment.AZURE_CHINA) {
            return "https://portal.azure.cn";
        }
        return azureEnvironment.getPortal();
    }

    protected AzureAppService getOrCreateAzureAppServiceClient() {
        if (appServiceClient == null) {
            try {
                final Account account = getAzureAccount();
                final List<Subscription> subscriptions = account.getSubscriptions();
                final String targetSubscriptionId = getTargetSubscriptionId(getSubscriptionId(), subscriptions, account.getSelectedSubscriptions());
                checkSubscription(subscriptions, targetSubscriptionId);
                com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account().selectSubscription(Collections.singletonList(targetSubscriptionId));
                appServiceClient = Azure.az(AzureAppService.class).subscription(targetSubscriptionId);
                printCurrentSubscription(appServiceClient);
                this.subscriptionId = targetSubscriptionId;
            } catch (AzureLoginException | AzureExecutionException | IOException e) {
                throw new AzureToolkitRuntimeException("Cannot authenticate", e);
            }
        }
        return appServiceClient;
    }

    // todo: Replace same method in AbstractAzureMojo after function track2 migration
    protected void printCurrentSubscription(AzureAppService appServiceClient) {
        if (appServiceClient == null) {
            return;
        }
        final Subscription subscription = appServiceClient.getDefaultSubscription();
        if (subscription != null) {
            Log.info(String.format(SUBSCRIPTION_TEMPLATE, TextUtils.cyan(subscription.getName()), TextUtils.cyan(subscription.getId())));
        }
    }
}
