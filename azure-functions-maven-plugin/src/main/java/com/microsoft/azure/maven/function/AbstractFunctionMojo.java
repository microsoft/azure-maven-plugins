/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.RuntimeConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.Map;

public abstract class AbstractFunctionMojo extends AbstractAppServiceMojo {

    private static final String FUNCTION_JAVA_VERSION_KEY = "functionJavaVersion";
    private static final String DISABLE_APP_INSIGHTS_KEY = "disableAppInsights";
    private static final String FUNCTION_RUNTIME_KEY = "os";
    private static final String FUNCTION_IS_DOCKER_KEY = "isDockerFunction";
    private static final String FUNCTION_REGION_KEY = "region";
    private static final String FUNCTION_PRICING_KEY = "pricingTier";
    private static final String FUNCTION_DEPLOY_TO_SLOT_KEY = "isDeployToFunctionSlot";
    protected static final String TRIGGER_TYPE = "triggerType";

    //region Properties
    /**
     * App Service pricing tier, which will only be used to create Functions App at the first time.<p>
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

    @Parameter(property = "functions.appInsightsInstance")
    protected String appInsightsInstance;

    @Parameter(property = "functions.appInsightsKey")
    protected String appInsightsKey;

    @Parameter(property = "functions.disableAppInsights", defaultValue = "false")
    protected boolean disableAppInsights;

    //endregion

    //region Getter

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

    public String getAppInsightsInstance() {
        return appInsightsInstance;
    }

    public String getAppInsightsKey() {
        return appInsightsKey;
    }

    public boolean isDisableAppInsights() {
        return disableAppInsights;
    }

    public IFunctionApp getFunctionApp() {
        return getOrCreateAzureAppServiceClient().functionApp(getResourceGroup(), getAppName());
    }

    public RuntimeConfiguration getRuntimeConfiguration() {
        return runtime;
    }

    protected void validateAppName() {
        if (StringUtils.isBlank(appName)) {
            throw new AzureToolkitRuntimeException("Please config the <appName> in pom.xml");
        }
    }

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> result = super.getTelemetryProperties();
        final String javaVersion = runtime == null ? null : runtime.getJavaVersion();
        final String os = runtime == null ? null : runtime.getOs();
        final boolean isDockerFunction = runtime != null && StringUtils.isNotEmpty(runtime.getImage());
        result.put(FUNCTION_JAVA_VERSION_KEY, StringUtils.isEmpty(javaVersion) ? "" : javaVersion);
        result.put(FUNCTION_RUNTIME_KEY, StringUtils.isEmpty(os) ? "" : os);
        result.put(FUNCTION_IS_DOCKER_KEY, String.valueOf(isDockerFunction));
        result.put(FUNCTION_REGION_KEY, region);
        result.put(FUNCTION_PRICING_KEY, pricingTier);
        result.put(DISABLE_APP_INSIGHTS_KEY, String.valueOf(isDisableAppInsights()));
        final boolean isDeployToFunctionSlot = getDeploymentSlotSetting() != null && StringUtils.isNotEmpty(getDeploymentSlotSetting().getName());
        result.put(FUNCTION_DEPLOY_TO_SLOT_KEY, String.valueOf(isDeployToFunctionSlot));
        return result;
    }
    //endregion
}
