/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.configurations.ElasticPremiumPricingTier;
import com.microsoft.azure.common.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.common.function.utils.FunctionUtils;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Map;

public abstract class AbstractFunctionMojo extends AbstractAppServiceMojo {

    private static final String JDK_VERSION_ERROR = "Azure Functions only support JDK 8, which is lower than local " +
            "JDK version %s.";
    private static final String FUNCTIONS_WORKER_RUNTIME_NAME = "FUNCTIONS_WORKER_RUNTIME";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String SET_FUNCTIONS_WORKER_RUNTIME = "Set function worker runtime to java.";
    private static final String CHANGE_FUNCTIONS_WORKER_RUNTIME = "Function worker runtime doesn't " +
            "meet the requirement, change it from %s to java.";
    private static final String FUNCTIONS_EXTENSION_VERSION_NAME = "FUNCTIONS_EXTENSION_VERSION";
    private static final String FUNCTIONS_EXTENSION_VERSION_VALUE = "~3";
    private static final String SET_FUNCTIONS_EXTENSION_VERSION = "Functions extension version " +
            "isn't configured, setting up the default value.";

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
    public Map getAppSettingsWithDefaultValue() {
        final Map settings = getAppSettings();
        overrideDefaultAppSetting(settings, FUNCTIONS_WORKER_RUNTIME_NAME, SET_FUNCTIONS_WORKER_RUNTIME,
                FUNCTIONS_WORKER_RUNTIME_VALUE, CHANGE_FUNCTIONS_WORKER_RUNTIME);
        setDefaultAppSetting(settings, FUNCTIONS_EXTENSION_VERSION_NAME, SET_FUNCTIONS_EXTENSION_VERSION,
                FUNCTIONS_EXTENSION_VERSION_VALUE);
        return settings;
    }

    public FunctionExtensionVersion getFunctionExtensionVersion() throws AzureExecutionException {
        final String extensionVersion = (String) getAppSettingsWithDefaultValue().get(FUNCTIONS_EXTENSION_VERSION_NAME);
        return FunctionUtils.parseFunctionExtensionVersion(extensionVersion);
    }

    private void overrideDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                           String settingValue, String changeSettingMessage) {

        final String setting = (String) result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            Log.info(settingIsEmptyMessage);
        } else if (!setting.equals(settingValue)) {
            Log.warn(String.format(changeSettingMessage, setting));
        }
        result.put(settingName, settingValue);
    }

    private void setDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                        String settingValue) {

        final String setting = (String) result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            Log.info(settingIsEmptyMessage);
            result.put(settingName, settingValue);
        }
    }
    //endregion

    //region Getter

    public PricingTier getPricingTier() throws AzureExecutionException {
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
        final PagedList<FunctionApp> functionList = getAzureClient().appServices().functionApps().list();
        return AppServiceUtils.findAppServiceInPagedList(functionList, getResourceGroup(), getAppName());
    }

    public RuntimeConfiguration getRuntime() {
        return runtime;
    }

    @Override
    public void execute() throws MojoExecutionException {
        checkJavaVersion();
        super.execute();
    }

    public void checkJavaVersion() {
        final String javaVersion = System.getProperty("java.version");
        if (!javaVersion.startsWith("1.8")) {
            Log.warn(String.format(JDK_VERSION_ERROR, javaVersion));
        }
    }

    //endregion
}
