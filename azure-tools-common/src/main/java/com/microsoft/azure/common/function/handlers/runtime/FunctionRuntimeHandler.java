/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.function.handlers.runtime;

import com.microsoft.azure.arm.utils.SdkContext;
import com.microsoft.azure.common.appservice.ConfigurationSourceType;
import com.microsoft.azure.common.appservice.DeploymentSlotSetting;
import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.common.function.utils.FunctionUtils;
import com.microsoft.azure.common.handlers.runtime.BaseRuntimeHandler;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionDeploymentSlot;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.storage.StorageAccountSkuType;
import org.apache.commons.lang3.StringUtils;

public abstract class FunctionRuntimeHandler extends BaseRuntimeHandler<FunctionApp> {

    private static final String TARGET_CONFIGURATION_SOURCE_SLOT_NOT_EXIST =
            "The deployment slot specified in <configurationSource> does not exist.";
    private static final String UNKNOWN_CONFIGURATION_SOURCE = "Unknown <configurationSource> value for creating deployment slot. " +
            "Please use 'NEW', 'PARENT' or specify an existing slot.";

    protected FunctionExtensionVersion functionExtensionVersion;
    protected RuntimeConfiguration runtimeConfiguration;
    protected IDockerCredentialProvider dockerCredentialProvider;
    protected JavaVersion javaVersion;

    public abstract static class Builder<T extends FunctionRuntimeHandler.Builder<T>> extends BaseRuntimeHandler.Builder<T> {
        protected FunctionExtensionVersion functionExtensionVersion;
        protected RuntimeConfiguration runtimeConfiguration;
        protected IDockerCredentialProvider dockerCredentialProvider;
        protected JavaVersion javaVersion;

        public T functionExtensionVersion(final FunctionExtensionVersion value) {
            this.functionExtensionVersion = value;
            return self();
        }

        public T runtime(final RuntimeConfiguration value) {
            this.runtimeConfiguration = value;
            return self();
        }

        public T dockerCredentialProvider(IDockerCredentialProvider value) {
            this.dockerCredentialProvider = value;
            return self();
        }

        public T javaVersion(JavaVersion value) {
            this.javaVersion = value;
            return self();
        }

        public abstract FunctionRuntimeHandler build();

        protected abstract T self();
    }

    protected FunctionRuntimeHandler(Builder<?> builder) {
        super(builder);
        this.functionExtensionVersion = builder.functionExtensionVersion;
        this.runtimeConfiguration = builder.runtimeConfiguration;
        this.dockerCredentialProvider = builder.dockerCredentialProvider;
        this.javaVersion = builder.javaVersion;
    }

    @Override
    public WebAppBase.DefinitionStages.WithCreate defineAppWithRuntime() throws AzureExecutionException {
        return defineFunctionApp().withNewStorageAccount(SdkContext.randomResourceName(StringUtils.replace(appName, "-", ""), 20), StorageAccountSkuType.STANDARD_GRS);
    }

    public abstract FunctionApp.DefinitionStages.WithCreate defineFunctionApp() throws AzureExecutionException;

    @Override
    public abstract FunctionApp.Update updateAppRuntime(FunctionApp app) throws AzureExecutionException;

    public abstract WebAppBase.Update<FunctionDeploymentSlot> updateDeploymentSlot(FunctionDeploymentSlot deploymentSlot) throws AzureExecutionException;

    public FunctionDeploymentSlot.DefinitionStages.WithCreate createDeploymentSlot(FunctionApp functionApp,
                                                                                   DeploymentSlotSetting deploymentSlotSetting) throws AzureExecutionException {
        final ConfigurationSourceType configurationSourceType = ConfigurationSourceType.fromString(deploymentSlotSetting.getConfigurationSource());
        final FunctionDeploymentSlot.DefinitionStages.Blank slot = functionApp.deploymentSlots().define(deploymentSlotSetting.getName());
        switch (configurationSourceType) {
            case PARENT:
                return slot.withConfigurationFromParent();
            case OTHERS:
                final FunctionDeploymentSlot configurationSourceSlot =
                        FunctionUtils.getFunctionDeploymentSlotByName(functionApp, deploymentSlotSetting.getConfigurationSource());
                if (configurationSourceSlot == null) {
                    throw new AzureExecutionException(TARGET_CONFIGURATION_SOURCE_SLOT_NOT_EXIST);
                }
                return slot.withConfigurationFromDeploymentSlot(configurationSourceSlot);
            default:
                throw new AzureExecutionException(UNKNOWN_CONFIGURATION_SOURCE);
        }
    }

    @Override
    protected void changeAppServicePlan(FunctionApp app, AppServicePlan appServicePlan) throws AzureExecutionException {
        app.update().withExistingAppServicePlan(appServicePlan).apply();
    }

    protected FunctionApp.DefinitionStages.Blank defineFunction() {
        return azure.appServices().functionApps().define(appName);
    }

    protected ResourceGroup getResourceGroup() {
        return azure.resourceGroups().getByName(resourceGroup);
    }
}
