/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.AppServiceManager;
import com.azure.resourcemanager.appservice.fluent.models.WebAppStackInner;
import com.azure.resourcemanager.appservice.models.WebAppMajorVersion;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppLinuxRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppWindowsRuntime;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebAppServiceSubscription extends AppServiceServiceSubscription {

    protected WebAppServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureAppService service) {
        super(subscriptionId, service);
    }

    protected WebAppServiceSubscription(@Nonnull AppServiceManager remote, @Nonnull AzureAppService service) {
        super(remote, service);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(this.getWebAppModule());
    }

    @Nonnull
    public List<WebAppRuntime> listWebAppMajorRuntimes() {
        if (!WebAppWindowsRuntime.isLoaded() && !WebAppLinuxRuntime.isLoaded()) {
            loadRuntimes();
        }
        return Stream.concat(WebAppWindowsRuntime.getMajorRuntimes().stream(), WebAppLinuxRuntime.getMajorRuntimes().stream()).collect(Collectors.toList());
    }

    public synchronized void loadRuntimes() {
        final AppServiceManager remote = this.getRemote();
        if (Objects.isNull(remote)) {
            return;
        }
        if (WebAppWindowsRuntime.isLoaded() && WebAppLinuxRuntime.isLoaded()) {
            return;
        }

        final Map<String, WebAppStackInner> stacks = remote.serviceClient().getProviders()
            .getWebAppStacksAsync().toStream()
            .filter(stack -> StringUtils.equalsAnyIgnoreCase(stack.name(), "javacontainers", "java"))
            .collect(Collectors.toMap(s -> s.name().toLowerCase(), s -> s));

        final List<WebAppMajorVersion> javaStacks = stacks.get("java").majorVersions();
        final List<WebAppMajorVersion> containerStacks = stacks.get("javacontainers").majorVersions();

        // fill `Runtime` only with major versions
        WebAppLinuxRuntime.loadAllWebAppLinuxRuntimes(javaStacks, containerStacks);
        WebAppWindowsRuntime.loadAllWebAppWindowsRuntimes(javaStacks, containerStacks);
    }
}
