/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.maven.appservice.PricingTierEnum;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Map;
import java.util.Properties;

/**
 * Base abstract class for all Azure App Service Mojos.
 */
public abstract class AbstractAppServiceMojo extends AbstractAzureMojo {
    /**
     * Resource group of App Service. It will be created if it doesn't exist.
     */
    @Parameter(property = "resourceGroup", required = true)
    protected String resourceGroup;

    /**
     * App Service name. It will be created if it doesn't exist.
     */
    @Parameter(property = "appName", required = true)
    protected String appName;

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
     * App Service region, which will only be used to create App Service at the first time.
     */
    @Parameter(property = "region", defaultValue = "westus")
    protected String region;

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

    public String getRegion() {
        return region;
    }

    public Map getAppSettings() {
        return appSettings;
    }
}
