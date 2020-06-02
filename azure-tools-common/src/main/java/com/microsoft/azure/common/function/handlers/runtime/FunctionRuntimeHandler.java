/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.function.handlers.runtime;

import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.common.handlers.runtime.BaseRuntimeHandler;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.resources.ResourceGroup;
import org.apache.commons.lang3.StringUtils;

public abstract class FunctionRuntimeHandler extends BaseRuntimeHandler<FunctionApp> {

    private static final JavaVersion DEFAULT_JAVA_VERSION = JavaVersion.JAVA_8_NEWEST;
    private static final String INVALID_JAVA_VERSION = "Invalid java version %s, using default value java 8";
    private static final String UNSUPPORTED_JAVA_VERSION = "Unsupported java version %s, using default value java 8";

    protected FunctionExtensionVersion functionExtensionVersion;
    protected RuntimeConfiguration runtimeConfiguration;
    protected IDockerCredentialProvider dockerCredentialProvider;
    protected JavaVersion javaVersion;

    public abstract static class Builder<T extends FunctionRuntimeHandler.Builder<T>> extends BaseRuntimeHandler.Builder<T> {
        protected FunctionExtensionVersion functionExtensionVersion;
        protected RuntimeConfiguration runtimeConfiguration;
        protected IDockerCredentialProvider dockerCredentialProvider;

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

        public abstract FunctionRuntimeHandler build();

        protected abstract T self();
    }

    protected FunctionRuntimeHandler(Builder<?> builder) {
        super(builder);
        this.functionExtensionVersion = builder.functionExtensionVersion;
        this.runtimeConfiguration = builder.runtimeConfiguration;
        this.dockerCredentialProvider = builder.dockerCredentialProvider;
        this.javaVersion = parseJavaVersion();
    }

    @Override
    public abstract FunctionApp.DefinitionStages.WithCreate defineAppWithRuntime() throws AzureExecutionException;

    @Override
    public abstract FunctionApp.Update updateAppRuntime(FunctionApp app) throws AzureExecutionException;

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

    protected JavaVersion parseJavaVersion() {
        final String javaVersionConfiguration = runtimeConfiguration.getJavaVersion();
        if (StringUtils.isEmpty(javaVersionConfiguration)) {
            return DEFAULT_JAVA_VERSION;
        }
        try {
            final int version = Integer.valueOf(runtimeConfiguration.getJavaVersion());
            switch (version) {
                case 8:
                    return JavaVersion.JAVA_8_NEWEST;
                case 11:
                    return JavaVersion.JAVA_11;
                default:
                    Log.warn(String.format(UNSUPPORTED_JAVA_VERSION, version));
                    return DEFAULT_JAVA_VERSION;
            }
        } catch (NumberFormatException e) {
            Log.warn(String.format(INVALID_JAVA_VERSION, runtimeConfiguration.getJavaVersion()));
            return DEFAULT_JAVA_VERSION;
        }
    }
}
