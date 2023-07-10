/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.deploy;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Objects;

public interface IFunctionDeployHandler {
    String DEPLOY_START = "Trying to deploy artifact to %s...";
    String DEPLOY_FINISH = "Successfully deployed the artifact to https://%s";
    String FAILED_TO_GET_FUNCTION = "Failed to get function app with id %s, please ensure the target exists";

    void deploy(@Nonnull final File file, @Nonnull final WebAppBase webAppBase);

    default void deploy(@Nonnull final File file, @Nonnull final FunctionAppBase<?, ?, ?> functionAppBase) {
        this.deploy(file, Objects.requireNonNull(functionAppBase.getFullRemote(), String.format(FAILED_TO_GET_FUNCTION, functionAppBase.getId())));
    }
}
