/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.webapp;

import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceServiceSubscription;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerConsumer;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

@Getter
public class WebApp extends WebAppBase<WebApp, AppServiceServiceSubscription, com.azure.resourcemanager.appservice.models.WebApp>
    implements Deletable, ServiceLinkerConsumer {
    @Nonnull
    private final WebAppDeploymentSlotModule deploymentModule;
    private final ServiceLinkerModule linkerModule;
    /**
     * copy constructor
     */
    protected WebApp(@Nonnull WebApp origin) {
        super(origin);
        this.deploymentModule = origin.deploymentModule;
        this.linkerModule = origin.linkerModule;
    }

    protected WebApp(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull WebAppModule module) {
        super(name, resourceGroupName, module);
        this.deploymentModule = new WebAppDeploymentSlotModule(this);
        this.linkerModule = new ServiceLinkerModule(getId(), this);
    }

    protected WebApp(@Nonnull WebAppBasic remote, @Nonnull WebAppModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.deploymentModule = new WebAppDeploymentSlotModule(this);
        this.linkerModule = new ServiceLinkerModule(getId(), this);
    }

    @AzureOperation(name = "azure/webapp.swap_slot.app|slot", params = {"this.getName()", "slotName"})
    public void swap(String slotName) {
        this.doModify(() -> {
            Objects.requireNonNull(this.getRemote()).swap(slotName);
            AzureMessager.getMessager().info(AzureString.format("Swap deployment slot %s into production successfully", slotName));
        }, Status.UPDATING);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Arrays.asList(deploymentModule, linkerModule);
    }

    @Nonnull
    public WebAppDeploymentSlotModule slots() {
        return this.deploymentModule;
    }

    @Override
    public ServiceLinkerModule getServiceLinkerModule() {
        return linkerModule;
    }

    @Nullable
    @Override
    protected com.azure.resourcemanager.appservice.models.WebApp doModify(@Nonnull Callable<com.azure.resourcemanager.appservice.models.WebApp> body, @Nullable String status) {
        // override only to provide package visibility
        return super.doModify(body, status);
    }
}
