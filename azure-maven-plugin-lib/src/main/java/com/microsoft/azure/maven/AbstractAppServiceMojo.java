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
    @Parameter(property = "webapp.resourceGroup", required = true)
    protected String resourceGroup;

    /**
     * App Service name. It will be created if it doesn't exist.
     */
    @Parameter(property = "webapp.appName", required = true)
    protected String appName;

    /**
     * Resource group of App Service Plan. It will be created if it doesn't exist.
     */
    @Parameter(property = "webapp.appServicePlanResourceGroup")
    protected String appServicePlanResourceGroup;

    /**
     * App Service Plan name. It will be created if it doesn't exist.
     */
    @Parameter(property = "webapp.appServicePlanName")
    protected String appServicePlanName;

    /**
     * Web App region, which will only be used to create App Service at the first time.
     */
    @Parameter(property = "webapp.region", defaultValue = "westus")
    protected String region;

    /**
     * App Service pricing tier, which will only be used to create Web App at the first time.<br/>
     * Below is the list of supported pricing tier:
     * <ul>
     *     <li>F1</li>
     *     <li>D1</li>
     *     <li>B1</li>
     *     <li>B2</li>
     *     <li>B3</li>
     *     <li>S1</li>
     *     <li>S2</li>
     *     <li>S3</li>
     *     <li>P1</li>
     *     <li>P2</li>
     *     <li>P3</li>
     * </ul>
     */
    @Parameter(property = "webapp.pricingTier", defaultValue = "S1")
    protected PricingTierEnum pricingTier;

    /**
     * Application settings of Web App, in the form of name-value pairs.
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

    public PricingTier getPricingTier() {
        return pricingTier == null ? PricingTier.STANDARD_S1 : pricingTier.toPricingTier();
    }

    public Map getAppSettings() {
        return appSettings;
    }
}
