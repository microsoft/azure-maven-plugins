/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import com.microsoft.azure.maven.function.configurations.ElasticPremiumPricingTier;
import com.microsoft.azure.maven.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.maven.utils.AppServiceUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

public abstract class AbstractFunctionMojo extends AbstractAppServiceMojo {

    private static final String JDK_VERSION_ERROR = "Azure Functions only support JDK 8, which is lower than local " +
            "JDK version %s";
    private static final String FUNCTIONS_WORKER_RUNTIME_NAME = "FUNCTIONS_WORKER_RUNTIME";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String SET_FUNCTIONS_WORKER_RUNTIME = "Set function worker runtime to java";
    private static final String CHANGE_FUNCTIONS_WORKER_RUNTIME = "Function worker runtime doesn't " +
            "meet the requirement, change it from %s to java";

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
     *     <li>P1V2</li>
     *     <li>P2V2</li>
     *     <li>P3V2</li>
     * </ul>
     */
    @Parameter(property = "functions.pricingTier")
    protected String pricingTier;

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

    @Parameter(property = "functions.runtime")
    protected RuntimeConfiguration runtime;

    //endregion

    //region get App Settings
    @Override
    public Map getAppSettings() {
        final Map result = super.getAppSettings();
        final String workerRuntime = (String) result.get(FUNCTIONS_WORKER_RUNTIME_NAME);
        if (StringUtils.isEmpty(workerRuntime)) {
            info(SET_FUNCTIONS_WORKER_RUNTIME);
        } else if (!FUNCTIONS_WORKER_RUNTIME_VALUE.equals(workerRuntime)) {
            warning(String.format(CHANGE_FUNCTIONS_WORKER_RUNTIME, workerRuntime));
        }
        result.put(FUNCTIONS_WORKER_RUNTIME_NAME, FUNCTIONS_WORKER_RUNTIME_VALUE);
        return result;
    }
    //endregion

    //region Getter

    public PricingTier getPricingTier() throws MojoExecutionException {
        if (StringUtils.isEmpty(pricingTier)) {
            return null;
        }
        final ElasticPremiumPricingTier elasticPremiumPricingTier = ElasticPremiumPricingTier.fromString(pricingTier);
        return elasticPremiumPricingTier != null ? elasticPremiumPricingTier.toPricingTier()
                : AppServiceUtils.getPricingTierFromString(pricingTier);
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
        if (!javaVersion.startsWith("1.8")) {
            super.warning(String.format(JDK_VERSION_ERROR, javaVersion));
        }
    }

    //endregion
}
