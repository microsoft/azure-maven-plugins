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

import java.io.File;
import java.util.Optional;

public class FlexFunctionDeployHandler implements IFunctionDeployHandler {

    public static final String FAILED_TO_GET_FUNCTION = "Failed to get function app with id %s, please ensure the target exists";
    public static final String FAILED_TO_DEPLOY = "Failed to deploy to Azure Function (%s) : ";

    @Override
    public void deploy(File file, WebAppBase webAppBase) {
        final AppServiceKuduClient kuduManager = Optional.ofNullable((FunctionAppBase<?, ?, ?>)
                Azure.az(AzureFunctions.class).getById(webAppBase.id()))
            .map(FunctionAppBase::getKuduManager)
            .orElseThrow(() -> new AzureToolkitRuntimeException(FAILED_TO_GET_FUNCTION, webAppBase.id()));
        try {
            kuduManager.flexZipDeploy(file);
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(String.format(FAILED_TO_DEPLOY, ExceptionUtils.getRootCauseMessage(e)), e);
        }
        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, webAppBase.defaultHostname()));
    }
}
