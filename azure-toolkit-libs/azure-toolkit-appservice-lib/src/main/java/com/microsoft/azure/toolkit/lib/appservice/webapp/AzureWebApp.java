/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
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
    public List<Runtime> listWebAppRuntimes(@Nonnull OperatingSystem os, @Nonnull JavaVersion version) {
        return Runtime.WEBAPP_RUNTIME.stream()
            .filter(runtime -> os == runtime.getOperatingSystem() && Objects.equals(version, runtime.getJavaVersion()))
            .collect(Collectors.toList());
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
