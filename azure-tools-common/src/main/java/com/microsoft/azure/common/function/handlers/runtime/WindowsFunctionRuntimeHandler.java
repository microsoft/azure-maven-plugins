/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.handlers.runtime;

import com.microsoft.azure.toolkit.lib.common.logging.Log;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionDeploymentSlot;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebAppBase;
import org.apache.commons.lang3.StringUtils;

public class WindowsFunctionRuntimeHandler extends FunctionRuntimeHandler {

    private static final String HOST_JAVA_VERSION = "Java version of function host : %s";
    private static final String HOST_JAVA_VERSION_OFF = "Java version of function host is not initiated," +
            " set it to Java 8.";
    private static final String HOST_JAVA_VERSION_UPDATE = "Updating function host java version from %s to %s";

    public static class Builder extends FunctionRuntimeHandler.Builder<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public WindowsFunctionRuntimeHandler build() {
            return new WindowsFunctionRuntimeHandler(this);
        }
    }

    protected WindowsFunctionRuntimeHandler(Builder builder) {
        super(builder);
    }

    @Override
    public FunctionApp.DefinitionStages.WithCreate defineFunctionApp() {
        final AppServicePlan appServicePlan = getAppServicePlan();
        final FunctionApp.DefinitionStages.Blank functionApp = defineFunction();
        FunctionApp.DefinitionStages.WithCreate appWithCreate;
        if (appServicePlan == null) {
            final FunctionApp.DefinitionStages.NewAppServicePlanWithGroup appWithNewServicePlan =
                    functionApp.withRegion(this.region);
            if (getResourceGroup() == null) {
                appWithCreate = appWithNewServicePlan.withNewResourceGroup(resourceGroup);
            } else {
                appWithCreate = appWithNewServicePlan.withExistingResourceGroup(resourceGroup);
            }
            if (pricingTier == null) {
                appWithCreate = StringUtils.isEmpty(servicePlanName) ? appWithCreate.withNewConsumptionPlan() :
                        appWithCreate.withNewConsumptionPlan(servicePlanName);
            } else {
                appWithCreate = StringUtils.isEmpty(servicePlanName) ? appWithCreate.withNewAppServicePlan(pricingTier) :
                        appWithCreate.withNewAppServicePlan(servicePlanName, pricingTier);
            }
        } else {
            final FunctionApp.DefinitionStages.ExistingAppServicePlanWithGroup appWithExistingServicePlan =
                    functionApp.withExistingAppServicePlan(appServicePlan);
            if (getResourceGroup() == null) {
                appWithCreate = appWithExistingServicePlan.withNewResourceGroup(resourceGroup);
            } else {
                appWithCreate = appWithExistingServicePlan.withExistingResourceGroup(resourceGroup);
            }
        }
        appWithCreate.withJavaVersion(javaVersion).withWebContainer(null);
        return appWithCreate;
    }

    @Override
    public FunctionApp.Update updateAppRuntime(FunctionApp app) {
        final JavaVersion serverJavaVersion = app.javaVersion();
        final FunctionApp.Update update = app.update();
        if (javaVersion.equals(serverJavaVersion)) {
            Log.info(String.format(HOST_JAVA_VERSION, serverJavaVersion));
        } else if (JavaVersion.OFF.equals(serverJavaVersion)) {
            Log.info(HOST_JAVA_VERSION_OFF);
            update.withJavaVersion(javaVersion);
        } else if (StringUtils.isNotEmpty(runtimeConfiguration.getJavaVersion())) {
            // Will update function host java version if user specify it in pom
            Log.info(String.format(HOST_JAVA_VERSION_UPDATE, serverJavaVersion, javaVersion));
            update.withJavaVersion(javaVersion);
        }
        return update;
    }

    @Override
    public WebAppBase.Update<FunctionDeploymentSlot> updateDeploymentSlot(FunctionDeploymentSlot deploymentSlot) {
        return deploymentSlot.update().withJavaVersion(javaVersion).withWebContainer(null);
    }

}
