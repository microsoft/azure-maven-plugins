/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.maven.AbstractAzureMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractFunctionMojo extends AbstractAzureMojo {
    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    protected String finalName;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    protected File outputDirectory;

    /**
     * Resource group of Function App. It will be created if it doesn't exist.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.resourceGroup", required = true)
    protected String resourceGroup;

    /**
     * Function App name. It will be created if it doesn't exist.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.appName", required = true)
    protected String appName;

    /**
     * Function App region, which will only be used to create Function App at the first time.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.region", defaultValue = "westus")
    protected String region;

    /**
     * Function App pricing tier, which will only be used to create Function App at the first time.<br/>
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
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.pricingTier", defaultValue = "S1")
    protected String pricingTier;

    /**
     * Application settings of Function App, in the form of name-value pairs.
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
     *
     * @since 0.1.0
     */
    @Parameter
    protected Properties appSettings;

    public String getFinalName() {
        return finalName;
    }

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
        return PricingTier.STANDARD_S1;
    }

    public Map getAppSettings() {
        return appSettings;
    }

    public String getDeploymentStageDirectory() {
        return Paths.get(getBuildDirectoryAbsolutePath(),
                "azure-functions",
                getAppName()).toString();
    }

    public FunctionApp getFunctionApp() {
        try {
            return getAzureClient().appServices().functionApps().getByResourceGroup(getResourceGroup(), getAppName());
        } catch (Exception e) {
            // Swallow exception
        }
        return null;
    }
}
