/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceManager;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;

import javax.annotation.Nonnull;

public class AzureWebApp extends AzureAppService {

    @Nonnull
    @Override
    protected AppServiceResourceManager newResource(@Nonnull AppServiceManager remote) {
        return new WebAppResourceManager(remote, this);
    }
}
