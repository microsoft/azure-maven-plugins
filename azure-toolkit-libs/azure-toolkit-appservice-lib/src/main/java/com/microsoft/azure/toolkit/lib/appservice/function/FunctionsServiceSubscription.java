/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.fluent.models.FunctionAppStackInner;
import com.azure.resourcemanager.appservice.models.FunctionAppMajorVersion;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionAppWindowsRuntime;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Nonnull
    public List<FunctionAppRuntime> listFunctionAppMajorRuntimes() {
        if (!FunctionAppWindowsRuntime.isLoaded() && !FunctionAppLinuxRuntime.isLoaded()) {
            loadRuntimes();
        }
        return Stream.concat(FunctionAppWindowsRuntime.getMajorRuntimes().stream(), FunctionAppLinuxRuntime.getMajorRuntimes().stream()).collect(Collectors.toList());
    }

    public synchronized void loadRuntimes() {
        final AppServiceManager remote = this.getRemote();
        if (Objects.isNull(remote)) {
            return;
        }
        if (FunctionAppWindowsRuntime.isLoaded() && FunctionAppLinuxRuntime.isLoaded()) {
            return;
        }

        final List<FunctionAppMajorVersion> javaStacks = remote.serviceClient().getProviders()
            .getFunctionAppStacksAsync().toStream()
            .filter(stack -> StringUtils.equalsIgnoreCase(stack.name(), "java"))
            .findFirst().map(FunctionAppStackInner::majorVersions).orElse(Collections.emptyList());

        // fill `Runtime` only with major versions
        FunctionAppLinuxRuntime.loadAllFunctionAppLinuxRuntimes(javaStacks);
        FunctionAppWindowsRuntime.loadAllFunctionAppWindowsRuntimes(javaStacks);
    }
}
