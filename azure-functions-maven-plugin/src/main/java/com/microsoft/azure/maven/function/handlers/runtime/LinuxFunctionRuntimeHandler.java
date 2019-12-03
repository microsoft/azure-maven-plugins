/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers.runtime;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionRuntimeStack;

public class LinuxFunctionRuntimeHandler extends AbstractLinuxFunctionRuntimeHandler {

    private static final FunctionRuntimeStack JAVA_8_RUNTIME = new FunctionRuntimeStack("java", "~3", "java|8",
            "DOCKER|mcr.microsoft.com/azure-functions/java:3.0-preview-java8-appservice");

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
        final FunctionApp.DefinitionStages.WithDockerContainerImage withDockerContainerImage = defineLinuxFunction();
        return withDockerContainerImage.withBuiltInImage(JAVA_8_RUNTIME);
    }

    @Override
    public FunctionApp.Update updateAppRuntime(FunctionApp app) {
        return app.update();
    }
}
