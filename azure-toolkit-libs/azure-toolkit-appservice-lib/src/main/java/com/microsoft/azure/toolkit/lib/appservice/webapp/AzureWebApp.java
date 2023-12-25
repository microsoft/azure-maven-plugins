/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class AzureWebApp extends AzureAppService {

    @Nonnull
    public WebAppModule webApps(@Nonnull String subscriptionId) {
        final AppServiceServiceSubscription rm = get(subscriptionId, null);
        assert rm != null;
        return rm.getWebAppModule();
    }

    @Nonnull
    public List<WebApp> webApps() {
        return this.list().stream().flatMap(m -> m.webApps().list().stream()).collect(Collectors.toList());
    }

    @Nullable
    public WebApp webApp(String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        return this.webApps(id.subscriptionId()).get(id.name(), id.resourceGroupName());
    }

    @Nonnull
    @Override
    protected AppServiceServiceSubscription newResource(@Nonnull AppServiceManager remote) {
        return new WebAppServiceSubscription(remote, this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Web Apps";
    }

    public String getServiceNameForTelemetry() {
        return "webapp";
    }
}
