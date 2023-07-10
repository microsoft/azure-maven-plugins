/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.deploy;

import com.azure.resourcemanager.appservice.models.WebAppBase;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceKuduClient;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Objects;

public class FlexFunctionDeployHandler implements IFunctionDeployHandler {

    public static final String FAILED_TO_DEPLOY = "Failed to deploy to Azure Function (%s) : ";

    @Override
    @Deprecated
    public void deploy(@Nonnull File file, @Nonnull WebAppBase webAppBase) {
        deploy(file, (FunctionAppBase<?, ?, ?>) Objects.requireNonNull(Azure.az(AzureFunctions.class).getById(webAppBase.id())));
    }

    @Override
    public void deploy(@Nonnull File file, @Nonnull final FunctionAppBase<?,?,?> functionAppBase) {
        final AppServiceKuduClient kuduManager = functionAppBase.getKuduManager();
        try {
            Objects.requireNonNull(kuduManager).flexZipDeploy(file);
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(String.format(FAILED_TO_DEPLOY, ExceptionUtils.getRootCauseMessage(e)), e);
        }
        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, functionAppBase.getHostName()));
    }
}
