/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class FunctionsServiceSubscription extends AppServiceServiceSubscription {

    protected FunctionsServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureAppService service) {
        super(subscriptionId, service);
    }

    protected FunctionsServiceSubscription(@Nonnull AppServiceManager remote, @Nonnull AzureAppService service) {
        super(remote, service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(this.getFunctionAppModule());
    }
}
