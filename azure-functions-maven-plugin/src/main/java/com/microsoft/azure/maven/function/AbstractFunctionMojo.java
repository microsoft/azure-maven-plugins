/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.appservice.PricingTierEnum;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.Nullable;
import java.io.File;

public abstract class AbstractFunctionMojo extends AbstractAppServiceMojo {

    private static final String JDK_VERSION_ERROR = "Local JDK Version %s does not meet the maven " +
            "plugin requirement. The supported version is JDK 8";

    //region Properties
    /**
     * App Service pricing tier, which will only be used to create Functions App at the first time.<br/>
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
    @Parameter(property = "functions.pricingTier")
    protected PricingTierEnum pricingTier;

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    protected String finalName;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    protected File outputDirectory;

    /**
     * Skip execution.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * App Service region, which will only be used to create App Service at the first time.
     */
    @Parameter(property = "functions.region", defaultValue = "westeurope")
    protected String region;

    //endregion

    //region Getter

    public PricingTier getPricingTier() {
        return pricingTier == null ? null : pricingTier.toPricingTier();
    }

    public String getRegion() {
        return region;
    }

    @Override
    protected boolean isSkipMojo() {
        return skip;
    }

    public String getFinalName() {
        return finalName;
    }

    @Nullable
    public FunctionApp getFunctionApp() throws AzureAuthFailureException {
        try {
            return getAzureClient().appServices().functionApps().getByResourceGroup(getResourceGroup(), getAppName());
        } catch (AzureAuthFailureException authEx) {
            throw authEx;
        } catch (Exception ex) {
            this.getLog().debug(ex);
            // Swallow exception for non-existing Azure Functions
        }
        return null;
    }

    @Override
    public void execute() throws MojoExecutionException {
        checkJavaVersion();
        super.execute();
    }

    public void checkJavaVersion() throws MojoExecutionException {
        final String javaVersion = System.getProperty("java.version");
        if (!javaVersion.startsWith("1.8")){
            throw new MojoExecutionException(String.format(JDK_VERSION_ERROR, javaVersion));
        }
    }

    //endregion
}
