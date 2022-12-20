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
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class WebApp extends WebAppBase<WebApp, AppServiceServiceSubscription, com.azure.resourcemanager.appservice.models.WebApp>
    implements Deletable {

    @Nonnull
    private final WebAppDeploymentSlotModule deploymentModule;

    /**
     * copy constructor
     */
    protected WebApp(@Nonnull WebApp origin) {
        super(origin);
        this.deploymentModule = origin.deploymentModule;
    }

    protected WebApp(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull WebAppModule module) {
        super(name, resourceGroupName, module);
        this.deploymentModule = new WebAppDeploymentSlotModule(this);
    }

    protected WebApp(@Nonnull WebAppBasic remote, @Nonnull WebAppModule module) {
        super(remote.name(), remote.resourceGroupName(), module);
        this.deploymentModule = new WebAppDeploymentSlotModule(this);
    }

    @AzureOperation(name = "azure/webapp.swap_slot.app|slot", params = {"this.getName()", "slotName"})
    public void swap(String slotName) {
        this.doModify(() -> {
            Objects.requireNonNull(this.getFullRemote()).swap(slotName);
            AzureMessager.getMessager().info(AzureString.format("Swap deployment slot %s into production successfully", slotName));
        }, Status.UPDATING);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(deploymentModule);
    }

    @Nonnull
    public WebAppDeploymentSlotModule slots() {
        return this.deploymentModule;
    }
}
