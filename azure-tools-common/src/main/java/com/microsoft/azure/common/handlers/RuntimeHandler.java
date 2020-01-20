/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.handlers;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.WebAppBase;

public interface RuntimeHandler<T extends WebAppBase> {

    WebAppBase.DefinitionStages.WithCreate defineAppWithRuntime() throws AzureExecutionException;

    WebAppBase.Update updateAppRuntime(final T app) throws AzureExecutionException;

    AppServicePlan updateAppServicePlan(final T app) throws AzureExecutionException;
}
