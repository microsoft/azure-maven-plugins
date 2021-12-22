/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.RuntimeConfiguration;
import lombok.Getter;
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
     * App Service pricing tier, will be used to create new service plan or update the existing one<p>
     * Supported values : CONSUMPTION, B1, B2, B3, S1, S2, S3, P1V2, P2V2, P3V2, P1V3, P2V3, P3V3, EP1, EP2, EP3
     */
    @Getter
    @Parameter(property = "functions.pricingTier")
    protected String pricingTier;

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    protected String finalName;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    protected File outputDirectory;

    /**
     * Boolean flag to skip the execution of Maven Plugin for Azure Functions
     * @since 0.1.0
     */
    @Parameter(property = "functions.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * App Service region, which will only be used to create App Service at the first time.
     * @since 1.2.0
     */
    @Parameter(property = "functions.region")
    protected String region;

    /**
     * The configuration for Function App runtime environment <p>
     * For Windows/Linux Function App, you may also set `JavaVersion` in `runtime`, supported values are `Java 8` and `Java 11` <p>
     * <pre>
     * {@code
     * <runtime>
     *     <os>windows</os>
     *     <javaVersion>Java 8</javaVersion>
     * </runtime>
     * }
     * </pre>
     * For Docker Function App, please set the image, registryUrl and serverId<p>
     * <pre>
     * {@code
     * <runtime>
     *     <os>docker</os>
     *     <image>[hub-user/]repo-name[:tag]</image>
     *     <serverId></serverId>
     *     <registryUrl></registryUrl> <!- could be omitted for docker hub images -->
     * </runtime>
     * }
     * </pre>
     * For private docker images, please set your username and password in maven settings.xml and refer it with `serverId` in runtime configuration
     * @since 1.4.0
     */
    @Parameter(property = "functions.runtime")
    protected RuntimeConfiguration runtime;

    /**
     * Name of the application insight instance which will bind to your function app, must be in the same resource group with function app.
     * Will be skipped if `appInsightsKey` is specified
     * @since 1.6.0
     */
    @Parameter(property = "functions.appInsightsInstance")
    protected String appInsightsInstance;

    /**
     * Instrumentation key of application insights which will bind to your function app
     * @since 1.6.0
     */
    @Parameter(property = "functions.appInsightsKey")
    protected String appInsightsKey;

    /**
     * Boolean flag to enable/disable application insights for Function App
     * @since 1.6.0
     */
    @Parameter(property = "functions.disableAppInsights", defaultValue = "false")
    protected boolean disableAppInsights;

    @Getter
    protected ConfigParser parser = new ConfigParser(this);

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

    public RuntimeConfiguration getRuntimeConfiguration() {
        return runtime;
    }

    protected void validateAppName() {
        if (StringUtils.isBlank(appName)) {
            throw new AzureToolkitRuntimeException("<appName> is not configured in pom");
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
