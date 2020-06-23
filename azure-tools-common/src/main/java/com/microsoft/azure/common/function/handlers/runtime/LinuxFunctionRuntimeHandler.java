/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.function.handlers.runtime;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionRuntimeStack;
import com.microsoft.azure.management.appservice.JavaVersion;

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
    public FunctionApp.DefinitionStages.WithCreate defineAppWithRuntime() {
        checkFunctionExtensionVersion();
        final FunctionApp.DefinitionStages.WithDockerContainerImage withDockerContainerImage = defineLinuxFunction();
        return withDockerContainerImage.withBuiltInImage(getRuntimeStack());
    }

    @Override
    public FunctionApp.Update updateAppRuntime(FunctionApp app) {
        checkFunctionExtensionVersion();
        return app.update().withBuiltInImage(getRuntimeStack());
    }

    private FunctionRuntimeStack getRuntimeStack() {
        return javaVersion == JavaVersion.JAVA_8_NEWEST ? FunctionRuntimeStack.JAVA_8 : FunctionRuntimeStack.JAVA_11;
    }
}
