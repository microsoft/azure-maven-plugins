/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.handlers.runtime;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionDeploymentSlot;
import com.microsoft.azure.management.appservice.FunctionRuntimeStack;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebAppBase;

public class LinuxFunctionRuntimeHandler extends AbstractLinuxFunctionRuntimeHandler {

    public static class Builder extends FunctionRuntimeHandler.Builder<Builder> {

        @Override
        public LinuxFunctionRuntimeHandler build() {
            return new LinuxFunctionRuntimeHandler(self());
        }

        @Override
        protected Builder self() {
            return this;
        }

    }

    protected LinuxFunctionRuntimeHandler(Builder builder) {
        super(builder);
    }

    @Override
    public FunctionApp.DefinitionStages.WithCreate defineFunctionApp() {
        checkFunctionExtensionVersion();
        final FunctionApp.DefinitionStages.WithDockerContainerImage withDockerContainerImage = defineLinuxFunction();
        return withDockerContainerImage.withBuiltInImage(getRuntimeStack());
    }

    @Override
    public FunctionApp.Update updateAppRuntime(FunctionApp app) {
        checkFunctionExtensionVersion();
        return app.update().withBuiltInImage(getRuntimeStack());
    }

    @Override
    public WebAppBase.Update<FunctionDeploymentSlot> updateDeploymentSlot(FunctionDeploymentSlot deploymentSlot) {
        checkFunctionExtensionVersion();
        throw new UnsupportedOperationException("Please switch track2 SDK.");
    }

    private FunctionRuntimeStack getRuntimeStack() {
        return javaVersion == JavaVersion.JAVA_8_NEWEST ? FunctionRuntimeStack.JAVA_8 : FunctionRuntimeStack.JAVA_11;
    }
}
