/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class AzureFunctions extends AzureAppService {

    @Nonnull
    public FunctionAppModule functionApps(@Nonnull String subscriptionId) {
        final AppServiceServiceSubscription rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getFunctionAppModule();
    }

    @Nonnull
    public List<FunctionApp> functionApps() {
        return this.list().stream().flatMap(m -> m.functionApps().list().stream()).collect(Collectors.toList());
    }

    @Nullable
    public FunctionApp functionApp(String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        return this.functionApps(id.subscriptionId()).get(id.name(), id.resourceGroupName());
    }

    @Nonnull
    @Override
    protected AppServiceServiceSubscription newResource(@Nonnull AppServiceManager remote) {
        return new FunctionsServiceSubscription(remote, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Function Apps";
    }

    public String getServiceNameForTelemetry() {
        return "function";
    }
}
