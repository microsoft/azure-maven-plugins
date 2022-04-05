/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceResourceManager;
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

public class AzureFunctions extends AzureAppService {

    @Nonnull
    public FunctionAppModule functionApps(@Nonnull String subscriptionId) {
        final AppServiceResourceManager rm = get(subscriptionId, null);
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
    @AzureOperation(name = "function.list_runtimes.os|version", params = {"os.getValue()", "version.getValue()"}, type = AzureOperation.Type.SERVICE)
    public List<Runtime> listFunctionAppRuntimes(@Nonnull OperatingSystem os, @Nonnull JavaVersion version) {
        return Runtime.FUNCTION_APP_RUNTIME.stream()
            .filter(runtime -> Objects.equals(os, runtime.getOperatingSystem()) && Objects.equals(version, runtime.getJavaVersion()))
            .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    protected AppServiceResourceManager newResource(@Nonnull AppServiceManager remote) {
        return new FunctionsResourceManager(remote, this);
    }
}
