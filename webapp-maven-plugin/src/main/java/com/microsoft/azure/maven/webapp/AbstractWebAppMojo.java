/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.AbstractAzureMojo;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.configuration.PricingTierEnum;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Map;
import java.util.Properties;

/**
 * Base abstract class for shared configurations and operations.
 */
public abstract class AbstractWebAppMojo extends AbstractAzureMojo {
    @Parameter(property = "resourceGroup", required = true)
    protected String resourceGroup;

    @Parameter(property = "appName", required = true)
    protected String appName;

    @Parameter(property = "region", defaultValue = "westus")
    protected String region;

    @Parameter(property = "pricingTier", defaultValue = "S1")
    protected PricingTierEnum pricingTier;

    @Parameter(property = "containerSetting")
    protected ContainerSetting containerSetting;

    @Parameter(property = "appSettings")
    protected Properties appSettings;

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getAppName() {
        return appName;
    }

    public String getRegion() {
        return region;
    }

    public PricingTier getPricingTier() {
        return pricingTier == null ? PricingTier.STANDARD_S1 : pricingTier.toPricingTier();
    }

    public ContainerSetting getContainerSetting() {
        return containerSetting;
    }

    public Map getAppSettings() {
        return appSettings;
    }

    public WebApp getWebApp() {
        try {
            return getAzureClient().webApps().getByResourceGroup(getResourceGroup(), getAppName());
        } catch (Exception e) {
            // Swallow exception for non-existing web app
        }
        return null;
    }
}
